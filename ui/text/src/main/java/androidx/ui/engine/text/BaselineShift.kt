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

package androidx.ui.engine.text

import androidx.ui.lerp

/**
 * The amount by which the text is shifted up from current baseline.
 * @constructor
 * @param multiplier shift the baseline by multiplier * (baseline - ascent)
 * TODO(Migration/haoyuchang): support baseline shift given by pixel and other multiplier reference
 */
/*inline*/ data class BaselineShift constructor(
    val multiplier: Float
) {
    companion object {
        /**
         * Default baselineShift for superscript.
         */
        val Superscript = BaselineShift(0.5f)

        /**
         * Default baselineShift for subscript
         */
        val Subscript = BaselineShift(-0.5f)

        /**
         * Linearly interpolate two [BaselineShift]s.
         */
        fun lerp(a: BaselineShift?, b: BaselineShift?, t: Float): BaselineShift? {
            if (a == null && b == null) {
                return null
            }
            if (a == null) {
                return BaselineShift(b!!.multiplier * t)
            }
            if (b == null) {
                return BaselineShift(a.multiplier * (1f - t))
            }
            return BaselineShift(lerp(a.multiplier, b.multiplier, t))
        }
    }
}