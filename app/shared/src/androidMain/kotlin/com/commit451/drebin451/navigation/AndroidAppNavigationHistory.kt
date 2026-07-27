package com.commit451.drebin451.navigation

internal object AndroidAppNavigationHistory : AppNavigationHistory {
    override fun currentIndex(): Int = 0

    override fun pushEntry(routeToken: String) = Unit

    override fun replaceEntry(routeToken: String) = Unit

    override fun restoreEntry(routeToken: String, targetIndex: Int) = Unit

    override fun requestBack(): Boolean = false
}
