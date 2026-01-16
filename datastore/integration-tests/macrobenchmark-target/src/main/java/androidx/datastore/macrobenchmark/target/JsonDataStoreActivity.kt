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
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.abs
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<JsonDataStoreActivity.Settings> by
    dataStore(
        fileName = "settings-json.json",
        serializer = JsonDataStoreActivity.SettingsSerializer,
    )

class JsonDataStoreActivity : ComponentActivity() {

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
            onUserChanged = {
                coroutineScope.launch {
                    trace("userUpdate") {
                        context.dataStore.updateData { settings -> settings.copy(user = it) }
                    }
                }
            },
            onDisplayModeChanged = {
                coroutineScope.launch {
                    trace("displayModeUpdate") {
                        context.dataStore.updateData { settings -> settings.copy(displayMode = it) }
                    }
                }
            },
            onCameraChanged = {
                coroutineScope.launch {
                    trace("cameraUpdate") {
                        context.dataStore.updateData { settings -> settings.copy(camera = it) }
                    }
                }
            },
            onVolumeChanged = {
                coroutineScope.launch {
                    trace("volumeUpdate") {
                        context.dataStore.updateData { settings -> settings.copy(volume = it) }
                    }
                }
            },
            onBrightnessChanged = {
                coroutineScope.launch {
                    trace("brightnessUpdate") {
                        context.dataStore.updateData { settings -> settings.copy(brightness = it) }
                    }
                }
            },
            onDarkModeChanged = {
                coroutineScope.launch {
                    trace("darkModeUpdate") {
                        context.dataStore.updateData { settings -> settings.copy(darkMode = it) }
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

    @Serializable
    data class Settings(
        val user: String,
        val volume: Float,
        val brightness: Double,
        val darkMode: Boolean,
        val displayMode: Int,
        val camera: Long,
    )

    object SettingsSerializer : Serializer<Settings> {

        override val defaultValue: Settings =
            Settings(
                user = "Unknown",
                volume = 0.0f,
                brightness = 0.0,
                darkMode = false,
                displayMode = 0,
                camera = 0L,
            )

        override suspend fun readFrom(input: InputStream): Settings =
            try {
                Json.decodeFromString<Settings>(input.readBytes().decodeToString())
            } catch (serialization: SerializationException) {
                throw CorruptionException("Unable to read Settings", serialization)
            }

        override suspend fun writeTo(t: Settings, output: OutputStream) {
            output.write(Json.encodeToString<Settings>(t).encodeToByteArray())
        }
    }
}
