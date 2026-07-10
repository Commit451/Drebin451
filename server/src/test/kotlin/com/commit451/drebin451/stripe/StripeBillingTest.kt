package com.commit451.drebin451.stripe

import com.commit451.drebin451.model.PlanIds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StripeBillingTest {

    @Test
    fun `active configured subscription maps to Pro`() {
        assertEquals(
            PlanIds.PRO,
            StripeBilling.planForSubscription(
                status = "active",
                priceId = "price_pro",
                configuredProPriceId = "price_pro",
            ),
        )
        assertEquals(
            PlanIds.PRO,
            StripeBilling.planForSubscription(
                status = "trialing",
                priceId = "price_pro",
                configuredProPriceId = "price_pro",
            ),
        )
    }

    @Test
    fun `inactive or different-price subscription maps to Free`() {
        assertEquals(
            PlanIds.FREE,
            StripeBilling.planForSubscription(
                status = "canceled",
                priceId = "price_pro",
                configuredProPriceId = "price_pro",
            ),
        )
        assertEquals(
            PlanIds.FREE,
            StripeBilling.planForSubscription(
                status = "active",
                priceId = "price_other",
                configuredProPriceId = "price_pro",
            ),
        )
    }

    @Test
    fun `account deletion cancels only still-billable subscription statuses`() {
        assertTrue(StripeBilling.cancelableSubscriptionStatus("active"))
        assertTrue(StripeBilling.cancelableSubscriptionStatus("trialing"))
        assertTrue(StripeBilling.cancelableSubscriptionStatus("past_due"))
        assertTrue(StripeBilling.cancelableSubscriptionStatus("unpaid"))
        assertTrue(StripeBilling.cancelableSubscriptionStatus("incomplete"))
        assertFalse(StripeBilling.cancelableSubscriptionStatus("canceled"))
        assertFalse(StripeBilling.cancelableSubscriptionStatus("incomplete_expired"))
        assertFalse(StripeBilling.cancelableSubscriptionStatus(""))
    }

    @Test
    fun `valid webhook signature verifies`() {
        val payload = "{\"id\":\"evt_123\"}"
        val secret = "whsec_test"
        val timestamp = 1_700_000_000L
        val header = StripeBilling.testSignatureHeader(payload, secret, timestamp)

        assertTrue(
            StripeBilling.isValidWebhookSignature(
                payload = payload,
                signatureHeader = header,
                secret = secret,
                nowEpochSeconds = timestamp,
            ),
        )
    }

    @Test
    fun `invalid webhook signature is rejected`() {
        val payload = "{\"id\":\"evt_123\"}"
        val timestamp = 1_700_000_000L

        assertFalse(
            StripeBilling.isValidWebhookSignature(
                payload = payload,
                signatureHeader = "t=$timestamp,v1=bad",
                secret = "whsec_test",
                nowEpochSeconds = timestamp,
            ),
        )
    }

    @Test
    fun `subscription updated webhook produces entitlement update`() {
        val payload = """
            {
              "type": "customer.subscription.updated",
              "data": {
                "object": {
                  "id": "sub_123",
                  "customer": "cus_123",
                  "status": "active",
                  "current_period_end": 1700000600,
                  "metadata": { "uid": "user_123" },
                  "items": {
                    "data": [
                      { "price": { "id": "price_pro" } }
                    ]
                  }
                }
              }
            }
        """.trimIndent()

        val update = assertNotNull(
            StripeBilling.webhookUpdateFromPayload(
                payload,
                configuredProPriceId = "price_pro"
            )
        )
        assertEquals("user_123", update.uid)
        assertEquals("cus_123", update.customerId)
        assertEquals("sub_123", update.subscriptionId)
        assertEquals("active", update.status)
        assertEquals("price_pro", update.priceId)
        assertEquals(1_700_000_600_000L, update.currentPeriodEnd)
        assertEquals(PlanIds.PRO, update.plan)
    }

    @Test
    fun `checkout completed webhook activates Pro for our checkout session`() {
        val payload = """
            {
              "type": "checkout.session.completed",
              "data": {
                "object": {
                  "mode": "subscription",
                  "client_reference_id": "user_123",
                  "customer": "cus_123",
                  "subscription": "sub_123"
                }
              }
            }
        """.trimIndent()

        val update = assertNotNull(
            StripeBilling.webhookUpdateFromPayload(
                payload,
                configuredProPriceId = "price_pro"
            )
        )
        assertEquals("user_123", update.uid)
        assertEquals("cus_123", update.customerId)
        assertEquals("sub_123", update.subscriptionId)
        assertEquals("active", update.status)
        assertEquals("price_pro", update.priceId)
        assertEquals(PlanIds.PRO, update.plan)
    }
}
