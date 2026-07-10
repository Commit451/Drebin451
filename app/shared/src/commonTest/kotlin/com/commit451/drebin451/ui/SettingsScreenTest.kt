package com.commit451.drebin451.ui

import com.commit451.drebin451.model.User
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsScreenTest {

    @Test
    fun signedInAccountSummaryShowsUserEmail() {
        val user = User(email = "jawn@example.com")

        assertEquals("jawn@example.com", user.accountSummary())
    }

    @Test
    fun deleteAccountConfirmationNamesTheSignedInEmail() {
        assertEquals(
            "This permanently deletes \"jawn@example.com\", every app and upload you own, your API keys, " +
                    "and your saved shared-app list. Any active Pro subscription will also be canceled. " +
                    "This can't be undone.",
            deleteAccountConfirmationText(" jawn@example.com "),
        )
    }

    @Test
    fun deleteAccountConfirmationFallsBackWhenEmailIsBlank() {
        assertEquals(
            "This permanently deletes this account, every app and upload you own, your API keys, " +
                    "and your saved shared-app list. Any active Pro subscription will also be canceled. " +
                    "This can't be undone.",
            deleteAccountConfirmationText("   "),
        )
    }
}
