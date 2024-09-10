/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.integration.demos.common.Centralize
import androidx.wear.compose.integration.demos.common.ComposableDemo
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Slider
import androidx.wear.compose.material3.SliderColors
import androidx.wear.compose.material3.SliderDefaults
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.samples.ChangedSliderSample
import androidx.wear.compose.material3.samples.SliderSample
import androidx.wear.compose.material3.samples.SliderSegmentedSample
import androidx.wear.compose.material3.samples.SliderWithIntegerSample

val SliderDemos =
    listOf(
        ComposableDemo("Slider") {
            Centralize(Modifier.padding(horizontal = 10.dp)) { SliderSample() }
        },
        ComposableDemo("Segmented slider") {
            Centralize(Modifier.padding(horizontal = 10.dp)) { SliderSegmentedSample() }
        },
        ComposableDemo("Integer slider") {
            Centralize(Modifier.padding(horizontal = 10.dp)) { SliderWithIntegerSample() }
        },
        ComposableDemo("Color changing slider") {
            Centralize(Modifier.padding(horizontal = 10.dp)) { ChangedSliderSample() }
        },
        ComposableDemo("Slider Demo") { SliderDemo() },
        ComposableDemo("RTL slider") { SliderRTLDemo() },
        ComposableDemo("Slider segmented") { SliderDemo(segmented = true) },
        ComposableDemo("With custom color") { SliderCustomColorsDemo() },
        ComposableDemo("Slider with integers") { SliderWithIntegersDemo() },
    )

@Composable
fun SliderDemo(segmented: Boolean = false) {
    var enabledValue by remember { mutableFloatStateOf(5f) }
    var disabledValue by remember { mutableFloatStateOf(5f) }
    val scrollState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = scrollState) {
        ScalingLazyColumn(
            state = scrollState,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.spacedBy(space = 4.dp, alignment = Alignment.CenterVertically),
            modifier = Modifier.fillMaxSize(),
            autoCentering = AutoCenteringParams(itemIndex = 0)
        ) {
            item { Text("Enabled Slider, value = $enabledValue") }
            item {
                DefaultSlider(
                    value = enabledValue,
                    enabled = true,
                    valueRange = 1f..9f,
                    steps = 7,
                    segmented = segmented,
                    onValueChange = { enabledValue = it }
                )
            }
            item { Text("Disabled Slider, value = $disabledValue") }
            item {
                DefaultSlider(
                    value = disabledValue,
                    enabled = false,
                    onValueChange = { disabledValue = it },
                    valueRange = 1f..9f,
                    segmented = segmented,
                    steps = 7
                )
            }
        }
    }
}

@Composable
fun SliderWithIntegersDemo() {
    var valueWithoutSegments by remember { mutableIntStateOf(5) }
    var valueWithSegments by remember { mutableIntStateOf(5) }
    val scrollState = rememberScalingLazyListState()
    ScreenScaffold(scrollState = scrollState) {
        ScalingLazyColumn(
            state = scrollState,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement =
                Arrangement.spacedBy(space = 4.dp, alignment = Alignment.CenterVertically),
            modifier = Modifier.fillMaxSize(),
            autoCentering = AutoCenteringParams(itemIndex = 0)
        ) {
            item { Text("No segments, value = $valueWithoutSegments") }
            item {
                DefaultSlider(
                    value = valueWithoutSegments,
                    valueProgression = IntProgression.fromClosedRange(0, 15, 3),
                    segmented = false,
                    onValueChange = { valueWithoutSegments = it }
                )
            }
            item { Text("With segments, value = $valueWithSegments") }
            item {
                DefaultSlider(
                    value = valueWithSegments,
                    onValueChange = { valueWithSegments = it },
                    valueProgression = IntProgression.fromClosedRange(0, 15, 3),
                    segmented = true
                )
            }
        }
    }
}

@Composable
fun SliderRTLDemo() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) { SliderDemo() }
}

@Composable
fun SliderCustomColorsDemo() {
    var value by remember { mutableFloatStateOf(4.5f) }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(10.dp)) {
        DefaultSlider(
            value = value,
            onValueChange = { value = it },
            valueRange = 3f..6f,
            steps = 5,
            segmented = true,
            colors =
                SliderDefaults.sliderColors(
                    containerColor = Color.Green,
                    buttonIconColor = Color.Yellow,
                    selectedBarColor = Color.Magenta,
                    unselectedBarColor = Color.White,
                    selectedBarSeparatorColor = Color.Cyan,
                    unselectedBarSeparatorColor = Color.Magenta,
                    disabledContainerColor = Color.DarkGray,
                    disabledButtonIconColor = Color.LightGray,
                    disabledSelectedBarColor = Color.Red,
                    disabledUnselectedBarColor = Color.Blue,
                    disabledSelectedBarSeparatorColor = Color.Gray,
                    disabledUnselectedBarSeparatorColor = Color.Gray,
                ),
        )
    }
}

@Composable
fun DefaultSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    steps: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..(steps + 1).toFloat(),
    segmented: Boolean = steps <= 8,
    colors: SliderColors = SliderDefaults.sliderColors(),
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        segmented = segmented,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        steps = steps
    )
}

@Composable
fun DefaultSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    valueProgression: IntProgression,
    segmented: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SliderColors = SliderDefaults.sliderColors(),
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueProgression = valueProgression,
        segmented = segmented,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
    )
}
