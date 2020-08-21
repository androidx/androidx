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
import androidx.datastore.DataMigration
import androidx.datastore.handlers.ReplaceFileCorruptionHandler
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
import kotlin.test.assertEquals

@ObsoleteCoroutinesApi
@kotlinx.coroutines.ExperimentalCoroutinesApi
@FlowPreview
class PreferenceDataStoreFactoryTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var testFile: File
    private lateinit var dataStoreScope: TestCoroutineScope
    private lateinit var context: Context

    @Before
    fun setUp() {
        testFile = tmp.newFile("test_file." + PreferencesSerializer.fileExtension)
        dataStoreScope = TestCoroutineScope()
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testNewInstance() = runBlockingTest {
        val store = PreferenceDataStoreFactory.create(
            produceFile = { testFile },
            scope = dataStoreScope
        )

        val expectedPreferences = Preferences.Builder()
            .setString("key", "value")
            .build()

        assertEquals(store.updateData {
            it.toBuilder().setString("key", "value").build()
        }, expectedPreferences)
        assertEquals(expectedPreferences, store.data.first())
    }

    @Test
    fun testCorruptionHandlerInstalled() = runBlockingTest {
        testFile.writeBytes(byteArrayOf(0x00, 0x00, 0x00, 0x03)) // Protos can not start with 0x00.

        val valueToReplace = Preferences.Builder().setBoolean("key", true).build()

        val store = PreferenceDataStoreFactory.create(
            produceFile = { testFile },
            corruptionHandler = ReplaceFileCorruptionHandler<Preferences> {
                valueToReplace
            },
            scope = dataStoreScope
        )
        assertEquals(valueToReplace, store.data.first())
    }

    @Test
    fun testMigrationsInstalled() = runBlockingTest {

        val expectedPreferences = Preferences.Builder()
            .setString("string_key", "value")
            .setBoolean("boolean_key", true)
            .build()

        val migrateTo5 = object : DataMigration<Preferences> {
            override suspend fun shouldMigrate(currentData: Preferences) = true

            override suspend fun migrate(currentData: Preferences) =
                currentData.toBuilder().setString("string_key", "value").build()

            override suspend fun cleanUp() {}
        }

        val migratePlus1 = object : DataMigration<Preferences> {
            override suspend fun shouldMigrate(currentData: Preferences) = true

            override suspend fun migrate(currentData: Preferences) =
                currentData.toBuilder().setBoolean("boolean_key", true).build()

            override suspend fun cleanUp() {}
        }

        val store = PreferenceDataStoreFactory.create(
            produceFile = { testFile },
            migrations = listOf(migrateTo5, migratePlus1),
            scope = dataStoreScope
        )

        assertEquals(expectedPreferences, store.data.first())
    }

    @Test
    fun testCreateWithContextAndName() = runBlockingTest {
        val prefs = Preferences.Builder().setInt("int_key", 12345).build()

        var store = context.createDataStore(
            name = "my_settings",
            scope = dataStoreScope
        )
        store.updateData { prefs }

        // Create it again and confirm it's still there
        store = context.createDataStore("my_settings", scope = dataStoreScope)
        assertEquals(prefs, store.data.first())

        // Check that the file name is context.filesDir + name + ".preferences_pb"
        store = PreferenceDataStoreFactory.create(produceFile = {
            File(context.filesDir, "datastore/my_settings.preferences_pb")
        }, scope = dataStoreScope)
        assertEquals(prefs, store.data.first())
    }
}