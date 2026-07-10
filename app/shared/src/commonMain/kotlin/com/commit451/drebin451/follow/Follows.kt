package com.commit451.drebin451.follow

/**
 * "Following" an app subscribes this device to that app's FCM update topic, so it gets a push when a
 * new version is uploaded. Backed by FCM topic subscriptions on Android; a no-op on web (no push
 * there, and the UI hides the follow control — see [isPushSupported]).
 *
 * FCM exposes no way to query a device's topic subscriptions, so the Android KMP target stores the
 * status in Preferences DataStore under noBackupFilesDir. That keeps the toggle device-local and
 * prevents a fresh install from restoring old notification state from cloud/device backup.
 */

/** Whether push / following is available on this platform (Android: yes, web: no). */
expect fun isPushSupported(): Boolean

/** Whether this device currently follows [appId]. */
expect fun isFollowing(appId: String): Boolean

/** Subscribes this device to new-version notifications for [appId]. Throws on failure. */
expect suspend fun followApp(appId: String)

/** Unsubscribes this device from [appId]. Throws on failure. */
expect suspend fun unfollowApp(appId: String)

/** Unsubscribes from every followed app and clears local follow state (called on sign-out). */
expect suspend fun unfollowAll()
