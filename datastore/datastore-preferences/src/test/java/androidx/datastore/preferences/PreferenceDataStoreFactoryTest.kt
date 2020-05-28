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

import androidx.datastore.DataMigration
import androidx.datastore.handlers.ReplaceFileCorruptionHandler
import com.google.common.truth.Truth.assertThat
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

@ObsoleteCoroutinesApi
@kotlinx.coroutines.ExperimentalCoroutinesApi
@FlowPreview
class PreferenceDataStoreFactoryTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var testFile: File
    private lateinit var dataStoreScope: TestCoroutineScope

    @Before
    fun setUp() {
        testFile = tmp.newFile("test_file." + PreferencesSerializer.fileExtension)
        dataStoreScope = TestCoroutineScope()
    }

    @Test
    fun testNewInstance() = runBlockingTest {
        val factory = PreferenceDataStoreFactory()
        val store = factory.create(
            produceFile = { testFile },
            scope = dataStoreScope
        )

        val expectedPreferences = Preferences.Builder()
            .setString("key", "value")
            .build()

        assertThat(store.updateData {
            it.toBuilder().setString("key", "value").build()
        }).isEqualTo(expectedPreferences)
        assertThat(store.data.first()).isEqualTo(expectedPreferences)
    }

    @Test
    fun testCorruptionHandlerInstalled() = runBlockingTest {
        testFile.writeBytes(byteArrayOf(0x00, 0x00, 0x00, 0x03)) // Protos can not start with 0x00.

        val factory = PreferenceDataStoreFactory()

        val valueToReplace = Preferences.Builder().setBoolean("key", true).build()

        val store = factory.create(
            produceFile = { testFile },
            corruptionHandler = ReplaceFileCorruptionHandler<Preferences> {
                valueToReplace
            },
            scope = dataStoreScope
        )
        assertThat(store.data.first()).isEqualTo(valueToReplace)
    }

    @Test
    fun testMigrationsInstalled() = runBlockingTest {
        val factory = PreferenceDataStoreFactory()

        val expectedPreferences = Preferences.Builder()
            .setString("string_key", "value")
            .setBoolean("boolean_key", true)
            .build()

        val migrateTo5 = {
            object : DataMigration<Preferences> {
                override suspend fun shouldMigrate() = true

                override suspend fun migrate(currentData: Preferences) =
                    currentData.toBuilder().setString("string_key", "value").build()

                override suspend fun cleanUp() {}
            }
        }
        val migratePlus1 = {
            object : DataMigration<Preferences> {
                override suspend fun shouldMigrate() = true

                override suspend fun migrate(currentData: Preferences) =
                    currentData.toBuilder().setBoolean("boolean_key", true).build()

                override suspend fun cleanUp() {}
            }
        }

        val store = factory.create(
            produceFile = { testFile },
            migrationProducers = listOf(migrateTo5, migratePlus1),
            scope = dataStoreScope
        )

        assertThat(store.data.first()).isEqualTo(expectedPreferences)
    }
}