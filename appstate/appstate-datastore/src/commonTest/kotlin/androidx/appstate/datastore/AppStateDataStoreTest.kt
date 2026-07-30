/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.appstate.datastore

import androidx.appstate.AppState
import androidx.appstate.AppStateKey
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot.Companion.sendApplyNotifications
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import androidx.kruth.assertThat
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.Serializable
import okio.FileSystem

@OptIn(ExperimentalCoroutinesApi::class)
class AppStateDataStoreTest {

    private val testFile =
        FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "test_datastore_${Random.nextInt()}.json"

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Serializable @PersistToDataStore object StringKey : AppStateKey<String>()

    @Serializable @PersistToDataStore object IntKey : AppStateKey<Int>()

    @Serializable object NonPersistedStringKey : AppStateKey<String>()

    @Test
    fun testAppStateGetStateReturnsDefaultValue() = runTest {
        // Required for Compose state observation in JVM tests, even though our logic runs on
        // Dispatchers.Default.
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val dataStore =
            DataStoreFactory.create(
                storage =
                    OkioStorage<AppStatePreferences>(FileSystem.SYSTEM, AppStateSerializer) {
                        testFile
                    },
                scope = backgroundScope,
            )
        val defaultValue = "default"
        val appState = AppState()
        val job = launch { appState.addAppStateToDataStoreListener(dataStore) }

        val state = appState.getState(StringKey, defaultValue)
        assertThat(state.value).isEqualTo(defaultValue)

        val appStateDataStore = dataStore.data.first()
        assertThat(appStateDataStore.asMap().isEmpty()).isTrue()

        job.cancel()
    }

    @Test
    fun testAppStateSetStateUpdatesDataStore() = runTest {
        // Required for Compose state observation in JVM tests, even though our logic runs on
        // Dispatchers.Default.
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val dataStore =
            DataStoreFactory.create(
                storage =
                    OkioStorage<AppStatePreferences>(FileSystem.SYSTEM, AppStateSerializer) {
                        testFile
                    },
                scope = backgroundScope,
            )
        val defaultValue = "default"
        val updatedValue = "new value"
        val appState = AppState()
        appState.getState(StringKey, defaultValue)
        val job = launch { appState.addAppStateToDataStoreListener(dataStore) }

        // Update AppState key value
        appState.setState(StringKey, updatedValue)

        // Ensure compose snapshot effects are propagated
        sendApplyNotifications()
        advanceUntilIdle()

        val state = appState.getState(StringKey, defaultValue)
        assertThat(state.value).isEqualTo(updatedValue)

        val appStateValue = appState.getState(StringKey, defaultValue).value
        val appStateDataStore = dataStore.data.first { it[StringKey] == updatedValue }

        // Confirm datastore reflects the updated app state
        assertThat(appStateDataStore[StringKey]).isEqualTo(appStateValue)

        job.cancel()
    }

    @Test
    fun testAppStateUpdateStateUpdatesDataStore() = runTest {
        // Required for Compose state observation in JVM tests, even though our logic runs on
        // Dispatchers.Default.
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val dataStore =
            DataStoreFactory.create(
                storage =
                    OkioStorage<AppStatePreferences>(FileSystem.SYSTEM, AppStateSerializer) {
                        testFile
                    },
                scope = backgroundScope,
            )
        val initialValue = 5
        val defaultValue = 0
        val updatedValue = 10

        val appState = AppState()
        appState.getState(IntKey, defaultValue)
        val job = launch { appState.addAppStateToDataStoreListener(dataStore) }

        // Set initial AppState value
        appState.setState(IntKey, initialValue)
        // Update AppState key value
        appState.updateState(IntKey, defaultValue) { it + 5 }

        // Ensure compose snapshot effects are propagated
        sendApplyNotifications()
        advanceUntilIdle()

        val appStateValue = appState.getState(IntKey, defaultValue).value
        val appStateDataStore = dataStore.data.first { it[IntKey] == updatedValue }

        // Confirm datastore reflects the updated app state
        assertThat(appStateDataStore[IntKey]).isEqualTo(appStateValue)

        job.cancel()
    }

    @Test
    fun testAppStateSetStateDoesNotUpdateDataStoreWhenNotAnnotated() = runTest {
        // Required for Compose state observation in JVM tests, even though our logic runs on
        // Dispatchers.Default.
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val dataStore =
            DataStoreFactory.create(
                storage =
                    OkioStorage<AppStatePreferences>(FileSystem.SYSTEM, AppStateSerializer) {
                        testFile
                    },
                scope = backgroundScope,
            )
        val defaultValue = "default"
        val updatedValue = "new value"
        val appState = AppState()
        appState.getState(NonPersistedStringKey, defaultValue)
        val job = launch { appState.addAppStateToDataStoreListener(dataStore) }

        // Update AppState key value
        appState.setState(NonPersistedStringKey, updatedValue)

        // Ensure compose snapshot effects are propagated
        sendApplyNotifications()
        advanceUntilIdle()

        val state = appState.getState(NonPersistedStringKey, defaultValue)
        assertThat(state.value).isEqualTo(updatedValue)

        val appStateDataStore = dataStore.data.first()

        // Confirm datastore does not reflect the updated app state
        val keyName = NonPersistedStringKey::class.qualifiedName
        assertThat(appStateDataStore.asMap().containsKey(keyName)).isFalse()

        job.cancel()
    }

    @Test
    fun testDataStoreRestoresIntoAppStateOnListenerRegistration() = runTest {
        // Required for Compose state observation in JVM tests, even though our logic runs on
        // Dispatchers.Default.
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val dataStore =
            DataStoreFactory.create(
                storage =
                    OkioStorage<AppStatePreferences>(FileSystem.SYSTEM, AppStateSerializer) {
                        testFile
                    },
                scope = backgroundScope,
            )
        val defaultValue = "default"
        val storedValue = "stored value"

        // Setup the datastore with an existing value first
        dataStore.edit { settings -> settings[StringKey] = storedValue }

        val appState = AppState()
        // Initialize AppState with default value
        val state = appState.getState(StringKey, defaultValue)
        assertThat(state.value).isEqualTo(defaultValue)

        // Register the listener, which should trigger a datastore read and update appstate
        val job = launch { appState.addAppStateToDataStoreListener(dataStore) }

        sendApplyNotifications()
        advanceUntilIdle()

        // Wait until AppState has been updated with the value from DataStore
        snapshotFlow { state.value }.first { it == storedValue }

        // Verify that AppState has been updated with the value from DataStore
        assertThat(state.value).isEqualTo(storedValue)

        job.cancel()
    }

    @Test
    fun testCancelAppStateToDataStoreListenerStopsWrites() = runTest {
        // Required for Compose state observation in JVM tests, even though our logic runs on
        // Dispatchers.Default.
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val dataStore =
            DataStoreFactory.create(
                storage =
                    OkioStorage<AppStatePreferences>(FileSystem.SYSTEM, AppStateSerializer) {
                        testFile
                    },
                scope = backgroundScope,
            )
        val defaultValue = "default"
        val firstValue = "first value"
        val secondValue = "second value"

        val appState = AppState()
        appState.getState(StringKey, defaultValue)

        // Register the listener and capture the job so we can cancel for cleanup
        val job = launch { appState.addAppStateToDataStoreListener(dataStore) }

        // Update AppState key value
        appState.setState(StringKey, firstValue)

        sendApplyNotifications()
        advanceUntilIdle()

        // Confirm datastore reflects the first update
        var appStateDataStore = dataStore.data.first { it[StringKey] == firstValue }
        assertThat(appStateDataStore[StringKey]).isEqualTo(firstValue)

        // Cancel the listener
        job.cancel()

        // Update AppState key value again
        appState.setState(StringKey, secondValue)

        sendApplyNotifications()
        advanceUntilIdle()

        // Confirm AppState was updated
        val state = appState.getState(StringKey, defaultValue)
        assertThat(state.value).isEqualTo(secondValue)

        // Confirm datastore does NOT reflect the second update
        appStateDataStore = dataStore.data.first()
        assertThat(appStateDataStore[StringKey]).isEqualTo(firstValue)
    }
}
