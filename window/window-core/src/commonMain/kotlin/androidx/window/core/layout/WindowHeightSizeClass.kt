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

import androidx.window.core.layout.WindowHeightSizeClass.Companion.COMPACT
import androidx.window.core.layout.WindowHeightSizeClass.Companion.EXPANDED
import androidx.window.core.layout.WindowHeightSizeClass.Companion.MEDIUM
import kotlin.jvm.JvmField

/**
 * A class to represent the height size buckets for a viewport. The possible values are [COMPACT],
 * [MEDIUM], and [EXPANDED]. [WindowHeightSizeClass] should not be used as a proxy for the device
 * type. It is possible to have resizeable windows in different device types.
 * The viewport might change from a [COMPACT] all the way to an [EXPANDED] size class.
 */
class WindowHeightSizeClass private constructor(
    private val rawValue: Int
) {

    override fun toString(): String {
        val name = when (this) {
            COMPACT -> "COMPACT"
            MEDIUM -> "MEDIUM"
            EXPANDED -> "EXPANDED"
            else -> "UNKNOWN"
        }
        return "WindowHeightSizeClass: $name"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (this::class != other::class) return false

        val that = other as WindowHeightSizeClass

        return rawValue == that.rawValue
    }

    override fun hashCode(): Int {
        return rawValue
    }

    companion object {
        /**
         * A bucket to represent a compact height, typical for a phone that is in landscape.
         */
        @JvmField
        val COMPACT: WindowHeightSizeClass = WindowHeightSizeClass(0)

        /**
         * A bucket to represent a medium height, typical for a phone in portrait or a tablet.
         */
        @JvmField
        val MEDIUM: WindowHeightSizeClass = WindowHeightSizeClass(1)

        /**
         * A bucket to represent an expanded height window, typical for a large tablet or a
         * desktop form-factor.
         */
        @JvmField
        val EXPANDED: WindowHeightSizeClass = WindowHeightSizeClass(2)

        /**
         * Returns a recommended [WindowHeightSizeClass] for the height of a window given the
         * height in DP.
         * @param dpHeight the height of the window in DP
         * @return A recommended size class for the height
         * @throws IllegalArgumentException if the height is negative
         */
        internal fun compute(dpHeight: Float): WindowHeightSizeClass {
            require(dpHeight >= 0) { "Height must be positive, received $dpHeight" }
            return when {
                dpHeight < 480 -> COMPACT
                dpHeight < 900 -> MEDIUM
                else -> EXPANDED
            }
        }
    }
}
