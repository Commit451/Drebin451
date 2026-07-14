package com.commit451.drebin451

import com.commit451.drebin451.model.AppVersion
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.copyTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/** Largest APK accepted by Drebin451: exactly 1 GiB. */
internal const val MAX_APK_UPLOAD_BYTES = AppVersion.MAX_FILE_SIZE_BYTES

internal fun requireApkUploadSize(byteCount: Long) {
    require(byteCount > 0) { "The APK file is empty." }
    require(byteCount <= MAX_APK_UPLOAD_BYTES) { "APK files may not exceed 1 GiB." }
}

/**
 * Streams one multipart file part to the container's temporary filesystem. Reading one byte past
 * [maxBytes] makes an oversized upload fail without buffering the request in the JVM heap.
 */
internal suspend fun copyUploadToFile(
    source: ByteReadChannel,
    target: File,
    maxBytes: Long = MAX_APK_UPLOAD_BYTES,
): Long = withContext(Dispatchers.IO) {
    require(maxBytes > 0) { "Maximum upload size must be positive." }
    val copied = FileOutputStream(target, false).channel.use { output ->
        source.copyTo(output, limit = maxBytes + 1)
    }
    require(copied > 0) { "The APK file is empty." }
    if (copied > maxBytes) {
        val cause = IllegalArgumentException("APK files may not exceed 1 GiB.")
        source.cancel(cause)
        throw cause
    }
    copied
}
