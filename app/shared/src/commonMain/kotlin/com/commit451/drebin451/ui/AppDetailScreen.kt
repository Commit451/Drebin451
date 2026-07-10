package com.commit451.drebin451.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.commit451.drebin451.auth.UserManager
import com.commit451.drebin451.file.InstallPhase
import com.commit451.drebin451.file.rememberApkInstaller
import com.commit451.drebin451.file.rememberApkPicker
import com.commit451.drebin451.follow.isPushSupported
import com.commit451.drebin451.model.AppVersion
import com.commit451.drebin451.navigation.AppDetailRoute
import com.commit451.drebin451.navigation.AppShareRoute
import com.commit451.drebin451.navigation.LocalAppNavigator
import com.commit451.drebin451.navigation.ReleaseDetailRoute
import com.commit451.drebin451.navigation.SubscriptionRoute
import com.commit451.drebin451.push.RequestNotificationPermission
import com.commit451.drebin451.util.formatRelativeTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppDetailScreen(route: AppDetailRoute) {
    val navigator = LocalAppNavigator.current
    val vm: AppDetailViewModel = viewModel { AppDetailViewModel(route.app) }
    val state by vm.state.collectAsStateWithLifecycle()
    val app = state.app
    val snackbarHostState = remember { SnackbarHostState() }
    val launchPicker = rememberApkPicker { picked -> picked?.let { vm.upload(it) } }
    val installer = rememberApkInstaller()
    // Owner-only actions (upload/delete) are hidden for shared apps — any signed-in user can view
    // and install/download, but only the owner manages uploads. Shared users can remove the app from
    // their own Shared collection.
    val currentUid = UserManager.current()?.uid
    val isOwner = app.ownerUserId == currentUid
    // Following is available wherever push is (Android) — owners included, since uploads can come
    // from CI / the API, not just from this device. Hidden on web (no push there).
    val showFollow = isPushSupported()
    var requestNotifPermission by remember { mutableStateOf(false) }
    var appMenuExpanded by remember { mutableStateOf(false) }
    var confirmDeleteApp by remember { mutableStateOf(false) }
    var confirmDeleteShared by remember { mutableStateOf(false) }
    var versionPendingDelete by remember { mutableStateOf<AppVersion?>(null) }
    var versionPendingNoteEdit by remember { mutableStateOf<AppVersion?>(null) }
    var versionPendingNoteDelete by remember { mutableStateOf<AppVersion?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            vm.messageShown()
        }
    }

    // Surface install failures (download error, or the unknown-sources permission left off).
    LaunchedEffect(installer.state) {
        val install = installer.state
        if (install.phase == InstallPhase.Error && install.message.isNotBlank()) {
            snackbarHostState.showSnackbar(install.message)
        }
    }

    // The app itself is gone — leave the now-empty detail screen.
    LaunchedEffect(state.deleted) {
        if (state.deleted) navigator.pop()
    }

    // Returning from the share screen may have refreshed the app's shareId; pick it up before the
    // overflow menu can open another share screen with a stale URL.
    LifecycleResumeEffect(Unit) {
        vm.refreshApp()
        onPauseOrDispose { }
    }

    DrebinGradientScaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(
                            imageUrl = app.imageUrl,
                            contentDescription = null,
                            size = 36.dp,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            AppBarTitleText(
                                text = app.label.ifBlank { app.applicationId },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            AppBarSubtitleText(
                                text = app.applicationId,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (showFollow) {
                        IconButton(
                            onClick = {
                                if (state.following) {
                                    vm.toggleFollow()
                                } else {
                                    // Opting in to notifications — request the permission first.
                                    requestNotifPermission = true
                                }
                            },
                            enabled = !state.followBusy,
                        ) {
                            // A spinner while the subscribe/unsubscribe is in flight makes "working"
                            // distinct from "broken" — otherwise the button just greys out. The icon
                            // shows the resulting state: a bell when following, a struck-through bell
                            // when not.
                            if (state.followBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    imageVector = if (state.following) {
                                        Icons.Filled.Notifications
                                    } else {
                                        Icons.Filled.NotificationsOff
                                    },
                                    contentDescription = if (state.following) "Unfollow" else "Follow",
                                )
                            }
                        }
                    }
                    Box {
                        IconButton(onClick = { appMenuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "App options")
                        }
                        DropdownMenu(
                            expanded = appMenuExpanded,
                            onDismissRequest = { appMenuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Share app") },
                                onClick = {
                                    appMenuExpanded = false
                                    navigator.push(AppShareRoute(app))
                                },
                            )
                            if (isOwner) {
                                DropdownMenuItem(
                                    text = { Text("Delete app") },
                                    onClick = {
                                        appMenuExpanded = false
                                        confirmDeleteApp = true
                                    },
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Delete from Shared") },
                                    onClick = {
                                        appMenuExpanded = false
                                        confirmDeleteShared = true
                                    },
                                )
                            }
                        }
                    }
                },
                colors = DrebinTopAppBarColors(),
            )
        },
        floatingActionButton = {
            if (isOwner) {
                ExtendedFloatingActionButton(
                    onClick = { if (!state.uploading) launchPicker() },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(if (state.uploading) "Uploading…" else "Upload version") },
                    shape = DrebinFabShape,
                    containerColor = DrebinFabContainerColor(),
                    contentColor = DrebinFabContentColor(),
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val horizontalMargin = ContentLayout.horizontalMargin(maxWidth)
            if (state.uploading) {
                LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
            if (state.loading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                PullToRefreshBox(
                    isRefreshing = state.refreshing,
                    onRefresh = { vm.refresh() },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (state.versions.isEmpty()) {
                        EmptyVersions()
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                horizontal = horizontalMargin,
                                vertical = 16.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(state.versions, key = { it.id }) { version ->
                                VersionCard(
                                    version = version,
                                    canDelete = isOwner,
                                    canEditNote = isOwner,
                                    installSupported = installer.supported,
                                    installActionLabel = installer.actionLabel,
                                    installBusyLabel = installer.busyLabel,
                                    installing = installer.state.versionId == version.id &&
                                            (installer.state.phase == InstallPhase.Downloading ||
                                                    installer.state.phase == InstallPhase.Installing),
                                    onOpen = {
                                        navigator.push(
                                            ReleaseDetailRoute(
                                                app = app,
                                                version = version
                                            )
                                        )
                                    },
                                    onInstall = { installer.install(version) },
                                    onEditNote = { versionPendingNoteEdit = version },
                                    onDeleteNote = { versionPendingNoteDelete = version },
                                    onDelete = { versionPendingDelete = version },
                                )
                            }
                            if (state.nextPageToken != null) {
                                item(key = "load-more-versions") {
                                    LaunchedEffect(state.nextPageToken) { vm.loadMore() }
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (requestNotifPermission) {
        RequestNotificationPermission { _ ->
            requestNotifPermission = false
            // Follow regardless of the outcome: the topic subscription works even without the
            // permission; notifications just won't display until it's granted in settings.
            vm.toggleFollow()
        }
    }

    state.storageLimitUploadDialogMessage?.let { message ->
        StorageLimitUploadDialog(
            message = message,
            onUpgrade = {
                vm.storageLimitUploadDialogShown()
                navigator.push(SubscriptionRoute)
            },
            onDismiss = vm::storageLimitUploadDialogShown,
        )
    }

    if (confirmDeleteApp) {
        val name = app.label.ifBlank { app.applicationId }
        val count = app.versionCount
        val versionsLabel = if (count == 1) "1 version" else "$count versions"
        AlertDialog(
            onDismissRequest = { confirmDeleteApp = false },
            title = { Text("Delete app?") },
            text = {
                Text("This permanently deletes \"$name\" and its $versionsLabel. This can't be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteApp = false
                    vm.deleteApp()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteApp = false }) { Text("Cancel") }
            },
        )
    }

    if (confirmDeleteShared) {
        val name = app.label.ifBlank { app.applicationId }
        AlertDialog(
            onDismissRequest = { confirmDeleteShared = false },
            title = { Text("Delete shared app?") },
            text = {
                Text("\"$name\" will be removed from your Shared tab. The owner's app is untouched.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteShared = false
                    vm.deleteSharedApp()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteShared = false }) { Text("Cancel") }
            },
        )
    }

    versionPendingDelete?.let { version ->
        val name = version.versionName.ifBlank { version.fileName }
        AlertDialog(
            onDismissRequest = { versionPendingDelete = null },
            title = { Text("Delete version?") },
            text = { Text("This permanently deletes \"$name\". This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteVersion(version)
                    versionPendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { versionPendingDelete = null }) { Text("Cancel") }
            },
        )
    }

    versionPendingNoteEdit?.let { version ->
        EditVersionNoteDialog(
            version = version,
            onSave = { note ->
                vm.updateVersionNote(version, note)
                versionPendingNoteEdit = null
            },
            onDismiss = { versionPendingNoteEdit = null },
        )
    }

    versionPendingNoteDelete?.let { version ->
        val name = version.versionName.ifBlank { version.fileName }
        AlertDialog(
            onDismissRequest = { versionPendingNoteDelete = null },
            title = { Text("Delete note?") },
            text = { Text("This clears the note for \"$name\". The APK version will stay available.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteVersionNote(version)
                    versionPendingNoteDelete = null
                }) { Text("Delete note") }
            },
            dismissButton = {
                TextButton(onClick = { versionPendingNoteDelete = null }) { Text("Cancel") }
            },
        )
    }

}

@Composable
private fun EditVersionNoteDialog(
    version: AppVersion,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var note by remember(version.id, version.note) { mutableStateOf(version.note) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (version.note.isBlank()) "Add note" else "Edit note") },
        text = {
            Column {
                Text("Notes are shown on this APK version and can be changed later.")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(note) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun VersionCard(
    version: AppVersion,
    canDelete: Boolean,
    canEditNote: Boolean,
    installSupported: Boolean,
    installActionLabel: String,
    installBusyLabel: String,
    installing: Boolean,
    onOpen: () -> Unit,
    onInstall: () -> Unit,
    onEditNote: () -> Unit,
    onDeleteNote: () -> Unit,
    onDelete: () -> Unit,
) {
    val title = when {
        version.versionName.isNotBlank() -> "v${version.versionName} (${version.versionCode})"
        version.versionCode > 0 -> "Build ${version.versionCode}"
        else -> version.fileName
    }
    var menuExpanded by remember { mutableStateOf(false) }
    DrebinGradientCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpen,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (canEditNote || canDelete) {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreHoriz, contentDescription = "Version options")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            if (canEditNote) {
                                DropdownMenuItem(
                                    text = { Text(if (version.note.isBlank()) "Add note" else "Edit note") },
                                    onClick = {
                                        menuExpanded = false
                                        onEditNote()
                                    },
                                )
                                if (version.note.isNotBlank()) {
                                    DropdownMenuItem(
                                        text = { Text("Delete note") },
                                        onClick = {
                                            menuExpanded = false
                                            onDeleteNote()
                                        },
                                    )
                                }
                            }
                            if (canDelete) {
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    onClick = {
                                        menuExpanded = false
                                        onDelete()
                                    },
                                )
                            }
                        }
                    }
                }
            }
            // Optional note (CI sets it to the commit message); only shown when present.
            if (version.note.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    version.note,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(2.dp))
            // Created time is the API/Firestore timestamp for this uploaded APK row.
            Text(
                "Created ${formatRelativeTime(version.createdAt)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${version.fileName} • ${formatSize(version.fileSizeBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (installSupported) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Android installs; web downloads because browsers can't install APKs directly.
                    Button(onClick = onInstall, enabled = !installing) {
                        if (installing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = LocalContentColor.current,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(installBusyLabel)
                        } else {
                            Text(installActionLabel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyVersions(modifier: Modifier = Modifier) {
    // Scrollable so the pull-to-refresh gesture registers even when there are no versions to show.
    Box(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No versions yet", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Tap \"Upload version\" to add one.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
