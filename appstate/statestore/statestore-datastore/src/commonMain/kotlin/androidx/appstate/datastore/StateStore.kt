/*
 * Copyright 2026 The Android Open Source Project
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

@file:JvmName("StateStoreKt")
@file:JvmMultifileClass

package androidx.appstate.datastore

import androidx.appstate.statestore.StateStore
import androidx.appstate.statestore.StateStoreKey
import androidx.appstate.transform.listener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.datastore.core.DataStore
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer

/**
 * Tracks changes to [StateStore] and syncs keys implementing [PersistToDataStore] to [DataStore].
 *
 * Keys must implement [PersistToDataStore] to be saved.
 *
 * @sample androidx.appstate.datastore.samples.SyncStateStoreDataStoreSample
 * @param dataStore the [DataStore] used to save and restore state
 */
@Suppress("PairedRegistration")
public suspend fun StateStore.syncToDataStore(dataStore: DataStore<StateStorePreferences>) {
    listener {
        val activeKeys = keys
        for (activeKey in activeKeys) {
            if (activeKey is PersistToDataStore<*>) {
                // Each key owns its own restore/save state, independent of iteration order.
                key(activeKey) {
                    @Suppress("UNCHECKED_CAST")
                    SyncKey(
                        key = activeKey as StateStoreKey<Any?>,
                        name = activeKey.name,
                        serializer = (activeKey as PersistToDataStore<Any?>).serializer,
                        dataStore = dataStore,
                    )
                }
            }
        }
    }
}

/**
 * Restores [key] from [dataStore] once, then writes every subsequent change of its value.
 *
 * Runs inside the [listener] composition: reading the key's state here is what re-runs the save
 * effect when the value changes.
 */
@Composable
private fun <T> StateStore.SyncKey(
    key: StateStoreKey<T>,
    name: String,
    serializer: KSerializer<T>,
    dataStore: DataStore<StateStorePreferences>,
) {
    val restored = remember { mutableStateOf(false) }
    // The last value read from or written to DataStore, so restoring does not write it back.
    val lastSynced = remember { mutableStateOf<T?>(null) }
    val value = getState(key).value

    // Keyed on Unit because we only want to restore the persisted value once, when this key
    // enters composition. Later changes are saved by the effect below.
    LaunchedEffect(Unit) {
        val valueJson = dataStore.data.first().asMap()[name]
        if (valueJson != null) {
            val decoded = decodeValue(serializer, valueJson, name)
            setState(key, decoded)
            lastSynced.value = decoded
        }
        restored.value = true
    }

    LaunchedEffect(value, restored.value) {
        if (restored.value && value != null && value != lastSynced.value) {
            withContext(ioDispatcher) {
                dataStore.edit { settings ->
                    settings.preferencesMap[name] = encodeValue(serializer, value, name)
                    lastSynced.value = value
                }
            }
        }
    }
}
