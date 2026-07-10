package com.commit451.drebin451.share

import com.commit451.drebin451.share.ShareLinks.HOST_NAME


/**
 * Builds shareable links to an app. These are Android App Links: opened on a device with Drebin
 * installed they resolve in-app (see [DeepLink] and the manifest intent-filter); [HOST_NAME] must
 * serve `/.well-known/assetlinks.json` for Android to auto-verify them.
 *
 * The id in the URL is the app's random [com.commit451.drebin451.model.App.shareId], not the
 * stable Firestore app id. Anyone signed in who opens the URL gets the app added to their Shared
 * collection.
 */
object ShareLinks {
    const val HOST_NAME = "drebin451.com"
    const val BASE = "https://$HOST_NAME"

    fun appUrl(shareId: String): String {
        require(shareId.isNotBlank()) { "shareId is required" }
        return "$BASE/app/$shareId"
    }

    fun releaseUrl(shareId: String, versionId: String): String {
        require(shareId.isNotBlank()) { "shareId is required" }
        require(versionId.isNotBlank()) { "versionId is required" }
        return "$BASE/app/$shareId/releases/$versionId"
    }
}
