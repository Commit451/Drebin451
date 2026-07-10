package com.commit451.drebin451.ui

import com.commit451.drebin451.api.HttpException
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StorageLimitUploadDialogTest {

    @Test
    fun uploadStorageLimitDialogMessage_returnsFriendlyMessageForPaymentRequiredUploadFailure() {
        val error = HttpException(
            statusCode = HttpStatusCode.PaymentRequired,
            message = "Storage limit exceeded, please update your plan to continue uploading",
        )

        assertEquals(
            "Your APK couldn't be uploaded because your account is out of storage. Upgrade your plan to add more space, then try uploading again.",
            uploadStorageLimitDialogMessage(error),
        )
    }

    @Test
    fun uploadStorageLimitDialogMessage_returnsFriendlyMessageForWrappedPaymentRequiredUploadFailure() {
        val error = IllegalStateException(
            "Upload failed",
            HttpException(statusCode = HttpStatusCode.PaymentRequired),
        )

        assertEquals(
            "Your APK couldn't be uploaded because your account is out of storage. Upgrade your plan to add more space, then try uploading again.",
            uploadStorageLimitDialogMessage(error),
        )
    }

    @Test
    fun uploadStorageLimitDialogMessage_ignoresOtherUploadFailures() {
        val error = HttpException(
            statusCode = HttpStatusCode.BadRequest,
            message = "Not a valid APK",
        )

        assertNull(uploadStorageLimitDialogMessage(error))
    }
}
