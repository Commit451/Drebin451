package com.commit451.drebin451.model

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

/**
 * A distinct app in a user's personal "Play Store", identified by its Android
 * [applicationId] (package name). One [App] groups many [AppVersion]s — every uploaded
 * APK sharing that [applicationId] adds another version.
 *
 * Persisted in Firestore (collection `apps`, keyed by [id] = `"$ownerUserId:$applicationId"`)
 * and returned to clients from the `/v1/apps` endpoints. [label] and the `latest*` fields are
 * extracted from the APK; [versionCount] and [updatedAt] are maintained on each upload. All
 * fields default so Firestore's `toObject(App::class.java)` can instantiate it.
 */
@Serializable
data class App(
    val id: String = "",
    val applicationId: String = "",
    val ownerUserId: String = "",
    val ownerName: String = "",
    val label: String = "",
    val latestVersionName: String = "",
    val latestVersionCode: Long = 0,
    val versionCount: Int = 0,
    /**
     * Public URL of the app's launcher icon, served by `GET /v1/apps/{id}/icon`. Extracted from
     * the APK and stored once (on the first upload that yields a raster icon); blank when none was
     * found. Clients load it with Coil.
     */
    val imageUrl: String = "",
    /**
     * Random, URL-safe token used in the app's public share URL (`/app/{shareId}`). This is generated
     * once by the server and intentionally does not expose the stable Firestore app id. It is required
     * in API JSON; the default exists only for Firestore's no-arg mapper.
     */
    @Required
    val shareId: String = "",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)
