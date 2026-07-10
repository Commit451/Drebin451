package com.commit451.drebin451.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.commit451.drebin451.model.PlanIds
import com.commit451.drebin451.model.PlanLimits
import com.commit451.drebin451.model.StorageStatus

@Composable
internal fun StorageLimitWarningCard(
    status: StorageStatus,
    modifier: Modifier = Modifier,
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isAtLimit = status.atLimit
    val title = if (isAtLimit) "Storage limit reached" else "Almost out of storage"
    val body = if (isAtLimit) {
        "You've used ${formatSize(status.usedBytes)} of your ${formatSize(status.limitBytes)} " +
                "storage limit. Upgrade to keep uploading APKs."
    } else {
        "You have ${formatSize(status.remainingBytes)} left before you hit your " +
                "${planLabel(status.plan)} storage limit. Upgrade for more space or dismiss this message."
    }

    DrebinGradientCard(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = onUpgrade) { Text("Upgrade") }
                TextButton(onClick = onDismiss) { Text("Dismiss") }
            }
        }
    }
}

internal fun shouldShowStorageLimitWarning(status: StorageStatus?, dismissed: Boolean): Boolean =
    !dismissed && status != null && (status.nearLimit || status.atLimit)

private fun planLabel(plan: String): String =
    if (PlanLimits.normalized(plan) == PlanIds.PRO) "Pro" else "Free"
