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
import androidx.ui.core.AlignmentLine
import androidx.ui.core.HorizontalAlignmentLine
import androidx.ui.core.Layout
import androidx.ui.core.Modifier
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import androidx.ui.unit.isFinite
import androidx.ui.unit.max

/**
 * Layout composable that takes a child and tries to position it within itself according to
 * specified offsets relative to an [alignment line][AlignmentLine], subject to the incoming
 * layout constraints. The [AlignmentLineOffset] layout will try to size itself to wrap the
 * child and include the needed padding, such that the distance from the [AlignmentLineOffset]
 * borders to the [AlignmentLine] of the child will be [before] and [after], respectively.
 * The [before] and [after] values will be interpreted as offsets on the axis corresponding to
 * the alignment line.
 *
 * @param alignmentLine the alignment line to be used for positioning the child
 * @param before the offset between the left or top container border and the alignment line
 * @param after the offset between the bottom or right container border and the alignment line
 */
// TODO(popam): consider RTL here
@Composable
fun AlignmentLineOffset(
    alignmentLine: AlignmentLine,
    modifier: Modifier = Modifier.None,
    before: Dp = 0.dp,
    after: Dp = 0.dp,
    children: @Composable() () -> Unit
) {
    Layout(children, modifier) { measurables, constraints ->
        require(measurables.isNotEmpty()) { "No child found in AlignmentLineOffset" }
        val placeable = measurables.first().measure(
            // Loose constraints perpendicular on the alignment line.
            if (alignmentLine.horizontal) constraints.copy(minHeight = 0.ipx)
            else constraints.copy(minWidth = 0.ipx)
        )
        val linePosition = placeable[alignmentLine] ?: 0.ipx
        val axis = if (alignmentLine.horizontal) placeable.height else placeable.width
        val axisMax = if (alignmentLine.horizontal) constraints.maxHeight else constraints.maxWidth
        // Compute padding required to satisfy the total before and after offsets.
        val paddingBefore = (before.toIntPx() - linePosition).coerceIn(0.ipx, axisMax - axis)
        val paddingAfter = (after.toIntPx() - axis + linePosition).coerceIn(
            0.ipx,
            axisMax - axis - paddingBefore
        )
        // Calculate the size of the AlignmentLineOffset composable & define layout.
        val containerWidth =
            if (alignmentLine.horizontal) placeable.width
            else paddingBefore + placeable.width + paddingAfter
        val containerHeight =
            if (alignmentLine.horizontal) paddingBefore + placeable.height + paddingAfter
            else placeable.height
        layout(containerWidth, containerHeight) {
            val x = if (alignmentLine.horizontal) 0.ipx else paddingBefore
            val y = if (alignmentLine.horizontal) paddingBefore else 0.ipx
            placeable.place(x, y)
        }
    }
}

/**
 * Layout composable that takes a child and positions it within itself such that the specified
 * alignment line is centered. The layout will expand to fill the available space in the
 * alignment line axis. If infinite space is available, the layout will wrap the child and
 * add the least possible amount of padding such that the centering will work, assuming that
 * the child provides the alignment line. The layout will wrap the child in the axis opposite
 * to the alignment line axis. If the child does not provide the specified alignment line, the
 * child will be centered in the parent (same behavior as [Center]). Similarly, if the child
 * decides to be smaller than the min constraints of the layout in the axis opposite to the
 * alignment line axis, the child will be centered with respect to that axis. To make the
 * layout expand to fill the available space in the axis opposite to the alignment line,
 * consider wrapping this composable in an [Align].
 *
 * @param alignmentLine the alignment line to be centered in the container
 *
 * @see Center
 * @see Align
 */
@Composable
fun CenterAlignmentLine(alignmentLine: AlignmentLine, children: @Composable() () -> Unit) {
    Layout(children) { measurables, constraints ->
        require(measurables.isNotEmpty()) { "No child found in CenterAlignmentLine" }
        val placeable = measurables.first().measure(
            // Loose constraints perpendicular on the alignment line.
            if (alignmentLine.horizontal) constraints.copy(minHeight = 0.ipx)
            else constraints.copy(minWidth = 0.ipx)
        )
        // TODO(popam): This is a workaround for b/140231932
        placeable[alignmentLine]
        // Expand in the direction of the alignment line and wrap in the other direction.
        val containerWidth =
            if (alignmentLine.horizontal) placeable.width
            else if (constraints.maxWidth.isFinite()) constraints.maxWidth
            else { // Wrap ourselves around the content such that the alignment line is centered.
                val linePosition = placeable[alignmentLine]
                if (linePosition != null) max(linePosition, placeable.width - linePosition) * 2
                else placeable.width
            }
        val containerHeight =
            if (!alignmentLine.horizontal) placeable.height
            else if (constraints.maxHeight.isFinite()) constraints.maxHeight
            else { // Wrap ourselves around the content such that the alignment line is centered.
                val linePosition = placeable[alignmentLine]
                if (linePosition != null) max(linePosition, placeable.height - linePosition) * 2
                else placeable.height
            }
        layout(containerWidth, containerHeight) {
            // Center child in cross axis or if alignment line is null.
            val centeredPosition = Alignment.Center.align(
                IntPxSize(containerWidth - placeable.width, containerHeight - placeable.height)
            )
            val childX = if (alignmentLine.horizontal) {
                centeredPosition.x
            } else {
                val linePosition = placeable[alignmentLine] ?: placeable.width / 2
                containerWidth / 2 - linePosition
            }
            val childY = if (alignmentLine.horizontal) {
                val linePosition = placeable[alignmentLine] ?: placeable.height / 2
                containerHeight / 2 - linePosition
            } else {
                centeredPosition.y
            }
            placeable.place(childX, childY)
        }
    }
}

private val AlignmentLine.horizontal: Boolean get() = this is HorizontalAlignmentLine
