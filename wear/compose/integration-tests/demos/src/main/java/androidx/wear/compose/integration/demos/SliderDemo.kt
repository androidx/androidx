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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.InlineSlider
import androidx.wear.compose.material.InlineSliderDefaults
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults

@Composable
fun InlineSliderDemo() {
    var valueWithoutSegments by remember { mutableStateOf(5f) }
    var valueWithSegments by remember { mutableStateOf(2f) }
    var enabled by remember { mutableStateOf(true) }

    ScalingLazyColumnWithRSB(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = 4.dp,
            alignment = Alignment.CenterVertically
        ),
        modifier = Modifier.fillMaxSize()
    ) {
        item { Text("No segments, value = $valueWithoutSegments") }
        item {
            InlineSlider(
                value = valueWithoutSegments,
                enabled = enabled,
                valueRange = 1f..100f,
                steps = 98,
                onValueChange = { valueWithoutSegments = it })
        }
        item { Text("With segments, value = $valueWithSegments") }
        item {
            InlineSlider(
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
                toggleControl = { ToggleChipDefaults.SwitchIcon(checked = enabled) }
            )
        }
    }
}

@Composable
fun InlineSliderWithIntegersDemo() {
    var valueWithoutSegments by remember { mutableStateOf(5) }
    var valueWithSegments by remember { mutableStateOf(2) }

    ScalingLazyColumnWithRSB(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = 4.dp,
            alignment = Alignment.CenterVertically
        ),
        modifier = Modifier.fillMaxSize()
    ) {
        item { Text("No segments, value = $valueWithoutSegments") }
        item {
            InlineSlider(
                value = valueWithoutSegments,
                valueProgression = IntProgression.fromClosedRange(0, 15, 3),
                segmented = false,
                onValueChange = { valueWithoutSegments = it })
        }
        item { Text("With segments, value = $valueWithSegments") }
        item {
            InlineSlider(
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
    var value by remember { mutableStateOf(4.5f) }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        InlineSlider(
            value = value,
            onValueChange = { value = it },
            valueRange = 3f..6f,
            steps = 5,
            segmented = false,
            colors = InlineSliderDefaults.colors(
                backgroundColor = Color.Green,
                spacerColor = Color.Yellow,
                selectedBarColor = Color.Magenta,
                unselectedBarColor = Color.White,
                disabledBackgroundColor = Color.DarkGray,
                disabledSpacerColor = Color.LightGray,
                disabledSelectedBarColor = Color.Red,
                disabledUnselectedBarColor = Color.Blue
            )
        )
    }
}

@Composable
fun InlineSliderSegmented() {
    var numberOfSegments by remember { mutableStateOf(5f) }
    var progress by remember { mutableStateOf(10f) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = 4.dp,
            alignment = Alignment.CenterVertically
        ),
        modifier = Modifier.fillMaxSize()
    ) {
        Text("Num of segments ${numberOfSegments.toInt()}")

        InlineSlider(
            value = numberOfSegments,
            valueRange = 0f..30f,
            onValueChange = { numberOfSegments = it },
            steps = 29
        )

        Text("Progress: $progress/20")

        InlineSlider(
            value = progress,
            onValueChange = { progress = it },
            valueRange = 1f..20f,
            segmented = numberOfSegments <= 8,
            steps = numberOfSegments.toInt()
        )
    }
}
