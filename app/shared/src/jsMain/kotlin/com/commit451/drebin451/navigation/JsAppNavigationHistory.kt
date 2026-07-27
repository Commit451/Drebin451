package com.commit451.drebin451.navigation

internal object JsAppNavigationHistory : AppNavigationHistory {
    override fun currentIndex(): Int = drebinCurrentBrowserHistoryIndex()

    override fun pushEntry(routeToken: String) {
        drebinPushBrowserHistoryEntry(routeToken)
    }

    override fun replaceEntry(routeToken: String) {
        drebinReplaceBrowserHistoryEntry(routeToken)
    }

    override fun restoreEntry(routeToken: String, targetIndex: Int) {
        drebinRestoreBrowserHistoryEntry(routeToken, targetIndex)
    }

    override fun requestBack(): Boolean = drebinRequestBrowserBack()
}
