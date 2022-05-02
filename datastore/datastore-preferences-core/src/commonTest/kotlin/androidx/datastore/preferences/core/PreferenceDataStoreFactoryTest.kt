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

import androidx.datastore.core.DataMigration
import androidx.datastore.core.TestIO
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.asBufferedSink
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.BeforeTest
//import org.junit.Before
//import org.junit.Rule
//import org.junit.Test
//import org.junit.rules.TemporaryFolder
//import java.io.File
//import java.lang.IllegalStateException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import okio.use

@ObsoleteCoroutinesApi
@kotlinx.coroutines.ExperimentalCoroutinesApi
@FlowPreview
class PreferenceDataStoreFactoryTest {

//    private lateinit var testFile: TestFile
    private lateinit var testIO: TestIO
    private lateinit var testScope: TestScope
    private lateinit var dataStoreScope: TestScope
    private val preferencesSerializer = getSerializer()

    val stringKey = stringPreferencesKey("key")
    val booleanKey = booleanPreferencesKey("key")

    @BeforeTest
    fun setUp() {
        testIO = TestIO()
//        testFile = testIO.createTempFile("test_file")
        dataStoreScope = TestScope(UnconfinedTestDispatcher())
        testScope = TestScope(UnconfinedTestDispatcher())
    }

    @Test
    fun testNewInstance() = testScope.runTest {
        val store = PreferenceDataStoreFactory.create(
            storage = testIO.newFileStorage(preferencesSerializer),
            scope = dataStoreScope
        )

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
    fun testCorruptionHandlerInstalled() = testScope.runTest {
        val testFile = testIO.createTempFile("test")
        testIO.outputStream(testFile).asBufferedSink().use {
            it.write(byteArrayOf(0x00, 0x00, 0x00, 0x03)) // Protos can not start with 0x00.
        }

        val valueToReplace = preferencesOf(booleanKey to true)

        val store = PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler<Preferences> {
                valueToReplace
            },
            storage = testIO.newFileStorage(preferencesSerializer, testFile),
            scope = dataStoreScope
        )
        assertEquals(valueToReplace, store.data.first())
    }

    @Test
    fun testMigrationsInstalled() = testScope.runTest {

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

        val store = PreferenceDataStoreFactory.create(
            storage = testIO.newFileStorage(preferencesSerializer),
            migrations = listOf(migrateTo5, migratePlus1),
            scope = dataStoreScope
        )

        assertEquals(expectedPreferences, store.data.first())
    }

    @Test
    fun testCantMutateInternalState() = testScope.runTest {
        val store =
            PreferenceDataStoreFactory.create(
                storage = testIO.newFileStorage(preferencesSerializer),
                scope = dataStoreScope)

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