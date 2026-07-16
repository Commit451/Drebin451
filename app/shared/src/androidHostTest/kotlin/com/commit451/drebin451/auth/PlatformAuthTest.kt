package com.commit451.drebin451.auth

import kotlin.test.Test
import kotlin.test.assertFalse

class PlatformAuthTest {
    @Test
    fun blankGoogleClientIdLeavesGoogleSignInUnavailable() {
        initializeAuth("   ")

        assertFalse(isGoogleSignInAvailable)
    }
}
