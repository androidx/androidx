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
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioSerializer
import kotlin.reflect.typeOf
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okio.BufferedSink
import okio.BufferedSource

/**
 * Marks an [StateStoreKey] to be persisted to [DataStore].
 *
 * Keys annotated with this will have their state automatically saved and restored.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
public annotation class PersistToDataStore

/**
 * Preferences map for [StateStore] backed by [DataStore].
 *
 * This is the type required for using [DataStore] with [StateStore].
 *
 * [StateStoreSerializer] serializes this preferences map by converting its internal map of
 * [StateStoreKey] names to JSON-encoded values into a single JSON string, and writing it to a UTF-8
 * file via Okio.
 */
public abstract class StateStorePreferences internal constructor() {
    /**
     * Returns an immutable map of the preferences.
     *
     * @return map of key names to serialized string values
     */
    internal abstract fun asMap(): Map<String, String>

    /**
     * Gets a typed value for the given [StateStoreKey].
     *
     * @param key the [StateStoreKey] to retrieve
     * @return the deserialized value, or null if the key is not set.
     * @throws CorruptionException if the key cannot be deserialized.
     */
    internal inline operator fun <reified T : Any> get(key: StateStoreKey<T>): T? {
        val keyName = key::class.qualifiedName ?: return null
        val valueJson = asMap()[keyName] ?: return null
        return try {
            val serializer = serializer(typeOf<T>())
            Json.decodeFromString(serializer, valueJson) as T
        } catch (e: SerializationException) {
            throw CorruptionException("Unable to deserialize JSON from String.", e)
        } catch (e: Exception) {
            throw CorruptionException("Unexpected error restoring state for key: $keyName", e)
        }
    }

    /**
     * Returns a mutable copy of the preferences.
     *
     * @return mutable preferences
     */
    internal fun toMutablePreferences(): MutableStateStorePreferences {
        return MutableStateStorePreferences(asMap().toMutableMap())
    }
}

/** Mutable version of [StateStorePreferences]. */
internal class MutableStateStorePreferences
internal constructor(
    @PublishedApi internal val preferencesMap: MutableMap<String, String> = mutableMapOf()
) : StateStorePreferences() {

    override fun asMap(): Map<String, String> {
        return preferencesMap.toMap()
    }

    /**
     * Sets a typed value for the given [StateStoreKey].
     *
     * @param key the [StateStoreKey] to set
     * @param value the value to set for the key
     * @throws CorruptionException if the key cannot be serialized.
     */
    internal inline operator fun <reified T : Any> set(key: StateStoreKey<T>, value: T) {
        val keyName = key::class.qualifiedName ?: return
        try {
            val serializer = serializer(typeOf<T>())
            preferencesMap[keyName] = Json.encodeToString(serializer, value)
        } catch (e: SerializationException) {
            throw CorruptionException("Unable to serialize String to JSON.", e)
        } catch (e: Exception) {
            throw CorruptionException("Unexpected error saving state for key: $keyName", e)
        }
    }
}

/**
 * Edits the [StateStorePreferences] in [DataStore] transactionally.
 *
 * @param transform block to mutate the [MutableStateStorePreferences]
 * @return the updated [StateStorePreferences]
 */
internal suspend fun DataStore<StateStorePreferences>.edit(
    transform: suspend (MutableStateStorePreferences) -> Unit
): StateStorePreferences {
    return this.updateData { current -> current.toMutablePreferences().apply { transform(this) } }
}

/**
 * Serializer for [StateStorePreferences] using [DataStore] and Okio.
 *
 * Serializes the preferences to a JSON string backed by a UTF-8 file.
 */
public object StateStoreSerializer : OkioSerializer<StateStorePreferences> {
    override val defaultValue: StateStorePreferences = MutableStateStorePreferences()

    override suspend fun readFrom(source: BufferedSource): StateStorePreferences {
        try {
            val string = source.readUtf8()
            if (string.isEmpty()) {
                return defaultValue
            }
            val map = Json.decodeFromString<Map<String, String>>(string)
            return MutableStateStorePreferences(map.toMutableMap())
        } catch (e: SerializationException) {
            throw CorruptionException("Unable to deserialize JSON from String.", e)
        } catch (e: Exception) {
            throw CorruptionException("Unexpected error reading state from DataStore", e)
        }
    }

    override suspend fun writeTo(t: StateStorePreferences, sink: BufferedSink) {
        try {
            val string = Json.encodeToString<Map<String, String>>(t.asMap())
            sink.writeUtf8(string)
        } catch (e: SerializationException) {
            throw CorruptionException("Unable to serialize String to JSON.", e)
        }
    }
}
