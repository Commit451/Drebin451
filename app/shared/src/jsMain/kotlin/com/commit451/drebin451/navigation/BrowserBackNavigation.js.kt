package com.commit451.drebin451.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState

internal external fun drebinPushBrowserHistoryEntry()
internal external fun drebinReplaceBrowserHistoryEntry()
internal external fun drebinRequestBrowserBack(): Boolean
private external fun drebinInstallBrowserBackHandler(callback: () -> Unit): () -> Unit

@Composable
internal actual fun BrowserBackNavigationEffect(onBack: () -> Unit) {
    val currentOnBack = rememberUpdatedState(onBack)
    DisposableEffect(Unit) {
        val dispose = drebinInstallBrowserBackHandler { currentOnBack.value() }
        onDispose { dispose() }
    }
}

internal actual fun platformAppNavigationHistory(): AppNavigationHistory = JsAppNavigationHistory
