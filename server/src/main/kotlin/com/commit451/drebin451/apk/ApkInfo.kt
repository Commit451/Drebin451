package com.commit451.drebin451.apk

import net.dongliu.apk.parser.AbstractApkFile
import net.dongliu.apk.parser.ApkFile
import net.dongliu.apk.parser.ByteArrayApkFile
import net.dongliu.apk.parser.bean.IconPath
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile

private const val MAX_APK_ENTRY_COUNT = 100_000
private const val MAX_MANIFEST_BYTES = 16L * 1024 * 1024
private const val MAX_RESOURCE_TABLE_BYTES = 64L * 1024 * 1024
private const val MAX_RESOURCE_ENTRY_BYTES = 16L * 1024 * 1024
private const val MAX_RESOURCE_TOTAL_BYTES = 256L * 1024 * 1024

/** Identity + version metadata pulled from an uploaded APK's manifest/resources. */
data class ApkInfo(
    val applicationId: String,
    val versionName: String,
    val versionCode: Long,
    val label: String,
    /** The launcher icon, when the APK ships a raster one we can serve; null otherwise. */
    val icon: ApkIcon? = null,
)

/**
 * Parses an uploaded APK's bytes into [ApkInfo] using net.dongliu apk-parser (pure JVM, no
 * Android SDK needed). Anything that isn't a readable APK with a package name throws
 * [IllegalArgumentException], which the route's StatusPages handler maps to HTTP 400 rather
 * than a 500.
 *
 * [label] is the app's display name resolved from resources; it falls back to the
 * [applicationId] when absent (some APKs use a non-default locale or a stripped label).
 */
fun parseApk(bytes: ByteArray): ApkInfo = parseApk(
    openApk = { ByteArrayApkFile(bytes) },
    iconExtractor = ::extractInflatedIcon,
)

/** Parses an APK from disk so large uploads never need to be duplicated in the JVM heap. */
fun parseApk(file: File): ApkInfo {
    validateApkArchive(file)
    return parseApk(
        openApk = { ApkFile(file) },
        iconExtractor = { apk -> extractFileIcon(file, apk.iconPaths.orEmpty()) },
    )
}

private inline fun parseApk(
    openApk: () -> AbstractApkFile,
    iconExtractor: (AbstractApkFile) -> ApkIcon?,
): ApkInfo {
    return try {
        openApk().use { apk ->
            val meta = apk.apkMeta
            val applicationId = meta.packageName?.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("APK is missing an applicationId (package name)")
            ApkInfo(
                applicationId = applicationId,
                versionName = meta.versionName ?: "",
                versionCode = meta.versionCode ?: 0L,
                label = meta.label?.takeIf { it.isNotBlank() } ?: applicationId,
                icon = iconExtractor(apk),
            )
        }
    } catch (e: IllegalArgumentException) {
        // Already a client-facing 400 (bad/empty package name) — keep its message.
        throw e
    } catch (t: Throwable) {
        throw IllegalArgumentException("Not a valid APK", t)
    }
}

/**
 * apk-parser inflates manifest/resource/icon ZIP entries into byte arrays. Bound the entries it can
 * reach before invoking it so a small compressed ZIP cannot exhaust the server heap.
 */
private fun validateApkArchive(file: File) {
    try {
        ZipFile(file).use { zip ->
            var entryCount = 0
            var resourceBytes = 0L
            var hasManifest = false
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.isDirectory) continue
                entryCount++
                require(entryCount <= MAX_APK_ENTRY_COUNT) { "APK contains too many ZIP entries" }
                val size = entry.size
                require(size >= 0) { "APK contains an entry with an unknown size" }
                when {
                    entry.name == "AndroidManifest.xml" -> {
                        hasManifest = true
                        require(size <= MAX_MANIFEST_BYTES) { "APK manifest is too large" }
                    }
                    entry.name == "resources.arsc" ->
                        require(size <= MAX_RESOURCE_TABLE_BYTES) { "APK resource table is too large" }
                    entry.name.startsWith("res/") -> {
                        require(size <= MAX_RESOURCE_ENTRY_BYTES) { "APK resource entry is too large" }
                        resourceBytes += size
                        require(resourceBytes <= MAX_RESOURCE_TOTAL_BYTES) { "APK resources are too large" }
                    }
                }
            }
            require(hasManifest) { "APK is missing AndroidManifest.xml" }
        }
    } catch (e: IllegalArgumentException) {
        throw e
    } catch (t: Throwable) {
        throw IllegalArgumentException("Not a valid APK", t)
    }
}

/** Image content types we recognise from an icon's path; null for anything we shouldn't serve. */
private fun contentTypeForIcon(path: String?): String? =
    when (path?.substringAfterLast('.', "")?.lowercase()) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        "jpg", "jpeg" -> "image/jpeg"
        else -> null
    }

/**
 * File-backed uploads read only one bounded raster icon selected from manifest icon paths. This
 * avoids apk-parser eagerly inflating every icon candidate from an untrusted ZIP.
 */
private fun extractFileIcon(file: File, iconPaths: List<IconPath>): ApkIcon? = try {
    ZipFile(file).use { zip ->
        val candidate = iconPaths.asSequence()
            .distinctBy { it.path }
            .take(512)
            .mapNotNull { iconPath ->
                val contentType = contentTypeForIcon(iconPath.path) ?: return@mapNotNull null
                val entry = zip.getEntry(iconPath.path) ?: return@mapNotNull null
                if (entry.isDirectory || entry.size !in 1..MAX_RESOURCE_ENTRY_BYTES) return@mapNotNull null
                Triple(iconPath, entry, contentType)
            }
            .maxWithOrNull(compareBy<Triple<IconPath, java.util.zip.ZipEntry, String>> { it.first.density }
                .thenBy { it.second.size })
            ?: return null
        val bytes = zip.getInputStream(candidate.second).use {
            it.readBytesBounded(MAX_RESOURCE_ENTRY_BYTES.toInt())
        }
        ApkIcon(bytes, candidate.third)
    }
} catch (t: Throwable) {
    null
}

private fun InputStream.readBytesBounded(maxBytes: Int): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        require(output.size() + read <= maxBytes) { "APK icon expands beyond the safe limit" }
        output.write(buffer, 0, read)
    }
    return output.toByteArray()
}

/** Byte-array compatibility path used by existing focused parser tests. */
private fun extractInflatedIcon(apk: AbstractApkFile): ApkIcon? = try {
    apk.iconFiles.orEmpty().ifEmpty { apk.allIcons.orEmpty() }
        .mapNotNull { face ->
            val data = face.data
            val contentType = contentTypeForIcon(face.path)
            if (face.isFile && data != null && data.isNotEmpty() && contentType != null) {
                ApkIcon(data, contentType)
            } else {
                null
            }
        }
        .maxByOrNull { it.bytes.size }
} catch (t: Throwable) {
    null
}
