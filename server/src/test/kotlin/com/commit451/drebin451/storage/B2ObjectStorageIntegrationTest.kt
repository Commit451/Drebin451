package com.commit451.drebin451.storage

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class B2ObjectStorageIntegrationTest {

    @Test
    fun `round trips object through Backblaze B2 when explicitly enabled`() = runBlocking {
        if (System.getenv("RUN_B2_INTEGRATION") != "true") return@runBlocking

        val config = B2StorageConfig.fromEnv()
        val storage = B2ObjectStorage(config)
        val path = "test/hermes-${System.currentTimeMillis()}.txt"
        val bytes = "hello from drebin451".encodeToByteArray()

        storage.put(path, bytes, "text/plain")
        val stored = storage.get(path)
        assertContentEquals(bytes, stored?.bytes)
        assertEquals("text/plain", stored?.contentType)

        storage.delete(path)
        assertNull(storage.get(path))
    }
}
