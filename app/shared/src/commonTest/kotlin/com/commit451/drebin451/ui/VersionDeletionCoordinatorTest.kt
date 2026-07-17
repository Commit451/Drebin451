package com.commit451.drebin451.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VersionDeletionCoordinatorTest {

    @Test
    fun reserve_preventsOverlappingBatchesForTheSameApp() {
        val coordinator = VersionDeletionCoordinator()

        assertEquals(
            setOf("build-1", "build-2"),
            coordinator.reserve("app-1", setOf("build-1", "build-2")),
        )
        assertEquals(
            setOf("build-3"),
            coordinator.reserve("app-1", setOf("build-2", "build-3")),
        )
        assertTrue(coordinator.reserve("app-1", setOf("build-1", "build-2")).isEmpty())
    }

    @Test
    fun reserve_tracksAppsIndependently() {
        val coordinator = VersionDeletionCoordinator()

        assertEquals(setOf("build-1"), coordinator.reserve("app-1", setOf("build-1")))
        assertEquals(setOf("build-1"), coordinator.reserve("app-2", setOf("build-1")))
    }
}
