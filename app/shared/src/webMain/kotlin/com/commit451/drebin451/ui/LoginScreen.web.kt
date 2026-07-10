package com.commit451.drebin451.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.commit451.drebin451.auth.AuthStatus
import com.commit451.drebin451.auth.AuthViewModel
import com.commit451.drebin451.auth.signInWithGoogle
import com.commit451.drebin451.navigation.HomeRoute
import com.commit451.drebin451.navigation.LocalAppNavigator
import kotlinx.coroutines.launch

@Composable
actual fun LoginScreen(startOnSignUp: Boolean) {
    val navigator = LocalAppNavigator.current
    val viewModel: AuthViewModel = viewModel { AuthViewModel() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var googleBusy by remember { mutableStateOf(false) }
    val busy = state.status == AuthStatus.Loading || googleBusy

    DisposableEffect(startOnSignUp) {
        val handle = installWebAuthScreen(
            startOnSignUp = startOnSignUp,
            onEmailSubmit = {
                val form = readWebAuthForm()
                viewModel.submitEmailAuth(
                    email = form.email,
                    password = form.password,
                    register = form.register,
                )
            },
            onForgotPassword = {
                val form = readWebAuthForm()
                viewModel.requestPasswordReset(form.email)
            },
            onGoogleSubmit = {
                if (googleBusy) return@installWebAuthScreen
                googleBusy = true
                scope.launch {
                    val form = readWebAuthForm()
                    val result = runCatching { signInWithGoogle(form.email, form.password) }
                    googleBusy = false
                    result
                        .onSuccess { viewModel.onSignedIn() }
                        .onFailure { viewModel.onSignInError(it) }
                }
            },
        )
        onDispose { handle.dispose() }
    }

    LaunchedEffect(busy, state.error, state.message) {
        updateWebAuthScreen(
            busy = busy,
            error = state.error.orEmpty(),
            message = state.message.orEmpty(),
        )
    }

    LaunchedEffect(state.status) {
        if (state.status == AuthStatus.SignedIn) {
            navigator.replaceAll(HomeRoute)
        }
    }
}
