package com.commit451.drebin451.util

// Keep JS date formatting dependency-free; ISO minutes are enough for upload timestamps.
actual fun formatDateTime(epochMillis: Long): String =
    if (epochMillis <= 0L) "—"
    else kotlin.js.Date(epochMillis.toDouble()).toISOString().take(16).replace('T', ' ')

internal actual fun currentEpochMillis(): Long = kotlin.js.Date().getTime().toLong()
