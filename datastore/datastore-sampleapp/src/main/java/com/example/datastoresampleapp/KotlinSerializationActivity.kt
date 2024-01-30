/*
 * Copyright 2021 The Android Open Source Project
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

package com.example.datastoresampleapp

import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.lifecycle.lifecycleScope
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class KotlinSerializationActivity : AppCompatActivity() {
    private val TAG = "SerializationActivity"

    private val PROTO_STORE_FILE_NAME = "kotlin_serialization_test_file.json"

    private val settingsStore: DataStore<MySettings> by lazy {
        DataStoreFactory.create(
            serializer = MySettingsSerializer
        ) { File(applicationContext.filesDir, PROTO_STORE_FILE_NAME) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Strict mode allows us to check that no writes or reads are blocking the UI thread.
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .penaltyDeath()
                .build()
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setUpProtoDataStoreUi()
    }

    private fun setUpProtoDataStoreUi() {
        findViewById<Button>(R.id.counter_dec).setOnClickListener {
            lifecycleScope.launch {
                settingsStore.updateData { currentSettings ->
                    currentSettings.copy(
                        count = currentSettings.count - 1
                    )
                }
            }
        }

        findViewById<Button>(R.id.counter_inc).setOnClickListener {
            lifecycleScope.launch {
                settingsStore.updateData { currentSettings ->
                    currentSettings.copy(
                        count = currentSettings.count + 1
                    )
                }
            }
        }

        lifecycleScope.launch {
            settingsStore.data
                .catch { e ->
                    if (e is IOException) {
                        Log.e(TAG, "Error reading preferences.", e)
                        emit(MySettings())
                    } else {
                        throw e
                    }
                }
                .map { it.count }
                .distinctUntilChanged()
                .collect { counterValue ->
                    findViewById<TextView>(R.id.counter_text_view).text =
                        counterValue.toString()
                }
        }
    }
}

@Serializable
data class MySettings(val count: Int = 0)

@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
object MySettingsSerializer : Serializer<MySettings> {
    override val defaultValue: MySettings
        get() = MySettings()

    override suspend fun readFrom(input: InputStream): MySettings {
        try {
            return Json.decodeFromString<MySettings>(input.readBytes().decodeToString())
        } catch (serialization: SerializationException) {
            throw CorruptionException("Unable to read Json", serialization)
        }
    }

    override suspend fun writeTo(t: MySettings, output: OutputStream) {
        output.write(Json.encodeToString(t).encodeToByteArray())
    }
}
