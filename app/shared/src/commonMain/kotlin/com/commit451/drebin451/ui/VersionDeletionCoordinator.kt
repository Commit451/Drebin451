package com.commit451.drebin451.ui

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class VersionDeletionCompletion(
    val appId: String,
    val deletedVersionIds: Set<String>,
)

/** App-scoped registry and invalidation stream for confirmed build-deletion batches. */
internal class VersionDeletionCoordinator {
    private val _inFlightByApp = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val inFlightByApp = _inFlightByApp.asStateFlow()

    private val _completed = MutableSharedFlow<VersionDeletionCompletion>()
    val completed = _completed.asSharedFlow()

    /** Atomically reserves IDs that are not already part of another detached deletion batch. */
    fun reserve(appId: String, versionIds: Set<String>): Set<String> {
        if (versionIds.isEmpty()) return emptySet()
        while (true) {
            val current = _inFlightByApp.value
            val currentIds = current[appId].orEmpty()
            val reserved = versionIds - currentIds
            if (reserved.isEmpty()) return emptySet()
            val next = current + (appId to (currentIds + reserved))
            if (_inFlightByApp.compareAndSet(current, next)) return reserved
        }
    }

    /** Publishes successful removals before making the batch IDs selectable again. */
    suspend fun complete(
        appId: String,
        reservedVersionIds: Set<String>,
        deletedVersionIds: Set<String>,
    ) {
        _completed.emit(
            VersionDeletionCompletion(
                appId = appId,
                deletedVersionIds = deletedVersionIds,
            )
        )
        release(appId, reservedVersionIds)
    }

    private fun release(appId: String, versionIds: Set<String>) {
        while (true) {
            val current = _inFlightByApp.value
            val remaining = current[appId].orEmpty() - versionIds
            val next = if (remaining.isEmpty()) current - appId else current + (appId to remaining)
            if (_inFlightByApp.compareAndSet(current, next)) return
        }
    }
}

internal val versionDeletionCoordinator = VersionDeletionCoordinator()
