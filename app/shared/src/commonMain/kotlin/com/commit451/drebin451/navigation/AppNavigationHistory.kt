package com.commit451.drebin451.navigation

internal interface AppNavigationHistory {
    /** Current app-owned browser-history index, or zero on platforms without browser history. */
    fun currentIndex(): Int

    /** Called after an in-app push adds one app-level browser history entry. */
    fun pushEntry(routeToken: String)

    /** Called after replaceAll makes the current screen the new app root. */
    fun replaceEntry(routeToken: String)

    /** Re-aligns browser history after contextual UI consumes an already-delivered Back event. */
    fun restoreEntry(routeToken: String, targetIndex: Int)

    /**
     * Requests a platform back navigation. Returns true when the platform will report the change
     * asynchronously through [BrowserBackNavigationEffect]; false means the caller should pop itself.
     */
    fun requestBack(): Boolean
}
