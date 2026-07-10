package com.commit451.drebin451.firebase

import kotlinx.serialization.Serializable

@Serializable
data class StorageReconciliationReport(
    val dryRun: Boolean,
    val scannedObjectCount: Int,
    val scannedBytes: Long,
    val expectedObjectCount: Int,
    val orphanCount: Int,
    val orphanBytes: Long,
    val missingCount: Int,
    val deletedOrphanCount: Int,
    val failedDeleteCount: Int,
    val orphans: List<StorageObjectSummary>,
    val missing: List<String>,
    val truncated: Boolean,
)
