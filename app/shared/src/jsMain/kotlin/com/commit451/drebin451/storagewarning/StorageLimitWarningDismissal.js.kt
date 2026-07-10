package com.commit451.drebin451.storagewarning

private external interface WebStorage {
    fun getItem(key: String): String?
    fun setItem(key: String, value: String)
}

private external val localStorage: WebStorage

internal actual fun readStorageLimitWarningDismissed(key: String): Boolean =
    runCatching { localStorage.getItem(key) == "true" }.getOrDefault(false)

internal actual fun writeStorageLimitWarningDismissed(key: String, dismissed: Boolean) {
    runCatching { localStorage.setItem(key, dismissed.toString()) }
}
