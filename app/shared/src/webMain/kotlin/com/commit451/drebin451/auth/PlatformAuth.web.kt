package com.commit451.drebin451.auth

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.auth.FirebaseAuthException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidCredentialsException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidUserException
import dev.gitlive.firebase.auth.FirebaseAuthUserCollisionException
import dev.gitlive.firebase.auth.FirebaseAuthWeakPasswordException
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.initialize
import kotlinx.coroutines.flow.first

/**
 * The OAuth 2.0 **web** client id (google-services client_type 3), captured at startup so the
 * Google button can hand it to Google Identity Services. Android reads the equivalent value from
 * `default_web_client_id`; web has no google-services plugin, so [initializeAuth] passes it in.
 */
internal var webGoogleClientId: String? = null
    private set

private var firebaseInitialized = false

/**
 * Unlike Android (auto-configured by the google-services plugin from `google-services.json`),
 * the Firebase JS SDK has to be initialized explicitly with the project's **web** app config.
 * Production values come from the generated [FirebaseWebConfig], sourced from an ignored local
 * properties file or the GitHub Actions secret. Also stashes [webClientId] for the Google sign-in
 * button. Safe to call more than once.
 */
actual fun initializeAuth(webClientId: String) {
    webGoogleClientId = webClientId
    if (firebaseInitialized) return
    Firebase.initialize(
        options = FirebaseOptions(
            applicationId = FirebaseWebConfig.APPLICATION_ID,
            apiKey = FirebaseWebConfig.API_KEY,
            projectId = FirebaseWebConfig.PROJECT_ID,
            storageBucket = FirebaseWebConfig.STORAGE_BUCKET,
            gcmSenderId = FirebaseWebConfig.GCM_SENDER_ID,
            authDomain = FirebaseWebConfig.AUTH_DOMAIN,
        ),
    )
    firebaseInitialized = true
}

actual suspend fun firebaseSignInWithEmail(email: String, password: String) {
    withFriendlyAuthError {
        Firebase.auth.signInWithEmailAndPassword(email, password)
    }
}

actual suspend fun firebaseCreateUserWithEmail(email: String, password: String) {
    withFriendlyAuthError {
        Firebase.auth.createUserWithEmailAndPassword(email, password)
    }
}

private suspend inline fun withFriendlyAuthError(block: () -> Unit) {
    try {
        block()
    } catch (e: FirebaseAuthException) {
        throw Exception(friendlyAuthMessage(e), e)
    }
}

private fun friendlyAuthMessage(e: FirebaseAuthException): String = when (e) {
    is FirebaseAuthUserCollisionException ->
        "An account already exists for that email. Keep the password entered and continue with Google to link sign-in methods, or sign in with the method you already use."

    is FirebaseAuthWeakPasswordException ->
        "Password is too weak — use at least 6 characters."

    is FirebaseAuthInvalidUserException ->
        "No account found for that email. Try signing up instead."

    is FirebaseAuthInvalidCredentialsException ->
        "Incorrect email or password."

    else -> e.message ?: "Authentication failed. Please try again."
}

actual suspend fun firebaseIdToken(forceRefresh: Boolean): String? =
    Firebase.auth.currentUser?.getIdToken(forceRefresh)

actual fun isSignedIn(): Boolean = Firebase.auth.currentUser != null

actual suspend fun hasRestoredSignedInUser(): Boolean {
    if (Firebase.auth.currentUser != null) return true
    return Firebase.auth.authStateChanged.first() != null
}

actual suspend fun firebaseSignOut() {
    Firebase.auth.signOut()
}
