package com.commit451.drebin451.stripe

import com.commit451.drebin451.model.PlanIds

data class StripeSubscriptionUpdate(
    val uid: String? = null,
    val customerId: String,
    val subscriptionId: String = "",
    val status: String = "",
    val priceId: String = "",
    val currentPeriodEnd: Long = 0,
    val plan: String = PlanIds.FREE,
)
