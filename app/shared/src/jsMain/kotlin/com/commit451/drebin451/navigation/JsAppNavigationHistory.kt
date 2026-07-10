package com.commit451.drebin451.navigation

internal object JsAppNavigationHistory : AppNavigationHistory {
    override fun pushEntry() {
        drebinPushBrowserHistoryEntry()
    }

    override fun replaceEntry() {
        drebinReplaceBrowserHistoryEntry()
    }

    override fun requestBack(): Boolean = drebinRequestBrowserBack()
}
