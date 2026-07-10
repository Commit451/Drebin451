package com.commit451.drebin451.model

object PlanLimits {
    const val FREE_STORAGE_BYTES = 1L * 1024L * 1024L * 1024L
    const val PRO_STORAGE_BYTES = 25L * 1024L * 1024L * 1024L
    const val STORAGE_WARNING_THRESHOLD_BYTES = 50L * 1024L * 1024L

    fun normalized(plan: String): String =
        if (plan == PlanIds.PRO) PlanIds.PRO else PlanIds.FREE

    fun storageQuotaBytes(plan: String): Long =
        when (normalized(plan)) {
            PlanIds.PRO -> PRO_STORAGE_BYTES
            else -> FREE_STORAGE_BYTES
        }
}
