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
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.wear.compose.integration.demos.common.ComposableDemo
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimePicker
import androidx.wear.compose.material3.TimePickerType
import androidx.wear.compose.material3.samples.TimePickerSample
import androidx.wear.compose.material3.samples.TimePickerWith12HourClockSample
import androidx.wear.compose.material3.samples.TimePickerWithMinutesAndSecondsSample
import androidx.wear.compose.material3.samples.TimePickerWithSecondsSample
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
val TimePickerDemos =
    listOf(
        ComposableDemo("Time HH:MM:SS") { TimePickerWithSecondsSample() },
        ComposableDemo("Time HH:MM") { TimePicker24hWithoutSecondsDemo() },
        ComposableDemo("Time MM:SS") { TimePickerWithMinutesAndSecondsSample() },
        ComposableDemo("Time 12 Hour") { TimePickerWith12HourClockSample() },
        ComposableDemo("Time System time format") { TimePickerSample() },
    )

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun TimePicker24hWithoutSecondsDemo() {
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
            initialTime = timePickerTime,
        )
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(
                onClick = { showTimePicker = true },
                label = { Text("Selected Time") },
                secondaryLabel = { Text(timePickerTime.format(formatter)) },
                icon = { Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit") },
            )
        }
    }
}
