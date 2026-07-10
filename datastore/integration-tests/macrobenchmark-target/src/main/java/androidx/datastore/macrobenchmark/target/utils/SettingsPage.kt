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

package androidx.datastore.macrobenchmark.target.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType.Companion.PrimaryNotEditable
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    modifier: Modifier = Modifier,
    user: String,
    displayMode: Int,
    camera: Long,
    volume: Float,
    brightness: Double,
    darkMode: Boolean,
    onUserChanged: (String) -> Unit,
    onDisplayModeChanged: (Int) -> Unit,
    onCameraChanged: (Long) -> Unit,
    onVolumeChanged: (Float) -> Unit,
    onBrightnessChanged: (Double) -> Unit,
    onDarkModeChanged: (Boolean) -> Unit,
) {
    Column(
        modifier.fillMaxSize().padding(16.dp).semantics { testTagsAsResourceId = true },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Settings", fontSize = 50.sp)

        // User.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "User", fontSize = 25.sp)
            TextField(
                modifier = Modifier.testTag("UserTextField"),
                value = user,
                onValueChange = onUserChanged,
                textStyle = TextStyle(fontSize = 25.sp, fontStyle = FontStyle.Italic),
            )
        }
        Spacer(Modifier.height(10.dp))

        // Display Mode.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "Display Mode", fontSize = 25.sp)
            var expanded by remember { mutableStateOf(false) }
            val items = listOf("Home", "Work", "Driving")

            ExposedDropdownMenuBox(
                modifier = Modifier.testTag("DisplayModeDropdownMenu"),
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
            ) {
                TextField(
                    modifier = Modifier.menuAnchor(PrimaryNotEditable),
                    value = items[displayMode],
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                )

                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    items.forEachIndexed { index, item ->
                        DropdownMenuItem(
                            modifier =
                                Modifier.semantics { testTagsAsResourceId = true }
                                    .testTag("DisplayModeOption$index"),
                            text = { Text(text = item) },
                            onClick = {
                                onDisplayModeChanged(index)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        // Camera.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "Camera", fontSize = 25.sp)
            var expanded by remember { mutableStateOf(false) }
            val items = listOf("Front", "Back", "Other")

            ExposedDropdownMenuBox(
                modifier = Modifier.testTag("CameraDropdownMenu"),
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
            ) {
                TextField(
                    modifier = Modifier.menuAnchor(PrimaryNotEditable),
                    value = items[camera.toInt()],
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                )

                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    items.forEachIndexed { index, item ->
                        DropdownMenuItem(
                            modifier =
                                Modifier.semantics { testTagsAsResourceId = true }
                                    .testTag("CameraOption$index"),
                            text = { Text(text = item) },
                            onClick = {
                                onCameraChanged(index.toLong())
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        // Volume.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "Volume", fontSize = 25.sp)
            Text(text = String.format("%.1f", volume), fontSize = 20.sp)
        }
        Slider(
            modifier = Modifier.testTag("VolumeSlider"),
            value = volume,
            valueRange = 0.0f..100.0f,
            onValueChange = onVolumeChanged,
        )
        Spacer(Modifier.height(10.dp))

        // Brightness.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "Brightness", fontSize = 25.sp)
            Text(text = String.format("%.1f", brightness), fontSize = 20.sp)
        }
        Slider(
            modifier = Modifier.testTag("BrightnessSlider"),
            value = brightness.toFloat(),
            valueRange = 0.0f..100.0f,
            onValueChange = { onBrightnessChanged(it.toDouble()) },
        )
        Spacer(Modifier.height(10.dp))

        // Dark Mode.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "Dark Mode", fontSize = 25.sp)
            Switch(
                modifier = Modifier.testTag("DarkModeSwitch"),
                checked = darkMode,
                onCheckedChange = onDarkModeChanged,
            )
        }
        Spacer(Modifier.height(10.dp))
    }
}
