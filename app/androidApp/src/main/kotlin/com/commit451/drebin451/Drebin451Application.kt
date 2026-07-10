package com.commit451.drebin451

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.commit451.drebin451.follow.initFollows
import com.commit451.drebin451.push.NotificationChannels
import com.commit451.drebin451.storagewarning.initStorageLimitWarningDismissal

/**
 * Runs on every process start — a UI launch or a cold start for the messaging service. Wires the
 * follow store to an app context, using KMP DataStore in noBackupFilesDir so local FCM topic state
 * is not restored across reinstalls, and creates the notification channel up front so it always
 * exists before [com.commit451.drebin451.push.AppMessagingService] posts a notification.
 */
class Drebin451Application : Application() {
    override fun onCreate() {
        super.onCreate()
        initFollows(this)
        initStorageLimitWarningDismissal(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            NotificationChannels.APP_UPDATES,
            "App updates",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "New versions of apps you follow"
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
