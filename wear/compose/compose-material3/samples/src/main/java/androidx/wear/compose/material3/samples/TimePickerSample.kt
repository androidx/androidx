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

package androidx.wear.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimePicker
import androidx.wear.compose.material3.TimePickerType
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Sampled
@Composable
fun TimePickerSample() {
    var showTimePicker by remember { mutableStateOf(true) }
    var timePickerTime by remember { mutableStateOf(LocalTime.now()) }
    val formatter =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
            .withLocale(LocalConfiguration.current.locales[0])
    if (showTimePicker) {
        TimePicker(
            onTimePicked = {
                timePickerTime = it
                showTimePicker = false
            },
            initialTime = timePickerTime // Initialize with last picked time on reopen
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
                icon = { Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit") },
            )
        }
    }
}

@Sampled
@Composable
fun TimePickerWithSecondsSample() {
    var showTimePicker by remember { mutableStateOf(true) }
    var timePickerTime by remember { mutableStateOf(LocalTime.now()) }
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    if (showTimePicker) {
        TimePicker(
            onTimePicked = {
                timePickerTime = it
                showTimePicker = false
            },
            timePickerType = TimePickerType.HoursMinutesSeconds24H,
            initialTime = timePickerTime // Initialize with last picked time on reopen
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
                icon = { Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit") },
            )
        }
    }
}

@Sampled
@Composable
fun TimePickerWith12HourClockSample() {
    var showTimePicker by remember { mutableStateOf(true) }
    var timePickerTime by remember { mutableStateOf(LocalTime.now()) }
    val formatter = DateTimeFormatter.ofPattern("hh:mm a")
    if (showTimePicker) {
        TimePicker(
            onTimePicked = {
                timePickerTime = it
                showTimePicker = false
            },
            timePickerType = TimePickerType.HoursMinutesAmPm12H,
            initialTime = timePickerTime // Initialize with last picked time on reopen
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
                icon = { Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit") },
            )
        }
    }
}
