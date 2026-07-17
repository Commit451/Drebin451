package com.commit451.drebin451.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commit451.drebin451.api.Api
import com.commit451.drebin451.file.PickedApk
import com.commit451.drebin451.follow.followApp
import com.commit451.drebin451.follow.isFollowing
import com.commit451.drebin451.follow.unfollowApp
import com.commit451.drebin451.model.App
import com.commit451.drebin451.model.AppVersion
import com.commit451.drebin451.model.BatchDeleteVersionsRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// A confirmed multi-build delete must finish even if the user leaves the detail route. Keeping this
// bounded destructive operation outside viewModelScope prevents Navigation3 from cancelling a
// partially completed batch when it clears the route's ViewModel.
private val confirmedVersionDeletionScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

internal fun versionDeletionBatches(versions: List<AppVersion>): List<List<AppVersion>> =
    versions.chunked(BatchDeleteVersionsRequest.MAX_VERSION_IDS)

class AppDetailViewModel(initialApp: App) : ViewModel() {
    private val appId = initialApp.id
    private val completedDeletedVersionIds = MutableStateFlow<Set<String>>(emptySet())
    private val _state = MutableStateFlow(
        AppDetailState(
            app = initialApp,
            deletingVersionIds = versionDeletionCoordinator.inFlightByApp.value[appId].orEmpty(),
        )
    )
    val state: StateFlow<AppDetailState> = _state.asStateFlow()

    init {
        load()
        _state.update { it.copy(following = isFollowing(appId)) }
        viewModelScope.launch {
            versionDeletionCoordinator.inFlightByApp.collect { inFlightByApp ->
                _state.update {
                    it.copy(deletingVersionIds = inFlightByApp[appId].orEmpty())
                }
            }
        }
        viewModelScope.launch {
            versionDeletionCoordinator.completed.collect { completion ->
                if (completion.appId != appId) return@collect
                completedDeletedVersionIds.update { it + completion.deletedVersionIds }
                _state.update { state ->
                    state.copy(
                        versions = state.versions.filterNot {
                            it.id in completion.deletedVersionIds
                        }
                    )
                }
                viewModelScope.launch { refreshAfterCompletedDeletion() }
            }
        }
    }

    private suspend fun refreshAfterCompletedDeletion() {
        val refreshedApp = runCatching { Api.app(appId) }.getOrNull()
        val refreshedVersions = runCatching { Api.appVersions(appId) }.getOrNull()
        if (refreshedApp == null && refreshedVersions == null) return
        _state.update { state ->
            state.copy(
                app = refreshedApp ?: state.app,
                versions = refreshedVersions?.items
                    ?.filterNot { it.id in completedDeletedVersionIds.value }
                    ?: state.versions,
                nextPageToken = if (refreshedVersions != null) {
                    refreshedVersions.nextPageToken
                } else {
                    state.nextPageToken
                },
            )
        }
    }

    /** Refreshes the parent app row, including shareId changes made on the share screen. */
    fun refreshApp() {
        viewModelScope.launch {
            try {
                val app = Api.app(appId)
                _state.update { it.copy(app = app) }
            } catch (_: Throwable) {
                // Keep the route-provided app cached. Version loading/deletion surfaces actionable
                // errors; this refresh is best-effort so returning from Share picks up a new URL.
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, loadingMore = false, nextPageToken = null) }
            try {
                val page = Api.appVersions(appId)
                _state.update {
                    it.copy(
                        versions = page.items.filterNot {
                            it.id in completedDeletedVersionIds.value
                        },
                        nextPageToken = page.nextPageToken,
                        loading = false,
                    )
                }
            } catch (t: Throwable) {
                _state.update {
                    it.copy(
                        loading = false,
                        message = t.message ?: "Failed to load versions"
                    )
                }
            }
        }
    }

    /**
     * User-initiated pull-to-refresh of the version list. Drives [AppDetailState.refreshing] and
     * surfaces failures (a deliberate refresh that didn't work should be visible). Handy while a
     * CI/API upload is in flight — pull to see the new version land.
     */
    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(refreshing = true, loadingMore = false, nextPageToken = null) }
            try {
                val page = Api.appVersions(appId)
                _state.update {
                    it.copy(
                        versions = page.items.filterNot {
                            it.id in completedDeletedVersionIds.value
                        },
                        nextPageToken = page.nextPageToken,
                        refreshing = false,
                    )
                }
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

    fun loadMore() {
        val current = _state.value
        val token = current.nextPageToken ?: return
        if (current.loading || current.refreshing || current.loadingMore) return
        viewModelScope.launch {
            _state.update { it.copy(loadingMore = true) }
            try {
                val page = Api.appVersions(appId = appId, pageToken = token)
                _state.update { state ->
                    state.copy(
                        versions = (
                            state.versions + page.items.filterNot {
                                it.id in completedDeletedVersionIds.value
                            }
                        ).distinctBy { it.id },
                        nextPageToken = page.nextPageToken,
                        loadingMore = false,
                    )
                }
            } catch (t: Throwable) {
                _state.update {
                    it.copy(
                        loadingMore = false,
                        message = t.message ?: "Failed to load more versions"
                    )
                }
            }
        }
    }

    fun upload(picked: PickedApk) {
        viewModelScope.launch {
            _state.update { it.copy(uploading = true, storageLimitUploadDialogMessage = null) }
            try {
                val version = Api.uploadApp(picked)
                val label = version.versionName.ifBlank { picked.fileName }
                _state.update { it.copy(uploading = false, message = "Uploaded $label") }
                load()
            } catch (t: Throwable) {
                val storageLimitMessage = uploadStorageLimitDialogMessage(t)
                _state.update {
                    if (storageLimitMessage != null) {
                        it.copy(
                            uploading = false,
                            message = null,
                            storageLimitUploadDialogMessage = storageLimitMessage,
                        )
                    } else {
                        it.copy(uploading = false, message = t.message ?: "Upload failed")
                    }
                }
            }
        }
    }

    /** Deletes a single version and drops it from the list in place. */
    fun deleteVersion(version: AppVersion) {
        viewModelScope.launch {
            try {
                Api.deleteVersion(version.appId, version.id)
                val label = version.versionName.ifBlank { version.fileName }
                _state.update { s ->
                    s.copy(
                        versions = s.versions.filterNot { it.id == version.id },
                        message = "Deleted $label",
                    )
                }
            } catch (t: Throwable) {
                _state.update { it.copy(message = t.message ?: "Delete failed") }
            }
        }
    }

    /** Deletes selected builds in bounded server batches and removes every success immediately. */
    fun deleteVersions(versions: Collection<AppVersion>) {
        val candidates = versions
            .asSequence()
            .filter { it.appId == appId }
            .distinctBy { it.id }
            .toList()
        val reservedIds = versionDeletionCoordinator.reserve(
            appId = appId,
            versionIds = candidates.mapTo(mutableSetOf()) { it.id },
        )
        if (reservedIds.isEmpty()) return
        val requested = candidates.filter { it.id in reservedIds }
        _state.update { state ->
            state.copy(deletingVersionIds = state.deletingVersionIds + reservedIds)
        }

        confirmedVersionDeletionScope.launch {
            val deletedIds = mutableSetOf<String>()
            var firstFailure: Throwable? = null
            versionDeletionBatches(requested).forEach { batch ->
                try {
                    val batchIds = batch.mapTo(mutableSetOf()) { it.id }
                    val response = Api.deleteVersions(appId, batchIds)
                    val confirmedDeletedIds = response.deletedVersionIds
                        .filterTo(mutableSetOf()) { it in batchIds }
                    deletedIds += confirmedDeletedIds
                    _state.update { state ->
                        state.copy(
                            versions = state.versions.filterNot { it.id in confirmedDeletedIds },
                        )
                    }
                } catch (t: Throwable) {
                    if (firstFailure == null) firstFailure = t
                }
            }

            val failedCount = requested.size - deletedIds.size
            val deletedLabel = if (deletedIds.size == 1) "1 build" else "${deletedIds.size} builds"
            val failedLabel = if (failedCount == 1) "1 build" else "$failedCount builds"
            val message = when {
                failedCount == 0 -> "Deleted $deletedLabel"
                deletedIds.isEmpty() -> firstFailure?.message ?: "Couldn't delete $failedLabel"
                else -> "Deleted $deletedLabel; couldn't delete $failedLabel"
            }
            _state.update { state -> state.copy(message = message) }
            versionDeletionCoordinator.complete(
                appId = appId,
                reservedVersionIds = reservedIds,
                deletedVersionIds = deletedIds,
            )
        }
    }

    /** Edits the note attached to a version and updates the local row in place. */
    fun updateVersionNote(version: AppVersion, note: String) {
        viewModelScope.launch {
            try {
                val updated = Api.updateVersionNote(version.appId, version.id, note)
                _state.update { s ->
                    s.copy(
                        versions = s.versions.map { if (it.id == updated.id) updated else it },
                        message = if (updated.note.isBlank()) "Note cleared" else "Note updated",
                    )
                }
            } catch (t: Throwable) {
                _state.update { it.copy(message = t.message ?: "Couldn't update note") }
            }
        }
    }

    /** Clears a version note without deleting the version itself. */
    fun deleteVersionNote(version: AppVersion) {
        viewModelScope.launch {
            try {
                Api.deleteVersionNote(version.appId, version.id)
                _state.update { s ->
                    s.copy(
                        versions = s.versions.map { if (it.id == version.id) it.copy(note = "") else it },
                        message = "Note deleted",
                    )
                }
            } catch (t: Throwable) {
                _state.update { it.copy(message = t.message ?: "Couldn't delete note") }
            }
        }
    }

    /** Deletes the whole app (and all versions); on success [AppDetailState.deleted] tells the screen to leave. */
    fun deleteApp() {
        viewModelScope.launch {
            try {
                Api.deleteApp(appId)
                _state.update { it.copy(deleted = true) }
            } catch (t: Throwable) {
                _state.update { it.copy(message = t.message ?: "Delete failed") }
            }
        }
    }

    /**
     * Removes this app from the signed-in user's Shared collection. Reuses [AppDetailState.deleted]
     * to send the screen back, since the app is gone from this user's Shared tab (the owner's copy is
     * untouched).
     */
    fun deleteSharedApp() {
        viewModelScope.launch {
            try {
                Api.deleteSharedApp(appId)
                _state.update { it.copy(deleted = true) }
            } catch (t: Throwable) {
                _state.update { it.copy(message = t.message ?: "Couldn't delete the shared app") }
            }
        }
    }

    /**
     * Follows or unfollows this app — subscribing/unsubscribing this device to its FCM update topic.
     * Only reachable where push is supported (the screen hides the control otherwise).
     */
    fun toggleFollow() {
        if (_state.value.followBusy) return
        viewModelScope.launch {
            val wasFollowing = _state.value.following
            _state.update { it.copy(followBusy = true) }
            try {
                if (wasFollowing) unfollowApp(appId) else followApp(appId)
                _state.update {
                    it.copy(
                        following = !wasFollowing,
                        followBusy = false,
                        message = if (wasFollowing) {
                            "Unfollowed"
                        } else {
                            "Following — you'll be notified of new versions"
                        },
                    )
                }
            } catch (t: Throwable) {
                // Covers both a real subscribe failure and the awaitUnit() timeout (FCM can leave a
                // rejected topic op's Task uncompleted). Either way clear followBusy so the spinner
                // stops, and show a retryable message rather than leaking a raw exception string.
                _state.update {
                    it.copy(
                        followBusy = false,
                        message = "Couldn't update notifications. Try again."
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
