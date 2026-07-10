package com.commit451.drebin451.ui

import com.commit451.drebin451.navigation.AppRoute

internal data class SplashState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val canRetry: Boolean = false,
    val shouldNavigate: Boolean = false,
    val loggedOutRoute: AppRoute? = null,
)
