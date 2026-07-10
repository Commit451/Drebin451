package com.commit451.drebin451.firebase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PasswordResetTest {

    @Test
    fun `password reset email is trimmed before sending`() {
        assertEquals("jawn@example.com", normalizePasswordResetEmail("  jawn@example.com  "))
    }

    @Test
    fun `blank password reset email is rejected`() {
        val error = assertFailsWith<IllegalArgumentException> {
            normalizePasswordResetEmail("   ")
        }

        assertEquals("Enter an email address.", error.message)
    }

    @Test
    fun `overlong password reset email is rejected before normalization`() {
        val error = assertFailsWith<IllegalArgumentException> {
            normalizePasswordResetEmail("a".repeat(MaxPasswordResetEmailLength + 1))
        }

        assertEquals("Enter a valid email address.", error.message)
    }

    @Test
    fun `password reset request posts Firebase send oob code payload`() {
        assertEquals(
            "{\"requestType\":\"PASSWORD_RESET\",\"email\":\"jawn@example.com\"}",
            passwordResetRequestBody("jawn@example.com"),
        )
    }

    @Test
    fun `password reset hides account existence errors`() {
        assertTrue(isPasswordResetNonDisclosureError("EMAIL_NOT_FOUND"))
        assertFalse(isPasswordResetNonDisclosureError("INVALID_EMAIL"))
        assertFalse(isPasswordResetNonDisclosureError("INVALID_API_KEY"))
    }

    @Test
    fun `password reset treats malformed email errors as validation errors`() {
        assertTrue(isPasswordResetValidationError("INVALID_EMAIL"))
        assertTrue(isPasswordResetValidationError("MISSING_EMAIL"))
        assertFalse(isPasswordResetValidationError("INVALID_API_KEY"))
    }

    @Test
    fun `password reset rate limiter throttles repeated emails`() {
        var now = 1_000L
        val limiter = PasswordResetRateLimiter(
            emailLimit = 2,
            clientLimit = 100,
            windowMillis = 1_000L,
            nowMillis = { now },
        )

        assertTrue(limiter.tryAcquire("JAWN@example.com", clientKey = "client-a"))
        assertTrue(limiter.tryAcquire(" jawn@example.com ", clientKey = "client-a"))
        assertFalse(limiter.tryAcquire("jawn@example.com", clientKey = "client-a"))

        now += 1_000L
        assertTrue(limiter.tryAcquire("jawn@example.com", clientKey = "client-a"))
    }

    @Test
    fun `password reset rate limiter throttles repeated clients across emails`() {
        val limiter = PasswordResetRateLimiter(
            emailLimit = 100,
            clientLimit = 2,
            windowMillis = 1_000L,
            nowMillis = { 1_000L },
        )

        assertTrue(limiter.tryAcquire("one@example.com", clientKey = "client-a"))
        assertTrue(limiter.tryAcquire("two@example.com", clientKey = "client-a"))
        assertFalse(limiter.tryAcquire("three@example.com", clientKey = "client-a"))
        assertTrue(limiter.tryAcquire("three@example.com", clientKey = "client-b"))
    }

    @Test
    fun `password reset rate limiter prunes stale buckets globally`() {
        var now = 1_000L
        val limiter = PasswordResetRateLimiter(
            emailLimit = 2,
            clientLimit = 2,
            windowMillis = 1_000L,
            nowMillis = { now },
        )

        assertTrue(limiter.tryAcquire("one@example.com", clientKey = "client-a"))
        assertTrue(limiter.tryAcquire("two@example.com", clientKey = "client-b"))
        assertEquals(4, limiter.trackedKeyCount())

        now += 1_000L
        assertTrue(limiter.tryAcquire("three@example.com", clientKey = "client-c"))
        assertEquals(2, limiter.trackedKeyCount())
    }

    @Test
    fun `password reset rate limiter bounds tracked keys`() {
        val limiter = PasswordResetRateLimiter(
            emailLimit = 100,
            clientLimit = 100,
            maxTrackedKeys = 4,
            windowMillis = 1_000L,
            nowMillis = { 1_000L },
        )

        assertTrue(limiter.tryAcquire("one@example.com", clientKey = "client-a"))
        assertTrue(limiter.tryAcquire("two@example.com", clientKey = "client-b"))
        assertEquals(4, limiter.trackedKeyCount())

        assertTrue(limiter.tryAcquire("three@example.com", clientKey = "client-c"))
        assertEquals(4, limiter.trackedKeyCount())
    }

    @Test
    fun `password reset uses the canonical Firebase web API key environment value`() {
        assertEquals(
            "canonical-key",
            firebaseWebApiKey(
                mapOf(
                    "DREBIN451_FIREBASE_WEB_API_KEY" to " canonical-key ",
                    "FIREBASE_WEB_API_KEY" to "legacy-key",
                ),
            ),
        )
    }

    @Test
    fun `password reset keeps the legacy Firebase web API key environment fallback`() {
        assertEquals(
            "legacy-key",
            firebaseWebApiKey(mapOf("FIREBASE_WEB_API_KEY" to " legacy-key ")),
        )
    }

    @Test
    fun `password reset rejects a missing Firebase web API key`() {
        val error = assertFailsWith<IllegalStateException> {
            firebaseWebApiKey(emptyMap())
        }

        assertTrue(error.message.orEmpty().contains("DREBIN451_FIREBASE_WEB_API_KEY"))
    }
}
