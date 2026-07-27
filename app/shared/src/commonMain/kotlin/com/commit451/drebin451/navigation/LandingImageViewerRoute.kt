package com.commit451.drebin451.navigation

import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
internal data class LandingImageViewerRoute(
    val screenshot: LandingScreenshot,
    val navigationInstanceId: Long = nextLandingImageViewerInstanceId(),
) : AppRoute

private fun nextLandingImageViewerInstanceId(): Long = Random.nextLong()

@Serializable
internal enum class LandingScreenshot {
    AppsList,
    AppReleases,
    ReleaseDetails,
}
