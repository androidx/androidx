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
 * @see WindowWidthSizeClass
 * @see WindowHeightSizeClass
 */
class WindowSizeClass private constructor(
    /**
     * Returns the [WindowWidthSizeClass] that corresponds to the widthDp of the window.
     */
    val windowWidthSizeClass: WindowWidthSizeClass,
    /**
     * Returns the [WindowHeightSizeClass] that corresponds to the heightDp of the window.
     */
    val windowHeightSizeClass: WindowHeightSizeClass
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as WindowSizeClass

        if (windowWidthSizeClass != other.windowWidthSizeClass) return false
        if (windowHeightSizeClass != other.windowHeightSizeClass) return false

        return true
    }

    override fun hashCode(): Int {
        var result = windowWidthSizeClass.hashCode()
        result = 31 * result + windowHeightSizeClass.hashCode()
        return result
    }

    override fun toString(): String {
        return "WindowSizeClass {" +
            "windowWidthSizeClass=$windowWidthSizeClass, " +
            "windowHeightSizeClass=$windowHeightSizeClass }"
    }

    companion object {

        /**
         * Computes the recommended [WindowSizeClass] for the given width and height in DP.
         * @param dpWidth width of a window in DP.
         * @param dpHeight height of a window in DP.
         * @return [WindowSizeClass] that is recommended for the given dimensions.
         * @throws IllegalArgumentException if [dpWidth] or [dpHeight] is
         * negative.
         */
        @JvmStatic
        fun compute(dpWidth: Float, dpHeight: Float): WindowSizeClass {
            return WindowSizeClass(
                WindowWidthSizeClass.compute(dpWidth),
                WindowHeightSizeClass.compute(dpHeight)
            )
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
            return compute(widthDp, heightDp)
        }
    }
}
