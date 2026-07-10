package com.commit451.drebin451.share

import com.commit451.drebin451.push.PushData

/**
 * Parses a Drebin link into a [DeepLinkTarget], or null if it isn't one. Accepts:
 *
 * - public App Links: `https://drebin451.com/app/{shareId}` and
 *   `https://drebin451.com/app/{shareId}/releases/{versionId}`
 * - private push deep links: `drebin451://app/{appId}/{versionId}`
 *
 * Share-link ids are random URL-safe tokens. Push links use the stable app/version ids because the
 * notification is already scoped to devices that follow that app.
 */
object DeepLink {
    fun parse(url: String?): DeepLinkTarget? {
        if (url.isNullOrBlank()) return null
        val scheme = url.substringBefore("://", missingDelimiterValue = "")
        val afterScheme = url.substringAfter("://", missingDelimiterValue = "")
        if (scheme.isEmpty() || afterScheme.isEmpty()) return null
        val host = afterScheme.substringBefore('/')
        val path = afterScheme.substringAfter('/', missingDelimiterValue = "")
        return when {
            scheme == "https" && host == ShareLinks.HOST_NAME -> parsePath(path)
            scheme == PushData.DEEP_LINK_SCHEME && host == PushData.DEEP_LINK_APP_HOST -> parseAppVersionPath(
                path
            )

            else -> null
        }
    }

    /**
     * Parses just the path of a link (no scheme/host) into a [ShareTarget], or null. Accepts
     * `app/{shareId}` and `app/{shareId}/releases/{versionId}` with an optional leading slash and
     * trailing query/fragment. Host-agnostic:
     * used on web, where the page is served from the hosting domain (not [ShareLinks.HOST_NAME])
     * but the path — e.g. `window.location.pathname` — is still ours.
     */
    fun parsePath(path: String?): ShareTarget? {
        if (path.isNullOrBlank()) return null
        val cleaned = path.substringBefore('?').substringBefore('#')
        val segments = cleaned.split('/').filter { it.isNotEmpty() }
        if (segments.firstOrNull() != "app") return null
        return when {
            segments.size == 2 -> ShareTarget.App(segments[1])
            segments.size == 4 && segments[2] == "releases" -> ShareTarget.Release(
                shareId = segments[1],
                versionId = segments[3],
            )

            else -> null
        }
    }

    private fun parseAppVersionPath(path: String?): AppVersionDeepLinkTarget? {
        if (path.isNullOrBlank()) return null
        val cleaned = path.substringBefore('?').substringBefore('#')
        val segments = cleaned.split('/').filter { it.isNotEmpty() }
        return when {
            segments.size == 2 -> AppVersionDeepLinkTarget(
                appId = segments[0],
                versionId = segments[1]
            )

            else -> null
        }
    }
}
