@file:OptIn(ExperimentalWasmJsInterop::class)

package com.commit451.drebin451.navigation

internal object WasmAppNavigationHistory : AppNavigationHistory {
    override fun currentIndex(): Int = jsCurrentBrowserHistoryIndex()

    override fun pushEntry(routeToken: String) {
        jsPushBrowserHistoryEntry(routeToken.toJsString())
    }

    override fun replaceEntry(routeToken: String) {
        jsReplaceBrowserHistoryEntry(routeToken.toJsString())
    }

    override fun restoreEntry(routeToken: String, targetIndex: Int) {
        jsRestoreBrowserHistoryEntry(routeToken.toJsString(), targetIndex)
    }

    override fun requestBack(): Boolean = jsRequestBrowserBack()
}
