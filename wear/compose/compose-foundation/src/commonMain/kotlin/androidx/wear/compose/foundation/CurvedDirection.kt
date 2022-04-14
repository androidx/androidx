/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.wear.compose.foundation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.LayoutDirection

public interface CurvedDirection {
    /**
     * The direction in which components are laid out on a [curvedRow] or [CurvedLayout]
     *
     * Example:
     * @sample androidx.wear.compose.foundation.samples.CurvedBottomLayout
     */
    @Immutable
    @kotlin.jvm.JvmInline
    public value class Angular internal constructor(internal val value: Int) {
        companion object {
            /**
             * Go in Clockwise direction for Ltr layout and Counter Clockwise for Rtl.
             * This is generally used for curved layouts on the top of the screen.
             */
            val Normal = Angular(0)

            /**
             * Go in Counter Clockwise direction for Ltr layout and Clockwise for Rtl.
             * This is generally used for curved layouts on the bottom of the screen.
             */
            val Reversed = Angular(1)

            /**
             * Go in Clockwise direction, independently of [LayoutDirection].
             */
            val Clockwise = Angular(2)

            /**
             * Go in Counter Clockwise direction, independently of [LayoutDirection].
             */
            val CounterClockwise = Angular(3)
        }
    }

    /**
     * The direction in which components are lay down on a [curvedColumn]
     */
    @Immutable
    @kotlin.jvm.JvmInline
    public value class Radial internal constructor(internal val value: Int) {
        companion object {
            /**
             * Lay components starting farther away from the center and going inwards.
             * This is generally used for curved layouts on the top of the screen.
             */
            val OutsideIn = Radial(0)

            /**
             * Go in Counter Clockwise direction for Ltr layout and Clockwise for Rtl.
             * This is generally used for curved layouts on the bottom of the screen.
             */
            val InsideOut = Radial(1)
        }
    }
}
