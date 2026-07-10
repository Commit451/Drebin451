package com.commit451.drebin451.ui

import com.commit451.drebin451.model.App
import com.commit451.drebin451.model.StorageStatus

data class HomeState(
    /** Apps owned by the signed-in user. */
    val apps: List<App> = emptyList(),
    /** Apps the signed-in user added by opening a share URL. */
    val sharedApps: List<App> = emptyList(),
    val loading: Boolean = true,
    // Drives the pull-to-refresh indicator; distinct from [loading] (the initial full-screen spinner).
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val nextPageToken: String? = null,
    val sharedNextPageToken: String? = null,
    val uploading: Boolean = false,
    val deletingSharedAppIds: Set<String> = emptySet(),
    val storageStatus: StorageStatus? = null,
    val storageWarningDismissed: Boolean = false,
    val message: String? = null,
    val storageLimitUploadDialogMessage: String? = null,
    // Set after the first upload creates a brand-new app; the screen asks whether to follow it.
    val notificationPromptApp: App? = null,
)
