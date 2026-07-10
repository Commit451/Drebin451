package com.commit451.drebin451.firebase

import com.commit451.drebin451.model.App
import com.commit451.drebin451.model.AppVersion
import com.commit451.drebin451.model.PlanIds
import com.commit451.drebin451.model.PlanLimits
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class FirebasisTest {

    @Test
    fun `version cursor uses updatedAt and id`() {
        val version = AppVersion(id = "version-1", createdAt = 1_000, updatedAt = 2_000)

        assertEquals(2_000L to "version-1", Firebasis.run { version.versionCursor() })
    }

    @Test
    fun `reserve storage increments used bytes when upload fits`() {
        assertEquals(
            125,
            storageUsedBytesAfterReserve(
                plan = PlanIds.FREE,
                usedBytes = 100,
                bytes = 25,
            ),
        )
    }

    @Test
    fun `reserve storage allows exact quota fill`() {
        assertEquals(
            PlanLimits.FREE_STORAGE_BYTES,
            storageUsedBytesAfterReserve(
                plan = PlanIds.FREE,
                usedBytes = PlanLimits.FREE_STORAGE_BYTES - 1,
                bytes = 1,
            ),
        )
    }

    @Test
    fun `reserve storage rejects uploads over quota`() {
        assertFailsWith<StorageQuotaExceededException> {
            storageUsedBytesAfterReserve(
                plan = PlanIds.FREE,
                usedBytes = PlanLimits.FREE_STORAGE_BYTES,
                bytes = 1,
            )
        }
    }

    @Test
    fun `release storage subtracts bytes and clamps at zero`() {
        assertEquals(75, storageUsedBytesAfterRelease(usedBytes = 100, bytes = 25))
        assertEquals(0, storageUsedBytesAfterRelease(usedBytes = 100, bytes = 150))
        assertEquals(0, storageUsedBytesAfterRelease(usedBytes = -100, bytes = 1))
    }

    @Test
    fun `recomputed app aggregate reflects remaining versions`() {
        val app = App(
            id = "app-1",
            latestVersionName = "old",
            latestVersionCode = 99,
            versionCount = 3,
            updatedAt = 1_000,
        )
        val remaining = listOf(
            AppVersion(id = "v1", versionName = "1.0", versionCode = 1, updatedAt = 2_000),
            AppVersion(id = "v2", versionName = "2.0", versionCode = 2, updatedAt = 1_500),
        )

        val updated = app.withRecomputedVersionAggregate(remaining)

        assertEquals("2.0", updated.latestVersionName)
        assertEquals(2, updated.latestVersionCode)
        assertEquals(2, updated.versionCount)
        assertEquals(2_000, updated.updatedAt)
    }

    @Test
    fun `recomputed app aggregate clears latest build but keeps timestamp when no versions remain`() {
        val app = App(
            id = "app-1",
            latestVersionName = "old",
            latestVersionCode = 99,
            versionCount = 1,
            updatedAt = 1_000,
        )

        val updated = app.withRecomputedVersionAggregate(emptyList())

        assertEquals("", updated.latestVersionName)
        assertEquals(0, updated.latestVersionCode)
        assertEquals(0, updated.versionCount)
        assertEquals(1_000, updated.updatedAt)
    }

    @Test
    fun `apk storage path keeps uid app version and file name in stable segments`() {
        assertEquals(
            "apks/user-1/com.example.app/version-1/app-release.apk",
            apkStoragePath(
                ownerUserId = "user-1",
                applicationId = "com.example.app",
                versionId = "version-1",
                fileName = "app-release.apk",
            ),
        )
    }

    @Test
    fun `apk storage path parser extracts version id even when file name contains slashes`() {
        val parsed = parseApkStoragePath("apks/user-1/com.example.app/version-1/nested/app.apk")

        assertEquals("user-1", parsed?.ownerUserId)
        assertEquals("com.example.app", parsed?.applicationId)
        assertEquals("version-1", parsed?.versionId)
        assertEquals("nested/app.apk", parsed?.fileName)
    }

    @Test
    fun `managed storage kind identifies malformed apk paths and icons`() {
        assertEquals("apk", managedStorageKind("apks/user-1/com.example.app/version-1/app.apk"))
        assertEquals("malformed-apk", managedStorageKind("apks/user-1/com.example.app"))
        assertEquals("icon", managedStorageKind("icons/user-1/com.example.app/icon"))
        assertEquals("unknown", managedStorageKind("tmp/object"))
        assertNull(parseApkStoragePath("apks/user-1/com.example.app"))
    }
}
