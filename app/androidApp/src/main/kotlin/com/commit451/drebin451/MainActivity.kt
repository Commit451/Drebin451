package com.commit451.drebin451

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.commit451.drebin451.auth.initializeAuth
import com.commit451.drebin451.push.PushData
import com.commit451.drebin451.share.DeepLink
import com.commit451.drebin451.share.PendingDeepLink

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // The app is always dark-themed (see App.kt), so force light system-bar icons regardless of
        // the device's light/dark setting. The default enableEdgeToEdge() uses SystemBarStyle.auto,
        // which keys icon color off the system setting and shows dark (invisible) icons in light mode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        super.onCreate(savedInstanceState)

        // Production/local builds get this resource from the ignored google-services.json. Public
        // and fork builds can still compile without Firebase configuration; Google sign-in remains
        // unavailable until a real config file is supplied.
        // Keep this as a static R reference: release resource shrinking cannot see resources looked
        // up through getIdentifier() and previously removed this value from the published APK.
        initializeAuth(getString(R.string.default_web_client_id))
        if (captureDeepLink(intent)) {
            clearDeepLinkIntent()
        }

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // singleTop: a link tapped while we're already running arrives here, not a fresh onCreate.
        setIntent(intent)
        if (captureDeepLink(intent)) {
            clearDeepLinkIntent()
        }
    }

    /** Records an incoming deep link (if any) so the UI can navigate to it once routing is ready. */
    private fun captureDeepLink(intent: Intent?): Boolean {
        // App/custom links arrive as intent.data. A "notification" FCM message tapped from the
        // background is launched by the system with the message's data payload as extras, so fall
        // back to the deep_link extra, then the appId/versionId pair.
        val link = intent?.data?.toString()
            ?: intent?.extras?.getString(PushData.DEEP_LINK)
            ?: intent?.extras?.let { extras ->
                val appId = extras.getString(PushData.APP_ID)
                val versionId = extras.getString(PushData.VERSION_ID)
                if (appId.isNullOrBlank() || versionId.isNullOrBlank()) {
                    null
                } else {
                    PushData.appVersionDeepLink(appId, versionId)
                }
            }
        val target = DeepLink.parse(link) ?: return false
        PendingDeepLink.set(target)
        return true
    }

    /**
     * Android keeps the Activity's last Intent around for task restore / launcher re-entry. Once a
     * notification or App Link has been copied into PendingDeepLink, replace that source Intent with
     * a plain launcher Intent so reopening the existing task cannot replay the same deep link.
     */
    private fun clearDeepLinkIntent() {
        setIntent(
            Intent(Intent.ACTION_MAIN)
                .setClass(this, MainActivity::class.java)
                .addCategory(Intent.CATEGORY_LAUNCHER),
        )
    }
}
