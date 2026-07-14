package com.commit451.drebin451.api

import com.commit451.drebin451.file.PlatformUploadResponse
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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

    @Test
    fun uploadResponse_decodesCreatedVersion() {
        val version = appVersionFromUploadResponse(
            PlatformUploadResponse(
                statusCode = 201,
                body = "{\"id\":\"version-1\",\"fileName\":\"large.apk\",\"fileSizeBytes\":1073741824}",
            ),
        )

        assertEquals("version-1", version.id)
        assertEquals("large.apk", version.fileName)
        assertEquals(1L shl 30, version.fileSizeBytes)
    }

    @Test
    fun uploadResponse_preservesStructuredApiErrors() {
        val error = assertFailsWith<HttpException> {
            appVersionFromUploadResponse(
                PlatformUploadResponse(
                    statusCode = 402,
                    body = "{\"errorMessage\":\"Storage limit exceeded\"}",
                ),
            )
        }

        assertEquals(HttpStatusCode.PaymentRequired, error.statusCode)
        assertEquals("Storage limit exceeded", error.message)
    }
}
