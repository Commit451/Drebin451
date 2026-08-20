package com.commit451.drebin451.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.mmk.kmpauth.uihelper.google.GoogleSignInButton
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

    GoogleSignInButton(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .alpha(if (enabled && !busy) 1f else 0.38f),
        text = if (busy) "Signing in…" else "Continue with Google",
        onClick = {
            if (!enabled || busy) return@GoogleSignInButton
            busy = true
            scope.launch {
                val result = runCatching { signInWithGoogle(existingEmail, existingPassword) }
                busy = false
                onResult(result)
            }
        },
    )
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
