package com.commit451.drebin451.share

/** A private push-notification target, identified by stable app/version ids. */
data class AppVersionDeepLinkTarget(
    val appId: String,
    val versionId: String,
) : DeepLinkTarget
