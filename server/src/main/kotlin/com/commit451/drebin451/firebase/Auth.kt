package com.commit451.drebin451.firebase

import com.commit451.drebin451.model.User
import com.google.firebase.auth.FirebaseAuthException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext

/** Header scripts can present a full-access API key on. */
const val ApiKeyHeader = "X-API-Key"

/**
 * An authenticated caller resolved from either an API key or Firebase ID token. When [apiKeyUser]
 * is non-null, the caller came from [ApiKeyHeader] and already has a persisted Firestore user.
 */
/**
 * Resolves request credentials with API keys taking precedence over Firebase bearer tokens. Invalid
 * API keys do not fall through to bearer auth: if a client sends [ApiKeyHeader], that credential is
 * authoritative for the request.
 */
internal suspend fun authenticateRequest(
    apiKey: String?,
    authorization: String?,
    resolveApiKey: suspend (String) -> User?,
    verifyFirebaseToken: suspend (String) -> Firebasis.AuthUser,
): AuthenticatedCredential? {
    val presentedApiKey = apiKey?.trim()?.takeIf { it.isNotEmpty() }
    if (presentedApiKey != null) {
        val user = resolveApiKey(presentedApiKey) ?: return null
        return AuthenticatedCredential(auth = user.toAuthUser(), apiKeyUser = user)
    }

    val idToken = authorization
        ?.removePrefix("Bearer ")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: return null

    return try {
        AuthenticatedCredential(auth = verifyFirebaseToken(idToken))
    } catch (_: FirebaseAuthException) {
        null
    }
}

/**
 * Verifies either the `X-API-Key: <token>` header or `Authorization: Bearer <firebase-id-token>`.
 * On success returns the caller; on a missing/invalid credential it responds 401 and returns null,
 * so call sites can early-return: `val user = requireUser() ?: return@get`.
 *
 * API keys are full-access credentials for the owning user: every route guarded by [requireUser]
 * accepts them, while public routes remain unauthenticated.
 */
suspend fun RoutingContext.requireUser(): Firebasis.AuthUser? {
    val credential = authenticatedCredential() ?: run {
        call.respond(HttpStatusCode.Unauthorized)
        return null
    }
    return credential.auth
}

/**
 * Verifies only an `Authorization: Bearer <firebase-id-token>` credential. API keys are intentionally
 * not accepted for account-level destructive actions such as deleting the Firebase Auth account.
 */
suspend fun RoutingContext.requireFirebaseUser(): Firebasis.AuthUser? {
    val auth = authenticateFirebaseRequest(
        authorization = call.request.headers[HttpHeaders.Authorization],
        verifyFirebaseToken = { idToken -> Firebasis.userIdFromAuth(idToken) },
    ) ?: run {
        call.respond(HttpStatusCode.Unauthorized)
        return null
    }
    return auth
}

/**
 * Authenticates an upload by either a full-access API key or Firebase session, and returns the
 * owning persisted [User]. Uploads need the stored display name for app metadata, so Firebase
 * callers are created/loaded here while API-key callers reuse the user resolved by the key.
 */
suspend fun RoutingContext.requireUploader(): User? {
    val credential = authenticatedCredential() ?: run {
        call.respond(HttpStatusCode.Unauthorized)
        return null
    }
    credential.apiKeyUser?.let { return it }

    val auth = credential.auth
    return Firebasis.getOrCreateUser(
        uid = auth.uid,
        email = auth.email,
        displayName = auth.name.ifBlank { auth.email.substringBefore('@') },
    )
}

private suspend fun RoutingContext.authenticatedCredential(): AuthenticatedCredential? =
    authenticateRequest(
        apiKey = call.request.headers[ApiKeyHeader],
        authorization = call.request.headers[HttpHeaders.Authorization],
        resolveApiKey = { token -> Firebasis.userForApiKey(token) },
        verifyFirebaseToken = { idToken -> Firebasis.userIdFromAuth(idToken) },
    )

internal suspend fun authenticateFirebaseRequest(
    authorization: String?,
    verifyFirebaseToken: suspend (String) -> Firebasis.AuthUser,
): Firebasis.AuthUser? {
    val header = authorization?.trim() ?: return null
    val idToken = header
        .takeIf { it.startsWith("Bearer ") }
        ?.removePrefix("Bearer ")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: return null

    return try {
        verifyFirebaseToken(idToken)
    } catch (_: FirebaseAuthException) {
        null
    }
}

private fun User.toAuthUser(): Firebasis.AuthUser =
    Firebasis.AuthUser(uid = uid, email = email, name = displayName)
