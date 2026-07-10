package com.commit451.drebin451

import com.commit451.drebin451.apikey.ApiKeys
import com.commit451.drebin451.apk.parseApk
import com.commit451.drebin451.firebase.ApiKeyHeader
import com.commit451.drebin451.firebase.Firebasis
import com.commit451.drebin451.firebase.Messenger
import com.commit451.drebin451.firebase.StorageQuotaExceededException
import com.commit451.drebin451.firebase.apkStoragePath
import com.commit451.drebin451.firebase.passwordResetRateLimiter
import com.commit451.drebin451.firebase.requireFirebaseUser
import com.commit451.drebin451.firebase.requireUploader
import com.commit451.drebin451.firebase.requireUser
import com.commit451.drebin451.model.ApiKey
import com.commit451.drebin451.model.ApiKeyCreated
import com.commit451.drebin451.model.AppVersion
import com.commit451.drebin451.model.CreateApiKeyRequest
import com.commit451.drebin451.model.PasswordResetRequest
import com.commit451.drebin451.model.VersionNote
import com.commit451.drebin451.model.storageStatus
import com.commit451.drebin451.stripe.StripeBilling
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.request.receiveChannel
import io.ktor.server.request.receiveMultipart
import io.ktor.server.request.receiveText
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.UUID

/** Upper bound on the free-text note stored per upload, to keep version rows small. */
internal const val MAX_NOTE_LENGTH = 2000

internal const val STORAGE_QUOTA_EXCEEDED_ERROR_MESSAGE =
    "Storage limit exceeded, please update your plan to continue uploading"

internal const val CronSecretHeader = "X-Cron-Secret"

internal const val PasswordResetRequestMaxBytes = 1024L

/** Base for App Links embedded in push notifications (see the Android manifest intent filter). */
private const val DEEP_LINK_BASE = "https://drebin451.com"

private val PasswordResetRequestJson = Json { ignoreUnknownKeys = true }

internal fun configuredCronSecret(env: Map<String, String> = System.getenv()): String? =
    env["DREBIN451_CRON_SECRET"]
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: env["CRON_SECRET"]
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

internal fun isAuthorizedCronSecret(presented: String?, configured: String?): Boolean {
    val expected = configured?.takeIf { it.isNotBlank() } ?: return false
    val actual = presented?.trim()?.takeIf { it.isNotEmpty() } ?: return false
    return MessageDigest.isEqual(
        actual.toByteArray(Charsets.UTF_8),
        expected.toByteArray(Charsets.UTF_8),
    )
}

internal fun normalizeVersionNote(note: String): String = note.trim().take(MAX_NOTE_LENGTH)

internal fun newVersionPushBody(versionName: String, versionCode: Long, note: String): String {
    val versionLabel = if (versionName.isNotBlank()) {
        "Version $versionName ($versionCode)"
    } else {
        "Build $versionCode"
    }
    val base = "$versionLabel is available"
    val normalizedNote = normalizeVersionNote(note)
    return if (normalizedNote.isBlank()) base else "$base\n$normalizedNote"
}

internal fun newVersionPushDeepLink(shareId: String, versionId: String): String {
    require(shareId.isNotBlank()) { "shareId is required" }
    require(versionId.isNotBlank()) { "versionId is required" }
    return "$DEEP_LINK_BASE/app/$shareId/releases/$versionId"
}

fun main() {
    Firebasis.initialize()
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // CORS: the web client is hosted on drebin451.com and calls the API on api.drebin451.com.
    // Auth is via bearer/API-key headers, not cookies; keep browser origins explicit for production.
    install(CORS) {
        allowHost("drebin451.com", schemes = listOf("https"))
        allowHost("www.drebin451.com", schemes = listOf("https"))
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(ApiKeyHeader)
        allowNonSimpleContentTypes = true
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
    }
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            val status = statusForException(cause)
            call.application.log.error("Request failed", cause)
            when (val errorResponse = errorResponseForException(cause)) {
                null -> call.respondText(cause.message ?: "Unknown error", status = status)
                else -> call.respond(status, errorResponse)
            }
        }
    }

    val prefix = "v1"
    // Base URL embedded in public asset URLs (currently launcher icons). For local dev this points
    // at the Android-emulator host; set PUBLIC_BASE_URL once the server is deployed publicly.
    val publicBaseUrl = (System.getenv("PUBLIC_BASE_URL") ?: "http://10.0.2.2:8080").trimEnd('/')

    routing {
        get("/") {
            call.respondText("OK")
        }

        // Digital Asset Links — lets Android auto-verify our App Links
        // (https://drebin451.com/app/..). Relevant when that host is mapped to this
        // server; the fingerprint is the checked-in release keystore's (debug shares it).
        get("/.well-known/assetlinks.json") {
            call.respondText(ASSET_LINKS_JSON, ContentType.Application.Json)
        }

        // App-wide client config (unauthenticated): kill-switch + maintenance message, read by
        // the client on splash before it knows whether anyone is signed in.
        get("/$prefix/config") {
            call.respond(Firebasis.getConfig())
        }

        // Public password-reset flow. Firebase sends the email; the server intentionally responds
        // the same way for unknown emails and throttled repeat requests so the endpoint cannot be
        // used to enumerate accounts or spam reset messages.
        post("/$prefix/auth/password-reset") {
            val request = call.receivePasswordResetRequest()
            if (passwordResetRateLimiter.tryAcquire(request.email, call.passwordResetClientKey())) {
                Firebasis.sendPasswordResetEmail(request.email)
            }
            call.respond(HttpStatusCode.NoContent)
        }

        // Maintenance endpoint for a trusted scheduler. Dry-run by default; pass ?delete=true
        // to remove B2 objects under managed prefixes that no Firestore app/version references.
        post("/$prefix/cron/storage/reconcile") {
            if (!call.requireCronSecret()) return@post
            call.respond(
                Firebasis.reconcileObjectStorage(
                    deleteOrphans = call.booleanQuery("delete"),
                    sampleLimit = call.sampleLimit(),
                ),
            )
        }

        // Stripe sends unsigned browser redirects through Checkout/Portal, and signed lifecycle
        // updates through this webhook. Webhooks are the source of truth for Pro entitlement.
        post("/$prefix/stripe/webhook") {
            val payload = call.receiveText()
            val signature = call.request.headers["Stripe-Signature"]
            StripeBilling.webhookUpdate(payload, signature)?.let { update ->
                Firebasis.applyStripeSubscriptionUpdate(update)
            }
            call.respond(HttpStatusCode.OK)
        }

        // Current user (creates the Firestore user doc on first sign-in).
        get("/$prefix/user") {
            val auth = requireUser() ?: return@get
            call.respond(currentUser(auth))
        }

        // Permanently delete the caller's account and all owned server-side data. This route only
        // accepts a Firebase bearer token (not API keys) because it also deletes the Firebase Auth
        // user after Firestore/B2 cleanup succeeds.
        delete("/$prefix/user") {
            val auth = requireFirebaseUser() ?: return@delete
            Firebasis.deleteAccount(auth.uid)
            call.respond(HttpStatusCode.NoContent)
        }

        // Server-computed upload storage quota for the current user. Home fetches this
        // asynchronously so it can warn before the next upload fails at the limit.
        get("/$prefix/user/storage") {
            val auth = requireUser() ?: return@get
            val user = Firebasis.refreshPlanIfStale(currentUser(auth))
            call.respond(user.storageStatus())
        }

        // Start a Stripe Checkout subscription flow for Pro.
        post("/$prefix/billing/checkout") {
            val auth = requireUser() ?: return@post
            var user = currentUser(auth)
            if (user.stripeCustomerId.isBlank()) {
                val customerId = StripeBilling.createCustomer(user)
                user = Firebasis.updateStripeCustomerId(user.uid, customerId)
            }
            call.respond(StripeBilling.createCheckoutSession(user))
        }

        // Start a Stripe Customer Portal flow for managing an existing subscription.
        post("/$prefix/billing/portal") {
            val auth = requireUser() ?: return@post
            val user = currentUser(auth)
            call.respond(StripeBilling.createPortalSession(user))
        }

        // Reconcile the user's server-side plan from Stripe. Webhooks should normally keep this fresh,
        // but the client can call this after returning from Checkout/Portal or on manual refresh.
        post("/$prefix/billing/refresh") {
            val auth = requireUser() ?: return@post
            currentUser(auth)
            call.respond(Firebasis.refreshStripePlan(auth.uid))
        }

        // Backward-compatible alias for older clients that still call the previous subscription-refresh path.
        post("/$prefix/user/subscription/refresh") {
            val auth = requireUser() ?: return@post
            currentUser(auth)
            call.respond(Firebasis.refreshStripePlan(auth.uid))
        }

        // The caller's API keys — metadata only; the hash and plaintext token are never returned.
        get("/$prefix/api-keys") {
            val auth = requireUser() ?: return@get
            call.respond(Firebasis.listApiKeys(auth.uid))
        }

        // Mint an API key. Its plaintext token is returned exactly once, here — never again.
        post("/$prefix/api-keys") {
            val auth = requireUser() ?: return@post
            // Ensure the user doc exists so a key can later resolve its owner on upload.
            currentUser(auth)
            val request = call.receive<CreateApiKeyRequest>()
            val generated = ApiKeys.generate()
            val apiKey = ApiKey(
                id = UUID.randomUUID().toString(),
                ownerUserId = auth.uid,
                label = request.label.trim().ifBlank { "API key" },
                maskedToken = generated.maskedToken,
                createdAt = System.currentTimeMillis(),
                tokenHash = generated.tokenHash,
            )
            Firebasis.createApiKey(apiKey)
            call.respond(ApiKeyCreated(apiKey = apiKey, token = generated.token))
        }

        // Revoke an API key (owner only).
        delete("/$prefix/api-keys/{id}") {
            val auth = requireUser() ?: return@delete
            val id = call.parameters["id"] ?: throw IllegalArgumentException("Missing id")
            when (val key = Firebasis.getApiKey(id)) {
                null -> call.respond(HttpStatusCode.NotFound)
                else -> if (key.ownerUserId != auth.uid) {
                    call.respond(HttpStatusCode.Forbidden)
                } else {
                    Firebasis.deleteApiKey(id)
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }

        // The caller's own apps — one entry per applicationId, newest-updated first, paged 10 at a time by default.
        get("/$prefix/apps") {
            val auth = requireUser() ?: return@get
            call.respond(
                Firebasis.listApps(
                    uid = auth.uid,
                    pageSize = call.pageSize(),
                    pageToken = call.pageToken(),
                ),
            )
        }

        // Apps the caller added by opening random public share URLs.
        get("/$prefix/shared-apps") {
            val auth = requireUser() ?: return@get
            call.respond(
                Firebasis.listSharedApps(
                    uid = auth.uid,
                    pageSize = call.pageSize(),
                    pageToken = call.pageToken(),
                ),
            )
        }

        // Open a public app share: add the app to this user's Shared collection and return it.
        post("/$prefix/app-shares/{shareId}") {
            val auth = requireUser() ?: return@post
            val shareId =
                call.parameters["shareId"] ?: throw IllegalArgumentException("Missing shareId")
            currentUser(auth)
            when (val app = Firebasis.addSharedApp(shareId, auth.uid)) {
                null -> call.respond(HttpStatusCode.NotFound)
                else -> call.respond(app)
            }
        }

        // Remove a shared app from this user's collection without affecting the owner.
        delete("/$prefix/shared-apps/{appId}") {
            val auth = requireUser() ?: return@delete
            val appId = call.parameters["appId"] ?: throw IllegalArgumentException("Missing appId")
            Firebasis.removeSharedApp(auth.uid, appId)
            call.respond(HttpStatusCode.NoContent)
        }

        // Upload an APK (multipart): part "apk" = file. The applicationId, version and label
        // are read from the APK itself — this creates the app on the first upload of an
        // applicationId, or adds another version to the existing app otherwise.
        post("/$prefix/apps") {
            // Accepts a full-access API key (CI/scripts) via X-API-Key, or a Firebase session;
            // either resolves to the owning user.
            val user = Firebasis.refreshPlanIfStale(requireUploader() ?: return@post)

            var fileName: String? = null
            var bytes: ByteArray? = null
            var note = ""

            call.receiveMultipart().forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        fileName = part.originalFileName?.takeIf { it.isNotBlank() } ?: "app.apk"
                        bytes = part.provider().readRemaining().readByteArray()
                    }
                    // Optional free-text annotation (CI sends the commit message); trimmed and
                    // capped. Any other form field is ignored.
                    is PartData.FormItem -> {
                        if (part.name == "note") note = normalizeVersionNote(part.value)
                    }

                    else -> {}
                }
                part.release()
            }

            val data = bytes ?: throw IllegalArgumentException("Missing 'apk' file part")
            val name = fileName ?: "app.apk"
            val info = parseApk(data)

            val appId = Firebasis.appDocId(user.uid, info.applicationId)
            val versionId = UUID.randomUUID().toString()
            val storagePath = apkStoragePath(user.uid, info.applicationId, versionId, name)
            val uploadSizeBytes = data.size.toLong()

            var storageReserved = false
            var iconUploaded = false
            val iconStoragePath = Firebasis.iconStoragePath(user.uid, info.applicationId)

            val (version, app) = try {
                Firebasis.reserveStorage(user.uid, uploadSizeBytes)
                storageReserved = true

                Firebasis.uploadBytes(storagePath, data, AppVersion.CONTENT_TYPE_APK)

                // Capture the launcher icon once: store it (and set the app's imageUrl) only while the
                // app has no icon yet — the first upload that yields a raster one. Served publicly by
                // GET /apps/{id}/icon.
                val existingImageUrl = Firebasis.getApp(appId)?.imageUrl ?: ""
                val imageUrl = if (existingImageUrl.isBlank() && info.icon != null) {
                    Firebasis.uploadBytes(
                        iconStoragePath,
                        info.icon.bytes,
                        info.icon.contentType,
                    )
                    iconUploaded = true
                    "$publicBaseUrl/$prefix/apps/$appId/icon"
                } else {
                    existingImageUrl
                }

                val now = System.currentTimeMillis()
                val version = AppVersion(
                    id = versionId,
                    appId = appId,
                    applicationId = info.applicationId,
                    ownerUserId = user.uid,
                    versionName = info.versionName,
                    versionCode = info.versionCode,
                    fileName = name,
                    fileSizeBytes = uploadSizeBytes,
                    contentType = AppVersion.CONTENT_TYPE_APK,
                    createdAt = now,
                    updatedAt = now,
                    note = note,
                    storagePath = storagePath,
                )
                val app = Firebasis.addVersion(
                    version,
                    appLabel = info.label,
                    ownerName = user.displayName,
                    imageUrl = imageUrl
                )
                storageReserved = false
                version to app
            } catch (t: Throwable) {
                if (storageReserved) Firebasis.releaseStorage(user.uid, uploadSizeBytes)
                Firebasis.deleteBlob(storagePath)
                if (iconUploaded) Firebasis.deleteBlob(iconStoragePath)
                throw t
            }

            // Notify devices that follow this app (subscribed to its update topic). Best-effort —
            // never fails the upload. The first upload of an app has no subscribers yet, so this is
            // naturally a no-op then.
            Messenger.sendNewVersion(
                appId = appId,
                versionId = version.id,
                title = info.label.ifBlank { info.applicationId },
                body = newVersionPushBody(version.versionName, version.versionCode, version.note),
                deepLink = newVersionPushDeepLink(app.shareId, version.id),
            )

            call.respond(version)
        }

        // Single app listing (auth; owner or added-to-Shared only).
        get("/$prefix/apps/{id}") {
            val auth = requireUser() ?: return@get
            val id = call.parameters["id"] ?: throw IllegalArgumentException("Missing id")
            when (val app = Firebasis.getApp(id)) {
                null -> call.respond(HttpStatusCode.NotFound)
                else -> if (!Firebasis.canAccessApp(auth.uid, app)) {
                    call.respond(HttpStatusCode.Forbidden)
                } else {
                    call.respond(app)
                }
            }
        }

        // Owner-only share-link revocation: replace the app's random share id so the old URL 404s.
        post("/$prefix/apps/{id}/share-id/refresh") {
            val auth = requireUser() ?: return@post
            val id = call.parameters["id"] ?: throw IllegalArgumentException("Missing id")
            when (val app = Firebasis.getApp(id)) {
                null -> call.respond(HttpStatusCode.NotFound)
                else -> if (app.ownerUserId != auth.uid) {
                    call.respond(HttpStatusCode.Forbidden)
                } else {
                    when (val updated = Firebasis.refreshAppShareId(id)) {
                        null -> call.respond(HttpStatusCode.NotFound)
                        else -> call.respond(updated)
                    }
                }
            }
        }

        // An app's uploaded versions, newest-updated first, paged 10 at a time by default (auth).
        get("/$prefix/apps/{id}/versions") {
            val auth = requireUser() ?: return@get
            val id = call.parameters["id"] ?: throw IllegalArgumentException("Missing id")
            when (val app = Firebasis.getApp(id)) {
                null -> call.respond(HttpStatusCode.NotFound)
                else -> if (!Firebasis.canAccessApp(auth.uid, app)) {
                    call.respond(HttpStatusCode.Forbidden)
                } else {
                    call.respond(
                        Firebasis.listVersions(
                            appId = id,
                            pageSize = call.pageSize(),
                            pageToken = call.pageToken(),
                        ),
                    )
                }
            }
        }

        // Single release/version detail (auth; owner or added-to-Shared only).
        get("/$prefix/apps/{appId}/versions/{versionId}") {
            val auth = requireUser() ?: return@get
            val appId = call.parameters["appId"] ?: throw IllegalArgumentException("Missing appId")
            val versionId =
                call.parameters["versionId"] ?: throw IllegalArgumentException("Missing versionId")
            val app = Firebasis.getApp(appId) ?: run {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }
            if (!Firebasis.canAccessApp(auth.uid, app)) {
                call.respond(HttpStatusCode.Forbidden)
                return@get
            }
            when (val version = Firebasis.getVersion(appId, versionId)) {
                null -> call.respond(HttpStatusCode.NotFound)
                else -> call.respond(version)
            }
        }

        // Fetch only the note for a specific uploaded version. The full version list already includes
        // this field; this small endpoint is for automation that wants just the note.
        get("/$prefix/apps/{appId}/versions/{versionId}/note") {
            val auth = requireUser() ?: return@get
            val appId = call.parameters["appId"] ?: throw IllegalArgumentException("Missing appId")
            val versionId =
                call.parameters["versionId"] ?: throw IllegalArgumentException("Missing versionId")
            val app = Firebasis.getApp(appId)
            if (app == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }
            if (!Firebasis.canAccessApp(auth.uid, app)) {
                call.respond(HttpStatusCode.Forbidden)
                return@get
            }
            when (val version = Firebasis.getVersion(appId, versionId)) {
                null -> call.respond(HttpStatusCode.NotFound)
                else -> call.respond(VersionNote(version.note))
            }
        }

        // Edit the free-text note on a version (owner only). This deliberately leaves updatedAt and
        // parent app aggregates unchanged so editing notes doesn't reorder APK releases.
        patch("/$prefix/apps/{appId}/versions/{versionId}/note") {
            val auth = requireUser() ?: return@patch
            val appId = call.parameters["appId"] ?: throw IllegalArgumentException("Missing appId")
            val versionId =
                call.parameters["versionId"] ?: throw IllegalArgumentException("Missing versionId")
            when (val version = Firebasis.getVersion(appId, versionId)) {
                null -> call.respond(HttpStatusCode.NotFound)
                else -> if (version.ownerUserId != auth.uid) {
                    call.respond(HttpStatusCode.Forbidden)
                } else {
                    val request = call.receive<VersionNote>()
                    val updated = version.copy(note = normalizeVersionNote(request.note))
                    Firebasis.updateVersionNote(updated)
                    call.respond(updated)
                }
            }
        }

        // Delete/clear the free-text note on a version without deleting the APK itself (owner only).
        delete("/$prefix/apps/{appId}/versions/{versionId}/note") {
            val auth = requireUser() ?: return@delete
            val appId = call.parameters["appId"] ?: throw IllegalArgumentException("Missing appId")
            val versionId =
                call.parameters["versionId"] ?: throw IllegalArgumentException("Missing versionId")
            when (val version = Firebasis.getVersion(appId, versionId)) {
                null -> call.respond(HttpStatusCode.NotFound)
                else -> if (version.ownerUserId != auth.uid) {
                    call.respond(HttpStatusCode.Forbidden)
                } else {
                    Firebasis.updateVersionNote(version.copy(note = ""))
                    call.respond(HttpStatusCode.NoContent)
                }
            }
        }

        // Delete an app and all of its versions (owner only).
        delete("/$prefix/apps/{id}") {
            val auth = requireUser() ?: return@delete
            val id = call.parameters["id"] ?: throw IllegalArgumentException("Missing id")
            when (Firebasis.deleteApp(id, auth.uid)) {
                Firebasis.DeleteAppResult.NotFound -> call.respond(HttpStatusCode.NotFound)
                Firebasis.DeleteAppResult.Forbidden -> call.respond(HttpStatusCode.Forbidden)
                is Firebasis.DeleteAppResult.Deleted -> call.respond(HttpStatusCode.NoContent)
            }
        }

        // Delete a single uploaded version (owner only).
        delete("/$prefix/apps/{appId}/versions/{versionId}") {
            val auth = requireUser() ?: return@delete
            val appId = call.parameters["appId"] ?: throw IllegalArgumentException("Missing appId")
            val versionId =
                call.parameters["versionId"] ?: throw IllegalArgumentException("Missing versionId")
            when (Firebasis.deleteVersion(appId, versionId, auth.uid)) {
                Firebasis.DeleteVersionResult.NotFound -> call.respond(HttpStatusCode.NotFound)
                Firebasis.DeleteVersionResult.Forbidden -> call.respond(HttpStatusCode.Forbidden)
                is Firebasis.DeleteVersionResult.Deleted -> call.respond(HttpStatusCode.NoContent)
            }
        }

        // Authenticated download of a specific version. The UI uses this internally for installs;
        // public sharing stays app-level via /app/{shareId}, not a per-version APK URL.
        get("/$prefix/apps/{appId}/versions/{versionId}/download") {
            val auth = requireUser() ?: return@get
            val appId = call.parameters["appId"] ?: throw IllegalArgumentException("Missing appId")
            val versionId =
                call.parameters["versionId"] ?: throw IllegalArgumentException("Missing versionId")
            val app = Firebasis.getApp(appId) ?: run {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }
            if (!Firebasis.canAccessApp(auth.uid, app)) {
                call.respond(HttpStatusCode.Forbidden)
                return@get
            }
            val version = Firebasis.getVersion(appId, versionId) ?: run {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }
            val blob = Firebasis.getBlob(version.storagePath) ?: run {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }
            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment
                    .withParameter(ContentDisposition.Parameters.FileName, version.fileName)
                    .toString(),
            )
            call.respondBytes(blob.bytes, ContentType.parse(version.contentType))
        }

        // Public, unauthenticated launcher icon for an app — this is App.imageUrl, loaded by the
        // client with Coil. The blob lives at a deterministic per-app path; its stored content type
        // drives the response.
        get("/$prefix/apps/{id}/icon") {
            val id = call.parameters["id"] ?: throw IllegalArgumentException("Missing id")
            val app = Firebasis.getApp(id) ?: run {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }
            val blob =
                Firebasis.getBlob(Firebasis.iconStoragePath(app.ownerUserId, app.applicationId))
                    ?: run {
                        call.respond(HttpStatusCode.NotFound)
                        return@get
                    }
            call.response.header(HttpHeaders.CacheControl, "public, max-age=86400")
            call.respondBytes(blob.bytes, ContentType.parse(blob.contentType ?: "image/png"))
        }
    }
}

private suspend fun ApplicationCall.requireCronSecret(): Boolean {
    val configured = configuredCronSecret()
    if (configured == null) {
        respond(
            HttpStatusCode.ServiceUnavailable,
            ErrorResponse("Cron secret is not configured"),
        )
        return false
    }
    if (!isAuthorizedCronSecret(request.headers[CronSecretHeader], configured)) {
        respond(HttpStatusCode.Unauthorized)
        return false
    }
    return true
}

private fun ApplicationCall.booleanQuery(name: String): Boolean =
    request.queryParameters[name]?.lowercase() in setOf("1", "true", "yes")

private fun ApplicationCall.passwordResetClientKey(): String? =
    // Use Ktor's resolved remote host rather than user-controllable forwarding headers.
    request.origin.remoteHost
        .trim()
        .takeIf { it.isNotEmpty() }

private suspend fun ApplicationCall.receivePasswordResetRequest(): PasswordResetRequest {
    request.headers[HttpHeaders.ContentLength]
        ?.toLongOrNull()
        ?.let(::requirePasswordResetRequestSize)

    val bytes = receiveChannel()
        .readRemaining(PasswordResetRequestMaxBytes + 1L)
        .readByteArray()
    requirePasswordResetRequestSize(bytes.size.toLong())

    return runCatching {
        PasswordResetRequestJson.decodeFromString<PasswordResetRequest>(bytes.toString(Charsets.UTF_8))
    }.getOrElse {
        throw IllegalArgumentException("Invalid password reset request.")
    }
}

internal fun requirePasswordResetRequestSize(byteCount: Long) {
    require(byteCount <= PasswordResetRequestMaxBytes) { "Password reset request is too large." }
}

private fun ApplicationCall.sampleLimit(default: Int = 100): Int =
    request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(0, 500) ?: default

private fun ApplicationCall.pageSize(): Int? =
    request.queryParameters["pageSize"]?.toIntOrNull()

private fun ApplicationCall.pageToken(): String? =
    request.queryParameters["pageToken"]?.takeIf { it.isNotBlank() }

internal fun statusForException(cause: Throwable): HttpStatusCode = when {
    cause.storageQuotaCause() != null -> HttpStatusCode.PaymentRequired
    cause is IllegalArgumentException -> HttpStatusCode.BadRequest
    else -> HttpStatusCode.InternalServerError
}

internal fun errorResponseForException(cause: Throwable): ErrorResponse? = when {
    cause.storageQuotaCause() != null -> ErrorResponse(STORAGE_QUOTA_EXCEEDED_ERROR_MESSAGE)
    else -> null
}

private tailrec fun Throwable.storageQuotaCause(): StorageQuotaExceededException? = when (this) {
    is StorageQuotaExceededException -> this
    else -> cause?.storageQuotaCause()
}

private suspend fun currentUser(auth: Firebasis.AuthUser) =
    Firebasis.getOrCreateUser(
        uid = auth.uid,
        email = auth.email,
        displayName = auth.name.ifBlank { auth.email.substringBefore('@') },
    )

private val ASSET_LINKS_JSON = """
    [
      {
        "relation": ["delegate_permission/common.handle_all_urls"],
        "target": {
          "namespace": "android_app",
          "package_name": "com.commit451.drebin451",
          "sha256_cert_fingerprints": [
            "45:72:46:AE:AB:AE:56:BA:40:60:A4:D6:49:6E:C6:F6:54:0F:0C:0F:9C:40:9F:BA:ED:73:EE:1D:A1:E3:CB:48"
          ]
        }
      }
    ]
""".trimIndent()
