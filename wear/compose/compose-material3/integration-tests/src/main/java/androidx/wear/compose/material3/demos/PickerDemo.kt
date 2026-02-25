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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.integration.demos.common.ComposableDemo
import androidx.wear.compose.material3.Picker
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.rememberPickerState
import androidx.wear.compose.material3.samples.AutoCenteringPickerGroup
import androidx.wear.compose.material3.samples.PickerAnimateScrollToOption
import androidx.wear.compose.material3.samples.PickerGroupSample
import androidx.wear.compose.material3.samples.SimplePicker

val PickerDemos =
    listOf(
        ComposableDemo("Simple Picker") { SimplePicker() },
        ComposableDemo("No gradient") { PickerWithoutGradient() },
        ComposableDemo("Animate picker change") { PickerAnimateScrollToOption() },
        ComposableDemo("Sample Picker Group") { PickerGroupSample() },
        ComposableDemo("Auto-centering Picker Group") { AutoCenteringPickerGroup() },
        ComposableDemo("Custom Background") { CustomBackgroundSimplePicker() },
    )

@Composable
fun PickerWithoutGradient() {
    val items = listOf("One", "Two", "Three", "Four", "Five")
    val state = rememberPickerState(items.size)
    val contentDescription by remember { derivedStateOf { "${state.selectedOptionIndex + 1}" } }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Picker(
            readOnly = false,
            modifier = Modifier.size(100.dp, 100.dp),
            gradientRatio = 0.0f,
            contentDescription = { contentDescription },
            state = state,
        ) {
            Text(items[it])
        }
    }
}

// To use custom backgrounds(ex: gradients, images), set gradientColor to [Color.Unspecified]
@Composable
fun CustomBackgroundSimplePicker() {
    // This picker is similar to SimplePicker sample
    val items = listOf("One", "Two", "Three", "Four", "Five")
    val state = rememberPickerState(items.size)
    Box(
        modifier =
            Modifier.fillMaxSize()
                .scrollable(
                    state = state,
                    orientation = Orientation.Vertical,
                    reverseDirection = true,
                )
                .background(Brush.linearGradient(listOf(Color.Red, Color.Blue, Color.Green))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp),
            text = "Selected: ${items[state.selectedOptionIndex]}",
        )
        Picker(
            modifier = Modifier.size(100.dp, 100.dp),
            state = state,
            contentDescription = { "${state.selectedOptionIndex + 1}" },
            gradientColor = Color.Unspecified,
        ) {
            Text(items[it])
        }
    }
}
