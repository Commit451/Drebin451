package com.commit451.drebin451.api

import com.commit451.drebin451.model.ApiKey
import com.commit451.drebin451.model.ApiKeyCreated
import com.commit451.drebin451.model.App
import com.commit451.drebin451.model.AppVersion
import com.commit451.drebin451.model.BillingSession
import com.commit451.drebin451.model.Config
import com.commit451.drebin451.model.CreateApiKeyRequest
import com.commit451.drebin451.model.PaginatedResponse
import com.commit451.drebin451.model.PasswordResetRequest
import com.commit451.drebin451.model.StorageStatus
import com.commit451.drebin451.model.User
import com.commit451.drebin451.model.VersionNote
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

private val errorJson = Json { ignoreUnknownKeys = true }

internal fun httpErrorMessage(status: HttpStatusCode, body: String): String {
    val fallback = body.trim().ifBlank { "HTTP error ${status.value}" }
    return runCatching { errorJson.decodeFromString<ApiErrorResponse>(body).errorMessage.trim() }
        .getOrNull()
        ?.takeIf { it.isNotBlank() }
        ?: fallback
}

/**
 * The single client entry point to the Ktor backend. The Firebase bearer token is
 * attached automatically by the configured [HttpClient] (see createHttpClient()).
 */
object Api {
    const val PAGE_SIZE = 10

    /**
     * Backend base URL. Defaults to the Android-emulator host loopback; point this at a
     * device-reachable host or the deployed server as needed.
     */
    var baseUrl: String = "https://api.drebin451.com/v1"

    // Lazy so the configured client is only built on first use.
    private val client by lazy { createHttpClient() }

    suspend fun user(): User =
        client.get("$baseUrl/user").bodyOrThrow()

    suspend fun storageStatus(): StorageStatus =
        client.get("$baseUrl/user/storage").bodyOrThrow()

    /** Permanently deletes the signed-in account and all owned server-side data. */
    suspend fun deleteAccount() {
        client.delete("$baseUrl/user").throwIfError()
    }

    suspend fun refreshSubscription(): User =
        client.post("$baseUrl/billing/refresh").bodyOrThrow()

    suspend fun createBillingCheckoutSession(): BillingSession =
        client.post("$baseUrl/billing/checkout").bodyOrThrow()

    suspend fun createBillingPortalSession(): BillingSession =
        client.post("$baseUrl/billing/portal").bodyOrThrow()

    /** App-wide config (kill-switch + maintenance message), read on splash. Unauthenticated. */
    suspend fun config(): Config =
        client.get("$baseUrl/config").bodyOrThrow()

    /** Requests a Firebase password-reset email through the unauthenticated backend endpoint. */
    suspend fun requestPasswordReset(email: String) {
        client.post("$baseUrl/auth/password-reset") {
            contentType(ContentType.Application.Json)
            setBody(PasswordResetRequest(email))
        }.throwIfError()
    }

    /** The caller's own apps — one per applicationId, newest-updated first, 10 per page by default. */
    suspend fun apps(pageToken: String? = null, pageSize: Int = PAGE_SIZE): PaginatedResponse<App> =
        client.get("$baseUrl/apps") {
            parameter("pageSize", pageSize)
            pageToken?.let { parameter("pageToken", it) }
        }.bodyOrThrow()

    /** Apps the caller added by opening a public share URL. */
    suspend fun sharedApps(
        pageToken: String? = null,
        pageSize: Int = PAGE_SIZE
    ): PaginatedResponse<App> =
        client.get("$baseUrl/shared-apps") {
            parameter("pageSize", pageSize)
            pageToken?.let { parameter("pageToken", it) }
        }.bodyOrThrow()

    suspend fun app(id: String): App =
        client.get("$baseUrl/apps/$id").bodyOrThrow()

    /** Replaces an owned app's public share id and returns the updated app. */
    suspend fun refreshAppShareId(appId: String): App =
        client.post("$baseUrl/apps/$appId/share-id/refresh").bodyOrThrow()

    /** All uploaded versions of an app, newest-updated first, 10 per page by default. */
    suspend fun appVersions(
        appId: String,
        pageToken: String? = null,
        pageSize: Int = PAGE_SIZE,
    ): PaginatedResponse<AppVersion> =
        client.get("$baseUrl/apps/$appId/versions") {
            parameter("pageSize", pageSize)
            pageToken?.let { parameter("pageToken", it) }
        }.bodyOrThrow()

    /** Single uploaded release/version detail. */
    suspend fun appVersion(appId: String, versionId: String): AppVersion =
        client.get("$baseUrl/apps/$appId/versions/$versionId").bodyOrThrow()

    /**
     * Uploads an APK. The server reads its applicationId/version/label from the bytes and
     * either creates a new app or appends a version to the existing one, returning the
     * created [AppVersion].
     */
    suspend fun uploadApp(fileName: String, bytes: ByteArray): AppVersion {
        val response = client.post("$baseUrl/apps") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append(
                            key = "apk",
                            value = bytes,
                            headers = Headers.build {
                                append(HttpHeaders.ContentType, AppVersion.CONTENT_TYPE_APK)
                                append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                            },
                        )
                    },
                ),
            )
        }
        return response.bodyOrThrow()
    }

    /** Deletes an app and every version under it. */
    suspend fun deleteApp(id: String) {
        client.delete("$baseUrl/apps/$id").throwIfError()
    }

    /** Deletes a single uploaded version (APK). */
    suspend fun deleteVersion(appId: String, versionId: String) {
        client.delete("$baseUrl/apps/$appId/versions/$versionId").throwIfError()
    }

    /** Edits the note attached to a single uploaded APK version, returning the updated version. */
    suspend fun updateVersionNote(appId: String, versionId: String, note: String): AppVersion =
        client.patch("$baseUrl/apps/$appId/versions/$versionId/note") {
            contentType(ContentType.Application.Json)
            setBody(VersionNote(note))
        }.bodyOrThrow()

    /** Clears the note attached to a single uploaded APK version without deleting the APK. */
    suspend fun deleteVersionNote(appId: String, versionId: String) {
        client.delete("$baseUrl/apps/$appId/versions/$versionId/note").throwIfError()
    }

    /** Downloads APK bytes through the authenticated API; this URL is not exposed as a share link. */
    suspend fun downloadVersion(appId: String, versionId: String): ByteArray =
        client.get("$baseUrl/apps/$appId/versions/$versionId/download").bodyOrThrow()

    /**
     * Opens a public app share URL: adds the app to the caller's Shared collection (unless the
     * caller owns it already) and returns the app to display.
     */
    suspend fun addSharedApp(shareId: String): App =
        client.post("$baseUrl/app-shares/$shareId").bodyOrThrow()

    /** Removes an app from the caller's Shared collection without affecting the owner's app. */
    suspend fun deleteSharedApp(appId: String) {
        client.delete("$baseUrl/shared-apps/$appId").throwIfError()
    }

    /** The caller's API keys (metadata only — tokens are never returned by this call). */
    suspend fun apiKeys(): List<ApiKey> =
        client.get("$baseUrl/api-keys").bodyOrThrow()

    /**
     * Mints an API key. [ApiKeyCreated.token] is the plaintext token — shown to the user once and
     * unrecoverable afterwards; only its masked hint is available later.
     */
    suspend fun createApiKey(label: String): ApiKeyCreated =
        client.post("$baseUrl/api-keys") {
            contentType(ContentType.Application.Json)
            setBody(CreateApiKeyRequest(label))
        }.bodyOrThrow()

    /** Revokes an API key. */
    suspend fun deleteApiKey(id: String) {
        client.delete("$baseUrl/api-keys/$id").throwIfError()
    }

    private suspend inline fun <reified T> HttpResponse.bodyOrThrow(): T =
        if (status.isSuccess()) body() else throw HttpException(
            statusCode = status,
            message = httpErrorMessage(status, bodyAsText()),
        )

    /** For requests with no response body to decode — throws [HttpException] on a non-2xx status. */
    private suspend fun HttpResponse.throwIfError() {
        if (!status.isSuccess()) {
            throw HttpException(
                statusCode = status,
                message = httpErrorMessage(status, bodyAsText()),
            )
        }
    }
}
