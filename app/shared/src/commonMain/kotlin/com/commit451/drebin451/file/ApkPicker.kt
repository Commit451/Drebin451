package com.commit451.drebin451.file

import androidx.compose.runtime.Composable

/**
 * Returns a launcher that opens the system file picker (filtered to APKs). The picked
 * file — or null if the user cancelled — is delivered to [onResult]. Android uses the
 * Storage Access Framework; web uses the JavaScript file-picker bridge from index.html.
 */
@Composable
expect fun rememberApkPicker(onResult: (PickedApk?) -> Unit): () -> Unit
