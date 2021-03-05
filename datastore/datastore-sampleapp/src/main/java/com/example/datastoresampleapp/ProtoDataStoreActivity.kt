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

package com.example.datastoresampleapp

import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.annotation.Sampled
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.lifecycle.lifecycleScope
import com.google.protobuf.InvalidProtocolBufferException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ProtoDataStoreActivity : AppCompatActivity() {
    private val TAG = "ProtoActivity"

    private val PROTO_STORE_FILE_NAME = "datastore_test_app.pb"

    private val settingsStore: DataStore<Settings> by lazy {
        DataStoreFactory.create(
            serializer = SettingsSerializer
        ) { File(applicationContext.filesDir, PROTO_STORE_FILE_NAME) }
    }

    @Sampled
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

    @Sampled
    private fun setUpProtoDataStoreUi() {
        findViewById<Button>(R.id.counter_dec).setOnClickListener {
            lifecycleScope.launch {
                settingsStore.updateData { currentSettings ->
                    currentSettings.toBuilder().setCounter(currentSettings.counter - 1).build()
                }
            }
        }

        findViewById<Button>(R.id.counter_inc).setOnClickListener {
            lifecycleScope.launch {
                settingsStore.updateData { currentSettings ->
                    currentSettings.toBuilder().setCounter(currentSettings.counter + 1).build()
                }
            }
        }

        lifecycleScope.launch {
            settingsStore.data
                .catch { e ->
                    if (e is IOException) {
                        Log.e(TAG, "Error reading preferences.", e)
                        emit(Settings.getDefaultInstance())
                    } else {
                        throw e
                    }
                }
                .map { it.counter }
                .distinctUntilChanged()
                .collect { counterValue ->
                    findViewById<TextView>(R.id.counter_text_view).text =
                        counterValue.toString()
                }
        }
    }

    private object SettingsSerializer : Serializer<Settings> {

        override val defaultValue: Settings = Settings.getDefaultInstance()

        override suspend fun readFrom(input: InputStream): Settings {
            try {
                return Settings.parseFrom(input)
            } catch (ipbe: InvalidProtocolBufferException) {
                throw CorruptionException("Cannot read proto.", ipbe)
            }
        }

        override suspend fun writeTo(t: Settings, output: OutputStream) = t.writeTo(output)
    }
}