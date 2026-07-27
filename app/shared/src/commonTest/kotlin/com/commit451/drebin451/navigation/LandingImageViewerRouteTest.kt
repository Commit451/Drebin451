package com.commit451.drebin451.navigation

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class LandingImageViewerRouteTest {

    @Test
    fun viewerRoute_keepsTheSelectedScreenshot() {
        val route = LandingImageViewerRoute(LandingScreenshot.AppsList)

        assertEquals(LandingScreenshot.AppsList, route.screenshot)
        assertNotEquals(
            route,
            LandingImageViewerRoute(LandingScreenshot.ReleaseDetails),
        )
    }

    @Test
    fun viewerRoute_instancesAreUniqueForTheSameScreenshot() {
        assertNotEquals(
            LandingImageViewerRoute(LandingScreenshot.AppsList),
            LandingImageViewerRoute(LandingScreenshot.AppsList),
        )
    }

    @Test
    fun viewerRoute_roundTripsThroughAppRouteSerialization() {
        val route: AppRoute = LandingImageViewerRoute(LandingScreenshot.AppReleases)

        val encoded = Json.encodeToString(AppRoute.serializer(), route)
        val decoded = Json.decodeFromString(AppRoute.serializer(), encoded)

        assertEquals(route, decoded)
    }
}
