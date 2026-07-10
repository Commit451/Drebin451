package com.commit451.drebin451.share

/**
 * Captures a share link opened in the browser. Firebase Hosting rewrites every path to index.html,
 * so an opened `…/app/{shareId}` link arrives as the page path: read it, and if it's a Drebin app
 * link, stash it in [PendingDeepLink] for Home to add to Shared and route once auth resolves — the
 * same holder the Android App Links flow uses. A no-op for any other path.
 *
 * Path-based (not host-checked) because the web app is served from the hosting domain, which may
 * differ from [ShareLinks.HOST_NAME]; if the page loaded at all, the path is ours.
 */
fun captureWebDeepLink() {
    DeepLink.parsePath(currentBrowserPath())?.let { PendingDeepLink.set(it) }
}

/**
 * The current browser path (`window.location.pathname`), e.g. `/app/randomShareId`. Implemented per
 * web backend (js vs wasmJs) since raw JS-global access differs between Kotlin/JS and Kotlin/Wasm —
 * mirroring how [pickApkRaw] is split.
 */
internal expect fun currentBrowserPath(): String
