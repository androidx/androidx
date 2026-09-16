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

package androidx.wear.compose.material3.demos

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.wear.compose.integration.demos.common.ComposableDemo
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.DurationPicker
import androidx.wear.compose.material3.DurationPickerType
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.samples.DurationPickerHoursMinutesSample
import androidx.wear.compose.material3.samples.DurationPickerHoursMinutesSecondsSample
import androidx.wear.compose.material3.samples.DurationPickerMaximumDurationSample
import androidx.wear.compose.material3.samples.DurationPickerMinutesSecondsSample
import java.time.Duration

@RequiresApi(Build.VERSION_CODES.O)
val DurationPickerDemos =
    listOf(
        ComposableDemo("Duration HH:MM:SS") { DurationPickerHoursMinutesSecondsSample() },
        ComposableDemo("Duration MM:SS") { DurationPickerMinutesSecondsSample() },
        ComposableDemo("Duration HH:MM") { DurationPickerHoursMinutesSample() },
        ComposableDemo("Max Duration (45h 30m)") { DurationPickerMaximumDurationSample() },
        ComposableDemo("Min Duration (1h 15m 30s)") { DurationPickerMinimumDurationDemo() },
        ComposableDemo("Custom Background") { CustomBackgroundDurationPicker() },
    )

/**
 * Demonstrates minimum duration bounds (`01:15:30`) when `initialDuration` is below
 * `minValidDuration` (such as [Duration.ZERO] when creating a new timer).
 *
 * On initial display, the picker opens at `00:00:00` with the confirm button disabled and options
 * below the minimum bound colored with `invalidPickerContentColor`. Auto-scrolling to the nearest
 * valid options is enabled after the first scroll whenever scrolling settles on an invalid
 * combination.
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun DurationPickerMinimumDurationDemo() {
    var showDurationPicker by remember { mutableStateOf(true) }
    var duration by remember { mutableStateOf(Duration.ZERO) }

    if (showDurationPicker) {
        DurationPicker(
            initialDuration = duration,
            onDurationPicked = {
                duration = it
                showDurationPicker = false
            },
            minValidDuration = Duration.ofHours(1).plusMinutes(15).plusSeconds(30),
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

// pickerBackgroundColor defaults to Color.Unspecified, which works over custom backgrounds (ex:
// gradients, images)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun CustomBackgroundDurationPicker() {
    var showDurationPicker by remember { mutableStateOf(true) }
    var duration by remember { mutableStateOf(Duration.ofHours(1).plusMinutes(30)) }

    if (showDurationPicker) {
        Box(
            Modifier.fillMaxSize()
                .background(Brush.linearGradient(listOf(Color.Red, Color.Blue, Color.Green)))
        ) {
            DurationPicker(
                initialDuration = duration,
                onDurationPicked = {
                    duration = it
                    showDurationPicker = false
                },
                durationPickerType = DurationPickerType.HoursMinutesSeconds,
            )
        }
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
