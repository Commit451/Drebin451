package com.commit451.drebin451.file

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.commit451.drebin451.api.Api
import com.commit451.drebin451.model.AppVersion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
actual fun rememberApkInstaller(): ApkInstaller {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var installState by remember { mutableStateOf(InstallState()) }
    // The version waiting on the "install unknown apps" settings round trip (null when none).
    var pendingPermission by remember { mutableStateOf<AppVersion?>(null) }

    // Returning from the system "allow from this source" screen: install if the user granted it,
    // otherwise surface why nothing happened.
    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        val version = pendingPermission
        pendingPermission = null
        if (version != null) {
            if (context.canInstallApks()) {
                installState = InstallState(InstallPhase.Downloading, version.id)
                scope.startInstall(context, version) { installState = it }
            } else {
                installState = InstallState(
                    InstallPhase.Error,
                    version.id,
                    "Allow installing apps from Drebin451 to continue.",
                )
            }
        }
    }

    return remember(context, scope) {
        object : ApkInstaller {
            override val supported = true
            override val actionLabel = "Install"
            override val busyLabel = "Installing…"
            override val state: InstallState get() = installState

            override fun install(version: AppVersion) {
                // Ignore taps while a download/handoff is already running.
                if (installState.phase == InstallPhase.Downloading ||
                    installState.phase == InstallPhase.Installing
                ) {
                    return
                }
                if (context.canInstallApks()) {
                    installState = InstallState(InstallPhase.Downloading, version.id)
                    scope.startInstall(context, version) { installState = it }
                } else {
                    // Send the user to enable "install unknown apps", then retry on return.
                    pendingPermission = version
                    settingsLauncher.launch(unknownSourcesSettingsIntent(context))
                }
            }
        }
    }
}

private fun CoroutineScope.startInstall(
    context: Context,
    version: AppVersion,
    onState: (InstallState) -> Unit,
) {
    launch {
        val file = try {
            withContext(Dispatchers.IO) {
                val dir = File(context.cacheDir, "apks").apply { mkdirs() }
                // One file per version id, so re-taps overwrite instead of piling up.
                val out = File(dir, "${version.id}.apk")
                out.writeBytes(Api.downloadVersion(version.appId, version.id))
                out
            }
        } catch (t: Throwable) {
            onState(
                InstallState(
                    InstallPhase.Error,
                    version.id,
                    "Download failed. Check your connection and try again.",
                ),
            )
            return@launch
        }
        try {
            onState(InstallState(InstallPhase.Installing, version.id))
            launchInstaller(context, file)
            // The system installer is now in front; reset so the button returns to the idle label.
            onState(InstallState())
        } catch (t: Throwable) {
            onState(InstallState(InstallPhase.Error, version.id, "Couldn't start the installer."))
        }
    }
}

/** Hands the downloaded APK to the system package installer via a FileProvider content URI. */
private fun launchInstaller(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, AppVersion.CONTENT_TYPE_APK)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(intent)
}

/**
 * Whether this app may install APKs. On API 26+ it's a per-app grant we can check; below that it's a
 * single global toggle the system itself prompts for during install, so we just proceed.
 */
private fun Context.canInstallApks(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        packageManager.canRequestPackageInstalls()
    } else {
        true
    }

private fun unknownSourcesSettingsIntent(context: Context): Intent =
    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
