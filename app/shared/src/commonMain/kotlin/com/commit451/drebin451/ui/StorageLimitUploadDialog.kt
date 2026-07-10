package com.commit451.drebin451.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.commit451.drebin451.api.HttpException
import io.ktor.http.HttpStatusCode

private const val UploadStorageLimitDialogMessage =
    "Your APK couldn't be uploaded because your account is out of storage. " +
            "Upgrade your plan to add more space, then try uploading again."

internal fun uploadStorageLimitDialogMessage(t: Throwable): String? =
    if (t.paymentRequiredHttpException() != null) UploadStorageLimitDialogMessage else null

private fun Throwable.paymentRequiredHttpException(): HttpException? {
    var current: Throwable? = this
    while (current != null) {
        if (current is HttpException && current.statusCode == HttpStatusCode.PaymentRequired) {
            return current
        }
        current = current.cause?.takeIf { it !== current }
    }
    return null
}

@Composable
internal fun StorageLimitUploadDialog(
    message: String,
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Storage limit reached") },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onUpgrade) { Text("Upgrade storage") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Not now") }
        },
    )
}
