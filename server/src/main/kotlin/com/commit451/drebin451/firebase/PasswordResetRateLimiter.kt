package com.commit451.drebin451.firebase

import java.util.Locale

private const val PasswordResetEmailLimit = 3
private const val PasswordResetClientLimit = 20
private const val PasswordResetMaxTrackedKeys = 10_000
private const val PasswordResetWindowMillis = 60 * 60 * 1000L

internal class PasswordResetRateLimiter(
    private val emailLimit: Int = PasswordResetEmailLimit,
    private val clientLimit: Int = PasswordResetClientLimit,
    private val maxTrackedKeys: Int = PasswordResetMaxTrackedKeys,
    private val windowMillis: Long = PasswordResetWindowMillis,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) {
    private val lock = Any()
    private val attempts = mutableMapOf<String, MutableList<Long>>()

    init {
        require(emailLimit > 0) { "emailLimit must be positive" }
        require(clientLimit > 0) { "clientLimit must be positive" }
        require(maxTrackedKeys >= 2) { "maxTrackedKeys must allow email and client buckets" }
        require(windowMillis > 0) { "windowMillis must be positive" }
    }

    fun tryAcquire(email: String, clientKey: String?): Boolean {
        val normalizedEmail = normalizePasswordResetEmail(email).lowercase(Locale.US)
        val keys = buildList {
            add("email:$normalizedEmail")
            clientKey
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { add("client:$it") }
        }
        val now = nowMillis()
        return synchronized(lock) {
            pruneExpired(now)
            evictOldestKeysIfNeeded(keys)
            keys.forEach { key -> attempts.getOrPut(key) { mutableListOf() } }

            val emailAllowed = attempts.getValue(keys.first()).size < emailLimit
            val clientAllowed = keys.drop(1).all { attempts.getValue(it).size < clientLimit }
            val allowed = emailAllowed && clientAllowed
            if (allowed) {
                keys.forEach { attempts.getValue(it).add(now) }
            }
            allowed
        }
    }

    internal fun trackedKeyCount(): Int = synchronized(lock) { attempts.size }

    private fun pruneExpired(now: Long) {
        attempts.values.forEach { entries ->
            entries.removeAll { attempt -> now - attempt >= windowMillis }
        }
        attempts.entries.removeAll { it.value.isEmpty() }
    }

    private fun evictOldestKeysIfNeeded(requiredKeys: List<String>) {
        val required = requiredKeys.toSet()
        while (attempts.size + requiredKeys.count { it !in attempts } > maxTrackedKeys) {
            val oldest = attempts.entries
                .filter { it.key !in required }
                .minByOrNull { it.value.firstOrNull() ?: Long.MAX_VALUE }
                ?: return
            attempts.remove(oldest.key)
        }
    }
}
