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

package androidx.ui.core.selection

import androidx.compose.Composable
import androidx.compose.remember
import androidx.ui.core.Constraints
import androidx.ui.core.Draw
import androidx.ui.core.Layout
import androidx.ui.core.withTight
import androidx.ui.geometry.Rect
import androidx.ui.graphics.Color
import androidx.ui.graphics.Paint
import androidx.ui.graphics.Path
import androidx.ui.text.style.TextDirection
import androidx.ui.unit.Dp
import androidx.ui.unit.dp
import androidx.ui.unit.ipx

internal val HANDLE_WIDTH = 25.dp
internal val HANDLE_HEIGHT = 25.dp
private val HANDLE_COLOR = Color(0xFF2B28F5.toInt())

@Composable
private fun SelectionHandle(left: Boolean) {
    val paint = remember { Paint().also { it.isAntiAlias = true } }
    paint.color = HANDLE_COLOR
    FixedDimension(width = HANDLE_WIDTH, height = HANDLE_HEIGHT) {
        Draw { canvas, _ ->
            val path = Path().apply {
                addRect(
                    Rect(
                        top = 0f,
                        bottom = 0.5f * HANDLE_HEIGHT.toPx().value,
                        left = if (left) {
                            0.5f * HANDLE_WIDTH.toPx().value
                        } else {
                            0f
                        },
                        right = if (left) {
                            HANDLE_WIDTH.toPx().value
                        } else {
                            0.5f * HANDLE_WIDTH.toPx().value
                        }
                    )
                )

                addOval(
                    Rect(
                        top = 0f,
                        bottom = HANDLE_HEIGHT.toPx().value,
                        left = 0f,
                        right = HANDLE_WIDTH.toPx().value
                    )
                )
            }

            canvas.drawPath(path, paint)
        }
    }
}

@Composable
private fun FixedDimension(
    width: Dp,
    height: Dp,
    children: @Composable() () -> Unit
) {
    Layout(children) { measurables, _ ->
        val constraints = Constraints().withTight(width.toIntPx(), height.toIntPx())
        val placeables = measurables.map { measurable ->
            measurable.measure(constraints)
        }
        layout(width.toIntPx(), height.toIntPx()) {
            placeables.forEach { placeable ->
                placeable.place(0.ipx, 0.ipx)
            }
        }
    }
}

@Composable
internal fun StartSelectionHandle(selection: Selection?) {
    selection?.let {
        if (isHandleLtrDirection(it.start.direction, it.handlesCrossed)) {
            SelectionHandle(left = true)
        } else {
            SelectionHandle(left = false)
        }
    }
}

@Composable
internal fun EndSelectionHandle(selection: Selection?) {
    selection?.let {
        if (isHandleLtrDirection(it.end.direction, it.handlesCrossed)) {
            SelectionHandle(left = false)
        } else {
            SelectionHandle(left = true)
        }
    }
}

/**
 * This method is to check if the selection handles should use the natural Ltr pointing
 * direction.
 * If the context is Ltr and the handles are not crossed, or if the context is Rtl and the handles
 * are crossed, return true.
 *
 * In Ltr context, the start handle should point to the left, and the end handle should point to
 * the right. However, in Rtl context or when handles are crossed, the start handle should point to
 * the right, and the end handle should point to left.
 */
internal fun isHandleLtrDirection(direction: TextDirection, areHandlesCrossed: Boolean): Boolean {
    return direction == TextDirection.Ltr && !areHandlesCrossed ||
            direction == TextDirection.Rtl && areHandlesCrossed
}
