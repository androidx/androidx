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

import androidx.appstate.AppState
import androidx.appstate.AppStateKey
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
 * Marks an [AppStateKey] to be persisted to [DataStore].
 *
 * Keys annotated with this will have their state automatically saved and restored.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
public annotation class PersistToDataStore

/** Preferences map for [AppState] backed by [DataStore]. */
public abstract class AppStatePreferences internal constructor() {
    /**
     * Returns an immutable map of the preferences.
     *
     * @return map of key names to serialized string values
     */
    internal abstract fun asMap(): Map<String, String>

    /**
     * Gets a typed value for the given [AppStateKey].
     *
     * @param key the [AppStateKey] to retrieve
     * @return the deserialized value, or null if the key is not set.
     * @throws CorruptionException if the key cannot be deserialized.
     */
    internal inline operator fun <reified T : Any> get(key: AppStateKey<T>): T? {
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
    internal fun toMutablePreferences(): MutableAppStatePreferences {
        return MutableAppStatePreferences(asMap().toMutableMap())
    }
}

/** Mutable version of [AppStatePreferences]. */
internal class MutableAppStatePreferences
internal constructor(
    @PublishedApi internal val preferencesMap: MutableMap<String, String> = mutableMapOf()
) : AppStatePreferences() {

    override fun asMap(): Map<String, String> {
        return preferencesMap.toMap()
    }

    /**
     * Sets a typed value for the given [AppStateKey].
     *
     * @param key the [AppStateKey] to set
     * @param value the value to set for the key
     * @throws CorruptionException if the key cannot be serialized.
     */
    internal inline operator fun <reified T : Any> set(key: AppStateKey<T>, value: T) {
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
 * Edits the [AppStatePreferences] in [DataStore] transactionally.
 *
 * @param transform block to mutate the [MutableAppStatePreferences]
 * @return the updated [AppStatePreferences]
 */
internal suspend fun DataStore<AppStatePreferences>.edit(
    transform: suspend (MutableAppStatePreferences) -> Unit
): AppStatePreferences {
    return this.updateData { current -> current.toMutablePreferences().apply { transform(this) } }
}

/**
 * Serializer for [AppStatePreferences] using [DataStore] and Okio.
 *
 * Serializes the preferences to a JSON string backed by a UTF-8 file.
 */
public object AppStateSerializer : OkioSerializer<AppStatePreferences> {
    override val defaultValue: AppStatePreferences = MutableAppStatePreferences()

    override suspend fun readFrom(source: BufferedSource): AppStatePreferences {
        try {
            val string = source.readUtf8()
            if (string.isEmpty()) {
                return defaultValue
            }
            val map = Json.decodeFromString<Map<String, String>>(string)
            return MutableAppStatePreferences(map.toMutableMap())
        } catch (e: SerializationException) {
            throw CorruptionException("Unable to deserialize JSON from String.", e)
        } catch (e: Exception) {
            throw CorruptionException("Unexpected error reading state from DataStore", e)
        }
    }

    override suspend fun writeTo(t: AppStatePreferences, sink: BufferedSink) {
        try {
            val string = Json.encodeToString<Map<String, String>>(t.asMap())
            sink.writeUtf8(string)
        } catch (e: SerializationException) {
            throw CorruptionException("Unable to serialize String to JSON.", e)
        }
    }
}
