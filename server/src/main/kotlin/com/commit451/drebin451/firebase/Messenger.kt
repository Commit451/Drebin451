package com.commit451.drebin451.firebase

import com.commit451.drebin451.push.PushData
import com.commit451.drebin451.push.PushTopics
import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import org.slf4j.LoggerFactory

/**
 * Sends FCM push notifications via the Firebase Admin SDK (already initialized by [Firebasis]).
 * Notifications are topic-based: a device that follows an app subscribes to that app's update topic
 * client-side, and here we send one message to the topic — no device-token bookkeeping or fan-out.
 */
object Messenger {
    private val log = LoggerFactory.getLogger(Messenger::class.java)

    /**
     * Notifies every device subscribed to [appId]'s update topic that a new version was uploaded.
     *
     * Sends a data-only message so Android calls `AppMessagingService.onMessageReceived()` in both
     * foreground and background. The service then posts the visible notification with an explicit
     * `ACTION_VIEW` data URI PendingIntent, keeping notification taps on one client-controlled path
     * instead of relying on the FCM SDK's background system notification click-action/extras bridge.
     * HIGH priority for prompt delivery. Best-effort: any failure is logged and swallowed so it can
     * never fail the upload that triggered it.
     */
    fun sendNewVersion(
        appId: String,
        versionId: String,
        title: String,
        body: String,
        deepLink: String
    ) {
        try {
            val message = buildNewVersionMessage(
                appId = appId,
                versionId = versionId,
                title = title,
                body = body,
                deepLink = deepLink,
            )
            val messageId = FirebaseMessaging.getInstance().send(message)
            log.info("Sent new-version push for $appId (messageId=$messageId)")
        } catch (t: Throwable) {
            log.warn("Failed to send new-version push for $appId", t)
        }
    }

    internal fun buildNewVersionMessage(
        appId: String,
        versionId: String,
        title: String,
        body: String,
        deepLink: String,
    ): Message {
        val data = mapOf(
            PushData.TITLE to title,
            PushData.BODY to body,
            PushData.APP_ID to appId,
            PushData.VERSION_ID to versionId,
            PushData.DEEP_LINK to deepLink,
        )
        return Message.builder()
            .setTopic(PushTopics.appUpdates(appId))
            .putAllData(data)
            .setAndroidConfig(
                AndroidConfig.builder()
                    .setPriority(AndroidConfig.Priority.HIGH)
                    // Intentionally no AndroidNotification/click_action: this stays data-only so
                    // AppMessagingService posts the notification and both foreground/background
                    // taps use the same ACTION_VIEW data URI PendingIntent.
                    .putAllData(data)
                    .build(),
            )
            .build()
    }
}
