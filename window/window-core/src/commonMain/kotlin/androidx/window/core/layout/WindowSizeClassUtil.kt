/*
 * Copyright 2024 The Android Open Source Project
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
@file:JvmName("WindowSizeClassUtil")
package androidx.window.core.layout

import androidx.window.core.ExperimentalWindowCoreApi

/**
 * A scoring function to calculate how close the width of a [WindowSizeClass] is to [widthDp]
 * without exceeding it.
 *
 * @param widthDp the width bound to try to match.
 * @return an integer from -1 to [Integer.MAX_VALUE] where a larger value indicates a better match.
 */
@ExperimentalWindowCoreApi
fun WindowSizeClass.scoreWithinWidthDp(widthDp: Int): Int {
    return if (this.widthDp <= widthDp) {
        Integer.MAX_VALUE / (1 + widthDp - this.widthDp)
    } else {
        -1
    }
}

/**
 * A scoring function to calculate how close the height of a [WindowSizeClass] is to [heightDp]
 * without exceeding it.
 *
 * @param heightDp the height bound to try to match.
 * @return an integer from -1 to [Integer.MAX_VALUE] where a larger value indicates a better match.
 */
@ExperimentalWindowCoreApi
fun WindowSizeClass.scoreWithinHeightDp(heightDp: Int): Int {
    return if (this.heightDp <= heightDp) {
        Integer.MAX_VALUE / (1 + heightDp - this.heightDp)
    } else {
        -1
    }
}

/**
 * A scoring function to calculate how close the area of a [WindowSizeClass] is to the area of a
 * window without exceeding it.
 *
 * @param windowWidthDp the width of a window constraint.
 * @param windowHeightDp the height of a window constraint.
 *
 * @return an integer from -1 to [Integer.MAX_VALUE] where a larger value indicates a better match.
 */
@ExperimentalWindowCoreApi
fun WindowSizeClass.scoreWithinAreaBounds(
    windowWidthDp: Int,
    windowHeightDp: Int
): Int {
    if (windowWidthDp < this.widthDp || windowHeightDp < this.heightDp) {
        return -1
    }
    val areaDifference = windowWidthDp * windowHeightDp - this.widthDp * this.heightDp
    return Integer.MAX_VALUE / (1 + areaDifference)
}

/**
 * Calculates which [WindowSizeClass] has the closest matching [windowWidthDp] within the given
 * value. If there are multiple matches then the tallest [WindowSizeClass] is selected within the
 * given value.
 *
 * @param windowWidthDp the width of the current window in DP to choose a [WindowSizeClass].
 * @param windowHeightDp the height of the current window in DP to chose a [WindowSizeClass].
 * @return a [WindowSizeClass] that has [WindowSizeClass.widthDp] less than or equal to the
 * [windowWidthDp] and is the closest to [windowWidthDp] if possible `null` otherwise.
 */
fun Set<WindowSizeClass>.widestOrEqualWidthDp(
    windowWidthDp: Int,
    windowHeightDp: Int
): WindowSizeClass? {
    require(0 <= windowHeightDp) {
        "Window height must be non-negative but got windowHeightDp: $windowHeightDp"
    }
    require(0 <= windowWidthDp) {
        "Window width must be non-negative but got windowHeightDp: $windowWidthDp"
    }
    var maxValue: WindowSizeClass? = null
    forEach { sizeClass ->
        if (sizeClass.widthDp > windowWidthDp) {
            return@forEach
        }
        if (sizeClass.heightDp > windowHeightDp) {
            return@forEach
        }

        val localMax = maxValue
        if (localMax == null) {
            maxValue = sizeClass
            return@forEach
        }
        if (localMax.widthDp > sizeClass.widthDp) {
            return@forEach
        }
        if (localMax.widthDp == sizeClass.widthDp && sizeClass.heightDp < localMax.heightDp) {
            return@forEach
        }
        maxValue = sizeClass
    }
    return maxValue
}
