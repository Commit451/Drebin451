package com.commit451.drebin451

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ApplicationPasswordResetTest {

    @Test
    fun `password reset request size accepts bounded body`() {
        requirePasswordResetRequestSize(PasswordResetRequestMaxBytes)
    }

    @Test
    fun `password reset request size rejects oversized body`() {
        val error = assertFailsWith<IllegalArgumentException> {
            requirePasswordResetRequestSize(PasswordResetRequestMaxBytes + 1L)
        }

        assertTrue(error.message.orEmpty().contains("too large"))
    }
}