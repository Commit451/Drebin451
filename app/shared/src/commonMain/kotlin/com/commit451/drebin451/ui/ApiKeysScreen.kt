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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreHoriz
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.commit451.drebin451.model.ApiKey
import com.commit451.drebin451.model.ApiKeyCreated
import com.commit451.drebin451.navigation.LocalAppNavigator
import com.commit451.drebin451.util.formatDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ApiKeysScreen() {
    val navigator = LocalAppNavigator.current
    val vm: ApiKeysViewModel = viewModel { ApiKeysViewModel() }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var keyPendingDelete by remember { mutableStateOf<ApiKey?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            vm.messageShown()
        }
    }

    DrebinGradientScaffold(
        topBar = {
            TopAppBar(
                title = { AppBarTitleText("API keys") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = DrebinTopAppBarColors(),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (!state.creating) showCreateDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(if (state.creating) "Creating…" else "Create API key") },
                shape = DrebinFabShape,
                containerColor = DrebinFabContainerColor(),
                contentColor = DrebinFabContentColor(),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val horizontalMargin = ContentLayout.horizontalMargin(maxWidth)
            if (state.creating) {
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
                    if (state.keys.isEmpty()) {
                        EmptyKeys()
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                horizontal = horizontalMargin,
                                vertical = 16.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(state.keys, key = { it.id }) { key ->
                                ApiKeyCard(key = key, onRevoke = { keyPendingDelete = key })
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateKeyDialog(
            onCreate = { label ->
                vm.create(label)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false },
        )
    }

    // One-time reveal of the freshly minted plaintext token — it cannot be shown again.
    state.createdKey?.let { created ->
        CreatedTokenDialog(
            created = created,
            onCopy = {
                clipboard.setText(AnnotatedString(created.token))
                vm.createdKeyDismissed()
                vm.notify("API key copied")
            },
            onDismiss = { vm.createdKeyDismissed() },
        )
    }

    keyPendingDelete?.let { key ->
        val name = key.label.ifBlank { "this API key" }
        AlertDialog(
            onDismissRequest = { keyPendingDelete = null },
            title = { Text("Revoke API key?") },
            text = {
                Text("Anything using \"$name\" will stop being able to access the API. This can't be undone.")
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(key)
                    keyPendingDelete = null
                }) { Text("Revoke") }
            },
            dismissButton = {
                TextButton(onClick = { keyPendingDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ApiKeyCard(key: ApiKey, onRevoke: () -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    DrebinGradientCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    key.label.ifBlank { "API key" },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreHoriz, contentDescription = "Key options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Revoke") },
                            onClick = {
                                menuExpanded = false
                                onRevoke()
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                key.maskedToken,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Created ${formatDateTime(key.createdAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                if (key.lastUsedAt > 0) "Last used ${formatDateTime(key.lastUsedAt)}" else "Never used",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CreateKeyDialog(onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    var label by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create API key") },
        text = {
            Column {
                Text("Give it a name so you can recognize it later, e.g. \"GitHub Actions\".")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    singleLine = true,
                    label = { Text("Name") },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(label.trim()) }) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun CreatedTokenDialog(
    created: ApiKeyCreated,
    onCopy: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("API key created") },
        text = {
            Column {
                Text(
                    "Copy this token now — you won't be able to see it again. " +
                            "Send it in the X-API-Key header for API requests.",
                )
                Spacer(Modifier.height(12.dp))
                SelectionContainer {
                    Text(
                        created.token,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCopy) { Text("Copy & close") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun EmptyKeys(modifier: Modifier = Modifier) {
    // Scrollable so the pull-to-refresh gesture registers even when there are no keys to show.
    Box(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No API keys yet", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "Create one to automate Drebin451 from CI or scripts.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
