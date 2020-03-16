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
import androidx.ui.core.LayoutDirection
import androidx.ui.core.LayoutModifier
import androidx.ui.unit.Density
import androidx.ui.core.offset
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.dp

/**
 * Layout modifier that applies the same padding of [all] dp on each side of the wrapped layout.
 * The requested padding will be subtracted from the available space before the wrapped layout has
 * the chance to choose its own size, so conceptually the padding has higher priority to occupy
 * the available space than the content.
 * If you only need to modify the position of the wrapped layout without affecting its size
 * as described above, you should use the [LayoutOffset] modifier instead.
 * Also note that padding must be non-negative. If you consider using negative (or positive)
 * padding to offset the wrapped layout, [LayoutOffset] should be used.
 *
 * Example usage:
 * @sample androidx.ui.layout.samples.LayoutPaddingAllModifier
 *
 * @see LayoutOffset
 */
fun LayoutPadding(all: Dp): LayoutPadding = LayoutPadding(
    start = all,
    top = all,
    end = all,
    bottom = all
)

/**
 * A [LayoutModifier] that adds [start], [top], [end] and [bottom] padding to the wrapped layout.
 * The requested padding will be subtracted from the available space before the wrapped layout has
 * the chance to choose its own size, so conceptually the padding has higher priority to occupy
 * the available space than the content.
 * If you only need to modify the position of the wrapped layout without affecting its size
 * as described above, you should use the [LayoutOffset] modifier instead.
 * Also note that padding must be non-negative. If you consider using negative (or positive)
 * padding to offset the wrapped layout, [LayoutOffset] should be used.
 *
 * Example usage:
 * @sample androidx.ui.layout.samples.LayoutPaddingModifier
 *
 * @see LayoutOffset
 */
data class LayoutPadding(
    val start: Dp = 0.dp,
    val top: Dp = 0.dp,
    val end: Dp = 0.dp,
    val bottom: Dp = 0.dp
) : LayoutModifier {
    init {
        require(
            start.value >= 0f && top.value >= 0f && end.value >= 0f && bottom.value >= 0f,
            PaddingMustBeNonNegative
        )
    }

    override fun Density.modifyConstraints(
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ) = constraints.offset(
        horizontal = -start.toIntPx() - end.toIntPx(),
        vertical = -top.toIntPx() - bottom.toIntPx()
    )

    override fun Density.modifySize(
        constraints: Constraints,
        layoutDirection: LayoutDirection,
        childSize: IntPxSize
    ) = IntPxSize(
        (start.toIntPx() + childSize.width + end.toIntPx())
            .coerceIn(constraints.minWidth, constraints.maxWidth),
        (top.toIntPx() + childSize.height + bottom.toIntPx())
            .coerceIn(constraints.minHeight, constraints.maxHeight)
    )

    override fun Density.modifyPosition(
        childSize: IntPxSize,
        containerSize: IntPxSize,
        layoutDirection: LayoutDirection
    ): IntPxPosition = if (layoutDirection == LayoutDirection.Ltr) {
        IntPxPosition(start.toIntPx(), top.toIntPx())
    } else {
        IntPxPosition(end.toIntPx(), top.toIntPx())
    }

    internal companion object {
        val PaddingMustBeNonNegative = { "Padding must be non-negative" }
    }
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
