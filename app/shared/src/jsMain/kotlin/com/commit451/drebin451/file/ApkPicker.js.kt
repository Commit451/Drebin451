package com.commit451.drebin451.file

import kotlinx.coroutines.await
import kotlin.js.Promise

/** Browser bridges installed by index.html for retaining and streaming the selected File object. */
private external fun drebinPickApk(): Promise<String>
private external fun drebinUploadPickedApk(
    sourceId: String,
    uploadUrl: String,
    bearerToken: String,
    retainOnUnauthorized: Boolean,
): Promise<String>
private external fun drebinDiscardPickedApk(sourceId: String)

internal actual suspend fun pickApkRaw(): String = drebinPickApk().await()

internal actual suspend fun uploadPickedApkRaw(
    sourceId: String,
    uploadUrl: String,
    bearerToken: String,
    retainOnUnauthorized: Boolean,
): String = drebinUploadPickedApk(sourceId, uploadUrl, bearerToken, retainOnUnauthorized).await()

internal actual suspend fun discardPickedApkRaw(sourceId: String) {
    drebinDiscardPickedApk(sourceId)
}
