package com.commit451.drebin451.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * A long-lived credential a user mints to call the HTTP API from outside the app (CI, scripts).
 * Sent via the `X-API-Key` header and treated like the owning user's full-access credential.
 *
 * Persisted in Firestore (collection `apiKeys`, keyed by [id]) and returned to the owner from
 * `GET /v1/api-keys`. Only the SHA-256 [tokenHash] is stored — never the plaintext token, which
 * is shown exactly once at creation (see [ApiKeyCreated]). [tokenHash] is [Transient] so Firestore
 * (which uses its own mapping, not kotlinx.serialization) persists and can query it, while it is
 * never leaked over the JSON API — mirroring [AppVersion.storagePath]. [maskedToken] is a
 * display-only hint (e.g. `drb_AbCd…wx12`). All fields default for Firestore's `toObject`.
 */
@Serializable
data class ApiKey(
    val id: String = "",
    val ownerUserId: String = "",
    val label: String = "",
    val maskedToken: String = "",
    val createdAt: Long = 0,
    // 0 = never used; bumped (best-effort) on each successful API-key-authenticated request.
    val lastUsedAt: Long = 0,
    @Transient val tokenHash: String = "",
)
