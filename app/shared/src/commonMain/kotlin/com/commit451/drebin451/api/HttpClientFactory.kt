package com.commit451.drebin451.api

import com.commit451.drebin451.auth.firebaseIdToken
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

/**
 * Builds the configured Ktor [HttpClient]. Only the engine is platform-specific — CIO on Android,
 * the browser fetch engine ([io.ktor.client.engine.js.Js]) on web — while [configureClient] adds
 * the shared JSON content negotiation, generous timeouts (APK uploads can be slow), and Firebase
 * bearer-token auth.
 */
expect fun createHttpClient(): HttpClient

internal fun HttpClientConfig<*>.configureClient() {
    install(HttpTimeout) {
        requestTimeoutMillis = 120.seconds.inWholeMilliseconds
        connectTimeoutMillis = 30.seconds.inWholeMilliseconds
        socketTimeoutMillis = 120.seconds.inWholeMilliseconds
    }
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
    install(Auth) {
        bearer {
            loadTokens {
                firebaseIdToken(forceRefresh = false)?.let { BearerTokens(it, "") }
            }
            refreshTokens {
                firebaseIdToken(forceRefresh = true)?.let { BearerTokens(it, "") }
            }
            // Attach the token proactively — every call goes to our own backend.
            sendWithoutRequest { true }
        }
    }
}
