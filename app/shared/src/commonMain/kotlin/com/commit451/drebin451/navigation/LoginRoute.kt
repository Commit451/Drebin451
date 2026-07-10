package com.commit451.drebin451.navigation

import kotlinx.serialization.Serializable

@Serializable
internal data class LoginRoute(
    val startOnSignUp: Boolean = false,
) : AppRoute
