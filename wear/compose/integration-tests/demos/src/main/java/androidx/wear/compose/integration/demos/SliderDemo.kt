/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.integration.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.InlineSlider
import androidx.wear.compose.material.InlineSliderColors
import androidx.wear.compose.material.InlineSliderDefaults
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults

@Composable
fun InlineSliderDemo() {
    var valueWithoutSegments by remember { mutableFloatStateOf(5f) }
    var valueWithSegments by remember { mutableFloatStateOf(2f) }
    var enabled by remember { mutableStateOf(true) }

    ScalingLazyColumnWithRSB(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = 4.dp,
            alignment = Alignment.CenterVertically
        ),
        modifier = Modifier.fillMaxSize(),
        autoCentering = AutoCenteringParams(itemIndex = 0)
    ) {
        item { Text("No segments, value = $valueWithoutSegments") }
        item {
            DefaultInlineSlider(
                value = valueWithoutSegments,
                enabled = enabled,
                valueRange = 1f..100f,
                steps = 98,
                onValueChange = { valueWithoutSegments = it })
        }
        item { Text("With segments, value = $valueWithSegments") }
        item {
            DefaultInlineSlider(
                value = valueWithSegments,
                enabled = enabled,
                onValueChange = { valueWithSegments = it },
                valueRange = 1f..10f,
                steps = 8,
                segmented = true
            )
        }
        item {
            ToggleChip(
                checked = enabled,
                onCheckedChange = { enabled = it },
                label = { Text("Sliders enabled") },
                // For Switch  toggle controls the Wear Material UX guidance is to set the
                // unselected toggle control color to ToggleChipDefaults.switchUncheckedIconColor()
                // rather than the default.
                colors = ToggleChipDefaults.toggleChipColors(
                    uncheckedToggleControlColor = ToggleChipDefaults.SwitchUncheckedIconColor
                ),
                toggleControl = {
                    Icon(
                        imageVector = ToggleChipDefaults.switchIcon(checked = enabled),
                        contentDescription = if (enabled) "On" else "Off"
                    )
                }
            )
        }
    }
}

@Composable
fun InlineSliderWithIntegersDemo() {
    var valueWithoutSegments by remember { mutableIntStateOf(5) }
    var valueWithSegments by remember { mutableIntStateOf(2) }

    ScalingLazyColumnWithRSB(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = 4.dp,
            alignment = Alignment.CenterVertically
        ),
        modifier = Modifier.fillMaxSize(),
        autoCentering = AutoCenteringParams(itemIndex = 0)
    ) {
        item { Text("No segments, value = $valueWithoutSegments") }
        item {
            DefaultInlineSlider(
                value = valueWithoutSegments,
                valueProgression = IntProgression.fromClosedRange(0, 15, 3),
                segmented = false,
                onValueChange = { valueWithoutSegments = it })
        }
        item { Text("With segments, value = $valueWithSegments") }
        item {
            DefaultInlineSlider(
                value = valueWithSegments,
                onValueChange = { valueWithSegments = it },
                valueProgression = IntProgression.fromClosedRange(110, 220, 5),
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

@Composable
fun InlineSliderCustomColorsDemo() {
    var value by remember { mutableFloatStateOf(4.5f) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        DefaultInlineSlider(
            value = value,
            onValueChange = { value = it },
            valueRange = 3f..6f,
            steps = 5,
            segmented = false,
            colors = InlineSliderDefaults.colors(
                selectedBarColor = AlternatePrimaryColor1,
            ),
            modifier = Modifier.padding(horizontal = 10.dp)
        )
    }
}

@Composable
fun InlineSliderSegmented() {
    var numberOfSegments by remember { mutableFloatStateOf(5f) }
    var progress by remember { mutableFloatStateOf(10f) }

    ScalingLazyColumnWithRSB(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = 4.dp,
            alignment = Alignment.CenterVertically
        ),
        modifier = Modifier.fillMaxSize(),
        autoCentering = AutoCenteringParams(itemIndex = 0)
    ) {
        item { Text("Num of segments ${numberOfSegments.toInt()}") }

        item {
            DefaultInlineSlider(
                value = numberOfSegments,
                valueRange = 0f..30f,
                onValueChange = { numberOfSegments = it },
                steps = 29
            )
        }

        item { Text("Progress: $progress/20") }

        item {
            DefaultInlineSlider(
                value = progress,
                onValueChange = { progress = it },
                valueRange = 1f..20f,
                segmented = numberOfSegments <= 8,
                steps = numberOfSegments.toInt()
            )
        }
    }
}

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

@Composable
fun DefaultInlineSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueProgression: IntProgression,
    segmented: Boolean,
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
