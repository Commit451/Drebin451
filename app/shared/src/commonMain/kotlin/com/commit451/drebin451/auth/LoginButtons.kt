package com.commit451.drebin451.auth

import androidx.compose.runtime.Composable

/**
 * Renders the platform sign-in button(s). [onResult] fires once the Firebase sign-in
 * flow completes (success or failure). Android and web both show a Google sign-in button.
 */
@Composable
expect fun LoginButtons(
    enabled: Boolean,
    existingEmail: String,
    existingPassword: String,
    onResult: (Result<Unit>) -> Unit,
)
