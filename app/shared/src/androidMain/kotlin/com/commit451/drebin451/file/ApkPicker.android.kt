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
        val name = context.displayName(uri) ?: "app.apk"
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        onResult(if (bytes == null) null else PickedApk(name, bytes))
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

private fun Context.displayName(uri: Uri): String? =
    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
    }
