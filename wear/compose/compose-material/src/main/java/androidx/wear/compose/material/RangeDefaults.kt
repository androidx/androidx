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

package androidx.wear.compose.material

import androidx.compose.foundation.progressSemantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.wear.compose.materialcore.RangeDefaults

internal fun Modifier.rangeSemantics(
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
