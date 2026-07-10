package com.commit451.drebin451.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.commit451.drebin451.api.Api
import com.commit451.drebin451.appinfo.currentAppBuildInfo
import com.commit451.drebin451.auth.UserManager
import com.commit451.drebin451.auth.shouldShowLoggedOutAboutPage
import com.commit451.drebin451.auth.signOut
import com.commit451.drebin451.download.DREBIN451_LATEST_APK_DOWNLOAD_URL
import com.commit451.drebin451.download.shouldShowDrebin451ApkDownload
import com.commit451.drebin451.model.PlanIds
import com.commit451.drebin451.model.PlanLimits
import com.commit451.drebin451.navigation.AboutRoute
import com.commit451.drebin451.navigation.ApiKeysRoute
import com.commit451.drebin451.navigation.LocalAppNavigator
import com.commit451.drebin451.navigation.LoginRoute
import com.commit451.drebin451.navigation.SubscriptionRoute
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen() {
    val navigator = LocalAppNavigator.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val buildInfo = remember { currentAppBuildInfo() }
    val user by UserManager.user.collectAsStateWithLifecycle()
    var overflowExpanded by remember { mutableStateOf(false) }
    var confirmSignOut by remember { mutableStateOf(false) }
    var confirmDeleteAccount by remember { mutableStateOf(false) }
    var deletingAccount by remember { mutableStateOf(false) }
    var deleteAccountError by remember { mutableStateOf<String?>(null) }

    DrebinGradientScaffold(
        topBar = {
            TopAppBar(
                title = { AppBarTitleText("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
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
                                text = { Text("Sign out") },
                                onClick = {
                                    overflowExpanded = false
                                    confirmSignOut = true
                                },
                            )
                        }
                    }
                },
                colors = DrebinTopAppBarColors(),
            )
        },
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val horizontalMargin = ContentLayout.horizontalMargin(maxWidth)
            Column(Modifier.fillMaxSize().padding(horizontal = horizontalMargin)) {
                user?.let { signedInUser ->
                    ListItem(
                        headlineContent = { Text("Signed in as") },
                        supportingContent = { Text(signedInUser.accountSummary()) },
                    )
                }
                if (shouldShowDrebin451ApkDownload()) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { uriHandler.openUri(DREBIN451_LATEST_APK_DOWNLOAD_URL) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Download latest Drebin451 APK")
                    }
                    Spacer(Modifier.height(8.dp))
                }
                ListItem(
                    headlineContent = { Text("Plan") },
                    supportingContent = { Text(user?.planSummary() ?: "Manage subscription") },
                    trailingContent = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable { navigator.push(SubscriptionRoute) },
                )
                ListItem(
                    headlineContent = { Text("API keys") },
                    supportingContent = { Text("Create and manage keys for API access") },
                    trailingContent = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable { navigator.push(ApiKeysRoute) },
                )
                ListItem(
                    headlineContent = { Text("App version") },
                    supportingContent = { Text(buildInfo.versionLabel) },
                )
                ListItem(
                    headlineContent = { Text("Commit") },
                    supportingContent = { Text(buildInfo.commitHashLabel) },
                )
                ListItem(
                    headlineContent = { Text("Privacy Policy") },
                    supportingContent = { Text("commit451.com/privacy") },
                    trailingContent = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable { uriHandler.openUri(LegalLinks.PRIVACY_POLICY_URL) },
                )
                ListItem(
                    headlineContent = { Text("Terms") },
                    supportingContent = { Text("commit451.com/terms") },
                    trailingContent = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.clickable { uriHandler.openUri(LegalLinks.TERMS_URL) },
                )
                user?.let {
                    ListItem(
                        headlineContent = {
                            Text("Delete account", color = MaterialTheme.colorScheme.error)
                        },
                        supportingContent = { Text(DeleteAccountSupportingText) },
                        modifier = Modifier.clickable {
                            deleteAccountError = null
                            confirmDeleteAccount = true
                        },
                    )
                }
            }
        }
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out?") },
            text = { Text("You'll need to sign in again to manage your apps and API keys.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmSignOut = false
                    scope.launch {
                        signOut()
                        navigator.replaceAll(
                            if (shouldShowLoggedOutAboutPage()) AboutRoute else LoginRoute(),
                        )
                    }
                }) { Text("Sign out") }
            },
            dismissButton = {
                TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") }
            },
        )
    }

    if (confirmDeleteAccount) {
        AlertDialog(
            onDismissRequest = {
                if (!deletingAccount) {
                    confirmDeleteAccount = false
                    deleteAccountError = null
                }
            },
            title = { Text("Delete account?") },
            text = {
                Column {
                    Text(deleteAccountConfirmationText(user?.email.orEmpty()))
                    deleteAccountError?.let { message ->
                        Spacer(Modifier.height(12.dp))
                        Text(message, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !deletingAccount,
                    onClick = {
                        deletingAccount = true
                        deleteAccountError = null
                        scope.launch {
                            try {
                                Api.deleteAccount()
                                signOut()
                                navigator.replaceAll(
                                    if (shouldShowLoggedOutAboutPage()) AboutRoute else LoginRoute(),
                                )
                            } catch (t: Throwable) {
                                deletingAccount = false
                                deleteAccountError = t.message ?: "Couldn't delete account"
                            }
                        }
                    },
                ) {
                    Text(
                        if (deletingAccount) "Deleting…" else "Delete account",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !deletingAccount,
                    onClick = {
                        confirmDeleteAccount = false
                        deleteAccountError = null
                    },
                ) { Text("Cancel") }
            },
        )
    }
}

internal fun com.commit451.drebin451.model.User.accountSummary(): String = email

internal const val DeleteAccountSupportingText =
    "Permanently delete your apps, uploads, API keys, and account"

internal fun deleteAccountConfirmationText(email: String): String {
    val account = email.trim().takeIf { it.isNotBlank() }?.let { "\"$it\"" } ?: "this account"
    return "This permanently deletes $account, every app and upload you own, your API keys, " +
            "and your saved shared-app list. Any active Pro subscription will also be canceled. " +
            "This can't be undone."
}

private fun com.commit451.drebin451.model.User.planSummary(): String {
    val planLabel = if (PlanLimits.normalized(plan) == PlanIds.PRO) "Pro" else "Free"
    return "$planLabel • ${formatSize(storageUsedBytes)} of ${
        formatSize(
            PlanLimits.storageQuotaBytes(
                plan
            )
        )
    } used"
}
