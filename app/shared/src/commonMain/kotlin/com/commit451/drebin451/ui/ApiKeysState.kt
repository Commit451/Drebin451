package com.commit451.drebin451.ui

import com.commit451.drebin451.model.ApiKey
import com.commit451.drebin451.model.ApiKeyCreated

data class ApiKeysState(
    val keys: List<ApiKey> = emptyList(),
    val loading: Boolean = true,
    // Drives the pull-to-refresh indicator; distinct from [loading] (the initial full-screen spinner).
    val refreshing: Boolean = false,
    val creating: Boolean = false,
    // Set after a successful create so the screen can show the one-time plaintext token, then
    // cleared by createdKeyDismissed() once the user has copied/acknowledged it.
    val createdKey: ApiKeyCreated? = null,
    val message: String? = null,
)
