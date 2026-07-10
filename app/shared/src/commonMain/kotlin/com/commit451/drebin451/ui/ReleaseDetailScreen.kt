package com.commit451.drebin451.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.commit451.drebin451.api.Api
import com.commit451.drebin451.file.InstallPhase
import com.commit451.drebin451.file.rememberApkInstaller
import com.commit451.drebin451.model.AppVersion
import com.commit451.drebin451.navigation.AppShareRoute
import com.commit451.drebin451.navigation.LocalAppNavigator
import com.commit451.drebin451.navigation.ReleaseDetailRoute
import com.commit451.drebin451.util.formatDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReleaseDetailScreen(route: ReleaseDetailRoute) {
    val navigator = LocalAppNavigator.current
    var app by remember(route.app.id) { mutableStateOf(route.app) }
    var version by remember(route.version.id) { mutableStateOf(route.version) }
    val installer = rememberApkInstaller()
    val snackbarHostState = remember { SnackbarHostState() }
    val appName = app.label.ifBlank { app.applicationId }
    val releaseTitle = releaseTitle(version)

    LaunchedEffect(route.app.id, route.version.id) {
        runCatching { Api.app(route.app.id) }.getOrNull()?.let { app = it }
        runCatching { Api.appVersion(route.app.id, route.version.id) }.getOrNull()
            ?.let { version = it }
    }

    LaunchedEffect(installer.state) {
        val install = installer.state
        if (install.phase == InstallPhase.Error && install.message.isNotBlank()) {
            snackbarHostState.showSnackbar(install.message)
        }
    }

    DrebinGradientScaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        AppBarTitleText(
                            text = releaseTitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        AppBarSubtitleText(
                            text = appName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = DrebinTopAppBarColors(),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val horizontalMargin = ContentLayout.horizontalMargin(maxWidth)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = horizontalMargin, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                DrebinGradientCard(Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(
                            imageUrl = app.imageUrl,
                            contentDescription = null,
                            size = 56.dp,
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                appName,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                app.applicationId,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                DrebinGradientCard(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(releaseTitle, style = MaterialTheme.typography.headlineSmall)
                        ReleaseMetadataRow(
                            label = "Uploaded",
                            value = formatDateTime(version.createdAt)
                        )
                        ReleaseMetadataRow(
                            label = "APK",
                            value = version.fileName.ifBlank { "Unknown file" })
                        ReleaseMetadataRow(
                            label = "Size",
                            value = formatSize(version.fileSizeBytes)
                        )
                        ReleaseMetadataRow(
                            label = "Package",
                            value = version.applicationId.ifBlank { app.applicationId })
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = { navigator.push(AppShareRoute(app)) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Share app")
                    }
                    if (installer.supported) {
                        val installing = installer.state.versionId == version.id &&
                                (installer.state.phase == InstallPhase.Downloading ||
                                        installer.state.phase == InstallPhase.Installing)
                        Button(
                            onClick = { installer.install(version) },
                            enabled = !installing,
                            modifier = Modifier.weight(1f),
                        ) {
                            if (installing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = LocalContentColor.current,
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(installer.busyLabel)
                            } else {
                                Text(installer.actionLabel)
                            }
                        }
                    }
                }

                DrebinGradientCard(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Notes", style = MaterialTheme.typography.titleMedium)
                        if (version.note.isBlank()) {
                            Text(
                                "No notes for this release.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Text(
                                version.note,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReleaseMetadataRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun releaseTitle(version: AppVersion): String = when {
    version.versionName.isNotBlank() -> "v${version.versionName} (${version.versionCode})"
    version.versionCode > 0 -> "Build ${version.versionCode}"
    else -> version.fileName.ifBlank { "Release" }
}
