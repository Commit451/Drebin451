package com.commit451.drebin451.firebase

import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FirebaseServiceAccountTest {

    @Test
    fun `base64 environment value takes precedence`() {
        val expected = "{\"project_id\":\"drebin451\"}".toByteArray()
        val env = mapOf(
            FirebaseServiceAccountBase64Env to Base64.getEncoder().encodeToString(expected),
            FirebaseServiceAccountRawEnv to "wrong",
            FirebaseServiceAccountPathEnv to "/wrong/path.json",
        )

        val actual = firebaseServiceAccountJson(env) { error("file fallback must not be read") }

        assertContentEquals(expected, actual)
    }

    @Test
    fun `raw JSON environment value remains a local fallback`() {
        val expected = "{\"type\":\"service_account\"}".toByteArray()

        val actual = firebaseServiceAccountJson(
            env = mapOf(FirebaseServiceAccountRawEnv to expected.decodeToString()),
            readFile = { error("file fallback must not be read") },
        )

        assertContentEquals(expected, actual)
    }

    @Test
    fun `explicit local file path is supported`() {
        val expected = "local-json".toByteArray()
        val requestedPaths = mutableListOf<String>()

        val actual = firebaseServiceAccountJson(
            env = mapOf(FirebaseServiceAccountPathEnv to "/tmp/firebase-admin.json"),
            readFile = { path ->
                requestedPaths += path
                expected
            },
        )

        assertContentEquals(expected, actual)
        assertContentEquals(listOf("/tmp/firebase-admin.json"), requestedPaths)
    }

    @Test
    fun `invalid base64 fails without falling back to a file`() {
        val error = assertFailsWith<IllegalStateException> {
            firebaseServiceAccountJson(
                env = mapOf(FirebaseServiceAccountBase64Env to "not-base64!"),
                readFile = { error("invalid configured base64 must not fall back") },
            )
        }

        assertTrue(error.message.orEmpty().contains(FirebaseServiceAccountBase64Env))
    }

    @Test
    fun `missing credentials report every supported local fallback`() {
        val error = assertFailsWith<IllegalStateException> {
            firebaseServiceAccountJson(emptyMap()) { throw java.io.FileNotFoundException(it) }
        }

        val message = error.message.orEmpty()
        assertTrue(message.contains(FirebaseServiceAccountBase64Env))
        assertTrue(message.contains(FirebaseServiceAccountRawEnv))
        assertTrue(message.contains(FirebaseServiceAccountPathEnv))
        assertTrue(message.contains(DefaultFirebaseServiceAccountPath))
    }
}
