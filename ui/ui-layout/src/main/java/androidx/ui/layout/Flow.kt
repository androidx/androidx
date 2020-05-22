/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.layout

import androidx.compose.Composable
import androidx.ui.core.Alignment
import androidx.ui.core.Constraints
import androidx.ui.core.Layout
import androidx.ui.core.Placeable
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPx
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.dp
import androidx.ui.unit.isFinite
import androidx.ui.unit.max
import androidx.ui.util.fastForEachIndexed

/**
 * A composable that places its children in a horizontal flow. Unlike [Row], if the
 * horizontal space is too small to put all the children in one row, multiple rows may be used.
 *
 * Note that just like [Row], flex values cannot be used with [FlowRow].
 *
 * Example usage:
 *
 * @sample androidx.ui.layout.samples.SimpleFlowRow
 *
 * @param mainAxisSize The size of the layout in the main axis direction.
 * @param mainAxisAlignment The alignment of each row's children in the main axis direction.
 * @param mainAxisSpacing The main axis spacing between the children of each row.
 * @param crossAxisAlignment The alignment of each row's children in the cross axis direction.
 * @param crossAxisSpacing The cross axis spacing between the rows of the layout.
 * @param lastLineMainAxisAlignment Overrides the main axis alignment of the last row.
 */
@ExperimentalLayout
@Composable
fun FlowRow(
    mainAxisSize: SizeMode = SizeMode.Wrap,
    mainAxisAlignment: FlowMainAxisAlignment = FlowMainAxisAlignment.Start,
    mainAxisSpacing: Dp = 0.dp,
    crossAxisAlignment: FlowCrossAxisAlignment = FlowCrossAxisAlignment.Start,
    crossAxisSpacing: Dp = 0.dp,
    lastLineMainAxisAlignment: FlowMainAxisAlignment = mainAxisAlignment,
    children: @Composable () -> Unit
) {
    Flow(
        orientation = LayoutOrientation.Horizontal,
        mainAxisSize = mainAxisSize,
        mainAxisAlignment = mainAxisAlignment,
        mainAxisSpacing = mainAxisSpacing,
        crossAxisAlignment = crossAxisAlignment,
        crossAxisSpacing = crossAxisSpacing,
        lastLineMainAxisAlignment = lastLineMainAxisAlignment,
        children = children
    )
}

/**
 * A composable that places its children in a vertical flow. Unlike [Column], if the
 * vertical space is too small to put all the children in one column, multiple columns may be used.
 *
 * Note that just like [Column], flex values cannot be used with [FlowColumn].
 *
 * Example usage:
 *
 * @sample androidx.ui.layout.samples.SimpleFlowColumn
 *
 * @param mainAxisSize The size of the layout in the main axis direction.
 * @param mainAxisAlignment The alignment of each column's children in the main axis direction.
 * @param mainAxisSpacing The main axis spacing between the children of each column.
 * @param crossAxisAlignment The alignment of each column's children in the cross axis direction.
 * @param crossAxisSpacing The cross axis spacing between the columns of the layout.
 * @param lastLineMainAxisAlignment Overrides the main axis alignment of the last column.
 */
@ExperimentalLayout
@Composable
fun FlowColumn(
    mainAxisSize: SizeMode = SizeMode.Wrap,
    mainAxisAlignment: FlowMainAxisAlignment = FlowMainAxisAlignment.Start,
    mainAxisSpacing: Dp = 0.dp,
    crossAxisAlignment: FlowCrossAxisAlignment = FlowCrossAxisAlignment.Start,
    crossAxisSpacing: Dp = 0.dp,
    lastLineMainAxisAlignment: FlowMainAxisAlignment = mainAxisAlignment,
    children: @Composable () -> Unit
) {
    Flow(
        orientation = LayoutOrientation.Vertical,
        mainAxisSize = mainAxisSize,
        mainAxisAlignment = mainAxisAlignment,
        mainAxisSpacing = mainAxisSpacing,
        crossAxisAlignment = crossAxisAlignment,
        crossAxisSpacing = crossAxisSpacing,
        lastLineMainAxisAlignment = lastLineMainAxisAlignment,
        children = children
    )
}

/**
 * Used to specify the alignment of a layout's children, in cross axis direction.
 */
enum class FlowCrossAxisAlignment {
    /**
     * Place children such that their center is in the middle of the cross axis.
     */
    Center,
    /**
     * Place children such that their start edge is aligned to the start edge of the cross axis.
     */
    Start,
    /**
     * Place children such that their end edge is aligned to the end edge of the cross axis.
     */
    End,
}

typealias FlowMainAxisAlignment = MainAxisAlignment

/**
 * Layout model that arranges its children in a horizontal or vertical flow.
 */
@Composable
private fun Flow(
    orientation: LayoutOrientation,
    mainAxisSize: SizeMode,
    mainAxisAlignment: FlowMainAxisAlignment,
    mainAxisSpacing: Dp,
    crossAxisAlignment: FlowCrossAxisAlignment,
    crossAxisSpacing: Dp,
    lastLineMainAxisAlignment: FlowMainAxisAlignment,
    children: @Composable () -> Unit
) {
    fun Placeable.mainAxisSize() =
        if (orientation == LayoutOrientation.Horizontal) width else height
    fun Placeable.crossAxisSize() =
        if (orientation == LayoutOrientation.Horizontal) height else width

    Layout(children) { measurables, outerConstraints, layoutDirection ->
        val sequences = mutableListOf<List<Placeable>>()
        val crossAxisSizes = mutableListOf<IntPx>()
        val crossAxisPositions = mutableListOf<IntPx>()

        var mainAxisSpace = IntPx.Zero
        var crossAxisSpace = IntPx.Zero

        val currentSequence = mutableListOf<Placeable>()
        var currentMainAxisSize = IntPx.Zero
        var currentCrossAxisSize = IntPx.Zero

        val constraints = OrientationIndependentConstraints(outerConstraints, orientation)

        val childConstraints = if (orientation == LayoutOrientation.Horizontal) {
            Constraints(maxWidth = constraints.mainAxisMax)
        } else {
            Constraints(maxHeight = constraints.mainAxisMax)
        }

        // Return whether the placeable can be added to the current sequence.
        fun canAddToCurrentSequence(placeable: Placeable) =
            currentSequence.isEmpty() || currentMainAxisSize + mainAxisSpacing.toIntPx() +
                    placeable.mainAxisSize() <= constraints.mainAxisMax

        // Store current sequence information and start a new sequence.
        fun startNewSequence() {
            if (sequences.isNotEmpty()) {
                crossAxisSpace += crossAxisSpacing.toIntPx()
            }
            sequences += currentSequence.toList()
            crossAxisSizes += currentCrossAxisSize
            crossAxisPositions += crossAxisSpace

            crossAxisSpace += currentCrossAxisSize
            mainAxisSpace = max(mainAxisSpace, currentMainAxisSize)

            currentSequence.clear()
            currentMainAxisSize = IntPx.Zero
            currentCrossAxisSize = IntPx.Zero
        }

        for (measurable in measurables) {
            // Ask the child for its preferred size.
            val placeable = measurable.measure(childConstraints)

            // Start a new sequence if there is not enough space.
            if (!canAddToCurrentSequence(placeable)) startNewSequence()

            // Add the child to the current sequence.
            if (currentSequence.isNotEmpty()) {
                currentMainAxisSize += mainAxisSpacing.toIntPx()
            }
            currentSequence.add(placeable)
            currentMainAxisSize += placeable.mainAxisSize()
            currentCrossAxisSize = max(currentCrossAxisSize, placeable.crossAxisSize())
        }

        if (currentSequence.isNotEmpty()) startNewSequence()

        val mainAxisLayoutSize = if (constraints.mainAxisMax.isFinite() &&
            mainAxisSize == SizeMode.Expand
        ) {
            constraints.mainAxisMax
        } else {
            max(mainAxisSpace, constraints.mainAxisMin)
        }
        val crossAxisLayoutSize = max(crossAxisSpace, constraints.crossAxisMin)

        val layoutWidth = if (orientation == LayoutOrientation.Horizontal) {
            mainAxisLayoutSize
        } else {
            crossAxisLayoutSize
        }
        val layoutHeight = if (orientation == LayoutOrientation.Horizontal) {
            crossAxisLayoutSize
        } else {
            mainAxisLayoutSize
        }

        layout(layoutWidth, layoutHeight) {
            sequences.fastForEachIndexed { i, placeables ->
                val childrenMainAxisSizes = placeables.mapIndexed { j, placeable ->
                    placeable.mainAxisSize() +
                        if (j < placeables.lastIndex) mainAxisSpacing.toIntPx() else IntPx.Zero
                }
                val arrangement = if (i < sequences.lastIndex) {
                    mainAxisAlignment.arrangement
                } else {
                    lastLineMainAxisAlignment.arrangement
                }
                // TODO(soboleva): rtl support
                // Handle vertical direction
                val mainAxisPositions = arrangement.arrange(
                    mainAxisLayoutSize,
                    childrenMainAxisSizes,
                    layoutDirection
                )
                placeables.fastForEachIndexed { j, placeable ->
                    val crossAxis = when (crossAxisAlignment) {
                        FlowCrossAxisAlignment.Start -> IntPx.Zero
                        FlowCrossAxisAlignment.End ->
                            crossAxisSizes[i] - placeable.crossAxisSize()
                        FlowCrossAxisAlignment.Center ->
                            Alignment.Center.align(
                                IntPxSize(
                                    width = IntPx.Zero,
                                    height = crossAxisSizes[i] - placeable.crossAxisSize()
                                )
                            ).y
                    }
                    if (orientation == LayoutOrientation.Horizontal) {
                        placeable.placeAbsolute(
                            x = mainAxisPositions[j],
                            y = crossAxisPositions[i] + crossAxis
                        )
                    } else {
                        placeable.placeAbsolute(
                            x = crossAxisPositions[i] + crossAxis,
                            y = mainAxisPositions[j]
                        )
                    }
                }
            }
        }
    }
}
