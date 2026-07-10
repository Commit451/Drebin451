@file:OptIn(ExperimentalWasmJsInterop::class)

package com.commit451.drebin451.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState

@JsFun("() => { window.drebinPushBrowserHistoryEntry(); }")
internal external fun jsPushBrowserHistoryEntry()

@JsFun("() => { window.drebinReplaceBrowserHistoryEntry(); }")
internal external fun jsReplaceBrowserHistoryEntry()

@JsFun("() => window.drebinRequestBrowserBack()")
internal external fun jsRequestBrowserBack(): Boolean

@JsFun("(callback) => window.drebinInstallBrowserBackHandler(callback)")
private external fun jsInstallBrowserBackHandler(callback: (JsAny?) -> Unit): JsAny

@JsFun("(dispose) => { dispose(); }")
private external fun jsDisposeBrowserBackHandler(dispose: JsAny)

@Composable
internal actual fun BrowserBackNavigationEffect(onBack: () -> Unit) {
    val currentOnBack = rememberUpdatedState(onBack)
    DisposableEffect(Unit) {
        val dispose = jsInstallBrowserBackHandler { currentOnBack.value() }
        onDispose { jsDisposeBrowserBackHandler(dispose) }
    }
}

internal actual fun platformAppNavigationHistory(): AppNavigationHistory = WasmAppNavigationHistory
