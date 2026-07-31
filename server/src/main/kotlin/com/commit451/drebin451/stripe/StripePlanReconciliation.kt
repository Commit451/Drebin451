package com.commit451.drebin451.stripe

import com.commit451.drebin451.model.PlanIds
import com.commit451.drebin451.model.PlanLimits
import com.commit451.drebin451.model.User
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable

@Serializable
data class StripePlanReconciliationReport(
    val scannedUserCount: Int,
    val reconciledUserCount: Int,
    val skippedUserCount: Int,
    val upgradedToProCount: Int,
    val downgradedToFreeCount: Int,
    val unchangedCount: Int,
    val failedCount: Int,
)

internal data class DecodedReconciliationDocuments<T>(
    val values: List<T>,
    val failedCount: Int,
)

internal fun <Document, Value : Any> decodeReconciliationDocuments(
    documents: Iterable<Document>,
    onFailure: (Document, Exception) -> Unit = { _, _ -> },
    decode: (Document) -> Value?,
): DecodedReconciliationDocuments<Value> {
    val values = mutableListOf<Value>()
    var failedCount = 0
    for (document in documents) {
        try {
            values += requireNotNull(decode(document)) { "Document could not be decoded" }
        } catch (failure: CancellationException) {
            throw failure
        } catch (failure: Exception) {
            failedCount++
            onFailure(document, failure)
        }
    }
    return DecodedReconciliationDocuments(values = values, failedCount = failedCount)
}

internal suspend fun reconcileScannedStripePlanUser(
    scannedUser: User,
    verifyAndDowngradeOrphan: suspend (User) -> User,
    refreshCanonicalState: suspend (User) -> User,
): User {
    if (scannedUser.stripeCustomerId.isNotBlank()) return refreshCanonicalState(scannedUser)
    val currentUser = verifyAndDowngradeOrphan(scannedUser)
    return if (currentUser.stripeCustomerId.isNotBlank()) {
        refreshCanonicalState(currentUser)
    } else {
        currentUser
    }
}

internal suspend fun reconcileStripePlanUsers(
    users: List<User>,
    decodeFailureCount: Int = 0,
    onFailure: (User, Exception) -> Unit = { _, _ -> },
    refresh: suspend (User) -> User,
): StripePlanReconciliationReport {
    require(decodeFailureCount >= 0) { "decodeFailureCount must not be negative" }
    val candidates = users.filter { user ->
        user.stripeCustomerId.isNotBlank() || PlanLimits.normalized(user.plan) == PlanIds.PRO
    }
    var upgradedToProCount = 0
    var downgradedToFreeCount = 0
    var unchangedCount = 0
    var failedCount = decodeFailureCount

    for (user in candidates) {
        try {
            val planBefore = PlanLimits.normalized(user.plan)
            val planAfter = PlanLimits.normalized(refresh(user).plan)
            when {
                planBefore != PlanIds.PRO && planAfter == PlanIds.PRO -> upgradedToProCount++
                planBefore == PlanIds.PRO && planAfter != PlanIds.PRO -> downgradedToFreeCount++
                else -> unchangedCount++
            }
        } catch (failure: CancellationException) {
            throw failure
        } catch (failure: Exception) {
            failedCount++
            onFailure(user, failure)
        }
    }

    return StripePlanReconciliationReport(
        scannedUserCount = users.size + decodeFailureCount,
        reconciledUserCount = candidates.size + decodeFailureCount,
        skippedUserCount = users.size - candidates.size,
        upgradedToProCount = upgradedToProCount,
        downgradedToFreeCount = downgradedToFreeCount,
        unchangedCount = unchangedCount,
        failedCount = failedCount,
    )
}
