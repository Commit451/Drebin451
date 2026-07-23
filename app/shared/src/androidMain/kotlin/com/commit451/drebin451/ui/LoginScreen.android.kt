package com.commit451.drebin451.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.commit451.drebin451.auth.AuthStatus
import com.commit451.drebin451.auth.AuthViewModel
import com.commit451.drebin451.auth.LoginButtons
import com.commit451.drebin451.navigation.HomeRoute
import com.commit451.drebin451.navigation.LocalAppNavigator

@Composable
actual fun LoginScreen(startOnSignUp: Boolean) {
    val navigator = LocalAppNavigator.current
    val uriHandler = LocalUriHandler.current
    val viewModel: AuthViewModel = viewModel { AuthViewModel() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val busy = state.status == AuthStatus.Loading
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var register by rememberSaveable { mutableStateOf(startOnSignUp) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.status) {
        if (state.status == AuthStatus.SignedIn) {
            navigator.replaceAll(HomeRoute)
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let { snackbarHostState.showSnackbar(it) }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val horizontalMargin = ContentLayout.horizontalMargin(maxWidth, minimum = 24.dp)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizontalMargin, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            DrebinLogo(modifier = Modifier.size(104.dp))
            Spacer(Modifier.height(16.dp))
            Text("Drebin451", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                "Your own private Play Store.\nSign in to upload and share your apps.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))

            EmailPasswordForm(
                enabled = !busy,
                email = email,
                onEmailChange = { email = it },
                password = password,
                onPasswordChange = { password = it },
                register = register,
                onRegisterChange = { register = it },
                onSubmit = { submittedEmail, submittedPassword, submittedRegister ->
                    viewModel.submitEmailAuth(submittedEmail, submittedPassword, submittedRegister)
                },
                onForgotPassword = { submittedEmail ->
                    viewModel.requestPasswordReset(submittedEmail)
                },
            )

            Spacer(Modifier.height(24.dp))
            OrSeparator()
            Spacer(Modifier.height(24.dp))

            LoginButtons(
                enabled = !busy,
                existingEmail = email,
                existingPassword = password,
                onResult = { result ->
                    result
                        .onSuccess { viewModel.onSignedIn() }
                        .onFailure { viewModel.onSignInError(it) }
                },
            )

            Spacer(Modifier.height(16.dp))
            LegalLinksRow(
                onPrivacyClick = { uriHandler.openUri(LegalLinks.PRIVACY_POLICY_URL) },
                onTermsClick = { uriHandler.openUri(LegalLinks.TERMS_URL) },
            )

            val error = state.error
            if (error != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeContentPadding()
                .padding(16.dp),
        )
    }
}

@Composable
private fun LegalLinksRow(
    onPrivacyClick: () -> Unit,
    onTermsClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onPrivacyClick) { Text("Privacy Policy") }
        Text(
            "•",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onTermsClick) { Text("Terms") }
    }
}

@Composable
private fun EmailPasswordForm(
    enabled: Boolean,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    register: Boolean,
    onRegisterChange: (Boolean) -> Unit,
    onSubmit: (email: String, password: String, register: Boolean) -> Unit,
    onForgotPassword: (email: String) -> Unit,
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val canSubmit = enabled && email.isNotBlank() && password.isNotBlank()
    val submit = { if (canSubmit) onSubmit(email, password, register) }

    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        label = { Text("Email") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
        ),
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        label = { Text("Password") },
        singleLine = true,
        visualTransformation = if (passwordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { submit() }),
        trailingIcon = {
            TextButton(
                onClick = { passwordVisible = !passwordVisible },
                enabled = enabled,
            ) {
                Text(if (passwordVisible) "Hide" else "Show")
            }
        },
    )
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = submit,
        modifier = Modifier.fillMaxWidth(),
        enabled = canSubmit,
    ) {
        Text(if (register) "Sign Up" else "Sign In")
    }
    if (!register) {
        TextButton(
            onClick = { onForgotPassword(email) },
            enabled = enabled,
        ) {
            Text("Forgot password?")
        }
    }
    TextButton(
        onClick = { onRegisterChange(!register) },
        enabled = enabled,
    ) {
        Text(
            if (register) {
                "Already have an account? Sign in"
            } else {
                "Need an account? Sign up"
            },
        )
    }
}

@Composable
private fun OrSeparator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(
            "  or  ",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}
