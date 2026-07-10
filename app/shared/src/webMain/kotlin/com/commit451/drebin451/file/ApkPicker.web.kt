package com.commit451.drebin451.file

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlin.io.encoding.Base64

/**
 * Web APK picker, shared across js + wasmJs. The actual file dialog and byte read live in plain JS
 * (`window.drebinPickApk` in index.html) because FileReader interop differs awkwardly between the
 * two web backends; only the thin token-style bridge [pickApkRaw] is platform-specific, mirroring
 * how Google sign-in is wired (see fetchGoogleIdToken). The JS side hands back the filename, a
 * newline, then the base64 bytes — or an empty string if the user cancelled.
 */
@Composable
actual fun rememberApkPicker(onResult: (PickedApk?) -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    return {
        scope.launch {
            // A read failure (or cancel) yields null, matching Android — the caller just no-ops.
            val picked = runCatching { parsePickedApk(pickApkRaw()) }.getOrNull()
            onResult(picked)
        }
    }
}

/**
 * Opens the browser file picker and suspends until the user chooses a file or dismisses the dialog.
 * Returns "<filename>\n<base64 of the bytes>", or an empty string on cancel. Implemented per web
 * backend (js vs wasmJs) since the JS interop differs between Kotlin/JS and Kotlin/Wasm.
 */
internal expect suspend fun pickApkRaw(): String

/**
 * Splits the "<name>\n<base64>" payload from JS back into a [PickedApk], or null if the user
 * cancelled (empty / delimiter-less string). The newline can't appear in a filename, so the first
 * one cleanly separates the name from the base64 body.
 */
private fun parsePickedApk(raw: String): PickedApk? {
    val newline = raw.indexOf('\n')
    if (newline < 0) return null
    val name = raw.substring(0, newline)
    val bytes = Base64.Default.decode(raw.substring(newline + 1))
    return PickedApk(name.ifBlank { "app.apk" }, bytes)
}
