package com.commit451.drebin451

import com.commit451.drebin451.model.BatchDeleteVersionsRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BatchDeleteVersionsTest {

    @Test
    fun `batch version ids are trimmed and deduplicated in request order`() {
        assertEquals(
            listOf("version-1", "version-2"),
            normalizedBatchVersionIds(listOf(" version-1 ", "version-2", "version-1")),
        )
    }

    @Test
    fun `batch version ids reject an empty request`() {
        assertFailsWith<IllegalArgumentException> {
            normalizedBatchVersionIds(emptyList())
        }
    }

    @Test
    fun `batch version ids reject blank ids`() {
        assertFailsWith<IllegalArgumentException> {
            normalizedBatchVersionIds(listOf("version-1", "  "))
        }
    }

    @Test
    fun `batch version ids enforce the server request limit`() {
        assertFailsWith<IllegalArgumentException> {
            normalizedBatchVersionIds(
                List(BatchDeleteVersionsRequest.MAX_VERSION_IDS + 1) { "version-$it" }
            )
        }
    }
}
