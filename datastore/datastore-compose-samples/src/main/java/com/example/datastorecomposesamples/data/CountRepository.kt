/*
 * Copyright 2022 The Android Open Source Project
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

package com.example.datastorecomposesamples.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.datastorecomposesamples.CountPreferences
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

data class CountState(val count: Int)

private const val COUNT_STATE_NAME = "count_state"

private val Context.preferencesDataStore by preferencesDataStore(
    name = COUNT_STATE_NAME
)

/**
 * Repository class for managing the DataStores.
 */
class CountRepository private constructor(
    private val dataStore: DataStore<Preferences>,
    private val protoDataStore: DataStore<CountPreferences>
) {

    companion object {
        private val PROTO_STORE_FILE_NAME = "datastore_compose_test_app.pb"
        private var instance: CountRepository? = null

        fun getInstance(context: Context): CountRepository {
            instance?.let { return it }
            synchronized(this) {
                instance?.let { return it }
                val protoDataStore =
                    DataStoreFactory.create(serializer = CountSerializer) {
                        File(context.filesDir, PROTO_STORE_FILE_NAME)
                    }
                return CountRepository(context.preferencesDataStore, protoDataStore).also {
                    instance = it
                }
            }
        }
    }

    private val TAG: String = "CountStateRepo"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private object PreferencesKeys {
        val COUNT = intPreferencesKey("count")
    }

    val countStateFlow: Flow<CountState> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading preferences.", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            CountState(preferences[PreferencesKeys.COUNT] ?: 0)
        }

    val countProtoStateFlow: Flow<CountState> = protoDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e(TAG, "Error reading proto.", exception)
                emit(CountPreferences.getDefaultInstance())
            } else {
                throw exception
            }
        }.map { proto ->
            CountState(proto.count)
        }

    fun incrementPreferenceCount() {
        scope.launch {
            dataStore.edit { preferences ->
                val count = preferences[PreferencesKeys.COUNT] ?: 0
                preferences[PreferencesKeys.COUNT] = count + 1
            }
        }
    }

    fun decrementPreferenceCount() {
        scope.launch {
            dataStore.edit { preferences ->
                val count = preferences[PreferencesKeys.COUNT] ?: 0
                preferences[PreferencesKeys.COUNT] = count - 1
            }
        }
    }

    fun incrementProtoCount() {
        scope.launch {
            protoDataStore.updateData { preferences ->
                preferences.toBuilder().setCount(preferences.count + 1).build()
            }
        }
    }

    fun decrementProtoCount() {
        scope.launch {
            protoDataStore.updateData { preferences ->
                preferences.toBuilder().setCount(preferences.count - 1).build()
            }
        }
    }
}