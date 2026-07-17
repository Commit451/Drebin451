package com.commit451.drebin451.ui

import com.commit451.drebin451.model.AppVersion
import kotlin.test.Test
import kotlin.test.assertEquals

class BatchVersionDeletionClientTest {

    @Test
    fun `fifty selected builds use one backend batch`() {
        val versions = List(50) { AppVersion(id = "version-$it") }

        val batches = versionDeletionBatches(versions)

        assertEquals(1, batches.size)
        assertEquals(50, batches.single().size)
    }

    @Test
    fun `selections over the endpoint limit are split into bounded sequential batches`() {
        val versions = List(250) { AppVersion(id = "version-$it") }

        val batches = versionDeletionBatches(versions)

        assertEquals(listOf(100, 100, 50), batches.map { it.size })
    }
}
