package com.commit451.drebin451.navigation

internal interface AppNavigationHistory {
    /** Called after an in-app push adds one app-level browser history entry. */
    fun pushEntry()

    /** Called after replaceAll makes the current screen the new app root. */
    fun replaceEntry()

    /**
     * Requests a platform back navigation. Returns true when the platform will report the back event
     * asynchronously through [BrowserBackNavigationEffect]; false means the caller should pop itself.
     */
    fun requestBack(): Boolean
}
