package com.commit451.drebin451.ui

import com.commit451.drebin451.model.App
import com.commit451.drebin451.model.AppVersion
import com.commit451.drebin451.model.PlanIds
import com.commit451.drebin451.model.PlanLimits
import com.commit451.drebin451.model.storageStatusFor
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeViewModelTest {

    @Test
    fun storageWarning_showsNearLimitWhenNotDismissed() {
        val status = storageStatusFor(
            plan = PlanIds.FREE,
            usedBytes = PlanLimits.FREE_STORAGE_BYTES - 1,
        )

        assertTrue(shouldShowStorageLimitWarning(status, dismissed = false))
    }

    @Test
    fun storageWarning_hidesWhenDismissed() {
        val status = storageStatusFor(
            plan = PlanIds.FREE,
            usedBytes = PlanLimits.FREE_STORAGE_BYTES,
        )

        assertFalse(shouldShowStorageLimitWarning(status, dismissed = true))
    }

    @Test
    fun storageWarning_hidesWhenStorageHasRoom() {
        val status = storageStatusFor(
            plan = PlanIds.FREE,
            usedBytes = 10,
        )

        assertFalse(shouldShowStorageLimitWarning(status, dismissed = false))
    }

    @Test
    fun firstUploadDetection_acceptsBrandNewAppWithFirstVersion() {
        val app = app(createdAt = 1_000, versionCount = 1)
        val version = version(createdAt = 1_000)

        assertTrue(isFirstUploadForNewApp(app, version))
    }

    @Test
    fun firstUploadDetection_rejectsExistingAppWithAnotherVersion() {
        val app = app(createdAt = 1_000, versionCount = 2)
        val version = version(createdAt = 2_000)

        assertFalse(isFirstUploadForNewApp(app, version))
    }

    @Test
    fun firstUploadDetection_rejectsExistingZeroVersionAppThatReceivesAFirstReplacement() {
        val app = app(createdAt = 1_000, versionCount = 1)
        val version = version(createdAt = 2_000)

        assertFalse(isFirstUploadForNewApp(app, version))
    }

    @Test
    fun firstUploadDetection_rejectsVersionForDifferentApp() {
        val app = app(id = "owner:com.example.one", createdAt = 1_000, versionCount = 1)
        val version = version(appId = "owner:com.example.two", createdAt = 1_000)

        assertFalse(isFirstUploadForNewApp(app, version))
    }

    private fun app(
        id: String = "owner:com.example.app",
        createdAt: Long,
        versionCount: Int,
    ) = App(
        id = id,
        applicationId = id.substringAfter(':'),
        ownerUserId = "owner",
        versionCount = versionCount,
        createdAt = createdAt,
        updatedAt = createdAt,
    )

    private fun version(
        appId: String = "owner:com.example.app",
        createdAt: Long,
    ) = AppVersion(
        id = "version",
        appId = appId,
        applicationId = appId.substringAfter(':'),
        ownerUserId = "owner",
        createdAt = createdAt,
        updatedAt = createdAt,
    )
}
