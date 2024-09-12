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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.integration.demos.common.Centralize
import androidx.wear.compose.integration.demos.common.ComposableDemo
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.CircularProgressIndicatorDefaults
import androidx.wear.compose.material3.ExperimentalWearMaterial3Api
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.InlineSlider
import androidx.wear.compose.material3.InlineSliderDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.SegmentedCircularProgressIndicator
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.samples.FullScreenProgressIndicatorSample
import androidx.wear.compose.material3.samples.IndeterminateProgressIndicatorSample
import androidx.wear.compose.material3.samples.LinearProgressIndicatorSample
import androidx.wear.compose.material3.samples.MediaButtonProgressIndicatorSample
import androidx.wear.compose.material3.samples.OverflowProgressIndicatorSample
import androidx.wear.compose.material3.samples.SegmentedProgressIndicatorBinarySample
import androidx.wear.compose.material3.samples.SegmentedProgressIndicatorSample
import androidx.wear.compose.material3.samples.SmallSegmentedProgressIndicatorSample
import androidx.wear.compose.material3.samples.SmallValuesProgressIndicatorSample

val ProgressIndicatorDemos =
    listOf(
        ComposableDemo("Full screen") { Centralize { FullScreenProgressIndicatorSample() } },
        ComposableDemo("Media button wrapping") {
            Centralize { MediaButtonProgressIndicatorSample() }
        },
        ComposableDemo("Overflow progress (>100%)") {
            Centralize { OverflowProgressIndicatorSample() }
        },
        ComposableDemo("Small sized indicator") {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            ) {
                Button({ /* No op */ }, modifier = Modifier.align(Alignment.Center)) {
                    Text("Loading...", modifier = Modifier.align(Alignment.CenterVertically))
                    Spacer(modifier = Modifier.size(10.dp))
                    CircularProgressIndicator(
                        progress = { 0.75f },
                        modifier = Modifier.size(IconButtonDefaults.DefaultButtonSize),
                        startAngle = 120f,
                        endAngle = 60f,
                        strokeWidth = CircularProgressIndicatorDefaults.smallStrokeWidth,
                        colors = ProgressIndicatorDefaults.colors(indicatorColor = Color.Red)
                    )
                }
            }
        },
        ComposableDemo("Small progress values") {
            Centralize { SmallValuesProgressIndicatorSample() }
        },
        ComposableDemo("Indeterminate progress") {
            Centralize { IndeterminateProgressIndicatorSample() }
        },
        ComposableDemo("Segmented progress") { Centralize { SegmentedProgressIndicatorSample() } },
        ComposableDemo("Segmented binary") {
            Centralize { SegmentedProgressIndicatorBinarySample() }
        },
        ComposableDemo("Small segmented progress") {
            Centralize { SmallSegmentedProgressIndicatorSample() }
        },
        ComposableDemo("Custom circular progress") {
            Centralize { CircularProgressCustomisableFullScreenDemo() }
        },
        ComposableDemo("Custom segmented progress") {
            Centralize { SegmentedProgressCustomisableFullScreenDemo() }
        },
        ComposableDemo("Linear progress indicator") {
            Centralize { LinearProgressIndicatorSamples() }
        },
    )

@Composable
fun LinearProgressIndicatorSamples() {
    Box(modifier = Modifier.background(MaterialTheme.colorScheme.background).fillMaxSize()) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item { ListHeader { Text("Progress 0%") } }
            item { LinearProgressIndicatorSample(progress = { 0f }) }
            item { ListHeader { Text("Progress 1%") } }
            item { LinearProgressIndicatorSample(progress = { 0.01f }) }
            item { ListHeader { Text("Progress 50%") } }
            item { LinearProgressIndicatorSample(progress = { 0.5f }) }
            item { ListHeader { Text("Progress 100%") } }
            item { LinearProgressIndicatorSample(progress = { 1f }) }
            item { ListHeader { Text("Disabled 0%") } }
            item { LinearProgressIndicatorSample(progress = { 0f }, enabled = false) }
            item { ListHeader { Text("Disabled 50%") } }
            item { LinearProgressIndicatorSample(progress = { 0.5f }, enabled = false) }
            item { ListHeader { Text("Disabled 100%") } }
            item { LinearProgressIndicatorSample(progress = { 1f }, enabled = false) }
        }
    }
}

@Composable
fun CircularProgressCustomisableFullScreenDemo() {
    val progress = remember { mutableFloatStateOf(0.4f) }
    val startAngle = remember { mutableFloatStateOf(360f) }
    val endAngle = remember { mutableFloatStateOf(360f) }
    val enabled = remember { mutableStateOf(true) }
    val overflowAllowed = remember { mutableStateOf(true) }
    val hasLargeStroke = remember { mutableStateOf(true) }
    val hasCustomColors = remember { mutableStateOf(false) }
    val colors =
        if (hasCustomColors.value) {
            ProgressIndicatorDefaults.colors(
                indicatorColor = Color.Green,
                trackColor = Color.Green.copy(alpha = 0.5f),
                overflowTrackColor = Color.Green.copy(alpha = 0.7f),
            )
        } else {
            ProgressIndicatorDefaults.colors()
        }
    val strokeWidth =
        if (hasLargeStroke.value) CircularProgressIndicatorDefaults.largeStrokeWidth
        else CircularProgressIndicatorDefaults.smallStrokeWidth

    Box(
        modifier =
            Modifier.background(MaterialTheme.colorScheme.background)
                .padding(CircularProgressIndicatorDefaults.FullScreenPadding)
                .fillMaxSize()
    ) {
        ProgressIndicatorCustomizer(
            progress = progress,
            startAngle = startAngle,
            endAngle = endAngle,
            enabled = enabled,
            overflowAllowed = overflowAllowed,
            hasLargeStroke = hasLargeStroke,
            hasCustomColors = hasCustomColors,
        )

        CircularProgressIndicator(
            progress = { progress.value },
            startAngle = startAngle.value,
            endAngle = endAngle.value,
            enabled = enabled.value,
            allowProgressOverflow = overflowAllowed.value,
            strokeWidth = strokeWidth,
            colors = colors,
        )
    }
}

@Composable
fun SegmentedProgressCustomisableFullScreenDemo() {
    val progress = remember { mutableFloatStateOf(0f) }
    val startAngle = remember { mutableFloatStateOf(0f) }
    val endAngle = remember { mutableFloatStateOf(0f) }
    val enabled = remember { mutableStateOf(true) }
    val overflowAllowed = remember { mutableStateOf(true) }
    val hasCustomColors = remember { mutableStateOf(false) }
    val hasLargeStroke = remember { mutableStateOf(true) }
    val numSegments = remember { mutableIntStateOf(5) }
    val colors =
        if (hasCustomColors.value) {
            ProgressIndicatorDefaults.colors(
                indicatorColor = Color.Green,
                trackColor = Color.Green.copy(alpha = 0.5f),
                overflowTrackColor = Color.Green.copy(alpha = 0.7f),
            )
        } else {
            ProgressIndicatorDefaults.colors()
        }
    val strokeWidth =
        if (hasLargeStroke.value) CircularProgressIndicatorDefaults.largeStrokeWidth
        else CircularProgressIndicatorDefaults.smallStrokeWidth

    Box(
        modifier =
            Modifier.background(MaterialTheme.colorScheme.background)
                .padding(CircularProgressIndicatorDefaults.FullScreenPadding)
                .fillMaxSize()
    ) {
        ProgressIndicatorCustomizer(
            progress = progress,
            startAngle = startAngle,
            endAngle = endAngle,
            enabled = enabled,
            hasLargeStroke = hasLargeStroke,
            hasCustomColors = hasCustomColors,
            numSegments = numSegments,
            overflowAllowed = overflowAllowed
        )

        SegmentedCircularProgressIndicator(
            segmentCount = numSegments.value,
            progress = { progress.value },
            startAngle = startAngle.value,
            endAngle = endAngle.value,
            enabled = enabled.value,
            allowProgressOverflow = overflowAllowed.value,
            strokeWidth = strokeWidth,
            colors = colors,
        )
    }
}

@OptIn(ExperimentalWearMaterial3Api::class)
@Composable
fun ProgressIndicatorCustomizer(
    progress: MutableState<Float>,
    startAngle: MutableState<Float>,
    endAngle: MutableState<Float>,
    enabled: MutableState<Boolean>,
    hasLargeStroke: MutableState<Boolean>,
    hasCustomColors: MutableState<Boolean>,
    overflowAllowed: MutableState<Boolean>,
    numSegments: MutableState<Int>? = null,
) {
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item { Text(String.format("Progress: %.0f%%", progress.value * 100)) }
        item {
            InlineSlider(
                value = progress.value,
                onValueChange = { progress.value = it },
                increaseIcon = { Icon(InlineSliderDefaults.Increase, "Increase") },
                decreaseIcon = { Icon(InlineSliderDefaults.Decrease, "Decrease") },
                valueRange = 0f..2f,
                steps = 9,
                colors =
                    InlineSliderDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                segmented = false
            )
        }
        if (numSegments != null) {
            item { Text("Segments: ${numSegments.value}") }
            item {
                InlineSlider(
                    value = numSegments.value.toFloat(),
                    onValueChange = { numSegments.value = it.toInt() },
                    increaseIcon = { Icon(InlineSliderDefaults.Increase, "Increase") },
                    decreaseIcon = { Icon(InlineSliderDefaults.Decrease, "Decrease") },
                    valueRange = 1f..12f,
                    steps = 10,
                    colors =
                        InlineSliderDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.background,
                        ),
                )
            }
        }
        item { Text("Start Angle: ${startAngle.value.toInt()}") }
        item {
            InlineSlider(
                value = startAngle.value,
                onValueChange = { startAngle.value = it },
                increaseIcon = { Icon(InlineSliderDefaults.Increase, "Increase") },
                decreaseIcon = { Icon(InlineSliderDefaults.Decrease, "Decrease") },
                valueRange = 0f..360f,
                steps = 7,
                segmented = false,
                colors =
                    InlineSliderDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
            )
        }
        item { Text("End angle: ${endAngle.value.toInt()}") }
        item {
            InlineSlider(
                value = endAngle.value,
                onValueChange = { endAngle.value = it },
                increaseIcon = { Icon(InlineSliderDefaults.Increase, "Increase") },
                decreaseIcon = { Icon(InlineSliderDefaults.Decrease, "Decrease") },
                valueRange = 0f..360f,
                steps = 7,
                segmented = false,
                colors =
                    InlineSliderDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
            )
        }
        item {
            SwitchButton(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                checked = enabled.value,
                onCheckedChange = { enabled.value = it },
                label = { Text("Enabled") },
            )
        }
        item {
            SwitchButton(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                checked = hasLargeStroke.value,
                onCheckedChange = { hasLargeStroke.value = it },
                label = { Text("Large stroke") },
            )
        }
        item {
            SwitchButton(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                checked = hasCustomColors.value,
                onCheckedChange = { hasCustomColors.value = it },
                label = { Text("Custom colors") },
            )
        }
        item {
            SwitchButton(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                checked = overflowAllowed.value,
                onCheckedChange = { overflowAllowed.value = it },
                label = { Text("Overflow") },
            )
        }
    }
}
