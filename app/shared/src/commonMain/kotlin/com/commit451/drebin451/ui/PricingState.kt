package com.commit451.drebin451.ui

import com.commit451.drebin451.model.BillingPrice

internal data class PricingState(
    val proPrice: BillingPrice? = null,
    val loading: Boolean = true,
)
