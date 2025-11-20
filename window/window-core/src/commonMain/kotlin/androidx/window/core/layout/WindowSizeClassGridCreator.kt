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

@file:JvmName("WindowSizeClassGridCreator")

package androidx.window.core.layout

import kotlin.jvm.JvmName

/**
 * Creates a new set of breakpoints by taking the cross product of [minWidthDpBreakPoints] and
 * [minHeightDpBreakPoints]. For example, if the width set is {10, 20} and the height set is { 100,
 * 200 } then the resulting set will be { (10, 100), (10, 200), (20, 100), (20, 200) }.
 *
 * @param minHeightDpBreakPoints the height breakpoints in DP to construct the [WindowSizeClass]
 *   [Set].
 * @param minWidthDpBreakPoints the width breakpoints in DP to construct the [WindowSizeClass] [Set]
 * @return a [Set] containing the breakpoints using a grid style
 */
public fun createGridWindowSizeClassSet(
    minWidthDpBreakPoints: Set<Int>,
    minHeightDpBreakPoints: Set<Int>,
): Set<WindowSizeClass> {
    return minWidthDpBreakPoints
        .flatMap { widthBreakPoint ->
            minHeightDpBreakPoints.map { heightBreakPoint ->
                WindowSizeClass(minWidthDp = widthBreakPoint, minHeightDp = heightBreakPoint)
            }
        }
        .toSet()
}

/**
 * Creates a new [Set] that contains the original [WindowSizeClass] values in addition to new
 * elements where the width is from the original set and the height is from
 * [minHeightDpBreakPoints]. Note this method does not fill in any gaps in the original set. For
 * example if the original set was { (10, 10), (20, 20) } and the height set is { 30 } the new set
 * will be { (10, 10), (20, 20), (10, 30), (20, 30) }. To maintain a grid set the source set must
 * also be a grid set.
 *
 * @param minHeightDpBreakPoints the height breakpoints in DP to construct the [WindowSizeClass]
 *   [Set].
 * @return a new [Set] that contains (minWidthDp, minHeightDp) for each pair possible taking the
 *     * height breakpoint from [minHeightDpBreakPoints] and the width breakpoint from [this].
 */
public fun Set<WindowSizeClass>.addHeightDpBreakpoints(
    minHeightDpBreakPoints: Set<Int>
): Set<WindowSizeClass> {
    val widthDpBreakPoints = this.map { windowSizeClass -> windowSizeClass.minWidthDp }.toSet()
    return this +
        createGridWindowSizeClassSet(
            minWidthDpBreakPoints = widthDpBreakPoints,
            minHeightDpBreakPoints = minHeightDpBreakPoints,
        )
}
