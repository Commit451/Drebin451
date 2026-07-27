package com.commit451.drebin451.navigation

internal class AppNavigator(
    private val backStack: MutableList<AppRoute>,
    private val history: AppNavigationHistory = platformAppNavigationHistory(),
) {
    private var backInterceptor: (() -> Boolean)? = null
    private var bypassNextBrowserBackInterceptor = false
    private var currentHistoryIndex = history.currentIndex()
    private var rootHistoryIndex =
        (currentHistoryIndex - backStack.lastIndex).coerceAtLeast(0)

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
            popBackStackLocally()
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
        // NavDisplay keys must be unique. Re-pushing an equal singleton/data route would create a
        // duplicate browser entry that cannot be reconstructed safely on Forward.
        if (backStack.lastOrNull() == route) return

        // replaceAll() can leave older same-document entries behind. If the user traversed below the
        // current app root and then navigates in-app, rebase that visible root before making a branch.
        if (currentHistoryIndex < rootHistoryIndex) {
            backStack.lastOrNull()?.let { history.replaceEntry(encodeBrowserHistoryRoute(it)) }
            rootHistoryIndex = currentHistoryIndex
        }

        backStack.add(route)
        currentHistoryIndex += 1
        history.pushEntry(encodeBrowserHistoryRoute(route))
    }

    fun replaceAll(route: AppRoute) {
        backStack.clear()
        backStack.add(route)
        rootHistoryIndex = currentHistoryIndex
        history.replaceEntry(encodeBrowserHistoryRoute(route))
    }

    internal fun navigateFromBrowser(change: BrowserHistoryChange) {
        if (change.toIndex < change.fromIndex) {
            navigateBackFromBrowser(change)
        } else if (change.toIndex > change.fromIndex) {
            navigateForwardFromBrowser(change)
        }
    }

    private fun navigateBackFromBrowser(change: BrowserHistoryChange) {
        if (bypassNextBrowserBackInterceptor) {
            bypassNextBrowserBackInterceptor = false
            applyBrowserBack(change)
            return
        }
        if (backInterceptor?.invoke() == true) {
            // The browser already moved before popstate fired. Return to the exact source index,
            // suppressing that corrective popstate, so multi-step Back stays aligned too.
            backStack.lastOrNull()?.let {
                history.restoreEntry(
                    routeToken = encodeBrowserHistoryRoute(it),
                    targetIndex = change.fromIndex,
                )
            }
            currentHistoryIndex = change.fromIndex
            return
        }
        applyBrowserBack(change)
    }

    private fun applyBrowserBack(change: BrowserHistoryChange) {
        val appTargetIndex = maxOf(change.toIndex, rootHistoryIndex)
        val steps = (change.fromIndex - appTargetIndex).coerceAtLeast(0)
        repeat(minOf(steps, backStack.size - 1)) {
            backStack.removeLastOrNull()
        }
        currentHistoryIndex = change.toIndex
    }

    private fun navigateForwardFromBrowser(change: BrowserHistoryChange) {
        val firstRestorableIndex = maxOf(change.fromIndex + 1, rootHistoryIndex)
        change.forwardRoutes
            .asSequence()
            .filter { it.index in firstRestorableIndex..change.toIndex }
            .sortedBy { it.index }
            .forEach { destination ->
                if (backStack.lastOrNull() != destination.route) {
                    backStack.add(destination.route)
                }
            }
        currentHistoryIndex = change.toIndex
    }

    private fun popBackStackLocally() {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
            currentHistoryIndex = (currentHistoryIndex - 1).coerceAtLeast(rootHistoryIndex)
        }
    }
}
