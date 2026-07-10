package com.commit451.drebin451.auth

data class AuthUiState(
    val status: AuthStatus = AuthStatus.SignedOut,
    val error: String? = null,
    val message: String? = null,
)
