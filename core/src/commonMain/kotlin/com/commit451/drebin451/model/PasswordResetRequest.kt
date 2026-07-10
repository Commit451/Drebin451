package com.commit451.drebin451.model

import kotlinx.serialization.Serializable

@Serializable
data class PasswordResetRequest(
    val email: String,
)
