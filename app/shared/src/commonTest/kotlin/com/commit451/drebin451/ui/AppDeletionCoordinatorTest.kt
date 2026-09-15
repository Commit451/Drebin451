package com.commit451.drebin451.ui

import com.commit451.drebin451.model.App
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AppDeletionCoordinatorTest {
    private val app = App(
        id = "owner:com.example.app",
        applicationId = "com.example.app",
        ownerUserId = "owner",
        label = "Example",
        createdAt = 100,
        updatedAt = 200,
    )
    private val other = app.copy(id = "owner:com.example.other", createdAt = 101, updatedAt = 150)

    @Test
    fun delete_removesRowBeforeRequestFinishesWithoutRefreshingOrResettingPagination() = runTest {
        val fixture = Fixture(backgroundScope)
        fixture.coordinator.delete(app)
        fixture.observer.onResume()

        assertEquals(listOf(other), fixture.home.value.apps)
        assertEquals("next-page", fixture.home.value.nextPageToken)
        assertEquals(listOf(other), fixture.home.value.sharedApps)
        assertFalse(fixture.home.value.loading)
        assertFalse(fixture.home.value.refreshing)
        assertEquals(0, fixture.listRefreshes)
        assertEquals(0, fixture.storageRefreshes)
        assertTrue(fixture.coordinator.state.value.entries.getValue(app.id).pending)

        runCurrent()
        assertEquals(listOf(app.id), fixture.requests)
        assertEquals(listOf(other), fixture.home.value.apps)
        fixture.response.complete(Result.success(Unit))
        runCurrent()
        assertFalse(fixture.coordinator.state.value.entries.getValue(app.id).pending)
        assertEquals(listOf(other), fixture.home.value.apps)
        assertEquals(0, fixture.listRefreshes)
        assertEquals(1, fixture.storageRefreshes)
        assertNull(fixture.home.value.message)
    }

    @Test
    fun fastSuccess_beforeHomeResumesStillSkipsListRefresh() = runTest {
        val fixture = Fixture(backgroundScope)
        fixture.response.complete(Result.success(Unit))
        fixture.coordinator.delete(app)
        runCurrent()
        fixture.observer.onResume()

        assertEquals(listOf(other), fixture.home.value.apps)
        assertEquals(0, fixture.listRefreshes)
        assertEquals(1, fixture.storageRefreshes)
    }

    @Test
    fun failure_afterReturnRestoresCachedRowAndRefreshesOnce() = runTest {
        val fixture = Fixture(backgroundScope)
        fixture.coordinator.delete(app)
        fixture.observer.onResume()
        runCurrent()
        fixture.response.complete(Result.failure(IllegalStateException("Offline")))
        runCurrent()

        // The refresh callback intentionally supplies no data: restoration must work even offline.
        assertEquals(listOf(app, other), fixture.home.value.apps)
        assertEquals(1, fixture.listRefreshes)
        assertEquals(0, fixture.storageRefreshes)
        assertEquals("Couldn't delete Example. Offline", fixture.home.value.message)
        assertEquals(listOf(app, other), fixture.coordinator.state.value.visibleApps(listOf(app, other)))
    }

    @Test
    fun fastFailure_beforeHomeResumesDoesNotCauseADuplicateRefresh() = runTest {
        val fixture = Fixture(backgroundScope)
        fixture.response.complete(Result.failure(IllegalStateException("Offline")))
        fixture.coordinator.delete(app)
        runCurrent()
        fixture.observer.onResume()
        runCurrent()

        assertEquals(listOf(app, other), fixture.home.value.apps)
        assertEquals(1, fixture.listRefreshes)
        assertTrue(fixture.home.value.message.orEmpty().contains("Offline"))
    }

    @Test
    fun staleRefreshAndPageResponses_cannotResurrectPendingOrSuccessfulDeletes() = runTest {
        val fixture = Fixture(backgroundScope)
        fixture.coordinator.delete(app)
        val stalePage = listOf(app, other)
        assertEquals(listOf(other), fixture.coordinator.state.value.visibleApps(stalePage))

        fixture.response.complete(Result.success(Unit))
        runCurrent()
        assertEquals(listOf(other), fixture.coordinator.state.value.visibleApps(stalePage))
    }

    @Test
    fun recreatedApp_withSameIdIsVisibleAndCanBeDeletedAgain() = runTest {
        val fixture = Fixture(backgroundScope)
        fixture.response.complete(Result.success(Unit))
        fixture.coordinator.delete(app)
        runCurrent()
        val recreated = app.copy(createdAt = 300, updatedAt = 300)
        assertEquals(listOf(recreated), fixture.coordinator.state.value.visibleApps(listOf(recreated)))

        fixture.coordinator.delete(recreated)
        runCurrent()
        assertEquals(listOf(app.id, app.id), fixture.requests)
        assertTrue(fixture.coordinator.state.value.visibleApps(listOf(recreated)).isEmpty())
    }

    @Test
    fun duplicateConfirmation_doesNotSendAnotherDelete() = runTest {
        val fixture = Fixture(backgroundScope)
        fixture.coordinator.delete(app)
        fixture.coordinator.delete(app)
        runCurrent()
        assertEquals(listOf(app.id), fixture.requests)

        fixture.response.complete(Result.success(Unit))
        runCurrent()
        fixture.coordinator.delete(app)
        runCurrent()
        assertEquals(listOf(app.id), fixture.requests)
    }

    @Test
    fun failedDelete_canBeRetried() = runTest {
        val fixture = Fixture(backgroundScope)
        fixture.response.complete(Result.failure(IllegalStateException("Offline")))
        fixture.coordinator.delete(app)
        runCurrent()
        fixture.coordinator.delete(app)
        assertTrue(fixture.coordinator.state.value.entries.getValue(app.id).pending)
        runCurrent()
        assertEquals(listOf(app.id, app.id), fixture.requests)
        assertEquals(2, fixture.listRefreshes)
    }

    @Test
    fun cancellingCallingRoute_doesNotCancelDeletion() = runTest {
        val fixture = Fixture(backgroundScope)
        val routeScope = CoroutineScope(coroutineContext + Job())
        try {
            routeScope.launch { fixture.coordinator.delete(app) }
            runCurrent()
            routeScope.cancel()
            fixture.response.complete(Result.success(Unit))
            runCurrent()

            val deletion = fixture.coordinator.state.value.entries.getValue(app.id)
            assertFalse(deletion.pending)
            assertNull(deletion.failureMessage)
            assertEquals(listOf(other), fixture.home.value.apps)
            assertEquals(0, fixture.listRefreshes)
        } finally {
            routeScope.cancel()
        }
    }

    @Test
    fun concurrentDeletes_failureRestoresOnlyFailedApp() = runTest {
        val firstResponse = CompletableDeferred<Unit>()
        val secondResponse = CompletableDeferred<Unit>()
        val coordinator = AppDeletionCoordinator(backgroundScope) { id ->
            if (id == app.id) firstResponse.await() else secondResponse.await()
        }
        val home = MutableStateFlow(HomeState(apps = listOf(app, other), loading = false))
        var refreshes = 0
        val observer = HomeAppDeletionObserver(coordinator, home, { refreshes++ }, {}, { "owner" })
        backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { observer.observe() }
        coordinator.delete(app)
        coordinator.delete(other)
        observer.onResume()
        assertTrue(home.value.apps.isEmpty())

        firstResponse.completeExceptionally(IllegalStateException("Offline"))
        runCurrent()
        assertEquals(listOf(app), home.value.apps)
        assertEquals(listOf(app), coordinator.state.value.visibleApps(listOf(app, other)))
        assertEquals(1, refreshes)
        secondResponse.complete(Unit)
        runCurrent()
        assertEquals(listOf(app), home.value.apps)
        assertEquals(1, refreshes)
    }

    @Test
    fun normalResumes_stillRefreshAfterDeletionReturnHasBeenHandled() = runTest {
        val fixture = Fixture(backgroundScope)
        fixture.observer.onResume()
        assertEquals(1, fixture.listRefreshes)
        fixture.coordinator.delete(app)
        fixture.response.complete(Result.success(Unit))
        runCurrent()
        fixture.observer.onResume()
        assertEquals(1, fixture.listRefreshes)
        fixture.observer.onResume()
        assertEquals(2, fixture.listRefreshes)
    }

    @Test
    fun failure_doesNotOverwriteANewerRowAlreadyInTheList() = runTest {
        val fixture = Fixture(backgroundScope)
        fixture.coordinator.delete(app)
        fixture.observer.onResume()
        val recreated = app.copy(createdAt = 300, updatedAt = 300, label = "Recreated")
        fixture.home.value = fixture.home.value.copy(apps = listOf(recreated, other))
        fixture.response.complete(Result.failure(IllegalStateException("Offline")))
        runCurrent()

        assertEquals(listOf(recreated, other), fixture.home.value.apps)
        assertEquals(1, fixture.listRefreshes)
    }

    @Test
    fun repeatedResumes_whileDeletionIsPendingDoNotRefresh() = runTest {
        val fixture = Fixture(backgroundScope)
        fixture.coordinator.delete(app)
        fixture.observer.onResume()
        fixture.observer.onResume()
        runCurrent()

        assertEquals(listOf(other), fixture.home.value.apps)
        assertEquals(0, fixture.listRefreshes)
        fixture.response.complete(Result.success(Unit))
    }

    @Test
    fun failure_afterAccountChangeDoesNotRestorePreviousOwnersApp() = runTest {
        val fixture = Fixture(backgroundScope)
        fixture.coordinator.delete(app)
        fixture.observer.onResume()
        runCurrent()
        fixture.userId = "another-owner"
        fixture.home.value = HomeState(loading = false)
        fixture.response.complete(Result.failure(IllegalStateException("Offline")))
        runCurrent()

        assertTrue(fixture.home.value.apps.isEmpty())
        assertNull(fixture.home.value.message)
        assertEquals(0, fixture.listRefreshes)
    }

    @Test
    fun failure_recoveryUsesBackendResultWhenDeleteCommittedBeforeTimeout() = runTest {
        val coordinator = AppDeletionCoordinator(backgroundScope) {
            throw IllegalStateException("Timed out")
        }
        val home = MutableStateFlow(HomeState(apps = listOf(app, other), loading = false))
        var refreshes = 0
        val observer = HomeAppDeletionObserver(
            coordinator = coordinator,
            homeState = home,
            refreshLists = {
                refreshes++
                assertTrue(home.value.apps.any { it.id == app.id }, "Restore before fetching")
                home.value = home.value.copy(apps = listOf(other))
            },
            refreshStorage = {},
            currentUserId = { "owner" },
        )
        backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { observer.observe() }
        coordinator.delete(app)
        observer.onResume()
        runCurrent()

        assertEquals(listOf(other), home.value.apps)
        assertEquals(1, refreshes)
        assertTrue(home.value.message.orEmpty().contains("Timed out"))
    }

    private inner class Fixture(scope: CoroutineScope) {
        var userId = "owner"
        val response = CompletableDeferred<Result<Unit>>()
        val requests = mutableListOf<String>()
        val coordinator = AppDeletionCoordinator(scope) { id ->
            requests += id
            response.await().getOrThrow()
        }
        val home = MutableStateFlow(HomeState(
            apps = listOf(app, other),
            sharedApps = listOf(other),
            loading = false,
            nextPageToken = "next-page",
        ))
        var listRefreshes = 0
        var storageRefreshes = 0
        val observer = HomeAppDeletionObserver(
            coordinator, home, { listRefreshes++ }, { storageRefreshes++ }, { userId },
        )

        init {
            scope.launch(start = CoroutineStart.UNDISPATCHED) { observer.observe() }
        }
    }
}
