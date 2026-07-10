package com.commit451.drebin451.storagewarning

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.runBlocking

internal const val STORAGE_LIMIT_WARNING_DATA_STORE_FILE_NAME =
    "storage_limit_warning.preferences_pb"
internal const val STORAGE_LIMIT_WARNING_DISMISSED_KEY_NAME = "storage_limit_warning_dismissed"
internal val STORAGE_LIMIT_WARNING_DISMISSED_KEY =
    booleanPreferencesKey(STORAGE_LIMIT_WARNING_DISMISSED_KEY_NAME)

private var storageLimitWarningDataStore: DataStore<Preferences>? = null

/** Wires the storage-limit dismissal store to an application [context]. Call once on app start. */
fun initStorageLimitWarningDismissal(context: Context) {
    val appContext = context.applicationContext
    val dataStore = storageLimitWarningDataStore ?: PreferenceDataStoreFactory.create(
        // This is a per-device UI dismissal, so keep it in noBackupFilesDir rather than letting
        // Android Auto Backup restore it onto another/fresh device.
        produceFile = {
            appContext.noBackupFilesDir.resolve(
                STORAGE_LIMIT_WARNING_DATA_STORE_FILE_NAME
            )
        },
    ).also { storageLimitWarningDataStore = it }
    runBlocking { StorageLimitWarningPreferencesStore.init(dataStore) }
}

internal actual suspend fun isStorageLimitWarningDismissed(): Boolean =
    StorageLimitWarningPreferencesStore.isDismissed()

internal actual suspend fun dismissStorageLimitWarning() {
    StorageLimitWarningPreferencesStore.dismiss()
}
