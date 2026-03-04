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

package androidx.xr.compose.subspace.layout

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.xr.compose.subspace.node.SubspaceLayoutModifierNode
import androidx.xr.compose.subspace.node.SubspaceModifierNodeElement
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.compose.unit.constrainDepth
import androidx.xr.compose.unit.constrainHeight
import androidx.xr.compose.unit.constrainWidth
import androidx.xr.compose.unit.offset
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3

/**
 * Apply additional space along each edge of the content in [Dp]: [start], [top], [end], [bottom],
 * [front] and [back]. The start and end edges will be determined by the current [LayoutDirection].
 * Padding is applied before content measurement and takes precedence; content may only be as large
 * as the remaining space. To not consider the layout direction when applying the padding, see
 * [absolutePadding].
 *
 * Negative padding is not permitted — it will cause [IllegalArgumentException].
 *
 * @param start The amount of space at the start edge of the content. Start edge is left if the
 *   layout direction is LTR, or right for RTL.
 * @param top The amount of space at the top edge of the content.
 * @param end The amount of space at the end edge of the content. End edge is right if the layout
 *   direction is LTR, or left for RTL.
 * @param bottom The amount of space at the bottom edge of the content.
 * @param front The amount of space at the front edge of the content.
 * @param back The amount of space at the back edge of the content.
 * @see absolutePadding
 */
public fun SubspaceModifier.padding(
    start: Dp = 0.dp,
    top: Dp = 0.dp,
    end: Dp = 0.dp,
    bottom: Dp = 0.dp,
    front: Dp = 0.dp,
    back: Dp = 0.dp,
): SubspaceModifier =
    this then
        SubspacePaddingElement(
            start = start,
            top = top,
            end = end,
            bottom = bottom,
            front = front,
            back = back,
            rtlAware = true,
        )

/**
 * Apply [horizontal] dp space along the left and right edges of the content, [vertical] dp space
 * along the top and bottom edges, and [depth] dp space along front and back edged. Padding is
 * applied before content measurement and takes precedence; content may only be as large as the
 * remaining space.
 *
 * Negative padding is not permitted — it will cause [IllegalArgumentException]. See [padding]
 *
 * @param horizontal The amount of space at the left and right edges of the content.
 * @param vertical The amount of space at the top and bottom edges of the content.
 * @param depth The amount of space at the front and back edges of the content.
 */
public fun SubspaceModifier.padding(
    horizontal: Dp = 0.dp,
    vertical: Dp = 0.dp,
    depth: Dp = 0.dp,
): SubspaceModifier =
    this then
        SubspacePaddingElement(
            start = horizontal,
            top = vertical,
            end = horizontal,
            bottom = vertical,
            front = depth,
            back = depth,
            rtlAware = true,
        )

/**
 * Apply [all] dp of additional space along each edge of the content, left, top, right, bottom,
 * front, and back. Padding is applied before content measurement and takes precedence; content may
 * only be as large as the remaining space.
 *
 * Negative padding is not permitted — it will cause [IllegalArgumentException]. See [padding]
 *
 * @param all The amount of space at each edge of the content.
 */
public fun SubspaceModifier.padding(all: Dp): SubspaceModifier =
    this then
        SubspacePaddingElement(
            start = all,
            top = all,
            end = all,
            bottom = all,
            front = all,
            back = all,
            rtlAware = true,
        )

/**
 * Apply additional space along each edge of the content in [Dp]: [left], [top], [right], [bottom],
 * [front] and [back]. Padding is applied before content measurement and takes precedence; content
 * may only be as large as the remaining space. To apply relative padding with layout direction, see
 * [padding].
 *
 * Negative padding is not permitted — it will cause [IllegalArgumentException].
 *
 * @param left The amount of space at the left edge of the content.
 * @param top The amount of space at the top edge of the content.
 * @param right The amount of space at the right edge of the content.
 * @param bottom The amount of space at the bottom edge of the content.
 * @param front The amount of space at the front edge of the content.
 * @param back The amount of space at the back edge of the content.
 * @see padding
 */
public fun SubspaceModifier.absolutePadding(
    left: Dp = 0.dp,
    top: Dp = 0.dp,
    right: Dp = 0.dp,
    bottom: Dp = 0.dp,
    front: Dp = 0.dp,
    back: Dp = 0.dp,
): SubspaceModifier =
    this then
        SubspacePaddingElement(
            start = left,
            top = top,
            end = right,
            bottom = bottom,
            front = front,
            back = back,
            rtlAware = false,
        )

private class SubspacePaddingElement(
    val start: Dp,
    val top: Dp,
    val end: Dp,
    val bottom: Dp,
    val front: Dp,
    val back: Dp,
    val rtlAware: Boolean,
) : SubspaceModifierNodeElement<PaddingNode>() {

    init {
        require(
            start.value >= 0f &&
                top.value >= 0f &&
                end.value >= 0f &&
                bottom.value >= 0f &&
                front.value >= 0f &&
                back.value >= 0f
        ) {
            "Padding must be non-negative"
        }
    }

    override fun create(): PaddingNode {
        return PaddingNode(start, top, end, bottom, front, back, rtlAware)
    }

    override fun update(node: PaddingNode) {
        node.start = start
        node.top = top
        node.end = end
        node.bottom = bottom
        node.front = front
        node.back = back
        node.rtlAware = rtlAware
    }

    override fun hashCode(): Int {
        var result = start.hashCode()
        result = 31 * result + top.hashCode()
        result = 31 * result + end.hashCode()
        result = 31 * result + bottom.hashCode()
        result = 31 * result + front.hashCode()
        result = 31 * result + back.hashCode()
        result = 31 * result + rtlAware.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherElement = other as? PaddingNode ?: return false

        return start == otherElement.start &&
            top == otherElement.top &&
            end == otherElement.end &&
            bottom == otherElement.bottom &&
            front == otherElement.front &&
            back == otherElement.back &&
            rtlAware == otherElement.rtlAware
    }
}

private class PaddingNode(
    var start: Dp,
    var top: Dp,
    var end: Dp,
    var bottom: Dp,
    var front: Dp,
    var back: Dp,
    var rtlAware: Boolean,
) : SubspaceLayoutModifierNode, SubspaceModifier.Node() {
    override fun SubspaceMeasureScope.measure(
        measurable: SubspaceMeasurable,
        constraints: VolumeConstraints,
    ): SubspaceMeasureResult {
        val horizontal = start.roundToPx() + end.roundToPx()
        val vertical = top.roundToPx() + bottom.roundToPx()
        val frontAndBack = front.roundToPx() + back.roundToPx()

        val subspacePlaceable =
            measurable.measure(constraints.offset(-horizontal, -vertical, -frontAndBack))

        val width = constraints.constrainWidth(subspacePlaceable.measuredWidth + horizontal)
        val height = constraints.constrainHeight(subspacePlaceable.measuredHeight + vertical)
        val depth = constraints.constrainDepth(subspacePlaceable.measuredDepth + frontAndBack)

        return layout(width, height, depth) {
            val pose =
                Pose(
                    Vector3(
                        ((start - end) / 2.0f).roundToPx().toFloat(),
                        ((bottom - top) / 2.0f).roundToPx().toFloat(),
                        ((back - front) / 2.0f).roundToPx().toFloat(),
                    )
                )
            if (rtlAware) {
                subspacePlaceable.placeRelative(pose)
            } else {
                subspacePlaceable.place(pose)
            }
        }
    }
}
