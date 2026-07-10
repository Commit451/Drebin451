package com.commit451.drebin451.share

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberShareLauncher(): ((String) -> Unit)? {
    val context = LocalContext.current
    return { text ->
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        // NEW_TASK so the chooser launches cleanly even when the context isn't an Activity.
        val chooser =
            Intent.createChooser(send, null).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(chooser)
    }
}
