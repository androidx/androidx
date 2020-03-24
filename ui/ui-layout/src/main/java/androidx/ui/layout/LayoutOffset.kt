/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.ui.core.LayoutDirection
import androidx.ui.core.LayoutModifier
import androidx.ui.unit.Density
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPxPosition
import androidx.ui.unit.IntPxSize

/**
 * A [LayoutModifier] that offsets the position of the wrapped layout with the given
 * horizontal and vertical offsets. The offsets can be positive as well as non positive.
 * If the position of the wrapped layout on screen is (posx, posy) and the offsets are [x] and [y],
 * by applying this modifier the position will become (posx + x, posy + y) if the current
 * layout direction is LTR, or (posx - x, posy + y) otherwise.
 * The [LayoutOffset] modifier should always be used instead of [LayoutPadding] whenever only the
 * position of the wrapped layout should be modified, since using [LayoutPadding] will also affect
 * the size of the wrapped layout. Also note that [LayoutPadding] cannot be used with negative
 * padding in order to achieve negative offsets.
 *
 * Example usage:
 * @sample androidx.ui.layout.samples.LayoutOffsetModifier
 *
 * @param x The horizontal offset added to the position of the wrapped layout
 * @param y The vertical offset added to the position of the wrapped layout
 */
data class LayoutOffset(val x: Dp, val y: Dp) : LayoutModifier {
    override fun Density.modifyPosition(
        childSize: IntPxSize,
        containerSize: IntPxSize,
        layoutDirection: LayoutDirection
    ) = IntPxPosition(
        (if (layoutDirection == LayoutDirection.Ltr) x else -x).toIntPx(),
        y.toIntPx()
    )
}
