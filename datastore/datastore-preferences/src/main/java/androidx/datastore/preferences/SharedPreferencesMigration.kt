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
import androidx.datastore.migrations.SharedPreferencesView
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.toMutablePreferences
import androidx.datastore.preferences.core.toPreferences

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
 *
 * TODO(rohitsat): determine whether to remove the deleteEmptyPreferences option.
 */
@JvmOverloads // Generate methods for default params for java users.
public fun SharedPreferencesMigration(
    context: Context,
    sharedPreferencesName: String,
    keysToMigrate: Set<String>? = MIGRATE_ALL_KEYS,
    deleteEmptyPreferences: Boolean = true
): SharedPreferencesMigration<Preferences> {
    return SharedPreferencesMigration(
        context = context,
        sharedPreferencesName = sharedPreferencesName,
        keysToMigrate = keysToMigrate,
        deleteEmptyPreferences = deleteEmptyPreferences,
        shouldRunMigration = { prefs ->
            // If any key hasn't been migrated to currentData, we can't skip the migration. If
            // the key set is not specified, we can't skip the migration.
            val allKeys = prefs.asMap().keys.map { it.name }
            keysToMigrate?.any { it !in allKeys } ?: true
        },
        migrate = { sharedPrefs: SharedPreferencesView, currentData: Preferences ->
            // prefs.getAll is already filtered to our key set, but we don't want to overwrite
            // already existing keys.
            val currentKeys = currentData.asMap().keys.map { it.name }

            val filteredSharedPreferences =
                sharedPrefs.getAll().filter { (key, _) -> key !in currentKeys }

            val mutablePreferences = currentData.toMutablePreferences()
            for ((key, value) in filteredSharedPreferences) {
                when (value) {
                    is Boolean -> mutablePreferences[
                        androidx.datastore.preferences.core.preferencesKey(
                            key
                        )
                    ] = value
                    is Float -> mutablePreferences[
                        androidx.datastore.preferences.core.preferencesKey(
                            key
                        )
                    ] = value
                    is Int -> mutablePreferences[
                        androidx.datastore.preferences.core.preferencesKey(
                            key
                        )
                    ] = value
                    is Long -> mutablePreferences[
                        androidx.datastore.preferences.core.preferencesKey(
                            key
                        )
                    ] = value
                    is String -> mutablePreferences[
                        androidx.datastore.preferences.core.preferencesKey(
                            key
                        )
                    ] = value
                    is Set<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        mutablePreferences[
                            androidx.datastore.preferences.core.preferencesSetKey<String>(
                                key
                            )
                        ] = value as Set<String>
                    }
                }
            }

            mutablePreferences.toPreferences()
        }
    )
}

internal val MIGRATE_ALL_KEYS = null