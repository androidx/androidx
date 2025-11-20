/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.compose.testapp.lifecycle

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

object LifecycleDataStore {
    private val Context.lifecycleDataStore: DataStore<Preferences> by
        preferencesDataStore(name = "lifecycle_data")

    private fun lifecycleEventsKey(activityName: String) =
        stringPreferencesKey("${activityName}_lifecycle_events")

    fun addLifecycleEvent(context: Context, event: String) {
        val key = lifecycleEventsKey(context.javaClass.simpleName)
        runBlocking {
            context.lifecycleDataStore.edit { preferences ->
                val currentEventsString = preferences[key] ?: ""
                val currentEventsList =
                    if (currentEventsString.isNotEmpty()) {
                        currentEventsString.split(",").toMutableList()
                    } else {
                        mutableListOf()
                    }
                currentEventsList.add(event)
                preferences[key] = currentEventsList.joinToString(",")
            }
        }
    }

    fun readLifecycleEvents(context: Context, activityName: String? = null): List<String> {
        val name = activityName ?: context.javaClass.simpleName
        val key = lifecycleEventsKey(name)
        return runBlocking {
            context.lifecycleDataStore.data
                .map { preferences ->
                    val eventsString = preferences[key] ?: ""
                    if (eventsString.isNotEmpty()) {
                        eventsString.split(",")
                    } else {
                        emptyList()
                    }
                }
                .first()
        }
    }

    fun clearAllData(context: Context) {
        runBlocking { context.lifecycleDataStore.edit { preferences -> preferences.clear() } }
    }
}
