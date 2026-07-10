package com.commit451.drebin451.file

/** Bridges to `window.drebinDownloadApk` in index.html. */
private external fun drebinDownloadApk(fileName: String, contentType: String, base64: String)

internal actual fun downloadApkRaw(fileName: String, contentType: String, base64: String) {
    drebinDownloadApk(fileName, contentType, base64)
}
