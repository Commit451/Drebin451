package com.commit451.drebin451.model

import kotlinx.serialization.Serializable

/** One bounded server-side build deletion request. Larger UI selections are sent in chunks. */
@Serializable
data class BatchDeleteVersionsRequest(
    val versionIds: List<String> = emptyList(),
) {
    companion object {
        const val MAX_VERSION_IDS = 100
    }
}

/** Reports exactly which requested build IDs were deleted or were already absent. */
@Serializable
data class BatchDeleteVersionsResponse(
    val deletedVersionIds: List<String> = emptyList(),
    val missingVersionIds: List<String> = emptyList(),
)
