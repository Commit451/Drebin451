package com.commit451.drebin451

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.commit451.drebin451.auth.FirebaseWebConfig
import com.commit451.drebin451.auth.initializeAuth
import com.commit451.drebin451.share.captureWebDeepLink

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    // Initializes the Firebase JS SDK (no google-services plugin on web) and stashes the Google
    // client id, before anything touches Firebase.auth.
    initializeAuth(FirebaseWebConfig.WEB_CLIENT_ID)
    // Capture a share link opened in the browser (window.location → PendingDeepLink) before
    // Compose starts, so Home can add it to Shared and route to detail once splash/login resolve.
    captureWebDeepLink()
    ComposeViewport(viewportContainerId = "compose-root") {
        App()
    }
}
