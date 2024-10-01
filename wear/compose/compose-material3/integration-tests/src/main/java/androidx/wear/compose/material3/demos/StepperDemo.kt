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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.integration.demos.common.Centralize
import androidx.wear.compose.integration.demos.common.ComposableDemo
import androidx.wear.compose.material3.LevelIndicator
import androidx.wear.compose.material3.LevelIndicatorDefaults
import androidx.wear.compose.material3.Stepper
import androidx.wear.compose.material3.StepperDefaults
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.samples.StepperSample
import androidx.wear.compose.material3.samples.StepperWithButtonSample
import androidx.wear.compose.material3.samples.StepperWithIntegerSample
import androidx.wear.compose.material3.samples.StepperWithRangeSemanticsSample

val StepperDemos =
    listOf(
        ComposableDemo("Stepper") { Centralize { StepperSample() } },
        ComposableDemo("Integer Stepper") { Centralize { StepperWithIntegerSample() } },
        ComposableDemo("Stepper with rangeSemantics") {
            Centralize { StepperWithRangeSemanticsSample() }
        },
        ComposableDemo("Disabled Stepper") { Centralize { DisabledStepperDemo() } },
        ComposableDemo("Custom Colors Stepper") { Centralize { CustomColorsStepperDemo() } },
        ComposableDemo("Stepper with Button") { Centralize { StepperWithButtonSample() } },
    )

@Composable
fun DisabledStepperDemo() {
    var value by remember { mutableFloatStateOf(2f) }
    val valueRange = 0f..4f
    Box(modifier = Modifier.fillMaxSize()) {
        Stepper(
            value = value,
            onValueChange = { value = it },
            valueRange = valueRange,
            steps = 7,
            enabled = false,
        ) {
            Text(String.format("Value: %.1f".format(value)))
        }
        LevelIndicator(
            value = { value },
            valueRange = valueRange,
            modifier = Modifier.align(Alignment.CenterStart),
            enabled = false
        )
    }
}

@Composable
fun CustomColorsStepperDemo() {
    var value by remember { mutableFloatStateOf(2f) }
    val valueRange = 0f..4f

    Box(modifier = Modifier.fillMaxSize()) {
        Stepper(
            value = value,
            onValueChange = { value = it },
            valueRange = valueRange,
            steps = 7,
            colors =
                StepperDefaults.colors(
                    contentColor = Color.Green,
                    buttonContainerColor = Color.Green,
                    buttonIconColor = Color.Black,
                    disabledContentColor = Color.Green.copy(alpha = 0.5f),
                    disabledButtonContainerColor = Color.Green.copy(alpha = 0.5f),
                    disabledButtonIconColor = Color.Black.copy(alpha = 0.5f),
                ),
        ) {
            Text(String.format("Value: %.1f".format(value)))
        }
        LevelIndicator(
            value = { value },
            valueRange = valueRange,
            modifier = Modifier.align(Alignment.CenterStart),
            colors =
                LevelIndicatorDefaults.colors(
                    indicatorColor = Color.Green,
                    trackColor = Color.Green.copy(alpha = 0.5f),
                )
        )
    }
}
