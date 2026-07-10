package com.commit451.drebin451.share

/** Kotlin/Wasm can't reference JS globals directly, hence the [JsFun] shim (mirrors ApkPicker). */
@JsFun("() => window.location.pathname")
private external fun locationPath(): JsString

internal actual fun currentBrowserPath(): String = locationPath().toString()
