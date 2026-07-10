package com.commit451.drebin451.navigation

/**
 * Optional public web route requested by the browser path before auth resolves.
 * Android returns null so native keeps the normal splash-driven flow.
 */
internal expect fun requestedPublicLandingRoute(): AppRoute?
