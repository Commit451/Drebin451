package com.commit451.drebin451.file

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val pickerJson = Json { ignoreUnknownKeys = true }

@Serializable
private data class WebPickedApk(
    val token: String,
    val name: String,
    val size: Long,
)

/**
 * Web APK picker shared across js + wasmJs. Plain JavaScript keeps the browser File object in a
 * short-lived token map; Kotlin receives metadata only, so selecting a large APK does not base64 or
 * duplicate the file in memory.
 */
@Composable
actual fun rememberApkPicker(onResult: (PickedApk?) -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    return {
        scope.launch {
            val picked = runCatching { parsePickedApk(pickApkRaw()) }.getOrNull()
            onResult(picked)
        }
    }
}

internal expect suspend fun pickApkRaw(): String
internal expect suspend fun uploadPickedApkRaw(
    sourceId: String,
    uploadUrl: String,
    bearerToken: String,
    retainOnUnauthorized: Boolean,
): String
internal expect suspend fun discardPickedApkRaw(sourceId: String)

private fun parsePickedApk(raw: String): PickedApk? {
    if (raw.isBlank()) return null
    val metadata = pickerJson.decodeFromString<WebPickedApk>(raw)
    return PickedApk(
        fileName = metadata.name.ifBlank { "app.apk" },
        sizeBytes = metadata.size,
        sourceId = metadata.token,
    )
}

actual suspend fun uploadPickedApk(
    picked: PickedApk,
    uploadUrl: String,
    bearerToken: String,
    retainOnUnauthorized: Boolean,
): PlatformUploadResponse = try {
    pickerJson.decodeFromString(
        uploadPickedApkRaw(picked.sourceId, uploadUrl, bearerToken, retainOnUnauthorized),
    )
} catch (t: Throwable) {
    // Promise cancellation/network failure must abort fetch and release the retained browser File.
    discardPickedApkRaw(picked.sourceId)
    throw t
}

actual suspend fun discardPickedApk(picked: PickedApk) {
    discardPickedApkRaw(picked.sourceId)
}
