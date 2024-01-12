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

import androidx.window.core.ExperimentalWindowCoreApi
import kotlin.jvm.JvmStatic

/**
 * [WindowSizeClass] represents breakpoints for a viewport. The recommended width and height break
 * points are presented through [windowWidthSizeClass] and [windowHeightSizeClass]. Designers
 * should design around the different combinations of width and height buckets. Developers should
 * use the different buckets to specify the layouts. Ideally apps will work well in each bucket and
 * by extension work well across multiple devices. If two devices are in similar buckets they
 * should behave similarly.
 *
 * This class is meant to be a common definition that can be shared across different device types.
 * Application developers can use WindowSizeClass to have standard window buckets and design the UI
 * around those buckets. Library developers can use these buckets to create different UI with
 * respect to each bucket. This will help with consistency across multiple device types.
 *
 * A library developer use-case can be creating some navigation UI library. For a size
 * class with the [WindowWidthSizeClass.EXPANDED] width it might be more reasonable to have a side
 * navigation. For a [WindowWidthSizeClass.COMPACT] width, a bottom navigation might be a better
 * fit.
 *
 * An application use-case can be applied for apps that use a list-detail pattern. The app can use
 * the [WindowWidthSizeClass.MEDIUM] to determine if there is enough space to show the list and the
 * detail side by side. If all apps follow this guidance then it will present a very consistent user
 * experience.
 *
 * In some cases developers or UI systems may decide to create their own break points. A developer
 * might optimize for a window that is smaller than the supported break points or larger. A UI
 * system might find that some break points are better suited than the recommended break points.
 * In these cases developers may wish to specify their own custom break points and match using
 * a `when` statement.
 *
 * @constructor the primary constructor taking the bounds of the size class.
 * @property widthDp the width in DP for the size class.
 * @property heightDp the height in DP for the size class.
 *
 * @throws IllegalArgumentException if [widthDp] or [heightDp] is negative.
 *
 * @see WindowWidthSizeClass
 * @see WindowHeightSizeClass
 */
class WindowSizeClass(
    val widthDp: Int,
    val heightDp: Int
) {

    init {
        require(widthDp >= 0) { "Must have non-negative widthDp: $widthDp." }
        require(heightDp >= 0) { "Must have non-negative heightDp: $heightDp." }
    }

    /**
     * Returns the [WindowWidthSizeClass] that corresponds to the [widthDp].
     */
    val windowWidthSizeClass: WindowWidthSizeClass
        get() = WindowWidthSizeClass.compute(widthDp.toFloat())

    /**
     * Returns the [WindowHeightSizeClass] that corresponds to the [heightDp].
     */
    val windowHeightSizeClass: WindowHeightSizeClass
        get() = WindowHeightSizeClass.compute(heightDp.toFloat())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WindowSizeClass

        if (widthDp != other.widthDp) return false
        if (heightDp != other.heightDp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = widthDp.hashCode()
        result = 31 * result + heightDp.hashCode()
        return result
    }

    override fun toString(): String {
        return "SizeClass { widthDp: $widthDp," +
            " heightDp: $heightDp }"
    }

    companion object {

        /**
         * Computes the [WindowSizeClass] for the given width and height in DP.
         * @param dpWidth width of a window in DP.
         * @param dpHeight height of a window in DP.
         * @return [WindowSizeClass] that is recommended for the given dimensions.
         * @throws IllegalArgumentException if [dpWidth] or [dpHeight] is
         * negative.
         *
         * @deprecated use the constructor instead.
         */
        @JvmStatic
        @Deprecated("Use constructor instead.",
            ReplaceWith("WindowSizeClass(widthDp = dpWidth, heightDp = dpHeight)"))
        fun compute(dpWidth: Float, dpHeight: Float): WindowSizeClass {
            return WindowSizeClass(dpWidth.toInt(), dpHeight.toInt())
        }

        /**
         * Computes the [WindowSizeClass] for the given width and height in pixels with density.
         * @param widthPx width of a window in PX.
         * @param heightPx height of a window in PX.
         * @param density density of the display where the window is shown.
         * @return [WindowSizeClass] that is recommended for the given dimensions.
         * @throws IllegalArgumentException if [widthPx], [heightPx], or [density] is
         * negative.
         */
        @JvmStatic
        @ExperimentalWindowCoreApi
        fun compute(widthPx: Int, heightPx: Int, density: Float): WindowSizeClass {
            val widthDp = widthPx / density
            val heightDp = heightPx / density
            return WindowSizeClass(widthDp = widthDp.toInt(), heightDp = heightDp.toInt())
        }
    }
}
