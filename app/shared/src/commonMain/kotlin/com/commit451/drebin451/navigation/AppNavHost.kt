package com.commit451.drebin451.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.navigation3.ui.NavDisplay
import com.commit451.drebin451.api.Api
import com.commit451.drebin451.auth.UserManager
import com.commit451.drebin451.model.App
import com.commit451.drebin451.share.AppVersionDeepLinkTarget
import com.commit451.drebin451.share.DeepLinkTarget
import com.commit451.drebin451.share.PendingDeepLink
import com.commit451.drebin451.share.ShareTarget
import com.commit451.drebin451.ui.AboutScreen
import com.commit451.drebin451.ui.ApiKeysScreen
import com.commit451.drebin451.ui.AppDetailScreen
import com.commit451.drebin451.ui.AppIconTheme
import com.commit451.drebin451.ui.AppShareScreen
import com.commit451.drebin451.ui.DebugScreen
import com.commit451.drebin451.ui.HomeScreen
import com.commit451.drebin451.ui.LoginScreen
import com.commit451.drebin451.ui.PricingScreen
import com.commit451.drebin451.ui.ReleaseDetailScreen
import com.commit451.drebin451.ui.SettingsScreen
import com.commit451.drebin451.ui.SplashScreen
import com.commit451.drebin451.ui.SubscriptionScreen
import kotlin.coroutines.cancellation.CancellationException

internal val LocalAppNavigator = staticCompositionLocalOf<AppNavigator> {
    error("No AppNavigator provided")
}

internal fun AppRoute.themedApp(): App? = when (this) {
    is AppDetailRoute -> app
    is AppShareRoute -> app
    is ReleaseDetailRoute -> app
    else -> null
}

internal fun canHandlePendingDeepLink(currentRoute: AppRoute?, isSignedIn: Boolean): Boolean =
    isSignedIn && currentRoute != null && currentRoute !is SplashRoute && currentRoute !is LoginRoute

private val appEntryProvider = entryProvider<AppRoute> {
    entry<SplashRoute> { SplashScreen() }
    entry<AboutRoute> { AboutScreen() }
    entry<PricingRoute> { PricingScreen() }
    entry<LoginRoute> { route -> LoginScreen(startOnSignUp = route.startOnSignUp) }
    entry<HomeRoute> { HomeScreen() }
    entry<DebugRoute> { DebugScreen() }
    entry<AppDetailRoute> { route -> AppDetailScreen(route) }
    entry<AppShareRoute> { route -> AppShareScreen(route) }
    entry<ReleaseDetailRoute> { route -> ReleaseDetailScreen(route) }
    entry<SettingsRoute> { SettingsScreen() }
    entry<ApiKeysRoute> { ApiKeysScreen() }
    entry<SubscriptionRoute> { SubscriptionScreen() }
}

@Composable
internal fun rememberAppBackStack(): NavBackStack<AppRoute> =
    rememberSerializable(serializer = NavBackStackSerializer<AppRoute>()) {
        NavBackStack<AppRoute>(SplashRoute)
    }

@Composable
internal fun rememberAppNavigator(backStack: MutableList<AppRoute>): AppNavigator =
    remember(backStack) { AppNavigator(backStack) }

@Composable
internal fun AppNavHost(
    backStack: NavBackStack<AppRoute>,
    navigator: AppNavigator,
) {
    val currentRoute = backStack.lastOrNull()
    BrowserBackNavigationEffect(onBack = navigator::popFromBrowser)
    PendingDeepLinkNavigationEffect(currentRoute = currentRoute, navigator = navigator)

    CompositionLocalProvider(LocalAppNavigator provides navigator) {
        AppIconTheme(currentRoute?.themedApp()) {
            NavDisplay(
                backStack = backStack,
                onBack = navigator::pop,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = appEntryProvider,
            )
        }
    }
}

@Composable
private fun PendingDeepLinkNavigationEffect(
    currentRoute: AppRoute?,
    navigator: AppNavigator,
) {
    val pendingDeepLink by PendingDeepLink.target.collectAsStateWithLifecycle()
    val user by UserManager.user.collectAsStateWithLifecycle()

    LaunchedEffect(currentRoute, pendingDeepLink, user) {
        val target = pendingDeepLink ?: return@LaunchedEffect
        if (!canHandlePendingDeepLink(currentRoute = currentRoute, isSignedIn = user != null)) {
            return@LaunchedEffect
        }

        try {
            // Do not clear PendingDeepLink before resolving the route. This LaunchedEffect is keyed
            // by pendingDeepLink, so setting it to null here cancels this coroutine at the first
            // suspension point inside toRoute() (the API calls), silently dropping the navigation and
            // leaving the user on Home.
            val route = target.toRoute()
            navigator.push(route)
            PendingDeepLink.set(null)
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            // Consume malformed/stale links after a real resolution failure so they do not retry on
            // every route/user recomposition. A valid fresh push can set a new target again.
            PendingDeepLink.set(null)
        }
    }
}

private suspend fun DeepLinkTarget.toRoute(): AppRoute = when (this) {
    is ShareTarget.App -> AppDetailRoute(Api.addSharedApp(shareId))
    is ShareTarget.Release -> {
        val app = Api.addSharedApp(shareId)
        ReleaseDetailRoute(app = app, version = Api.appVersion(app.id, versionId))
    }

    is AppVersionDeepLinkTarget -> {
        val app = Api.app(appId)
        ReleaseDetailRoute(app = app, version = Api.appVersion(appId, versionId))
    }
}
