package com.commit451.drebin451.firebase

import com.commit451.drebin451.newVersionPushDeepLink
import com.commit451.drebin451.push.PushData
import com.commit451.drebin451.push.PushTopics
import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.Notification
import kotlin.test.Test
import kotlin.test.assertEquals

class MessengerTest {

    @Test
    fun `new version push is data-only so app posts tap notification`() {
        val appId = "owner:com.example.app"
        val versionId = "version-1"
        val title = "Example App"
        val body = "Version 1.2.3 is available"
        val deepLink = newVersionPushDeepLink("share-token-123", versionId)

        val message = Messenger.buildNewVersionMessage(
            appId = appId,
            versionId = versionId,
            title = title,
            body = body,
            deepLink = deepLink,
        )

        assertEquals(PushTopics.appUpdates(appId), message.privateField("topic"))

        // No top-level Notification payload: keep the FCM message data-only so the Android app's
        // FirebaseMessagingService receives it in the background and posts the notification itself.
        assertEquals(null, message.privateField<Notification?>("notification"))

        val expectedData = mapOf(
            PushData.TITLE to title,
            PushData.BODY to body,
            PushData.APP_ID to appId,
            PushData.VERSION_ID to versionId,
            PushData.DEEP_LINK to deepLink,
        )
        assertEquals(expectedData, message.privateField("data"))

        val androidConfig = message.privateField<AndroidConfig>("androidConfig")
        assertEquals("high", androidConfig.privateField("priority"))
        assertEquals(expectedData, androidConfig.privateField("data"))
        assertEquals(null, androidConfig.privateField<Any?>("notification"))
    }
}

private inline fun <reified T> Any.privateField(name: String): T {
    val field = javaClass.getDeclaredField(name)
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    return field.get(this) as T
}
