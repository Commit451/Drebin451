package com.commit451.drebin451.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.commit451.drebin451.api.Api
import com.commit451.drebin451.model.BillingPrice
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class PricingViewModel(
    private val loadProPrice: suspend () -> BillingPrice = {
        Api.proPlanPrice()
    },
) : ViewModel() {
    private val _state = MutableStateFlow(PricingState())
    val state: StateFlow<PricingState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val price = try {
                loadProPrice()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                null
            }
            _state.update { it.copy(proPrice = price, loading = false) }
        }
    }
}
