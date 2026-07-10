package com.commit451.drebin451.api

import kotlinx.serialization.Serializable

@Serializable
internal data class ApiErrorResponse(val errorMessage: String)
