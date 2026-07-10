package com.commit451.drebin451.stripe

import com.commit451.drebin451.model.BillingSession
import com.commit451.drebin451.model.PlanIds
import com.commit451.drebin451.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.abs

object StripeBilling {
    private val log = LoggerFactory.getLogger(StripeBilling::class.java)
    private val client = HttpClient.newHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    private val secretKey: String?
        get() = System.getenv("STRIPE_SECRET_KEY")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    private val webhookSecret: String?
        get() = System.getenv("STRIPE_WEBHOOK_SECRET")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    val proPriceId: String?
        get() = System.getenv("STRIPE_PRO_PRICE_ID")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    val syncStaleMs: Long
        get() = System.getenv("STRIPE_SYNC_STALE_MS")
            ?.toLongOrNull()
            ?.takeIf { it > 0 }
            ?: 6L * 60L * 60L * 1000L

    val isConfigured: Boolean
        get() = secretKey != null && proPriceId != null

    private val defaultReturnUrl: String
        get() = System.getenv("STRIPE_RETURN_URL")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: "https://drebin451.com"

    private val checkoutSuccessUrl: String
        get() = System.getenv("STRIPE_CHECKOUT_SUCCESS_URL")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: "$defaultReturnUrl?billing=success"

    private val checkoutCancelUrl: String
        get() = System.getenv("STRIPE_CHECKOUT_CANCEL_URL")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: "$defaultReturnUrl?billing=cancelled"

    private val portalReturnUrl: String
        get() = System.getenv("STRIPE_PORTAL_RETURN_URL")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: defaultReturnUrl

    suspend fun createCustomer(user: User): String {
        val root = postForm(
            path = "/v1/customers",
            params = buildList {
                add("metadata[uid]" to user.uid)
                if (user.email.isNotBlank()) add("email" to user.email)
                if (user.displayName.isNotBlank()) add("name" to user.displayName)
            },
        )
        return root.string("id") ?: error("Stripe did not return a customer id")
    }

    suspend fun createCheckoutSession(user: User): BillingSession {
        val customerId = user.stripeCustomerId.takeIf { it.isNotBlank() }
            ?: error("Stripe customer id is required")
        val priceId = requireProPriceId()
        val root = postForm(
            path = "/v1/checkout/sessions",
            params = listOf(
                "mode" to "subscription",
                "customer" to customerId,
                "client_reference_id" to user.uid,
                "line_items[0][price]" to priceId,
                "line_items[0][quantity]" to "1",
                "success_url" to checkoutSuccessUrl,
                "cancel_url" to checkoutCancelUrl,
                "allow_promotion_codes" to "true",
                "metadata[uid]" to user.uid,
                "subscription_data[metadata][uid]" to user.uid,
            ),
        )
        return BillingSession(root.string("url") ?: error("Stripe did not return a checkout URL"))
    }

    suspend fun createPortalSession(user: User): BillingSession {
        val customerId = user.stripeCustomerId.takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("No Stripe customer exists for this user yet")
        val root = postForm(
            path = "/v1/billing_portal/sessions",
            params = listOf(
                "customer" to customerId,
                "return_url" to portalReturnUrl,
            ),
        )
        return BillingSession(root.string("url") ?: error("Stripe did not return a portal URL"))
    }

    suspend fun currentSubscriptionFor(customerId: String): StripeSubscriptionUpdate? {
        if (customerId.isBlank()) return null
        val priceId = requireProPriceId()
        val root = get(
            path = "/v1/subscriptions",
            params = listOf(
                "customer" to customerId,
                "status" to "all",
                "limit" to "100",
            ),
        )
        val matchingSubscriptions = root["data"]
            ?.jsonArrayOrNull()
            .orEmpty()
            .mapNotNull {
                it.jsonObjectOrNull()?.subscriptionUpdate(configuredProPriceId = priceId)
            }
            .filter { it.priceId == priceId }
        return matchingSubscriptions.firstOrNull { it.plan == PlanIds.PRO }
            ?: matchingSubscriptions.firstOrNull()
    }

    /**
     * Cancels any still-billable Stripe subscription before account deletion removes the local user
     * record that Stripe webhooks normally reconcile against. No-ops when Stripe is not configured
     * or the user has no active/trialing/past-due subscription.
     */
    suspend fun cancelSubscriptionForAccountDeletion(user: User): Boolean {
        if (!isConfigured) return false
        val refreshedSubscription = user.stripeCustomerId
            .takeIf { it.isNotBlank() }
            ?.let { currentSubscriptionFor(it) }
        val subscriptionId = if (refreshedSubscription != null) {
            refreshedSubscription
                .takeIf { cancelableSubscriptionStatus(it.status) }
                ?.subscriptionId
                ?.takeIf { it.isNotBlank() }
                ?: return false
        } else {
            user.stripeSubscriptionId
                .takeIf { it.isNotBlank() && cancelableSubscriptionStatus(user.stripeSubscriptionStatus) }
                ?: return false
        }

        delete(path = "/v1/subscriptions/${urlEncode(subscriptionId)}")
        return true
    }

    fun webhookUpdate(payload: String, signatureHeader: String?): StripeSubscriptionUpdate? {
        val secret = webhookSecret ?: error("Stripe webhook secret is not configured")
        if (!isValidWebhookSignature(
                payload = payload,
                signatureHeader = signatureHeader,
                secret = secret
            )
        ) {
            throw IllegalArgumentException("Invalid Stripe webhook signature")
        }
        return webhookUpdateFromPayload(payload, configuredProPriceId = requireProPriceId())
    }

    internal fun webhookUpdateFromPayload(
        payload: String,
        configuredProPriceId: String,
    ): StripeSubscriptionUpdate? {
        val root = json.parseToJsonElement(payload).jsonObject
        val type = root.string("type") ?: return null
        val dataObject = root["data"]
            ?.jsonObjectOrNull()
            ?.get("object")
            ?.jsonObjectOrNull()
            ?: return null

        return when {
            type == "checkout.session.completed" -> checkoutSessionUpdate(
                dataObject,
                configuredProPriceId
            )

            type.startsWith("customer.subscription.") -> dataObject.subscriptionUpdate(
                configuredProPriceId
            )

            else -> null
        }
    }

    internal fun planForSubscription(
        status: String,
        priceId: String,
        configuredProPriceId: String
    ): String {
        val isActive = status.lowercase() in setOf("active", "trialing")
        return if (isActive && priceId == configuredProPriceId) PlanIds.PRO else PlanIds.FREE
    }

    internal fun cancelableSubscriptionStatus(status: String): Boolean =
        status.lowercase() in setOf("active", "trialing", "past_due", "unpaid", "incomplete")

    internal fun isValidWebhookSignature(
        payload: String,
        signatureHeader: String?,
        secret: String,
        nowEpochSeconds: Long = Instant.now().epochSecond,
        toleranceSeconds: Long = 300,
    ): Boolean {
        val fields = signatureHeader
            ?.split(',')
            .orEmpty()
            .mapNotNull { part ->
                val separator = part.indexOf('=')
                if (separator <= 0) null else part.substring(0, separator) to part.substring(
                    separator + 1
                )
            }
        val timestamp =
            fields.firstOrNull { it.first == "t" }?.second?.toLongOrNull() ?: return false
        if (abs(nowEpochSeconds - timestamp) > toleranceSeconds) return false
        val signedPayload = "$timestamp.$payload"
        val expected = hmacSha256Hex(secret = secret, message = signedPayload)
        return fields
            .filter { it.first == "v1" }
            .any { (_, signature) ->
                MessageDigest.isEqual(
                    expected.encodeToByteArray(),
                    signature.lowercase().encodeToByteArray(),
                )
            }
    }

    internal fun testSignatureHeader(payload: String, secret: String, timestamp: Long): String =
        "t=$timestamp,v1=${hmacSha256Hex(secret, "$timestamp.$payload")}"

    private fun checkoutSessionUpdate(
        session: JsonObject,
        configuredProPriceId: String,
    ): StripeSubscriptionUpdate? {
        if (session.string("mode") != "subscription") return null
        val uid = session.string("client_reference_id")
            ?: session.metadataString("uid")
            ?: return null
        val customerId = session.string("customer") ?: return null
        val subscriptionId = session.string("subscription") ?: ""
        return StripeSubscriptionUpdate(
            uid = uid,
            customerId = customerId,
            subscriptionId = subscriptionId,
            status = "active",
            priceId = configuredProPriceId,
            plan = PlanIds.PRO,
        )
    }

    private fun JsonObject.subscriptionUpdate(configuredProPriceId: String): StripeSubscriptionUpdate? {
        val customerId = string("customer") ?: return null
        val status = string("status") ?: ""
        val priceId = subscriptionPriceId() ?: ""
        return StripeSubscriptionUpdate(
            uid = metadataString("uid"),
            customerId = customerId,
            subscriptionId = string("id") ?: "",
            status = status,
            priceId = priceId,
            currentPeriodEnd = secondsToMillis(long("current_period_end")),
            plan = planForSubscription(status, priceId, configuredProPriceId),
        )
    }

    private suspend fun postForm(path: String, params: List<Pair<String, String>>): JsonObject {
        val secret = requireSecretKey()
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.stripe.com$path"))
            .header("Authorization", "Bearer $secret")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(formEncode(params)))
            .build()
        return send(request, path)
    }

    private suspend fun get(path: String, params: List<Pair<String, String>>): JsonObject {
        val secret = requireSecretKey()
        val query = formEncode(params)
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.stripe.com$path?$query"))
            .header("Authorization", "Bearer $secret")
            .GET()
            .build()
        return send(request, path)
    }

    private suspend fun delete(path: String): JsonObject {
        val secret = requireSecretKey()
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.stripe.com$path"))
            .header("Authorization", "Bearer $secret")
            .DELETE()
            .build()
        return send(request, path)
    }

    private suspend fun send(request: HttpRequest, path: String): JsonObject {
        val response = withContext(Dispatchers.IO) {
            client.send(request, HttpResponse.BodyHandlers.ofString())
        }
        if (response.statusCode() !in 200..299) {
            log.warn(
                "Stripe request to {} failed: HTTP {} {}",
                path,
                response.statusCode(),
                response.body().take(500)
            )
            error("Stripe request failed")
        }
        return json.parseToJsonElement(response.body()).jsonObject
    }

    private fun requireSecretKey(): String =
        secretKey ?: error("Stripe secret key is not configured")

    private fun requireProPriceId(): String =
        proPriceId ?: error("Stripe Pro price id is not configured")

    private fun JsonObject.subscriptionPriceId(): String? =
        get("items")
            ?.jsonObjectOrNull()
            ?.get("data")
            ?.jsonArrayOrNull()
            ?.firstOrNull()
            ?.jsonObjectOrNull()
            ?.get("price")
            ?.jsonObjectOrNull()
            ?.string("id")

    private fun JsonObject.metadataString(key: String): String? =
        get("metadata")
            ?.jsonObjectOrNull()
            ?.string(key)

    private fun JsonObject.string(key: String): String? =
        (get(key) as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

    private fun JsonObject.long(key: String): Long =
        get(key)?.jsonPrimitiveOrNull()?.longOrNull ?: 0L

    private fun secondsToMillis(seconds: Long): Long =
        if (seconds > 0) seconds * 1000L else 0L

    private fun formEncode(params: List<Pair<String, String>>): String =
        params.joinToString("&") { (key, value) -> "${urlEncode(key)}=${urlEncode(value)}" }

    private fun urlEncode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")

    private fun hmacSha256Hex(secret: String, message: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(message.toByteArray(StandardCharsets.UTF_8)).joinToString("") { byte ->
            "%02x".format(byte)
        }
    }

    private fun kotlinx.serialization.json.JsonElement.jsonObjectOrNull(): JsonObject? =
        this as? JsonObject

    private fun kotlinx.serialization.json.JsonElement.jsonArrayOrNull(): JsonArray? =
        this as? JsonArray

    private fun kotlinx.serialization.json.JsonElement.jsonPrimitiveOrNull(): JsonPrimitive? =
        this as? JsonPrimitive
}
