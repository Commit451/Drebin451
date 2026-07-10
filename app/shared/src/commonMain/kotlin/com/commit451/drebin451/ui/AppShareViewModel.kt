package com.commit451.drebin451.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commit451.drebin451.api.Api
import com.commit451.drebin451.model.App
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AppShareViewModel(initialApp: App) : ViewModel() {
    private val _state = MutableStateFlow(AppShareState(app = initialApp))
    val state: StateFlow<AppShareState> = _state.asStateFlow()

    fun refreshShareLink() {
        if (_state.value.refreshingShareId) return
        viewModelScope.launch {
            _state.update { it.copy(refreshingShareId = true) }
            try {
                val updated = Api.refreshAppShareId(_state.value.app.id)
                _state.update {
                    it.copy(
                        app = updated,
                        refreshingShareId = false,
                        message = "Share link refreshed",
                    )
                }
            } catch (t: Throwable) {
                _state.update {
                    it.copy(
                        refreshingShareId = false,
                        message = t.message ?: "Couldn't refresh share link",
                    )
                }
            }
        }
    }

    fun notify(message: String) {
        _state.update { it.copy(message = message) }
    }

    fun messageShown() {
        _state.update { it.copy(message = null) }
    }
}
