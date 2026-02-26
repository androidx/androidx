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

package androidx.compose.remote.integration.view.demos

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

enum class RenderMode {
    REMOTE,
    REMOTE_VIDEO_ENCODE;

    companion object {
        fun fromString(value: String?): RenderMode =
            when (value?.lowercase()) {
                "remote_video" -> REMOTE_VIDEO_ENCODE
                else -> REMOTE
            }
    }
}

enum class Resolution(val width: Int, val height: Int) {
    RES_480X480(480, 480),
    RES_720X720(720, 720),
    RES_1280X720(1280, 720),
}

enum class Duration(val label: String, val millis: Long) {
    SEC_5("5 seconds", 5_000L),
    SEC_30("30 seconds", 30_000L),
    MIN_2("2 minutes", 120_000L),
}

enum class Fps(val value: Int) {
    FPS_1(1),
    FPS_10(15),
    FPS_30(30),
}

@Composable
private fun <T> DropdownSelector(
    label: String,
    currentValueName: String,
    items: List<T>,
    itemToName: (T) -> String,
    onItemSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Button(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text("$label: $currentValueName")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(itemToName(item)) },
                    onClick = {
                        onItemSelected(item)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun RenderModeSelector(selectedMode: RenderMode, onModeSelected: (RenderMode) -> Unit) {
    DropdownSelector(
        label = "Mode",
        currentValueName = selectedMode.name,
        items = RenderMode.values().toList(),
        itemToName = { it.name },
        onItemSelected = onModeSelected,
    )
}

@Composable
fun ResolutionSelector(selectedRes: Resolution, onResSelected: (Resolution) -> Unit) {
    DropdownSelector(
        label = "Res",
        currentValueName = selectedRes.name,
        items = Resolution.values().toList(),
        itemToName = { it.name },
        onItemSelected = onResSelected,
    )
}

@Composable
fun DurationSelector(selectedDuration: Duration, onDurationSelected: (Duration) -> Unit) {
    DropdownSelector(
        label = "Duration",
        currentValueName = selectedDuration.label,
        items = Duration.values().toList(),
        itemToName = { it.label },
        onItemSelected = onDurationSelected,
    )
}

@Composable
fun FpsSelector(selectedFps: Fps, onFpsSelected: (Fps) -> Unit) {
    DropdownSelector(
        label = "FPS",
        currentValueName = selectedFps.value.toString(),
        items = Fps.values().toList(),
        itemToName = { it.value.toString() },
        onItemSelected = onFpsSelected,
    )
}

@Composable
fun SampleSelector(
    samples: List<DumperSample>,
    selectedSampleName: String,
    onSampleSelected: (DumperSample) -> Unit,
) {
    DropdownSelector(
        label = "Sample",
        currentValueName = selectedSampleName,
        items = samples,
        itemToName = { it.name },
        onItemSelected = onSampleSelected,
    )
}
