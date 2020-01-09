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

import androidx.compose.Composable
import androidx.compose.remember
import androidx.ui.core.Constraints
import androidx.ui.core.Draw
import androidx.ui.core.Layout
import androidx.ui.graphics.Color
import androidx.ui.graphics.Paint
import androidx.ui.graphics.PaintingStyle
import androidx.ui.unit.Dp
import androidx.ui.unit.IntPx
import androidx.ui.unit.Px
import androidx.ui.unit.dp
import androidx.ui.unit.ipx

/**
 * A simple layout composable that matches the size of it's parent layout.
 */
@Composable
internal fun MatchParent(children: @Composable() () -> Unit) {
    Layout(children) { _, constraints ->
        layout(constraints.maxWidth, constraints.maxHeight) {}
    }
}

@Composable
internal fun Center(children: @Composable() () -> Unit) {
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
private fun Padding(padding: Dp, children: @Composable() () -> Unit) {
    Padding(padding, padding, padding, padding, children)
}

@Composable
internal fun Padding(
    paddingLeft: Dp? = null,
    paddingTop: Dp? = null,
    paddingRight: Dp? = null,
    paddingBottom: Dp? = null,
    children: @Composable() () -> Unit
) {
    Layout(children) { measurables, constraints ->
        val paddingLeftIpx = paddingLeft?.toIntPx() ?: IntPx.Zero
        val paddingTopIpx = paddingTop?.toIntPx() ?: IntPx.Zero
        val paddingRightIpx = paddingRight?.toIntPx() ?: IntPx.Zero
        val paddingBottomIpx = paddingBottom?.toIntPx() ?: IntPx.Zero
        val maxWidth = constraints.maxWidth - paddingLeftIpx - paddingRightIpx
        val maxHeight = constraints.maxHeight - paddingTopIpx - paddingBottomIpx
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

        val placeable = if (measurables.isNotEmpty()) {
            measurables.first().measure(Constraints(minWidth, maxWidth, minHeight, maxHeight))
        } else {
            null
        }

        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable?.place(paddingLeftIpx, paddingTopIpx)
        }
    }
}

@Composable
internal fun Border(color: Color, width: Dp, children: @Composable() () -> Unit) {
    Layout(
        children = {
            children()
            Draw { canvas, parentSize ->

                val floatWidth = width.toPx().value

                val backgroundPaint = Paint().apply {
                    this.color = color
                    style = PaintingStyle.stroke
                    strokeWidth = floatWidth
                }
                canvas.drawRect(
                    androidx.ui.geometry.Rect(
                        floatWidth / 2,
                        floatWidth / 2,
                        parentSize.width.value - floatWidth / 2 + 1,
                        parentSize.height.value - floatWidth / 2 + 1
                    ),
                    backgroundPaint
                )
            }
        },
        measureBlock = { measurables, constraints ->
            val placeable =
                if (measurables.isNotEmpty()) measurables.first().measure(constraints) else null
            val layoutWidth = placeable?.width ?: constraints.maxWidth
            val layoutHeight = placeable?.height ?: constraints.maxHeight
            layout(layoutWidth, layoutHeight) {
                placeable?.place(0.ipx, 0.ipx)
            }
        })
}

@Composable
internal fun SimpleContainer(
    width: Dp,
    height: Dp,
    padding: Dp,
    children: @Composable() () -> Unit
) {

    val borderWidth: Dp = 2.dp
    val borderColor = BorderColor

    Layout({
        Padding(padding) {
            Border(
                color = borderColor,
                width = borderWidth,
                children = children
            )
        }
    }) { measurables, constraints ->
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
    }
}

@Composable
internal fun DrawBox(
    xOffset: Px,
    yOffset: Px,
    width: Dp,
    height: Dp,
    color: Color
) {
    val paint = remember { Paint() }
    Draw { canvas, parentSize ->
        paint.color = color
        val centerX = parentSize.width.value / 2 + xOffset.value
        val centerY = parentSize.height.value / 2 + yOffset.value
        val widthPx = width.toPx()
        val heightPx = height.toPx()
        val widthValue = if (widthPx.value < 0) parentSize.width.value else widthPx.value
        val heightValue = if (heightPx.value < 0) parentSize.height.value else heightPx.value
        canvas.drawRect(
            androidx.ui.geometry.Rect(
                centerX - widthValue / 2,
                centerY - heightValue / 2,
                centerX + widthValue / 2,
                centerY + heightValue / 2
            ),
            paint
        )
    }
}