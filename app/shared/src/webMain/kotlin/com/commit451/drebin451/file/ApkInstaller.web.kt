package com.commit451.drebin451.file

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.commit451.drebin451.api.Api
import com.commit451.drebin451.model.AppVersion
import kotlinx.coroutines.launch
import kotlin.io.encoding.Base64

/**
 * Web APK action: browsers cannot install Android APKs, so expose the same version action as an
 * authenticated file download instead. Shared across js + wasmJs; the final browser download handoff
 * lives in a tiny per-target bridge to plain JS.
 */
@Composable
actual fun rememberApkInstaller(): ApkInstaller {
    val scope = rememberCoroutineScope()
    var downloadState by remember { mutableStateOf(InstallState()) }

    return remember(scope) {
        object : ApkInstaller {
            override val supported = true
            override val actionLabel = "Download"
            override val busyLabel = "Downloading…"
            override val state: InstallState get() = downloadState

            override fun install(version: AppVersion) {
                if (downloadState.phase == InstallPhase.Downloading ||
                    downloadState.phase == InstallPhase.Installing
                ) {
                    return
                }
                downloadState = InstallState(InstallPhase.Downloading, version.id)
                scope.launch {
                    try {
                        val bytes = Api.downloadVersion(version.appId, version.id)
                        downloadApkRaw(
                            fileName = version.downloadFileName(),
                            contentType = version.contentType.ifBlank { AppVersion.CONTENT_TYPE_APK },
                            base64 = Base64.Default.encode(bytes),
                        )
                        downloadState = InstallState()
                    } catch (t: Throwable) {
                        downloadState = InstallState(
                            InstallPhase.Error,
                            version.id,
                            "Download failed. Check your connection and try again.",
                        )
                    }
                }
            }
        }
    }
}

private fun AppVersion.downloadFileName(): String {
    if (fileName.isNotBlank()) return fileName
    val baseName = applicationId.ifBlank { id.ifBlank { "app" } }
    return if (baseName.endsWith(".apk", ignoreCase = true)) baseName else "$baseName.apk"
}

/**
 * Starts a browser download for the already-authenticated APK bytes. Implemented per web backend
 * because Kotlin/JS and Kotlin/Wasm call JavaScript globals differently.
 */
internal expect fun downloadApkRaw(fileName: String, contentType: String, base64: String)
