/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.material3.adaptive

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_MEDIUM_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND

/**
 * The set of width size classes in DP. These values are the lower bounds for the corresponding size
 * classes.
 *
 * This object defines different width size classes, including:
 * - [Compact]: Represents the smallest width size class, starting at 0 dp.
 * - [Medium]: Represents a medium width size class, starting at 600 dp.
 * - [Expanded]: Represents an expanded width size class, starting at 840 dp.
 * - [Large]: Represents a large width size class, starting at 1200 dp.
 * - [ExtraLarge]: Represents an extremely large width size class, starting at 1600 dp.
 *
 * These values are used to define breakpoints for adaptive layouts, and are intended to align with
 * the window size class definitions.
 *
 * @see WindowSizeClass
 */
@Suppress("PrimitiveInCollection")
private object DpWidthSizeClasses {
    /**
     * The lower bound for the Compact width size class. By default, any window width which is at
     * least this value and less than [Medium] will be considered [Compact].
     */
    val Compact = 0.dp

    /**
     * The lower bound for the Medium width size class. By default, any window width which is at
     * least this value and less than [Expanded] will be considered [Medium].
     *
     * @see WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND
     */
    val Medium = WIDTH_DP_MEDIUM_LOWER_BOUND.dp

    /**
     * The lower bound for the Expanded width size class. By default, any window width which is at
     * least this value will be considered [Expanded]; or in the [DefaultV2] definition of the width
     * size classes, any window width which is at least this value and less than [Large] will be
     * considered [Expanded].
     *
     * @see WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND
     */
    val Expanded = WIDTH_DP_EXPANDED_LOWER_BOUND.dp

    /**
     * The lower bound for the Large width size class. With the [DefaultV2] definition of the width
     * size, any window width which is at least this value and less than [ExtraLarge] will be
     * considered [Large].
     */
    // TODO(conradchen): Move to window-core definition when it goes to 1.5 stable
    val Large = 1200.dp

    /**
     * The lower bound for the Extra-Large width size class. With the [DefaultV2] definition of the
     * width size, any window width which is at least this value will be considered [ExtraLarge].
     */
    // TODO(conradchen): Move to window-core definition when it goes to 1.5 stable
    val ExtraLarge = 1600.dp

    /**
     * The default set of supported width size classes that only contains [Compact], [Medium], and
     * [Expanded] but not [Large] or [ExtraLarge], which has been introduced since the 1.2.0 version
     * of the Material3 Adaptive library.
     */
    val Default = setOf(Compact, Medium, Expanded)

    /**
     * The second version of the default set of supported width size classes that contains width
     * size classes included in [Default], plus [Large] and [ExtraLarge].
     */
    val DefaultV2 = setOf(Compact, Medium, Expanded, Large, ExtraLarge)
}

/**
 * The set of height size classes in DP. These values are the lower bounds for the corresponding
 * size classes.
 *
 * This object defines different height size classes, including:
 * - [Compact]: Represents the smallest height size class, starting at 0 dp.
 * - [Medium]: Represents a medium height size class, starting at 480 dp.
 * - [Expanded]: Represents an expanded height size class, starting at 900 dp.
 * - [Default]: A set containing the base size classes [Compact], [Medium], and [Expanded].
 *
 * These values are used to define breakpoints for adaptive layouts, and are intended to align with
 * the window size class definitions.
 *
 * @see WindowSizeClass
 */
@Suppress("PrimitiveInCollection")
private object DpHeightSizeClasses {
    /**
     * The lower bound for the Compact height size class. By default, any window height which is at
     * least this value and less than [Medium] will be considered [Compact].
     */
    val Compact = 0.dp

    /**
     * The lower bound for the Medium height size class. By default, any window height which is at
     * least this value and less than [Expanded] will be considered [Medium].
     *
     * @see WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND
     */
    val Medium = HEIGHT_DP_MEDIUM_LOWER_BOUND.dp

    /**
     * The lower bound for the Expanded height size class. By default, any window height which is at
     * least this value will be considered [Expanded].
     *
     * @see WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND
     */
    val Expanded = HEIGHT_DP_EXPANDED_LOWER_BOUND.dp

    /**
     * The default set of supported height size classes that contains [Compact], [Medium], and
     * [Expanded].
     */
    val Default = setOf(Compact, Medium, Expanded)
}

/**
 * Computes a [WindowSizeClass] from a given [DpSize] based on a set of supported width and height
 * size class breakpoints.
 *
 * This function determines the [WindowSizeClass] based on the provided [windowSize] and the
 * [supportedWidthSizeClasses] and [supportedHeightSizeClasses]. It does this by finding the largest
 * size class with its lower bound less than or equal to the window's current dimensions.
 *
 * @param windowSize The [DpSize] of the window.
 * @param supportedWidthSizeClasses The set of supported width size classes. Defaults to
 *   [DpWidthSizeClasses.Default].
 * @param supportedHeightSizeClasses The set of supported height size classes. Defaults to
 *   [DpHeightSizeClasses.Default].
 * @return A [WindowSizeClass] instance representing the calculated size class based on the provided
 *   dimensions and supported classes.
 */
@Suppress("PrimitiveInCollection")
internal fun WindowSizeClass.Companion.computeFromDpSize(
    windowSize: DpSize,
    supportedWidthSizeClasses: Set<Dp> = DpWidthSizeClasses.Default,
    supportedHeightSizeClasses: Set<Dp> = DpHeightSizeClasses.Default,
) =
    WindowSizeClass(
        supportedWidthSizeClasses.filter { windowSize.width >= it }.maxOf { it.value },
        supportedHeightSizeClasses.filter { windowSize.height >= it }.maxOf { it.value },
    )

/**
 * Computes the [WindowSizeClass] from a given [DpSize] based on a set of supported width and height
 * size class breakpoints.
 *
 * This function determines the [WindowSizeClass] based on the provided [windowSize] and the
 * [supportedWidthSizeClasses] and [supportedHeightSizeClasses]. It does this by finding the largest
 * size class with its lower bound less than or equal to the window's current dimensions.
 *
 * Note that the underlying the logic of this function is the same as [computeFromDpSize], except
 * that the default set of supported width classes also includes [DpWidthSizeClasses.Large] and
 * [DpWidthSizeClasses.ExtraLarge], in addition to [DpWidthSizeClasses.Compact],
 * [DpWidthSizeClasses.Medium] and [[DpWidthSizeClasses.Large].
 *
 * @param windowSize The size of the window to compute the size class for.
 * @param supportedWidthSizeClasses The set of supported width size classes in DP. Defaults to
 *   [DpWidthSizeClasses.DefaultV2], which supports a larger range of width breakpoints.
 * @param supportedHeightSizeClasses The set of supported height size classes in DP. Defaults to
 *   [DpHeightSizeClasses.Default], which is the V1 set of height breakpoints.
 * @see computeFromDpSize
 * @see DpWidthSizeClasses.DefaultV2
 * @see DpHeightSizeClasses.Default
 */
@Suppress("PrimitiveInCollection")
internal fun WindowSizeClass.Companion.computeFromDpSizeV2(
    windowSize: DpSize,
    supportedWidthSizeClasses: Set<Dp> = DpWidthSizeClasses.DefaultV2,
    supportedHeightSizeClasses: Set<Dp> = DpHeightSizeClasses.Default,
) = computeFromDpSize(windowSize, supportedWidthSizeClasses, supportedHeightSizeClasses)
