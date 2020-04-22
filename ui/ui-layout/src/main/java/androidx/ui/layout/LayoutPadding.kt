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
import androidx.ui.core.Modifier
import androidx.ui.core.LayoutModifier2
import androidx.ui.core.Measurable
import androidx.ui.core.MeasureScope
import androidx.ui.core.offset
import androidx.ui.unit.Dp
import androidx.ui.unit.dp

/**
 * Apply additional space along each edge of the content in [Dp]: [start], [top], [end] and
 * [bottom]. The start and end edges will be determined by the current [LayoutDirection].
 * Padding is applied before content measurement and takes precedence; content may only be as large
 * as the remaining space.
 *
 * Negative padding is not permitted. See [offset].
 */
@Suppress("DEPRECATION")
fun Modifier.padding(
    start: Dp = 0.dp,
    top: Dp = 0.dp,
    end: Dp = 0.dp,
    bottom: Dp = 0.dp
) = this + LayoutPadding(
    start = start,
    top = top,
    end = end,
    bottom = bottom
)

/**
 * Apply [all]dp of additional space along each edge of the content, left, top, right and bottom.
 * Padding is applied before content measurement and takes precedence; content may only be as large
 * as the remaining space.
 */
fun Modifier.padding(all: Dp) = padding(start = all, top = all, end = all, bottom = all)

/**
 * Apply additional space along each edge of the content in [Dp]: [left], [top], [right] and
 * [bottom]. These paddings are applied without regard to the current [LayoutDirection], see
 * [padding] to apply relative paddings. Padding is applied before content measurement and takes
 * precedence; content may only be as large as the remaining space.
 *
 * Negative padding is not permitted. See [offset].
 */
@Suppress("DEPRECATION")
fun Modifier.absolutePadding(
    left: Dp = 0.dp,
    top: Dp = 0.dp,
    right: Dp = 0.dp,
    bottom: Dp = 0.dp
) = this + LayoutPaddingAbsolute(
    left = left,
    top = top,
    right = right,
    bottom = bottom
)

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
@Suppress("DEPRECATION")
@Deprecated(
    "Use Modifier.padding",
    replaceWith = ReplaceWith(
        "Modifier.padding(all)",
        "androidx.ui.core.Modifier",
        "androidx.ui.layout.padding"
    )
)
fun LayoutPadding(all: Dp): LayoutPadding = LayoutPadding(
    start = all,
    top = all,
    end = all,
    bottom = all
)

/**
 * A [LayoutModifier2] that adds [start], [top], [end] and [bottom] padding to the wrapped layout.
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
 * @see LayoutPaddingAbsolute
 */
data class LayoutPadding
@Deprecated(
    "Use Modifier.padding",
    replaceWith = ReplaceWith(
        "Modifier.padding(start, top, end, bottom)",
        "androidx.ui.core.Modifier",
        "androidx.ui.layout.padding"
    )
)
constructor(
    val start: Dp = 0.dp,
    val top: Dp = 0.dp,
    val end: Dp = 0.dp,
    val bottom: Dp = 0.dp
) : LayoutModifier2 {
    init {
        require(start.value >= 0f && top.value >= 0f && end.value >= 0f && bottom.value >= 0f) {
            "Padding must be non-negative"
        }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ): MeasureScope.MeasureResult {
        val horizontal = start.toIntPx() + end.toIntPx()
        val vertical = top.toIntPx() + bottom.toIntPx()

        val placeable = measurable.measure(constraints.offset(-horizontal, -vertical))

        val width = (placeable.width + horizontal)
            .coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = (placeable.height + vertical)
            .coerceIn(constraints.minHeight, constraints.maxHeight)
        return layout(width, height) {
            placeable.place(start.toIntPx(), top.toIntPx())
        }
    }
}

/**
 * A [LayoutModifier2] that adds [left], [top], [right] and [bottom] padding to the wrapped layout
 * without regard for layout direction.
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
 * @see LayoutPadding
 */
data class LayoutPaddingAbsolute
@Deprecated(
    "Use Modifier.absolutePadding",
    replaceWith = ReplaceWith(
        "Modifier.absolutePadding(left, top, right, bottom)",
        "androidx.ui.core.Modifier",
        "androidx.ui.layout.absolutePadding"
    )
)
constructor(
    val left: Dp = 0.dp,
    val top: Dp = 0.dp,
    val right: Dp = 0.dp,
    val bottom: Dp = 0.dp
) : LayoutModifier2 {
    init {
        require(left.value >= 0f && top.value >= 0f && right.value >= 0f && bottom.value >= 0f) {
            "Padding must be non-negative"
        }
    }

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints,
        layoutDirection: LayoutDirection
    ): MeasureScope.MeasureResult {
        val horizontal = left.toIntPx() + right.toIntPx()
        val vertical = top.toIntPx() + bottom.toIntPx()

        val placeable = measurable.measure(constraints.offset(-horizontal, -vertical))

        val width = (placeable.width + horizontal)
            .coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = (placeable.height + vertical)
            .coerceIn(constraints.minHeight, constraints.maxHeight)
        return layout(width, height) {
            placeable.placeAbsolute(left.toIntPx(), top.toIntPx())
        }
    }
}

/**
 * Describes a padding to be applied along the edges inside a box.
 */
@Immutable
data class InnerPadding(
    val start: Dp = 0.dp,
    val top: Dp = 0.dp,
    val end: Dp = 0.dp,
    val bottom: Dp = 0.dp
) {
    constructor(all: Dp) : this(all, all, all, all)
}

/**
 * Describes a set of offsets from each of the four sides of a box.
 */
@Immutable
@Deprecated(
    "EdgeInsets is deprecated; please use InnerPadding instead.",
    ReplaceWith("InnerPadding", "androidx.ui.layout.InnerPadding")
)
data class EdgeInsets(
    val left: Dp = 0.dp,
    val top: Dp = 0.dp,
    val right: Dp = 0.dp,
    val bottom: Dp = 0.dp
) {
    constructor(all: Dp) : this(all, all, all, all)
}
