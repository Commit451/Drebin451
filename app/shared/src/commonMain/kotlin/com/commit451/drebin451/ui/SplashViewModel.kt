package com.commit451.drebin451.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commit451.drebin451.api.Api
import com.commit451.drebin451.auth.UserManager
import com.commit451.drebin451.auth.hasRestoredSignedInUser
import com.commit451.drebin451.auth.shouldShowLoggedOutAboutPage
import com.commit451.drebin451.model.Config
import com.commit451.drebin451.navigation.AboutRoute
import com.commit451.drebin451.navigation.LoginRoute
import com.commit451.drebin451.navigation.requestedPublicLandingRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the splash screen: fetch the app [Config], then resolve auth. The result is exposed as
 * one-shot navigation flags the [SplashScreen] reacts to ([shouldNavigate] → home,
 * [loggedOutRoute] → public logged-out route or login).
 */
internal class SplashViewModel : ViewModel() {
    private val _state = MutableStateFlow(SplashState())
    val state: StateFlow<SplashState> = _state.asStateFlow()

    init {
        load()
    }

    fun retry() {
        load()
    }

    private fun load() {
        _state.value = SplashState()
        viewModelScope.launch {
            val fastAuthRouting = shouldUseFastAuthStartupRouting()

            // Config is best-effort: a transient fetch failure shouldn't block startup, so fall
            // back to defaults and carry on. Web intentionally skips this startup network gate so
            // the invisible splash route only waits for local Firebase auth restoration, then sends
            // users directly to Home or the logged-out landing page.
            if (!fastAuthRouting) {
                val config = try {
                    Api.config()
                } catch (t: Throwable) {
                    Config()
                }
                if (config.disabled) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = config.message.ifBlank {
                                "Drebin451 is currently unavailable. Please check back later."
                            },
                            canRetry = false,
                        )
                    }
                    return@launch
                }
            }
            // Wait for the platform auth SDK to restore any persisted local session before routing.
            // Firebase Web restores currentUser asynchronously after reload, so checking too early
            // sends returning users to the public landing page even though their session is valid.
            if (!hasRestoredSignedInUser()) {
                val showAbout = shouldShowLoggedOutAboutPage()
                _state.update {
                    it.copy(
                        isLoading = false,
                        loggedOutRoute = if (showAbout) {
                            requestedPublicLandingRoute() ?: AboutRoute
                        } else {
                            LoginRoute()
                        },
                    )
                }
                return@launch
            }
            if (fastAuthRouting) {
                _state.update { it.copy(isLoading = false, shouldNavigate = true) }
                return@launch
            }
            // Signed in: load the backend user before showing home. A failure here is retryable.
            try {
                val user = Api.user()
                UserManager.set(user)
                _state.update { it.copy(isLoading = false, shouldNavigate = true) }
            } catch (t: Throwable) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Couldn't reach Drebin. Check your connection and try again.",
                        canRetry = true,
                    )
                }
            }
        }
    }
}
