package com.commit451.drebin451.share

internal actual fun currentBrowserPath(): String {
    val path: String = js("window.location.pathname")
    return path
}
