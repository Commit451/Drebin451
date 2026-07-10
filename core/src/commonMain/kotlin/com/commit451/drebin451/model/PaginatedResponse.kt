package com.commit451.drebin451.model

import kotlinx.serialization.Serializable

/**
 * A single cursor-paginated API response. [nextPageToken] is null when there are no more rows.
 */
@Serializable
data class PaginatedResponse<T>(
    val items: List<T> = emptyList(),
    val nextPageToken: String? = null,
)
