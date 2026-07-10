package com.commit451.drebin451.storagewarning

private const val STORAGE_LIMIT_WARNING_DISMISSED_LOCAL_STORAGE_KEY =
    "drebin451.storageLimitWarning.dismissed"

internal actual suspend fun isStorageLimitWarningDismissed(): Boolean =
    readStorageLimitWarningDismissed(STORAGE_LIMIT_WARNING_DISMISSED_LOCAL_STORAGE_KEY)

internal actual suspend fun dismissStorageLimitWarning() {
    writeStorageLimitWarningDismissed(
        STORAGE_LIMIT_WARNING_DISMISSED_LOCAL_STORAGE_KEY,
        dismissed = true
    )
}

internal expect fun readStorageLimitWarningDismissed(key: String): Boolean

internal expect fun writeStorageLimitWarningDismissed(key: String, dismissed: Boolean)
