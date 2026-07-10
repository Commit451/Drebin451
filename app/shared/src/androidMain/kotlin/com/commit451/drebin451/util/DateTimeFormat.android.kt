package com.commit451.drebin451.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun formatDateTime(epochMillis: Long): String {
    if (epochMillis <= 0L) return "—"
    // Default time zone = the device's local zone.
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(epochMillis))
}

internal actual fun currentEpochMillis(): Long = System.currentTimeMillis()
