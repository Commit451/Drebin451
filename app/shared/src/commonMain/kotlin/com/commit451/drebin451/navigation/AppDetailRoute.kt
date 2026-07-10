package com.commit451.drebin451.navigation

import com.commit451.drebin451.model.App
import kotlinx.serialization.Serializable

@Serializable
internal data class AppDetailRoute(
    val app: App,
) : AppRoute
