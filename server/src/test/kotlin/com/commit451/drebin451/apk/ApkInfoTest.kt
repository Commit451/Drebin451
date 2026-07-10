package com.commit451.drebin451.apk

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
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
}
