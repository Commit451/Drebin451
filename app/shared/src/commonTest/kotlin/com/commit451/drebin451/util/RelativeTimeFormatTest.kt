package com.commit451.drebin451.util

import kotlin.test.Test
import kotlin.test.assertEquals

class RelativeTimeFormatTest {

    private val now = 2 * 365.days

    @Test
    fun relativeTime_formatsMinutesHoursAndDays() {
        assertEquals("15 minutes ago", formatRelativeTime(now - 15.minutes, now))
        assertEquals("one hour ago", formatRelativeTime(now - 1.hours, now))
        assertEquals("23 hours ago", formatRelativeTime(now - 23.hours, now))
        assertEquals("one day ago", formatRelativeTime(now - 1.days, now))
        assertEquals("6 days ago", formatRelativeTime(now - 6.days, now))
    }

    @Test
    fun relativeTime_handlesShortRecentAndInvalidInstants() {
        assertEquals("just now", formatRelativeTime(now - 30.seconds, now))
        assertEquals("one minute ago", formatRelativeTime(now - 1.minutes, now))
        assertEquals("—", formatRelativeTime(0, now))
    }

    @Test
    fun relativeTime_handlesLongerDurations() {
        assertEquals("one week ago", formatRelativeTime(now - 7.days, now))
        assertEquals("3 weeks ago", formatRelativeTime(now - 23.days, now))
        assertEquals("one month ago", formatRelativeTime(now - 30.days, now))
        assertEquals("one year ago", formatRelativeTime(now - 365.days, now))
    }
}

private val Int.seconds: Long get() = this * 1_000L
private val Int.minutes: Long get() = this * 60.seconds
private val Int.hours: Long get() = this * 60.minutes
private val Int.days: Long get() = this * 24.hours
