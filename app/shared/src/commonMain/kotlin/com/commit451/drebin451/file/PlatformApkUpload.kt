package com.commit451.drebin451.file

import kotlinx.serialization.Serializable

@Serializable
data class PlatformUploadResponse(
    val statusCode: Int,
    val body: String,
)

/** Uploads the selected platform file as multipart form data without loading it all into memory. */
expect suspend fun uploadPickedApk(
    picked: PickedApk,
    uploadUrl: String,
    bearerToken: String,
    retainOnUnauthorized: Boolean,
): PlatformUploadResponse

/** Releases platform resources when validation fails or an upload coroutine is cancelled. */
expect suspend fun discardPickedApk(picked: PickedApk)
