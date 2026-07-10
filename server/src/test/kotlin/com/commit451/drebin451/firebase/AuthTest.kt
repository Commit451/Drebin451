package com.commit451.drebin451.firebase

import com.commit451.drebin451.model.User
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class AuthTest {

    @Test
    fun `API key resolves into a full user credential`() = runBlocking {
        val owner = User(uid = "user-1", email = "owner@example.com", displayName = "Owner")

        val credential = authenticateRequest(
            apiKey = " drb_test ",
            authorization = null,
            resolveApiKey = { token ->
                assertEquals("drb_test", token)
                owner
            },
            verifyFirebaseToken = { error("Firebase token should not be checked when an API key is present") },
        )

        assertEquals(
            Firebasis.AuthUser(
                uid = "user-1",
                email = "owner@example.com",
                name = "Owner"
            ), credential?.auth
        )
        assertEquals(owner, credential?.apiKeyUser)
    }

    @Test
    fun `blank API key falls back to bearer token`() = runBlocking {
        val credential = authenticateRequest(
            apiKey = "   ",
            authorization = "Bearer firebase-token",
            resolveApiKey = { error("Blank API key should be ignored") },
            verifyFirebaseToken = { token ->
                assertEquals("firebase-token", token)
                Firebasis.AuthUser(
                    uid = "firebase-user",
                    email = "firebase@example.com",
                    name = "Firebase User"
                )
            },
        )

        assertEquals(
            Firebasis.AuthUser(
                uid = "firebase-user",
                email = "firebase@example.com",
                name = "Firebase User"
            ), credential?.auth
        )
        assertNull(credential?.apiKeyUser)
    }

    @Test
    fun `invalid API key does not fall through to bearer token`() = runBlocking {
        var firebaseChecked = false

        val credential = authenticateRequest(
            apiKey = "drb_bad",
            authorization = "Bearer firebase-token",
            resolveApiKey = { null },
            verifyFirebaseToken = {
                firebaseChecked = true
                Firebasis.AuthUser(
                    uid = "firebase-user",
                    email = "firebase@example.com",
                    name = "Firebase User"
                )
            },
        )

        assertNull(credential)
        assertFalse(firebaseChecked)
    }

    @Test
    fun `firebase-only account deletion auth trims bearer token`() = runBlocking {
        val auth = authenticateFirebaseRequest(
            authorization = "Bearer firebase-token  ",
            verifyFirebaseToken = { token ->
                assertEquals("firebase-token", token)
                Firebasis.AuthUser(
                    uid = "firebase-user",
                    email = "firebase@example.com",
                    name = "Firebase User"
                )
            },
        )

        assertEquals(
            Firebasis.AuthUser(
                uid = "firebase-user",
                email = "firebase@example.com",
                name = "Firebase User"
            ), auth
        )
    }

    @Test
    fun `firebase-only account deletion auth rejects missing bearer token`() = runBlocking {
        val auth = authenticateFirebaseRequest(
            authorization = null,
            verifyFirebaseToken = { error("Missing bearer token should not be verified") },
        )

        assertNull(auth)
    }

    @Test
    fun `firebase-only account deletion auth rejects non-bearer authorization`() = runBlocking {
        val auth = authenticateFirebaseRequest(
            authorization = "drb_test",
            verifyFirebaseToken = { error("Non-bearer credentials should not be verified") },
        )

        assertNull(auth)
    }
}
