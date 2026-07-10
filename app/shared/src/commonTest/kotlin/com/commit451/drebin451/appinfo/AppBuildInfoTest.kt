package com.commit451.drebin451.appinfo

import kotlin.test.Test
import kotlin.test.assertEquals

class AppBuildInfoTest {

    @Test
    fun version_label_includes_version_code() {
        assertEquals(
            "1.2.3 (45)",
            AppBuildInfo(versionName = "1.2.3", versionCode = 45, commitHash = "abc123").versionLabel,
        )
    }

    @Test
    fun version_label_omits_non_positive_version_code() {
        assertEquals(
            "1.2.3",
            AppBuildInfo(versionName = "1.2.3", versionCode = 0, commitHash = "abc123").versionLabel,
        )
    }

    @Test
    fun blank_values_display_unknown() {
        val info = AppBuildInfo(versionName = " ", versionCode = 0, commitHash = " ")

        assertEquals("unknown", info.versionLabel)
        assertEquals("unknown", info.commitHashLabel)
    }
}
