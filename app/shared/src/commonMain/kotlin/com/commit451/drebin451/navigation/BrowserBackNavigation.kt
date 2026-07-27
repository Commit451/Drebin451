package com.commit451.drebin451.navigation

import androidx.compose.runtime.Composable
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal data class BrowserHistoryRoute(
    val index: Int,
    val route: AppRoute,
)

internal data class BrowserHistoryChange(
    val fromIndex: Int,
    val toIndex: Int,
    val forwardRoutes: List<BrowserHistoryRoute> = emptyList(),
)

@Serializable
private data class BrowserHistoryChangePayload(
    val fromIndex: Int,
    val toIndex: Int,
    val forwardRoutes: List<BrowserHistoryRoutePayload> = emptyList(),
)

@Serializable
private data class BrowserHistoryRoutePayload(
    val index: Int,
    val routeToken: String,
)

private val browserHistoryJson = Json {
    ignoreUnknownKeys = true
}

internal fun encodeBrowserHistoryRoute(route: AppRoute): String =
    browserHistoryJson.encodeToString(AppRoute.serializer(), route)

internal fun decodeBrowserHistoryChange(payload: String): BrowserHistoryChange? = runCatching {
    val decoded = browserHistoryJson.decodeFromString(
        BrowserHistoryChangePayload.serializer(),
        payload,
    )
    BrowserHistoryChange(
        fromIndex = decoded.fromIndex,
        toIndex = decoded.toIndex,
        forwardRoutes = decoded.forwardRoutes.mapNotNull { entry ->
            runCatching {
                BrowserHistoryRoute(
                    index = entry.index,
                    route = browserHistoryJson.decodeFromString(
                        AppRoute.serializer(),
                        entry.routeToken,
                    ),
                )
            }.getOrNull()
        },
    )
}.getOrNull()

/**
 * Hooks the app's in-memory Navigation 3 stack up to platform browser history.
 * Android has no browser history, so its actual implementation is a no-op.
 */
@Composable
internal expect fun BrowserBackNavigationEffect(
    onHistoryChange: (BrowserHistoryChange) -> Unit,
)

internal expect fun platformAppNavigationHistory(): AppNavigationHistory
