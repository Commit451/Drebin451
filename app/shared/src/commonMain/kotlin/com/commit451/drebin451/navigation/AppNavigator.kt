package com.commit451.drebin451.navigation

internal class AppNavigator(
    private val backStack: MutableList<AppRoute>,
    private val history: AppNavigationHistory = platformAppNavigationHistory(),
) {
    fun pop() {
        if (backStack.size <= 1) return

        if (!history.requestBack()) {
            popBackStack()
        }
    }

    fun push(route: AppRoute) {
        backStack.add(route)
        history.pushEntry()
    }

    fun replaceAll(route: AppRoute) {
        backStack.clear()
        backStack.add(route)
        history.replaceEntry()
    }

    internal fun popFromBrowser() {
        popBackStack()
    }

    private fun popBackStack() {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
    }
}
