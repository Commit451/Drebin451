package com.commit451.drebin451.firebase

import kotlinx.serialization.Serializable

@Serializable
internal data class FirebasePasswordResetRequest(
    val requestType: String,
    val email: String,
)
