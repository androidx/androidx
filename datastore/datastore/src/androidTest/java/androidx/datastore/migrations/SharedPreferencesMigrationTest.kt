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

package androidx.datastore.migrations

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.TestingSerializer
import androidx.datastore.dataStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

private const val sharedPrefsName = "shared_prefs_name"

private val Context.dsWithSpMigration by dataStore(
    "ds_with_sp_migration",
    serializer = TestingSerializer(),
    produceMigrations = { applicationContext ->
        listOf(
            SharedPreferencesMigration(
                applicationContext,
                sharedPrefsName
            ) { sharedPreferencesView: SharedPreferencesView, t: Byte ->
                t.plus(sharedPreferencesView.getInt("integer_key", -1)).toByte()
            }
        )
    }
)

@MediumTest
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SharedPreferencesMigrationTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var datastoreFile: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        sharedPrefs = context.getSharedPreferences(sharedPrefsName, Context.MODE_PRIVATE)
        datastoreFile = temporaryFolder.newFile("test_file.preferences_pb")

        assertThat(sharedPrefs.edit().clear().commit()).isTrue()
    }

    @Test
    fun testShouldMigrateSkipsMigration() = runBlockingTest {
        val sharedPrefsMigration = SharedPreferencesMigration<Byte>(
            context = context,
            sharedPreferencesName = sharedPrefsName,
            shouldRunMigration = { false }
        ) { _: SharedPreferencesView, _: Byte ->
            throw IllegalStateException("Migration should've been skipped.")
        }

        val dataStore = getDataStoreWithMigrations(listOf(sharedPrefsMigration))

        // Make sure we aren't running migrate()
        dataStore.data.first()
    }

    @Test
    fun testSharedPrefsViewContainsSpecifiedKeys() = runBlockingTest {
        val includedKey = "key1"
        val includedVal = 1
        val notMigratedKey = "key2"

        assertThat(
            sharedPrefs.edit()
                .putInt(includedKey, includedVal)
                .putInt(notMigratedKey, 123).commit()
        ).isTrue()

        val sharedPrefsMigration = SharedPreferencesMigration(
            context = context,
            sharedPreferencesName = sharedPrefsName,
            keysToMigrate = setOf(includedKey)
        ) { prefs: SharedPreferencesView, _: Byte ->
            assertThat(prefs.getInt(includedKey, -1)).isEqualTo(includedVal)
            assertThrows<IllegalStateException> { prefs.getInt(notMigratedKey, -1) }
            assertThat(prefs.getAll()).isEqualTo(mapOf(includedKey to includedVal))

            99.toByte()
        }

        val dataStore = getDataStoreWithMigrations(listOf(sharedPrefsMigration))

        assertThat(dataStore.data.first()).isEqualTo(99)
        assertThat(sharedPrefs.contains(includedKey)).isFalse()
        assertThat(sharedPrefs.contains(notMigratedKey)).isTrue()
    }

    @Test
    fun testSharedPrefsViewWithAllKeysSpecified() = runBlockingTest {
        val key1 = "key1"
        val val1 = 1
        val key2 = "key2"
        val val2 = 2

        assertThat(
            sharedPrefs.edit()
                .putInt(key1, val1)
                .putInt(key2, val2)
                .commit()
        ).isTrue()

        val sharedPrefsMigration = SharedPreferencesMigration(
            context = context,
            sharedPreferencesName = sharedPrefsName
        ) { prefs: SharedPreferencesView, _: Byte ->
            assertThat(prefs.getInt(key1, -1)).isEqualTo(val1)
            assertThat(prefs.getInt(key2, -1)).isEqualTo(val2)

            assertThat(prefs.getAll()).isEqualTo(mapOf(key1 to val1, key2 to val2))

            99.toByte()
        }

        val dataStore = getDataStoreWithMigrations(listOf(sharedPrefsMigration))

        assertThat(dataStore.data.first()).isEqualTo(99)
        assertThat(sharedPrefs.contains(key1)).isFalse()
        assertThat(sharedPrefs.contains(key2)).isFalse()
    }

    @Test
    fun testSharedPrefsViewWithAllKeysSpecified_doesntThrowErrorWhenKeyDoesntExist() =
        runBlockingTest {
            assertThat(sharedPrefs.edit().putInt("unrelated_key", -123).commit()).isTrue()

            val migration = SharedPreferencesMigration(
                produceSharedPreferences = { sharedPrefs },
            ) { prefs: SharedPreferencesView, _: Byte ->
                prefs.getInt("this_key_doesnt_exist_yet", 123).toByte()
            }

            val dataStore = getDataStoreWithMigrations(listOf(migration))
            assertThat(dataStore.data.first()).isEqualTo(123)
        }

    @Test
    fun producedSharedPreferencesIsUsed() = runBlockingTest {
        assertThat(sharedPrefs.edit().putInt("integer_key", 123).commit()).isTrue()

        val migration = SharedPreferencesMigration(
            produceSharedPreferences = { sharedPrefs }
        ) { prefs: SharedPreferencesView, _: Byte ->
            assertThat(prefs.getAll().size).isEqualTo(1)
            assertThat(prefs.getInt("integer_key", 0)).isEqualTo(123)
            123
        }

        val dataStore = getDataStoreWithMigrations(listOf(migration))
        assertThat(dataStore.data.first()).isEqualTo(123)
    }

    @Test
    fun testWithTopLevelDataStoreDelegate() = runBlocking<Unit> {
        File(context.filesDir, "/datastore").deleteRecursively()
        assertThat(sharedPrefs.edit().putInt("integer_key", 123).commit()).isTrue()

        assertThat(context.dsWithSpMigration.data.first()).isEqualTo(123)
    }

    private fun getDataStoreWithMigrations(
        migrations: List<DataMigration<Byte>>
    ): DataStore<Byte> {
        return DataStoreFactory.create(
            serializer = TestingSerializer(),
            migrations = migrations,
            scope = TestCoroutineScope()
        ) { datastoreFile }
    }
}