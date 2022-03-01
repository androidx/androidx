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
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals

val stringKey = stringPreferencesKey("key1")
val booleanKey = booleanPreferencesKey("key2")

val Context.basic by preferencesDataStore(name = "test1")
val Context.withCorruptionHandler by preferencesDataStore(
    name = "test2",
    corruptionHandler = ReplaceFileCorruptionHandler {
        preferencesOf(booleanKey to true)
    }
)

val Context.withMigrations by preferencesDataStore(
    name = "test3",
    produceMigrations = {
        listOf(
            object : DataMigration<Preferences> {
                override suspend fun shouldMigrate(currentData: Preferences) = true

                override suspend fun migrate(currentData: Preferences) =
                    currentData.toMutablePreferences().apply { set(stringKey, "value") }
                        .toPreferences()

                override suspend fun cleanUp() {}
            },
            object : DataMigration<Preferences> {
                override suspend fun shouldMigrate(currentData: Preferences) = true

                override suspend fun migrate(currentData: Preferences) =
                    currentData.toMutablePreferences().apply { set(booleanKey, true) }
                        .toPreferences()

                override suspend fun cleanUp() {}
            }

        )
    }
)

@ObsoleteCoroutinesApi
@kotlinx.coroutines.ExperimentalCoroutinesApi
@FlowPreview
class PreferenceDataStoreDelegateTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private lateinit var dataStoreScope: TestScope
    private lateinit var context: Context

    @Before
    fun setUp() {
        dataStoreScope = TestScope(UnconfinedTestDispatcher())
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testNewInstance() = runBlocking<Unit> {
        val expectedPreferences =
            preferencesOf(stringKey to "value")

        assertEquals(
            context.basic.edit { prefs ->
                prefs[stringKey] = "value"
            },
            expectedPreferences
        )
        assertEquals(expectedPreferences, context.basic.data.first())
    }

    @Test
    fun testCorruptionHandlerInstalled() = runBlocking<Unit> {
        context.preferencesDataStoreFile("test2").let {
            it.parentFile!!.mkdirs()

            it.writeBytes(
                byteArrayOf(0x00, 0x00, 0x00, 0x03) // Protos can not start with 0x00.
            )
        }

        val valueToReplace = preferencesOf(booleanKey to true)

        assertEquals(valueToReplace, context.withCorruptionHandler.data.first())
    }

    @Test
    fun testMigrationsInstalled() = runBlocking<Unit> {
        val expectedPreferences = preferencesOf(
            stringKey to "value",
            booleanKey to true
        )

        assertEquals(expectedPreferences, context.withMigrations.data.first())
    }
}