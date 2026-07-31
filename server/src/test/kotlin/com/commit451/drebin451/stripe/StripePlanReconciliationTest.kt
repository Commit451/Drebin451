package com.commit451.drebin451.stripe

import com.commit451.drebin451.model.PlanIds
import com.commit451.drebin451.model.User
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class StripePlanReconciliationTest {

    @Test
    fun `reconciliation corrects stale plans and skips users without billing history`() = runBlocking {
        val users = listOf(
            User(uid = "downgrade", plan = PlanIds.PRO, stripeCustomerId = "cus_downgrade"),
            User(uid = "upgrade", plan = PlanIds.FREE, stripeCustomerId = "cus_upgrade"),
            User(uid = "unchanged", plan = PlanIds.PRO, stripeCustomerId = "cus_unchanged"),
            User(uid = "orphaned-pro", plan = PlanIds.PRO),
            User(uid = "free", plan = PlanIds.FREE),
        )
        val refreshedPlans = mapOf(
            "downgrade" to PlanIds.FREE,
            "upgrade" to PlanIds.PRO,
            "unchanged" to PlanIds.PRO,
            "orphaned-pro" to PlanIds.FREE,
        )

        val report = reconcileStripePlanUsers(users) { user ->
            user.copy(plan = refreshedPlans.getValue(user.uid))
        }

        assertEquals(5, report.scannedUserCount)
        assertEquals(4, report.reconciledUserCount)
        assertEquals(1, report.skippedUserCount)
        assertEquals(1, report.upgradedToProCount)
        assertEquals(2, report.downgradedToFreeCount)
        assertEquals(1, report.unchangedCount)
        assertEquals(0, report.failedCount)
    }

    @Test
    fun `reconciliation continues after one Stripe refresh fails`() = runBlocking {
        val users = listOf(
            User(uid = "failed", plan = PlanIds.PRO, stripeCustomerId = "cus_failed"),
            User(uid = "healthy", plan = PlanIds.PRO, stripeCustomerId = "cus_healthy"),
        )
        val failures = mutableListOf<String>()

        val report = reconcileStripePlanUsers(
            users = users,
            refresh = { user ->
                if (user.uid == "failed") error("Stripe unavailable")
                user.copy(plan = PlanIds.FREE)
            },
            onFailure = { user, _ -> failures += user.uid },
        )

        assertEquals(listOf("failed"), failures)
        assertEquals(2, report.scannedUserCount)
        assertEquals(2, report.reconciledUserCount)
        assertEquals(1, report.downgradedToFreeCount)
        assertEquals(1, report.failedCount)
    }

    @Test
    fun `reconciliation does not swallow coroutine cancellation`() {
        val users = listOf(
            User(uid = "cancelled", plan = PlanIds.PRO, stripeCustomerId = "cus_cancelled"),
        )

        assertFailsWith<CancellationException> {
            runBlocking {
                reconcileStripePlanUsers(users) {
                    throw CancellationException("job stopped")
                }
            }
        }
    }

    @Test
    fun `orphan reconciliation refreshes canonical state when a customer appears after the scan`() = runBlocking {
        val scanned = User(uid = "stale", plan = PlanIds.PRO)
        val current = scanned.copy(
            plan = PlanIds.FREE,
            stripeCustomerId = "cus_created_during_scan",
        )
        val refreshedUsers = mutableListOf<User>()

        val reconciled = reconcileScannedStripePlanUser(
            scannedUser = scanned,
            verifyAndDowngradeOrphan = { current },
            refreshCanonicalState = { user ->
                refreshedUsers += user
                user.copy(plan = PlanIds.PRO)
            },
        )

        assertEquals(listOf(current), refreshedUsers)
        assertEquals("cus_created_during_scan", reconciled.stripeCustomerId)
        assertEquals(PlanIds.PRO, reconciled.plan)
    }

    @Test
    fun `fatal JVM errors propagate out of per-user reconciliation`() {
        val fatal = LinkageError("fatal JVM failure")

        val thrown = assertFailsWith<LinkageError> {
            runBlocking {
                reconcileStripePlanUsers(
                    users = listOf(
                        User(uid = "fatal", plan = PlanIds.PRO, stripeCustomerId = "cus_fatal"),
                    ),
                    onFailure = { _, _ -> error("fatal errors must not reach the failure callback") },
                    refresh = { throw fatal },
                )
            }
        }

        assertTrue(thrown === fatal)
    }

    @Test
    fun `malformed documents are isolated logged and included in reconciliation counts`() = runBlocking {
        val decodeFailures = mutableListOf<Pair<String, String?>>()
        val decoded = decodeReconciliationDocuments(
            documents = listOf("healthy-before", "malformed", "healthy-after"),
            decode = { document ->
                if (document == "malformed") throw IllegalArgumentException("invalid user fields")
                User(uid = document, plan = PlanIds.PRO, stripeCustomerId = "cus_$document")
            },
            onFailure = { document, failure ->
                decodeFailures += document to failure.message
            },
        )
        val refreshedUsers = mutableListOf<String>()

        val report = reconcileStripePlanUsers(
            users = decoded.values,
            decodeFailureCount = decoded.failedCount,
            refresh = { user ->
                refreshedUsers += user.uid
                user
            },
        )

        assertEquals(
            listOf<Pair<String, String?>>("malformed" to "invalid user fields"),
            decodeFailures,
        )
        assertEquals(listOf("healthy-before", "healthy-after"), refreshedUsers)
        assertEquals(3, report.scannedUserCount)
        assertEquals(3, report.reconciledUserCount)
        assertEquals(0, report.skippedUserCount)
        assertEquals(2, report.unchangedCount)
        assertEquals(1, report.failedCount)
    }
}