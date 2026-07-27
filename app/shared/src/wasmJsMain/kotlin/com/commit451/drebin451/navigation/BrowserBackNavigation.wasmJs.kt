@file:OptIn(ExperimentalWasmJsInterop::class)

package com.commit451.drebin451.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState

@JsFun("() => window.drebinCurrentBrowserHistoryIndex()")
internal external fun jsCurrentBrowserHistoryIndex(): Int

@JsFun("(routeToken) => { window.drebinPushBrowserHistoryEntry(routeToken); }")
internal external fun jsPushBrowserHistoryEntry(routeToken: JsString)

@JsFun("(routeToken) => { window.drebinReplaceBrowserHistoryEntry(routeToken); }")
internal external fun jsReplaceBrowserHistoryEntry(routeToken: JsString)

@JsFun("(routeToken, targetIndex) => { window.drebinRestoreBrowserHistoryEntry(routeToken, targetIndex); }")
internal external fun jsRestoreBrowserHistoryEntry(routeToken: JsString, targetIndex: Int)

@JsFun("() => window.drebinRequestBrowserBack()")
internal external fun jsRequestBrowserBack(): Boolean

@JsFun("(callback) => window.drebinInstallBrowserBackHandler(callback)")
private external fun jsInstallBrowserBackHandler(callback: (JsAny?) -> Unit): JsAny

@JsFun("(dispose) => { dispose(); }")
private external fun jsDisposeBrowserBackHandler(dispose: JsAny)

@Composable
internal actual fun BrowserBackNavigationEffect(
    onHistoryChange: (BrowserHistoryChange) -> Unit,
) {
    val currentOnHistoryChange = rememberUpdatedState(onHistoryChange)
    DisposableEffect(Unit) {
        val dispose = jsInstallBrowserBackHandler { payload ->
            decodeBrowserHistoryChange(payload.toString())?.let(currentOnHistoryChange.value)
        }
        onDispose { jsDisposeBrowserBackHandler(dispose) }
    }
}

internal actual fun platformAppNavigationHistory(): AppNavigationHistory = WasmAppNavigationHistory
