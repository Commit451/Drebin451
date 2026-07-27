package com.commit451.drebin451.navigation

import androidx.compose.runtime.Composable

@Composable
internal actual fun BrowserBackNavigationEffect(
    onHistoryChange: (BrowserHistoryChange) -> Unit,
) = Unit

internal actual fun platformAppNavigationHistory(): AppNavigationHistory =
    AndroidAppNavigationHistory
