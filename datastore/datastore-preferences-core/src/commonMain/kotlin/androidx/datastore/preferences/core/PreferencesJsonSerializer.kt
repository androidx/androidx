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
import androidx.datastore.core.InputStream
import androidx.datastore.core.OutputStream
import androidx.datastore.core.Serializer
import androidx.datastore.core.okio.asBufferedSink
import androidx.datastore.core.okio.asBufferedSource
import java.io.IOException
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import okio.use

object PreferencesJsonSerializer  : Serializer<Preferences> {
    override val defaultValue: Preferences
        get() {
            return emptyPreferences()
        }

    override suspend fun readFrom(input: InputStream): Preferences {
        val buf = input.asBufferedSource()
        val prefMap: PreferencesMap = try {
            val jsonString = buf.readUtf8()
            if (jsonString.isBlank()) {
                return defaultValue
            }
            Json.decodeFromString<PreferencesMap>(jsonString)
        } catch (e: SerializationException) {
            throw CorruptionException("Unable to parse preferences json.", e)
        }
        val prefs = mutablePreferencesOf()
        prefMap.preferences.forEach { (name, value) ->
            addPrefToMap(name, value, prefs)
        }
        return prefs

    }

    private fun addPrefToMap(key: String, value: Value, prefs: MutablePreferences) {
        prefs[Preferences.Key(key)] =
        if (value.boolean != null) {
            value.boolean
        } else if (value.int != null) {
            value.int
        } else if (value.double != null) {
            value.double
        } else if (value.float != null) {
            value.float
        } else if (value.bytes != null) {
            value.bytes
        } else if (value.long != null) {
            value.long
        } else if (value.string != null) {
            value.string
        } else if (value.stringSet != null) {
            value.stringSet
        } else {
            throw IOException("Invalid preference type for key: $key")
        }
    }

    override suspend fun writeTo(t: Preferences, output: OutputStream) {
        val prefs = mutableMapOf<String, Value>()
        t.asMap().map { (key, value) ->
            prefs.put(key.name, toValue(value))
        }

        val string = Json.encodeToString(PreferencesMap(prefs))
        output.asBufferedSink().use {
            it.writeUtf8(string)
        }
    }

    private fun toValue(value: Any): Value {
        return when(value) {
            is Boolean -> Value(boolean = value)
            is ByteArray -> Value(bytes = value)
            is Double -> Value(double = value)
            is Float -> Value(float=value)
            is Int -> Value(int=value)
            is Long -> Value(long = value)
            is Set<*> -> {
                @Suppress("UNCHECKED_CAST")
                Value(stringSet = value as Set<String>)
            }
            is String -> Value(string = value)
            else -> throw IOException("Invalid preference type: ${value::class}")
        }
    }


}

@kotlinx.serialization.Serializable
internal data class PreferencesMap(
    val preferences:Map<String, Value>
)

@kotlinx.serialization.Serializable
internal data class Value(
    val boolean:Boolean? = null,
    val float:Float? = null,
    val int: Int? = null,
    val long: Long? = null,
    val string:String? = null,
    val stringSet:Set<String>? = null,
    val double:Double? = null,
    val bytes:ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Value

        if (boolean != other.boolean) return false
        if (float != other.float) return false
        if (int != other.int) return false
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
        result = 31 * result + (int ?: 0)
        result = 31 * result + (long?.hashCode() ?: 0)
        result = 31 * result + (string?.hashCode() ?: 0)
        result = 31 * result + (stringSet?.hashCode() ?: 0)
        result = 31 * result + (double?.hashCode() ?: 0)
        result = 31 * result + (bytes?.contentHashCode() ?: 0)
        return result
    }
}