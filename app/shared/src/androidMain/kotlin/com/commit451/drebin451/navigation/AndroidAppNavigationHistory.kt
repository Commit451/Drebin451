package com.commit451.drebin451.navigation

internal object AndroidAppNavigationHistory : AppNavigationHistory {
    override fun pushEntry() = Unit

    override fun replaceEntry() = Unit

    override fun requestBack(): Boolean = false
}
