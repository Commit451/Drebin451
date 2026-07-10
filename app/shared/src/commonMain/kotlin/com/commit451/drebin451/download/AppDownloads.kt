package com.commit451.drebin451.download

internal const val DREBIN451_APK_RELEASE_ASSET_NAME = "Drebin451.apk"

// Keep this asset name in sync with .github/workflows/deploy.yml.
internal const val DREBIN451_LATEST_APK_DOWNLOAD_URL =
    "https://github.com/Commit451/Drebin451/releases/latest/download/Drebin451.apk"

internal expect fun shouldShowDrebin451ApkDownload(): Boolean
