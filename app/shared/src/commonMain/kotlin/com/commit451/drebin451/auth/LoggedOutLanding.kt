package com.commit451.drebin451.auth

/**
 * Whether signed-out users should see the product landing/about page before auth.
 *
 * Web needs a public marketing/education surface before asking for credentials. Android keeps the
 * direct splash → auth flow so app launches stay concise.
 */
internal expect fun shouldShowLoggedOutAboutPage(): Boolean
