package com.commit451.drebin451.auth

import com.commit451.drebin451.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Holds the signed-in [User] in memory for app-wide access. */
object UserManager {
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    fun set(user: User?) {
        _user.value = user
    }

    fun current(): User? = _user.value
}
