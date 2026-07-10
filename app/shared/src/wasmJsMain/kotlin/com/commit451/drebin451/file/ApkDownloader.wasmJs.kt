@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.commit451.drebin451.file

/** Bridges to `window.drebinDownloadApk` in index.html. */
@JsFun("(fileName, contentType, base64) => { window.drebinDownloadApk(fileName, contentType, base64); }")
private external fun jsDownloadApk(fileName: String, contentType: String, base64: String)

internal actual fun downloadApkRaw(fileName: String, contentType: String, base64: String) {
    jsDownloadApk(fileName, contentType, base64)
}
