package com.commit451.drebin451.model

import kotlinx.serialization.Serializable

/** A server-created Stripe flow that the client should open in the user's browser. */
@Serializable
data class BillingSession(
    val url: String,
)
