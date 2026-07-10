package com.commit451.drebin451.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class B2StorageTest {

    @Test
    fun `config defaults to drebin451 bucket and us-east-005 endpoint`() {
        val config = B2StorageConfig.fromEnv(
            mapOf(
                "B2_KEY_ID" to "key-id",
                "B2_APPLICATION_KEY" to "app-key",
            ),
        )

        assertEquals("key-id", config.keyId)
        assertEquals("app-key", config.applicationKey)
        assertEquals("drebin451", config.bucket)
        assertEquals("https://s3.us-east-005.backblazeb2.com", config.endpoint)
        assertEquals("us-east-005", config.region)
    }

    @Test
    fun `config can be overridden from environment`() {
        val config = B2StorageConfig.fromEnv(
            mapOf(
                "B2_KEY_ID" to "key-id",
                "B2_APPLICATION_KEY" to "app-key",
                "B2_BUCKET" to "custom-bucket",
                "B2_ENDPOINT" to "https://s3.example.com",
                "B2_REGION" to "custom-region",
            ),
        )

        assertEquals("custom-bucket", config.bucket)
        assertEquals("https://s3.example.com", config.endpoint)
        assertEquals("custom-region", config.region)
    }

    @Test
    fun `missing credentials fail with actionable message`() {
        val error = assertFailsWith<IllegalStateException> {
            B2StorageConfig.fromEnv(emptyMap())
        }

        assertEquals(
            "Missing Backblaze B2 credentials: set B2_KEY_ID and B2_APPLICATION_KEY",
            error.message,
        )
    }
}
