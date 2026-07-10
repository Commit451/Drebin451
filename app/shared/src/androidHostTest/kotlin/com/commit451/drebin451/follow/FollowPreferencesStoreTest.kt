package com.commit451.drebin451.follow

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FollowPreferencesStoreTest {

    @Test
    fun initSeedsCacheFromPersistedFollowSet() = runBlocking {
        val store = fakeStore(preferencesOf(FOLLOWED_APP_IDS_KEY to setOf("app-1")))

        FollowPreferencesStore.init(store)

        assertTrue(FollowPreferencesStore.isFollowing("app-1"))
        assertFalse(FollowPreferencesStore.isFollowing("app-2"))
    }

    @Test
    fun addRemoveAndClearKeepCacheInSyncWithDataStore() = runBlocking {
        val store = fakeStore()

        FollowPreferencesStore.init(store)
        FollowPreferencesStore.add("app-1")
        assertTrue(FollowPreferencesStore.isFollowing("app-1"))

        FollowPreferencesStore.remove("app-1")
        assertFalse(FollowPreferencesStore.isFollowing("app-1"))

        FollowPreferencesStore.add("app-1")
        FollowPreferencesStore.add("app-2")
        FollowPreferencesStore.clear()

        assertFalse(FollowPreferencesStore.isFollowing("app-1"))
        assertFalse(FollowPreferencesStore.isFollowing("app-2"))
    }

    private fun fakeStore(initial: Preferences = preferencesOf()) =
        object : DataStore<Preferences> {
            private val state = MutableStateFlow(initial)

            override val data: Flow<Preferences> = state

            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
                val next = transform(state.value)
                state.value = next
                return next
            }
        }
}
