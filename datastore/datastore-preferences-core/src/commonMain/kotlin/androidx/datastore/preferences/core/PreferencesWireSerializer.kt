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
import androidx.datastore.core.IOException
import androidx.datastore.core.InputStream
import androidx.datastore.core.OutputStream
import androidx.datastore.core.Serializer
import androidx.datastore.core.okio.asBufferedSink
import androidx.datastore.core.okio.asBufferedSource
import androidx.datastore.preferences.PreferenceMap
import androidx.datastore.preferences.StringSet
import androidx.datastore.preferences.Value
import com.squareup.wire.ProtoReader
import okio.ByteString.Companion.toByteString
import okio.use

object PreferencesWireSerializer : Serializer<Preferences> {
    override val defaultValue: Preferences
        get() {
            return emptyPreferences()
        }

    override suspend fun readFrom(input: InputStream): Preferences {
        val proto = try {
            PreferenceMap.ADAPTER.decode(input.asBufferedSource())
        } catch (e: IOException) {
            throw CorruptionException("Unable to parse preferences proto.", e)
        }
        val mutablePreferences = mutablePreferencesOf()
        proto.preferences.forEach { (name, value) ->
            addProtoEntryToPreferences(name, value, mutablePreferences)
        }

        return mutablePreferences.toPreferences()
    }

    private fun addProtoEntryToPreferences(
        name: String,
        value: Value,
        mutablePreferences: MutablePreferences
    ) {
        if (value.boolean != null) {
            mutablePreferences[booleanPreferencesKey(name)] = value.boolean
        } else if (value.androidx_testing_float != null) {
            mutablePreferences[floatPreferencesKey(name)] = value.androidx_testing_float
        } else if (value.androidx_testing_double != null) {
            mutablePreferences[doublePreferencesKey(name)] = value.androidx_testing_double
        } else if (value.integer != null) {
            mutablePreferences[intPreferencesKey(name)] = value.integer
        } else if (value.long != null) {
            mutablePreferences[longPreferencesKey(name)] = value.long
        } else if (value.androidx_testing_string != null) {
            mutablePreferences[stringPreferencesKey(name)] = value.androidx_testing_string
        } else if (value.string_set != null) {
            mutablePreferences[stringSetPreferencesKey(name)] =
                value.string_set.strings.toSet()
        } else if (value.androidx_testing_bytes != null) {
            mutablePreferences[byteArrayPreferencesKey(name)] =
                value.androidx_testing_bytes.toByteArray()
        } else {
            throw CorruptionException("Value not set.")
        }
    }

    override suspend fun writeTo(t: Preferences, output: OutputStream) {
        val preferences = t.asMap()
        val map = mutableMapOf<String, Value>()
        for ((key, value) in preferences) {
            map.put(key.name, getValueProto(value))
        }
        output.asBufferedSink().use {
            PreferenceMap.ADAPTER.encode(it, PreferenceMap(map))
        }
    }

    private fun getValueProto(value: Any): Value {
        return when (value) {
            is Boolean -> Value(boolean = value)
            is Float -> Value(androidx_testing_float = value)
            is Double -> Value(androidx_testing_double = value)
            is Int -> Value(integer = value)
            is Long -> Value(long = value)
            is String -> Value(androidx_testing_string = value)
            is Set<*> ->
                @Suppress("UNCHECKED_CAST")
                Value(string_set = StringSet(value.map { it as String }))
            is ByteArray -> Value(androidx_testing_bytes = value.toByteString())
            else -> throw IllegalStateException(
                "PreferencesSerializer does not support type: ${value::class}"
            )
        }
    }

}