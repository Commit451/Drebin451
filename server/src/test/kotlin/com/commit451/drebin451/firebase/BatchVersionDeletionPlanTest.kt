package com.commit451.drebin451.firebase

import com.commit451.drebin451.model.AppVersion
import kotlin.test.Test
import kotlin.test.assertEquals

class BatchVersionDeletionPlanTest {

    @Test
    fun `batch deletion plan separates deleted missing and remaining versions`() {
        val versions = listOf(
            AppVersion(id = "version-1", fileSizeBytes = 10),
            AppVersion(id = "version-2", fileSizeBytes = 20),
            AppVersion(id = "version-3", fileSizeBytes = 30),
        )

        val plan = batchVersionDeletionPlan(
            versions = versions,
            requestedVersionIds = linkedSetOf("version-2", "missing", "version-1"),
        )

        assertEquals(listOf("version-1", "version-2"), plan.deletedVersions.map { it.id })
        assertEquals(listOf("missing"), plan.missingVersionIds)
        assertEquals(listOf("version-3"), plan.remainingVersions.map { it.id })
        assertEquals(30, plan.releasedStorageBytes)
    }

    @Test
    fun `batch deletion plan is a no-op when every requested id is missing`() {
        val versions = listOf(AppVersion(id = "version-1", fileSizeBytes = 10))

        val plan = batchVersionDeletionPlan(
            versions = versions,
            requestedVersionIds = linkedSetOf("missing"),
        )

        assertEquals(emptyList(), plan.deletedVersions)
        assertEquals(listOf("missing"), plan.missingVersionIds)
        assertEquals(listOf("version-1"), plan.remainingVersions.map { it.id })
        assertEquals(0, plan.releasedStorageBytes)
    }
}
