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

package androidx.ui.engine.geometry

import androidx.ui.lerp
import androidx.ui.toStringAsFixed
import kotlin.math.absoluteValue

// TODO(mount): Normalize this class. There are many methods that can be extension functions.
/**
 * An immutable, 2D, axis-aligned, floating-point rectangle whose coordinates
 * are relative to a given origin.
 *
 * A Rect can be created with one its constructors or from an [Offset] and a
 * [Size] using the `&` operator:
 *
 * ```dart
 * Rect myRect = const Offset(1.0, 2.0) & const Size(3.0, 4.0);
 * ```
 */
data class Rect(
        // The offset of the left edge of this rectangle from the x axis.
    val left: Float,
        // The offset of the top edge of this rectangle from the y axis.
    val top: Float,
        // The offset of the right edge of this rectangle from the x axis.
    val right: Float,
        // The offset of the bottom edge of this rectangle from the y axis.
    val bottom: Float
) {

    companion object {
        /** Construct a rectangle from its left, top, right, and bottom edges. */
        fun fromLTRB(left: Float, top: Float, right: Float, bottom: Float): Rect {
            return Rect(left, top, right, bottom)
        }

        /**
         * Construct a rectangle from its left and top edges, its width, and its
         * height.
         *
         * To construct a [Rect] from an [Offset] and a [Size], you can use the
         * rectangle constructor operator `&`. See [Offset.&].
         */
        fun fromLTWH(left: Float, top: Float, width: Float, height: Float): Rect {
            return Rect(left, top, left + width, top + height)
        }

        /**
         * Construct a rectangle that bounds the given circle.
         *
         * The `center` argument is assumed to be an offset from the origin.
         */
        fun fromCircle(center: Offset, radius: Float): Rect {
            return Rect(
                    center.dx - radius,
                    center.dy - radius,
                    center.dx + radius,
                    center.dy + radius
            )
        }

        /**
         * Construct the smallest rectangle that encloses the given offsets, treating
         * them as vectors from the origin.
         */
        fun fromPoints(a: Offset, b: Offset): Rect {
            return Rect(
                    Math.min(a.dx, b.dx),
                    Math.min(a.dy, b.dy),
                    Math.max(a.dx, b.dx),
                    Math.max(a.dy, b.dy)
            )
        }

        /** A rectangle with left, top, right, and bottom edges all at zero. */
        val zero: Rect = Rect(0.0f, 0.0f, 0.0f, 0.0f)

        val _giantScalar: Float = 1e7f // matches kGiantRect from default_layer_builder.cc

        /**
         * A rectangle that covers the entire coordinate space.
         *
         * This covers the space from -1e7,-1e7 to 1e7, 1e7.
         * This is the space over which graphics operations are valid.
         */
        val largest: Rect =
            fromLTRB(
                -_giantScalar,
                -_giantScalar,
                _giantScalar,
                _giantScalar
            )
    }

    /** The distance between the left and right edges of this rectangle. */
    val width = right - left

    /** The distance between the top and bottom edges of this rectangle. */
    val height = bottom - top

    // static const int _kDataSize = 4;
    // final Float32List _value = new Float32List(_kDataSize);
    // double get left => _value[0];
    // double get top => _value[1];
    //
    // double get right => _value[2];
    /** The offset of the bottom edge of this rectangle from the y axis. */
    // double get bottom => _value[3];

    /**
     * The distance between the upper-left corner and the lower-right corner of
     * this rectangle.
     */
    fun getSize() = Size(width, height)

    /** Whether any of the coordinates of this rectangle are equal to positive infinity. */
    // included for consistency with Offset and Size
    fun isInfinite(): Boolean {
        return left >= Float.POSITIVE_INFINITY ||
                top >= Float.POSITIVE_INFINITY ||
                right >= Float.POSITIVE_INFINITY ||
                bottom >= Float.POSITIVE_INFINITY
    }

    /** Whether all coordinates of this rectangle are finite. */
    fun isFinite(): Boolean =
            left.isFinite() &&
            top.isFinite() &&
            right.isFinite() &&
            bottom.isFinite()

    /**
     * Whether this rectangle encloses a non-zero area. Negative areas are
     * considered empty.
     */
    fun isEmpty(): Boolean = left >= right || top >= bottom

    /**
     * Returns a new rectangle translated by the given offset.
     *
     * To translate a rectangle by separate x and y components rather than by an
     * [Offset], consider [translate].
     */
    fun shift(offset: Offset): Rect {
        return fromLTRB(left + offset.dx, top + offset.dy, right + offset.dx, bottom + offset.dy)
    }

    /**
     * Returns a new rectangle with translateX added to the x components and
     * translateY added to the y components.
     *
     * To translate a rectangle by an [Offset] rather than by separate x and y
     * components, consider [shift].
     */
    fun translate(translateX: Float, translateY: Float): Rect {
        return fromLTRB(
                left + translateX,
                top + translateY,
                right + translateX,
                bottom + translateY
        )
    }

    /** Returns a new rectangle with edges moved outwards by the given delta. */
    fun inflate(delta: Float): Rect {
        return fromLTRB(left - delta, top - delta, right + delta, bottom + delta)
    }

    /** Returns a new rectangle with edges moved inwards by the given delta. */
    fun deflate(delta: Float): Rect = inflate(-delta)

    /**
     * Returns a new rectangle that is the intersection of the given
     * rectangle and this rectangle. The two rectangles must overlap
     * for this to be meaningful. If the two rectangles do not overlap,
     * then the resulting Rect will have a negative width or height.
     */
    fun intersect(other: Rect): Rect {
        return fromLTRB(
                Math.max(left, other.left),
                Math.max(top, other.top),
                Math.min(right, other.right),
                Math.min(bottom, other.bottom)
        )
    }

    /**
     * Returns a new rectangle which is the bounding box containing this
     * rectangle and the given rectangle.
     */
    fun expandToInclude(other: Rect): Rect {
        return fromLTRB(
                Math.min(left, other.left),
                Math.min(top, other.top),
                Math.max(right, other.right),
                Math.max(bottom, other.bottom)
        )
    }

    fun join(other: Rect): Rect {
        if (other.isEmpty()) {
            // return this if the other params are empty
            return this
        }
        if (isEmpty()) {
            // if we are empty, just take other
            return other
        }
        return expandToInclude(other)
    }

    /** Whether `other` has a nonzero area of overlap with this rectangle. */
    fun overlaps(other: Rect): Boolean {
        if (right <= other.left || other.right <= left)
            return false
        if (bottom <= other.top || other.bottom <= top)
            return false
        return true
    }

    /**
     * The lesser of the magnitudes of the [width] and the [height] of this
     * rectangle.
     */
    fun getShortestSide(): Float = Math.min(width.absoluteValue, height.absoluteValue)

    /**
     * The greater of the magnitudes of the [width] and the [height] of this
     * rectangle.
     */
    fun getLongestSide(): Float = Math.max(width.absoluteValue, height.absoluteValue)

    /**
     * The offset to the intersection of the top and left edges of this rectangle.
     *
     * See also [Size.topLeft].
     */
    fun getTopLeft(): Offset = Offset(left, top)

    /**
     * The offset to the center of the top edge of this rectangle.
     *
     * See also [Size.topCenter].
     */
    fun getTopCenter(): Offset = Offset(left + width / 2.0f, top)

    /**
     * The offset to the intersection of the top and right edges of this rectangle.
     *
     * See also [Size.topRight].
     */
    fun getTopRight(): Offset = Offset(right, top)

    /**
     * The offset to the center of the left edge of this rectangle.
     *
     * See also [Size.centerLeft].
     */
    fun getCenterLeft(): Offset = Offset(left, top + height / 2.0f)

    /**
     * The offset to the point halfway between the left and right and the top and
     * bottom edges of this rectangle.
     *
     * See also [Size.center].
     */
    fun getCenter(): Offset = Offset(left + width / 2.0f, top + height / 2.0f)

    /**
     * The offset to the center of the right edge of this rectangle.
     *
     * See also [Size.centerLeft].
     */
    fun getCenterRight(): Offset = Offset(right, top + height / 2.0f)

    /**
     * The offset to the intersection of the bottom and left edges of this rectangle.
     *
     * See also [Size.bottomLeft].
     */
    fun getBottomLeft(): Offset = Offset(left, bottom)

    /**
     * The offset to the center of the bottom edge of this rectangle.
     *
     * See also [Size.bottomLeft].
     */
    fun getBottomCenter(): Offset = Offset(left + width / 2.0f, bottom)

    /**
     * The offset to the intersection of the bottom and right edges of this rectangle.
     *
     * See also [Size.bottomRight].
     */
    fun getBottomRight(): Offset = Offset(right, bottom)

    /**
     * Whether the point specified by the given offset (which is assumed to be
     * relative to the origin) lies between the left and right and the top and
     * bottom edges of this rectangle.
     *
     * Rectangles include their top and left edges but exclude their bottom and
     * right edges.
     */
    fun contains(offset: Offset): Boolean {
        return offset.dx >= left && offset.dx < right && offset.dy >= top && offset.dy < bottom
    }

    override fun toString() = "Rect.fromLTRB(" +
            "${left.toStringAsFixed(1)}, " +
            "${top.toStringAsFixed(1)}, " +
            "${right.toStringAsFixed(1)}, " +
            "${bottom.toStringAsFixed(1)})"
}

/**
 * Linearly interpolate between two rectangles.
 *
 * The `t` argument represents position on the timeline, with 0.0 meaning
 * that the interpolation has not started, returning `a` (or something
 * equivalent to `a`), 1.0 meaning that the interpolation has finished,
 * returning `b` (or something equivalent to `b`), and values in between
 * meaning that the interpolation is at the relevant point on the timeline
 * between `a` and `b`. The interpolation can be extrapolated beyond 0.0 and
 * 1.0, so negative values and values greater than 1.0 are valid (and can
 * easily be generated by curves such as [Curves.elasticInOut]).
 *
 * Values for `t` are usually obtained from an [Animation<Float>], such as
 * an [AnimationController].
 */
fun lerp(a: Rect, b: Rect, t: Float): Rect {
    return Rect.fromLTRB(
        lerp(a.left, b.left, t),
        lerp(a.top, b.top, t),
        lerp(a.right, b.right, t),
        lerp(a.bottom, b.bottom, t)
    )
}
