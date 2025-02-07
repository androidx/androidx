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
import androidx.compose.ui.unit.dp
import androidx.xr.compose.subspace.node.SubspaceLayoutModifierNode
import androidx.xr.compose.subspace.node.SubspaceModifierElement
import androidx.xr.compose.unit.VolumeConstraints
import androidx.xr.compose.unit.constrainDepth
import androidx.xr.compose.unit.constrainHeight
import androidx.xr.compose.unit.constrainWidth
import androidx.xr.compose.unit.offset
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3

/**
 * Apply additional space along each edge of the content in [Dp]: [left], [top], [right], [bottom],
 * [front] and [back]. Padding is applied before content measurement and takes precedence; content
 * may only be as large as the remaining space.
 *
 * Negative padding is not permitted — it will cause [IllegalArgumentException].
 */
public fun SubspaceModifier.padding(
    left: Dp = 0.dp,
    top: Dp = 0.dp,
    right: Dp = 0.dp,
    bottom: Dp = 0.dp,
    front: Dp = 0.dp,
    back: Dp = 0.dp,
): SubspaceModifier =
    this then
        SubspacePaddingElement(
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            front = front,
            back = back,
        )

/**
 * Apply [horizontal] dp space along the left and right edges of the content, [vertical] dp space
 * along the top and bottom edges, and [depth] dp space along front and back edged. Padding is
 * applied before content measurement and takes precedence; content may only be as large as the
 * remaining space.
 *
 * Negative padding is not permitted — it will cause [IllegalArgumentException]. See [padding]
 */
public fun SubspaceModifier.padding(
    horizontal: Dp = 0.dp,
    vertical: Dp = 0.dp,
    depth: Dp = 0.dp,
): SubspaceModifier =
    this then
        SubspacePaddingElement(
            left = horizontal,
            top = vertical,
            right = horizontal,
            bottom = vertical,
            front = depth,
            back = depth,
        )

/**
 * Apply [all] dp of additional space along each edge of the content, left, top, right, bottom,
 * front, and back. Padding is applied before content measurement and takes precedence; content may
 * only be as large as the remaining space.
 *
 * Negative padding is not permitted — it will cause [IllegalArgumentException]. See [padding]
 */
public fun SubspaceModifier.padding(all: Dp): SubspaceModifier =
    this then
        SubspacePaddingElement(
            left = all,
            top = all,
            right = all,
            bottom = all,
            front = all,
            back = all,
        )

private class SubspacePaddingElement(
    public val left: Dp,
    public val top: Dp,
    public val right: Dp,
    public val bottom: Dp,
    public val front: Dp,
    public val back: Dp,
) : SubspaceModifierElement<PaddingNode>() {

    init {
        require(
            left.value >= 0f &&
                top.value >= 0f &&
                right.value >= 0f &&
                bottom.value >= 0f &&
                front.value >= 0f &&
                back.value >= 0f
        ) {
            "Padding must be non-negative"
        }
    }

    override fun create(): PaddingNode {
        return PaddingNode(left, top, right, bottom, front, back)
    }

    override fun update(node: PaddingNode) {
        node.left = left
        node.top = top
        node.right = right
        node.bottom = bottom
        node.front = front
        node.back = back
    }

    override fun hashCode(): Int {
        var result = left.hashCode()
        result = 31 * result + top.hashCode()
        result = 31 * result + right.hashCode()
        result = 31 * result + bottom.hashCode()
        result = 31 * result + front.hashCode()
        result = 31 * result + back.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherElement = other as? PaddingNode ?: return false

        return left == otherElement.left &&
            top == otherElement.top &&
            right == otherElement.right &&
            bottom == otherElement.bottom &&
            front == otherElement.front &&
            back == otherElement.back
    }
}

private class PaddingNode(
    public var left: Dp,
    public var top: Dp,
    public var right: Dp,
    public var bottom: Dp,
    public var front: Dp,
    public var back: Dp,
) : SubspaceLayoutModifierNode, SubspaceModifier.Node() {
    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: VolumeConstraints,
    ): MeasureResult {
        val horizontal = left.roundToPx() + right.roundToPx()
        val vertical = top.roundToPx() + bottom.roundToPx()
        val frontAndBack = front.roundToPx() + back.roundToPx()

        val placeable =
            measurable.measure(constraints.offset(-horizontal, -vertical, -frontAndBack))

        val width = constraints.constrainWidth(placeable.measuredWidth + horizontal)
        val height = constraints.constrainHeight(placeable.measuredHeight + vertical)
        val depth = constraints.constrainDepth(placeable.measuredDepth + frontAndBack)

        return layout(width, height, depth) {
            placeable.place(
                Pose(
                    Vector3(
                        ((left - right) / 2.0f).roundToPx().toFloat(),
                        ((bottom - top) / 2.0f).roundToPx().toFloat(),
                        ((back - front) / 2.0f).roundToPx().toFloat(),
                    )
                )
            )
        }
    }
}
