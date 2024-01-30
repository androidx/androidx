/*
 * Copyright 2020 The Android Open Source Project
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
import androidx.datastore.preferences.PreferencesMapCompat
import androidx.datastore.preferences.PreferencesProto.PreferenceMap
import androidx.datastore.preferences.PreferencesProto.StringSet
import androidx.datastore.preferences.PreferencesProto.Value
import androidx.datastore.preferences.protobuf.ByteString
import kotlin.jvm.Throws
import okio.BufferedSink
import okio.BufferedSource
import okio.IOException

/**
 * Proto based serializer for Preferences. Can be used to manually create
 * [DataStore][androidx.datastore.core.DataStore] using the
 * [DataStoreFactory#create][androidx.datastore.core.DataStoreFactory.create] function.
 */
actual object PreferencesSerializer : OkioSerializer<Preferences> {
    internal const val fileExtension = "preferences_pb"

    override val defaultValue: Preferences
        get() {
            return emptyPreferences()
        }

    @Throws(IOException::class, CorruptionException::class)
    override suspend fun readFrom(source: BufferedSource): Preferences {
        val preferencesProto = PreferencesMapCompat.readFrom(source.inputStream())

        val mutablePreferences = mutablePreferencesOf()

        preferencesProto.preferencesMap.forEach { (name, value) ->
            addProtoEntryToPreferences(name, value, mutablePreferences)
        }

        return mutablePreferences.toPreferences()
    }

    @Suppress("InvalidNullabilityOverride") // Remove after b/232460179 is fixed
    @Throws(IOException::class, CorruptionException::class)
    override suspend fun writeTo(t: Preferences, sink: BufferedSink) {
        val preferences = t.asMap()
        val protoBuilder = PreferenceMap.newBuilder()

        for ((key, value) in preferences) {
            protoBuilder.putPreferences(key.name, getValueProto(value))
        }

        protoBuilder.build().writeTo(sink.outputStream())
    }

    private fun getValueProto(value: Any): Value {
        return when (value) {
            is Boolean -> Value.newBuilder().setBoolean(value).build()
            is Float -> Value.newBuilder().setFloat(value).build()
            is Double -> Value.newBuilder().setDouble(value).build()
            is Int -> Value.newBuilder().setInteger(value).build()
            is Long -> Value.newBuilder().setLong(value).build()
            is String -> Value.newBuilder().setString(value).build()
            is Set<*> ->
                @Suppress("UNCHECKED_CAST")
                Value.newBuilder().setStringSet(
                    StringSet.newBuilder().addAllStrings(value as Set<String>)
                ).build()
            is ByteArray -> Value.newBuilder().setBytes(ByteString.copyFrom(value)).build()
            else -> throw IllegalStateException(
                "PreferencesSerializer does not support type: ${value.javaClass.name}"
            )
        }
    }

    private fun addProtoEntryToPreferences(
        name: String,
        value: Value,
        mutablePreferences: MutablePreferences
    ) {
        return when (value.valueCase) {
            Value.ValueCase.BOOLEAN ->
                mutablePreferences[booleanPreferencesKey(name)] =
                    value.boolean
            Value.ValueCase.FLOAT -> mutablePreferences[floatPreferencesKey(name)] = value.float
            Value.ValueCase.DOUBLE -> mutablePreferences[doublePreferencesKey(name)] = value.double
            Value.ValueCase.INTEGER -> mutablePreferences[intPreferencesKey(name)] = value.integer
            Value.ValueCase.LONG -> mutablePreferences[longPreferencesKey(name)] = value.long
            Value.ValueCase.STRING -> mutablePreferences[stringPreferencesKey(name)] = value.string
            Value.ValueCase.STRING_SET ->
                mutablePreferences[stringSetPreferencesKey(name)] =
                    value.stringSet.stringsList.toSet()
            Value.ValueCase.BYTES ->
                mutablePreferences[byteArrayPreferencesKey(name)] = value.bytes.toByteArray()
            Value.ValueCase.VALUE_NOT_SET ->
                throw CorruptionException("Value not set.")
            null -> throw CorruptionException("Value case is null.")
        }
    }
}
