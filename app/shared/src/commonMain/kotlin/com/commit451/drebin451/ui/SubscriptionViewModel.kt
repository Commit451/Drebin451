package com.commit451.drebin451.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commit451.drebin451.api.Api
import com.commit451.drebin451.auth.UserManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SubscriptionViewModel : ViewModel() {
    private val _state = MutableStateFlow(SubscriptionState())
    val state: StateFlow<SubscriptionState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, message = null) }
            val currentUser = UserManager.current()
            if (currentUser == null) {
                _state.update { it.copy(loading = false, message = "Sign in to manage your plan.") }
                return@launch
            }

            val refreshedUser = try {
                Api.refreshSubscription()
            } catch (t: Throwable) {
                _state.update {
                    it.copy(
                        loading = false,
                        message = t.message ?: "Couldn't refresh plan"
                    )
                }
                currentUser
            }
            UserManager.set(refreshedUser)
            _state.update {
                it.copy(
                    user = refreshedUser,
                    loading = false,
                )
            }
        }
    }

    fun subscribe() {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            try {
                val session = Api.createBillingCheckoutSession()
                _state.update {
                    it.copy(
                        busy = false,
                        externalUrl = session.url,
                        awaitingExternalFlow = true,
                        message = "Complete checkout in Stripe to activate Pro.",
                    )
                }
            } catch (t: Throwable) {
                _state.update {
                    it.copy(
                        busy = false,
                        message = t.message ?: "Could not start checkout"
                    )
                }
            }
        }
    }

    fun manageBilling() {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, message = null) }
            try {
                val session = Api.createBillingPortalSession()
                _state.update {
                    it.copy(
                        busy = false,
                        externalUrl = session.url,
                        awaitingExternalFlow = true,
                        message = "Manage your subscription in Stripe.",
                    )
                }
            } catch (t: Throwable) {
                _state.update {
                    it.copy(
                        busy = false,
                        message = t.message ?: "Could not open billing portal"
                    )
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            refreshSubscription(message = "Plan refreshed")
        }
    }

    fun refreshAfterExternalFlowIfNeeded() {
        if (!_state.value.awaitingExternalFlow) return
        viewModelScope.launch {
            _state.update { it.copy(awaitingExternalFlow = false) }
            refreshSubscription(message = "Plan refreshed from Stripe")
        }
    }

    fun externalUrlOpened() {
        _state.update { it.copy(externalUrl = null) }
    }

    fun externalUrlOpenFailed(t: Throwable) {
        _state.update {
            it.copy(
                externalUrl = null,
                awaitingExternalFlow = false,
                message = t.message ?: "Could not open Stripe",
            )
        }
    }

    fun messageShown() {
        _state.update { it.copy(message = null) }
    }

    private suspend fun refreshSubscription(message: String) {
        _state.update { it.copy(busy = true, message = null) }
        try {
            val updatedUser = Api.refreshSubscription()
            UserManager.set(updatedUser)
            _state.update { it.copy(user = updatedUser, busy = false, message = message) }
        } catch (t: Throwable) {
            _state.update { it.copy(busy = false, message = t.message ?: "Refresh failed") }
        }
    }
}
