package com.commit451.drebin451.file

import android.content.ContentResolver
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

private const val MAX_RESPONSE_BYTES = 1024 * 1024
private lateinit var apkContentResolver: ContentResolver

internal fun initializeApkUpload(contentResolver: ContentResolver) {
    apkContentResolver = contentResolver
}

actual suspend fun uploadPickedApk(
    picked: PickedApk,
    uploadUrl: String,
    bearerToken: String,
    retainOnUnauthorized: Boolean,
): PlatformUploadResponse = coroutineScope {
    val connection = URL(uploadUrl).openConnection() as HttpURLConnection
    // This sibling reacts to parent cancellation even while blocking URLConnection I/O is in progress.
    val cancellationWatcher = launch(Dispatchers.IO) {
        try {
            awaitCancellation()
        } finally {
            connection.disconnect()
        }
    }

    try {
        withContext(Dispatchers.IO) {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 30_000
            connection.readTimeout = 3_600_000
            if (picked.sizeBytes > 0) {
                connection.setFixedLengthStreamingMode(picked.sizeBytes)
            } else {
                connection.setChunkedStreamingMode(256 * 1024)
            }
            connection.setRequestProperty("Authorization", "Bearer $bearerToken")
            connection.setRequestProperty("Content-Type", "application/vnd.android.package-archive")
            connection.setRequestProperty(
                "X-Apk-File-Name-Base64",
                Base64.encodeToString(picked.fileName.encodeToByteArray(), Base64.NO_WRAP),
            )

            connection.outputStream.buffered(256 * 1024).use { output ->
                val uri = Uri.parse(picked.sourceId)
                val input = apkContentResolver.openInputStream(uri)
                    ?: throw IllegalArgumentException("Could not open the selected APK.")
                input.use {
                    val buffer = ByteArray(256 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                    }
                }
            }

            val statusCode = connection.responseCode
            val responseStream = if (statusCode >= 400) connection.errorStream else connection.inputStream
            PlatformUploadResponse(
                statusCode = statusCode,
                body = responseStream?.use { it.readUtf8Limited(MAX_RESPONSE_BYTES) }.orEmpty(),
            )
        }
    } finally {
        cancellationWatcher.cancelAndJoin()
        connection.disconnect()
    }
}

actual suspend fun discardPickedApk(picked: PickedApk) = Unit

private fun InputStream.readUtf8Limited(maxBytes: Int): String {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        require(output.size() + read <= maxBytes) { "Drebin451 response is too large." }
        output.write(buffer, 0, read)
    }
    return output.toByteArray().decodeToString()
}
