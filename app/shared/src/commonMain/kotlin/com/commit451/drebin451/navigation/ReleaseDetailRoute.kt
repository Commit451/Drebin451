package com.commit451.drebin451.navigation

import com.commit451.drebin451.model.App
import com.commit451.drebin451.model.AppVersion
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
internal data class ReleaseDetailRoute(
    val app: App,
    val version: AppVersion,
    val navigationInstanceId: Long = nextNavigationInstanceId(),
) : AppRoute

private fun nextNavigationInstanceId(): Long = Random.nextLong()
