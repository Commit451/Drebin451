package com.commit451.drebin451.ui

import com.commit451.drebin451.model.BillingPrice
import kotlin.test.Test
import kotlin.test.assertEquals

class PricingTest {

    @Test
    fun `formats the monthly US dollar Pro price`() {
        assertEquals(
            "\$5 / month",
            formatBillingPrice(
                BillingPrice(
                    unitAmount = 500,
                    currency = "usd",
                    interval = "month",
                ),
            ),
        )
    }

    @Test
    fun `preserves fractional amounts and multi-interval cadence`() {
        assertEquals(
            "€5.50 / 3 months",
            formatBillingPrice(
                BillingPrice(
                    unitAmount = 550,
                    currency = "eur",
                    interval = "month",
                    intervalCount = 3,
                ),
            ),
        )
    }
}
