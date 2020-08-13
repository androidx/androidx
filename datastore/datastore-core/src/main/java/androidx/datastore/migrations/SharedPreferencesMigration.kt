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

@file:JvmName("SharedPreferencesMigration")

package androidx.datastore.migrations

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.DataMigration
import java.io.File
import java.io.IOException

/**
 * A DataMigration which migrates SharedPreferences to DataStore.
 *
 * Note: this accesses the SharedPreferences using MODE_PRIVATE.
 */
internal val MIGRATE_ALL_KEYS = null

/**
 * Returns a factory function which creates a SharedPreferences Data Migration.
 *
 * @param context Context used for getting SharedPreferences.
 * @param sharedPreferencesName The name of the SharedPreferences.
 * @param migration The mapping function for the migration.
 * @param keysToMigrate The list of keys to migrate. The keys will be mapped to datastore
 * .Preferences with their same values. If the key is already present in the new Preferences, the key
 * will not be migrated again. If the key is not present in the SharedPreferences it will not be
 * migrated. If keysToMigrate is null, all keys will be migrated from the existing
 * SharedPreferences.
 * @param deleteEmptyPreferences If enabled and the SharedPreferences are empty (i.e. no remaining
 * keys) after this migration runs, the leftover SharedPreferences file is deleted. Note that
 * this cleanup runs only if the migration itself runs, i.e., if the keys were never in
 * SharedPreferences to begin with then the (potentially) empty SharedPreferences won't be
 * cleaned up by this option. This functionality is best effort - if there is an issue deleting
 * the SharedPreferences file it will be silently ignored.
 */
fun <T> SharedPreferencesMigration(
    context: Context,
    sharedPreferencesName: String,
    migration: MigrationFromSharedPreferences<T>,
    keysToMigrate: Set<String>? = MIGRATE_ALL_KEYS,
    deleteEmptyPreferences: Boolean = true
): () -> DataMigration<T> = {
    SharedPreferencesDataMigration(
        context,
        sharedPreferencesName,
        migration,
        keysToMigrate?.toMutableSet(),
        deleteEmptyPreferences
    )
}

/**
 * User implemented migration interface. Contains logic for mapping SharedPreferences data to T.
 */
interface MigrationFromSharedPreferences<T> {
    /**
     * Optional method that should return false if the migration should be skipped. This can
     * be useful to stop unnecessary calls into SharedPreferences. This can be implemented by
     * including a field in your data that specifies whether your migration has already been
     * run.
     *
     * @param currentData the current data (it might already populated from this or other
     * migrations)
     * @return Whether or not this migration should run.
     */
    suspend fun shouldMigrate(currentData: T): Boolean = true

    /**
     * Perform the migration. Implementations should be idempotent since this may be called
     * multiple times. See {@code DataMigration#migrate} for more information.
     *
     * @param prefs the view of the SharedPreferences to migrate from.
     * @param currentData the current data (it might be populated from other migrations or from
     * manual changes before this migration was added to the app)
     * @return The migrated data.
     */
    suspend fun migrate(prefs: SharedPreferencesView, currentData: T): T
}

/**
 *  Read-only wrapper around SharedPreferences. This will be passed in to your migration. The
 *  constructor is public to enable easier testing of migrations.
 */
class SharedPreferencesView(
    private val prefs: SharedPreferences,
    private val keySet: Set<String>
) {
    /**
     * Checks whether the preferences contains a preference.
     *
     * @param key the name of the preference to check
     * @throws IllegalArgumentException if `key` wasn't specified as part of this migration
     */
    operator fun contains(key: String): Boolean = prefs.contains(checkKey(key))

    /**
     * Retrieves a boolean value from the preferences.
     *
     * @param key the name of the preference to retrieve
     * @param defValue value to return if this preference does not exist
     * @throws IllegalArgumentException if `key` wasn't specified as part of this migration
     */
    fun getBoolean(key: String, defValue: Boolean): Boolean =
        prefs.getBoolean(checkKey(key), defValue)

    /**
     * Retrieves a float value from the preferences.
     *
     * @param key the name of the preference to retrieve
     * @param defValue value to return if this preference does not exist
     * @throws IllegalArgumentException if `key` wasn't specified as part of this migration
     */
    fun getFloat(key: String, defValue: Float): Float = prefs.getFloat(checkKey(key), defValue)

    /**
     * Retrieves a int value from the preferences.
     *
     * @param key the name of the preference to retrieve
     * @param defValue value to return if this preference does not exist
     * @throws IllegalArgumentException if `key` wasn't specified as part of this migration
     */
    fun getInt(key: String, defValue: Int): Int = prefs.getInt(checkKey(key), defValue)

    /**
     * Retrieves a long value from the preferences.
     *
     * @param key the name of the preference to retrieve
     * @param defValue value to return if this preference does not exist
     * @throws IllegalArgumentException if `key` wasn't specified as part of this migration
     */
    fun getLong(key: String, defValue: Long): Long = prefs.getLong(checkKey(key), defValue)

    /**
     * Retrieves a string value from the preferences.
     *
     * @param key the name of the preference to retrieve
     * @param defValue value to return if this preference does not exist
     * @throws IllegalArgumentException if `key` wasn't specified as part of this migration
     */
    fun getString(key: String, defValue: String? = null): String? =
        prefs.getString(checkKey(key), defValue)

    /**
     * Retrieves a string set value from the preferences.
     *
     * @param key the name of the preference to retrieve
     * @param defValues value to return if this preference does not exist
     * @throws IllegalArgumentException if `key` wasn't specified as part of this migration
     */
    fun getStringSet(key: String, defValues: Set<String>? = null): Set<String>? =
        prefs.getStringSet(checkKey(key), defValues)?.toMutableSet()

    /** Retrieve all values from the preferences that are in the specified keySet. */
    fun getAll(): Map<String, Any?> = prefs.all.filter { (key, _) ->
        key in keySet
    }.mapValues { (_, value) ->
        if (value is Set<*>) {
            value.toSet()
        } else {
            value
        }
    }

    private fun checkKey(key: String): String? {
        check(key in keySet) { "Can't access key outside migration: $key" }
        return key
    }
}

private class SharedPreferencesDataMigration<T> internal constructor(
    private val context: Context,
    private val sharedPreferencesName: String,
    private val migration: MigrationFromSharedPreferences<T>,
    keysToMigrate: MutableSet<String>?,
    private val deleteEmptyPreferences: Boolean
) : DataMigration<T> {
    private val sharedPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(sharedPreferencesName, Context.MODE_PRIVATE)
    }

    private val keySet: MutableSet<String> by lazy {
        keysToMigrate ?: sharedPrefs.all.keys.toMutableSet()
    }

    override suspend fun shouldMigrate(currentData: T): Boolean {
        if (!migration.shouldMigrate(currentData)) {
            return false
        }

        return keySet.any(sharedPrefs::contains)
    }

    override suspend fun migrate(currentData: T): T =
        migration.migrate(
            SharedPreferencesView(
                sharedPrefs,
                keySet
            ), currentData
        )

    @Throws(IOException::class)
    override suspend fun cleanUp() {
        val sharedPrefsEditor = sharedPrefs.edit()

        for (key in keySet) {
            sharedPrefsEditor.remove(key)
        }

        if (!sharedPrefsEditor.commit()) {
            throw IOException(
                "Unable to delete migrated keys from SharedPreferences: $sharedPreferencesName"
            )
        }

        if (deleteEmptyPreferences && sharedPrefs.all.isEmpty()) {
            deleteSharedPreferences(context, sharedPreferencesName)
        }

        keySet.clear()
    }

    private fun deleteSharedPreferences(context: Context, name: String) {
        if (android.os.Build.VERSION.SDK_INT >= 24) {
            if (!context.deleteSharedPreferences(name)) {
                throw IOException("Unable to delete SharedPreferences: $name")
            }
            return
        }

        // Context.deleteSharedPreferences is SDK 24+, so we have to reproduce the definition
        val prefsFile = getSharedPrefsFile(context, name)
        val prefsBackup = getSharedPrefsBackup(prefsFile)

        // Silently continue if we aren't able to delete the Shared Preferences File.
        prefsFile.delete()
        prefsBackup.delete()
    }

    // ContextImpl.getSharedPreferencesPath is private, so we have to reproduce the definition
    private fun getSharedPrefsFile(context: Context, name: String): File {
        val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        return File(prefsDir, "$name.xml")
    }

    // SharedPreferencesImpl.makeBackupFile is private, so we have to reproduce the definition
    private fun getSharedPrefsBackup(prefsFile: File) = File(prefsFile.path + ".bak")
}
