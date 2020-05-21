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
import androidx.compose.Stable

import androidx.ui.core.AlignmentLine
import androidx.ui.core.Constraints
import androidx.ui.core.HorizontalAlignmentLine
import androidx.ui.core.Layout
import androidx.ui.core.LayoutDirection
import androidx.ui.core.LayoutModifier
import androidx.ui.core.Measurable
import androidx.ui.core.MeasureScope
import androidx.ui.core.Modifier
import androidx.ui.unit.Dp
import androidx.ui.unit.dp
import androidx.ui.unit.ipx

/**
 * Note: This composable is on the deprecation path and will be soon replaced with a [Modifier].
 *
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
@Deprecated(
    "AlignmentLineOffset is deprecated. Instead apply Modifier.relativePaddingFrom modifier to " +
            "the children",
    replaceWith = ReplaceWith(
        "Stack(Modifier.relativePaddingFrom(alignmentLine, before, after), children)",
        "import androidx.ui.layout.relativePaddingFrom",
        "import androidx.ui.layout.Stack"
    )
)
@Composable
fun AlignmentLineOffset(
    alignmentLine: AlignmentLine,
    modifier: Modifier = Modifier,
    before: Dp = 0.dp,
    after: Dp = 0.dp,
    children: @Composable () -> Unit
) {
    Layout(children, modifier) { measurables, constraints, _ ->
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
            placeable.placeAbsolute(x, y)
        }
    }
}

/**
 * Allow the content to be positioned according to the specified offset relative to the
 * [alignment line][AlignmentLine], subject to the incoming layout constraints.
 *
 * The modified layout will include the needed padding, such that the distance from its borders
 * to the [alignmentLine] of the content box will be [before] and [after], respectively.
 * The [before] and [after] values will be interpreted as offsets on the axis corresponding to
 * the alignment line.
 *
 * @param alignmentLine the alignment line to be used for positioning the content
 * @param before the offset between the container's top edge and the horizontal alignment line, or
 * the container's start edge and the vertical alignment line
 * @param after the offset between the container's bottom edge and the horizontal alignment line, or
 * the container's end edge and the vertical alignment line
 *
 * Example usage:
 * @sample androidx.ui.layout.samples.RelativePaddingFromSample
 */
@Stable
fun Modifier.relativePaddingFrom(
    alignmentLine: AlignmentLine,
    before: Dp = 0.dp,
    after: Dp = 0.dp
): Modifier = this + AlignmentLineOffset(alignmentLine, before, after)

private data class AlignmentLineOffset(
    val alignmentLine: AlignmentLine,
    val before: Dp = 0.dp,
    val after: Dp = 0.dp
) : LayoutModifier {
    init {
        require(before.value >= 0f && after.value >= 0f) {
            "Padding from alignment line must be non-negative"
        }
    }
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ): MeasureScope.MeasureResult {
        val placeable = measurable.measure(
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

        val width =
            if (alignmentLine.horizontal) placeable.width
            else paddingBefore + placeable.width + paddingAfter
        val height =
            if (alignmentLine.horizontal) paddingBefore + placeable.height + paddingAfter
            else placeable.height
        return layout(width, height) {
            val x = if (alignmentLine.horizontal) 0.ipx else paddingBefore
            val y = if (alignmentLine.horizontal) paddingBefore else 0.ipx
            placeable.place(x, y)
        }
    }
}

private val AlignmentLine.horizontal: Boolean get() = this is HorizontalAlignmentLine
