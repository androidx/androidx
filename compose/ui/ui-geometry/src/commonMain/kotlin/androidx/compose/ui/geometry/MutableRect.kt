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

package androidx.compose.ui.geometry

import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

/**
 * An mutable, 2D, axis-aligned, floating-point rectangle whose coordinates are relative to a given
 * origin.
 *
 * @param left The offset of the left edge of this rectangle from the x axis.
 * @param top The offset of the top edge of this rectangle from the y axis.
 * @param right The offset of the right edge of this rectangle from the x axis.
 * @param bottom The offset of the bottom edge of this rectangle from the y axis.
 */
class MutableRect(var left: Float, var top: Float, var right: Float, var bottom: Float) {
    /** The distance between the left and right edges of this rectangle. */
    inline val width: Float
        get() = right - left

    /** The distance between the top and bottom edges of this rectangle. */
    inline val height: Float
        get() = bottom - top

    /** The distance between the upper-left corner and the lower-right corner of this rectangle. */
    val size: Size
        get() = Size(width, height)

    /** Whether any of the coordinates of this rectangle are equal to positive infinity. */
    // included for consistency with Offset and Size
    val isInfinite: Boolean
        get() =
            (left == Float.POSITIVE_INFINITY) or
                (top == Float.POSITIVE_INFINITY) or
                (right == Float.POSITIVE_INFINITY) or
                (bottom == Float.POSITIVE_INFINITY)

    /** Whether all coordinates of this rectangle are finite. */
    val isFinite: Boolean
        get() =
            ((left.toRawBits() and 0x7fffffff) < FloatInfinityBase) and
                ((top.toRawBits() and 0x7fffffff) < FloatInfinityBase) and
                ((right.toRawBits() and 0x7fffffff) < FloatInfinityBase) and
                ((bottom.toRawBits() and 0x7fffffff) < FloatInfinityBase)

    /** Whether this rectangle encloses a non-zero area. Negative areas are considered empty. */
    val isEmpty: Boolean
        get() = left >= right || top >= bottom

    /** Translates the rect by the provided [Offset]. */
    fun translate(offset: Offset) = translate(offset.x, offset.y)

    /**
     * Updates this rectangle with translateX added to the x components and translateY added to the
     * y components.
     */
    fun translate(translateX: Float, translateY: Float) {
        left += translateX
        top += translateY
        right += translateX
        bottom += translateY
    }

    /** Moves edges outwards by the given delta. */
    fun inflate(delta: Float) {
        left -= delta
        top -= delta
        right += delta
        bottom += delta
    }

    /** Moves edges inwards by the given delta. */
    fun deflate(delta: Float) = inflate(-delta)

    /**
     * Modifies `this` to be the intersection of this and the rect formed by [left], [top], [right],
     * and [bottom].
     */
    fun intersect(left: Float, top: Float, right: Float, bottom: Float) {
        this.left = max(left, this.left)
        this.top = max(top, this.top)
        this.right = min(right, this.right)
        this.bottom = min(bottom, this.bottom)
    }

    /** Whether `other` has a nonzero area of overlap with this rectangle. */
    fun overlaps(other: Rect): Boolean {
        return (left < other.right) and
            (other.left < right) and
            (top < other.bottom) and
            (other.top < bottom)
    }

    /** Whether `other` has a nonzero area of overlap with this rectangle. */
    fun overlaps(other: MutableRect): Boolean {
        if (right <= other.left || other.right <= left) return false
        if (bottom <= other.top || other.bottom <= top) return false
        return true
    }

    /** The lesser of the magnitudes of the [width] and the [height] of this rectangle. */
    val minDimension: Float
        get() = min(width.absoluteValue, height.absoluteValue)

    /** The greater of the magnitudes of the [width] and the [height] of this rectangle. */
    val maxDimension: Float
        get() = max(width.absoluteValue, height.absoluteValue)

    /** The offset to the intersection of the top and left edges of this rectangle. */
    val topLeft: Offset
        get() = Offset(left, top)

    /** The offset to the center of the top edge of this rectangle. */
    val topCenter: Offset
        get() = Offset(left + width / 2.0f, top)

    /** The offset to the intersection of the top and right edges of this rectangle. */
    val topRight: Offset
        get() = Offset(right, top)

    /** The offset to the center of the left edge of this rectangle. */
    val centerLeft: Offset
        get() = Offset(left, top + height / 2.0f)

    /**
     * The offset to the point halfway between the left and right and the top and bottom edges of
     * this rectangle.
     *
     * See also [Size.center].
     */
    val center: Offset
        get() = Offset(left + width / 2.0f, top + height / 2.0f)

    /** The offset to the center of the right edge of this rectangle. */
    val centerRight: Offset
        get() = Offset(right, top + height / 2.0f)

    /** The offset to the intersection of the bottom and left edges of this rectangle. */
    val bottomLeft: Offset
        get() = Offset(left, bottom)

    /** The offset to the center of the bottom edge of this rectangle. */
    val bottomCenter: Offset
        get() {
            return Offset(left + width / 2.0f, bottom)
        }

    /** The offset to the intersection of the bottom and right edges of this rectangle. */
    val bottomRight: Offset
        get() {
            return Offset(right, bottom)
        }

    /**
     * Whether the point specified by the given offset (which is assumed to be relative to the
     * origin) lies between the left and right and the top and bottom edges of this rectangle.
     *
     * Rectangles include their top and left edges but exclude their bottom and right edges.
     */
    operator fun contains(offset: Offset): Boolean {
        val x = offset.x
        val y = offset.y
        return (x >= left) and (x < right) and (y >= top) and (y < bottom)
    }

    /** Sets new bounds to ([left], [top], [right], [bottom]) */
    fun set(left: Float, top: Float, right: Float, bottom: Float) {
        this.left = left
        this.top = top
        this.right = right
        this.bottom = bottom
    }

    override fun toString() =
        "MutableRect(" +
            "${left.toStringAsFixed(1)}, " +
            "${top.toStringAsFixed(1)}, " +
            "${right.toStringAsFixed(1)}, " +
            "${bottom.toStringAsFixed(1)})"
}

fun MutableRect.toRect(): Rect = Rect(left, top, right, bottom)

/**
 * Construct a rectangle from its left and top edges as well as its width and height.
 *
 * @param offset Offset to represent the top and left parameters of the Rect
 * @param size Size to determine the width and height of this [Rect].
 * @return Rect with [Rect.left] and [Rect.top] configured to [Offset.x] and [Offset.y] as
 *   [Rect.right] and [Rect.bottom] to [Offset.x] + [Size.width] and [Offset.y] + [Size.height]
 *   respectively
 */
fun MutableRect(offset: Offset, size: Size): MutableRect =
    MutableRect(offset.x, offset.y, offset.x + size.width, offset.y + size.height)

/**
 * Construct the smallest rectangle that encloses the given offsets, treating them as vectors from
 * the origin.
 *
 * @param topLeft Offset representing the left and top edges of the rectangle
 * @param bottomRight Offset representing the bottom and right edges of the rectangle
 */
fun MutableRect(topLeft: Offset, bottomRight: Offset): MutableRect =
    MutableRect(topLeft.x, topLeft.y, bottomRight.x, bottomRight.y)

/**
 * Construct a rectangle that bounds the given circle
 *
 * @param center Offset that represents the center of the circle
 * @param radius Radius of the circle to enclose
 */
fun MutableRect(center: Offset, radius: Float): MutableRect =
    MutableRect(center.x - radius, center.y - radius, center.x + radius, center.y + radius)
