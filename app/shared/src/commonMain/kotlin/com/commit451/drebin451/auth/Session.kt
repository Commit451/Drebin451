package com.commit451.drebin451.auth

import com.commit451.drebin451.follow.unfollowAll

/** Signs out of Firebase and clears the in-memory user. Safe to call when already signed out. */
suspend fun signOut() {
    // Unsubscribe from all followed apps and clear local follow state, so on a shared device the
    // next account doesn't inherit this one's follows. No-op on web.
    runCatching { unfollowAll() }
    runCatching { firebaseSignOut() }
    UserManager.set(null)
}
