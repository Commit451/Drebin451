package com.commit451.drebin451.push

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.commit451.drebin451.MainActivity
import com.commit451.drebin451.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Handles the "new version" pushes the server sends to an app's update topic. The server sends a
 * notification message, so this `onMessageReceived` runs only while the app is in the FOREGROUND
 * (the system displays the notification itself when backgrounded, using the manifest's default
 * notification icon + channel). Registered in the manifest for `com.google.firebase.MESSAGING_EVENT`.
 */
class AppMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val notification = message.notification
        val data = message.data
        val title = notification?.title ?: data[PushData.TITLE] ?: "New version available"
        val body = notification?.body ?: data[PushData.BODY].orEmpty()
        // Pushes carry the private release deep link directly, plus appId/versionId as a fallback
        // contract for background system notifications that surface data payload extras.
        val deepLink = data[PushData.DEEP_LINK] ?: run {
            val appId = data[PushData.APP_ID] ?: return
            val versionId = data[PushData.VERSION_ID] ?: return
            PushData.appVersionDeepLink(appId, versionId)
        }
        showNotification(deepLink, data[PushData.APP_ID], title, body)
    }

    private fun showNotification(deepLink: String, appId: String?, title: String, body: String) {
        // Route through MainActivity (singleTop), which records the link into PendingDeepLink, then
        // the nav host resolves it to the app-version release detail screen. The background tap of a
        // system-shown notification is handled by MainActivity reading the deep_link/appId/versionId
        // extras instead (see MainActivity.captureDeepLink).
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink), this, MainActivity::class.java)
            .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        // One notification per app: a newer "update available" replaces the older.
        val id = appId?.hashCode() ?: deepLink.hashCode()
        val pendingIntent = PendingIntent.getActivity(
            this,
            id,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, NotificationChannels.APP_UPDATES)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        builder.setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        getSystemService(NotificationManager::class.java).notify(id, builder.build())
    }
}
