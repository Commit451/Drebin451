package com.commit451.drebin451.ui

import com.commit451.drebin451.model.App
import com.commit451.drebin451.model.AppVersion

data class AppDetailState(
    val app: App,
    val versions: List<AppVersion> = emptyList(),
    val loading: Boolean = true,
    // Drives the pull-to-refresh indicator; distinct from [loading] (the initial full-screen spinner).
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val nextPageToken: String? = null,
    val uploading: Boolean = false,
    // Flips true once the whole app has been deleted, signalling the screen to navigate away.
    val deleted: Boolean = false,
    // Whether this device follows the app (subscribed to its update topic); followBusy gates the toggle.
    val following: Boolean = false,
    val followBusy: Boolean = false,
    val message: String? = null,
    val storageLimitUploadDialogMessage: String? = null,
)
