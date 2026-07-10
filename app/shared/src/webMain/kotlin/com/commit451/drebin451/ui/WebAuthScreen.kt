package com.commit451.drebin451.ui

internal expect fun installWebAuthScreen(
    startOnSignUp: Boolean,
    onEmailSubmit: () -> Unit,
    onForgotPassword: () -> Unit,
    onGoogleSubmit: () -> Unit,
): WebAuthScreenHandle

internal expect fun readWebAuthForm(): WebAuthForm

internal expect fun updateWebAuthScreen(busy: Boolean, error: String, message: String)
