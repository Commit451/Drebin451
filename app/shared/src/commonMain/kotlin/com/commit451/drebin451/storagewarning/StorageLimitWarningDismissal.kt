package com.commit451.drebin451.storagewarning

internal expect suspend fun isStorageLimitWarningDismissed(): Boolean

internal expect suspend fun dismissStorageLimitWarning()
