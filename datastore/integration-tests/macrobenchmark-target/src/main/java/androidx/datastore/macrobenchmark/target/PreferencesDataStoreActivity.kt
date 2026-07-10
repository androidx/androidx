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
import androidx.datastore.core.DataStore
import androidx.datastore.macrobenchmark.target.ui.theme.AndroidxTheme
import androidx.datastore.macrobenchmark.target.utils.SettingsPage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.tracing.trace
import kotlin.math.abs
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore: DataStore<Preferences> by
    preferencesDataStore(name = "settings-preferences")

class PreferencesDataStoreActivity : ComponentActivity() {
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
            remember(context.dataStore) {
                    context.dataStore.data.map { preferences -> preferences[USER] ?: "Unknown" }
                }
                .collectAsState(initial = "Unknown")
        val displayMode by
            remember(context.dataStore) {
                    context.dataStore.data.map { preferences -> preferences[DISPLAY_MODE] ?: 0 }
                }
                .collectAsState(initial = 0)
        val camera by
            remember(context.dataStore) {
                    context.dataStore.data.map { preferences -> preferences[CAMERA] ?: 0L }
                }
                .collectAsState(initial = 0L)
        val volume by
            remember(context.dataStore) {
                    context.dataStore.data.map { preferences -> preferences[VOLUME] ?: 0.0f }
                }
                .collectAsState(initial = 0.0f)
        val brightness by
            remember(context.dataStore) {
                    context.dataStore.data.map { preferences -> preferences[BRIGHTNESS] ?: 0.0 }
                }
                .collectAsState(initial = 0.0)
        val darkMode by
            remember(context.dataStore) {
                    context.dataStore.data.map { preferences -> preferences[DARK_MODE] ?: false }
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
                        context.dataStore.updateData {
                            it.toMutablePreferences().also { preferences ->
                                preferences[USER] = user
                            }
                        }
                    }
                }
            },
            onDisplayModeChanged = { displayMode ->
                coroutineScope.launch {
                    trace("displayModeUpdate") {
                        context.dataStore.updateData {
                            it.toMutablePreferences().also { preferences ->
                                preferences[DISPLAY_MODE] = displayMode
                            }
                        }
                    }
                }
            },
            onCameraChanged = { camera ->
                coroutineScope.launch {
                    trace("cameraUpdate") {
                        context.dataStore.updateData {
                            it.toMutablePreferences().also { preferences ->
                                preferences[CAMERA] = camera
                            }
                        }
                    }
                }
            },
            onVolumeChanged = { volume ->
                coroutineScope.launch {
                    trace("volumeUpdate") {
                        context.dataStore.updateData {
                            it.toMutablePreferences().also { preferences ->
                                preferences[VOLUME] = volume
                            }
                        }
                    }
                }
            },
            onBrightnessChanged = { brightness ->
                coroutineScope.launch {
                    trace("brightnessUpdate") {
                        context.dataStore.updateData {
                            it.toMutablePreferences().also { preferences ->
                                preferences[BRIGHTNESS] = brightness
                            }
                        }
                    }
                }
            },
            onDarkModeChanged = { darkMode ->
                coroutineScope.launch {
                    trace("darkModeUpdate") {
                        context.dataStore.updateData {
                            it.toMutablePreferences().also { preferences ->
                                preferences[DARK_MODE] = darkMode
                            }
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

    companion object {
        private val USER = stringPreferencesKey("user")
        private val DISPLAY_MODE = intPreferencesKey("displayMode")
        private val CAMERA = longPreferencesKey("camera")
        private val VOLUME = floatPreferencesKey("volume")
        private val BRIGHTNESS = doublePreferencesKey("brightness")
        private val DARK_MODE = booleanPreferencesKey("darkMode")
    }
}
