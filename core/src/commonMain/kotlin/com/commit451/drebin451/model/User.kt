package com.commit451.drebin451.model

import kotlinx.serialization.Serializable

/**
 * An authenticated user. Persisted in Firestore (collection `users`, keyed by [uid])
 * and returned to the client from `GET /v1/user`.
 *
 * All fields default so Firestore's `toObject(User::class.java)` can instantiate it.
 */
@Serializable
data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val createdAt: Long = 0,
    val plan: String = PlanIds.FREE,
    val storageUsedBytes: Long = 0,
    val admin: Boolean = false,
    val planUpdatedAt: Long = 0,
    val planSyncedAt: Long = 0,
    val stripeCustomerId: String = "",
    val stripeSubscriptionId: String = "",
    val stripeSubscriptionStatus: String = "",
    val stripePriceId: String = "",
    val stripeCurrentPeriodEnd: Long = 0,
)
