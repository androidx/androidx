/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.wear.watchface.data

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

/** @hide */
@IntDef(
    value = [
        ComplicationSlotBoundsType.ROUND_RECT,
        ComplicationSlotBoundsType.BACKGROUND,
        ComplicationSlotBoundsType.EDGE
    ]
)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public annotation class ComplicationSlotBoundsType {
    public companion object {
        /** The default, most complication slots are either circular or rounded rectangles. */
        public const val ROUND_RECT: Int = 0

        /**
         * For a full screen image complication slot drawn behind the watch face. Note you can only
         * have a single background complication slot.
         */
        public const val BACKGROUND: Int = 1

        /** For edge of screen complication slots. */
        public const val EDGE: Int = 2
    }
}