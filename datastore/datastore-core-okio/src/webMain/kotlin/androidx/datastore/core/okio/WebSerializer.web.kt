/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.datastore.core.okio

import androidx.datastore.core.CorruptionException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okio.BufferedSink
import okio.BufferedSource

/**
 * A Serializer that uses kotlinx.serialization.json to convert any @Serializable data class T to
 * and from a byte stream using Okio.
 *
 * @param T The data type to serialize. Must be annotated with @Serializable.
 * @param kSerializer The KSerializer for type T, usually accessed via `T.serializer()`.
 * @param defaultValue The default instance of T to return if data is corrupted or doesn't exist.
 */
public class WebSerializer<T>(
    internal val kSerializer: KSerializer<T>,
    override val defaultValue: T,
) : OkioSerializer<T> {

    /**
     * Reads a UTF-8 string from the source, and then parses that string back into an object of type
     * T using JSON.
     */
    override suspend fun readFrom(source: BufferedSource): T {
        try {
            val jsonString = source.readUtf8()
            // If the string is empty, it means no data has been stored yet.
            if (jsonString.isEmpty()) {
                return defaultValue
            }
            return Json.decodeFromString(kSerializer, jsonString)
        } catch (e: SerializationException) {
            throw CorruptionException("Unable to deserialize object", e)
        }
    }

    /**
     * Encodes the object of type T into a JSON string, and writes that UTF-8 string to the sink.
     */
    override suspend fun writeTo(t: T, sink: BufferedSink) {
        val jsonString = Json.encodeToString(kSerializer, t)
        sink.writeUtf8(jsonString)
    }
}
