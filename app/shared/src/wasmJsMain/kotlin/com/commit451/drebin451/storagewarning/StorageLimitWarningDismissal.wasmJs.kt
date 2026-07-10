@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.commit451.drebin451.storagewarning

@JsFun("(key) => window.localStorage.getItem(key) === 'true'")
private external fun jsReadStorageLimitWarningDismissed(key: String): Boolean

@JsFun("(key, value) => { window.localStorage.setItem(key, value); }")
private external fun jsWriteStorageLimitWarningDismissed(key: String, value: String)

internal actual fun readStorageLimitWarningDismissed(key: String): Boolean =
    runCatching { jsReadStorageLimitWarningDismissed(key) }.getOrDefault(false)

internal actual fun writeStorageLimitWarningDismissed(key: String, dismissed: Boolean) {
    runCatching { jsWriteStorageLimitWarningDismissed(key, dismissed.toString()) }
}
