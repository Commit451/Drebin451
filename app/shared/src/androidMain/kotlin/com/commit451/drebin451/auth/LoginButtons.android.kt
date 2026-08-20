package com.commit451.drebin451.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.mmk.kmpauth.google.rememberGoogleSignInState
import com.mmk.kmpauth.uihelper.google.GoogleSignInButton
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

    val googleSignIn = rememberGoogleSignInState(
        filterByAuthorizedAccounts = false,
        onResult = { signInResult ->
            signInResult
                .onSuccess { googleUser ->
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
                }
                .onFailure { throwable ->
                    onResult(Result.failure(throwable))
                }
        },
    )
    val canLaunch = enabled && !busy && !googleSignIn.isInProgress

    GoogleSignInButton(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .alpha(if (canLaunch) 1f else 0.38f),
        text = if (busy || googleSignIn.isInProgress) "Signing in…" else "Continue with Google",
        onClick = {
            if (canLaunch) googleSignIn.launch()
        },
    )
}
