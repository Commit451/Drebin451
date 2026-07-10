package com.commit451.drebin451.ui

/** Whether the route resolver should render branded splash/loading chrome while it works. */
internal expect fun shouldShowSplashScreenContent(): Boolean

/**
 * Whether startup should act as a fast, invisible auth router instead of blocking on network-backed
 * app config/user loading before it leaves the splash route.
 */
internal expect fun shouldUseFastAuthStartupRouting(): Boolean
