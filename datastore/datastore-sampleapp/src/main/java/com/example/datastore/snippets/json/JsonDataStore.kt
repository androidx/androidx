/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.datastore.snippets.json

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

// [START android_datastore_json_create]
val Context.dataStore: DataStore<Settings> by
    dataStore(
        fileName = "settings.json",
        serializer = SettingsSerializer,
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    )

// [END android_datastore_json_create]

class JsonDataStore(private val context: Context) {

    // [START android_datastore_json_read]
    fun counterFlow(): Flow<Int> =
        context.dataStore.data.map { settings -> settings.exampleCounter }

    // [END android_datastore_json_read]

    // [START android_datastore_json_write]
    suspend fun incrementCounter() {
        context.dataStore.updateData { settings ->
            settings.copy(exampleCounter = settings.exampleCounter + 1)
        }
    }
    // [END android_datastore_json_write]
}

// [START android_datastore_json_definition]
@Serializable data class Settings(val exampleCounter: Int)

// [END android_datastore_json_definition]

// [START android_datastore_json_serializer]
object SettingsSerializer : Serializer<Settings> {

    override val defaultValue: Settings = Settings(exampleCounter = 0)

    override suspend fun readFrom(input: InputStream): Settings =
        try {
            Json.decodeFromString(Settings.serializer(), input.readBytes().decodeToString())
        } catch (serialization: SerializationException) {
            throw CorruptionException("Unable to read Settings", serialization)
        }

    override suspend fun writeTo(t: Settings, output: OutputStream) {
        output.write(Json.encodeToString(Settings.serializer(), t).encodeToByteArray())
    }
}
// [END android_datastore_json_serializer]
