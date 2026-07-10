package com.commit451.drebin451.follow

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.commit451.drebin451.push.PushTopics
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// The followed-app set lives in KMP Preferences DataStore. FCM exposes no way to query a device's
// topic subscriptions, so we mirror the set locally to drive the follow toggle's state.
private var followDataStore: DataStore<Preferences>? = null

/** Wires the follow store to an application [context]. Call once from Application.onCreate. */
fun initFollows(context: Context) {
    val appContext = context.applicationContext
    val dataStore = followDataStore ?: createAndroidFollowDataStore(appContext).also {
        followDataStore = it
    }
    // One-time blocking read to seed the cache. The file is tiny, and this keeps isFollowing()
    // synchronous so the icon doesn't flash the wrong way on the first frame.
    runBlocking { FollowPreferencesStore.init(dataStore) }
}

private fun createAndroidFollowDataStore(context: Context): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
        // Store local FCM topic state in noBackupFilesDir. Auto Backup always excludes this
        // directory, so a fresh reinstall cannot restore old notification toggles.
        produceFile = { context.noBackupFilesDir.resolve(FOLLOW_DATA_STORE_FILE_NAME) },
    )

actual fun isPushSupported(): Boolean = true

actual fun isFollowing(appId: String): Boolean = FollowPreferencesStore.isFollowing(appId)

actual suspend fun followApp(appId: String) {
    FirebaseMessaging.getInstance().subscribeToTopic(PushTopics.appUpdates(appId)).awaitUnit()
    FollowPreferencesStore.add(appId)
}

actual suspend fun unfollowApp(appId: String) {
    FirebaseMessaging.getInstance().unsubscribeFromTopic(PushTopics.appUpdates(appId)).awaitUnit()
    FollowPreferencesStore.remove(appId)
}

actual suspend fun unfollowAll() {
    FollowPreferencesStore.followedAppIds().forEach { appId ->
        runCatching {
            FirebaseMessaging.getInstance()
                .unsubscribeFromTopic(PushTopics.appUpdates(appId))
                .awaitUnit()
        }
    }
    FollowPreferencesStore.clear()
}

// FCM's subscribe/unsubscribe Task only completes once the background topic sync reaches Google's
// servers, and a *non-retryable* failure (e.g. the server rejecting an invalid topic name with a
// 400) leaves the Task uncompleted forever instead of failing it. Bounding the await surfaces that
// as a timeout the caller can show, rather than a follow toggle that spins indefinitely.
private const val TOPIC_OP_TIMEOUT_MS = 15_000L

/** Awaits a Play-services [Task] without pulling in kotlinx-coroutines-play-services. */
private suspend fun Task<Void>.awaitUnit(): Unit = withTimeout(TOPIC_OP_TIMEOUT_MS) {
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(Unit) }
        addOnFailureListener { e -> cont.resumeWithException(e) }
    }
}
