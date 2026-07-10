package com.commit451.drebin451.share

import androidx.compose.runtime.Composable

/**
 * Returns a launcher that hands a share link to the platform's share UI. Android opens the system
 * share sheet; web returns null because the share dialog's Copy action covers web. Mirrors
 * [com.commit451.drebin451.file.rememberApkPicker].
 */
@Composable
expect fun rememberShareLauncher(): ((String) -> Unit)?
