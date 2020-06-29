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
import android.content.SharedPreferences
import androidx.datastore.DataMigration
import androidx.datastore.DataStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@kotlinx.coroutines.ExperimentalCoroutinesApi
@kotlinx.coroutines.ObsoleteCoroutinesApi
@kotlinx.coroutines.FlowPreview
@MediumTest
class SharedPreferencesToPreferencesTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val sharedPrefsName = "shared_prefs_name"

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
    fun existingKey_isMigrated() = runBlockingTest {
        val stringKey = "string_key"
        val stringValue = "string value"

        assertThat(sharedPrefs.edit().putString(stringKey, stringValue).commit()).isTrue()

        val migration = SharedPreferencesMigration(
            context = context,
            sharedPreferencesName = sharedPrefsName
        )

        val preferencesStore = getDataStoreWithMigrations(listOf(migration))

        val prefs = preferencesStore.data.first()

        assertThat(prefs.getString(stringKey, "default_string")).isEqualTo(stringValue)
        assertThat(prefs.getAll().size).isEqualTo(1)
    }

    @Test
    fun existingKey_isRemovedFromSharedPrefs() = runBlockingTest {
        val stringKey = "string_key"
        val stringValue = "string value"

        assertThat(sharedPrefs.edit().putString(stringKey, stringValue).commit()).isTrue()

        val migration = SharedPreferencesMigration(
            context = context,
            sharedPreferencesName = sharedPrefsName
        )

        val preferencesStore = getDataStoreWithMigrations(listOf(migration))

        // Get data so migration is run.
        preferencesStore.data.first()

        assertThat(sharedPrefs.contains(stringKey)).isFalse()
    }

    @Test
    fun supportsStringKey() = runBlockingTest {
        val stringKey = "string_key"
        val stringValue = "string_value"

        assertThat(sharedPrefs.edit().putString(stringKey, stringValue).commit()).isTrue()

        val migration = SharedPreferencesMigration(
            context = context,
            sharedPreferencesName = sharedPrefsName
        )

        val preferencesStore = getDataStoreWithMigrations(listOf(migration))
        val prefs = preferencesStore.data.first()
        assertThat(prefs.getString(stringKey, "default_string")).isEqualTo(stringValue)
        assertThat(prefs.getAll().size).isEqualTo(1)
    }

    @Test
    fun supportsIntegerKey() = runBlockingTest {
        val integerKey = "integer_key"
        val integerValue = 123

        assertThat(sharedPrefs.edit().putInt(integerKey, integerValue).commit()).isTrue()

        val migration = SharedPreferencesMigration(
            context = context,
            sharedPreferencesName = sharedPrefsName
        )

        val preferencesStore = getDataStoreWithMigrations(listOf(migration))
        val prefs = preferencesStore.data.first()
        assertThat(prefs.getInt(integerKey, -1)).isEqualTo(integerValue)
        assertThat(prefs.getAll().size).isEqualTo(1)
    }

    @Test
    fun supportsFloatKey() = runBlockingTest {
        val floatKey = "float_key"
        val floatValue = 123.0f

        assertThat(sharedPrefs.edit().putFloat(floatKey, floatValue).commit()).isTrue()

        val migration = SharedPreferencesMigration(
            context = context,
            sharedPreferencesName = sharedPrefsName
        )

        val preferencesStore = getDataStoreWithMigrations(listOf(migration))
        val prefs = preferencesStore.data.first()
        assertThat(prefs.getFloat(floatKey, -1.0f)).isEqualTo(floatValue)
        assertThat(prefs.getAll().size).isEqualTo(1)
    }

    @Test
    fun supportsBooleanKey() = runBlockingTest {
        val booleanKey = "boolean_key"
        val booleanValue = true

        assertThat(sharedPrefs.edit().putBoolean(booleanKey, booleanValue).commit()).isTrue()

        val migration = SharedPreferencesMigration(
            context = context,
            sharedPreferencesName = sharedPrefsName
        )

        val preferencesStore = getDataStoreWithMigrations(listOf(migration))
        val prefs = preferencesStore.data.first()
        assertThat(prefs.getBoolean(booleanKey, false)).isEqualTo(booleanValue)
        assertThat(prefs.getAll().size).isEqualTo(1)
    }

    @Test
    fun supportsLongKey() = runBlockingTest {
        val longKey = "long_key"
        val longValue = 1L shr 50

        assertThat(sharedPrefs.edit().putLong(longKey, longValue).commit()).isTrue()

        val migration = SharedPreferencesMigration(
            context = context,
            sharedPreferencesName = sharedPrefsName
        )

        val preferencesStore = getDataStoreWithMigrations(listOf(migration))
        val prefs = preferencesStore.data.first()
        assertThat(prefs.getLong(longKey, -1)).isEqualTo(longValue)
        assertThat(prefs.getAll().size).isEqualTo(1)
    }

    @Test
    fun supportsStringSetKey() = runBlockingTest {
        val stringSetKey = "stringSet_key"
        val stringSetValue = setOf("a", "b", "c")

        assertThat(sharedPrefs.edit().putStringSet(stringSetKey, stringSetValue).commit()).isTrue()

        val migration = SharedPreferencesMigration(
            context = context,
            sharedPreferencesName = sharedPrefsName
        )

        val preferencesStore = getDataStoreWithMigrations(listOf(migration))
        val prefs = preferencesStore.data.first()
        assertThat(prefs.getStringSet(stringSetKey, setOf())).isEqualTo(stringSetValue)
        assertThat(prefs.getAll().size).isEqualTo(1)
    }

    @Test
    fun migratedStringSetNotMutable() = runBlockingTest {
        val stringSetKey = "stringSet_key"
        val stringSetValue = setOf("a", "b", "c")

        assertThat(sharedPrefs.edit().putStringSet(stringSetKey, stringSetValue).commit()).isTrue()
        val sharedPrefsSet = sharedPrefs.getStringSet(stringSetKey, mutableSetOf())!!
        assertThat(sharedPrefsSet).isEqualTo(stringSetValue)

        val migration = SharedPreferencesMigration(
            context = context,
            sharedPreferencesName = sharedPrefsName
        )

        val preferencesStore = getDataStoreWithMigrations(listOf(migration))

        val prefs = preferencesStore.data.first()

        // Modify the sharedPrefs string set:
        sharedPrefsSet.add("d")

        assertThat(prefs.getStringSet(stringSetKey, setOf())).isEqualTo(stringSetValue)
        assertThat(prefs.getAll().size).isEqualTo(1)
    }

    @Test
    fun sharedPreferencesFileDeletedIfPrefsEmpty() = runBlockingTest {
        val integerKey = "integer_key"

        assertThat(sharedPrefs.edit().putInt(integerKey, 123).commit()).isTrue()

        val migration = SharedPreferencesMigration(
            context = context,
            sharedPreferencesName = sharedPrefsName,
            keysToMigrate = setOf(integerKey),
            deleteEmptyPreferences = true
        )

        val preferenceStore = getDataStoreWithMigrations(listOf(migration))
        preferenceStore.data.first()

        assertThat(getSharedPrefsFile(context, sharedPrefsName).exists()).isFalse()
    }

    @Test
    fun sharedPreferencesFileNotDeletedIfDisabled() = runBlockingTest {
        val integerKey = "integer_key"

        assertThat(sharedPrefs.edit().putInt(integerKey, 123).commit()).isTrue()

        val migration = SharedPreferencesMigration(
            context = context,
            sharedPreferencesName = sharedPrefsName,
            keysToMigrate = setOf(integerKey),
            deleteEmptyPreferences = false
        )

        val preferenceStore = getDataStoreWithMigrations(listOf(migration))
        preferenceStore.data.first()

        assertThat(getSharedPrefsFile(context, sharedPrefsName).exists()).isTrue()
    }

    @Test
    fun sharedPreferencesFileNotDeletedIfPrefsNotEmpty() = runBlockingTest {
        val integerKey1 = "integer_key1"
        val integerKey2 = "integer_key2"

        assertThat(sharedPrefs.edit().putInt(integerKey1, 123).putInt(integerKey2, 123).commit())
            .isTrue()

        val migration = SharedPreferencesMigration(
            context = context,
            sharedPreferencesName = sharedPrefsName,
            keysToMigrate = setOf(integerKey1),
            deleteEmptyPreferences = true
        )

        val preferenceStore = getDataStoreWithMigrations(listOf(migration))
        preferenceStore.data.first()

        assertThat(getSharedPrefsFile(context, sharedPrefsName).exists()).isTrue()
    }

    @Test
    fun sharedPreferencesBackupFileDeleted() = runBlockingTest {
        val integerKey = "integer_key"

        // Write to shared preferences then create the backup file
        val sharedPrefsFile = getSharedPrefsFile(context, sharedPrefsName)
        val sharedPrefsBackupFile = getSharedPrefsBackup(sharedPrefsFile)
        assertThat(sharedPrefs.edit().putInt(integerKey, 123).commit()).isTrue()
        assertThat(sharedPrefsFile.exists()).isTrue()
        assertThat(sharedPrefsFile.renameTo(sharedPrefsBackupFile)).isTrue()

        val migration = SharedPreferencesMigration(
            context = context,
            sharedPreferencesName = sharedPrefsName,
            keysToMigrate = setOf(integerKey),
            deleteEmptyPreferences = true
        )

        val preferenceStore = getDataStoreWithMigrations(listOf(migration))
        preferenceStore.data.first()

        assertThat(sharedPrefsBackupFile.exists()).isFalse()
    }

    @Test
    fun canSpecifyMultipleKeys() = runBlockingTest {
        val stringKey = "string_key"
        val integerKey = "integer_key"
        val keyNotMigrated = "dont_migrate_this_key"

        val stringValue = "string_value"
        val intValue = 12345
        val notMigratedString = "dont migrate this string"

        assertThat(
            sharedPrefs.edit()
                .putString(stringKey, stringValue)
                .putInt(integerKey, intValue)
                .putString(keyNotMigrated, notMigratedString)
                .commit()
        ).isTrue()

        val migration = SharedPreferencesMigration(
            context = context,
            sharedPreferencesName = sharedPrefsName,
            keysToMigrate = setOf(stringKey, integerKey)
        )

        val preferencesStore = getDataStoreWithMigrations(listOf(migration))

        val prefs = preferencesStore.data.first()

        assertThat(prefs.getString(stringKey, "default_string"))
            .isEqualTo(stringValue)
        assertThat(prefs.getInt(integerKey, -1))
            .isEqualTo(intValue)

        assertThat(prefs.getAll().size).isEqualTo(2)
        assertThat(sharedPrefs.getString(keyNotMigrated, "")).isEqualTo(notMigratedString)
    }

    @Test
    fun missingSpecifiedKeyIsNotMigrated() = runBlockingTest {
        val missingKey = "missing_key"

        val migration = SharedPreferencesMigration(
            context = context,
            sharedPreferencesName = sharedPrefsName,
            keysToMigrate = setOf(missingKey)
        )

        val preferencesStore = getDataStoreWithMigrations(listOf(migration))

        val prefs = preferencesStore.data.first()
        assertThat(prefs.contains(missingKey)).isFalse()
        assertThat(prefs.getAll()).doesNotContainKey(missingKey)
    }

    @Test
    fun runsIfAnySpecifiedKeyExists() = runBlockingTest {
        val integerKey = "integer_key"
        val missingKey = "missing_key"

        val integerValue = 123

        assertThat(sharedPrefs.edit().putInt(integerKey, integerValue).commit()).isTrue()

        val migration = SharedPreferencesMigration(
            context = context,
            sharedPreferencesName = sharedPrefsName,
            keysToMigrate = setOf(integerKey, missingKey)
        )

        val preferencesStore = getDataStoreWithMigrations(listOf(migration))
        val prefs = preferencesStore.data.first()
        assertThat(prefs.getInt(integerKey, -1)).isEqualTo(integerValue)
        assertThat(prefs.getAll().size).isEqualTo(1)
    }

    private fun getDataStoreWithMigrations(
        migrationProducers: List<() -> DataMigration<Preferences>>
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory().create(
            produceFile = { datastoreFile },
            migrationProducers = migrationProducers,
            scope = TestCoroutineScope()
        )
    }

    private fun getSharedPrefsFile(context: Context, name: String): File {
        // ContextImpl.getSharedPreferencesPath is private, so we have to reproduce the definition
        // here
        val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        return File(prefsDir, "$name.xml")
    }

    private fun getSharedPrefsBackup(prefsFile: File): File {
        // SharedPreferencesImpl.makeBackupFile is private, so we have to reproduce the definition
        // here
        return File(prefsFile.path + ".bak")
    }
}