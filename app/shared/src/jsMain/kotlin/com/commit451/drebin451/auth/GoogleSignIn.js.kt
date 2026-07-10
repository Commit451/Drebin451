package com.commit451.drebin451.auth

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.auth.externals.AuthProvider
import dev.gitlive.firebase.auth.externals.AuthResult
import dev.gitlive.firebase.auth.externals.EmailAuthProvider
import dev.gitlive.firebase.auth.externals.User
import dev.gitlive.firebase.auth.externals.linkWithCredential
import dev.gitlive.firebase.auth.externals.signInWithEmailAndPassword
import dev.gitlive.firebase.auth.externals.signInWithPopup
import dev.gitlive.firebase.auth.js
import kotlinx.coroutines.await
import kotlin.js.Promise
import dev.gitlive.firebase.auth.externals.GoogleAuthProvider as JsGoogleAuthProvider

@JsModule("firebase/auth")
@JsNonModule
private external fun linkWithPopup(user: User, provider: AuthProvider): Promise<AuthResult>

/**
 * Uses Firebase Auth's hosted OAuth popup flow instead of Google Identity Services' browser-origin
 * ID-token prompt. If Google collides with an existing email/password account, the password field
 * from the auth form is used to sign in that account and link the Google credential to the same
 * Firebase user.
 */
internal actual suspend fun firebaseSignInWithGooglePopup(existingEmail: String, existingPassword: String) {
    val provider = googleProvider()
    try {
        val result = signInWithPopup(Firebase.auth.js, provider).await()
        linkEmailPasswordToGoogleAccountIfRequested(
            user = result.user,
            existingEmail = existingEmail,
            existingPassword = existingPassword,
        )
    } catch (t: Throwable) {
        if (!t.isAccountExistsWithDifferentCredential()) throw t
        linkGooglePopupToExistingEmailAccount(
            provider = provider,
            email = t.authEmail().ifBlank { existingEmail },
            password = existingPassword,
        )
    }
}

private suspend fun linkGooglePopupToExistingEmailAccount(
    provider: JsGoogleAuthProvider,
    email: String,
    password: String,
) {
    val trimmedEmail = email.trim()
    if (trimmedEmail.isBlank() || password.isBlank()) {
        throw GoogleAccountLinkRequiredException(trimmedEmail)
    }
    try {
        signInWithEmailAndPassword(Firebase.auth.js, trimmedEmail, password).await()
        val user = Firebase.auth.js.currentUser
            ?: throw Exception("Sign in with email/password succeeded, but Firebase did not return a user.")
        linkWithPopup(user, provider).await()
    } catch (t: Throwable) {
        throw Exception(
            t.message ?: "Could not link Google sign-in to $trimmedEmail.",
            t,
        )
    }
}

private suspend fun linkEmailPasswordToGoogleAccountIfRequested(
    user: User?,
    existingEmail: String,
    existingPassword: String,
) {
    val email = existingEmail.trim()
    if (user == null || email.isBlank() || existingPassword.isBlank()) return
    if (!user.email.equals(email, ignoreCase = true)) return
    if (user.providerData.asList().any { it.providerId == "password" }) return

    try {
        linkWithCredential(user, EmailAuthProvider.credential(email, existingPassword)).await()
    } catch (t: Throwable) {
        if (t.isProviderAlreadyLinked()) return
        throw Exception(t.message ?: "Could not link email/password sign-in to $email.", t)
    }
}

private fun googleProvider(): JsGoogleAuthProvider = JsGoogleAuthProvider().apply {
    addScope("email")
    addScope("profile")
}

private fun Throwable.isAccountExistsWithDifferentCredential(): Boolean {
    val code = authCode()
    return code == "auth/account-exists-with-different-credential" ||
            code == "auth/credential-already-in-use" ||
            message?.contains("account-exists-with-different-credential") == true ||
            message?.contains("credential-already-in-use") == true
}

private fun Throwable.isProviderAlreadyLinked(): Boolean {
    val code = authCode()
    return code == "auth/provider-already-linked" ||
            code == "auth/credential-already-in-use" ||
            message?.contains("provider-already-linked") == true ||
            message?.contains("credential-already-in-use") == true
}

private fun Throwable.authCode(): String =
    asDynamic().code?.toString()
        ?: asDynamic().cause?.code?.toString()
        ?: ""

private fun Throwable.authEmail(): String =
    asDynamic().customData?.email?.toString()
        ?: asDynamic().email?.toString()
        ?: asDynamic().cause?.customData?.email?.toString()
        ?: ""
