package com.commit451.drebin451

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class AppIconUrlTest {

    @Test
    fun `icon URL includes app id and first icon upload UUID`() {
        assertEquals(
            "https://api.example.com/v1/apps/user-1:com.example.app/icon?v=version-1",
            appIconUrl(
                publicBaseUrl = "https://api.example.com/",
                prefix = "v1",
                appId = "user-1:com.example.app",
                cacheKey = "version-1",
            ),
        )
    }

    @Test
    fun `recreated app gets a different icon cache key`() {
        val firstUrl = appIconUrl(
            publicBaseUrl = "https://api.example.com",
            prefix = "v1",
            appId = "user-1:com.example.app",
            cacheKey = "version-1",
        )
        val recreatedUrl = appIconUrl(
            publicBaseUrl = "https://api.example.com",
            prefix = "v1",
            appId = "user-1:com.example.app",
            cacheKey = "version-2",
        )

        assertNotEquals(firstUrl, recreatedUrl)
    }
}
