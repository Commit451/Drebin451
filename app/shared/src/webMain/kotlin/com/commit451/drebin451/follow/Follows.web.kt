package com.commit451.drebin451.follow

// Web has no push, so following is unavailable and the UI hides its toggle (see isPushSupported()).
// Everything here is a no-op.

actual fun isPushSupported(): Boolean = false

actual fun isFollowing(appId: String): Boolean = false

actual suspend fun followApp(appId: String) = Unit

actual suspend fun unfollowApp(appId: String) = Unit

actual suspend fun unfollowAll() = Unit
