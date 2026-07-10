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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.commit451.drebin451.model.PlanIds
import com.commit451.drebin451.model.PlanLimits
import com.commit451.drebin451.model.storageStatusFor
import com.commit451.drebin451.navigation.LocalAppNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DebugScreen() {
    val navigator = LocalAppNavigator.current
    DrebinGradientScaffold(
        topBar = {
            TopAppBar(
                title = { AppBarTitleText("Debug") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = DrebinTopAppBarColors(),
            )
        },
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val horizontalMargin = ContentLayout.horizontalMargin(maxWidth)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = horizontalMargin, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "intro") {
                    Column(Modifier.fillMaxWidth()) {
                        Text(
                            "Storage warning previews",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "These are the same cards Home shows when the signed-in account " +
                                    "is within 50 MB of its storage limit or has reached it.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                item(key = "near-limit") {
                    StorageLimitWarningCard(
                        status = storageStatusFor(
                            plan = PlanIds.FREE,
                            usedBytes = PlanLimits.FREE_STORAGE_BYTES - (25L * 1024L * 1024L),
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        onUpgrade = {},
                        onDismiss = {},
                    )
                }
                item(key = "at-limit") {
                    StorageLimitWarningCard(
                        status = storageStatusFor(
                            plan = PlanIds.FREE,
                            usedBytes = PlanLimits.FREE_STORAGE_BYTES,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        onUpgrade = {},
                        onDismiss = {},
                    )
                }
            }
        }
    }
}
