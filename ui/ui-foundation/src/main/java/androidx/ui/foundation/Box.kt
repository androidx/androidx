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

package androidx.ui.foundation

import androidx.compose.Composable
import androidx.ui.core.Alignment
import androidx.ui.core.Layout
import androidx.ui.core.Modifier
import androidx.ui.core.offset
import androidx.ui.foundation.shape.RectangleShape
import androidx.ui.graphics.Color
import androidx.ui.graphics.Shape
import androidx.ui.layout.EdgeInsets
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPxSize
import androidx.ui.unit.dp
import androidx.ui.unit.ipx
import androidx.ui.unit.max

/**
 * A convenience composable that combines common layout and draw logic.
 *
 * In order to define the size of the [Box], the [androidx.ui.layout.LayoutWidth],
 * [androidx.ui.layout.LayoutHeight] and [androidx.ui.layout.LayoutSize] modifiers can be used.
 * The [Box] will try to be only as small as its content. However, if it is constrained
 * otherwise, [Box] will allow its content to be smaller and will position the content inside,
 * according to [gravity].
 *
 * The specified [padding] will be applied inside the [Box]. In order to apply padding outside
 * the [Box], the [androidx.ui.layout.LayoutPadding] modifier should be used.
 *
 * @sample androidx.ui.foundation.samples.SimpleCircleBox
 *
 * @param modifier The modifier to be applied to the Box
 * @param shape The shape of the box
 * @param backgroundColor The [Color] for background with. If [Color.Transparent], there will be no
 * background
 * @param border [Border] object that specifies border appearance, such as size and color. If
 * `null`, there will be no border
 * @param padding The padding to be applied inside Box, along its edges. Unless otherwise
 * specified, content will be padded by the [Border.size], if [border] is provided
 * @param paddingLeft specific padding for left side. Setting this will override
 * [padding] for the left side
 * @param paddingTop specific padding for right side. Setting this will override
 * [padding] for the top side
 * @param paddingRight specific padding for top side. Setting this will override
 * [padding] for the right side
 * @param paddingBottom specific padding for bottom side. Setting this will override
 * [padding] for the bottom side
 * @param gravity The gravity of the content inside Box
 */
@Composable
fun Box(
    modifier: Modifier = Modifier.None,
    shape: Shape = RectangleShape,
    backgroundColor: Color = Color.Transparent,
    border: Border? = null,
    padding: Dp = border?.size ?: 0.dp,
    paddingLeft: Dp = Dp.Unspecified,
    paddingTop: Dp = Dp.Unspecified,
    paddingRight: Dp = Dp.Unspecified,
    paddingBottom: Dp = Dp.Unspecified,
    gravity: ContentGravity = ContentGravity.TopLeft,
    children: @Composable() () -> Unit
) {
    val borderModifier =
        if (border != null) DrawBorder(border, shape) else Modifier.None
    val backgroundModifier =
        if (backgroundColor == Color.Transparent) {
            Modifier.None
        } else {
            background(shape, backgroundColor)
        }
    // TODO(malkov): support ContentColor prorogation (b/148129218)
    // TODO(popam): there should be no custom layout, use Column instead (b/148809177)
    Layout(children, modifier + backgroundModifier + borderModifier) { measurables, constraints ->
        val leftPadding = if (paddingLeft != Dp.Unspecified) paddingLeft else padding
        val rightPadding = if (paddingTop != Dp.Unspecified) paddingTop else padding
        val topPadding = if (paddingRight != Dp.Unspecified) paddingRight else padding
        val bottomPadding = if (paddingBottom != Dp.Unspecified) paddingBottom else padding
        val totalHorizontal = leftPadding.toIntPx() + rightPadding.toIntPx()
        val totalVertical = topPadding.toIntPx() + bottomPadding.toIntPx()

        val childConstraints = constraints
            .offset(-totalHorizontal, -totalVertical)
            .copy(minWidth = 0.ipx, minHeight = 0.ipx)

        val placeables = measurables.map { it.measure(childConstraints) }
        var containerWidth = constraints.minWidth
        var containerHeight = constraints.minHeight
        placeables.forEach {
            containerWidth = max(containerWidth, it.width)
            containerHeight = max(containerHeight, it.height)
        }

        layout(containerWidth, containerHeight) {
            placeables.forEach {
                val position = gravity.align(
                    IntPxSize(
                        containerWidth - it.width - totalHorizontal,
                        containerHeight - it.height - totalVertical
                    )
                )
                it.place(
                    leftPadding.toIntPx() + position.x,
                    topPadding.toIntPx() + position.y
                )
            }
        }
    }
}

// TODO(popam/148015674): move EdgeInsets here and rename the API to ContentPadding
typealias ContentPadding = EdgeInsets

// TODO(popam/148014745): add a Gravity class consistent with cross axis alignment for Row/Column
typealias ContentGravity = Alignment