package com.commit451.drebin451.auth

import com.mmk.kmpauth.google.GoogleAuthCredentials
import com.mmk.kmpauth.google.GoogleAuthProvider
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.AuthCredential
import dev.gitlive.firebase.auth.EmailAuthProvider
import dev.gitlive.firebase.auth.FirebaseAuthException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidCredentialsException
import dev.gitlive.firebase.auth.FirebaseAuthInvalidUserException
import dev.gitlive.firebase.auth.FirebaseAuthUserCollisionException
import dev.gitlive.firebase.auth.FirebaseAuthWeakPasswordException
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.auth.GoogleAuthProvider as FirebaseGoogleAuthProvider

internal var isGoogleSignInAvailable = false
    private set

actual fun initializeAuth(webClientId: String) {
    isGoogleSignInAvailable = webClientId.isNotBlank()
    if (!isGoogleSignInAvailable) return

    GoogleAuthProvider.create(
        credentials = GoogleAuthCredentials(serverId = webClientId),
    )
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

internal suspend fun firebaseSignInOrLinkGoogle(
    idToken: String,
    accessToken: String?,
    existingEmail: String,
    existingPassword: String,
) {
    val googleCredential = FirebaseGoogleAuthProvider.credential(idToken, accessToken)
    try {
        Firebase.auth.signInWithCredential(googleCredential)
        linkEmailPasswordToCurrentGoogleAccountIfRequested(
            existingEmail = existingEmail,
            existingPassword = existingPassword,
        )
    } catch (e: FirebaseAuthUserCollisionException) {
        linkGoogleToExistingEmailAccount(
            googleCredential = googleCredential,
            collisionEmail = e.email ?: existingEmail,
            existingPassword = existingPassword,
            cause = e,
        )
    } catch (e: FirebaseAuthException) {
        throw Exception(friendlyAuthMessage(e), e)
    }
}

private suspend fun linkGoogleToExistingEmailAccount(
    googleCredential: AuthCredential,
    collisionEmail: String,
    existingPassword: String,
    cause: Throwable,
) {
    val email = collisionEmail.trim()
    if (email.isBlank() || existingPassword.isBlank()) {
        throw GoogleAccountLinkRequiredException(email)
    }
    try {
        Firebase.auth.signInWithEmailAndPassword(email, existingPassword)
        val user = Firebase.auth.currentUser
            ?: throw Exception("Sign in with email/password succeeded, but Firebase did not return a user.")
        user.linkWithCredential(googleCredential)
    } catch (e: FirebaseAuthInvalidCredentialsException) {
        throw Exception(
            "Incorrect password for $email. Enter that account's password, then continue with Google again to link Google sign-in.",
            e,
        )
    } catch (e: FirebaseAuthException) {
        throw Exception(friendlyAuthMessage(e), e)
    } catch (t: Throwable) {
        throw Exception(t.message ?: googleAccountLinkRequiredMessage(email), cause)
    }
}

private suspend fun linkEmailPasswordToCurrentGoogleAccountIfRequested(
    existingEmail: String,
    existingPassword: String,
) {
    val email = existingEmail.trim()
    if (email.isBlank() || existingPassword.isBlank()) return
    val user = Firebase.auth.currentUser ?: return
    if (!user.email.equals(email, ignoreCase = true)) return
    if (user.providerData.any { it.providerId == "password" }) return

    try {
        user.linkWithCredential(EmailAuthProvider.credential(email, existingPassword))
    } catch (e: FirebaseAuthWeakPasswordException) {
        throw Exception(friendlyAuthMessage(e), e)
    } catch (e: FirebaseAuthUserCollisionException) {
        throw Exception(
            "An email/password account already exists for $email. Sign in with email/password, then continue with Google to link Google sign-in.",
            e,
        )
    } catch (e: FirebaseAuthException) {
        throw Exception(friendlyAuthMessage(e), e)
    }
}

/**
 * Runs a Firebase auth call, translating its exceptions into short, user-facing messages.
 * Firebase's own messages are technical (and, with email-enumeration protection on, vague),
 * so we map the common cases and fall back to the raw message otherwise.
 */
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
    // WeakPassword is a subtype of InvalidCredentials, so it must be checked first.
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

actual suspend fun hasRestoredSignedInUser(): Boolean = isSignedIn()

actual suspend fun firebaseSignOut() {
    Firebase.auth.signOut()
}
