/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.text.style

import androidx.compose.Immutable
import androidx.ui.util.lerp

/**
 * The amount by which the text is shifted up or down from current the baseline.
 * @constructor
 * @sample androidx.ui.text.samples.BaselineShiftSample
 * @sample androidx.ui.text.samples.BaselineShiftAnnotatedStringSample
 *
 * @param multiplier shift the baseline by multiplier * (baseline - ascent)
 */
@Immutable
/*inline*/ data class BaselineShift constructor(
    val multiplier: Float
) {
    companion object {
        /**
         * Default baseline shift for superscript.
         */
        val Superscript = BaselineShift(0.5f)

        /**
         * Default baseline shift for subscript
         */
        val Subscript = BaselineShift(-0.5f)

        /**
         * Constant for no baseline shift.
         */
        val None = BaselineShift(0.0f)
    }
}

/**
 * Linearly interpolate two [BaselineShift]s.
 */
fun lerp(start: BaselineShift, stop: BaselineShift, fraction: Float): BaselineShift {
    return BaselineShift(
        lerp(
            start.multiplier,
            stop.multiplier,
            fraction
        )
    )
}