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

import androidx.appstate.statestore.StateStore
import androidx.appstate.statestore.StateStoreKey
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource

/**
 * Marks a [StateStoreKey] to be persisted to [DataStore].
 *
 * Keys implementing this interface will have their state automatically saved and restored if the
 * [StateStore] calls the [syncToDataStore] function. The key provides the [name] its value is
 * stored under and the [KSerializer] for the value:
 * ```
 * @Serializable
 * object SettingsKey : StateStoreKey<Settings>(Settings()), PersistToDataStore<Settings> {
 *     override val name = "settings"
 *     override val serializer = Settings.serializer()
 * }
 * ```
 *
 * @param T the type of the value held by the implementing [StateStoreKey]
 */
public interface PersistToDataStore<T> {
    /**
     * The name the value is stored under.
     *
     * It must be unique among the keys synced to the same [DataStore] and must not change between
     * versions of the app, since persisted data is looked up by it.
     */
    public val name: String

    /** The serializer used to save and restore the value of the implementing [StateStoreKey]. */
    public val serializer: KSerializer<T>
}

/** Encodes [value] with the key's [serializer], mapping failures to [CorruptionException]. */
internal fun <T> encodeValue(serializer: KSerializer<T>, value: T, name: String): String =
    try {
        Json.encodeToString(serializer, value)
    } catch (e: SerializationException) {
        throw CorruptionException("Unable to serialize the value of $name to JSON.", e)
    } catch (e: Exception) {
        throw CorruptionException("Unexpected error saving state for key: $name", e)
    }

/** Decodes [json] with the key's [serializer], mapping failures to [CorruptionException]. */
internal fun <T> decodeValue(serializer: KSerializer<T>, json: String, name: String): T =
    try {
        Json.decodeFromString(serializer, json)
    } catch (e: SerializationException) {
        throw CorruptionException("Unable to deserialize the value of $name from JSON.", e)
    } catch (e: Exception) {
        throw CorruptionException("Unexpected error restoring state for key: $name", e)
    }

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
    internal operator fun <T, K> get(key: K): T?
        where K : StateStoreKey<T>, K : PersistToDataStore<T> {
        val valueJson = asMap()[key.name] ?: return null
        return decodeValue(key.serializer, valueJson, key.name)
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
internal constructor(internal val preferencesMap: MutableMap<String, String> = mutableMapOf()) :
    StateStorePreferences() {

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
    internal operator fun <T, K> set(key: K, value: T)
        where K : StateStoreKey<T>, K : PersistToDataStore<T> {
        preferencesMap[key.name] = encodeValue(key.serializer, value, key.name)
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
