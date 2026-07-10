@file:OptIn(ExperimentalWasmJsInterop::class)

package com.commit451.drebin451.navigation

internal object WasmAppNavigationHistory : AppNavigationHistory {
    override fun pushEntry() {
        jsPushBrowserHistoryEntry()
    }

    override fun replaceEntry() {
        jsReplaceBrowserHistoryEntry()
    }

    override fun requestBack(): Boolean = jsRequestBrowserBack()
}
