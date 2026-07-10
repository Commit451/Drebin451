package com.commit451.drebin451.model

import kotlinx.serialization.Serializable

/** Request body for `POST /v1/api-keys`: a human-readable [label] to tell keys apart. */
@Serializable
data class CreateApiKeyRequest(
    val label: String = "",
)
