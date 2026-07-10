package com.commit451.drebin451.storagewarning

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first

internal object StorageLimitWarningPreferencesStore {
    private var dataStore: DataStore<Preferences>? = null

    @Volatile
    private var dismissedCache: Boolean = false

    suspend fun init(dataStore: DataStore<Preferences>) {
        this.dataStore = dataStore
        dismissedCache = dataStore.data.first()[STORAGE_LIMIT_WARNING_DISMISSED_KEY] ?: false
    }

    fun isDismissed(): Boolean = dismissedCache

    suspend fun dismiss() {
        val updated = requireDataStore().edit { prefs ->
            prefs[STORAGE_LIMIT_WARNING_DISMISSED_KEY] = true
        }
        dismissedCache = updated[STORAGE_LIMIT_WARNING_DISMISSED_KEY] ?: true
    }

    private fun requireDataStore(): DataStore<Preferences> =
        dataStore ?: error("StorageLimitWarningPreferencesStore has not been initialized")
}
