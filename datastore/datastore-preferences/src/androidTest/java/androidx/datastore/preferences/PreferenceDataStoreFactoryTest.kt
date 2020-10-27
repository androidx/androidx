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

package androidx.datastore.preferences

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.clear
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.minusAssign
import androidx.datastore.preferences.core.plusAssign
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.putAll
import androidx.datastore.preferences.core.remove
import androidx.datastore.preferences.core.to
import androidx.datastore.preferences.core.toMutablePreferences
import androidx.datastore.preferences.core.toPreferences
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.lang.IllegalStateException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@ObsoleteCoroutinesApi
@kotlinx.coroutines.ExperimentalCoroutinesApi
@FlowPreview
class PreferenceDataStoreFactoryTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var testFile: File
    private lateinit var dataStoreScope: TestCoroutineScope
    private lateinit var context: Context

    val stringKey = preferencesKey<String>("key")
    val booleanKey = preferencesKey<Boolean>("key")

    @Before
    fun setUp() {
        testFile =
            tmp.newFile("test_file." + /*PreferencesSerializer.fileExtension=*/"preferences_pb")
        dataStoreScope = TestCoroutineScope()
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testNewInstance() = runBlockingTest {
        val store = PreferenceDataStoreFactory.create(
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
    fun testCorruptionHandlerInstalled() = runBlockingTest {
        testFile.writeBytes(byteArrayOf(0x00, 0x00, 0x00, 0x03)) // Protos can not start with 0x00.

        val valueToReplace = preferencesOf(booleanKey to true)

        val store = PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler<Preferences> {
                valueToReplace
            },
            scope = dataStoreScope
        ) { testFile }
        assertEquals(valueToReplace, store.data.first())
    }

    @Test
    fun testMigrationsInstalled() = runBlockingTest {

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
            migrations = listOf(migrateTo5, migratePlus1),
            scope = dataStoreScope
        ) { testFile }

        assertEquals(expectedPreferences, store.data.first())
    }

    @Test
    fun testCreateWithContextAndName() = runBlockingTest {
        val prefs = preferencesOf(stringKey to "value")

        var store = context.createDataStore(
            name = "my_settings",
            scope = dataStoreScope
        )
        store.updateData { prefs }

        // Create it again and confirm it's still there
        store = context.createDataStore("my_settings", scope = dataStoreScope)
        assertEquals(prefs, store.data.first())

        // Check that the file name is context.filesDir + name + ".preferences_pb"
        store = PreferenceDataStoreFactory.create(
            scope = dataStoreScope
        ) {
            File(context.filesDir, "datastore/my_settings.preferences_pb")
        }
        assertEquals(prefs, store.data.first())
    }

    @Test
    fun testCantMutateInternalState() = runBlockingTest {
        val store =
            PreferenceDataStoreFactory.create(scope = dataStoreScope) { testFile }

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