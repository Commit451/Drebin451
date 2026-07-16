package com.commit451.drebin451

import com.commit451.drebin451.firebase.StorageQuotaExceededException
import com.commit451.drebin451.model.PlanIds
import com.commit451.drebin451.model.PlanLimits
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApplicationErrorTest {

    @Test
    fun `storage quota errors are payment required with upload guidance JSON`() {
        val cause = StorageQuotaExceededException(
            plan = PlanIds.FREE,
            usedBytes = 1_073_741_824,
            attemptedBytes = 1,
            limitBytes = 1_073_741_824,
        )

        assertEquals(HttpStatusCode.PaymentRequired, statusForException(cause))
        assertEquals(
            ErrorResponse("Storage limit exceeded, please update your plan to continue uploading"),
            errorResponseForException(cause),
        )
        assertEquals(
            "{\"errorMessage\":\"Storage limit exceeded, please update your plan to continue uploading\"}",
            Json.encodeToString(errorResponseForException(cause)),
        )
    }

    @Test
    fun `wrapped storage quota errors are also payment required`() {
        val cause = StorageQuotaExceededException(
            plan = PlanIds.FREE,
            usedBytes = 1_073_741_824,
            attemptedBytes = 1,
            limitBytes = 1_073_741_824,
        )
        val wrapped = IllegalStateException("Firestore transaction failed", cause)

        assertEquals(HttpStatusCode.PaymentRequired, statusForException(wrapped))
        assertEquals(
            ErrorResponse("Storage limit exceeded, please update your plan to continue uploading"),
            errorResponseForException(wrapped),
        )
    }

    @Test
    fun `pro storage quota errors direct users to contact support`() {
        val cause = StorageQuotaExceededException(
            plan = PlanIds.PRO,
            usedBytes = PlanLimits.PRO_STORAGE_BYTES,
            attemptedBytes = 1,
            limitBytes = PlanLimits.PRO_STORAGE_BYTES,
        )

        assertEquals(HttpStatusCode.PaymentRequired, statusForException(cause))
        assertEquals(
            ErrorResponse(
                "Storage limit exceeded, please contact us at hello@commit451 for additional options for more storage",
            ),
            errorResponseForException(cause),
        )
    }

    @Test
    fun `cron secret accepts drebin specific env before generic env`() {
        assertEquals(
            "specific",
            configuredCronSecret(
                mapOf(
                    "DREBIN451_CRON_SECRET" to " specific ",
                    "CRON_SECRET" to "generic",
                ),
            ),
        )
    }

    @Test
    fun `cron secret falls back to generic env`() {
        assertEquals("generic", configuredCronSecret(mapOf("CRON_SECRET" to " generic ")))
    }

    @Test
    fun `cron secret authorization requires configured and matching secret`() {
        assertTrue(isAuthorizedCronSecret(presented = " secret ", configured = "secret"))
        assertFalse(isAuthorizedCronSecret(presented = "wrong", configured = "secret"))
        assertFalse(isAuthorizedCronSecret(presented = "secret", configured = null))
        assertFalse(isAuthorizedCronSecret(presented = null, configured = "secret"))
    }
}
