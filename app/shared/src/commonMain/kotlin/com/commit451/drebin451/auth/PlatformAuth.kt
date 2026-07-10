package com.commit451.drebin451.auth

/**
 * Platform auth surface, backed by the gitlive Firebase Auth wrapper on every target
 * (Android also pulls in kmpauth for the Google sign-in button). Kept as `expect` so
 * each target can wire in its own engine — the Android Firebase SDK, the Firebase JS
 * SDK on web — without commonMain depending on either.
 */

/** Initializes the Google auth provider with the project's web client id. Call once at startup. */
expect fun initializeAuth(webClientId: String)

/**
 * Signs in an existing user with [email] + [password] via Firebase. Suspends until the
 * Firebase session is established; throws with a user-facing message on failure.
 */
expect suspend fun firebaseSignInWithEmail(email: String, password: String)

/**
 * Registers a new user with [email] + [password] via Firebase and signs them in.
 * Suspends until the session is established; throws with a user-facing message on failure.
 */
expect suspend fun firebaseCreateUserWithEmail(email: String, password: String)

/** The current Firebase ID token, or null if signed out. [forceRefresh] forces a token refresh. */
expect suspend fun firebaseIdToken(forceRefresh: Boolean = false): String?

/** Local (no-network) check for whether a user is currently signed in. */
expect fun isSignedIn(): Boolean

/**
 * Waits until the platform auth SDK has finished restoring any persisted local session, then
 * returns whether a Firebase user is available.
 *
 * On web, Firebase restores IndexedDB/localStorage state asynchronously after startup, so a
 * synchronous [isSignedIn] check can briefly report signed out even for a returning user.
 */
expect suspend fun hasRestoredSignedInUser(): Boolean

/** Signs the current user out of Firebase. */
expect suspend fun firebaseSignOut()

internal fun googleAccountLinkRequiredMessage(email: String): String {
    val target = email.trim().ifBlank { "that email" }
    return "An email/password account already exists for $target. Enter that account's password, then continue with Google again to link Google sign-in."
}

class GoogleAccountLinkRequiredException(
    val email: String,
) : Exception(googleAccountLinkRequiredMessage(email))
