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

import androidx.compose.Immutable
import androidx.ui.core.Constraints
import androidx.ui.core.LayoutModifier
import androidx.ui.core.ModifierScope
import androidx.ui.core.offset
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.dp

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
    override fun ModifierScope.modifyConstraints(
        constraints: Constraints
    ) = constraints.offset(
        horizontal = -left.toIntPx() - right.toIntPx(),
        vertical = -top.toIntPx() - bottom.toIntPx()
    )

    override fun ModifierScope.modifySize(
        constraints: Constraints,
        childSize: IntPxSize
    ) = IntPxSize(
        (left.toIntPx() + childSize.width + right.toIntPx())
            .coerceIn(constraints.minWidth, constraints.maxWidth),
        (top.toIntPx() + childSize.height + bottom.toIntPx())
            .coerceIn(constraints.minHeight, constraints.maxHeight)
    )

    override fun ModifierScope.modifyPosition(
        childSize: IntPxSize,
        containerSize: IntPxSize
    ) = IntPxPosition(left.toIntPx(), top.toIntPx())
}

/**
 * Describes a set of offsets from each of the four sides of a box.
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
