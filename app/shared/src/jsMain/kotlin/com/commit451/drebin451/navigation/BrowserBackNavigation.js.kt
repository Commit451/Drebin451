package com.commit451.drebin451.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState

internal external fun drebinCurrentBrowserHistoryIndex(): Int
internal external fun drebinPushBrowserHistoryEntry(routeToken: String)
internal external fun drebinReplaceBrowserHistoryEntry(routeToken: String)
internal external fun drebinRestoreBrowserHistoryEntry(routeToken: String, targetIndex: Int)
internal external fun drebinRequestBrowserBack(): Boolean
private external fun drebinInstallBrowserBackHandler(callback: (String) -> Unit): () -> Unit

@Composable
internal actual fun BrowserBackNavigationEffect(
    onHistoryChange: (BrowserHistoryChange) -> Unit,
) {
    val currentOnHistoryChange = rememberUpdatedState(onHistoryChange)
    DisposableEffect(Unit) {
        val dispose = drebinInstallBrowserBackHandler { payload ->
            decodeBrowserHistoryChange(payload)?.let(currentOnHistoryChange.value)
        }
        onDispose { dispose() }
    }
}

internal actual fun platformAppNavigationHistory(): AppNavigationHistory = JsAppNavigationHistory
