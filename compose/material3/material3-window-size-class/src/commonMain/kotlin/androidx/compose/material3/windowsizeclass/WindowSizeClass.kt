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

package androidx.compose.material3.windowsizeclass

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach

/**
 * Window size classes are a set of opinionated viewport breakpoints to design, develop, and test
 * responsive application layouts against. For more details check <a
 * href="https://developer.android.com/guide/topics/large-screens/support-different-screen-sizes"
 * class="external" target="_blank">Support different screen sizes</a> documentation.
 *
 * WindowSizeClass contains a [WindowWidthSizeClass] and [WindowHeightSizeClass], representing the
 * window size classes for this window's width and height respectively.
 *
 * See [calculateWindowSizeClass] to calculate the WindowSizeClass for an Activity's current window
 *
 * @property widthSizeClass width-based window size class ([WindowWidthSizeClass])
 * @property heightSizeClass height-based window size class ([WindowHeightSizeClass])
 */
@Immutable
class WindowSizeClass
private constructor(
    val widthSizeClass: WindowWidthSizeClass,
    val heightSizeClass: WindowHeightSizeClass,
) {
    companion object {
        /**
         * Calculates the best matched [WindowSizeClass] for a given [size] according to the
         * provided [supportedWidthSizeClasses] and [supportedHeightSizeClasses].
         *
         * @param size of the window
         * @param supportedWidthSizeClasses the set of width size classes that are supported
         * @param supportedHeightSizeClasses the set of height size classes that are supported
         * @return [WindowSizeClass] corresponding to the given width and height
         */
        @ExperimentalMaterial3WindowSizeClassApi
        @Suppress("PrimitiveInCollection")
        fun calculateFromSize(
            size: DpSize,
            supportedWidthSizeClasses: Set<WindowWidthSizeClass> =
                WindowWidthSizeClass.DefaultSizeClasses,
            supportedHeightSizeClasses: Set<WindowHeightSizeClass> =
                WindowHeightSizeClass.DefaultSizeClasses,
        ): WindowSizeClass {
            val windowWidthSizeClass =
                WindowWidthSizeClass.fromWidth(size.width, supportedWidthSizeClasses)
            val windowHeightSizeClass =
                WindowHeightSizeClass.fromHeight(size.height, supportedHeightSizeClasses)
            return WindowSizeClass(windowWidthSizeClass, windowHeightSizeClass)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as WindowSizeClass

        if (widthSizeClass != other.widthSizeClass) return false
        if (heightSizeClass != other.heightSizeClass) return false

        return true
    }

    override fun hashCode(): Int {
        var result = widthSizeClass.hashCode()
        result = 31 * result + heightSizeClass.hashCode()
        return result
    }

    override fun toString() = "WindowSizeClass($widthSizeClass, $heightSizeClass)"
}

/**
 * Width-based window size class.
 *
 * A window size class represents a breakpoint that can be used to build responsive layouts. Each
 * window size class breakpoint represents a majority case for typical device scenarios so your
 * layouts will work well on most devices and configurations.
 *
 * For more details see <a
 * href="https://developer.android.com/guide/topics/large-screens/support-different-screen-sizes#window_size_classes"
 * class="external" target="_blank">Window size classes documentation</a>.
 */
@Immutable
@kotlin.jvm.JvmInline
value class WindowWidthSizeClass private constructor(private val value: Int) :
    Comparable<WindowWidthSizeClass> {

    override operator fun compareTo(other: WindowWidthSizeClass) =
        breakpoint().compareTo(other.breakpoint())

    override fun toString(): String {
        return "WindowWidthSizeClass." +
            when (this) {
                Compact -> "Compact"
                Medium -> "Medium"
                Expanded -> "Expanded"
                else -> ""
            }
    }

    companion object {
        /** Represents the majority of phones in portrait. */
        val Compact = WindowWidthSizeClass(0)

        /**
         * Represents the majority of tablets in portrait and large unfolded inner displays in
         * portrait.
         */
        val Medium = WindowWidthSizeClass(1)

        /**
         * Represents the majority of tablets in landscape and large unfolded inner displays in
         * landscape.
         */
        val Expanded = WindowWidthSizeClass(2)

        /**
         * The default set of size classes that includes [Compact], [Medium], and [Expanded] size
         * classes. Should never expand to ensure behavioral consistency.
         */
        @Suppress("PrimitiveInCollection") val DefaultSizeClasses = setOf(Compact, Medium, Expanded)

        @Suppress("PrimitiveInCollection")
        private val AllSizeClassList = listOf(Expanded, Medium, Compact)

        /**
         * The set of all size classes. It's supposed to be expanded whenever a new size class is
         * defined. By default [WindowSizeClass.calculateFromSize] will only return size classes in
         * [DefaultSizeClasses] in order to avoid behavioral changes when new size classes are
         * added. You can opt in to support all available size classes by doing:
         * ```
         * WindowSizeClass.calculateFromSize(
         *     size = size,
         *     density = density,
         *     supportedWidthSizeClasses = WindowWidthSizeClass.AllSizeClasses,
         *     supportedHeightSizeClasses = WindowHeightSizeClass.AllSizeClasses
         * )
         * ```
         */
        @Suppress("ListIterator", "PrimitiveInCollection")
        val AllSizeClasses = AllSizeClassList.toSet()

        private fun WindowWidthSizeClass.breakpoint(): Dp {
            return when {
                this == Expanded -> 840.dp
                this == Medium -> 600.dp
                else -> 0.dp
            }
        }

        /**
         * Calculates the best matched [WindowWidthSizeClass] for a given [width] in Pixels and a
         * given [Density] from [supportedSizeClasses].
         */
        @Suppress("PrimitiveInCollection")
        internal fun fromWidth(
            width: Dp,
            supportedSizeClasses: Set<WindowWidthSizeClass>,
        ): WindowWidthSizeClass {
            require(width >= 0.dp) { "Width must not be negative" }
            require(supportedSizeClasses.isNotEmpty()) { "Must support at least one size class" }
            var smallestSupportedSizeClass = Compact
            AllSizeClassList.fastForEach {
                if (it in supportedSizeClasses) {
                    if (width >= it.breakpoint()) {
                        return it
                    }
                    smallestSupportedSizeClass = it
                }
            }

            // If none of the size classes matches, return the largest one.
            return smallestSupportedSizeClass
        }
    }
}

/**
 * Height-based window size class.
 *
 * A window size class represents a breakpoint that can be used to build responsive layouts. Each
 * window size class breakpoint represents a majority case for typical device scenarios so your
 * layouts will work well on most devices and configurations.
 *
 * For more details see <a
 * href="https://developer.android.com/guide/topics/large-screens/support-different-screen-sizes#window_size_classes"
 * class="external" target="_blank">Window size classes documentation</a>.
 */
@Immutable
@kotlin.jvm.JvmInline
value class WindowHeightSizeClass private constructor(private val value: Int) :
    Comparable<WindowHeightSizeClass> {

    override operator fun compareTo(other: WindowHeightSizeClass) =
        breakpoint().compareTo(other.breakpoint())

    override fun toString(): String {
        return "WindowHeightSizeClass." +
            when (this) {
                Compact -> "Compact"
                Medium -> "Medium"
                Expanded -> "Expanded"
                else -> ""
            }
    }

    companion object {
        /** Represents the majority of phones in landscape */
        val Compact = WindowHeightSizeClass(0)

        /** Represents the majority of tablets in landscape and majority of phones in portrait */
        val Medium = WindowHeightSizeClass(1)

        /** Represents the majority of tablets in portrait */
        val Expanded = WindowHeightSizeClass(2)

        /**
         * The default set of size classes that includes [Compact], [Medium], and [Expanded] size
         * classes. Should never expand to ensure behavioral consistency.
         */
        @Suppress("PrimitiveInCollection") val DefaultSizeClasses = setOf(Compact, Medium, Expanded)

        @Suppress("PrimitiveInCollection")
        private val AllSizeClassList = listOf(Expanded, Medium, Compact)

        /**
         * The set of all size classes. It's supposed to be expanded whenever a new size class is
         * defined. By default [WindowSizeClass.calculateFromSize] will only return size classes in
         * [DefaultSizeClasses] in order to avoid behavioral changes when new size classes are
         * added. You can opt in to support all available size classes by doing:
         * ```
         * WindowSizeClass.calculateFromSize(
         *     size = size,
         *     density = density,
         *     supportedWidthSizeClasses = WindowWidthSizeClass.AllSizeClasses,
         *     supportedHeightSizeClasses = WindowHeightSizeClass.AllSizeClasses
         * )
         * ```
         */
        @Suppress("ListIterator", "PrimitiveInCollection")
        val AllSizeClasses = AllSizeClassList.toSet()

        private fun WindowHeightSizeClass.breakpoint(): Dp {
            return when {
                this == Expanded -> 900.dp
                this == Medium -> 480.dp
                else -> 0.dp
            }
        }

        /**
         * Calculates the best matched [WindowHeightSizeClass] for a given [height] in Pixels and a
         * given [Density] from [supportedSizeClasses].
         */
        @Suppress("PrimitiveInCollection")
        internal fun fromHeight(
            height: Dp,
            supportedSizeClasses: Set<WindowHeightSizeClass>,
        ): WindowHeightSizeClass {
            require(height >= 0.dp) { "Width must not be negative" }
            require(supportedSizeClasses.isNotEmpty()) { "Must support at least one size class" }
            var smallestSupportedSizeClass = Expanded
            AllSizeClassList.fastForEach {
                if (it in supportedSizeClasses) {
                    if (height >= it.breakpoint()) {
                        return it
                    }
                    smallestSupportedSizeClass = it
                }
            }

            // If none of the size classes matches, return the largest one.
            return smallestSupportedSizeClass
        }
    }
}
