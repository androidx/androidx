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
import androidx.compose.Stable
import androidx.ui.core.Constraints
import androidx.ui.core.LayoutDirection
import androidx.ui.core.Modifier
import androidx.ui.core.LayoutModifier
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
 *
 * Example usage:
 * @sample androidx.ui.layout.samples.PaddingModifier
 */
@Stable
fun Modifier.padding(
    start: Dp = 0.dp,
    top: Dp = 0.dp,
    end: Dp = 0.dp,
    bottom: Dp = 0.dp
) = this + PaddingModifier(
    start = start,
    top = top,
    end = end,
    bottom = bottom,
    rtlAware = true
)

/**
 * Apply [horizontal] dp space along the left and right edges of the content, and [vertical] dp
 * space along the top and bottom edges.
 * Padding is applied before content measurement and takes precedence; content may only be as large
 * as the remaining space.
 *
 * Negative padding is not permitted. See [offset].
 *
 * Example usage:
 * @sample androidx.ui.layout.samples.SymmetricPaddingModifier
 */
@Stable
fun Modifier.padding(
    horizontal: Dp = 0.dp,
    vertical: Dp = 0.dp
) = this + PaddingModifier(
    start = horizontal,
    top = vertical,
    end = horizontal,
    bottom = vertical,
    rtlAware = true
)

/**
 * Apply [all] dp of additional space along each edge of the content, left, top, right and bottom.
 * Padding is applied before content measurement and takes precedence; content may only be as large
 * as the remaining space.
 *
 * Negative padding is not permitted. See [offset].
 *
 * Example usage:
 * @sample androidx.ui.layout.samples.PaddingAllModifier
 */
@Stable
fun Modifier.padding(all: Dp) =
    this + PaddingModifier(start = all, top = all, end = all, bottom = all, rtlAware = true)

/**
 * Apply [InnerPadding] to the component as additional space along each edge of the content's left,
 * top, right and bottom. Padding is applied before content measurement and takes precedence;
 * content may only be as large as the remaining space.
 *
 * Negative padding is not permitted. See [offset].
 *
 * Example usage:
 * @sample androidx.ui.layout.samples.PaddingInnerPaddingModifier
 */
fun Modifier.padding(padding: InnerPadding) =
    this + PaddingModifier(
        start = padding.start,
        top = padding.top,
        end = padding.end,
        bottom = padding.bottom,
        rtlAware = true
    )

/**
 * Apply additional space along each edge of the content in [Dp]: [left], [top], [right] and
 * [bottom]. These paddings are applied without regard to the current [LayoutDirection], see
 * [padding] to apply relative paddings. Padding is applied before content measurement and takes
 * precedence; content may only be as large as the remaining space.
 *
 * Negative padding is not permitted. See [offset].
 *
 * Example usage:
 * @sample androidx.ui.layout.samples.AbsolutePaddingModifier
 */
@Stable
fun Modifier.absolutePadding(
    left: Dp = 0.dp,
    top: Dp = 0.dp,
    right: Dp = 0.dp,
    bottom: Dp = 0.dp
) = this + PaddingModifier(
    start = left,
    top = top,
    end = right,
    bottom = bottom,
    rtlAware = false
)

private data class PaddingModifier(
    val start: Dp = 0.dp,
    val top: Dp = 0.dp,
    val end: Dp = 0.dp,
    val bottom: Dp = 0.dp,
    val rtlAware: Boolean
) : LayoutModifier {
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
            if (rtlAware) {
                placeable.place(start.toIntPx(), top.toIntPx())
            } else {
                placeable.placeAbsolute(start.toIntPx(), top.toIntPx())
            }
        }
    }
}

/**
 * Describes a padding to be applied along the edges inside a box.
 */
@Immutable
data class InnerPadding(
    @Stable
    val start: Dp = 0.dp,
    @Stable
    val top: Dp = 0.dp,
    @Stable
    val end: Dp = 0.dp,
    @Stable
    val bottom: Dp = 0.dp
) {
    constructor(all: Dp) : this(all, all, all, all)
}
