package com.commit451.drebin451.follow

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.first

internal const val FOLLOW_DATA_STORE_FILE_NAME = "follows.preferences_pb"
internal const val FOLLOWED_APP_IDS_KEY_NAME = "followed_app_ids"
internal val FOLLOWED_APP_IDS_KEY = stringSetPreferencesKey(FOLLOWED_APP_IDS_KEY_NAME)

internal object FollowPreferencesStore {
    private var dataStore: DataStore<Preferences>? = null

    @Volatile
    private var followedCache: Set<String> = emptySet()

    suspend fun init(dataStore: DataStore<Preferences>) {
        this.dataStore = dataStore
        followedCache = dataStore.data.first()[FOLLOWED_APP_IDS_KEY] ?: emptySet()
    }

    fun isFollowing(appId: String): Boolean = followedCache.contains(appId)

    fun followedAppIds(): Set<String> = followedCache

    suspend fun add(appId: String) {
        val updated = requireDataStore().edit { prefs ->
            prefs[FOLLOWED_APP_IDS_KEY] = (prefs[FOLLOWED_APP_IDS_KEY] ?: emptySet()) + appId
        }
        followedCache = updated[FOLLOWED_APP_IDS_KEY] ?: emptySet()
    }

    suspend fun remove(appId: String) {
        val updated = requireDataStore().edit { prefs ->
            prefs[FOLLOWED_APP_IDS_KEY] = (prefs[FOLLOWED_APP_IDS_KEY] ?: emptySet()) - appId
        }
        followedCache = updated[FOLLOWED_APP_IDS_KEY] ?: emptySet()
    }

    suspend fun clear() {
        requireDataStore().edit { it.remove(FOLLOWED_APP_IDS_KEY) }
        followedCache = emptySet()
    }

    private fun requireDataStore(): DataStore<Preferences> =
        dataStore ?: error("FollowPreferencesStore has not been initialized")
}
