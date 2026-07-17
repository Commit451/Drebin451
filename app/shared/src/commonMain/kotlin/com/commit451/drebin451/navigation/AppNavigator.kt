package com.commit451.drebin451.navigation

internal class AppNavigator(
    private val backStack: MutableList<AppRoute>,
    private val history: AppNavigationHistory = platformAppNavigationHistory(),
) {
    private var backInterceptor: (() -> Boolean)? = null
    private var bypassNextBrowserBackInterceptor = false

    fun pop() {
        pop(allowInterception = true)
    }

    /** Leaves the current route even when contextual UI is consuming user Back gestures. */
    fun popIgnoringInterceptor() {
        pop(allowInterception = false)
    }

    private fun pop(allowInterception: Boolean) {
        if (allowInterception && backInterceptor?.invoke() == true) return
        if (backStack.size <= 1) return

        if (!allowInterception) bypassNextBrowserBackInterceptor = true
        if (!history.requestBack()) {
            bypassNextBrowserBackInterceptor = false
            popBackStack()
        }
    }

    /**
     * Installs the current route's contextual Back handler. Returning true consumes Back without
     * leaving the route. The returned callback removes this exact handler.
     */
    fun interceptBack(interceptor: () -> Boolean): () -> Unit {
        backInterceptor = interceptor
        return {
            if (backInterceptor === interceptor) backInterceptor = null
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
        if (bypassNextBrowserBackInterceptor) {
            bypassNextBrowserBackInterceptor = false
            popBackStack()
            return
        }
        if (backInterceptor?.invoke() == true) {
            // The browser already moved back before popstate fired. Restore a matching entry when
            // contextual UI consumes Back so browser and in-memory history stay aligned.
            history.pushEntry()
            return
        }
        popBackStack()
    }

    private fun popBackStack() {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
    }
}
