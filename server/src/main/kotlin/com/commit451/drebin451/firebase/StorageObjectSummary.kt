package com.commit451.drebin451.firebase

import kotlinx.serialization.Serializable

@Serializable
data class StorageObjectSummary(
    val path: String,
    val sizeBytes: Long,
    val kind: String,
)
