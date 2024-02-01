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
import androidx.wear.compose.integration.demos.common.Centralize
import androidx.wear.compose.integration.demos.common.ComposableDemo
import androidx.wear.compose.integration.demos.common.DemoCategory
import androidx.wear.compose.integration.demos.common.ScalingLazyColumnWithRSB
import androidx.wear.compose.material3.ExperimentalWearMaterial3Api
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.InlineSlider
import androidx.wear.compose.material3.InlineSliderColors
import androidx.wear.compose.material3.InlineSliderDefaults
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.samples.InlineSliderSample
import androidx.wear.compose.material3.samples.InlineSliderSegmentedSample
import androidx.wear.compose.material3.samples.InlineSliderWithIntegerSample

val SliderDemos =
    listOf(
        DemoCategory(
            "Samples",
            listOf(
                ComposableDemo("Inline slider") {
                    Centralize(Modifier.padding(horizontal = 10.dp)) {
                        InlineSliderSample()
                    }
                },
                ComposableDemo("Segmented inline slider") {
                    Centralize(Modifier.padding(horizontal = 10.dp)) {
                        InlineSliderSegmentedSample()
                    }
                },
                ComposableDemo("Integer inline slider") {
                    Centralize(Modifier.padding(horizontal = 10.dp)) {
                        InlineSliderWithIntegerSample()
                    }
                },
            )
        ),
        DemoCategory(
            "Demos",
            listOf(
                ComposableDemo("Inline slider") { InlineSliderDemo() },
                ComposableDemo("RTL Inline slider") { InlineSliderRTLDemo() },
                ComposableDemo("Inline slider segmented") { InlineSliderDemo(segmented = true) },
                ComposableDemo("With custom color") { InlineSliderCustomColorsDemo() },
                ComposableDemo("Inline slider with integers") {
                    InlineSliderWithIntegersDemo()
                },
            )
        )
    )

@OptIn(ExperimentalWearMaterial3Api::class)
@Composable
fun InlineSliderDemo(segmented: Boolean = false) {
    var enabledValue by remember { mutableFloatStateOf(5f) }
    var disabledValue by remember { mutableFloatStateOf(5f) }

    ScalingLazyColumnWithRSB(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = 4.dp,
            alignment = Alignment.CenterVertically
        ),
        modifier = Modifier.fillMaxSize(),
        autoCentering = AutoCenteringParams(itemIndex = 0)
    ) {
        item {
            Text("Enabled Slider, value = $enabledValue")
        }
        item {
            DefaultInlineSlider(
                value = enabledValue,
                enabled = true,
                valueRange = 1f..9f,
                steps = 7,
                segmented = segmented,
                onValueChange = { enabledValue = it }
            )
        }
        item {
            Text("Disabled Slider, value = $disabledValue")
        }
        item {
            DefaultInlineSlider(
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

@OptIn(ExperimentalWearMaterial3Api::class)
@Composable
fun InlineSliderWithIntegersDemo() {
    var valueWithoutSegments by remember { mutableIntStateOf(5) }
    var valueWithSegments by remember { mutableIntStateOf(5) }

    ScalingLazyColumnWithRSB(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = 4.dp,
            alignment = Alignment.CenterVertically
        ),
        modifier = Modifier.fillMaxSize(),
        autoCentering = AutoCenteringParams(itemIndex = 0)
    ) {
        item {
            Text("No segments, value = $valueWithoutSegments")
        }
        item {
            DefaultInlineSlider(
                value = valueWithoutSegments,
                valueProgression = IntProgression.fromClosedRange(0, 15, 3),
                segmented = false,
                onValueChange = { valueWithoutSegments = it })
        }
        item {
            Text("With segments, value = $valueWithSegments")
        }
        item {
            DefaultInlineSlider(
                value = valueWithSegments,
                onValueChange = { valueWithSegments = it },
                valueProgression = IntProgression.fromClosedRange(0, 15, 3),
                segmented = true
            )
        }
    }
}

@Composable
fun InlineSliderRTLDemo() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        InlineSliderDemo()
    }
}

@OptIn(ExperimentalWearMaterial3Api::class)
@Composable
fun InlineSliderCustomColorsDemo() {
    var value by remember { mutableFloatStateOf(4.5f) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)
    ) {
        DefaultInlineSlider(
            value = value,
            onValueChange = { value = it },
            valueRange = 3f..6f,
            steps = 5,
            segmented = true,
            colors = InlineSliderDefaults.colors(
                containerColor = Color.Green,
                buttonIconColor = Color.Yellow,
                selectedBarColor = Color.Magenta,
                unselectedBarColor = Color.White,
                barSeparatorColor = Color.Cyan,
                disabledContainerColor = Color.DarkGray,
                disabledButtonIconColor = Color.LightGray,
                disabledSelectedBarColor = Color.Red,
                disabledUnselectedBarColor = Color.Blue,
                disabledBarSeparatorColor = Color.Gray
            ),
        )
    }
}

@OptIn(ExperimentalWearMaterial3Api::class)
@Composable
fun DefaultInlineSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    steps: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..(steps + 1).toFloat(),
    segmented: Boolean = steps <= 8,
    decreaseIcon: @Composable () -> Unit = { Icon(InlineSliderDefaults.Decrease, "Decrease") },
    increaseIcon: @Composable () -> Unit = { Icon(InlineSliderDefaults.Increase, "Increase") },
    colors: InlineSliderColors = InlineSliderDefaults.colors(),
) {
    InlineSlider(
        value = value,
        onValueChange = onValueChange,
        increaseIcon = increaseIcon,
        decreaseIcon = decreaseIcon,
        valueRange = valueRange,
        segmented = segmented,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        steps = steps
    )
}

@OptIn(ExperimentalWearMaterial3Api::class)
@Composable
fun DefaultInlineSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    valueProgression: IntProgression,
    segmented: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    decreaseIcon: @Composable () -> Unit = { Icon(InlineSliderDefaults.Decrease, "Decrease") },
    increaseIcon: @Composable () -> Unit = { Icon(InlineSliderDefaults.Increase, "Increase") },
    colors: InlineSliderColors = InlineSliderDefaults.colors(),
) {
    InlineSlider(
        value = value,
        onValueChange = onValueChange,
        increaseIcon = increaseIcon,
        decreaseIcon = decreaseIcon,
        valueProgression = valueProgression,
        segmented = segmented,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
    )
}
