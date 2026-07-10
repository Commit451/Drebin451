package com.commit451.drebin451.push

/**
 * FCM topic names, shared by the server (which sends to them) and the client (which subscribes to
 * them when a user follows an app) so both sides compute the exact same string. Lives in `:core`
 * for that reason.
 */
object PushTopics {
    private const val APP_UPDATES_PREFIX = "app_updates_"

    /**
     * Topic for "a new version of [appId] was uploaded". An app id is `"$ownerUserId:$applicationId"`,
     * but FCM topic names may only match `[a-zA-Z0-9-_.~%]+`, so the `:` separator has to go.
     *
     * It becomes a `-`, not a `%3A` percent-escape: subscribing hits the InstanceID
     * `rel/topics/{topic}` endpoint where the topic is a URL path segment, so the server decodes
     * `%3A` back to `:` and rejects it with a non-retryable 400. A `-` survives the round trip, and
     * since neither a Firebase uid (alphanumeric) nor an Android applicationId (`[A-Za-z0-9_.]`) ever
     * contains a `-`, the encoding stays collision-free.
     */
    fun appUpdates(appId: String): String = APP_UPDATES_PREFIX + appId.replace(":", "-")
}
