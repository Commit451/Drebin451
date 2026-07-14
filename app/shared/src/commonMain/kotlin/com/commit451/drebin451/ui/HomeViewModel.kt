package com.commit451.drebin451.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commit451.drebin451.api.Api
import com.commit451.drebin451.auth.UserManager
import com.commit451.drebin451.file.PickedApk
import com.commit451.drebin451.follow.followApp
import com.commit451.drebin451.follow.isFollowing
import com.commit451.drebin451.follow.isPushSupported
import com.commit451.drebin451.model.App
import com.commit451.drebin451.model.AppVersion
import com.commit451.drebin451.model.PaginatedResponse
import com.commit451.drebin451.storagewarning.dismissStorageLimitWarning
import com.commit451.drebin451.storagewarning.isStorageLimitWarningDismissed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        refreshCurrentUser()
        load()
        checkStorageStatus()
    }

    private fun refreshCurrentUser() {
        viewModelScope.launch { fetchCurrentUser() }
    }

    private suspend fun fetchCurrentUser() {
        runCatching { Api.user() }
            .onSuccess { UserManager.set(it) }
    }

    fun checkStorageStatus(showErrors: Boolean = false) {
        viewModelScope.launch {
            val dismissed = runCatching { isStorageLimitWarningDismissed() }
                .getOrDefault(_state.value.storageWarningDismissed)
            runCatching { Api.storageStatus() }
                .onSuccess { status ->
                    _state.update {
                        it.copy(storageStatus = status, storageWarningDismissed = dismissed)
                    }
                }
                .onFailure { t ->
                    _state.update { it.copy(storageWarningDismissed = dismissed) }
                    if (showErrors) {
                        _state.update { it.copy(message = t.message ?: "Failed to check storage") }
                    }
                }
        }
    }

    fun dismissStorageWarning() {
        viewModelScope.launch {
            runCatching { dismissStorageLimitWarning() }
            _state.update { it.copy(storageWarningDismissed = true) }
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = true,
                    loadingMore = false,
                    nextPageToken = null,
                    sharedNextPageToken = null,
                )
            }
            try {
                val yours = Api.apps()
                val shared = Api.sharedApps()
                _state.update {
                    it.copy(
                        apps = yours.items,
                        sharedApps = shared.items,
                        nextPageToken = yours.nextPageToken,
                        sharedNextPageToken = shared.nextPageToken,
                        loading = false,
                    )
                }
                checkStorageStatus()
            } catch (t: Throwable) {
                _state.update {
                    it.copy(
                        loading = false,
                        message = t.message ?: "Failed to load apps"
                    )
                }
            }
        }
    }

    /**
     * User-initiated pull-to-refresh. Drives [HomeState.refreshing] for the pull indicator and,
     * unlike [silentRefresh], surfaces a failure — a refresh the user explicitly asked for that
     * didn't work should be visible.
     */
    fun refresh() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    refreshing = true,
                    loadingMore = false,
                    nextPageToken = null,
                    sharedNextPageToken = null,
                )
            }
            try {
                val yours = Api.apps()
                val shared = Api.sharedApps()
                _state.update {
                    it.copy(
                        apps = yours.items,
                        sharedApps = shared.items,
                        nextPageToken = yours.nextPageToken,
                        sharedNextPageToken = shared.nextPageToken,
                        refreshing = false,
                    )
                }
                checkStorageStatus()
            } catch (t: Throwable) {
                _state.update {
                    it.copy(
                        refreshing = false,
                        message = t.message ?: "Failed to refresh"
                    )
                }
            }
        }
    }

    /**
     * Reloads the app lists without any spinner — used to pick up changes (e.g. an app deleted
     * from its detail screen) when Home resumes. A transient failure keeps the cached lists rather
     * than surfacing an error.
     */
    fun silentRefresh() {
        viewModelScope.launch {
            try {
                fetchCurrentUser()
                val yours = Api.apps()
                val shared = Api.sharedApps()
                _state.update {
                    it.copy(
                        apps = yours.items,
                        sharedApps = shared.items,
                        nextPageToken = yours.nextPageToken,
                        sharedNextPageToken = shared.nextPageToken,
                        loadingMore = false,
                    )
                }
            } catch (_: Throwable) {
            }
            checkStorageStatus()
        }
    }

    fun loadMore(tab: HomeTab) {
        val current = _state.value
        val token = when (tab) {
            HomeTab.Yours -> current.nextPageToken
            HomeTab.Shared -> current.sharedNextPageToken
        } ?: return
        if (current.loading || current.refreshing || current.loadingMore) return
        viewModelScope.launch {
            _state.update { it.copy(loadingMore = true) }
            try {
                val page = when (tab) {
                    HomeTab.Yours -> Api.apps(pageToken = token)
                    HomeTab.Shared -> Api.sharedApps(pageToken = token)
                }
                _state.update { state -> state.withAdditionalPage(tab, page) }
            } catch (t: Throwable) {
                _state.update {
                    it.copy(
                        loadingMore = false,
                        message = t.message ?: "Failed to load more apps"
                    )
                }
            }
        }
    }

    private fun HomeState.withAdditionalPage(
        tab: HomeTab,
        page: PaginatedResponse<App>
    ): HomeState =
        when (tab) {
            HomeTab.Yours -> copy(
                apps = (apps + page.items).distinctBy { it.id },
                nextPageToken = page.nextPageToken,
                loadingMore = false,
            )

            HomeTab.Shared -> copy(
                sharedApps = (sharedApps + page.items).distinctBy { it.id },
                sharedNextPageToken = page.nextPageToken,
                loadingMore = false,
            )
        }

    fun upload(picked: PickedApk) {
        viewModelScope.launch {
            _state.update { it.copy(uploading = true, storageLimitUploadDialogMessage = null) }
            try {
                val version = Api.uploadApp(picked)
                val label = version.versionName.ifBlank { picked.fileName }
                val promptApp = notificationPromptAppFor(version)
                _state.update {
                    it.copy(
                        uploading = false,
                        message = "Uploaded $label",
                        notificationPromptApp = promptApp ?: it.notificationPromptApp,
                    )
                }
                load()
                checkStorageStatus()
            } catch (t: Throwable) {
                val storageLimitMessage = uploadStorageLimitDialogMessage(t)
                if (storageLimitMessage != null) {
                    _state.update {
                        it.copy(
                            uploading = false,
                            message = null,
                            storageLimitUploadDialogMessage = storageLimitMessage,
                        )
                    }
                    checkStorageStatus()
                } else {
                    _state.update {
                        it.copy(
                            uploading = false,
                            message = t.message ?: "Upload failed"
                        )
                    }
                }
            }
        }
    }

    /**
     * After a Home upload, ask Android users whether to follow the app only when the upload created
     * the app's very first version. The app's `createdAt` mirrors the version timestamp only for a
     * brand-new app; checking [App.versionCount] too avoids false positives if future imports or
     * manual data repair ever reuse timestamps.
     */
    private suspend fun notificationPromptAppFor(version: AppVersion): App? {
        if (!isPushSupported() || isFollowing(version.appId)) return null
        val uploadedApp = runCatching { Api.app(version.appId) }.getOrNull() ?: return null
        return uploadedApp.takeIf { app ->
            isFirstUploadForNewApp(app = app, version = version) && !isFollowing(app.id)
        }
    }

    fun notificationPromptHandled() {
        _state.update { it.copy(notificationPromptApp = null) }
    }

    /**
     * Follows a newly-created app after the screen has shown the same runtime permission prompt used
     * by the detail-screen bell. As there, we subscribe regardless of the permission result: the FCM
     * topic registration succeeds even if Android won't display notifications until permission is
     * granted later in Settings.
     */
    fun followNewApp(appId: String) {
        if (!isPushSupported() || isFollowing(appId)) return
        viewModelScope.launch {
            try {
                followApp(appId)
                _state.update { it.copy(message = "Following — you'll be notified of new versions") }
            } catch (_: Throwable) {
                _state.update { it.copy(message = "Couldn't update notifications. Try again.") }
            }
        }
    }

    /** Removes a shared app from this user's collection. The owner app and its uploads are untouched. */
    fun deleteSharedApp(app: App) {
        if (app.id in _state.value.deletingSharedAppIds) return
        viewModelScope.launch {
            _state.update { it.copy(deletingSharedAppIds = it.deletingSharedAppIds + app.id) }
            try {
                Api.deleteSharedApp(app.id)
                val name = app.label.ifBlank { app.applicationId }
                _state.update { state ->
                    state.copy(
                        sharedApps = state.sharedApps.filterNot { it.id == app.id },
                        deletingSharedAppIds = state.deletingSharedAppIds - app.id,
                        message = "Deleted $name from Shared",
                    )
                }
            } catch (t: Throwable) {
                _state.update {
                    it.copy(
                        deletingSharedAppIds = it.deletingSharedAppIds - app.id,
                        message = t.message ?: "Couldn't delete shared app",
                    )
                }
            }
        }
    }

    fun notify(message: String) {
        _state.update { it.copy(message = message) }
    }

    fun messageShown() {
        _state.update { it.copy(message = null) }
    }

    fun storageLimitUploadDialogShown() {
        _state.update { it.copy(storageLimitUploadDialogMessage = null) }
    }
}

internal fun isFirstUploadForNewApp(app: App, version: AppVersion): Boolean =
    app.id == version.appId &&
            app.versionCount == 1 &&
            app.createdAt == version.createdAt
