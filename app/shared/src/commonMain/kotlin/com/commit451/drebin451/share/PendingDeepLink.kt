package com.commit451.drebin451.share

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds a deep link captured at app launch (from an App Links, custom-scheme, or FCM tap intent)
 * until the app is ready to navigate to it — consumed once at the nav host, after splash/login
 * resolve. A [StateFlow] so a link that arrives while the app is already running (onNewIntent) also
 * triggers navigation. Mirrors the in-memory holder style of [com.commit451.drebin451.auth.UserManager].
 */
object PendingDeepLink {
    private val _target = MutableStateFlow<DeepLinkTarget?>(null)
    val target: StateFlow<DeepLinkTarget?> = _target.asStateFlow()

    fun set(target: DeepLinkTarget?) {
        _target.value = target
    }
}
