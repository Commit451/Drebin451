package com.commit451.drebin451.apikey

import com.commit451.drebin451.apikey.ApiKeys.TOKEN_PREFIX
import com.commit451.drebin451.apikey.ApiKeys.hash
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * Generation and hashing for API key tokens. Pure JVM crypto ([SecureRandom] + SHA-256) with no
 * external dependencies, kept separate from Firebase so it can be unit-tested in isolation.
 *
 * A token is [TOKEN_PREFIX] followed by 32 bytes of cryptographically secure randomness,
 * base64url-encoded without padding (URL/header-safe). Only its [hash] is ever persisted; the
 * plaintext is returned to the user once at creation. The hash is an unsalted SHA-256: the token
 * already carries 256 bits of entropy (so it is not brute-forceable and needs no per-token salt),
 * and a deterministic hash is what lets an upload look the key up by
 * `whereEqualTo("tokenHash", …)`. This mirrors how GitHub stores personal access tokens.
 */
object ApiKeys {
    /** Identifies a Drebin451 token; aids secret scanners spotting a leaked key. */
    const val TOKEN_PREFIX = "drb_"

    private const val TOKEN_BYTES = 32

    private val secureRandom = SecureRandom()

    data class Generated(
        val token: String,
        val tokenHash: String,
        val maskedToken: String,
    )

    /** Mints a fresh token plus the hash to store and a masked hint to display. */
    fun generate(): Generated {
        val bytes = ByteArray(TOKEN_BYTES).also { secureRandom.nextBytes(it) }
        val token = TOKEN_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        return Generated(token = token, tokenHash = hash(token), maskedToken = mask(token))
    }

    /** SHA-256 of [token] as lowercase hex — deterministic, for storage and lookup. */
    fun hash(token: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }

    /** A display-only hint revealing the prefix and last four chars, e.g. `drb_AbCd…wx12`. */
    private fun mask(token: String): String = "${token.take(8)}…${token.takeLast(4)}"
}
