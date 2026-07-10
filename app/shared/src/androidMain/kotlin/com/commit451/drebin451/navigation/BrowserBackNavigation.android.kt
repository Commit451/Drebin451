package com.commit451.drebin451.navigation

import androidx.compose.runtime.Composable

@Composable
internal actual fun BrowserBackNavigationEffect(onBack: () -> Unit) = Unit

internal actual fun platformAppNavigationHistory(): AppNavigationHistory =
    AndroidAppNavigationHistory
