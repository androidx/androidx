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

import androidx.appstate.StateStore
import androidx.appstate.StateStoreKey
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
class StateStoreDataStoreTest {

    private val testFile =
        FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "test_datastore_${Random.nextInt()}.json"

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    val defaultValue = "default"

    @Serializable @PersistToDataStore object StringKey : StateStoreKey<String>("default")

    @Serializable @PersistToDataStore object IntKey : StateStoreKey<Int>(0)

    @Serializable object NonPersistedStringKey : StateStoreKey<String>("default")

    @Test
    fun testStateStoreGetStateReturnsDefaultValue() = runTest {
        // Required for Compose state observation in JVM tests, even though our logic runs on
        // Dispatchers.Default.
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val stateStore = StateStore()
        val job = launch { stateStore.syncToDataStore(testFile.toString(), backgroundScope) }

        val state = stateStore.getState(StringKey)
        assertThat(state.value).isEqualTo(defaultValue)

        val dataStore =
            DataStoreFactory.create(
                storage = OkioStorage(FileSystem.SYSTEM, StateStoreSerializer) { testFile },
                scope = backgroundScope,
            )
        val stateStoreDataStore = dataStore.data.first()
        assertThat(stateStoreDataStore.asMap().isEmpty()).isTrue()

        job.cancel()
    }

    @Test
    fun testStateStoreSetStateUpdatesDataStore() = runTest {
        // Required for Compose state observation in JVM tests, even though our logic runs on
        // Dispatchers.Default.
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val dataStore =
            DataStoreFactory.create(
                storage = OkioStorage(FileSystem.SYSTEM, StateStoreSerializer) { testFile },
                scope = backgroundScope,
            )
        val updatedValue = "new value"
        val stateStore = StateStore()
        stateStore.getState(StringKey)
        val job = launch { stateStore.syncToDataStore(dataStore) }

        // Update StateStore key value
        stateStore.setState(StringKey, updatedValue)

        // Ensure compose snapshot effects are propagated
        sendApplyNotifications()
        advanceUntilIdle()

        val state = stateStore.getState(StringKey)
        assertThat(state.value).isEqualTo(updatedValue)

        val stateStoreValue = stateStore.getState(StringKey).value
        val stateStoreDataStore = dataStore.data.first { it[StringKey] == updatedValue }

        // Confirm datastore reflects the updated app state
        assertThat(stateStoreDataStore[StringKey]).isEqualTo(stateStoreValue)

        job.cancel()
    }

    @Test
    fun testStateStoreUpdateStateUpdatesDataStore() = runTest {
        // Required for Compose state observation in JVM tests, even though our logic runs on
        // Dispatchers.Default.
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val dataStore =
            DataStoreFactory.create(
                storage = OkioStorage(FileSystem.SYSTEM, StateStoreSerializer) { testFile },
                scope = backgroundScope,
            )
        val initialValue = 5
        val updatedValue = 10

        val stateStore = StateStore()
        stateStore.getState(IntKey)
        val job = launch { stateStore.syncToDataStore(dataStore) }

        // Set initial StateStore value
        stateStore.setState(IntKey, initialValue)
        // Update StateStore key value
        stateStore.updateState(IntKey) { it + 5 }

        // Ensure compose snapshot effects are propagated
        sendApplyNotifications()
        advanceUntilIdle()

        val stateStoreValue = stateStore.getState(IntKey).value
        val stateStoreDataStore = dataStore.data.first { it[IntKey] == updatedValue }

        // Confirm datastore reflects the updated app state
        assertThat(stateStoreDataStore[IntKey]).isEqualTo(stateStoreValue)

        job.cancel()
    }

    @Test
    fun testStateStoreSetStateDoesNotUpdateDataStoreWhenNotAnnotated() = runTest {
        // Required for Compose state observation in JVM tests, even though our logic runs on
        // Dispatchers.Default.
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val dataStore =
            DataStoreFactory.create(
                storage = OkioStorage(FileSystem.SYSTEM, StateStoreSerializer) { testFile },
                scope = backgroundScope,
            )
        val updatedValue = "new value"
        val stateStore = StateStore()
        stateStore.getState(NonPersistedStringKey)
        val job = launch { stateStore.syncToDataStore(dataStore) }

        // Update StateStore key value
        stateStore.setState(NonPersistedStringKey, updatedValue)

        // Ensure compose snapshot effects are propagated
        sendApplyNotifications()
        advanceUntilIdle()

        val state = stateStore.getState(NonPersistedStringKey)
        assertThat(state.value).isEqualTo(updatedValue)

        val stateStoreDataStore = dataStore.data.first()

        // Confirm datastore does not reflect the updated app state
        val keyName = NonPersistedStringKey::class.qualifiedName
        assertThat(stateStoreDataStore.asMap().containsKey(keyName)).isFalse()

        job.cancel()
    }

    @Test
    fun testDataStoreRestoresIntoStateStoreOnListenerRegistration() = runTest {
        // Required for Compose state observation in JVM tests, even though our logic runs on
        // Dispatchers.Default.
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val dataStore =
            DataStoreFactory.create(
                storage =
                    OkioStorage<StateStorePreferences>(FileSystem.SYSTEM, StateStoreSerializer) {
                        testFile
                    },
                scope = backgroundScope,
            )
        val storedValue = "stored value"

        // Setup the datastore with an existing value first
        dataStore.edit { settings -> settings[StringKey] = storedValue }

        val stateStore = StateStore()
        // Initialize StateStore with default value
        val state = stateStore.getState(StringKey)
        assertThat(state.value).isEqualTo(defaultValue)

        // Register the listener, which should trigger a datastore read and update stateStore
        val job = launch { stateStore.syncToDataStore(dataStore) }

        sendApplyNotifications()
        advanceUntilIdle()

        // Wait until StateStore has been updated with the value from DataStore
        snapshotFlow { state.value }.first { it == storedValue }

        // Verify that StateStore has been updated with the value from DataStore
        assertThat(state.value).isEqualTo(storedValue)

        job.cancel()
    }

    @Test
    fun testCancelStateStoreToDataStoreListenerStopsWrites() = runTest {
        // Required for Compose state observation in JVM tests, even though our logic runs on
        // Dispatchers.Default.
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val dataStore =
            DataStoreFactory.create(
                storage =
                    OkioStorage<StateStorePreferences>(FileSystem.SYSTEM, StateStoreSerializer) {
                        testFile
                    },
                scope = backgroundScope,
            )
        val firstValue = "first value"
        val secondValue = "second value"

        val stateStore = StateStore()
        stateStore.getState(StringKey)

        // Register the listener and capture the job so we can cancel for cleanup
        val job = launch { stateStore.syncToDataStore(dataStore) }

        // Update StateStore key value
        stateStore.setState(StringKey, firstValue)

        sendApplyNotifications()
        advanceUntilIdle()

        // Confirm datastore reflects the first update
        var stateStoreDataStore = dataStore.data.first { it[StringKey] == firstValue }
        assertThat(stateStoreDataStore[StringKey]).isEqualTo(firstValue)

        // Cancel the listener
        job.cancel()

        // Update StateStore key value again
        stateStore.setState(StringKey, secondValue)

        sendApplyNotifications()
        advanceUntilIdle()

        // Confirm StateStore was updated
        val state = stateStore.getState(StringKey)
        assertThat(state.value).isEqualTo(secondValue)

        // Confirm datastore does NOT reflect the second update
        stateStoreDataStore = dataStore.data.first()
        assertThat(stateStoreDataStore[StringKey]).isEqualTo(firstValue)
    }
}
