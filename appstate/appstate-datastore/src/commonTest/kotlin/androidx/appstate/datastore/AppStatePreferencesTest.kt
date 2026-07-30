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

import androidx.appstate.AppStateKey
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import androidx.kruth.assertThat
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import okio.FileSystem

@OptIn(ExperimentalCoroutinesApi::class)
class AppStatePreferencesTest {

    private val testFile =
        FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "test_datastore_${Random.nextInt()}.json"

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Serializable @PersistToDataStore object StringKey : AppStateKey<String>()

    @Serializable @PersistToDataStore object IntKey : AppStateKey<Int>()

    @Test
    fun testGetStateReturnsDefaultValue() = runTest {
        val dataStore =
            DataStoreFactory.create(
                storage =
                    OkioStorage<AppStatePreferences>(FileSystem.SYSTEM, AppStateSerializer) {
                        testFile
                    },
                scope = backgroundScope,
            )

        val state = dataStore.data.first()
        assertThat(state.asMap().isEmpty()).isTrue()
    }

    @Test
    fun testSetStateUpdatesValue() = runTest {
        val dataStore =
            DataStoreFactory.create(
                storage =
                    OkioStorage<AppStatePreferences>(FileSystem.SYSTEM, AppStateSerializer) {
                        testFile
                    },
                scope = backgroundScope,
            )
        val updatedValue = "new value"

        dataStore.edit { settings -> settings[StringKey] = updatedValue }

        val state = dataStore.data.first()
        assertThat(state[StringKey]).isEqualTo(updatedValue)
    }

    @Test
    fun testUpdateState() = runTest {
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

        dataStore.edit { settings -> settings[IntKey] = initialValue }

        dataStore.edit { settings ->
            val currentValue = settings[IntKey] ?: defaultValue
            settings[IntKey] = currentValue + 5
        }

        val state = dataStore.data.first()
        assertThat(state[IntKey]).isEqualTo(initialValue + 5)
    }
}
