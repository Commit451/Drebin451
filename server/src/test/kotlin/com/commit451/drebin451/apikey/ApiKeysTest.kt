package com.commit451.drebin451.apikey

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ApiKeysTest {

    @Test
    fun `generated token carries the drb prefix`() {
        assertTrue(ApiKeys.generate().token.startsWith(ApiKeys.TOKEN_PREFIX))
    }

    @Test
    fun `stored hash matches a fresh hash of the token`() {
        // The server looks a key up by re-hashing the presented token, so generate()'s stored
        // hash must equal hash(token) — otherwise no uploaded key would ever resolve.
        val generated = ApiKeys.generate()
        assertEquals(generated.tokenHash, ApiKeys.hash(generated.token))
    }

    @Test
    fun `hash is deterministic and 64 hex chars`() {
        val token = ApiKeys.generate().token
        assertEquals(ApiKeys.hash(token), ApiKeys.hash(token))
        assertTrue(ApiKeys.hash(token).matches(Regex("[0-9a-f]{64}")))
    }

    @Test
    fun `each generated token and hash is unique`() {
        val a = ApiKeys.generate()
        val b = ApiKeys.generate()
        assertNotEquals(a.token, b.token)
        assertNotEquals(a.tokenHash, b.tokenHash)
    }

    @Test
    fun `masked token reveals only the prefix and last four chars`() {
        val generated = ApiKeys.generate()
        assertTrue(generated.maskedToken.startsWith(ApiKeys.TOKEN_PREFIX))
        assertTrue(generated.maskedToken.contains("…"))
        assertTrue(generated.maskedToken.endsWith(generated.token.takeLast(4)))
        // The full secret must never be embedded in the display hint.
        assertNotEquals(generated.token, generated.maskedToken)
    }
}
