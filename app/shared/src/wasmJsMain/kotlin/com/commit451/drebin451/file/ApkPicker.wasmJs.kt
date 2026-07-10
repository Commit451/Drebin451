package com.commit451.drebin451.file

import kotlinx.coroutines.await
import kotlin.js.Promise

/**
 * Bridges to `window.drebinPickApk` (index.html), which opens the file dialog and resolves with
 * "<filename>\n<base64 bytes>" (or "" on cancel). Kotlin/Wasm can't reference JS globals directly,
 * hence the [JsFun] shim.
 */
@JsFun("() => drebinPickApk()")
private external fun drebinPickApk(): Promise<JsString>

internal actual suspend fun pickApkRaw(): String = drebinPickApk().await().toString()
