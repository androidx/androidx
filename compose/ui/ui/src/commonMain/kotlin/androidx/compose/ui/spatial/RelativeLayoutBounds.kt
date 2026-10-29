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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.requireLayoutNode
import androidx.compose.ui.node.requireOwner
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.util.fastMaxOf
import androidx.compose.ui.util.fastMinOf
import androidx.compose.ui.util.fastRoundToInt
import kotlin.math.max
import kotlin.math.min

/**
 * Represents an axis-aligned bounding Rectangle for an element in a compose hierarchy, in the
 * coordinates of either the Root of the compose hierarchy, the Window, or the Screen.
 *
 * @see androidx.compose.ui.layout.onLayoutRectChanged
 */
public class RelativeLayoutBounds
internal constructor(
    private val topLeft: Long,
    private val bottomRight: Long,
    private val windowOffset: IntOffset,
    private val screenOffset: IntOffset,
    private val windowSize: Long,
    private val viewToWindowMatrix: Matrix?,
    private val node: DelegatableNode,
) {
    /**
     * The top left position of the Rect in the coordinates of the root node of the compose
     * hierarchy.
     */
    public val positionInRoot: IntOffset
        get() = IntOffset(topLeft)

    /** The top left position of the Rect in the coordinates of the Window it is contained in */
    public val positionInWindow: IntOffset
        get() {
            val x = screenOffset.x - windowOffset.x
            val y = screenOffset.y - windowOffset.y
            val l = unpackX(topLeft)
            val t = unpackY(topLeft)
            return IntOffset(l + x, t + y)
        }

    /** The top left position of the Rect in the coordinates of the Screen it is contained in. */
    public val positionInScreen: IntOffset
        get() {
            val x = screenOffset.x
            val y = screenOffset.y
            val l = unpackX(topLeft)
            val t = unpackY(topLeft)
            return IntOffset(l + x, t + y)
        }

    /** The width, in pixels, of the Rect */
    public val width: Int
        get() {
            val l = unpackX(topLeft)
            val r = unpackX(bottomRight)
            return r - l
        }

    /** The height, in pixels, of the Rect */
    public val height: Int
        get() {
            val t = unpackY(topLeft)
            val b = unpackY(bottomRight)
            return b - t
        }

    /**
     * The positioned bounding Rect in the coordinates of the root node of the compose hierarchy.
     */
    public val boundsInRoot: IntRect
        get() {
            val l = unpackX(topLeft)
            val t = unpackY(topLeft)
            val r = unpackX(bottomRight)
            val b = unpackY(bottomRight)
            return IntRect(l, t, r, b)
        }

    /** The positioned bounding Rect in the coordinates of the Window which it is contained in. */
    public val boundsInWindow: IntRect
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
    public val boundsInScreen: IntRect
        get() {
            if (viewToWindowMatrix != null) {
                val windowRect = boundsInWindow
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

    /**
     * At the current state of the layout, calculates which other Composable Layouts are occluding
     * the Composable associated with this [RelativeLayoutBounds]. **Note**: Calling this method
     * during measure or layout may result on calculations with stale (or partially stale) layout
     * information.
     *
     * An occlusion is defined by an intersecting Composable that may draw on top of the target
     * Composable.
     *
     * There's no guarantee that something was actually drawn to occlude, so a transparent
     * Composable that could otherwise draw on top of the target is considered to be occluding.
     *
     * There's no differentiation between partial and complete occlusions, they are all included as
     * part of this calculation.
     *
     * Ancestors, child and grandchild Layouts are never considered to be occluding.
     *
     * @return A [List] of the rectangles that occlude the associated Composable Layout.
     */
    public fun calculateOcclusions(): List<IntRect> {
        val targetNode = node.requireLayoutNode()
        if (targetNode.rectListIndex == NotFound) {
            return emptyList()
        }

        val rectManager = node.requireOwner().rectManager
        val rectList = rectManager.rects

        val id = targetNode.semanticsId
        val idIndex = rectList.indexOf(targetNode.semanticsId, targetNode.rectListIndex)
        // For the given `id`, finds intersections and determines occlusions by the result of
        // the 'RectManager.isTargetDrawnFirst', if the node for 'id' is drawn first, then it's
        // being occluded and the intersecting rect is added to the list result.
        return buildList {
            rectList.forEachIntersectingRectWithValueAt(idIndex) { l, t, r, b, intersectingId ->
                if (rectManager.isTargetDrawnFirst(id, intersectingId)) {
                    this@buildList.add(IntRect(l, t, r, b))
                }
            }
        }
    }

    /**
     * Used to determine the fraction of pixels that are inside the bounds of the [viewport].
     *
     * If the [viewport] is entirely contained inside of the bounds, this method will return 1f.
     * Otherwise, this will return the fraction of pixels of this element that are contained inside
     * the [viewport] bounds.
     *
     * @see fractionVisibleInWindow
     * @see fractionVisibleInRect
     * @see fractionVisibleInWindowWithInsets
     */
    public fun fractionVisibleIn(viewport: RelativeLayoutBounds): Float {
        val tl = viewport.topLeft
        val br = viewport.bottomRight
        return fractionVisibleInRect(
            left = unpackX(tl),
            top = unpackY(tl),
            right = unpackX(br),
            bottom = unpackY(br),
        )
    }

    /**
     * Used to determine the fraction of pixels that are inside the provided rectangle.
     *
     * If the rectangle is entirely contained inside of the bounds, this method will return 1f.
     * Otherwise, this will return the fraction of pixels of this element that are contained inside
     * the rectangle.
     *
     * @see fractionVisibleInWindow
     * @see fractionVisibleIn
     * @see fractionVisibleInWindowWithInsets
     */
    public fun fractionVisibleInRect(left: Int, top: Int, right: Int, bottom: Int): Float =
        fractionOfElementInside(
            elementLeft = unpackX(topLeft),
            elementTop = unpackY(topLeft),
            elementRight = unpackX(bottomRight),
            elementBottom = unpackY(bottomRight),
            left = left,
            top = top,
            right = right,
            bottom = bottom,
        )

    /**
     * Used to determine the fraction of pixels that are inside the bounds of the window.
     *
     * If the window is entirely contained inside of the bounds, this method will return 1f.
     * Otherwise, this will return the fraction of pixels of this element that are contained inside
     * the rectangle.
     *
     * @see fractionVisibleInWindow
     * @see fractionVisibleIn
     * @see fractionVisibleInWindowWithInsets
     */
    public fun fractionVisibleInWindow(): Float {
        val windowSize = windowSize
        return fractionVisibleInWindowRect(0, 0, unpackX(windowSize), unpackY(windowSize))
    }

    /**
     * Used to determine the fraction of pixels that are inside the rectangle defined by the window
     * with the provided insets subtracted.
     *
     * If the rectangle is entirely contained inside of the bounds, this method will return 1f.
     * Otherwise, this will return the fraction of pixels of this element that are contained inside
     * the rectangle.
     *
     * @see fractionVisibleInWindow
     * @see fractionVisibleIn
     * @see fractionVisibleInWindowWithInsets
     */
    public fun fractionVisibleInWindowWithInsets(
        topLeftInset: IntOffset,
        bottomRightInset: IntOffset,
    ): Float {
        val windowSize = windowSize
        return fractionVisibleInWindowRect(
            topLeftInset.x,
            topLeftInset.y,
            unpackX(windowSize) - bottomRightInset.x,
            unpackY(windowSize) - bottomRightInset.y,
        )
    }

    /**
     * The fraction of pixels of this element inside the rectangle of the window from ([left],
     * [top]) to ([right], [bottom]).
     *
     * This element's bounds are relative to the root of its compose hierarchy, which isn't always
     * at the window's origin, for example in an item of a RecyclerView:
     * ```
     * +--------- window ---------+
     * |                          |
     * |  (x, y)                  |
     * |    +------ root ------+  |
     * |    |  +- element -+   |  |
     * |    |  |           |   |  |
     * +----|--|-----------|---|--+
     *      |  +-----------+   |
     *      +------------------+
     * ```
     *
     * So the element's bounds are moved by (x, y) into the window's coordinates, where they can be
     * compared with the rectangle.
     */
    private fun fractionVisibleInWindowRect(left: Int, top: Int, right: Int, bottom: Int): Float {
        val viewToWindowMatrix = viewToWindowMatrix
        if (viewToWindowMatrix == null) {
            val x = screenOffset.x - windowOffset.x
            val y = screenOffset.y - windowOffset.y
            return fractionOfElementInside(
                elementLeft = unpackX(topLeft) + x,
                elementTop = unpackY(topLeft) + y,
                elementRight = unpackX(bottomRight) + x,
                elementBottom = unpackY(bottomRight) + y,
                left = left,
                top = top,
                right = right,
                bottom = bottom,
            )
        }
        // The root is also scaled or rotated in the window: the element is bounded there by its
        // four corners.
        val l = unpackX(topLeft).toFloat()
        val t = unpackY(topLeft).toFloat()
        val r = unpackX(bottomRight).toFloat()
        val b = unpackY(bottomRight).toFloat()
        val topLeftInWindow = viewToWindowMatrix.map(Offset(l, t))
        val topRightInWindow = viewToWindowMatrix.map(Offset(r, t))
        val bottomLeftInWindow = viewToWindowMatrix.map(Offset(l, b))
        val bottomRightInWindow = viewToWindowMatrix.map(Offset(r, b))
        val minX =
            fastMinOf(
                topLeftInWindow.x,
                topRightInWindow.x,
                bottomLeftInWindow.x,
                bottomRightInWindow.x,
            )
        val minY =
            fastMinOf(
                topLeftInWindow.y,
                topRightInWindow.y,
                bottomLeftInWindow.y,
                bottomRightInWindow.y,
            )
        val maxX =
            fastMaxOf(
                topLeftInWindow.x,
                topRightInWindow.x,
                bottomLeftInWindow.x,
                bottomRightInWindow.x,
            )
        val maxY =
            fastMaxOf(
                topLeftInWindow.y,
                topRightInWindow.y,
                bottomLeftInWindow.y,
                bottomRightInWindow.y,
            )
        return fractionOfElementInside(
            elementLeft = minX.fastRoundToInt(),
            elementTop = minY.fastRoundToInt(),
            elementRight = maxX.fastRoundToInt(),
            elementBottom = maxY.fastRoundToInt(),
            left = left,
            top = top,
            right = right,
            bottom = bottom,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as RelativeLayoutBounds

        if (topLeft != other.topLeft) return false
        if (bottomRight != other.bottomRight) return false
        if (windowSize != other.windowSize) return false
        if (windowOffset != other.windowOffset) return false
        if (screenOffset != other.screenOffset) return false
        if (viewToWindowMatrix != other.viewToWindowMatrix) return false
        if (node != other.node) return false

        return true
    }

    override fun hashCode(): Int {
        var result = topLeft.hashCode()
        result = 31 * result + bottomRight.hashCode()
        result = 31 * result + windowSize.hashCode()
        result = 31 * result + windowOffset.hashCode()
        result = 31 * result + screenOffset.hashCode()
        result = 31 * result + (viewToWindowMatrix?.hashCode() ?: 0)
        result = 31 * result + node.hashCode()
        return result
    }
}

/**
 * The fraction of the pixels of an element, from ([elementLeft], [elementTop]) to ([elementRight],
 * [elementBottom]), that are inside the rectangle from ([left], [top]) to ([right], [bottom]). It
 * is 1f when the rectangle is entirely inside the element.
 */
private fun fractionOfElementInside(
    elementLeft: Int,
    elementTop: Int,
    elementRight: Int,
    elementBottom: Int,
    left: Int,
    top: Int,
    right: Int,
    bottom: Int,
): Float {
    val clippedWidth = min(elementRight, right) - max(elementLeft, left)
    val clippedHeight = min(elementBottom, bottom) - max(elementTop, top)
    if (clippedWidth <= 0 || clippedHeight <= 0) return 0f

    val clippedArea = clippedWidth * clippedHeight
    val rectangleArea = (right - left) * (bottom - top)
    val elementArea = (elementRight - elementLeft) * (elementBottom - elementTop)
    val maxArea = min(rectangleArea, elementArea)

    // One rectangle contains the other.
    if (clippedArea == maxArea) return 1f

    return clippedArea.toFloat() / maxArea.toFloat()
}
