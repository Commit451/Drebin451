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
import com.mmk.kmpauth.google.GoogleButtonUiContainer
import kotlinx.coroutines.launch

@Composable
actual fun LoginButtons(
    enabled: Boolean,
    existingEmail: String,
    existingPassword: String,
    onResult: (Result<Unit>) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }

    if (!isGoogleSignInAvailable) {
        Button(
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
            onClick = {},
        ) {
            Text("Google sign-in unavailable")
        }
        return
    }

    GoogleButtonUiContainer(
        filterByAuthorizedAccounts = false,
        onGoogleSignInResult = { googleUser ->
            if (googleUser == null) {
                onResult(Result.failure(Exception("Google sign-in was cancelled.")))
                return@GoogleButtonUiContainer
            }
            busy = true
            scope.launch {
                val result = runCatching {
                    firebaseSignInOrLinkGoogle(
                        idToken = googleUser.idToken,
                        accessToken = googleUser.accessToken,
                        existingEmail = existingEmail,
                        existingPassword = existingPassword,
                    )
                }
                busy = false
                onResult(result)
            }
        },
    ) {
        Button(
            enabled = enabled && !busy,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (!enabled || busy) return@Button
                this.onClick()
            },
        ) {
            Text(if (busy) "Signing in…" else "Continue with Google")
        }
    }
}
