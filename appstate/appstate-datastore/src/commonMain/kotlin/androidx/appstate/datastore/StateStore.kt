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

package androidx.appstate.datastore

import androidx.appstate.StateStore
import androidx.appstate.StateStoreKey
import androidx.appstate.transform.listener
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.allSupertypes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okio.FileSystem
import okio.Path.Companion.toPath

/**
 * Tracks changes to [StateStore] and persists annotated keys to a [DataStore] backed by a file.
 *
 * Keys must be annotated with [PersistToDataStore] to be saved.
 *
 * @sample androidx.appstate.datastore.samples.SyncStateStoreDataStorePathSample
 * @param path the file path to save and restore state from
 * @param coroutineScope the [CoroutineScope] used for the [DataStore]
 */
@Suppress("PairedRegistration")
public suspend fun StateStore.syncToDataStore(path: String, coroutineScope: CoroutineScope) {
    val dataStore =
        DataStoreFactory.create(
            storage = OkioStorage(FileSystem.SYSTEM, StateStoreSerializer) { path.toPath() },
            scope = coroutineScope,
        )
    syncToDataStore(dataStore)
}

/**
 * Tracks changes to [StateStore] and syncs annotated keys to [DataStore].
 *
 * Keys must be annotated with [PersistToDataStore] to be saved.
 *
 * @sample androidx.appstate.datastore.samples.SyncStateStoreDataStoreSample
 * @param dataStore the [DataStore] used to save and restore state
 */
@Suppress("PairedRegistration")
public suspend fun StateStore.syncToDataStore(dataStore: DataStore<StateStorePreferences>) {
    listener {
        val activeKeys = keys
        for (key in activeKeys) {
            if (key::class.hasAnnotation<PersistToDataStore>()) {
                val restored = remember(key) { mutableStateOf(false) }
                val restoredValue = remember(key) { mutableStateOf<Any?>(null) }
                val state = getUntypedState<Any>(key, null)
                val value = state.value

                LaunchedEffect(key) {
                    val preferences = dataStore.data.first()
                    val keyName =
                        checkNotNull(key::class.qualifiedName) {
                            "Keys annotated with @PersistToDataStore must have a qualified name."
                        }
                    val valueJson = preferences.asMap()[keyName]
                    if (valueJson != null) {
                        try {
                            val valueType = key.getValueType()
                            val serializer = serializer(valueType)
                            val decoded = Json.decodeFromString(serializer, valueJson)
                            @Suppress("UNCHECKED_CAST")
                            this@syncToDataStore.setState(key as StateStoreKey<Any>, decoded as Any)
                            restoredValue.value = decoded
                        } catch (e: SerializationException) {
                            throw CorruptionException("Unable to deserialize JSON from String.", e)
                        } catch (e: Exception) {
                            throw CorruptionException(
                                "Unexpected error restoring state for key: $keyName",
                                e,
                            )
                        }
                    }
                    restored.value = true
                }

                LaunchedEffect(key, value, restored.value) {
                    if (restored.value) {
                        if (value != null && value != restoredValue.value) {
                            val keyName =
                                checkNotNull(key::class.qualifiedName) {
                                    "Keys annotated with @PersistToDataStore must have a qualified name."
                                }
                            withContext(Dispatchers.IO) {
                                dataStore.edit { settings ->
                                    try {
                                        val valueType = key.getValueType()
                                        val serializer = serializer(valueType)
                                        val jsonValue = Json.encodeToString(serializer, value)
                                        settings.preferencesMap[keyName] = jsonValue
                                        restoredValue.value = value
                                    } catch (e: SerializationException) {
                                        throw CorruptionException(
                                            "Unable to serialize String to JSON.",
                                            e,
                                        )
                                    } catch (e: Exception) {
                                        throw CorruptionException(
                                            "Unexpected error saving state for key: $keyName",
                                            e,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun StateStoreKey<*>.getValueType(): KType {
    val stateStoreKeySupertype =
        this::class.allSupertypes.firstOrNull { it.classifier == StateStoreKey::class }
    return stateStoreKeySupertype?.arguments?.firstOrNull()?.type
        ?: error("Could not find StateStoreKey supertype for ${this::class}")
}

private inline fun <reified T : Annotation> KClass<*>.hasAnnotation(): Boolean {
    for (i in this.annotations.indices) {
        if (this.annotations[i] is T) return true
    }
    return false
}

@Suppress("UNCHECKED_CAST")
private fun <T> StateStore.getUntypedState(key: StateStoreKey<*>, defaultValue: T?): State<T?> {
    return this.getState(key as StateStoreKey<T?>, defaultValue)
}
