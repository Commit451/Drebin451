package com.commit451.drebin451

import com.commit451.drebin451.stripe.StripePlanReconciliationReport
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StripePlanReconciliationRouteTest {

    @Test
    fun `nightly reconciliation endpoint requires the cron secret`() = testApplication {
        var reconciled = false
        application {
            install(ContentNegotiation) { json() }
            routing {
                stripePlanReconciliationRoute(
                    configuredSecret = { "expected" },
                    stripeConfigured = { true },
                    reconcile = {
                        reconciled = true
                        emptyReport()
                    },
                )
            }
        }

        assertEquals(
            HttpStatusCode.Unauthorized,
            client.post("/v1/cron/stripe/reconcile").status,
        )
        assertFalse(reconciled)
    }

    @Test
    fun `nightly reconciliation endpoint returns the reconciliation report`() = testApplication {
        val expected = StripePlanReconciliationReport(
            scannedUserCount = 3,
            reconciledUserCount = 2,
            skippedUserCount = 1,
            upgradedToProCount = 0,
            downgradedToFreeCount = 1,
            unchangedCount = 1,
            failedCount = 0,
        )
        application {
            install(ContentNegotiation) { json() }
            routing {
                stripePlanReconciliationRoute(
                    configuredSecret = { "expected" },
                    stripeConfigured = { true },
                    reconcile = { expected },
                )
            }
        }

        val response = client.post("/v1/cron/stripe/reconcile") {
            header(CronSecretHeader, "expected")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(expected, Json.decodeFromString(response.bodyAsText()))
    }

    @Test
    fun `nightly reconciliation endpoint reports partial failures for scheduler retry`() = testApplication {
        val report = StripePlanReconciliationReport(
            scannedUserCount = 3,
            reconciledUserCount = 2,
            skippedUserCount = 1,
            upgradedToProCount = 0,
            downgradedToFreeCount = 1,
            unchangedCount = 0,
            failedCount = 1,
        )
        application {
            install(ContentNegotiation) { json() }
            routing {
                stripePlanReconciliationRoute(
                    configuredSecret = { "expected" },
                    stripeConfigured = { true },
                    reconcile = { report },
                )
            }
        }

        val response = client.post("/v1/cron/stripe/reconcile") {
            header(CronSecretHeader, "expected")
        }

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertEquals(
            report,
            Json.decodeFromString<StripePlanReconciliationReport>(response.bodyAsText()),
        )
    }

    @Test
    fun `nightly reconciliation endpoint fails closed when Stripe is not configured`() = testApplication {
        var reconciled = false
        application {
            install(ContentNegotiation) { json() }
            routing {
                stripePlanReconciliationRoute(
                    configuredSecret = { "expected" },
                    stripeConfigured = { false },
                    reconcile = {
                        reconciled = true
                        emptyReport()
                    },
                )
            }
        }

        val response = client.post("/v1/cron/stripe/reconcile") {
            header(CronSecretHeader, "expected")
        }

        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        assertTrue(response.bodyAsText().contains("Stripe billing is not configured"))
        assertFalse(reconciled)
    }

    private fun emptyReport() = StripePlanReconciliationReport(
        scannedUserCount = 0,
        reconciledUserCount = 0,
        skippedUserCount = 0,
        upgradedToProCount = 0,
        downgradedToFreeCount = 0,
        unchangedCount = 0,
        failedCount = 0,
    )
}