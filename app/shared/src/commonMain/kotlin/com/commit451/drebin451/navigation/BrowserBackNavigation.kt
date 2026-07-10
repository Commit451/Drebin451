package com.commit451.drebin451.navigation

import androidx.compose.runtime.Composable

/**
 * Hooks the app's in-memory Navigation 3 stack up to platform browser history.
 * Android has no browser history, so its actual implementation is a no-op.
 */
@Composable
internal expect fun BrowserBackNavigationEffect(onBack: () -> Unit)

internal expect fun platformAppNavigationHistory(): AppNavigationHistory
