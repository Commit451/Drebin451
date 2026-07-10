package com.commit451.drebin451.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.commit451.drebin451.model.PlanIds
import com.commit451.drebin451.model.PlanLimits
import com.commit451.drebin451.model.User
import com.commit451.drebin451.navigation.LocalAppNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SubscriptionScreen() {
    val navigator = LocalAppNavigator.current
    val uriHandler = LocalUriHandler.current
    val vm: SubscriptionViewModel = viewModel { SubscriptionViewModel() }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.externalUrl) {
        state.externalUrl?.let { url ->
            try {
                uriHandler.openUri(url)
                vm.externalUrlOpened()
            } catch (t: Throwable) {
                vm.externalUrlOpenFailed(t)
            }
        }
    }

    LifecycleResumeEffect(Unit) {
        vm.refreshAfterExternalFlowIfNeeded()
        onPauseOrDispose { }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            vm.messageShown()
        }
    }

    DrebinGradientScaffold(
        topBar = {
            TopAppBar(
                title = { AppBarTitleText("Plan") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { vm.refresh() },
                        enabled = !state.busy && !state.loading
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh plan")
                    }
                },
                colors = DrebinTopAppBarColors(),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val horizontalMargin = ContentLayout.horizontalMargin(maxWidth)
            if (state.busy) {
                LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
            if (state.loading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = horizontalMargin, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    state.user?.let { user ->
                        item(key = "current-plan") {
                            CurrentPlanCard(user = user)
                        }
                        item(key = "stripe-plan") {
                            StripePlanCard(
                                user = user,
                                busy = state.busy,
                                onSubscribe = vm::subscribe,
                                onManageBilling = vm::manageBilling,
                            )
                        }
                    } ?: item(key = "signed-out") {
                        DrebinGradientCard(Modifier.fillMaxWidth()) {
                            Text(
                                text = "Sign in to manage your plan.",
                                modifier = Modifier.padding(16.dp),
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
private fun CurrentPlanCard(user: User) {
    val plan = PlanLimits.normalized(user.plan)
    val quotaBytes = PlanLimits.storageQuotaBytes(plan)
    val progress = if (quotaBytes > 0) {
        (user.storageUsedBytes.toDouble() / quotaBytes.toDouble()).coerceIn(0.0, 1.0).toFloat()
    } else {
        0f
    }

    DrebinGradientCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(planLabel(plan), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "${formatSize(user.storageUsedBytes)} of ${formatSize(quotaBytes)} used",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StripePlanCard(
    user: User,
    busy: Boolean,
    onSubscribe: () -> Unit,
    onManageBilling: () -> Unit,
) {
    val isPro = user.isPro()
    DrebinGradientCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Pro", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${formatSize(PlanLimits.PRO_STORAGE_BYTES)} storage for APK uploads, billed through Stripe.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = if (isPro) onManageBilling else onSubscribe,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isPro) "Manage billing" else "Subscribe with Stripe")
            }
        }
    }
}

private fun User.isPro(): Boolean =
    PlanLimits.normalized(plan) == PlanIds.PRO

private fun planLabel(plan: String): String =
    if (PlanLimits.normalized(plan) == PlanIds.PRO) "Pro" else "Free"
