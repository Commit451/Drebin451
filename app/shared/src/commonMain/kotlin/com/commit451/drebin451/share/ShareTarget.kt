package com.commit451.drebin451.share

/** A public share link target, identified by a random share id. */
sealed interface ShareTarget : DeepLinkTarget {
    val shareId: String

    data class App(override val shareId: String) : ShareTarget
    data class Release(override val shareId: String, val versionId: String) : ShareTarget
}
