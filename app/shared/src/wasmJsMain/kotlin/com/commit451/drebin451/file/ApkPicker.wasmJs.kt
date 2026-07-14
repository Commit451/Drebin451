@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.commit451.drebin451.file

import kotlinx.coroutines.await
import kotlin.js.Promise

@JsFun("() => drebinPickApk()")
private external fun drebinPickApk(): Promise<JsString>

@JsFun("(sourceId, uploadUrl, bearerToken, retainOnUnauthorized) => drebinUploadPickedApk(sourceId, uploadUrl, bearerToken, retainOnUnauthorized)")
private external fun drebinUploadPickedApk(
    sourceId: JsString,
    uploadUrl: JsString,
    bearerToken: JsString,
    retainOnUnauthorized: Boolean,
): Promise<JsString>

@JsFun("(sourceId) => drebinDiscardPickedApk(sourceId)")
private external fun drebinDiscardPickedApk(sourceId: JsString)

internal actual suspend fun pickApkRaw(): String = drebinPickApk().await().toString()

internal actual suspend fun uploadPickedApkRaw(
    sourceId: String,
    uploadUrl: String,
    bearerToken: String,
    retainOnUnauthorized: Boolean,
): String = drebinUploadPickedApk(
    sourceId.toJsString(),
    uploadUrl.toJsString(),
    bearerToken.toJsString(),
    retainOnUnauthorized,
).await().toString()

internal actual suspend fun discardPickedApkRaw(sourceId: String) {
    drebinDiscardPickedApk(sourceId.toJsString())
}
