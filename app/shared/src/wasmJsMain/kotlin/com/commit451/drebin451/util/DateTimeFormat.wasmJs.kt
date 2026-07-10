@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.commit451.drebin451.util

// Kotlin/Wasm has limited date formatting APIs available here; fall back to the raw epoch value.
actual fun formatDateTime(epochMillis: Long): String =
    if (epochMillis <= 0L) "—" else epochMillis.toString()

@JsFun("() => Date.now()")
private external fun jsDateNow(): Double

internal actual fun currentEpochMillis(): Long = jsDateNow().toLong()
