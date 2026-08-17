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

package com.example.datastore.snippets.proto

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.example.datastoresampleapp.Settings
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// [START android_datastore_proto_create]
val Context.dataStore: DataStore<Settings> by
    dataStore(fileName = "settings.pb", serializer = SettingsSerializer)

// [END android_datastore_proto_create]

class ProtoDataStore(private val context: Context) {

    // [START android_datastore_proto_read]
    fun counterFlow(): Flow<Int> = context.dataStore.data.map { settings -> settings.counter }

    // [END android_datastore_proto_read]

    // [START android_datastore_proto_write]
    suspend fun incrementCounter() {
        context.dataStore.updateData { settings ->
            settings.toBuilder().setCounter(settings.counter + 1).build()
        }
    }
    // [END android_datastore_proto_write]
}

// [START android_datastore_proto_serializer]
object SettingsSerializer : Serializer<Settings> {
    override val defaultValue: Settings = Settings.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): Settings {
        try {
            return Settings.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: Settings, output: OutputStream) {
        return t.writeTo(output)
    }
}
// [END android_datastore_proto_serializer]
