package com.commit451.drebin451.model

import kotlinx.serialization.Serializable

/**
 * The response to creating an [ApiKey]. Carries the full plaintext [token] — the *only* time it
 * is ever returned — alongside the stored [apiKey] metadata. The client must surface [token] to
 * the user immediately; it cannot be recovered afterwards.
 */
@Serializable
data class ApiKeyCreated(
    val apiKey: ApiKey = ApiKey(),
    val token: String = "",
)
