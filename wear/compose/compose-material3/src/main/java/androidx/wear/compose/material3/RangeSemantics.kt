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

package androidx.wear.compose.material3

import androidx.compose.foundation.progressSemantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.wear.compose.materialcore.RangeDefaults

/**
 * Modifier to add semantics signifying progress of the Stepper/Slider.
 *
 * @param value Current value of the ProgressIndicator/Slider. If outside of [valueRange] provided,
 * value will be coerced to this range. Must not be NaN.
 * @param enabled If false then semantics will not be added.
 * @param onValueChange Lambda which updates [value].
 * @param valueRange Range of values that value can take. Passed [value] will be coerced to this
 * range.
 * @param steps If greater than 0, specifies the amounts of discrete values, evenly distributed
 * between across the whole value range. If 0, any value from the range specified is allowed.
 * Must not be negative.
 */
public fun Modifier.rangeSemantics(
    value: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int
): Modifier {
    val step = RangeDefaults.snapValueToStep(value, valueRange, steps)
    return semantics(mergeDescendants = true) {
        if (!enabled) disabled()
        setProgress(
            action = { targetValue ->
                val newStepIndex = RangeDefaults.snapValueToStep(targetValue, valueRange, steps)
                if (step == newStepIndex) {
                    false
                } else {
                    onValueChange(targetValue)
                    true
                }
            }
        )
    }.progressSemantics(
        RangeDefaults.calculateCurrentStepValue(step, steps, valueRange), valueRange, steps
    )
}

internal fun IntProgression.stepsNumber(): Int = (last - first) / step - 1
