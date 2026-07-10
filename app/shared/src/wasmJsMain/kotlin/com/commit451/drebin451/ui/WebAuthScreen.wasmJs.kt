@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.commit451.drebin451.ui

@JsFun(
    "(startOnSignUp, onEmailSubmit, onForgotPassword, onGoogleSubmit) => " +
            "window.drebinAuthInstall(startOnSignUp, onEmailSubmit, onForgotPassword, onGoogleSubmit)",
)
private external fun jsInstallWebAuthScreen(
    startOnSignUp: Boolean,
    onEmailSubmit: () -> Unit,
    onForgotPassword: () -> Unit,
    onGoogleSubmit: () -> Unit,
): JsAny

@JsFun("() => window.drebinAuthEmail()")
private external fun jsWebAuthEmail(): JsString

@JsFun("() => window.drebinAuthPassword()")
private external fun jsWebAuthPassword(): JsString

@JsFun("() => window.drebinAuthRegister()")
private external fun jsWebAuthRegister(): Boolean

@JsFun("(busy, error, message) => { window.drebinAuthSetState(busy, error, message); }")
private external fun jsUpdateWebAuthScreen(busy: Boolean, error: String, message: String)

@JsFun("(dispose) => { dispose(); }")
private external fun jsDisposeWebAuthScreen(dispose: JsAny)

internal actual fun installWebAuthScreen(
    startOnSignUp: Boolean,
    onEmailSubmit: () -> Unit,
    onForgotPassword: () -> Unit,
    onGoogleSubmit: () -> Unit,
): WebAuthScreenHandle {
    val dispose =
        jsInstallWebAuthScreen(startOnSignUp, onEmailSubmit, onForgotPassword, onGoogleSubmit)
    return object : WebAuthScreenHandle {
        override fun dispose() {
            jsDisposeWebAuthScreen(dispose)
        }
    }
}

internal actual fun readWebAuthForm(): WebAuthForm =
    WebAuthForm(
        email = jsWebAuthEmail().toString(),
        password = jsWebAuthPassword().toString(),
        register = jsWebAuthRegister(),
    )

internal actual fun updateWebAuthScreen(busy: Boolean, error: String, message: String) {
    jsUpdateWebAuthScreen(busy, error, message)
}
