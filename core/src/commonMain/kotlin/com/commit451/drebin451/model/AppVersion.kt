package com.commit451.drebin451.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * One uploaded APK — a single version of an [App]. Multiple uploads of the *same*
 * [versionName]/[versionCode] are allowed and kept as distinct rows, told apart by
 * [createdAt].
 *
 * Persisted in Firestore under `apps/{appId}/versions/{id}`; the APK bytes live in
 * Backblaze B2 at [storagePath]. Returned to clients from the `/v1/apps/{appId}/versions`
 * endpoint. [storagePath] is [Transient] so it is stored in Firestore (which uses its own
 * mapping, not kotlinx.serialization) but never leaked over the JSON API. Clients download APK
 * bytes through the authenticated `/v1/apps/{appId}/versions/{versionId}/download` endpoint instead
 * of receiving a shareable per-version URL. All fields default for Firestore's `toObject`.
 */
@Serializable
data class AppVersion(
    val id: String = "",
    val appId: String = "",
    val applicationId: String = "",
    val ownerUserId: String = "",
    val versionName: String = "",
    val versionCode: Long = 0,
    /** Optional free-text note for this upload (e.g. a CI commit message); blank if none. */
    val note: String = "",
    val fileName: String = "",
    val fileSizeBytes: Long = 0,
    val contentType: String = CONTENT_TYPE_APK,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    @Transient val storagePath: String = "",
) {
    companion object {
        const val CONTENT_TYPE_APK = "application/vnd.android.package-archive"
    }
}
