package com.commit451.drebin451.navigation

import com.commit451.drebin451.model.App
import com.commit451.drebin451.model.AppVersion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppNavigatorTest {

    @Test
    fun push_adds_route_and_browser_history_entry() {
        val history = RecordingNavigationHistory()
        val backStack = mutableListOf<AppRoute>(HomeRoute)
        val navigator = AppNavigator(backStack, history)

        navigator.push(SettingsRoute)

        assertEquals(listOf<AppRoute>(HomeRoute, SettingsRoute), backStack)
        assertEquals(1, history.pushes)
    }

    @Test
    fun pop_uses_browser_history_when_it_is_available() {
        val history = RecordingNavigationHistory(requestBackResult = true, initialIndex = 2)
        val backStack = mutableListOf<AppRoute>(HomeRoute, SettingsRoute, ApiKeysRoute)
        val navigator = AppNavigator(backStack, history)

        navigator.pop()

        assertEquals(listOf<AppRoute>(HomeRoute, SettingsRoute, ApiKeysRoute), backStack)
        assertEquals(1, history.backRequests)

        navigator.navigateFromBrowser(BrowserHistoryChange(fromIndex = 2, toIndex = 1))

        assertEquals(listOf<AppRoute>(HomeRoute, SettingsRoute), backStack)
    }

    @Test
    fun browserForward_restoresViewerRouteWithoutWritingAnotherHistoryEntry() {
        val history = RecordingNavigationHistory(requestBackResult = true)
        val viewerRoute = LandingImageViewerRoute(LandingScreenshot.AppsList)
        val backStack = mutableListOf<AppRoute>(AboutRoute)
        val navigator = AppNavigator(backStack, history)

        navigator.push(viewerRoute)
        navigator.navigateFromBrowser(
            BrowserHistoryChange(fromIndex = 1, toIndex = 0),
        )
        navigator.navigateFromBrowser(
            BrowserHistoryChange(
                fromIndex = 0,
                toIndex = 1,
                forwardRoutes = listOf(BrowserHistoryRoute(index = 1, route = viewerRoute)),
            ),
        )

        assertEquals(listOf<AppRoute>(AboutRoute, viewerRoute), backStack)
        assertEquals(1, history.pushes)
    }

    @Test
    fun browserForward_restoresMultipleRoutesInOrder() {
        val history = RecordingNavigationHistory(requestBackResult = true)
        val viewerRoute = LandingImageViewerRoute(LandingScreenshot.ReleaseDetails)
        val backStack = mutableListOf<AppRoute>(AboutRoute)
        val navigator = AppNavigator(backStack, history)

        navigator.push(PricingRoute)
        navigator.push(viewerRoute)
        navigator.navigateFromBrowser(BrowserHistoryChange(fromIndex = 2, toIndex = 0))
        navigator.navigateFromBrowser(
            BrowserHistoryChange(
                fromIndex = 0,
                toIndex = 2,
                forwardRoutes = listOf(
                    BrowserHistoryRoute(index = 1, route = PricingRoute),
                    BrowserHistoryRoute(index = 2, route = viewerRoute),
                ),
            ),
        )

        assertEquals(listOf<AppRoute>(AboutRoute, PricingRoute, viewerRoute), backStack)
        assertEquals(2, history.pushes)
    }

    @Test
    fun pushAfterBrowserBackBelowReplacedRoot_rebasesVisibleRoot() {
        val history = RecordingNavigationHistory(initialIndex = 1)
        val backStack = mutableListOf<AppRoute>(SplashRoute, LoginRoute())
        val navigator = AppNavigator(backStack, history)

        navigator.replaceAll(HomeRoute)
        navigator.navigateFromBrowser(BrowserHistoryChange(fromIndex = 1, toIndex = 0))
        navigator.push(SettingsRoute)

        assertEquals(listOf<AppRoute>(HomeRoute, SettingsRoute), backStack)
        assertEquals(2, history.replaces)
        assertEquals(1, history.pushes)
    }

    @Test
    fun pop_falls_back_to_local_stack_when_browser_history_is_unavailable() {
        val history = RecordingNavigationHistory(requestBackResult = false)
        val backStack = mutableListOf<AppRoute>(HomeRoute, SettingsRoute)
        val navigator = AppNavigator(backStack, history)

        navigator.pop()

        assertEquals(listOf<AppRoute>(HomeRoute), backStack)
        assertEquals(1, history.backRequests)
    }

    @Test
    fun pop_is_consumed_by_contextual_back_interceptor() {
        val history = RecordingNavigationHistory(requestBackResult = false)
        val backStack = mutableListOf<AppRoute>(HomeRoute, SettingsRoute)
        val navigator = AppNavigator(backStack, history)
        var consumed = 0
        val removeInterceptor = navigator.interceptBack {
            consumed++
            true
        }

        navigator.pop()

        assertEquals(1, consumed)
        assertEquals(listOf<AppRoute>(HomeRoute, SettingsRoute), backStack)
        assertEquals(0, history.backRequests)

        removeInterceptor()
        navigator.pop()

        assertEquals(listOf<AppRoute>(HomeRoute), backStack)
    }

    @Test
    fun programmaticPop_bypasses_contextual_back_interceptor() {
        val history = RecordingNavigationHistory(requestBackResult = false)
        val backStack = mutableListOf<AppRoute>(HomeRoute, SettingsRoute)
        val navigator = AppNavigator(backStack, history)
        var intercepted = false
        navigator.interceptBack {
            intercepted = true
            true
        }

        navigator.popIgnoringInterceptor()

        assertFalse(intercepted)
        assertEquals(listOf<AppRoute>(HomeRoute), backStack)
        assertEquals(1, history.backRequests)
    }

    @Test
    fun browserProgrammaticPop_bypassesInterceptorWhenHistoryEventArrives() {
        val history = RecordingNavigationHistory(requestBackResult = true, initialIndex = 1)
        val backStack = mutableListOf<AppRoute>(HomeRoute, SettingsRoute)
        val navigator = AppNavigator(backStack, history)
        var intercepted = false
        navigator.interceptBack {
            intercepted = true
            true
        }

        navigator.popIgnoringInterceptor()
        assertEquals(listOf<AppRoute>(HomeRoute, SettingsRoute), backStack)

        navigator.navigateFromBrowser(BrowserHistoryChange(fromIndex = 1, toIndex = 0))

        assertFalse(intercepted)
        assertEquals(listOf<AppRoute>(HomeRoute), backStack)
    }

    @Test
    fun browserBack_restores_consumed_history_position() {
        val history = RecordingNavigationHistory(initialIndex = 1)
        val backStack = mutableListOf<AppRoute>(HomeRoute, SettingsRoute)
        val navigator = AppNavigator(backStack, history)
        navigator.interceptBack { true }

        navigator.navigateFromBrowser(BrowserHistoryChange(fromIndex = 1, toIndex = 0))

        assertEquals(listOf<AppRoute>(HomeRoute, SettingsRoute), backStack)
        assertEquals(0, history.pushes)
        assertEquals(1, history.restores)
        assertEquals(1, history.lastRestoreTargetIndex)
    }

    @Test
    fun browserMultiStepBack_restoresConsumedSourceIndex() {
        val history = RecordingNavigationHistory(initialIndex = 3)
        val backStack = mutableListOf<AppRoute>(
            HomeRoute,
            SettingsRoute,
            ApiKeysRoute,
            SubscriptionRoute,
        )
        val navigator = AppNavigator(backStack, history)
        navigator.interceptBack { true }

        navigator.navigateFromBrowser(BrowserHistoryChange(fromIndex = 3, toIndex = 0))

        assertEquals(
            listOf<AppRoute>(HomeRoute, SettingsRoute, ApiKeysRoute, SubscriptionRoute),
            backStack,
        )
        assertEquals(1, history.restores)
        assertEquals(3, history.lastRestoreTargetIndex)
    }

    @Test
    fun push_equalRoute_doesNotCreateDuplicateStackOrHistoryEntry() {
        val history = RecordingNavigationHistory()
        val backStack = mutableListOf<AppRoute>(HomeRoute)
        val navigator = AppNavigator(backStack, history)

        navigator.push(HomeRoute)

        assertEquals(listOf<AppRoute>(HomeRoute), backStack)
        assertEquals(0, history.pushes)
    }

    @Test
    fun replaceAll_clears_stack_and_replaces_current_history_entry() {
        val history = RecordingNavigationHistory()
        val backStack = mutableListOf<AppRoute>(SplashRoute, LoginRoute())
        val navigator = AppNavigator(backStack, history)

        navigator.replaceAll(HomeRoute)

        assertEquals(listOf<AppRoute>(HomeRoute), backStack)
        assertEquals(1, history.replaces)
    }

    @Test
    fun themedApp_returns_the_app_for_app_scoped_routes() {
        val app = App(
            id = "app-1",
            applicationId = "com.example.one",
            imageUrl = "https://example.com/icon.png"
        )
        val version = AppVersion(id = "version-1", appId = app.id)

        assertEquals(app, AppDetailRoute(app).themedApp())
        assertEquals(app, AppShareRoute(app).themedApp())
        assertEquals(app, ReleaseDetailRoute(app, version).themedApp())
    }

    @Test
    fun themedApp_is_null_for_global_routes() {
        assertNull(HomeRoute.themedApp())
        assertNull(SettingsRoute.themedApp())
        assertNull(ApiKeysRoute.themedApp())
        assertNull(SubscriptionRoute.themedApp())
        assertNull(LoginRoute().themedApp())
    }

    @Test
    fun releaseDetailRoute_instances_are_unique_even_for_the_same_version() {
        val app = App(id = "app-1", applicationId = "com.example.one")
        val version = AppVersion(id = "version-1", appId = app.id)

        val first = ReleaseDetailRoute(app, version)
        val second = ReleaseDetailRoute(app, version)

        assertNotEquals(first, second)
        assertEquals(app, first.app)
        assertEquals(app, second.app)
        assertEquals(version, first.version)
        assertEquals(version, second.version)
    }

    @Test
    fun pendingDeepLinks_wait_until_auth_navigation_has_settled() {
        assertFalse(canHandlePendingDeepLink(currentRoute = null, isSignedIn = true))
        assertFalse(canHandlePendingDeepLink(currentRoute = SplashRoute, isSignedIn = true))
        assertFalse(canHandlePendingDeepLink(currentRoute = LoginRoute(), isSignedIn = true))
        assertFalse(canHandlePendingDeepLink(currentRoute = HomeRoute, isSignedIn = false))
        assertTrue(canHandlePendingDeepLink(currentRoute = HomeRoute, isSignedIn = true))
        assertTrue(
            canHandlePendingDeepLink(
                currentRoute = ReleaseDetailRoute(App(), AppVersion()),
                isSignedIn = true
            )
        )
    }

    private class RecordingNavigationHistory(
        private val requestBackResult: Boolean = false,
        private val initialIndex: Int = 0,
    ) : AppNavigationHistory {
        var pushes = 0
            private set
        var replaces = 0
            private set
        var backRequests = 0
            private set
        var restores = 0
            private set
        var lastRestoreTargetIndex: Int? = null
            private set

        override fun currentIndex(): Int = initialIndex

        override fun pushEntry(routeToken: String) {
            pushes++
        }

        override fun replaceEntry(routeToken: String) {
            replaces++
        }

        override fun restoreEntry(routeToken: String, targetIndex: Int) {
            restores++
            lastRestoreTargetIndex = targetIndex
        }

        override fun requestBack(): Boolean {
            backRequests++
            return requestBackResult
        }
    }
}
