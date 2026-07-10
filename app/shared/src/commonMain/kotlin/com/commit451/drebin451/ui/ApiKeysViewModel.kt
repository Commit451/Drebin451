package com.commit451.drebin451.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commit451.drebin451.api.Api
import com.commit451.drebin451.model.ApiKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ApiKeysViewModel : ViewModel() {
    private val _state = MutableStateFlow(ApiKeysState())
    val state: StateFlow<ApiKeysState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            try {
                val keys = Api.apiKeys()
                _state.update { it.copy(keys = keys, loading = false) }
            } catch (t: Throwable) {
                _state.update {
                    it.copy(
                        loading = false,
                        message = t.message ?: "Failed to load API keys"
                    )
                }
            }
        }
    }

    /**
     * User-initiated pull-to-refresh. Drives [ApiKeysState.refreshing] and surfaces failures, so a
     * deliberate refresh that didn't work is visible.
     */
    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(refreshing = true) }
            try {
                val keys = Api.apiKeys()
                _state.update { it.copy(keys = keys, refreshing = false) }
            } catch (t: Throwable) {
                _state.update {
                    it.copy(
                        refreshing = false,
                        message = t.message ?: "Failed to refresh"
                    )
                }
            }
        }
    }

    fun create(label: String) {
        viewModelScope.launch {
            _state.update { it.copy(creating = true) }
            try {
                val created = Api.createApiKey(label)
                // Surface the plaintext token via createdKey and prepend the new key to the list.
                _state.update {
                    it.copy(
                        creating = false,
                        createdKey = created,
                        keys = listOf(created.apiKey) + it.keys,
                    )
                }
            } catch (t: Throwable) {
                _state.update {
                    it.copy(
                        creating = false,
                        message = t.message ?: "Failed to create key"
                    )
                }
            }
        }
    }

    fun delete(key: ApiKey) {
        viewModelScope.launch {
            try {
                Api.deleteApiKey(key.id)
                _state.update { s ->
                    s.copy(
                        keys = s.keys.filterNot { it.id == key.id },
                        message = "Revoked ${key.label}",
                    )
                }
            } catch (t: Throwable) {
                _state.update { it.copy(message = t.message ?: "Revoke failed") }
            }
        }
    }

    /** Clears the one-time token dialog once the user has copied/acknowledged it. */
    fun createdKeyDismissed() {
        _state.update { it.copy(createdKey = null) }
    }

    fun notify(message: String) {
        _state.update { it.copy(message = message) }
    }

    fun messageShown() {
        _state.update { it.copy(message = null) }
    }
}
