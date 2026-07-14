package com.commit451.drebin451.apk

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith

class ApkInfoTest {

    @Test
    fun `rejects bytes that are not an apk`() {
        // Should surface as IllegalArgumentException (-> HTTP 400), never an opaque 500.
        assertFailsWith<IllegalArgumentException> {
            parseApk("definitely not an apk".encodeToByteArray())
        }
    }

    @Test
    fun `rejects a file that is not an apk without loading it into a byte array`() {
        val file = File.createTempFile("drebin451-invalid-", ".apk")
        try {
            file.writeText("definitely not an apk")

            assertFailsWith<IllegalArgumentException> { parseApk(file) }
        } finally {
            file.delete()
        }
    }

    @Test
    fun `rejects a zip without an android manifest`() {
        val zip = ByteArrayOutputStream().use { bos ->
            ZipOutputStream(bos).use { zos ->
                zos.putNextEntry(ZipEntry("hello.txt"))
                zos.write("hi".encodeToByteArray())
                zos.closeEntry()
            }
            bos.toByteArray()
        }
        assertFailsWith<IllegalArgumentException> { parseApk(zip) }
    }

    @Test
    fun `rejects a compressed apk resource that would inflate beyond the parser limit`() {
        val file = File.createTempFile("drebin451-zip-bomb-", ".apk")
        try {
            ZipOutputStream(file.outputStream().buffered()).use { zip ->
                zip.putNextEntry(ZipEntry("AndroidManifest.xml"))
                zip.write("placeholder".encodeToByteArray())
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("res/drawable/oversized.png"))
                val block = ByteArray(64 * 1024)
                repeat(256) { zip.write(block) }
                zip.write(0)
                zip.closeEntry()
            }

            val error = assertFailsWith<IllegalArgumentException> { parseApk(file) }
            assertContains(error.message.orEmpty(), "resource entry is too large")
        } finally {
            file.delete()
        }
    }
}
