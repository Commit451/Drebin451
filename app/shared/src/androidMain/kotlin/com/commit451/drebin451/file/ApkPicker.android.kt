package com.commit451.drebin451.file

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberApkPicker(onResult: (PickedApk?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri == null) {
            onResult(null)
            return@rememberLauncherForActivityResult
        }
        val metadata = context.apkMetadata(uri)
        initializeApkUpload(context.applicationContext.contentResolver)
        onResult(
            PickedApk(
                fileName = metadata.fileName,
                sizeBytes = metadata.sizeBytes,
                sourceId = uri.toString(),
            ),
        )
    }
    return {
        // Some providers report APKs as octet-stream/zip, so accept those too.
        launcher.launch(
            arrayOf(
                "application/vnd.android.package-archive",
                "application/octet-stream",
                "application/zip",
            ),
        )
    }
}

private data class ApkMetadata(val fileName: String, val sizeBytes: Long)

private fun Context.apkMetadata(uri: Uri): ApkMetadata {
    var fileName = "app.apk"
    var sizeBytes = -1L
    contentResolver.query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
        null,
        null,
        null,
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0) fileName = cursor.getString(nameIndex)?.ifBlank { "app.apk" } ?: "app.apk"
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) sizeBytes = cursor.getLong(sizeIndex)
        }
    }
    if (sizeBytes < 0) {
        sizeBytes = contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
    }
    return ApkMetadata(fileName, sizeBytes)
}
