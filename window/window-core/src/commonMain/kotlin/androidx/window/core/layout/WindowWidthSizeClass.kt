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

package androidx.window.core.layout

import kotlin.jvm.JvmField

/**
 * A class to represent the width size buckets for a viewport. The possible values are [COMPACT],
 * [MEDIUM], and [EXPANDED]. [WindowWidthSizeClass] should not be used as a proxy for the device
 * type. It is possible to have resizeable windows in different device types.
 * The viewport might change from a [COMPACT] all the way to an [EXPANDED] size class.
 */
class WindowWidthSizeClass private constructor(
    private val rawValue: Int
) {
    override fun toString(): String {
        val name = when (this) {
            COMPACT -> "COMPACT"
            MEDIUM -> "MEDIUM"
            EXPANDED -> "EXPANDED"
            else -> "UNKNOWN"
        }
        return "WindowWidthSizeClass: $name"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        val that = other as WindowWidthSizeClass

        return rawValue == that.rawValue
    }

    override fun hashCode(): Int {
        return rawValue
    }

    companion object {
        /**
         * A bucket to represent a compact width window, typical for a phone in portrait.
         */
        @JvmField
        val COMPACT: WindowWidthSizeClass = WindowWidthSizeClass(0)

        /**
         * A bucket to represent a medium width window, typical for a phone in landscape or
         * a tablet.
         */
        @JvmField
        val MEDIUM: WindowWidthSizeClass = WindowWidthSizeClass(1)

        /**
         * A bucket to represent an expanded width window, typical for a large tablet or desktop
         * form-factor.
         */
        @JvmField
        val EXPANDED: WindowWidthSizeClass = WindowWidthSizeClass(2)

        /**
         * Returns a recommended [WindowWidthSizeClass] for the width of a window given the width
         * in DP.
         * @param dpWidth the width of the window in DP
         * @return A recommended size class for the width
         * @throws IllegalArgumentException if the width is negative
         */
        internal fun compute(dpWidth: Float): WindowWidthSizeClass {
            require(dpWidth >= 0) { "Width must be positive, received $dpWidth" }
            return when {
                dpWidth < 600 -> COMPACT
                dpWidth < 840 -> MEDIUM
                else -> EXPANDED
            }
        }
    }
}
