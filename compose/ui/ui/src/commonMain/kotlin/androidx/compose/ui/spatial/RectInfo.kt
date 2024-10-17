/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.spatial

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.roundToIntRect

/**
 * Represents an axis-aligned bounding Rectangle for an element in a compose hierarchy, in the
 * coordinates of either the Root of the compose hierarchy, the Window, or the Screen.
 *
 * @see androidx.compose.ui.layout.onRectChanged
 */
class RectInfo
internal constructor(
    private val topLeft: Long,
    private val bottomRight: Long,
    private val windowOffset: IntOffset,
    private val screenOffset: IntOffset,
    private val viewToWindowMatrix: Matrix?,
) {
    /**
     * The top left position of the Rect in the coordinates of the root node of the compose
     * hierarchy.
     */
    val positionInRoot: IntOffset
        get() = IntOffset(topLeft)

    /** The top left position of the Rect in the coordinates of the Window it is contained in */
    val positionInWindow: IntOffset
        get() {
            val x = screenOffset.x - windowOffset.x
            val y = screenOffset.y - windowOffset.y
            val l = unpackX(topLeft)
            val t = unpackY(topLeft)
            return IntOffset(l + x, t + y)
        }

    /** The top left position of the Rect in the coordinates of the Screen it is contained in. */
    val positionInScreen: IntOffset
        get() {
            val x = screenOffset.x
            val y = screenOffset.y
            val l = unpackX(topLeft)
            val t = unpackY(topLeft)
            return IntOffset(l + x, t + y)
        }

    /** The width, in pixels, of the Rect */
    val width: Int
        get() {
            val l = unpackX(topLeft)
            val r = unpackX(bottomRight)
            return r - l
        }

    /** The height, in pixels, of the Rect */
    val height: Int
        get() {
            val t = unpackY(topLeft)
            val b = unpackY(bottomRight)
            return b - t
        }

    /**
     * The positioned bounding Rect in the coordinates of the root node of the compose hierarchy.
     */
    val rootRect: IntRect
        get() {
            val l = unpackX(topLeft)
            val t = unpackY(topLeft)
            val r = unpackX(bottomRight)
            val b = unpackY(bottomRight)
            return IntRect(l, t, r, b)
        }

    /** The positioned bounding Rect in the coordinates of the Window which it is contained in. */
    val windowRect: IntRect
        get() {
            val l = unpackX(topLeft)
            val t = unpackY(topLeft)
            val r = unpackX(bottomRight)
            val b = unpackY(bottomRight)
            if (viewToWindowMatrix != null) {
                // TODO: we could implement a `Matrix.map(l, t, r, b): IntRect` that was only a
                //  single allocation if we wanted to. this would avoid the two Rect(FFFF)
                //  allocations that we have here.
                return viewToWindowMatrix
                    .map(Rect(l.toFloat(), t.toFloat(), r.toFloat(), b.toFloat()))
                    .roundToIntRect()
            }
            val x = screenOffset.x - windowOffset.x
            val y = screenOffset.y - windowOffset.y
            return IntRect(l + x, t + y, r + x, b + y)
        }

    /** The positioned bounding Rect in the coordinates of the Screen which it is contained in. */
    val screenRect: IntRect
        get() {
            if (viewToWindowMatrix != null) {
                val windowRect = windowRect
                val offset = windowOffset
                return IntRect(
                    windowRect.left + offset.x,
                    windowRect.top + offset.y,
                    windowRect.right + offset.x,
                    windowRect.bottom + offset.y,
                )
            }
            val l = unpackX(topLeft)
            val t = unpackY(topLeft)
            val r = unpackX(bottomRight)
            val b = unpackY(bottomRight)
            val x = screenOffset.x
            val y = screenOffset.y
            return IntRect(l + x, t + y, r + x, b + y)
        }
}
