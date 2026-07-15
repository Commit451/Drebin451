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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.commit451.drebin451.auth.UserManager
import com.commit451.drebin451.file.rememberApkPicker
import com.commit451.drebin451.model.App
import com.commit451.drebin451.navigation.AppDetailRoute
import com.commit451.drebin451.navigation.AppShareRoute
import com.commit451.drebin451.navigation.DebugRoute
import com.commit451.drebin451.navigation.LocalAppNavigator
import com.commit451.drebin451.navigation.SettingsRoute
import com.commit451.drebin451.navigation.SubscriptionRoute
import com.commit451.drebin451.push.RequestNotificationPermission
import com.commit451.drebin451.util.formatDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val navigator = LocalAppNavigator.current
    val vm: HomeViewModel = viewModel { HomeViewModel() }
    val state by vm.state.collectAsStateWithLifecycle()
    val user by UserManager.user.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val launchPicker = rememberApkPicker { picked -> picked?.let { vm.upload(it) } }
    var selectedTab by rememberSaveable { mutableStateOf(HomeTab.Yours) }
    var sharedAppPendingDelete by remember { mutableStateOf<App?>(null) }
    var notificationPermissionApp by remember { mutableStateOf<App?>(null) }
    var overflowExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            vm.messageShown()
        }
    }

    // Re-fetch whenever Home comes back to the foreground so a delete made on an app's detail
    // screen is reflected here without a spinner flash.
    LifecycleResumeEffect(Unit) {
        vm.silentRefresh()
        onPauseOrDispose { }
    }

    DrebinGradientScaffold(
        topBar = {
            TopAppBar(
                title = {
                    AppBarTitleText(
                        text = "Drebin451",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                actions = {
                    Box {
                        IconButton(onClick = { overflowExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = overflowExpanded,
                            onDismissRequest = { overflowExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                onClick = {
                                    overflowExpanded = false
                                    navigator.push(SettingsRoute)
                                },
                            )
                            if (user?.admin == true) {
                                DropdownMenuItem(
                                    text = { Text("Debug") },
                                    onClick = {
                                        overflowExpanded = false
                                        navigator.push(DebugRoute)
                                    },
                                )
                            }
                        }
                    }
                },
                colors = DrebinTopAppBarColors(),
            )
        },
        bottomBar = {
            NavigationBar(containerColor = DrebinNavigationBarContainerColor()) {
                NavigationBarItem(
                    selected = selectedTab == HomeTab.Yours,
                    onClick = { selectedTab = HomeTab.Yours },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("Yours") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onBackground,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
                NavigationBarItem(
                    selected = selectedTab == HomeTab.Shared,
                    onClick = { selectedTab = HomeTab.Shared },
                    icon = { Icon(Icons.Filled.Share, contentDescription = null) },
                    label = { Text("Shared") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onBackground,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == HomeTab.Yours) {
                ExtendedFloatingActionButton(
                    onClick = { if (!state.uploading) launchPicker() },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(if (state.uploading) "Uploading…" else "Upload APK") },
                    shape = DrebinFabShape,
                    containerColor = DrebinFabContainerColor(),
                    contentColor = DrebinFabContentColor(),
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentUnderlapsNavigationBar = false,
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val horizontalMargin = ContentLayout.horizontalMargin(maxWidth)
            val visibleApps = if (selectedTab == HomeTab.Yours) state.apps else state.sharedApps
            val storageWarningStatus = state.storageStatus.takeIf {
                selectedTab == HomeTab.Yours &&
                        shouldShowStorageLimitWarning(it, dismissed = state.storageWarningDismissed)
            }
            val nextPageToken =
                if (selectedTab == HomeTab.Yours) state.nextPageToken else state.sharedNextPageToken
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
                    if (visibleApps.isEmpty() && storageWarningStatus == null) {
                        EmptyState(tab = selectedTab)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                horizontal = horizontalMargin,
                                vertical = 16.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            storageWarningStatus?.let { status ->
                                item(key = "storage-warning") {
                                    StorageLimitWarningCard(
                                        status = status,
                                        modifier = Modifier.fillMaxWidth(),
                                        onUpgrade = { navigator.push(SubscriptionRoute) },
                                        onDismiss = vm::dismissStorageWarning,
                                    )
                                }
                            }
                            if (visibleApps.isEmpty()) {
                                item(key = "empty-${selectedTab.name}") {
                                    EmptyState(
                                        tab = selectedTab,
                                        modifier = Modifier.fillMaxWidth().height(320.dp),
                                    )
                                }
                            } else {
                                items(visibleApps, key = { it.id }) { app ->
                                    val isShared = selectedTab == HomeTab.Shared
                                    AppCard(
                                        app = app,
                                        isShared = isShared,
                                        deletingShared = app.id in state.deletingSharedAppIds,
                                        onClick = { navigator.push(AppDetailRoute(app)) },
                                        onShare = { navigator.push(AppShareRoute(app)) },
                                        onDeleteShared = if (isShared) {
                                            { sharedAppPendingDelete = app }
                                        } else {
                                            null
                                        },
                                    )
                                }
                            }
                            if (nextPageToken != null) {
                                item(key = "load-more-${selectedTab.name}") {
                                    LaunchedEffect(selectedTab, nextPageToken) {
                                        vm.loadMore(
                                            selectedTab
                                        )
                                    }
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                            item(key = "bottom-spacer") {
                                Spacer(Modifier.height(64.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    sharedAppPendingDelete?.let { app ->
        val name = app.label.ifBlank { app.applicationId }
        AlertDialog(
            onDismissRequest = { sharedAppPendingDelete = null },
            title = { Text("Delete shared app?") },
            text = { Text("\"$name\" will be removed from your Shared tab. The owner's app is untouched.") },
            confirmButton = {
                TextButton(onClick = {
                    sharedAppPendingDelete = null
                    vm.deleteSharedApp(app)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { sharedAppPendingDelete = null }) { Text("Cancel") }
            },
        )
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

    state.notificationPromptApp?.let { app ->
        val name = app.label.ifBlank { app.applicationId }
        AlertDialog(
            onDismissRequest = { vm.notificationPromptHandled() },
            title = { Text("Enable notifications?") },
            text = {
                Text("Would you like push notifications when new versions of \"$name\" are uploaded?")
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.notificationPromptHandled()
                    notificationPermissionApp = app
                }) { Text("Enable") }
            },
            dismissButton = {
                TextButton(onClick = { vm.notificationPromptHandled() }) { Text("Not now") }
            },
        )
    }

    notificationPermissionApp?.let { app ->
        RequestNotificationPermission { _ ->
            notificationPermissionApp = null
            // Same as the detail bell: subscribe even if permission was denied, so enabling the
            // permission later in Settings starts showing notifications without another follow tap.
            vm.followNewApp(app.id)
        }
    }
}

@Composable
private fun AppCard(
    app: App,
    isShared: Boolean,
    deletingShared: Boolean,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onDeleteShared: (() -> Unit)?,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val versionLabel = when {
        app.latestVersionName.isNotBlank() -> "v${app.latestVersionName} (${app.latestVersionCode})"
        app.latestVersionCode > 0 -> "Build ${app.latestVersionCode}"
        else -> "No versions"
    }
    val countLabel = "${app.versionCount} version" + if (app.versionCount == 1) "" else "s"

    DrebinGradientCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        app.label.ifBlank { app.applicationId },
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreHoriz, contentDescription = "App options")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Share") },
                                onClick = {
                                    menuExpanded = false
                                    onShare()
                                },
                            )
                            onDeleteShared?.let { delete ->
                                DropdownMenuItem(
                                    text = { Text(if (deletingShared) "Deleting…" else "Delete") },
                                    enabled = !deletingShared,
                                    onClick = {
                                        menuExpanded = false
                                        delete()
                                    },
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    app.applicationId,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isShared) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Shared by ${app.ownerName.ifBlank { "another user" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "$versionLabel • $countLabel",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Updated ${formatDateTime(app.updatedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyState(tab: HomeTab, modifier: Modifier = Modifier) {
    // Scrollable so the pull-to-refresh gesture registers even when there are no apps to show.
    Box(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            when (tab) {
                HomeTab.Yours -> {
                    Text("No apps yet", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tap \"Upload APK\" to add your first one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                HomeTab.Shared -> {
                    Text("No shared apps yet", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Open a Drebin share link to add one here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
