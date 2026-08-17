package com.commit451.drebin451

import com.commit451.drebin451.model.BillingPrice
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class BillingPriceRouteTest {

    @Test
    fun `public billing endpoint returns the configured Pro price`() = testApplication {
        val expected = BillingPrice(
            unitAmount = 500,
            currency = "usd",
            interval = "month",
        )
        application {
            install(ContentNegotiation) { json() }
            routing {
                billingPriceRoute(proPrice = { expected })
            }
        }

        val response = client.get("/v1/billing/price")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("public, max-age=300", response.headers[HttpHeaders.CacheControl])
        assertEquals(expected, Json.decodeFromString<BillingPrice>(response.bodyAsText()))
    }
}
