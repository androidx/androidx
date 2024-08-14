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

package androidx.wear.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.wear.compose.material3.ExperimentalWearMaterial3Api
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.LevelIndicator
import androidx.wear.compose.material3.Stepper
import androidx.wear.compose.material3.StepperDefaults
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.rangeSemantics

@Sampled
@OptIn(ExperimentalWearMaterial3Api::class)
@Composable
fun StepperSample() {
    var value by remember { mutableFloatStateOf(2f) }
    val valueRange = 0f..4f
    Box(modifier = Modifier.fillMaxSize()) {
        Stepper(
            value = value,
            onValueChange = { value = it },
            valueRange = valueRange,
            increaseIcon = { Icon(StepperDefaults.Increase, "Increase") },
            decreaseIcon = { Icon(StepperDefaults.Decrease, "Decrease") },
            steps = 7
        ) {
            Text(String.format("Value: %.1f".format(value)))
        }
        LevelIndicator(
            value = { value },
            valueRange = valueRange,
            modifier = Modifier.align(Alignment.CenterStart)
        )
    }
}

@Sampled
@OptIn(ExperimentalWearMaterial3Api::class)
@Composable
fun StepperWithIntegerSample() {
    var value by remember { mutableIntStateOf(3) }
    val valueProgression = 0..10
    Box(modifier = Modifier.fillMaxSize()) {
        Stepper(
            value = value,
            onValueChange = { value = it },
            increaseIcon = { Icon(StepperDefaults.Increase, "Increase") },
            decreaseIcon = { Icon(StepperDefaults.Decrease, "Decrease") },
            valueProgression = valueProgression
        ) {
            Text(String.format("Value: %d".format(value)))
        }
        LevelIndicator(
            value = { value },
            valueProgression = valueProgression,
            modifier = Modifier.align(Alignment.CenterStart)
        )
    }
}

@Sampled
@OptIn(ExperimentalWearMaterial3Api::class)
@Composable
fun StepperWithRangeSemanticsSample() {
    var value by remember { mutableFloatStateOf(2f) }
    val valueRange = 0f..4f
    val onValueChange = { i: Float -> value = i }
    val steps = 7
    Box(modifier = Modifier.fillMaxSize()) {
        Stepper(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.rangeSemantics(value, true, onValueChange, valueRange, steps),
            increaseIcon = { Icon(StepperDefaults.Increase, "Increase") },
            decreaseIcon = { Icon(StepperDefaults.Decrease, "Decrease") },
            steps = steps,
        ) {
            Text("Value: $value")
        }
        LevelIndicator(
            value = { value },
            valueRange = valueRange,
            modifier = Modifier.align(Alignment.CenterStart)
        )
    }
}
