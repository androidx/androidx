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

package androidx.wear.compose.material3.samples

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.Sampled
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
import androidx.compose.ui.platform.LocalLocale
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.DurationPicker
import androidx.wear.compose.material3.DurationPickerType
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import java.time.Duration

@RequiresApi(Build.VERSION_CODES.O)
@Sampled
@Composable
fun DurationPickerHoursMinutesSecondsSample() {
    var showDurationPicker by remember { mutableStateOf(true) }
    var duration by remember { mutableStateOf(Duration.ofHours(1).plusMinutes(30)) }

    if (showDurationPicker) {
        DurationPicker(
            initialDuration = duration,
            onDurationPicked = {
                duration = it
                showDurationPicker = false
            },
            durationPickerType = DurationPickerType.HoursMinutesSeconds,
        )
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val hours = duration.toHours()
            val minutes = duration.toMinutes() % 60
            val seconds = duration.seconds % 60
            val labelText =
                "%02d:%02d:%02d".format(LocalLocale.current.platformLocale, hours, minutes, seconds)

            Button(
                onClick = { showDurationPicker = true },
                label = { Text("Selected Duration") },
                secondaryLabel = { Text(labelText) },
                icon = { Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit") },
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Sampled
@Composable
fun DurationPickerMinutesSecondsSample() {
    var showDurationPicker by remember { mutableStateOf(true) }
    var duration by remember { mutableStateOf(Duration.ofMinutes(5)) }

    if (showDurationPicker) {
        DurationPicker(
            initialDuration = duration,
            onDurationPicked = {
                duration = it
                showDurationPicker = false
            },
            durationPickerType = DurationPickerType.MinutesSeconds,
        )
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val minutes = duration.toMinutes()
            val seconds = duration.seconds % 60
            val labelText = "%02d:%02d".format(LocalLocale.current.platformLocale, minutes, seconds)

            Button(
                onClick = { showDurationPicker = true },
                label = { Text("Selected Duration") },
                secondaryLabel = { Text(labelText) },
                icon = { Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit") },
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Sampled
@Composable
fun DurationPickerHoursMinutesSample() {
    var showDurationPicker by remember { mutableStateOf(true) }
    var duration by remember { mutableStateOf(Duration.ofHours(2)) }

    if (showDurationPicker) {
        DurationPicker(
            initialDuration = duration,
            onDurationPicked = {
                duration = it
                showDurationPicker = false
            },
            durationPickerType = DurationPickerType.HoursMinutes,
        )
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val hours = duration.toHours()
            val minutes = duration.toMinutes() % 60
            val labelText = "%02d:%02d".format(LocalLocale.current.platformLocale, hours, minutes)

            Button(
                onClick = { showDurationPicker = true },
                label = { Text("Selected Duration") },
                secondaryLabel = { Text(labelText) },
                icon = { Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit") },
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Sampled
@Composable
fun DurationPickerMaximumDurationSample() {
    var showDurationPicker by remember { mutableStateOf(true) }
    var duration by remember { mutableStateOf(Duration.ofHours(36).plusMinutes(45)) }

    if (showDurationPicker) {
        DurationPicker(
            initialDuration = duration,
            onDurationPicked = {
                duration = it
                showDurationPicker = false
            },
            maxValidDuration = Duration.ofHours(45).plusMinutes(30),
            durationPickerType = DurationPickerType.HoursMinutes,
        )
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val hours = duration.toHours()
            val minutes = duration.toMinutes() % 60
            val labelText = "%02d:%02d".format(LocalLocale.current.platformLocale, hours, minutes)

            Button(
                onClick = { showDurationPicker = true },
                label = { Text("Selected Duration") },
                secondaryLabel = { Text(labelText) },
                icon = { Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit") },
            )
        }
    }
}
