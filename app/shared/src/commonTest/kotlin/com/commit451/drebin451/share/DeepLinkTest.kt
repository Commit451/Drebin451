package com.commit451.drebin451.share

import com.commit451.drebin451.push.PushData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeepLinkTest {

    @Test
    fun parses_app_share_link() {
        assertEquals(
            ShareTarget.App("share-token-123"),
            DeepLink.parse("https://drebin451.com/app/share-token-123"),
        )
    }

    @Test
    fun tolerates_trailing_slash_and_query() {
        assertEquals(ShareTarget.App("abc"), DeepLink.parse("https://drebin451.com/app/abc/"))
        assertEquals(ShareTarget.App("abc"), DeepLink.parse("https://drebin451.com/app/abc?x=1"))
    }

    @Test
    fun parses_release_share_link() {
        assertEquals(
            ShareTarget.Release(shareId = "share-token-123", versionId = "version-1"),
            DeepLink.parse("https://drebin451.com/app/share-token-123/releases/version-1"),
        )
    }

    @Test
    fun rejects_wrong_host() {
        assertNull(DeepLink.parse("https://evil.com/app/abc"))
    }

    @Test
    fun rejects_non_app_path() {
        assertNull(DeepLink.parse("https://drebin451.com/settings/abc"))
    }

    @Test
    fun rejects_app_link_without_id_or_with_unknown_extra_segments() {
        assertNull(DeepLink.parse("https://drebin451.com/app/"))
        assertNull(DeepLink.parse("https://drebin451.com/app"))
        assertNull(DeepLink.parse("https://drebin451.com/app/abc/version-1"))
        assertNull(DeepLink.parse("https://drebin451.com/app/abc/releases"))
        assertNull(DeepLink.parse("https://drebin451.com/app/abc/releases/"))
    }

    @Test
    fun rejects_null_blank_and_schemeless() {
        assertNull(DeepLink.parse(null))
        assertNull(DeepLink.parse(""))
        assertNull(DeepLink.parse("drebin451.com/app/abc"))
    }

    @Test
    fun parses_private_push_app_version_deep_link() {
        assertEquals(
            AppVersionDeepLinkTarget(appId = "owner:com.example.app", versionId = "version-1"),
            DeepLink.parse(PushData.appVersionDeepLink("owner:com.example.app", "version-1")),
        )
    }

    @Test
    fun private_push_app_version_deep_link_tolerates_trailing_slash_and_query() {
        assertEquals(
            AppVersionDeepLinkTarget(appId = "owner:com.example.app", versionId = "version-1"),
            DeepLink.parse("drebin451://app/owner:com.example.app/version-1/?from=fcm"),
        )
    }

    @Test
    fun rejects_malformed_private_push_deep_links() {
        assertNull(DeepLink.parse("drebin451://app/owner:com.example.app"))
        assertNull(DeepLink.parse("drebin451://wrong/owner:com.example.app/version-1"))
        assertNull(DeepLink.parse("https://drebin451.com/app/owner:com.example.app/version-1"))
    }

    // --- parsePath: host-agnostic, used on web with window.location.pathname ---

    @Test
    fun parsePath_parses_app_share_with_leading_slash() {
        assertEquals(ShareTarget.App("share-token-123"), DeepLink.parsePath("/app/share-token-123"))
    }

    @Test
    fun parsePath_tolerates_missing_leading_slash_trailing_slash_and_query() {
        assertEquals(ShareTarget.App("abc"), DeepLink.parsePath("app/abc"))
        assertEquals(ShareTarget.App("abc"), DeepLink.parsePath("/app/abc/"))
        assertEquals(ShareTarget.App("abc"), DeepLink.parsePath("/app/abc?ref=mail"))
    }

    @Test
    fun parsePath_parses_release_share_with_query() {
        assertEquals(
            ShareTarget.Release(shareId = "abc", versionId = "version-1"),
            DeepLink.parsePath("/app/abc/releases/version-1?from=push"),
        )
    }

    @Test
    fun parsePath_rejects_non_app_root_blank_and_extra_segments() {
        assertNull(DeepLink.parsePath("/settings/abc"))
        assertNull(DeepLink.parsePath("/app/"))
        assertNull(DeepLink.parsePath("/"))
        assertNull(DeepLink.parsePath(""))
        assertNull(DeepLink.parsePath(null))
        assertNull(DeepLink.parsePath("/app/abc/version-1"))
        assertNull(DeepLink.parsePath("/app/abc/releases"))
    }
}
