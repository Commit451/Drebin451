package com.commit451.drebin451

import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UploadStreamingTest {

    @Test
    fun `maximum APK upload size is one gibibyte`() {
        assertEquals(1L shl 30, MAX_APK_UPLOAD_BYTES)
    }

    @Test
    fun `upload size accepts one gibibyte and rejects anything larger`() {
        requireApkUploadSize(MAX_APK_UPLOAD_BYTES)

        assertFailsWith<IllegalArgumentException> {
            requireApkUploadSize(MAX_APK_UPLOAD_BYTES + 1)
        }
    }

    @Test
    fun `upload size rejects an empty file`() {
        assertFailsWith<IllegalArgumentException> { requireApkUploadSize(0) }
    }

    @Test
    fun `upload channel is copied to disk at the configured boundary`() = runBlocking {
        val bytes = "12345678".encodeToByteArray()
        val file = File.createTempFile("drebin451-upload-test-", ".apk")
        try {
            val copied = copyUploadToFile(ByteReadChannel(bytes), file, maxBytes = bytes.size.toLong())

            assertEquals(bytes.size.toLong(), copied)
            assertContentEquals(bytes, file.readBytes())
        } finally {
            file.delete()
        }
    }

    @Test
    fun `upload channel stops after one byte beyond the configured boundary`() = runBlocking {
        val file = File.createTempFile("drebin451-upload-test-", ".apk")
        try {
            assertFailsWith<IllegalArgumentException> {
                copyUploadToFile(
                    ByteReadChannel("123456789".encodeToByteArray()),
                    file,
                    maxBytes = 8,
                )
            }
            assertEquals(9, file.length())
        } finally {
            file.delete()
        }
    }
}
