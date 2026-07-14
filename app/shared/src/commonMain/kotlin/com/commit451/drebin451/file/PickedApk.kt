package com.commit451.drebin451.file

/**
 * A user-picked APK represented by a platform file handle rather than raw bytes. Keeping the source
 * handle lets Android and web stream files as large as 1 GiB without first duplicating them in RAM.
 */
class PickedApk(
    val fileName: String,
    val sizeBytes: Long,
    internal val sourceId: String,
)
