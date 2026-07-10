package com.commit451.drebin451.model

import kotlinx.serialization.Serializable

/** Server-computed storage quota summary for the signed-in user's upload space. */
@Serializable
data class StorageStatus(
    val plan: String = PlanIds.FREE,
    val usedBytes: Long = 0,
    val limitBytes: Long = PlanLimits.FREE_STORAGE_BYTES,
    val remainingBytes: Long = limitBytes,
    val warningThresholdBytes: Long = PlanLimits.STORAGE_WARNING_THRESHOLD_BYTES,
    val nearLimit: Boolean = false,
    val atLimit: Boolean = false,
)

fun User.storageStatus(): StorageStatus = storageStatusFor(
    plan = plan,
    usedBytes = storageUsedBytes,
)

fun storageStatusFor(
    plan: String,
    usedBytes: Long,
    warningThresholdBytes: Long = PlanLimits.STORAGE_WARNING_THRESHOLD_BYTES,
): StorageStatus {
    val normalizedPlan = PlanLimits.normalized(plan)
    val limitBytes = PlanLimits.storageQuotaBytes(normalizedPlan)
    val normalizedUsedBytes = usedBytes.coerceAtLeast(0)
    val remainingBytes = (limitBytes - normalizedUsedBytes).coerceAtLeast(0)
    val atLimit = normalizedUsedBytes >= limitBytes
    val nearLimit = !atLimit && remainingBytes <= warningThresholdBytes
    return StorageStatus(
        plan = normalizedPlan,
        usedBytes = normalizedUsedBytes,
        limitBytes = limitBytes,
        remainingBytes = remainingBytes,
        warningThresholdBytes = warningThresholdBytes,
        nearLimit = nearLimit,
        atLimit = atLimit,
    )
}
