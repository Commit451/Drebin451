package com.commit451.drebin451.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

/**
 * Web Google sign-in. The flow is implemented per web backend because Firebase's popup API is a JS
 * external, but it signs into the same gitlive Firebase Auth instance used by the rest of the app.
 * That keeps web on Firebase's hosted OAuth redirect flow instead of Google Identity Services'
 * JavaScript-origin-bound ID token prompt.
 */
@Composable
actual fun LoginButtons(
    enabled: Boolean,
    existingEmail: String,
    existingPassword: String,
    onResult: (Result<Unit>) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }

    Button(
        onClick = {
            if (!enabled || busy) return@Button
            busy = true
            scope.launch {
                val result = runCatching { signInWithGoogle(existingEmail, existingPassword) }
                busy = false
                onResult(result)
            }
        },
        enabled = enabled && !busy,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(if (busy) "Signing in…" else "Continue with Google")
    }
}

/**
 * Establishes a Firebase session through the Firebase Web SDK popup flow. Throws with a
 * user-facing message; the caller maps that onto the login screen error.
 */
internal suspend fun signInWithGoogle(existingEmail: String, existingPassword: String) {
    try {
        firebaseSignInWithGooglePopup(existingEmail, existingPassword)
    } catch (t: Throwable) {
        throw Exception(t.message ?: "Google sign-in was cancelled.", t)
    }
}

/**
 * Opens the Firebase Auth Google popup and suspends until the user signs in or cancels. Implemented
 * per web backend since the JS/Wasm external interop differs slightly.
 */
internal expect suspend fun firebaseSignInWithGooglePopup(existingEmail: String, existingPassword: String)
