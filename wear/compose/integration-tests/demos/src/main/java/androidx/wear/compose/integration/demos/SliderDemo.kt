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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.InlineSlider
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults

@Composable
fun InlineSliderDemo() {
    var valueWithoutSegments by remember { mutableStateOf(5f) }
    var valueWithSegments by remember { mutableStateOf(2f) }
    var enabled by remember { mutableStateOf(true) }

    ScalingLazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = 4.dp,
            alignment = Alignment.CenterVertically
        ),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 30.dp),
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
                toggleIcon = { ToggleChipDefaults.SwitchIcon(checked = enabled) }
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