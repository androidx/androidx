/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3.demos

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.integration.demos.common.ComposableDemo
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Picker
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimePicker
import androidx.wear.compose.material3.TimePickerType
import androidx.wear.compose.material3.rememberPickerState
import androidx.wear.compose.material3.samples.AutoCenteringPickerGroup
import androidx.wear.compose.material3.samples.PickerAnimateScrollToOption
import androidx.wear.compose.material3.samples.PickerGroupSample
import androidx.wear.compose.material3.samples.SimplePicker
import androidx.wear.compose.material3.samples.TimePickerSample
import androidx.wear.compose.material3.samples.TimePickerWith12HourClockSample
import androidx.wear.compose.material3.samples.TimePickerWithSecondsSample
import java.time.LocalTime
import java.time.format.DateTimeFormatter

val PickerDemos =
    listOf(
        // Requires API level 26 or higher due to java.time dependency.
        *(if (Build.VERSION.SDK_INT >= 26)
            arrayOf(
                ComposableDemo("Time HH:MM:SS") { TimePickerWithSecondsSample() },
                ComposableDemo("Time HH:MM") {
                    var showTimePicker by remember { mutableStateOf(true) }
                    var timePickerTime by remember { mutableStateOf(LocalTime.now()) }
                    val formatter = DateTimeFormatter.ofPattern("HH:mm")
                    if (showTimePicker) {
                        TimePicker(
                            onTimePicked = {
                                timePickerTime = it
                                showTimePicker = false
                            },
                            timePickerType = TimePickerType.HoursMinutes24H,
                            // Initialize with last picked time on reopen
                            initialTime = timePickerTime
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Selected Time")
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { showTimePicker = true },
                                label = { Text(timePickerTime.format(formatter)) },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "Edit"
                                    )
                                },
                            )
                        }
                    }
                },
                ComposableDemo("Time 12 Hour") { TimePickerWith12HourClockSample() },
                ComposableDemo("Time System time format") { TimePickerSample() },
            )
        else emptyArray<ComposableDemo>()),
        ComposableDemo("Simple Picker") { SimplePicker() },
        ComposableDemo("No gradient") { PickerWithoutGradient() },
        ComposableDemo("Animate picker change") { PickerAnimateScrollToOption() },
        ComposableDemo("Sample Picker Group") { PickerGroupSample() },
        ComposableDemo("Auto-centering Picker Group") { AutoCenteringPickerGroup() }
    )

@Composable
fun PickerWithoutGradient() {
    val items = listOf("One", "Two", "Three", "Four", "Five")
    val state = rememberPickerState(items.size)
    val contentDescription by remember { derivedStateOf { "${state.selectedOption + 1}" } }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Picker(
            readOnly = false,
            modifier = Modifier.size(100.dp, 100.dp),
            gradientRatio = 0.0f,
            contentDescription = contentDescription,
            state = state
        ) {
            Text(items[it])
        }
    }
}
