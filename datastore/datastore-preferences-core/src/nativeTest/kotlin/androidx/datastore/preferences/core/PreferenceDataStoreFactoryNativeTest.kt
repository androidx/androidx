/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.datastore.preferences.core

import androidx.datastore.OkioTestIO
import androidx.datastore.core.DataMigration
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path

@OptIn(ExperimentalCoroutinesApi::class)
class PreferenceDataStoreFactoryNativeTest {

    private lateinit var testIO: OkioTestIO
    private lateinit var testFile: Path
    private lateinit var dataStoreScope: TestScope

    val stringKey = stringPreferencesKey("key")
    val booleanKey = booleanPreferencesKey("key")

    @BeforeTest
    fun setUp() {
        testIO = OkioTestIO()
        testFile = testIO.newTempFile(relativePath = "test.preferences_pb").path
        dataStoreScope = TestScope(UnconfinedTestDispatcher())
    }

    @Test
    fun testNewInstance() = runTest {
        val store = PreferenceDataStoreFactory.createWithPath(
            scope = dataStoreScope
        ) { testFile }

        val expectedPreferences =
            preferencesOf(stringKey to "value")

        assertEquals(
            store.edit { prefs ->
                prefs[stringKey] = "value"
            },
            expectedPreferences
        )
        assertEquals(expectedPreferences, store.data.first())
    }

    @Test
    fun testCorruptionHandlerInstalled() = runTest {
        FileSystem.SYSTEM.createDirectories(testFile.parent!!, false)
        FileSystem.SYSTEM.write(testFile, false) {
            write("BadData".encodeToByteArray())
        }

        val valueToReplace = preferencesOf(booleanKey to true)

        val store = PreferenceDataStoreFactory.createWithPath(
            corruptionHandler = ReplaceFileCorruptionHandler<Preferences> {
                valueToReplace
            },
            scope = dataStoreScope
        ) { testFile }
        assertEquals(valueToReplace, store.data.first())
    }

    @Test
    fun testMigrationsInstalled() = runTest {

        val expectedPreferences = preferencesOf(
            stringKey to "value",
            booleanKey to true
        )

        val migrateTo5 = object : DataMigration<Preferences> {
            override suspend fun shouldMigrate(currentData: Preferences) = true

            override suspend fun migrate(currentData: Preferences) =
                currentData.toMutablePreferences().apply { set(stringKey, "value") }.toPreferences()

            override suspend fun cleanUp() {}
        }

        val migratePlus1 = object : DataMigration<Preferences> {
            override suspend fun shouldMigrate(currentData: Preferences) = true

            override suspend fun migrate(currentData: Preferences) =
                currentData.toMutablePreferences().apply { set(booleanKey, true) }.toPreferences()

            override suspend fun cleanUp() {}
        }

        val store = PreferenceDataStoreFactory.createWithPath(
            migrations = listOf(migrateTo5, migratePlus1),
            scope = dataStoreScope
        ) { testFile }

        assertEquals(expectedPreferences, store.data.first())
    }

    @Test
    fun testCantMutateInternalState() = runTest {
        val store =
            PreferenceDataStoreFactory.createWithPath(scope = dataStoreScope) { testFile }

        var mutableReference: MutablePreferences? = null
        store.edit {
            mutableReference = it
            it[stringKey] = "ABCDEF"
        }

        assertEquals(
            store.data.first(),
            preferencesOf(stringKey to "ABCDEF")
        )

        assertFailsWith<IllegalStateException> {
            mutableReference!!.clear()
        }

        assertFailsWith<IllegalStateException> {
            mutableReference!! += preferencesOf(stringKey to "abc")
        }

        assertFailsWith<IllegalStateException> {
            mutableReference!! += stringKey to "abc"
        }

        assertFailsWith<IllegalStateException> {
            mutableReference!! -= stringKey
        }

        assertFailsWith<IllegalStateException> {
            mutableReference!!.remove(stringKey)
        }

        assertFailsWith<IllegalStateException> {
            mutableReference!!.putAll(stringKey to "abc")
        }

        assertFailsWith<IllegalStateException> {
            mutableReference!![stringKey] = "asdjkfajksdhljkasdhf"
        }
        assertEquals(
            store.data.first(),
            preferencesOf(stringKey to "ABCDEF")
        )
    }
}
