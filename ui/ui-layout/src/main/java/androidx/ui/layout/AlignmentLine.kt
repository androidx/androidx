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
import androidx.compose.composer
import androidx.ui.core.AlignmentLine
import androidx.ui.core.Dp
import androidx.ui.core.HorizontalAlignmentLine
import androidx.ui.core.Layout
import androidx.ui.core.coerceIn
import androidx.ui.core.dp
import androidx.ui.core.ipx
import androidx.ui.core.looseMin

/**
 * Layout widget that takes a child and tries to position it within itself according to
 * specified offsets relative to an [alignment line][AlignmentLine], subject to the incoming
 * layout constraints. The [AlignmentLineOffset] layout will try to size itself to wrap the
 * child and include the needed padding, such that the distance from the [AlignmentLineOffset]
 * borders to the [AlignmentLine] of the child will be [before] and [after], respectively.
 * The [before] and [after] values will be interpreted as offsets on the axis corresponding to
 * the alignment line.
 * TODO(popam): consider RTL here
 *
 * @param alignmentLine the alignment line to be used for positioning the child
 * @param before the offset between the left or top container border and the alignment line
 * @param after the offset between the bottom or right container border and the alignment line
 */
@Composable
fun AlignmentLineOffset(
    alignmentLine: AlignmentLine,
    before: Dp = 0.dp,
    after: Dp = 0.dp,
    children: @Composable() () -> Unit
) {
    Layout(children) { measurables, constraints ->
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
        // Calculate the size of the AlignmentLineOffset widget & define layout.
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