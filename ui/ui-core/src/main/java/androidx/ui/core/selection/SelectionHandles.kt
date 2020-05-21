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
import androidx.compose.emptyContent
import androidx.compose.remember
import androidx.ui.core.Layout
import androidx.ui.core.Modifier
import androidx.ui.core.drawBehind
import androidx.ui.geometry.Rect
import androidx.ui.graphics.Color
import androidx.ui.graphics.Path
import androidx.ui.graphics.drawscope.DrawScope
import androidx.ui.text.style.TextDirection
import androidx.ui.unit.Density
import androidx.ui.unit.Dp
import androidx.ui.unit.dp

internal val HANDLE_WIDTH = 25.dp
internal val HANDLE_HEIGHT = 25.dp
private val HANDLE_COLOR = Color(0xFF2B28F5.toInt())

@Composable
private fun SelectionHandle(left: Boolean) {
    val selectionHandleCache = remember { SelectionHandleCache() }
    HandleDrawLayout(width = HANDLE_WIDTH, height = HANDLE_HEIGHT) {
        drawPath(selectionHandleCache.createPath(this, left), HANDLE_COLOR)
    }
}

/**
 * Class used to cache a Path object to represent a selection handle
 * based on the given handle direction
 */
private class SelectionHandleCache {
    private var path: Path? = null
    private var left: Boolean = false

    fun createPath(density: Density, left: Boolean): Path {
        return with(density) {
            val current = path
            if (this@SelectionHandleCache.left == left && current != null) {
                // If we have already created the Path for the correct handle direction
                // return it
                current
            } else {
                this@SelectionHandleCache.left = left
                // Otherwise, if this is the first time we are creating the Path
                // or the current handle direction is different than the one we
                // previously created, recreate the path and cache the result
                (current ?: Path().also { path = it }).apply {
                    reset()
                    addRect(
                        Rect(
                            top = 0f,
                            bottom = 0.5f * HANDLE_HEIGHT.toPx(),
                            left = if (left) {
                                0.5f * HANDLE_WIDTH.toPx()
                            } else {
                                0f
                            },
                            right = if (left) {
                                HANDLE_WIDTH.toPx()
                            } else {
                                0.5f * HANDLE_WIDTH.toPx()
                            }
                        )
                    )
                    addOval(
                        Rect(
                            top = 0f,
                            bottom = HANDLE_HEIGHT.toPx(),
                            left = 0f,
                            right = HANDLE_WIDTH.toPx()
                        )
                    )
                }
            }
        }
    }
}

/**
 * Simple container to perform drawing of selection handles. This layout takes size on the screen
 * according to [width] and [height] params and performs drawing in this space as specified in
 * [onCanvas]
 */
@Composable
private fun HandleDrawLayout(
    width: Dp,
    height: Dp,
    onCanvas: DrawScope.() -> Unit
) {
    Layout(emptyContent(), Modifier.drawBehind(onCanvas)) { _, _, _ ->
        // take width and height space of the screen and allow draw modifier to draw inside of it
        layout(width.toIntPx(), height.toIntPx()) {
            // this layout has no children, only draw modifier.
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
