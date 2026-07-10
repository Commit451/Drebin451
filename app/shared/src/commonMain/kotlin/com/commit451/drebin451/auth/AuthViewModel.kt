package com.commit451.drebin451.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commit451.drebin451.api.Api
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal fun passwordResetEmailError(email: String): String? =
    if (email.trim().isBlank()) "Enter your email address first." else null

internal fun passwordResetSuccessMessage(email: String): String =
    "If an account exists for ${email.trim()}, we sent a password reset email."

internal fun passwordResetSuccessState(current: AuthUiState, email: String): AuthUiState =
    if (current.status == AuthStatus.SignedIn) {
        current
    } else {
        AuthUiState(
            status = AuthStatus.SignedOut,
            message = passwordResetSuccessMessage(email),
        )
    }

internal fun passwordResetErrorState(current: AuthUiState, error: Throwable): AuthUiState =
    if (current.status == AuthStatus.SignedIn) {
        current
    } else {
        AuthUiState(
            status = AuthStatus.SignedOut,
            error = error.message ?: "Could not send password reset email.",
        )
    }

/**
 * Backs the login screen: email/password or Google sign-in, each of which establishes a Firebase
 * session and then loads the backend [User]. The "are we already signed in?" check happens on the
 * splash screen now, so this starts [AuthStatus.SignedOut] and only reaches [AuthStatus.SignedIn]
 * once a sign-in here succeeds — which is the signal the login screen navigates home on.
 */
class AuthViewModel : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    /** Called after the platform (Google) sign-in flow reports success. */
    fun onSignedIn() {
        _state.update { it.copy(status = AuthStatus.Loading, error = null, message = null) }
        loadUser()
    }

    /**
     * Signs in with email + password, or registers a new account when [register] is true,
     * then loads the backend [User] — the same path the Google flow takes once a Firebase
     * session exists.
     */
    fun submitEmailAuth(email: String, password: String, register: Boolean) {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isEmpty() || password.isEmpty()) {
            _state.update { it.copy(error = "Enter an email and password.", message = null) }
            return
        }
        _state.update { it.copy(status = AuthStatus.Loading, error = null, message = null) }
        viewModelScope.launch {
            try {
                if (register) {
                    firebaseCreateUserWithEmail(trimmedEmail, password)
                } else {
                    firebaseSignInWithEmail(trimmedEmail, password)
                }
                loadUser()
            } catch (t: Throwable) {
                _state.value = AuthUiState(
                    status = AuthStatus.SignedOut,
                    error = t.message ?: "Sign-in failed",
                )
            }
        }
    }

    fun requestPasswordReset(email: String) {
        val validationError = passwordResetEmailError(email)
        if (validationError != null) {
            _state.update { it.copy(error = validationError, message = null) }
            return
        }

        val trimmedEmail = email.trim()
        _state.update { it.copy(status = AuthStatus.Loading, error = null, message = null) }
        viewModelScope.launch {
            try {
                Api.requestPasswordReset(trimmedEmail)
                _state.update { current -> passwordResetSuccessState(current, trimmedEmail) }
            } catch (t: Throwable) {
                _state.update { current -> passwordResetErrorState(current, t) }
            }
        }
    }

    fun onSignInError(t: Throwable) {
        viewModelScope.launch {
            runCatching { firebaseSignOut() }
            UserManager.set(null)
            _state.value = AuthUiState(
                status = AuthStatus.SignedOut,
                error = t.message ?: "Sign-in failed",
            )
        }
    }

    private fun loadUser() {
        viewModelScope.launch {
            try {
                val user = Api.user()
                UserManager.set(user)
                _state.value = AuthUiState(status = AuthStatus.SignedIn)
            } catch (t: Throwable) {
                // Token rejected or backend unreachable — fall back to signed out.
                runCatching { firebaseSignOut() }
                UserManager.set(null)
                _state.value = AuthUiState(status = AuthStatus.SignedOut, error = t.message)
            }
        }
    }
}
