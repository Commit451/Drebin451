package com.commit451.drebin451.navigation

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BrowserBackNavigationTest {

    @Test
    fun browserHistoryPayload_restoresSerializedForwardRoute() {
        val viewerRoute = LandingImageViewerRoute(LandingScreenshot.ReleaseDetails)
        val routeToken = encodeBrowserHistoryRoute(viewerRoute)
        val payload = """
            {
              "fromIndex": 0,
              "toIndex": 1,
              "forwardRoutes": [
                {
                  "index": 1,
                  "routeToken": ${Json.encodeToString(routeToken)}
                }
              ]
            }
        """.trimIndent()

        val change = assertNotNull(decodeBrowserHistoryChange(payload))

        assertEquals(0, change.fromIndex)
        assertEquals(1, change.toIndex)
        assertEquals(
            listOf(BrowserHistoryRoute(index = 1, route = viewerRoute)),
            change.forwardRoutes,
        )
    }
}
