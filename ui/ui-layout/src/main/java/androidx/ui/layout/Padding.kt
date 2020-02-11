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
import androidx.compose.Immutable
import androidx.ui.core.Constraints
import androidx.ui.core.Layout
import androidx.ui.core.LayoutModifier
import androidx.ui.core.offset
import androidx.ui.unit.Density
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.dp
import androidx.ui.unit.min

/**
 * Layout modifier that applies the same padding of [all] dp on each side of the target layout.
 *
 * Example usage:
 * @sample androidx.ui.layout.samples.LayoutPaddingAllModifier
 */
fun LayoutPadding(all: Dp): LayoutPadding = LayoutPadding(
    left = all,
    top = all,
    right = all,
    bottom = all
)

/**
 * A [LayoutModifier] that adds [left], [top], [right] and [bottom] padding
 * to the wrapped layout.
 *
 * Example usage:
 * @sample androidx.ui.layout.samples.LayoutPaddingModifier
 */
data class LayoutPadding(
    val left: Dp = 0.dp,
    val top: Dp = 0.dp,
    val right: Dp = 0.dp,
    val bottom: Dp = 0.dp
) : LayoutModifier {
    override fun Density.modifyConstraints(
        constraints: Constraints
    ) = constraints.offset(
        horizontal = -left.toIntPx() - right.toIntPx(),
        vertical = -top.toIntPx() - bottom.toIntPx()
    )

    override fun Density.modifySize(
        constraints: Constraints,
        childSize: IntPxSize
    ) = IntPxSize(
        (left.toIntPx() + childSize.width + right.toIntPx())
            .coerceIn(constraints.minWidth, constraints.maxWidth),
        (top.toIntPx() + childSize.height + bottom.toIntPx())
            .coerceIn(constraints.minHeight, constraints.maxHeight)
    )

    override fun Density.modifyPosition(
        childSize: IntPxSize,
        containerSize: IntPxSize
    ) = IntPxPosition(left.toIntPx(), top.toIntPx())
}

/**
 * Describes a set of offsets from each of the four sides of a box. For example,
 * it is used to describe padding for the [Padding] composable.
 */
@Immutable
data class EdgeInsets(
    val left: Dp = 0.dp,
    val top: Dp = 0.dp,
    val right: Dp = 0.dp,
    val bottom: Dp = 0.dp
) {
    constructor(all: Dp) : this(all, all, all, all)
}

/**
 * Layout composable that takes a child composable and applies whitespace padding around it.
 * When passing layout constraints to its child, [Padding] shrinks the constraints by the
 * requested padding, causing the child to layout at a smaller size.
 *
 * Example usage:
 * @sample androidx.ui.layout.samples.PaddingComposableEdgeInsets
 */
@Composable
fun Padding(
    padding: EdgeInsets,
    children: @Composable() () -> Unit
) {
    Layout(children) { measurables, constraints ->
        val measurable = measurables.firstOrNull()
        if (measurable == null) {
            layout(constraints.minWidth, constraints.minHeight) { }
        } else {
            val paddingLeft = padding.left.toIntPx()
            val paddingTop = padding.top.toIntPx()
            val paddingRight = padding.right.toIntPx()
            val paddingBottom = padding.bottom.toIntPx()
            val horizontalPadding = (paddingLeft + paddingRight)
            val verticalPadding = (paddingTop + paddingBottom)

            val newConstraints = constraints.offset(-horizontalPadding, -verticalPadding)
            val placeable = measurable.measure(newConstraints)
            val width =
                min(placeable.width + horizontalPadding, constraints.maxWidth)
            val height =
                min(placeable.height + verticalPadding, constraints.maxHeight)

            layout(width, height) {
                placeable.place(paddingLeft, paddingTop)
            }
        }
    }
}

/**
 * Layout composable that takes a child composable and applies whitespace padding around it.
 *
 * When passing layout constraints to its child, [Padding] shrinks the constraints by the
 * requested padding, causing the child to layout at a smaller size.
 *
 * Example usage:
 * @sample androidx.ui.layout.samples.PaddingComposable
 */
@Composable
fun Padding(
    left: Dp = 0.dp,
    top: Dp = 0.dp,
    right: Dp = 0.dp,
    bottom: Dp = 0.dp,
    children: @Composable() () -> Unit
) {
    Padding(
        padding = EdgeInsets(left = left, top = top, right = right, bottom = bottom),
        children = children
    )
}

/**
 * Layout composable that takes a child composable and applies
 * the same amount of whitespace padding around it.
 *
 * When passing layout constraints to its child, [Padding] shrinks the constraints by the
 * requested padding, causing the child to layout at a smaller size.
 *
 * Example usage:
 * @sample androidx.ui.layout.samples.PaddingComposableSameInset
 */
@Composable
fun Padding(
    padding: Dp,
    children: @Composable() () -> Unit
) {
    Padding(padding = EdgeInsets(padding), children = children)
}
