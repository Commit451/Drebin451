package com.commit451.drebin451.firebase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

private val PasswordResetErrorJson = Json { ignoreUnknownKeys = true }

internal object PasswordResetEmailSender {
    private val log = LoggerFactory.getLogger(PasswordResetEmailSender::class.java)
    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    suspend fun send(email: String, apiKey: String = firebaseWebApiKey()) {
        val normalizedEmail = normalizePasswordResetEmail(email)
        val request = HttpRequest.newBuilder()
            .uri(
                URI.create(
                    "https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode" +
                            "?key=${urlEncode(apiKey)}",
                ),
            )
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(15))
            .POST(HttpRequest.BodyPublishers.ofString(passwordResetRequestBody(normalizedEmail)))
            .build()
        val response = withContext(Dispatchers.IO) {
            client.send(request, HttpResponse.BodyHandlers.ofString())
        }
        if (response.statusCode() in 200..299) return

        val errorCode = firebaseRestErrorCode(response.body())
        if (isPasswordResetNonDisclosureError(errorCode)) return
        if (isPasswordResetValidationError(errorCode)) {
            throw IllegalArgumentException("Enter a valid email address.")
        }

        log.warn(
            "Firebase password reset request failed: HTTP {} {}",
            response.statusCode(),
            response.body().take(500),
        )
        error("Could not send password reset email")
    }
}

private fun firebaseRestErrorCode(body: String): String? =
    runCatching {
        PasswordResetErrorJson.parseToJsonElement(body)
            .jsonObject
            .jsonObject("error")
            ?.string("message")
            ?.substringBefore(' ')
    }.getOrNull()

private fun JsonObject.jsonObject(key: String): JsonObject? = get(key) as? JsonObject

private fun JsonObject.string(key: String): String? =
    (get(key) as? JsonPrimitive)?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

private fun urlEncode(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")
