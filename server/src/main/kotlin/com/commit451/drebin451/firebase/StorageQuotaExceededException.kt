package com.commit451.drebin451.firebase

import com.commit451.drebin451.model.PlanIds
import com.commit451.drebin451.model.PlanLimits

class StorageQuotaExceededException(
    val plan: String,
    val usedBytes: Long,
    val attemptedBytes: Long,
    val limitBytes: Long,
) : IllegalStateException(quotaMessage(plan, usedBytes, attemptedBytes, limitBytes))

private fun quotaMessage(
    plan: String,
    usedBytes: Long,
    attemptedBytes: Long,
    limitBytes: Long
): String {
    val planLabel = if (PlanLimits.normalized(plan) == PlanIds.PRO) "Pro" else "Free"
    val base = "Storage limit exceeded. $planLabel plan uses ${formatSize(usedBytes)} of " +
            "${formatSize(limitBytes)}; this upload needs ${formatSize(attemptedBytes)}."
    return if (PlanLimits.normalized(plan) == PlanIds.FREE) {
        "$base Upgrade to Pro for ${formatSize(PlanLimits.PRO_STORAGE_BYTES)}."
    } else {
        base
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = listOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    val rounded = (value * 10).toLong() / 10.0
    return "$rounded ${units[unit]}"
}
