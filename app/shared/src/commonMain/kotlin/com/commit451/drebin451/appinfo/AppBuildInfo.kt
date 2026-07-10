package com.commit451.drebin451.appinfo

internal data class AppBuildInfo(
    val versionName: String,
    val versionCode: Int,
    val commitHash: String,
) {
    val versionLabel: String
        get() {
            val name = versionName.trim().ifBlank { "unknown" }
            return if (versionCode > 0) "$name ($versionCode)" else name
        }

    val commitHashLabel: String
        get() = commitHash.trim().ifBlank { "unknown" }
}

internal fun currentAppBuildInfo(): AppBuildInfo = AppBuildInfo(
    versionName = GeneratedAppBuildInfo.VERSION_NAME,
    versionCode = GeneratedAppBuildInfo.VERSION_CODE,
    commitHash = GeneratedAppBuildInfo.COMMIT_HASH,
)
