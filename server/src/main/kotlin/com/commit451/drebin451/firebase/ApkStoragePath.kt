package com.commit451.drebin451.firebase

internal data class ApkStoragePath(
    val ownerUserId: String,
    val applicationId: String,
    val versionId: String,
    val fileName: String,
)
