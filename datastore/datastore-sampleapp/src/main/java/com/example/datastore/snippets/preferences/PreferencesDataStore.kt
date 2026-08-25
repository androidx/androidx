/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.datastore.snippets.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// [START android_datastore_preferences_create]
// At the top level of your kotlin file:
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// [END android_datastore_preferences_create]

class PreferencesDataStore(private val context: Context) {

    // [START android_datastore_preferences_read]
    fun counterFlow(): Flow<Int> =
        context.dataStore.data.map { preferences -> preferences[EXAMPLE_COUNTER] ?: 0 }

    // [END android_datastore_preferences_read]

    // [START android_datastore_preferences_write]
    suspend fun incrementCounter() {
        context.dataStore.updateData {
            it.toMutablePreferences().also { preferences ->
                preferences[EXAMPLE_COUNTER] = (preferences[EXAMPLE_COUNTER] ?: 0) + 1
            }
        }
    }

    // [END android_datastore_preferences_write]

    /**
     * Write example with helper function that is similar to
     * [SharedPreferences.edit](https://developer.android.com/reference/android/content/SharedPreferences#edit())
     */
    // [START android_datastore_preferences_write_edit]
    suspend fun incrementCounterWithEdit() {
        context.dataStore.edit { preferences ->
            preferences[EXAMPLE_COUNTER] = (preferences[EXAMPLE_COUNTER] ?: 0) + 1
        }
    }

    // [END android_datastore_preferences_write_edit]

    companion object {
        // [START android_datastore_preferences_key]
        val EXAMPLE_COUNTER = intPreferencesKey("example_counter")
        // [END android_datastore_preferences_key]
    }
}
