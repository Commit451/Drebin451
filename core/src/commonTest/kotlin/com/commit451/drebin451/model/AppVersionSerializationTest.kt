package com.commit451.drebin451.model

import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertFalse

class AppVersionSerializationTest {

    @Test
    fun appVersionJsonDoesNotAdvertiseDirectApkLinksOrStoragePaths() {
        val serializedFieldNames = AppVersion.serializer().descriptor.elementNames.toSet()

        assertFalse(
            "downloadUrl" in serializedFieldNames,
            "Clients should download APKs through the authenticated API, not receive a shareable per-version URL.",
        )

        val encoded = Json.encodeToString(
            AppVersion(
                id = "version-1",
                appId = "owner:com.example.app",
                storagePath = "gs://private-bucket/apks/owner/com.example.app/version-1/app.apk",
            ),
        )

        assertFalse(
            "storagePath" in encoded,
            "The Firebase Storage path must stay server-only.",
        )
        assertFalse(
            "private-bucket" in encoded,
            "Serialized app versions must not leak the APK's backing storage location.",
        )
    }
}
