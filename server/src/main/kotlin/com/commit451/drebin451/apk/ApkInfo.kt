package com.commit451.drebin451.apk

import net.dongliu.apk.parser.ByteArrayApkFile

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
fun parseApk(bytes: ByteArray): ApkInfo {
    return try {
        ByteArrayApkFile(bytes).use { apk ->
            val meta = apk.apkMeta
            val applicationId = meta.packageName?.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("APK is missing an applicationId (package name)")
            ApkInfo(
                applicationId = applicationId,
                versionName = meta.versionName ?: "",
                versionCode = meta.versionCode ?: 0L,
                label = meta.label?.takeIf { it.isNotBlank() } ?: applicationId,
                icon = extractIcon(apk),
            )
        }
    } catch (e: IllegalArgumentException) {
        // Already a client-facing 400 (bad/empty package name) — keep its message.
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
 * Picks the best raster launcher icon from an APK, or null when it has none we can serve (e.g. a
 * purely adaptive/vector icon). Considers both the dedicated raster icon files and the general icon
 * list, choosing the largest image — i.e. the highest-density variant. Never throws: icon
 * extraction is best-effort and must not fail an otherwise-valid upload.
 */
private fun extractIcon(apk: ByteArrayApkFile): ApkIcon? = try {
    (apk.iconFiles.orEmpty() + apk.allIcons.orEmpty())
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
