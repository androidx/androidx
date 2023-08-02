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

package androidx.datastore.preferences.core

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.okio.OkioSerializer
import okio.BufferedSink
import okio.BufferedSource
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException

@OptIn(ExperimentalSerializationApi::class)
internal object PreferencesSerializationSerializer : OkioSerializer<Preferences> {
    val fileExtension = "preferences_pb"

    override val defaultValue: Preferences
        get() = emptyPreferences()

    override suspend fun readFrom(source: BufferedSource): Preferences {
        val prefMap: PreferencesMap = try {
            ProtoBuf.decodeFromByteArray<PreferencesMap>(
                source.readByteArray())
        } catch (e: SerializationException) {
            throw CorruptionException("Unable to parse preferences proto.", e)
        }

        val mutablePreferences = mutablePreferencesOf()

        prefMap.preferences.forEach { (name, value) ->
            addProtoEntryToPreferences(name, value, mutablePreferences)
        }

        return mutablePreferences.toPreferences()
    }

    override suspend fun writeTo(t: Preferences, sink: BufferedSink) {
        val preferences = t.asMap()
        val prefMap = mutableMapOf<String, Value>()
        for ((key, value) in preferences) {
            prefMap[key.name] = getValueProto(value)
        }
        val byteArray: ByteArray = ProtoBuf.encodeToByteArray(PreferencesMap(prefMap))
        sink.write(byteArray)
    }

    private fun addProtoEntryToPreferences(
        name: String,
        value: Value,
        mutablePreferences: MutablePreferences
    ) {
        if (value.boolean != null) {
            mutablePreferences[booleanPreferencesKey(name)] = value.boolean
        } else if (value.float != null) {
            mutablePreferences[floatPreferencesKey(name)] = value.float
        } else if (value.double != null) {
            mutablePreferences[doublePreferencesKey(name)] = value.double
        } else if (value.integer != null) {
            mutablePreferences[intPreferencesKey(name)] = value.integer
        } else if (value.long != null) {
            mutablePreferences[longPreferencesKey(name)] = value.long
        } else if (value.string != null) {
            mutablePreferences[stringPreferencesKey(name)] = value.string
        } else if (value.stringSet != null) {
            mutablePreferences[stringSetPreferencesKey(name)] =
                value.stringSet.strings.toSet()
        } else if (value.bytes != null) {
            mutablePreferences[byteArrayPreferencesKey(name)] = value.bytes
        } else {
            throw CorruptionException("Value case is null.")
        }
    }

    private fun getValueProto(value: Any): Value {
        return when (value) {
            is Boolean -> Value(boolean = value)
            is Float -> Value(float = value)
            is Double -> Value(double = value)
            is Int -> Value(integer = value)
            is Long -> Value(long = value)
            is String -> Value(string = value)
            is Set<*> ->
                @Suppress("UNCHECKED_CAST")
                Value(stringSet = StringSet(strings = value.map { it.toString() }))
            is ByteArray -> Value(bytes = value.copyOf())
            else -> throw IllegalStateException(
                "PreferencesSerializer does not support type: ${value::class}"
            )
        }
    }
}

// These data classes below map directly to
// datastore/datastore-preferences-proto/src/main/proto/preferences.proto
@Serializable
internal data class PreferencesMap(
    val preferences: Map<String, Value> = emptyMap()
)

@Serializable
internal data class Value(
    val boolean: Boolean? = null,
    val float: Float? = null,
    val integer: Int? = null,
    val long: Long? = null,
    val string: String? = null,
    val stringSet: StringSet? = null,
    val double: Double? = null,
    val bytes: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Value) return false

        if (boolean != other.boolean) return false
        if (float != other.float) return false
        if (integer != other.integer) return false
        if (long != other.long) return false
        if (string != other.string) return false
        if (stringSet != other.stringSet) return false
        if (double != other.double) return false
        if (bytes != null) {
            if (other.bytes == null) return false
            if (!bytes.contentEquals(other.bytes)) return false
        } else if (other.bytes != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = boolean?.hashCode() ?: 0
        result = 31 * result + (float?.hashCode() ?: 0)
        result = 31 * result + (integer ?: 0)
        result = 31 * result + (long?.hashCode() ?: 0)
        result = 31 * result + (string?.hashCode() ?: 0)
        result = 31 * result + stringSet.hashCode()
        result = 31 * result + (double?.hashCode() ?: 0)
        result = 31 * result + (bytes?.contentHashCode() ?: 0)
        return result
    }
}

@Serializable
internal data class StringSet(
    val strings: List<String> = emptyList(),
)
