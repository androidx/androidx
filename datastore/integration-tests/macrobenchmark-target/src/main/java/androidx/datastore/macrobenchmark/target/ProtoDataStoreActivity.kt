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

package androidx.datastore.macrobenchmark.target

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.ReportDrawnWhen
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import androidx.datastore.macrobenchmark.target.ui.theme.AndroidxTheme
import androidx.datastore.macrobenchmark.target.utils.SettingsPage
import androidx.tracing.trace
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.abs
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Settings> by
    dataStore(fileName = "settings-proto.pb", serializer = SettingsSerializer)

class ProtoDataStoreActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AndroidxTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Content(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    @Composable
    fun Content(modifier: Modifier) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

        val user by
            remember(context.dataStore) { context.dataStore.data.map { settings -> settings.user } }
                .collectAsState(initial = "Unknown")

        val displayMode by
            remember(context.dataStore) {
                    context.dataStore.data.map { settings -> settings.displayMode }
                }
                .collectAsState(initial = 0)

        val camera by
            remember(context.dataStore) {
                    context.dataStore.data.map { settings -> settings.camera }
                }
                .collectAsState(initial = 0L)

        val volume by
            remember(context.dataStore) {
                    context.dataStore.data.map { settings -> settings.volume }
                }
                .collectAsState(initial = 0.0f)

        val brightness by
            remember(context.dataStore) {
                    context.dataStore.data.map { settings -> settings.brightness }
                }
                .collectAsState(initial = 0.0)

        val darkMode by
            remember(context.dataStore) {
                    context.dataStore.data.map { settings -> settings.darkMode }
                }
                .collectAsState(initial = false)

        SettingsPage(
            modifier = modifier,
            user = user,
            displayMode = displayMode,
            camera = camera,
            volume = volume,
            brightness = brightness,
            darkMode = darkMode,
            onUserChanged = { user ->
                coroutineScope.launch {
                    trace("userUpdate") {
                        context.dataStore.updateData { settings ->
                            settings.copy { this.user = user }
                        }
                    }
                }
            },
            onDisplayModeChanged = { displayMode ->
                coroutineScope.launch {
                    trace("displayModeUpdate") {
                        context.dataStore.updateData { settings ->
                            settings.copy { this.displayMode = displayMode }
                        }
                    }
                }
            },
            onCameraChanged = { camera ->
                coroutineScope.launch {
                    trace("cameraUpdate") {
                        context.dataStore.updateData { settings ->
                            settings.copy { this.camera = camera }
                        }
                    }
                }
            },
            onVolumeChanged = { volume ->
                coroutineScope.launch {
                    trace("volumeUpdate") {
                        context.dataStore.updateData { settings ->
                            settings.copy { this.volume = volume }
                        }
                    }
                }
            },
            onBrightnessChanged = { brightness ->
                coroutineScope.launch {
                    trace("brightnessUpdate") {
                        context.dataStore.updateData { settings ->
                            settings.copy { this.brightness = brightness }
                        }
                    }
                }
            },
            onDarkModeChanged = { darkMode ->
                coroutineScope.launch {
                    trace("darkModeUpdate") {
                        context.dataStore.updateData { settings ->
                            settings.copy { this.darkMode = darkMode }
                        }
                    }
                }
            },
        )

        ReportDrawnWhen {
            user == "John Doe" &&
                displayMode == 1 &&
                camera == 1L &&
                abs(volume - 51.2f) < 0.1f &&
                abs(brightness - 51.2) < 0.1 &&
                darkMode
        }
    }
}

private object SettingsSerializer : Serializer<Settings> {
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
