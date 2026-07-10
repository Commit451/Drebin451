package com.commit451.drebin451.util

/**
 * Formats an epoch-millisecond instant as a short, human-readable date-time — used to tell
 * apart uploads (e.g. two uploads of the same app version). Android renders it in the
 * device's local time zone; web targets use lightweight platform-specific fallbacks.
 */
expect fun formatDateTime(epochMillis: Long): String

/** Formats an epoch-millisecond instant as relative time, e.g. "15 minutes ago". */
fun formatRelativeTime(epochMillis: Long, nowMillis: Long = currentEpochMillis()): String {
    if (epochMillis <= 0L) return "—"

    val elapsedMillis = (nowMillis - epochMillis).coerceAtLeast(0L)
    val elapsedSeconds = elapsedMillis / MILLIS_PER_SECOND
    if (elapsedSeconds < SECONDS_PER_MINUTE) return "just now"

    val elapsedMinutes = elapsedSeconds / SECONDS_PER_MINUTE
    if (elapsedMinutes < MINUTES_PER_HOUR) return relativeQuantity(elapsedMinutes, "minute")

    val elapsedHours = elapsedMinutes / MINUTES_PER_HOUR
    if (elapsedHours < HOURS_PER_DAY) return relativeQuantity(elapsedHours, "hour")

    val elapsedDays = elapsedHours / HOURS_PER_DAY
    if (elapsedDays < DAYS_PER_WEEK) return relativeQuantity(elapsedDays, "day")

    val elapsedWeeks = elapsedDays / DAYS_PER_WEEK
    if (elapsedDays < DAYS_PER_MONTH) return relativeQuantity(elapsedWeeks, "week")

    val elapsedMonths = elapsedDays / DAYS_PER_MONTH
    if (elapsedDays < DAYS_PER_YEAR) return relativeQuantity(elapsedMonths, "month")

    return relativeQuantity(elapsedDays / DAYS_PER_YEAR, "year")
}

internal expect fun currentEpochMillis(): Long

private fun relativeQuantity(value: Long, unit: String): String =
    if (value == 1L) "one $unit ago" else "$value ${unit}s ago"

private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
private const val MINUTES_PER_HOUR = 60L
private const val HOURS_PER_DAY = 24L
private const val DAYS_PER_WEEK = 7L
private const val DAYS_PER_MONTH = 30L
private const val DAYS_PER_YEAR = 365L
