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
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val sharedPrefsName = "shared_prefs_name"

private val Context.dsWithSpMigration by preferencesDataStore(
    "ds_with_sp_migration",
    produceMigrations = { applicationContext ->
        listOf(SharedPreferencesMigration(applicationContext, sharedPrefsName))
    }
)

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

        assertTrue { sharedPrefs.edit().clear().commit() }
    }

    @Test
    fun existingKey_isMigrated() = runBlockingTest {
        val stringKey = stringPreferencesKey("string_key")
        val stringValue = "string value"

        assertTrue { sharedPrefs.edit().putString(stringKey.name, stringValue).commit() }

        val migration = SharedPreferencesMigration(
            context = context,
            sharedPreferencesName = sharedPrefsName
        )

        val preferencesStore = getDataStoreWithMigrations(listOf(migration))

        val prefs = preferencesStore.data.first()

        assertEquals(stringValue, prefs[stringKey])
        assertEquals(prefs.asMap().size, 1)
    }

    @Test
    fun existingKey_isRemovedFromSharedPrefs() = runBlockingTest {
        val stringKey = stringPreferencesKey("string_key")
        val stringValue = "string value"

        assertTrue { sharedPrefs.edit().putString(stringKey.name, stringValue).commit() }

        val migration = SharedPreferencesMigration(
            context = context,
            sharedPreferencesName = sharedPrefsName
        )

        val preferencesStore = getDataStoreWithMigrations(listOf(migration))

        // Get data so migration is run.
        preferencesStore.data.first()

        assertFalse(sharedPrefs.contains(stringKey.name))
    }

    @Test
    fun supportsStringKey() = runBlockingTest {
        val stringKey = stringPreferencesKey("string_key")
        val stringValue = "string_value"

        assertTrue { sharedPrefs.edit().putString(stringKey.name, stringValue).commit() }

        val migration = SharedPreferencesMigration(
            context = context,
            sharedPreferencesName = sharedPrefsName
        )

        val preferencesStore = getDataStoreWithMigrations(listOf(migration))
        val prefs = preferencesStore.data.first()
        assertEquals(stringValue, prefs[stringKey])
        assertEquals(prefs.asMap().size, 1)
    }

    @Test
    fun supportsIntegerKey() = runBlockingTest {
        val integerKey = intPreferencesKey("integer_key")
        val integerValue = 123

        assertTrue { sharedPrefs.edit().putInt(integerKey.name, integerValue).commit() }

        val migration = SharedPreferencesMigration(
            context = context,
            sharedPreferencesName = sharedPrefsName
        )

        val preferencesStore = getDataStoreWithMigrations(listOf(migration))
        val prefs = preferencesStore.data.first()
        assertEquals(integerValue, prefs[integerKey])
        assertEquals(1, prefs.asMap().size)
    }

    @Test
    fun supportsFloatKey() = runBlockingTest {
        val floatKey = floatPreferencesKey("float_key")
        val floatValue = 123.0f

        assertTrue { sharedPrefs.edit().putFloat(floatKey.name, floatValue).commit() }

        val migration = SharedPreferencesMigration(
            context = context,
            sharedPreferencesName = sharedPrefsName
        )

        val preferencesStore = getDataStoreWithMigrations(listOf(migration))
        val prefs = preferencesStore.data.first()
        assertEquals(floatValue, prefs[floatKey])
        assertEquals(1, prefs.asMap().size)
    }

    @Test
    fun supportsBooleanKey() = runBlockingTest {
        val booleanKey = booleanPreferencesKey("boolean_key")
        val booleanValue = true

        assertTrue { sharedPrefs.edit().putBoolean(booleanKey.name, booleanValue).commit() }

        val migration = SharedPreferencesMigration(
            context = context,
            sharedPreferencesName = sharedPrefsName
        )

        val preferencesStore = getDataStoreWithMigrations(listOf(migration))
        val prefs = preferencesStore.data.first()
        assertEquals(booleanValue, prefs[booleanKey])
        assertEquals(1, prefs.asMap().size)
    }

    @Test
    fun supportsLongKey() = runBlockingTest {
        val longKey = longPreferencesKey("long_key")
        val longValue = 1L shr 50

        assertTrue { sharedPrefs.edit().putLong(longKey.name, longValue).commit() }

        val migration = SharedPreferencesMigration(
            context = context,
            sharedPreferencesName = sharedPrefsName
        )

        val preferencesStore = getDataStoreWithMigrations(listOf(migration))
        val prefs = preferencesStore.data.first()
        assertEquals(longValue, prefs[longKey])
        assertEquals(1, prefs.asMap().size)
    }

    @Test
    fun supportsStringSetKey() = runBlockingTest {
        val stringSetKey =
            androidx.datastore.preferences.core.stringSetPreferencesKey("stringSet_key")
        val stringSetValue = setOf("a", "b", "c")

        assertTrue { sharedPrefs.edit().putStringSet(stringSetKey.name, stringSetValue).commit() }

        val migration = SharedPreferencesMigration(
            context = context,
            sharedPreferencesName = sharedPrefsName
        )

        val preferencesStore = getDataStoreWithMigrations(listOf(migration))
        val prefs = preferencesStore.data.first()
        assertEquals(stringSetValue, prefs[stringSetKey])
        assertEquals(1, prefs.asMap().size)
    }

    @Test
    fun migratedStringSetNotMutable() = runBlockingTest {
        val stringSetKey =
            androidx.datastore.preferences.core.stringSetPreferencesKey("stringSet_key")

        val originalStringSetValue = setOf("a", "b", "c")
        val stringSetValue = originalStringSetValue.toSet()

        assertTrue { sharedPrefs.edit().putStringSet(stringSetKey.name, stringSetValue).commit() }
        val sharedPrefsSet = sharedPrefs.getStringSet(stringSetKey.name, mutableSetOf())!!
        assertEquals(stringSetValue, sharedPrefsSet)

        val migration = SharedPreferencesMigration(
            context = context,
            sharedPreferencesName = sharedPrefsName
        )

        val preferencesStore = getDataStoreWithMigrations(listOf(migration))

        val prefs = preferencesStore.data.first()

        // Modify the sharedPrefs string set:
        sharedPrefsSet.add("d")

        assertEquals(originalStringSetValue, prefs[stringSetKey])
        assertEquals(1, prefs.asMap().size)
    }

    @Test
    fun sharedPreferencesFileDeletedIfPrefsEmpty() = runBlockingTest {
        val integerKey = intPreferencesKey("integer_key")

        assertTrue { sharedPrefs.edit().putInt(integerKey.name, 123).commit() }

        val migration = SharedPreferencesMigration(
            context = context,
            sharedPreferencesName = sharedPrefsName,
            keysToMigrate = setOf(integerKey.name),
        )

        val preferenceStore = getDataStoreWithMigrations(listOf(migration))
        preferenceStore.data.first()

        assertFalse(getSharedPrefsFile(context, sharedPrefsName).exists())
    }

    @Test
    fun sharedPreferencesFileNotDeletedIfPrefsNotEmpty() = runBlockingTest {
        val integerKey1 = intPreferencesKey("integer_key1")
        val integerKey2 = intPreferencesKey("integer_key2")

        assertTrue {
            sharedPrefs.edit().putInt(integerKey1.name, 123).putInt(integerKey2.name, 123).commit()
        }

        val migration = SharedPreferencesMigration(
            context = context,
            sharedPreferencesName = sharedPrefsName,
            keysToMigrate = setOf(integerKey1.name),
        )

        val preferenceStore = getDataStoreWithMigrations(listOf(migration))
        preferenceStore.data.first()

        assertTrue { getSharedPrefsFile(context, sharedPrefsName).exists() }
    }

    @Test
    fun sharedPreferencesBackupFileDeleted() = runBlockingTest {
        val integerKey = intPreferencesKey("integer_key")

        // Write to shared preferences then create the backup file
        val sharedPrefsFile = getSharedPrefsFile(context, sharedPrefsName)
        val sharedPrefsBackupFile = getSharedPrefsBackup(sharedPrefsFile)
        assertTrue { sharedPrefs.edit().putInt(integerKey.name, 123).commit() }
        assertTrue { sharedPrefsFile.exists() }
        assertTrue { sharedPrefsFile.renameTo(sharedPrefsBackupFile) }

        val migration = SharedPreferencesMigration(
            context = context,
            sharedPreferencesName = sharedPrefsName,
            keysToMigrate = setOf(integerKey.name),
        )

        val preferenceStore = getDataStoreWithMigrations(listOf(migration))
        preferenceStore.data.first()

        assertFalse(sharedPrefsBackupFile.exists())
    }

    @Test
    fun canSpecifyMultipleKeys() = runBlockingTest {
        val stringKey = stringPreferencesKey("string_key")
        val integerKey = intPreferencesKey("integer_key")
        val keyNotMigrated = "dont_migrate_this_key"

        val stringValue = "string_value"
        val intValue = 12345
        val notMigratedString = "dont migrate this string"

        assertTrue {
            sharedPrefs.edit()
                .putString(stringKey.name, stringValue)
                .putInt(integerKey.name, intValue)
                .putString(keyNotMigrated, notMigratedString)
                .commit()
        }

        val migration = SharedPreferencesMigration(
            context = context,
            sharedPreferencesName = sharedPrefsName,
            keysToMigrate = setOf(stringKey.name, integerKey.name)
        )

        val preferencesStore = getDataStoreWithMigrations(listOf(migration))

        val prefs = preferencesStore.data.first()

        assertEquals(stringValue, prefs[stringKey])
        assertEquals(intValue, prefs[integerKey])

        assertEquals(2, prefs.asMap().size)
        assertEquals(notMigratedString, sharedPrefs.getString(keyNotMigrated, ""))
    }

    @Test
    fun missingSpecifiedKeyIsNotMigrated() = runBlockingTest {
        val missingKey = intPreferencesKey("missing_key")

        val migration = SharedPreferencesMigration(
            context = context,
            sharedPreferencesName = sharedPrefsName,
            keysToMigrate = setOf(missingKey.name)
        )

        val preferencesStore = getDataStoreWithMigrations(listOf(migration))

        val prefs = preferencesStore.data.first()
        assertFalse(prefs.contains(missingKey))
        assertEquals(0, prefs.asMap().size)
    }

    @Test
    fun runsIfAnySpecifiedKeyExists() = runBlockingTest {
        val integerKey = intPreferencesKey("integer_key")
        val missingKey = intPreferencesKey("missing_key")

        val integerValue = 123

        assertTrue { sharedPrefs.edit().putInt(integerKey.name, integerValue).commit() }

        val migration = SharedPreferencesMigration(
            context = context,
            sharedPreferencesName = sharedPrefsName,
            keysToMigrate = setOf(integerKey.name, missingKey.name)
        )

        val preferencesStore = getDataStoreWithMigrations(listOf(migration))
        val prefs = preferencesStore.data.first()
        assertEquals(integerValue, prefs[integerKey])
        assertEquals(1, prefs.asMap().size)
    }

    @Test
    fun noKeysSpecifiedMigratesNoKeys() = runBlockingTest {
        assertTrue { sharedPrefs.edit().putInt("some_key", 123).commit() }

        val migration = SharedPreferencesMigration(
            context = context,
            sharedPreferencesName = sharedPrefsName,
            keysToMigrate = setOf()
        )

        val preferencesStore = getDataStoreWithMigrations(listOf(migration))
        val prefs = preferencesStore.data.first()
        assertEquals(0, prefs.asMap().size)
    }

    @Test
    fun producedSharedPreferencesIsUsed() = runBlockingTest {
        val integerKey = intPreferencesKey("integer_key")
        val integerValue = 123

        assertTrue { sharedPrefs.edit().putInt(integerKey.name, integerValue).commit() }

        val migration = SharedPreferencesMigration(produceSharedPreferences = { sharedPrefs })

        val preferencesStore = getDataStoreWithMigrations(listOf(migration))
        val prefs = preferencesStore.data.first()
        assertEquals(integerValue, prefs[integerKey])
        assertEquals(1, prefs.asMap().size)
    }

    @Test
    fun testWithTopLevelDataStoreDelegate() = runBlocking<Unit> {
        File(context.filesDir, "/datastore").deleteRecursively()
        assertTrue { sharedPrefs.edit().putInt("integer_key", 123).commit() }

        assertEquals(
            123,
            context.dsWithSpMigration.data.first()[intPreferencesKey("integer_key")]
        )
    }

    private fun getDataStoreWithMigrations(
        migrations: List<DataMigration<Preferences>>
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            migrations = migrations,
            scope = TestCoroutineScope()
        ) { datastoreFile }
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