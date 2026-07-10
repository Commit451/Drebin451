package com.commit451.drebin451.firebase

import com.google.api.core.ApiFuture
import com.google.api.core.ApiFutureCallback
import com.google.api.core.ApiFutures
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Bridges Google's callback-based [ApiFuture] (used throughout the Firebase Admin
 * SDK) into a Kotlin coroutine `suspend` call. Mirrors askarasu's `firebase/ext.kt`.
 */
suspend fun <T> ApiFuture<T>.await(): T = suspendCancellableCoroutine { cont ->
    ApiFutures.addCallback(
        this,
        object : ApiFutureCallback<T> {
            override fun onSuccess(result: T) {
                cont.resume(result)
            }

            override fun onFailure(t: Throwable) {
                cont.resumeWithException(t)
            }
        },
        MoreExecutors.directExecutor(),
    )
}
