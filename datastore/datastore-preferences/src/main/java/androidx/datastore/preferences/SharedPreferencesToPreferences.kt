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
import androidx.datastore.migrations.MigrationFromSharedPreferences
import androidx.datastore.migrations.SharedPreferencesView
import androidx.datastore.migrations.SharedPreferencesMigration

/**
 * Creates a SharedPreferencesMigration for DataStore<Preferences>.
 *
 * @param context Context used for getting SharedPreferences.
 * @param sharedPreferencesName The name of the SharedPreferences.
 * @param keysToMigrate The list of keys to migrate. The keys will be mapped to datastore.Preferences with
 * their same values. If the key is already present in the new Preferences, the key
 * will not be migrated again. If the key is not present in the SharedPreferences it
 * will not be migrated. If keysToMigrate is not set, all keys will be migrated from the existing
 * SharedPreferences.
 * @param deleteEmptyPreferences If enabled and the SharedPreferences are empty (i.e. no remaining
 * keys) after this migration runs, the leftover SharedPreferences file is deleted. Note that
 * this cleanup runs only if the migration itself runs, i.e., if the keys were never in
 * SharedPreferences to begin with then the (potentially) empty SharedPreferences
 * won't be cleaned up by this option. This functionality is best effort - if there
 * is an issue deleting the SharedPreferences file it will be silently ignored.
 */
fun SharedPreferencesMigration(
    context: Context,
    sharedPreferencesName: String,
    keysToMigrate: Set<String>? = SharedPreferencesToPreferences.MIGRATE_ALL_KEYS,
    deleteEmptyPreferences: Boolean = true
): () -> DataMigration<Preferences> {
    return SharedPreferencesMigration(
        context,
        sharedPreferencesName,
        SharedPreferencesToPreferences(keysToMigrate),
        keysToMigrate,
        deleteEmptyPreferences
    )
}

/**
 * A DataMigration which migrates SharedPreferences to DataStore.
 *
 * Note: this accesses the SharedPreferences using MODE_PRIVATE.
 */
internal class SharedPreferencesToPreferences(
    private val keysToMigrate: Set<String>?
) : MigrationFromSharedPreferences<Preferences> {

    companion object {
        internal val MIGRATE_ALL_KEYS = null
    }

    override suspend fun shouldMigrate(currentData: Preferences): Boolean {
        if (keysToMigrate == null) {
            // We need to migrate all keys from the SharedPreferences.
            return true
        }

        // If any key hasn't been migrated to currentData, we can't skip the migration.
        return keysToMigrate.any { it !in currentData }
    }

    override suspend fun migrate(
        prefs: SharedPreferencesView,
        currentData: Preferences
    ): Preferences {
        // prefs.getAll is already filtered to our key set.
        val preferencesToMigrate = prefs.getAll().filter { (key, _) -> key !in currentData }

        val preferencesBuilder = currentData.toBuilder()
        for ((key, value) in preferencesToMigrate) {
            when (value) {
                is Boolean -> preferencesBuilder.setBoolean(key, value)
                is Float -> preferencesBuilder.setFloat(key, value)
                is Int -> preferencesBuilder.setInt(key, value)
                is Long -> preferencesBuilder.setLong(key, value)
                is String -> preferencesBuilder.setString(key, value)
                is Set<*> ->
                    @Suppress("UNCHECKED_CAST")
                    preferencesBuilder.setStringSet(key, value.toSet() as Set<String>)
            }
        }

        return preferencesBuilder.build()
    }
}