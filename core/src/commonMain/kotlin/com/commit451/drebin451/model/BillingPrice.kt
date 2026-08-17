package com.commit451.drebin451.model

import kotlinx.serialization.Serializable

/** The configured recurring Stripe price for the Pro plan. */
@Serializable
data class BillingPrice(
    val unitAmount: Long,
    val currency: String,
    val interval: String,
    val intervalCount: Int = 1,
)
