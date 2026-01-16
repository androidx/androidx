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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.ReportDrawnWhen
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.datastore.macrobenchmark.target.ui.theme.AndroidxTheme
import androidx.datastore.macrobenchmark.target.utils.SettingsPage
import androidx.tracing.trace
import kotlin.math.abs

class NoDataStoreActivity : ComponentActivity() {

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
        var user: String by remember { mutableStateOf("John Doe") }
        var displayMode: Int by remember { mutableIntStateOf(1) }
        var camera: Long by remember { mutableLongStateOf(1L) }
        var volume: Float by remember { mutableFloatStateOf(51.2f) }
        var brightness: Double by remember { mutableDoubleStateOf(51.2) }
        var darkMode: Boolean by remember { mutableStateOf(true) }

        SettingsPage(
            modifier = modifier,
            user = user,
            displayMode = displayMode,
            camera = camera,
            volume = volume,
            brightness = brightness,
            darkMode = darkMode,
            onUserChanged = { trace("userUpdate") { user = it } },
            onDisplayModeChanged = { trace("displayModeUpdate") { displayMode = it } },
            onCameraChanged = { trace("cameraUpdate") { camera = it } },
            onVolumeChanged = { trace("volumeUpdate") { volume = it } },
            onBrightnessChanged = { trace("brightnessUpdate") { brightness = it } },
            onDarkModeChanged = { trace("darkModeUpdate") { darkMode = it } },
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
