package com.commit451.drebin451.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppDetailSelectionTest {

    @Test
    fun longPressSelection_startsWithOnlyPressedBuildSelected() {
        val selection = VersionSelection().select("build-2")

        assertTrue(selection.active)
        assertEquals(setOf("build-2"), selection.versionIds)
    }

    @Test
    fun toggleSelection_addsAndRemovesBuildsAndExitsWhenEmpty() {
        val selection = VersionSelection()
            .select("build-1")
            .toggle("build-2")

        assertEquals(setOf("build-1", "build-2"), selection.versionIds)

        val oneRemaining = selection.toggle("build-1")
        assertEquals(setOf("build-2"), oneRemaining.versionIds)

        val empty = oneRemaining.toggle("build-2")
        assertFalse(empty.active)
        assertTrue(empty.versionIds.isEmpty())
    }

    @Test
    fun retainAvailableBuilds_dropsSelectionsThatAreNoLongerLoaded() {
        val selection = VersionSelection(setOf("build-1", "build-2"))

        assertEquals(
            setOf("build-2"),
            selection.retainAvailable(setOf("build-2", "build-3")).versionIds,
        )
    }

    @Test
    fun clearSelection_exitsSelectionMode() {
        val selection = VersionSelection(setOf("build-1", "build-2")).clear()

        assertFalse(selection.active)
        assertTrue(selection.versionIds.isEmpty())
    }
}
