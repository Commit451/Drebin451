package com.commit451.drebin451.navigation

import com.commit451.drebin451.share.currentBrowserPath

internal actual fun requestedPublicLandingRoute(): AppRoute? =
    when (currentBrowserPath().substringBefore('?').substringBefore('#').trim('/')) {
        "pricing" -> PricingRoute
        else -> null
    }
