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
import androidx.datastore.migrations.SharedPreferencesView
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey

/**
 * Creates a SharedPreferencesMigration for DataStore<Preferences>.
 *
 * Note: This migration only supports the basic SharedPreferences types: boolean, float, int,
 * long, string and string set. If the result of getAll contains other types, they will be ignored.
 *
 * @param produceSharedPreferences Should return the instance of SharedPreferences to migrate from.
 * @param keysToMigrate The list of keys to migrate. The keys will be mapped to
 * datastore.Preferences with their same values. If the key is already present in the new
 * Preferences, the key will not be migrated again. If the key is not present in the
 * SharedPreferences it will not be migrated. If keysToMigrate is not set, all keys will be
 * migrated from the existing SharedPreferences.
 */
@JvmOverloads // Generate methods for default params for java users.
public fun SharedPreferencesMigration(
    produceSharedPreferences: () -> SharedPreferences,
    keysToMigrate: Set<String> = MIGRATE_ALL_KEYS
): SharedPreferencesMigration<Preferences> =
    if (keysToMigrate === MIGRATE_ALL_KEYS) {
        SharedPreferencesMigration(
            produceSharedPreferences = produceSharedPreferences,
            shouldRunMigration = getShouldRunMigration(keysToMigrate),
            migrate = getMigrationFunction(),
        )
    } else {
        SharedPreferencesMigration(
            produceSharedPreferences = produceSharedPreferences,
            keysToMigrate = keysToMigrate,
            shouldRunMigration = getShouldRunMigration(keysToMigrate),
            migrate = getMigrationFunction(),
        )
    }

/**
 * Creates a SharedPreferencesMigration for DataStore<Preferences>.
 *
 * If the SharedPreferences is empty once the migration completes, this migration will attempt to
 * delete it.
 *
 * @param context Context used for getting SharedPreferences.
 * @param sharedPreferencesName The name of the SharedPreferences.
 * @param keysToMigrate The list of keys to migrate. The keys will be mapped to
 * datastore.Preferences with their same values. If the key is already present in the new
 * Preferences, the key will not be migrated again. If the key is not present in the
 * SharedPreferences it will not be migrated. If keysToMigrate is not set, all keys will be
 * migrated from the existing SharedPreferences.
 */
@JvmOverloads // Generate methods for default params for java users.
public fun SharedPreferencesMigration(
    context: Context,
    sharedPreferencesName: String,
    keysToMigrate: Set<String> = MIGRATE_ALL_KEYS,
): SharedPreferencesMigration<Preferences> =
    if (keysToMigrate === MIGRATE_ALL_KEYS) {
        SharedPreferencesMigration(
            context = context,
            sharedPreferencesName = sharedPreferencesName,
            shouldRunMigration = getShouldRunMigration(keysToMigrate),
            migrate = getMigrationFunction()
        )
    } else {
        SharedPreferencesMigration(
            context = context,
            sharedPreferencesName = sharedPreferencesName,
            keysToMigrate = keysToMigrate,
            shouldRunMigration = getShouldRunMigration(keysToMigrate),
            migrate = getMigrationFunction()
        )
    }

private fun getMigrationFunction(): suspend (SharedPreferencesView, Preferences) -> Preferences =
    { sharedPrefs: SharedPreferencesView, currentData: Preferences ->
        // prefs.getAll is already filtered to our key set, but we don't want to overwrite
        // already existing keys.
        val currentKeys = currentData.asMap().keys.map { it.name }

        val filteredSharedPreferences =
            sharedPrefs.getAll().filter { (key, _) -> key !in currentKeys }

        val mutablePreferences = currentData.toMutablePreferences()
        for ((key, value) in filteredSharedPreferences) {
            when (value) {
                is Boolean -> mutablePreferences[
                    booleanPreferencesKey(key)
                ] = value
                is Float -> mutablePreferences[
                    floatPreferencesKey(key)
                ] = value
                is Int -> mutablePreferences[
                    intPreferencesKey(key)
                ] = value
                is Long -> mutablePreferences[
                    longPreferencesKey(key)
                ] = value
                is String -> mutablePreferences[
                    stringPreferencesKey(key)
                ] = value
                is Set<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    mutablePreferences[
                        stringSetPreferencesKey(key)
                    ] = value as Set<String>
                }
            }
        }

        mutablePreferences.toPreferences()
    }

private fun getShouldRunMigration(keysToMigrate: Set<String>): suspend (Preferences) -> Boolean =
    { prefs ->
        // If any key hasn't been migrated to currentData, we can't skip the migration. If
        // the key set is not specified, we can't skip the migration.
        val allKeys = prefs.asMap().keys.map { it.name }

        if (keysToMigrate === MIGRATE_ALL_KEYS) {
            true
        } else {
            keysToMigrate.any { it !in allKeys }
        }
    }

internal val MIGRATE_ALL_KEYS: Set<String> = mutableSetOf()
