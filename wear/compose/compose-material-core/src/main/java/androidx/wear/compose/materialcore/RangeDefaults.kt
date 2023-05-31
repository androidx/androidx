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

package androidx.wear.compose.materialcore

import androidx.annotation.RestrictTo
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.util.lerp
import kotlin.math.roundToInt

/**
 * Icons which are used by Range controls like slider and stepper
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RangeIcons {

    /**
     * An [ImageVector] with a minus sign.
     */
    val Minus: ImageVector
        get() = if (_minus != null) _minus!!
        else {
            _minus = materialIcon(name = "MinusIcon") {
                materialPath {
                    moveTo(19.0f, 13.0f)
                    horizontalLineTo(5.0f)
                    verticalLineToRelative(-2.0f)
                    horizontalLineToRelative(14.0f)
                    verticalLineToRelative(2.0f)
                    close()
                }
            }
            _minus!!
        }

    private var _minus: ImageVector? = null
}

/**
 * Defaults used by range controls like slider and stepper
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object RangeDefaults {
    /**
     * Calculates value of [currentStep] in [valueRange] depending on number of [steps]
     */
    fun calculateCurrentStepValue(
        currentStep: Int,
        steps: Int,
        valueRange: ClosedFloatingPointRange<Float>
    ): Float = lerp(
        valueRange.start, valueRange.endInclusive,
        currentStep.toFloat() / (steps + 1).toFloat()
    ).coerceIn(valueRange)

    /**
     * Snaps [value] to the closest [step] in the [valueRange]
     */
    fun snapValueToStep(
        value: Float,
        valueRange: ClosedFloatingPointRange<Float>,
        steps: Int
    ): Int = ((value - valueRange.start) /
        (valueRange.endInclusive - valueRange.start) * (steps + 1))
        .roundToInt().coerceIn(0, steps + 1)
}
