package com.commit451.drebin451.api

import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals

class ApiErrorTest {

    @Test
    fun httpErrorMessage_readsJsonErrorMessage() {
        assertEquals(
            "Storage limit exceeded, please update your plan to continue uploading",
            httpErrorMessage(
                status = HttpStatusCode.PaymentRequired,
                body = "{\"errorMessage\":\"Storage limit exceeded, please update your plan to continue uploading\"}",
            ),
        )
    }

    @Test
    fun httpErrorMessage_fallsBackToPlainTextBody() {
        assertEquals(
            "Missing id",
            httpErrorMessage(
                status = HttpStatusCode.BadRequest,
                body = "Missing id",
            ),
        )
    }

    @Test
    fun httpErrorMessage_fallsBackToStatusForBlankBody() {
        assertEquals(
            "HTTP error 404",
            httpErrorMessage(
                status = HttpStatusCode.NotFound,
                body = "",
            ),
        )
    }
}
