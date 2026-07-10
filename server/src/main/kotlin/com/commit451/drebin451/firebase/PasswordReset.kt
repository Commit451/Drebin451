package com.commit451.drebin451.firebase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

private const val PasswordResetRequestType = "PASSWORD_RESET"
internal const val MaxPasswordResetEmailLength = 254
private val PasswordResetJson = Json { ignoreUnknownKeys = true }
internal val passwordResetRateLimiter = PasswordResetRateLimiter()

internal fun normalizePasswordResetEmail(email: String): String {
    val trimmed = email.trim().takeIf { it.isNotEmpty() }
        ?: throw IllegalArgumentException("Enter an email address.")
    require(trimmed.length <= MaxPasswordResetEmailLength) { "Enter a valid email address." }
    return trimmed
}

internal fun passwordResetRequestBody(email: String): String =
    PasswordResetJson.encodeToString(
        FirebasePasswordResetRequest(
            requestType = PasswordResetRequestType,
            email = normalizePasswordResetEmail(email),
        ),
    )

internal fun isPasswordResetNonDisclosureError(errorCode: String?): Boolean =
    errorCode == "EMAIL_NOT_FOUND"

internal fun isPasswordResetValidationError(errorCode: String?): Boolean =
    errorCode in setOf("INVALID_EMAIL", "MISSING_EMAIL")

internal fun firebaseWebApiKey(env: Map<String, String> = System.getenv()): String =
    env["DREBIN451_FIREBASE_WEB_API_KEY"]
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: env["FIREBASE_WEB_API_KEY"]
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        ?: error(
            "DREBIN451_FIREBASE_WEB_API_KEY (or FIREBASE_WEB_API_KEY) is required " +
                    "to send password-reset emails",
        )
