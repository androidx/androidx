/*
 * Copyright 2026 The Android Open Source Project
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

package com.example.datastorecomposesamples

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
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

class TimestampUpdateService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}

@Composable
fun MultiProcessDataStoreScreen() {
    Column(Modifier.fillMaxSize()) {
        Text(text = "Multi-process DataStore", fontSize = 30.sp)

        // [START android_datastore_multiprocess_app]
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val multiProcessDataStore = remember(context) { MultiProcessDataStore(context) }

        // Display time written by other process.
        val lastUpdateTime by
            multiProcessDataStore
                .timeFlow()
                .collectAsState(initial = 0, coroutineScope.coroutineContext)
        Text(text = "Last updated: $lastUpdateTime", fontSize = 25.sp)

        DisposableEffect(context) {
            val serviceIntent = Intent(context, TimestampUpdateService::class.java)
            context.startService(serviceIntent)
            onDispose { context.stopService(serviceIntent) }
        }
        // [END android_datastore_multiprocess_app]
    }
}
