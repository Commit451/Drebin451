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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.commit451.drebin451.auth.UserManager
import com.commit451.drebin451.navigation.AppShareRoute
import com.commit451.drebin451.navigation.LocalAppNavigator
import com.commit451.drebin451.share.ShareLinks
import com.commit451.drebin451.share.rememberShareLauncher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppShareScreen(route: AppShareRoute) {
    val navigator = LocalAppNavigator.current
    val vm: AppShareViewModel = viewModel { AppShareViewModel(route.app) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current
    val share = rememberShareLauncher()
    val app = state.app
    val appName = app.label.ifBlank { app.applicationId }
    val link = app.shareId.takeIf { it.isNotBlank() }?.let { ShareLinks.appUrl(it) }
    val isOwner = app.ownerUserId == UserManager.current()?.uid
    var confirmRefresh by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            vm.messageShown()
        }
    }

    DrebinGradientScaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        AppBarTitleText(
                            text = "Share app",
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
                        Text(
                            "Anyone signed in with this link can add \"$appName\" to their Shared tab. " +
                                    "Only the owner can upload or delete the app.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (link == null) {
                            Text(
                                "No share link is available yet. Refresh the share link to create one.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        } else {
                            SelectionContainer {
                                Text(
                                    link,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                )
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        link?.let {
                            clipboard.setText(AnnotatedString(it))
                            vm.notify("Link copied")
                        }
                    },
                    enabled = link != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Copy to clipboard")
                }

                if (share != null) {
                    Button(
                        onClick = { link?.let(share) },
                        enabled = link != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Share")
                    }
                } else {
                    Text(
                        "System share is not available on this platform; copy the link instead.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (isOwner) {
                    OutlinedButton(
                        onClick = { confirmRefresh = true },
                        enabled = !state.refreshingShareId,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.refreshingShareId) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Refreshing…")
                        } else {
                            Text("Revoke & refresh link")
                        }
                    }
                    Text(
                        "Refreshing the link keeps existing Shared entries, but the previous URL stops working.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (confirmRefresh) {
        AlertDialog(
            onDismissRequest = { confirmRefresh = false },
            title = { Text("Refresh share link?") },
            text = {
                Text(
                    "The current share URL will become invalid immediately. Anyone who already added " +
                            "\"$appName\" keeps it in Shared, but the old URL won't work for future opens.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmRefresh = false
                    vm.refreshShareLink()
                }) { Text("Refresh link") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRefresh = false }) { Text("Cancel") }
            },
        )
    }
}
