package com.commit451.drebin451.file

import kotlinx.coroutines.await
import kotlin.js.Promise

/**
 * `window.drebinPickApk`, installed in index.html — opens the file dialog, reads the chosen APK,
 * and resolves with "<filename>\n<base64 bytes>" (or "" if the dialog was dismissed).
 */
private external fun drebinPickApk(): Promise<String>

internal actual suspend fun pickApkRaw(): String = drebinPickApk().await()
