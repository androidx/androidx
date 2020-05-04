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

package androidx.datastore.preferences

import androidx.datastore.CorruptionException
import androidx.datastore.preferences.PreferencesProto.PreferenceMap
import androidx.datastore.preferences.PreferencesProto.Value
import androidx.datastore.preferences.PreferencesProto.StringSet
import androidx.datastore.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Proto based serializer for Preferences.
 *
 * TODO(b/156533452): this is a temporary implementation to allow for development. This will be
 * replaced before launching.
 */
internal object PreferencesSerializer : Serializer<Preferences> {
    override val defaultValue = Preferences.empty()

    override val fileExtension = "preferences_pb"

    @Throws(IOException::class, CorruptionException::class)
    override fun readFrom(input: InputStream): Preferences {
        val preferencesProto = try {
            PreferenceMap.parseFrom(input)
        } catch (invalidProtocolBufferException: InvalidProtocolBufferException) {
            throw CorruptionException(
                "Unable to parse preferences proto.",
                invalidProtocolBufferException
            )
        }

        val preferencesMap = preferencesProto.preferencesMap.mapValues {
            convertProtoToObject(it.value)
        }

        return Preferences(preferencesMap)
    }

    @Throws(IOException::class, CorruptionException::class)
    override fun writeTo(t: Preferences, output: OutputStream) {
        val preferences = t.getAll()
        val protoBuilder = PreferenceMap.newBuilder()

        for ((key, value) in preferences) {
            protoBuilder.putPreferences(key, getValueProto(value))
        }

        protoBuilder.build().writeTo(output)
    }

    private fun getValueProto(value: Any): Value {
        return when (value) {
            is Boolean -> Value.newBuilder().setBoolean(value).build()
            is Float -> Value.newBuilder().setFloat(value).build()
            is Int -> Value.newBuilder().setInteger(value).build()
            is Long -> Value.newBuilder().setLong(value).build()
            is String -> Value.newBuilder().setString(value).build()
            is Set<*> ->
                @Suppress("UNCHECKED_CAST")
                Value.newBuilder().setStringSet(
                    StringSet.newBuilder().addAllStrings(value as Set<String>)
                ).build()
            else -> throw IllegalStateException(
                "PreferencesSerializer does not support type: ${value.javaClass.name}"
            )
        }
    }

    private fun convertProtoToObject(value: Value): Any {
        return when (value.valueCase) {
            Value.ValueCase.BOOLEAN -> value.boolean
            Value.ValueCase.FLOAT -> value.float
            Value.ValueCase.INTEGER -> value.integer
            Value.ValueCase.LONG -> value.long
            Value.ValueCase.STRING -> value.string
            Value.ValueCase.STRING_SET -> value.stringSet.stringsList.toSet()
            Value.ValueCase.VALUE_NOT_SET ->
                throw CorruptionException("Value not set.")
            null -> throw CorruptionException("Value case is null.")
        }
    }
}