package com.commit451.drebin451.file

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.commit451.drebin451.model.AppVersion

/**
 * Handles the primary APK action for a version. Android downloads and opens the package installer;
 * web downloads the APK file because browsers cannot install Android apps. Tracks a single
 * in-flight action in [state] so the UI can show progress on the right version's button. Mirrors the
 * [rememberApkPicker] / [rememberShareLauncher] expect/actual pattern.
 */
@Stable
interface ApkInstaller {
    /** True where the UI should show an APK action for each version. */
    val supported: Boolean

    /** Idle button label for the platform action: "Install" on Android, "Download" on web. */
    val actionLabel: String

    /** Busy button label while [state] is [InstallPhase.Downloading] or [InstallPhase.Installing]. */
    val busyLabel: String

    /** Snapshot-backed install state; reading it in composition recomposes as it changes. */
    val state: InstallState

    /**
     * Runs the platform APK action for [version]. Android downloads and launches the system
     * installer; web downloads the APK file. No-op while an action is already in flight, and on
     * platforms where [supported] is false.
     */
    fun install(version: AppVersion)
}

@Composable
expect fun rememberApkInstaller(): ApkInstaller
