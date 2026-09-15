package com.commit451.drebin451.ui

import com.commit451.drebin451.api.Api
import com.commit451.drebin451.model.App
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class AppDeletion(
    val app: App,
    val revision: Long,
    val pending: Boolean = true,
    val failureMessage: String? = null,
)

internal data class AppDeletions(
    val revision: Long = 0,
    val entries: Map<String, AppDeletion> = emptyMap(),
) {
    fun visibleApps(apps: List<App>): List<App> = apps.filterNot { app ->
        val deletion = entries[app.id]
        // IDs are reused when an APK is uploaded again after deleting its app. Only hide the
        // deleted incarnation, including in stale refresh/page responses that arrive later.
        deletion != null && deletion.app.createdAt == app.createdAt && deletion.failureMessage == null
    }
}

/** Owns confirmed app deletes independently of the detail route's ViewModel lifetime. */
internal class AppDeletionCoordinator(
    private val scope: CoroutineScope,
    private val deleteRequest: suspend (String) -> Unit,
) {
    private val _state = MutableStateFlow(AppDeletions())
    val state = _state.asStateFlow()

    fun delete(app: App) {
        val deletion: AppDeletion
        while (true) {
            val current = _state.value
            val existing = current.entries[app.id]
            val alreadyDeleted = existing != null && existing.app.createdAt == app.createdAt &&
                    existing.failureMessage == null
            if (existing?.pending == true || alreadyDeleted) {
                return
            }
            val next = AppDeletion(app, revision = current.revision + 1)
            if (_state.compareAndSet(
                    current,
                    current.copy(revision = next.revision, entries = current.entries + (app.id to next)),
                )) {
                deletion = next
                break
            }
        }
        scope.launch {
            try {
                deleteRequest(app.id)
                complete(deletion.copy(pending = false))
            } catch (t: Throwable) {
                val name = app.label.ifBlank { app.applicationId }
                complete(
                    deletion.copy(
                        pending = false,
                        failureMessage = "Couldn't delete $name. ${t.message ?: "Please try again."}",
                    )
                )
                if (t is CancellationException) throw t
            }
        }
    }

    private fun complete(deletion: AppDeletion) {
        _state.update { current ->
            current.copy(entries = current.entries + (deletion.app.id to deletion))
        }
    }
}

internal val appDeletionCoordinator = AppDeletionCoordinator(
    scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    deleteRequest = Api::deleteApp,
)

/** Applies the optimistic overlay and reconciles failures on Home, where errors remain visible. */
internal class HomeAppDeletionObserver(
    private val coordinator: AppDeletionCoordinator,
    private val homeState: MutableStateFlow<HomeState>,
    private val refreshLists: () -> Unit,
    private val refreshStorage: () -> Unit,
    private val currentUserId: () -> String?,
) {
    private var lastResumedRevision = coordinator.state.value.forCurrentUser().revision
    private val handledCompletions = coordinator.state.value.entries.values
        .filterNot { it.pending }.mapTo(mutableSetOf()) { it.revision }

    suspend fun observe() {
        coordinator.state.collect { apply(it.forCurrentUser()) }
    }

    fun onResume() {
        val deletions = coordinator.state.value.forCurrentUser()
        val reconciledFailure = apply(deletions)
        // Normal returns (uploads, sharing, settings) still refresh. Returning after an optimistic
        // delete must not refetch the lists, whether the request is pending or already succeeded.
        if (!reconciledFailure && lastResumedRevision == deletions.revision &&
            deletions.entries.values.none { it.pending }) {
            refreshLists()
        }
        lastResumedRevision = deletions.revision
    }

    private fun AppDeletions.forCurrentUser(): AppDeletions {
        // A detached request may finish after signing out. Never restore the previous account's
        // cached app or display its failure in the new account's Home.
        val userId = currentUserId()
        val relevant = entries.filterValues { it.app.ownerUserId == userId }
        return AppDeletions(relevant.values.maxOfOrNull { it.revision } ?: 0, relevant)
    }

    private fun apply(deletions: AppDeletions): Boolean {
        val completions = deletions.entries.values.filter {
            !it.pending && handledCompletions.add(it.revision)
        }
        val failures = completions.filter { it.failureMessage != null }
        homeState.update { current ->
            // Restore a cached row first so an offline recovery fetch cannot leave the app hidden.
            // The subsequent backend refresh remains authoritative (the DELETE may have timed out
            // after the server committed it). Don't replace newer/recreated rows already present.
            val restored = failures.fold(current.apps) { apps, failure ->
                if (apps.any { it.id == failure.app.id }) {
                    apps
                } else {
                    (apps + failure.app).sortedByDescending { it.updatedAt }
                }
            }
            current.copy(
                apps = deletions.visibleApps(restored),
                message = failures.firstOrNull()?.failureMessage ?: current.message,
            )
        }
        if (failures.isNotEmpty()) refreshLists()
        if (completions.any { it.failureMessage == null }) refreshStorage()
        return failures.isNotEmpty()
    }
}
