package com.commit451.drebin451.model

import kotlinx.serialization.Serializable

/**
 * App-wide client configuration, fetched on the splash screen. Lets the backend flip a global
 * kill-switch ([disabled]) and surface a maintenance [message] without shipping a new client.
 * All fields default so Firestore's `toObject(Config::class.java)` and lenient JSON decoding can
 * instantiate it even when the `config/app` doc is absent or only partially filled in.
 *
 * Property names must match the `config/app` Firestore document's field names exactly — Firestore's
 * bean mapper binds by name (no kotlinx aliases), so a mismatch silently leaves the field at its
 * default (which is how an earlier `disableApp`-vs-`disabled` mismatch disabled the kill-switch).
 */
@Serializable
data class Config(
    val disabled: Boolean = false,
    val message: String = "",
)
