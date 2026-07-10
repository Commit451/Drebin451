package com.commit451.drebin451.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthViewModelTest {

    @Test
    fun googleAccountLinkRequiredMessage_usesExistingEmailWhenAvailable() {
        assertEquals(
            "An email/password account already exists for jawn@example.com. Enter that account's password, then continue with Google again to link Google sign-in.",
            googleAccountLinkRequiredMessage("  jawn@example.com  "),
        )
    }

    @Test
    fun googleAccountLinkRequiredMessage_handlesMissingEmail() {
        assertEquals(
            "An email/password account already exists for that email. Enter that account's password, then continue with Google again to link Google sign-in.",
            googleAccountLinkRequiredMessage("   "),
        )
    }

    @Test
    fun passwordResetEmailError_requiresEmail() {
        assertEquals("Enter your email address first.", passwordResetEmailError("   "))
    }

    @Test
    fun passwordResetEmailError_acceptsTrimmedEmail() {
        assertNull(passwordResetEmailError("  jawn@example.com  "))
    }

    @Test
    fun passwordResetSuccessMessage_usesTrimmedEmailWithoutPromisingAccountExists() {
        assertEquals(
            "If an account exists for jawn@example.com, we sent a password reset email.",
            passwordResetSuccessMessage("  jawn@example.com  "),
        )
    }

    @Test
    fun passwordResetSuccessState_doesNotOverwriteSignedInState() {
        val signedIn = AuthUiState(status = AuthStatus.SignedIn)

        assertEquals(
            signedIn,
            passwordResetSuccessState(signedIn, "jawn@example.com"),
        )
    }

    @Test
    fun passwordResetErrorState_doesNotOverwriteSignedInState() {
        val signedIn = AuthUiState(status = AuthStatus.SignedIn)

        assertEquals(
            signedIn,
            passwordResetErrorState(signedIn, IllegalStateException("Reset failed")),
        )
    }
}
