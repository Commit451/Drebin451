package com.commit451.drebin451.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StorageStatusTest {

    @Test
    fun storageStatus_reportsNearLimitInsideThreshold() {
        val status = storageStatusFor(
            plan = PlanIds.FREE,
            usedBytes = PlanLimits.FREE_STORAGE_BYTES - PlanLimits.STORAGE_WARNING_THRESHOLD_BYTES,
        )

        assertTrue(status.nearLimit)
        assertFalse(status.atLimit)
        assertEquals(PlanLimits.STORAGE_WARNING_THRESHOLD_BYTES, status.remainingBytes)
    }

    @Test
    fun storageStatus_reportsAtLimitWhenUsedMeetsQuota() {
        val status = storageStatusFor(
            plan = PlanIds.FREE,
            usedBytes = PlanLimits.FREE_STORAGE_BYTES,
        )

        assertFalse(status.nearLimit)
        assertTrue(status.atLimit)
        assertEquals(0, status.remainingBytes)
    }

    @Test
    fun storageStatus_usesProQuotaForProUsers() {
        val status = storageStatusFor(
            plan = PlanIds.PRO,
            usedBytes = PlanLimits.FREE_STORAGE_BYTES,
        )

        assertEquals(PlanIds.PRO, status.plan)
        assertEquals(PlanLimits.PRO_STORAGE_BYTES, status.limitBytes)
        assertFalse(status.nearLimit)
        assertFalse(status.atLimit)
    }
}
