package com.commit451.drebin451.model

import kotlinx.serialization.Serializable

/** Request/response body for fetching, editing, or clearing an uploaded APK version note. */
@Serializable
data class VersionNote(
    val note: String = "",
)
