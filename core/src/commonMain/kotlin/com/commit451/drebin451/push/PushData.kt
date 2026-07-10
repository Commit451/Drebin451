package com.commit451.drebin451.push

/**
 * Keys in the "new version" FCM message's data payload. Shared by the server (which sets them) and
 * the Android client (which reads them — in `onMessageReceived` when foregrounded, or from the
 * launch intent's extras when a system-shown notification is tapped from the background), so both
 * sides agree on the contract.
 */
object PushData {
    const val APP_ID = "appId"
    const val VERSION_ID = "versionId"
    const val TITLE = "title"
    const val BODY = "body"
    const val DEEP_LINK = "deep_link"

    /** Action FCM uses when the system displays a background notification and the user taps it. */
    const val CLICK_ACTION_OPEN_APP_VERSION = "com.commit451.drebin451.action.OPEN_APP_VERSION"

    /** Custom-scheme deep links used for private push notification targets. */
    const val DEEP_LINK_SCHEME = "drebin451"
    const val DEEP_LINK_APP_HOST = "app"

    fun appVersionDeepLink(appId: String, versionId: String): String {
        require(appId.isNotBlank()) { "appId is required" }
        require(versionId.isNotBlank()) { "versionId is required" }
        return "$DEEP_LINK_SCHEME://$DEEP_LINK_APP_HOST/$appId/$versionId"
    }
}
