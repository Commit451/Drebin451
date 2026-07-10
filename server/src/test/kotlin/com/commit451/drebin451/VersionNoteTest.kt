package com.commit451.drebin451

import com.commit451.drebin451.push.PushData
import kotlin.test.Test
import kotlin.test.assertEquals

class VersionNoteTest {

    @Test
    fun `version notes are trimmed`() {
        assertEquals("Release notes", normalizeVersionNote("  Release notes  "))
    }

    @Test
    fun `version notes are capped at max length`() {
        val note = "x".repeat(MAX_NOTE_LENGTH + 5)

        assertEquals(MAX_NOTE_LENGTH, normalizeVersionNote(note).length)
    }

    @Test
    fun `deleting a version note stores the blank note`() {
        assertEquals("", normalizeVersionNote("   "))
    }

    @Test
    fun `new version push body includes note when present`() {
        assertEquals(
            "Version 1.2.3 (42) is available\nFixed login crash",
            newVersionPushBody(
                versionName = "1.2.3",
                versionCode = 42,
                note = "  Fixed login crash  ",
            ),
        )
    }

    @Test
    fun `new version push body keeps existing message when note is blank`() {
        assertEquals(
            "Version 1.2.3 (42) is available",
            newVersionPushBody(
                versionName = "1.2.3",
                versionCode = 42,
                note = "   ",
            ),
        )
    }

    @Test
    fun `new version push body includes build and note when version name is blank`() {
        assertEquals(
            "Build 42 is available\nFixed login crash",
            newVersionPushBody(
                versionName = "",
                versionCode = 42,
                note = "Fixed login crash",
            ),
        )
    }

    @Test
    fun `new version push deep link points at private app version route`() {
        assertEquals(
            "drebin451://app/owner:com.example.app/version-1",
            PushData.appVersionDeepLink("owner:com.example.app", "version-1"),
        )
    }

    @Test
    fun `new version notification deep link uses public release share route`() {
        assertEquals(
            "https://drebin451.com/app/share-token-123/releases/version-1",
            newVersionPushDeepLink("share-token-123", "version-1"),
        )
    }
}
