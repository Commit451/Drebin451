package com.commit451.drebin451.push

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
actual fun RequestNotificationPermission(onResult: (Boolean) -> Unit) {
    // No web push; report "granted" so common callers can proceed without platform branching.
    LaunchedEffect(Unit) { onResult(true) }
}
