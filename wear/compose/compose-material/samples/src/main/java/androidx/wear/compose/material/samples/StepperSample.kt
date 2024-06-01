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

package androidx.wear.compose.material.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.progressSemantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.util.lerp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Stepper
import androidx.wear.compose.material.StepperDefaults
import androidx.wear.compose.material.Text
import kotlin.math.roundToInt

@Sampled
@Composable
fun StepperSample() {
    var value by remember { mutableStateOf(2f) }
    Stepper(
        value = value,
        onValueChange = { value = it },
        valueRange = 1f..4f,
        increaseIcon = { Icon(StepperDefaults.Increase, "Increase") },
        decreaseIcon = { Icon(StepperDefaults.Decrease, "Decrease") },
        steps = 7
    ) {
        Text("Value: $value")
    }
}

@Sampled
@Composable
fun StepperWithIntegerSample() {
    var value by remember { mutableStateOf(2) }
    Stepper(
        value = value,
        onValueChange = { value = it },
        increaseIcon = { Icon(StepperDefaults.Increase, "Increase") },
        decreaseIcon = { Icon(StepperDefaults.Decrease, "Decrease") },
        valueProgression = 1..10
    ) {
        Text("Value: $value")
    }
}

@Sampled
@Composable
fun StepperWithoutRangeSemanticsSample() {
    var value by remember { mutableStateOf(2f) }
    Stepper(
        value = value,
        onValueChange = { value = it },
        valueRange = 1f..4f,
        increaseIcon = { Icon(StepperDefaults.Increase, "Increase") },
        decreaseIcon = { Icon(StepperDefaults.Decrease, "Decrease") },
        steps = 7,
        enableRangeSemantics = false
    ) {
        Text("Value: $value")
    }
}

@Sampled
@Composable
fun StepperWithCustomSemanticsSample() {
    var value by remember { mutableStateOf(2f) }
    val valueRange = 1f..4f
    val onValueChange = { i: Float -> value = i }
    val steps = 7

    Stepper(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        modifier = Modifier.customSemantics(value, true, onValueChange, valueRange, steps),
        increaseIcon = { Icon(StepperDefaults.Increase, "Increase") },
        decreaseIcon = { Icon(StepperDefaults.Decrease, "Decrease") },
        steps = steps,
        enableRangeSemantics = false
    ) {
        Text("Value: $value")
    }
}

// Declaring the custom semantics for StepperWithCustomSemanticsSample
private fun Modifier.customSemantics(
    value: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int
): Modifier =
    semantics(mergeDescendants = true) {
            if (!enabled) disabled()
            setProgress(
                action = { targetValue ->
                    val newStepIndex =
                        ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start) *
                                (steps + 1))
                            .roundToInt()
                            .coerceIn(0, steps + 1)

                    if (value.toInt() == newStepIndex) {
                        false
                    } else {
                        onValueChange(targetValue)
                        true
                    }
                }
            )
        }
        .progressSemantics(
            lerp(valueRange.start, valueRange.endInclusive, value / (steps + 1).toFloat())
                .coerceIn(valueRange),
            valueRange,
            steps
        )
