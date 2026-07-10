package com.commit451.drebin451.push

import androidx.compose.runtime.Composable

/**
 * Requests the runtime notification permission, invoking [onResult] with whether it's granted.
 * On Android 13+ this shows the system POST_NOTIFICATIONS dialog (or returns the existing decision
 * without a dialog if already decided); on older Android and on web it reports granted immediately.
 *
 * Like the other `@Composable expect` surfaces (e.g. [com.commit451.drebin451.file.rememberApkPicker]),
 * drop this into composition to trigger it — typically gated behind a state flag so it fires once
 * per request.
 */
@Composable
expect fun RequestNotificationPermission(onResult: (Boolean) -> Unit)
