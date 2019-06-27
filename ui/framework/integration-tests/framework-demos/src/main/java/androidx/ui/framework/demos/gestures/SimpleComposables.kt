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

package androidx.ui.framework.demos.gestures

import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.memo
import androidx.compose.unaryPlus
import androidx.ui.core.Constraints
import androidx.ui.core.Dp
import androidx.ui.core.Layout
import androidx.ui.core.Px
import androidx.ui.core.dp
import androidx.ui.core.ipx
import androidx.ui.graphics.Color
import androidx.ui.painting.Paint
import androidx.compose.composer
import androidx.ui.core.Draw

/**
 * A simple layout composable that matches the size of it's parent layout.
 */
@Composable
internal fun MatchParent(@Children children: @Composable() () -> Unit) {
    Layout({
        children()
    }, { _, constraints ->
        layout(constraints.maxWidth, constraints.maxHeight) {}
    })
}

@Composable
internal fun Center(@Children children: @Composable() () -> Unit) {
    Layout(children) { measurables, constraints ->
        val placeable = measurables.first().measure(constraints)
        layout(constraints.maxWidth, constraints.maxHeight) {
            val left = (constraints.maxWidth - placeable.width) / 2
            val top = (constraints.maxHeight - placeable.height) / 2
            placeable.place(left, top)
        }
    }
}

/**
 * A simple composable that pads items by [padding].
 */
@Composable
private fun Padding(padding: Dp?, @Children children: @Composable() () -> Unit) {
    Layout(children) { measurables, constraints ->
        val paddingPx = padding?.toIntPx() ?: 0.ipx
        val doublePadding = paddingPx * 2
        val maxWidth = constraints.maxWidth - doublePadding
        val maxHeight = constraints.maxHeight - doublePadding
        val minWidth =
            if (constraints.minWidth > maxWidth) {
                maxWidth
            } else {
                constraints.minWidth
            }
        val minHeight =
            if (constraints.minHeight > maxHeight) {
                maxHeight
            } else {
                constraints.minHeight
            }
        val placeable = measurables.first().measure(
            Constraints(minWidth, maxWidth, minHeight, maxHeight)
        )
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable.place(paddingPx, paddingPx)
        }
    }
}

/**
 * A simple composable that draws a border around it's children.
 */
@Composable
private fun Border(color: Color, width: Dp, @Children children: @Composable() () -> Unit) {
    Layout(
        children = {
            children()
            Draw { canvas, parentSize ->

                val floatWidth = width.toPx().value

                val backgroundPaint = Paint().apply {
                    this.color = color
                    style = androidx.ui.painting.PaintingStyle.stroke
                    strokeWidth = floatWidth
                }
                canvas.drawRect(
                    androidx.ui.engine.geometry.Rect(
                        floatWidth / 2,
                        floatWidth / 2,
                        parentSize.width.value - floatWidth / 2 + 1,
                        parentSize.height.value - floatWidth / 2 + 1
                    ),
                    backgroundPaint
                )
            }
        },
        layoutBlock = { measurables, constraints ->
            val placeable =
                if (measurables.isNotEmpty()) measurables.first().measure(constraints) else null
            val layoutWidth = placeable?.width ?: constraints.maxWidth
            val layoutHeight = placeable?.height ?: constraints.maxHeight
            layout(layoutWidth, layoutHeight) {
                placeable?.place(0.ipx, 0.ipx)
            }
        })
}

/**
 * A simple composable that contains items within optional [width] and [height] dimensions, wraps
 * the contents in a border (with [borderColor] and [borderWidth]), and optionally pads everything
 * with [padding],
 *
 * If [width] or [height] are not set, the parent's min and max constraints are passed through for
 * the given dimension.  If [padding] is not set, no padding will be applied. If
 * [borderColor] is not set, a reasonable default will be used, and if [borderWidth] is not set,
 * no border will be drawn.
 */
@Composable
internal fun SimpleContainer(
    width: Dp,
    height: Dp,
    padding: Dp,
    @Children children: @Composable() () -> Unit
) {

    val borderWidth: Dp = 2.dp
    val borderColor: Color = Color(0f, 0f, 0f, .12f)

    Layout({
        Padding(padding) {
            Border(
                color = borderColor,
                width = borderWidth
            ) {
                children()
            }
        }
    }, { measurables, constraints ->
        val newConstraints =
            constraints.copy(
                maxWidth = if (width.value >= 0) width.toIntPx() else constraints.maxWidth,
                minWidth = if (width.value >= 0) width.toIntPx() else constraints.minWidth,
                maxHeight = if (height.value >= 0) height.toIntPx() else constraints.maxHeight,
                minHeight = if (height.value >= 0) height.toIntPx() else constraints.minHeight
            )

        val placeable = measurables.first().measure(newConstraints)
        layout(newConstraints.maxWidth, newConstraints.maxHeight) {
            placeable.place(0.ipx, 0.ipx)
        }
    })
}

@Composable
internal fun DrawBox(
    xOffset: Px,
    yOffset: Px,
    width: Px,
    height: Px,
    color: Color
) {
    val paint = +memo { Paint() }
    Draw { canvas, parentSize ->
        paint.color = color
        val centerX = parentSize.width.value / 2 + xOffset.value
        val centerY = parentSize.height.value / 2 + yOffset.value
        val widthValue = if (width.value < 0) parentSize.width.value else width.value
        val heightValue = if (height.value < 0) parentSize.height.value else height.value
        canvas.drawRect(
            androidx.ui.engine.geometry.Rect(
                centerX - widthValue / 2,
                centerY - heightValue / 2,
                centerX + widthValue / 2,
                centerY + heightValue / 2
            ),
            paint
        )
    }
}