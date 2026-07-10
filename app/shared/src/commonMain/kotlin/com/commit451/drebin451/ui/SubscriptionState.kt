package com.commit451.drebin451.ui

import com.commit451.drebin451.auth.UserManager
import com.commit451.drebin451.model.User

data class SubscriptionState(
    val user: User? = UserManager.current(),
    val loading: Boolean = true,
    val busy: Boolean = false,
    val message: String? = null,
    val externalUrl: String? = null,
    val awaitingExternalFlow: Boolean = false,
)
