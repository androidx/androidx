/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import androidx.datastore.core.okio.WebSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlinx.serialization.json.add
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.double
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import okio.BufferedSink
import okio.BufferedSource
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.toByteString

/**
 * Proto based serializer for Preferences. Can be used to manually create
 * [DataStore][androidx.datastore.core.DataStore] using the
 * [DataStoreFactory#create][androidx.datastore.core.DataStoreFactory.create] function.
 */
actual object PreferencesSerializer : OkioSerializer<Preferences> {
    private val delegateJsonSerializer =
        WebSerializer(JsonObject.serializer(), JsonObject(emptyMap()))

    actual override val defaultValue: Preferences = emptyPreferences()

    actual override suspend fun readFrom(source: BufferedSource): Preferences {
        try {
            val jsonObject = delegateJsonSerializer.readFrom(source)
            if (jsonObject == delegateJsonSerializer.defaultValue) {
                // String was empty, we return the PreferencesSerializer default value.
                return defaultValue
            }

            val mutablePreferences = mutablePreferencesOf()

            jsonObject.entries.forEach { (prefixedKey, jsonElement) ->
                // Keys are stored as "type/name", for example "i/my_int_key".
                // We limit the split to 2 in case the key name contains a '/'.
                val parts = prefixedKey.split('/', limit = 2)
                if (parts.size != 2) return@forEach // Skip malformed keys
                val type = parts[0]
                val key = parts[1]

                when (type) {
                    "b" ->
                        mutablePreferences[booleanPreferencesKey(key)] =
                            jsonElement.jsonPrimitive.boolean
                    "f" ->
                        mutablePreferences[floatPreferencesKey(key)] =
                            jsonElement.jsonPrimitive.float
                    "d" ->
                        mutablePreferences[doublePreferencesKey(key)] =
                            jsonElement.jsonPrimitive.double
                    "i" ->
                        mutablePreferences[intPreferencesKey(key)] = jsonElement.jsonPrimitive.int
                    "l" ->
                        mutablePreferences[longPreferencesKey(key)] = jsonElement.jsonPrimitive.long
                    "s" ->
                        mutablePreferences[stringPreferencesKey(key)] =
                            jsonElement.jsonPrimitive.content
                    "ss" -> {
                        val stringSet =
                            jsonElement.jsonArray.map { it.jsonPrimitive.content }.toSet()
                        mutablePreferences[stringSetPreferencesKey(key)] = stringSet
                    }
                    "ba" -> {
                        // Decode Base64 string back to ByteArray
                        val byteArray =
                            jsonElement.jsonPrimitive.content.decodeBase64()?.toByteArray()
                        if (byteArray != null) {
                            mutablePreferences[byteArrayPreferencesKey(key)] = byteArray
                        }
                    }
                }
            }
            return mutablePreferences.toPreferences()
        } catch (ex: Exception) {
            throw CorruptionException("Unable to parse preferences json.", ex)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    actual override suspend fun writeTo(t: Preferences, sink: BufferedSink) {
        val preferencesMap = t.asMap()
        val jsonMap = mutableMapOf<String, JsonElement>()

        for ((key, value) in preferencesMap) {
            val typePrefix: String
            val jsonValue: JsonElement
            when (value) {
                is Boolean -> {
                    typePrefix = "b"
                    jsonValue = JsonPrimitive(value)
                }
                is Float -> {
                    typePrefix = "f"
                    jsonValue = JsonUnquotedLiteral(value.toString())
                }
                is Double -> {
                    typePrefix = "d"
                    jsonValue = JsonUnquotedLiteral(value.toString())
                }
                is Int -> {
                    typePrefix = "i"
                    jsonValue = JsonPrimitive(value)
                }
                is Long -> {
                    typePrefix = "l"
                    jsonValue = JsonPrimitive(value)
                }
                is String -> {
                    typePrefix = "s"
                    jsonValue = JsonPrimitive(value)
                }
                is Set<*> -> {
                    typePrefix = "ss"
                    @Suppress("UNCHECKED_CAST")
                    jsonValue = buildJsonArray {
                        (value as Set<String>).forEach { add(it) }
                    }
                }
                is ByteArray -> {
                    typePrefix = "ba"
                    // Encode ByteArray to a Base64 string for JSON compatibility
                    jsonValue = JsonPrimitive(value.toByteString().base64())
                }
                else ->
                    throw IllegalStateException(
                        "PreferencesSerializer does not support the provided type on WASM."
                    )
            }
            jsonMap["$typePrefix/${key.name}"] = jsonValue
        }

        delegateJsonSerializer.writeTo(JsonObject(jsonMap), sink)
    }
}
