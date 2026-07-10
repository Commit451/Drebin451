package com.commit451.drebin451.ui

private external fun drebinAuthInstall(
    startOnSignUp: Boolean,
    onEmailSubmit: () -> Unit,
    onForgotPassword: () -> Unit,
    onGoogleSubmit: () -> Unit,
): () -> Unit

private external fun drebinAuthEmail(): String

private external fun drebinAuthPassword(): String

private external fun drebinAuthRegister(): Boolean

private external fun drebinAuthSetState(busy: Boolean, error: String, message: String)

internal actual fun installWebAuthScreen(
    startOnSignUp: Boolean,
    onEmailSubmit: () -> Unit,
    onForgotPassword: () -> Unit,
    onGoogleSubmit: () -> Unit,
): WebAuthScreenHandle {
    val dispose = drebinAuthInstall(startOnSignUp, onEmailSubmit, onForgotPassword, onGoogleSubmit)
    return object : WebAuthScreenHandle {
        override fun dispose() {
            dispose()
        }
    }
}

internal actual fun readWebAuthForm(): WebAuthForm =
    WebAuthForm(
        email = drebinAuthEmail(),
        password = drebinAuthPassword(),
        register = drebinAuthRegister(),
    )

internal actual fun updateWebAuthScreen(busy: Boolean, error: String, message: String) {
    drebinAuthSetState(busy, error, message)
}
