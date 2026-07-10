package com.commit451.drebin451.firebase

import com.commit451.drebin451.model.User

/**
 * An authenticated caller resolved from either an API key or Firebase ID token. When [apiKeyUser]
 * is non-null, the caller came from [ApiKeyHeader] and already has a persisted Firestore user.
 */
internal data class AuthenticatedCredential(
    val auth: Firebasis.AuthUser,
    val apiKeyUser: User? = null,
)
