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

package com.example.datastore.snippets.multiprocess

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.MultiProcessDataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.dataStoreFile
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

fun createMultiProcessDataStore(context: Context): DataStore<Time> {
    // [START android_datastore_multiprocess_create]
    val dataStore =
        MultiProcessDataStoreFactory.create(
            serializer = TimeSerializer,
            produceFile = { context.dataStoreFile("time.pb") },
            corruptionHandler = null,
        )
    // [END android_datastore_multiprocess_create]
    MultiProcessDataStore.Companion.dataStore = dataStore
    return dataStore
}

class MultiProcessDataStore(context: Context) {
    val dataStore: DataStore<Time> = Companion.dataStore ?: createMultiProcessDataStore(context)

    // [START android_datastore_multiprocess_read]
    fun timeFlow(): Flow<Long> = dataStore.data.map { time -> time.lastUpdateMillis }

    // [END android_datastore_multiprocess_read]

    // [START android_datastore_multiprocess_write]
    suspend fun updateLastUpdateTime() {
        dataStore.updateData { time -> time.copy(lastUpdateMillis = System.currentTimeMillis()) }
    }

    // [END android_datastore_multiprocess_write]

    companion object {
        var dataStore: DataStore<Time>? = null
    }
}

// [START android_datastore_multiprocess_definition]
@Serializable data class Time(val lastUpdateMillis: Long)

// [END android_datastore_multiprocess_definition]

// [START android_datastore_multiprocess_serializer]
object TimeSerializer : Serializer<Time> {

    override val defaultValue: Time = Time(lastUpdateMillis = 0L)

    override suspend fun readFrom(input: InputStream): Time =
        try {
            Json.decodeFromString(Time.serializer(), input.readBytes().decodeToString())
        } catch (serialization: SerializationException) {
            throw CorruptionException("Unable to read Time", serialization)
        }

    override suspend fun writeTo(t: Time, output: OutputStream) {
        output.write(Json.encodeToString(Time.serializer(), t).encodeToByteArray())
    }
}
// [END android_datastore_multiprocess_serializer]
