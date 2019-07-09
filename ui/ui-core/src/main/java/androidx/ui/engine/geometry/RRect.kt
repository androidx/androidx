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

/**
 * An immutable rounded rectangle with custom radii for all four corners.
 */
data class RRect(
    /** The offset of the left edge of this rectangle from the x axis */
    val left: Float,
    /** The offset of the top edge of this rectangle from the y axis */
    val top: Float,
    /** The offset of the right edge of this rectangle from the x axis */
    val right: Float,
    /** The offset of the bottom edge of this rectangle from the y axis */
    val bottom: Float,
    /** The top-left horizontal radius */
    val topLeftRadiusX: Float,
    /** The top-left vertical radius */
    val topLeftRadiusY: Float,
    /** The top-right horizontal radius */
    val topRightRadiusX: Float,
    /** The top-right vertical radius */
    val topRightRadiusY: Float,
    /** The bottom-right horizontal radius */
    val bottomRightRadiusX: Float,
    /** The bottom-right vertical radius */
    val bottomRightRadiusY: Float,
    /** The bottom-left horizontal radius */
    val bottomLeftRadiusX: Float,
    /** The bottom-left vertical radius */
    val bottomLeftRadiusY: Float
) {
    /** The distance between the left and right edges of this rectangle. */
    val width = right - left

    /** The distance between the top and bottom edges of this rectangle. */
    val height = bottom - top

    /**
     * Same RRect with scaled radii per side. If you need this call [scaleRadii] instead.
     * Not @Volatile since the computed result will always be the same even if we race
     * and duplicate creation/computation in [scaleRadii].
     */
    private var _scaledRadiiRect: RRect? = null

    /**
     * Scales all radii so that on each side their sum will not pass the size of
     * the width/height.
     *
     * Inspired from: https://github.com/google/skia/blob/master/src/core/SkRRect.cpp#L164
     */
    private fun scaledRadiiRect(): RRect = _scaledRadiiRect ?: run {
        var scale = 1.0f
        scale = minRadius(scale, bottomLeftRadiusY, topLeftRadiusY, height)
        scale = minRadius(scale, topLeftRadiusX, topRightRadiusX, width)
        scale = minRadius(scale, topRightRadiusY, bottomRightRadiusY, height)
        scale = minRadius(scale, bottomRightRadiusX, bottomLeftRadiusX, width)

        RRect(
            left = left * scale,
            top = top * scale,
            right = right * scale,
            bottom = bottom * scale,
            topLeftRadiusX = topLeftRadiusX * scale,
            topLeftRadiusY = topLeftRadiusY * scale,
            topRightRadiusX = topRightRadiusX * scale,
            topRightRadiusY = topRightRadiusY * scale,
            bottomRightRadiusX = bottomRightRadiusX * scale,
            bottomRightRadiusY = bottomRightRadiusY * scale,
            bottomLeftRadiusX = bottomLeftRadiusX * scale,
            bottomLeftRadiusY = bottomLeftRadiusY * scale
        )
    }.also {
        // This might happen racey on different threads, we don't care, it'll be the same results.
        _scaledRadiiRect = it
    }

    /**
     * Returns the minimum between min and scale to which radius1 and radius2
     * should be scaled with in order not to exceed the limit.
     */
    private fun minRadius(min: Float, radius1: Float, radius2: Float, limit: Float): Float {
        val sum = radius1 + radius2
        return if (sum > limit && sum != 0.0f) {
            Math.min(min, limit / sum)
        } else {
            min
        }
    }

    /**
     * Whether the point specified by the given offset (which is assumed to be
     * relative to the origin) lies inside the rounded rectangle.
     *
     * This method may allocate (and cache) a copy of the object with normalized
     * radii the first time it is called on a particular [RRect] instance. When
     * using this method, prefer to reuse existing [RRect]s rather than
     * recreating the object each time.
     */
    fun contains(point: Offset): Boolean {
        if (point.dx < left || point.dx >= right || point.dy < top || point.dy >= bottom) {
            return false; // outside bounding box
        }

        val scaled = scaledRadiiRect()

        val x: Float
        val y: Float
        val radiusX: Float
        val radiusY: Float
        // check whether point is in one of the rounded corner areas
        // x, y -> translate to ellipse center
        if (point.dx < left + scaled.topLeftRadiusX &&
            point.dy < top + scaled.topLeftRadiusY
        ) {
            x = point.dx - left - scaled.topLeftRadiusX
            y = point.dy - top - scaled.topLeftRadiusY
            radiusX = scaled.topLeftRadiusX
            radiusY = scaled.topLeftRadiusY
        } else if (point.dx > right - scaled.topRightRadiusX &&
            point.dy < top + scaled.topRightRadiusY
        ) {
            x = point.dx - right + scaled.topRightRadiusX
            y = point.dy - top - scaled.topRightRadiusY
            radiusX = scaled.topRightRadiusX
            radiusY = scaled.topRightRadiusY
        } else if (point.dx > right - scaled.bottomRightRadiusX &&
            point.dy > bottom - scaled.bottomRightRadiusY
        ) {
            x = point.dx - right + scaled.bottomRightRadiusX
            y = point.dy - bottom + scaled.bottomRightRadiusY
            radiusX = scaled.bottomRightRadiusX
            radiusY = scaled.bottomRightRadiusY
        } else if (point.dx < left + scaled.bottomLeftRadiusX &&
            point.dy > bottom - scaled.bottomLeftRadiusY
        ) {
            x = point.dx - left - scaled.bottomLeftRadiusX
            y = point.dy - bottom + scaled.bottomLeftRadiusY
            radiusX = scaled.bottomLeftRadiusX
            radiusY = scaled.bottomLeftRadiusY
        } else {
            return true; // inside and not within the rounded corner area
        }

        val newX = x / radiusX
        val newY = y / radiusY

        // check if the point is inside the unit circle
        return newX * newX + newY * newY <= 1.0f
    }

    // Kept this with a deprecated annotation to facilitate porting other code that uses
    // the function's old name/location
    @Deprecated(
        "renamed to avoid conceptual naming collision with android inflate",
        replaceWith = ReplaceWith("grow(delta)", "androidx.ui.engine.geometry.grow"),
        level = DeprecationLevel.ERROR
    )
    fun inflate(delta: Float): RRect = grow(delta)

    // Kept this with a deprecated annotation to facilitate porting other code that uses
    // the function's old name/location
    @Deprecated(
        "renamed to avoid conceptual naming collision with android inflate",
        replaceWith = ReplaceWith("shrink(delta)", "androidx.ui.engine.geometry.shrink"),
        level = DeprecationLevel.ERROR
    )
    fun deflate(delta: Float): RRect = shrink(delta)

    override fun toString(): String {
        val tlRadius = topLeftRadius()
        val trRadius = topRightRadius()
        val brRadius = bottomRightRadius()
        val blRadius = bottomLeftRadius()
        val rect =
            "${left.toStringAsFixed(1)}, " +
                    "${top.toStringAsFixed(1)}, " +
                    "${right.toStringAsFixed(1)}, " +
                    bottom.toStringAsFixed(1)
        if (tlRadius == trRadius &&
            trRadius == brRadius &&
            brRadius == blRadius
        ) {
            if (tlRadius.x == tlRadius.y) {
                return "RRect(rect=$rect, radius=${tlRadius.x.toStringAsFixed(1)})"
            }
            return "RRect(rect=$rect, x=${tlRadius.x.toStringAsFixed(1)}, " +
                    "y=${tlRadius.y.toStringAsFixed(1)})"
        }
        return "RRect(" +
                "rect=$rect, " +
                "topLeft=$tlRadius, " +
                "topRight=$trRadius, " +
                "bottomRight=$brRadius, " +
                "bottomLeft=$blRadius)"
    }

    companion object {
        /** A rounded rectangle with all the values set to zero. */
        @JvmStatic
        val Zero = RRect(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
    }
}

/**
 * Construct a rounded rectangle from its left, top, right, and bottom edges,
 * and the same radii along its horizontal axis and its vertical axis.
 */
fun RRect(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    radiusX: Float,
    radiusY: Float
) = RRect(
    left = left,
    top = top,
    right = right,
    bottom = bottom,
    topLeftRadiusX = radiusX,
    topLeftRadiusY = radiusY,
    topRightRadiusX = radiusX,
    topRightRadiusY = radiusY,
    bottomRightRadiusX = radiusX,
    bottomRightRadiusY = radiusY,
    bottomLeftRadiusX = radiusX,
    bottomLeftRadiusY = radiusY
)

/**
 * Construct a rounded rectangle from its left, top, right, and bottom edges,
 * and the same radius in each corner.
 */
fun RRect(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    radius: Radius
) = RRect(
    left,
    top,
    right,
    bottom,
    radius.x,
    radius.y
)

/**
 * Construct a rounded rectangle from its bounding box and the same radii
 * along its horizontal axis and its vertical axis.
 */
fun RRect(
    rect: Rect,
    radiusX: Float,
    radiusY: Float
): RRect = RRect(
    left = rect.left,
    top = rect.top,
    right = rect.right,
    bottom = rect.bottom,
    radiusX = radiusX,
    radiusY = radiusY
)

/**
 * Construct a rounded rectangle from its bounding box and a radius that is
 * the same in each corner.
 */
fun RRect(
    rect: Rect,
    radius: Radius
): RRect = RRect(
    rect = rect,
    radiusX = radius.x,
    radiusY = radius.y
)

/**
 * Construct a rounded rectangle from its left, top, right, and bottom edges,
 * and topLeft, topRight, bottomRight, and bottomLeft radii.
 *
 * The corner radii default to [Radius.zero], i.e. right-angled corners.
 */
fun RRect(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    topLeft: Radius = Radius.zero,
    topRight: Radius = Radius.zero,
    bottomRight: Radius = Radius.zero,
    bottomLeft: Radius = Radius.zero
): RRect = RRect(
    left = left,
    top = top,
    right = right,
    bottom = bottom,
    topLeftRadiusX = topLeft.x,
    topLeftRadiusY = topLeft.y,
    topRightRadiusX = topRight.x,
    topRightRadiusY = topRight.y,
    bottomRightRadiusX = bottomRight.x,
    bottomRightRadiusY = bottomRight.y,
    bottomLeftRadiusX = bottomLeft.x,
    bottomLeftRadiusY = bottomLeft.y
)

/**
 * Construct a rounded rectangle from its bounding box and and topLeft,
 * topRight, bottomRight, and bottomLeft radii.
 *
 * The corner radii default to [Radius.zero], i.e. right-angled corners
 */
fun RRect(
    rect: Rect,
    topLeft: Radius = Radius.zero,
    topRight: Radius = Radius.zero,
    bottomRight: Radius = Radius.zero,
    bottomLeft: Radius = Radius.zero
): RRect = RRect(
    left = rect.left,
    top = rect.top,
    right = rect.right,
    bottom = rect.bottom,
    topLeftRadiusX = topLeft.x,
    topLeftRadiusY = topLeft.y,
    topRightRadiusX = topRight.x,
    topRightRadiusY = topRight.y,
    bottomRightRadiusX = bottomRight.x,
    bottomRightRadiusY = bottomRight.y,
    bottomLeftRadiusX = bottomLeft.x,
    bottomLeftRadiusY = bottomLeft.y
)

/** The top-left [Radius]. */
fun RRect.topLeftRadius(): Radius = Radius.elliptical(topLeftRadiusX, topLeftRadiusY)

/**  The top-right [Radius]. */
fun RRect.topRightRadius(): Radius = Radius.elliptical(topRightRadiusX, topRightRadiusY)

/**  The bottom-right [Radius]. */
fun RRect.bottomRightRadius(): Radius = Radius.elliptical(bottomRightRadiusX, bottomRightRadiusY)

/** The bottom-left [Radius]. */
fun RRect.bottomLeftRadius(): Radius = Radius.elliptical(bottomLeftRadiusX, bottomLeftRadiusY)

/** Returns a new [RRect] translated by the given offset. */
fun RRect.shift(offset: Offset): RRect = RRect(
    left = left + offset.dx,
    top = top + offset.dy,
    right = right + offset.dx,
    bottom = bottom + offset.dy,
    topLeft = Radius.elliptical(topLeftRadiusX, topLeftRadiusY),
    topRight = Radius.elliptical(topRightRadiusX, topRightRadiusY),
    bottomRight = Radius.elliptical(bottomRightRadiusX, bottomRightRadiusY),
    bottomLeft = Radius.elliptical(bottomLeftRadiusX, bottomLeftRadiusY)
)

/**
 * Returns a new [RRect] with edges and radii moved outwards by the given
 * delta.
 */
fun RRect.grow(delta: Float): RRect = RRect(
    left = left - delta,
    top = top - delta,
    right = right + delta,
    bottom = bottom + delta,
    topLeft = Radius.elliptical(topLeftRadiusX + delta, topLeftRadiusY + delta),
    topRight = Radius.elliptical(topRightRadiusX + delta, topRightRadiusY + delta),
    bottomRight = Radius.elliptical(bottomRightRadiusX + delta, bottomRightRadiusY + delta),
    bottomLeft = Radius.elliptical(bottomLeftRadiusX + delta, bottomLeftRadiusY + delta)
)

fun RRect.withRadius(radius: Radius): RRect = RRect(
    left = left,
    top = top,
    right = right,
    bottom = bottom,
    topLeft = radius,
    topRight = radius,
    bottomLeft = radius,
    bottomRight = radius
)

/** Returns a new [RRect] with edges and radii moved inwards by the given delta. */
fun RRect.shrink(delta: Float): RRect = grow(-delta)

/** The bounding box of this rounded rectangle (the rectangle with no rounded corners). */
fun RRect.outerRect(): Rect = Rect.fromLTRB(left, top, right, bottom)

/**
 * The non-rounded rectangle that is constrained by the smaller of the two
 * diagonals, with each diagonal traveling through the middle of the curve
 * corners. The middle of a corner is the intersection of the curve with its
 * respective quadrant bisector.
 */
fun RRect.safeInnerRect(): Rect {
    val insetFactor = 0.29289321881f; // 1-cos(pi/4)

    val leftRadius = Math.max(bottomLeftRadiusX, topLeftRadiusX)
    val topRadius = Math.max(topLeftRadiusY, topRightRadiusY)
    val rightRadius = Math.max(topRightRadiusX, bottomRightRadiusX)
    val bottomRadius = Math.max(bottomRightRadiusY, bottomLeftRadiusY)

    return Rect.fromLTRB(
        left + leftRadius * insetFactor,
        top + topRadius * insetFactor,
        right - rightRadius * insetFactor,
        bottom - bottomRadius * insetFactor
    )
}

/**
 * The rectangle that would be formed using the axis-aligned intersection of
 * the sides of the rectangle, i.e., the rectangle formed from the
 * inner-most centers of the ellipses that form the corners. This is the
 * intersection of the [wideMiddleRect] and the [tallMiddleRect]. If any of
 * the intersections are void, the resulting [Rect] will have negative width
 * or height.
 */
fun RRect.middleRect(): Rect {
    val leftRadius = Math.max(bottomLeftRadiusX, topLeftRadiusX)
    val topRadius = Math.max(topLeftRadiusY, topRightRadiusY)
    val rightRadius = Math.max(topRightRadiusX, bottomRightRadiusX)
    val bottomRadius = Math.max(bottomRightRadiusY, bottomLeftRadiusY)
    return Rect.fromLTRB(
        left + leftRadius,
        top + topRadius,
        right - rightRadius,
        bottom - bottomRadius
    )
}

/**
 * The biggest rectangle that is entirely inside the rounded rectangle and
 * has the full width of the rounded rectangle. If the rounded rectangle does
 * not have an axis-aligned intersection of its left and right side, the
 * resulting [Rect] will have negative width or height.
 */
fun RRect.wideMiddleRect(): Rect {
    val topRadius = Math.max(topLeftRadiusY, topRightRadiusY)
    val bottomRadius = Math.max(bottomRightRadiusY, bottomLeftRadiusY)
    return Rect.fromLTRB(
        left,
        top + topRadius,
        right,
        bottom - bottomRadius
    )
}

/**
 * The biggest rectangle that is entirely inside the rounded rectangle and
 * has the full height of the rounded rectangle. If the rounded rectangle
 * does not have an axis-aligned intersection of its top and bottom side, the
 * resulting [Rect] will have negative width or height.
 */
fun RRect.tallMiddleRect(): Rect {
    val leftRadius = Math.max(bottomLeftRadiusX, topLeftRadiusX)
    val rightRadius = Math.max(topRightRadiusX, bottomRightRadiusX)
    return Rect.fromLTRB(
        left + leftRadius,
        top,
        right - rightRadius,
        bottom
    )
}

/**
 * Whether this rounded rectangle encloses a non-zero area.
 * Negative areas are considered empty.
 */
val RRect.isEmpty get() = left >= right || top >= bottom

/** Whether all coordinates of this rounded rectangle are finite. */
val RRect.isFinite get() =
    left.isFinite() && top.isFinite() && right.isFinite() && bottom.isFinite()

/**
 * Whether this rounded rectangle is a simple rectangle with zero
 * corner radii.
 */
val RRect.isRect get(): Boolean = (topLeftRadiusX == 0.0f || topLeftRadiusY == 0.0f) &&
        (topRightRadiusX == 0.0f || topRightRadiusY == 0.0f) &&
        (bottomLeftRadiusX == 0.0f || bottomLeftRadiusY == 0.0f) &&
        (bottomRightRadiusX == 0.0f || bottomRightRadiusY == 0.0f)

/** Whether this rounded rectangle has a side with no straight section. */
val RRect.isStadium get(): Boolean =
    topLeftRadiusX == topRightRadiusX && topLeftRadiusY == topRightRadiusY &&
            topRightRadiusX == bottomRightRadiusX && topRightRadiusY == bottomRightRadiusY &&
            bottomRightRadiusX == bottomLeftRadiusX && bottomRightRadiusY == bottomLeftRadiusY &&
            (width <= 2.0 * topLeftRadiusX || height <= 2.0 * topLeftRadiusY)

/** Whether this rounded rectangle has no side with a straight section. */
val RRect.isEllipse get(): Boolean =
    topLeftRadiusX == topRightRadiusX && topLeftRadiusY == topRightRadiusY &&
            topRightRadiusX == bottomRightRadiusX && topRightRadiusY == bottomRightRadiusY &&
            bottomRightRadiusX == bottomLeftRadiusX && bottomRightRadiusY == bottomLeftRadiusY &&
            width <= 2.0 * topLeftRadiusX &&
            height <= 2.0 * topLeftRadiusY

/** Whether this rounded rectangle would draw as a circle. */
val RRect.isCircle get() = width == height && isEllipse

/**
 * The lesser of the magnitudes of the [width] and the [height] of this
 * rounded rectangle.
 */
val RRect.shortestSide get(): Float = Math.min(width.absoluteValue, height.absoluteValue)

/**
 * The greater of the magnitudes of the [width] and the [height] of this
 * rounded rectangle.
 */
val RRect.longestSide get(): Float = Math.max(width.absoluteValue, height.absoluteValue)

/**
 * The offset to the point halfway between the left and right and the top and
 * bottom edges of this rectangle.
 */
fun RRect.center(): Offset = Offset((left + width / 2.0f), (top + height / 2.0f))

/**
 * Linearly interpolate between two rounded rectangles.
 *
 * If either is null, this function substitutes [RRect.Zero] instead.
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
fun lerp(a: RRect?, b: RRect?, t: Float): RRect? = when {
    a == null && b == null -> null
    a == null -> {
        b!! // Force the smart cast below; if it were null it would have tripped the case above
        RRect(
            left = b.left * t,
            top = b.top * t,
            right = b.right * t,
            bottom = b.bottom * t,
            topLeftRadiusX = b.topLeftRadiusX * t,
            topLeftRadiusY = b.topLeftRadiusY * t,
            topRightRadiusX = b.topRightRadiusX * t,
            topRightRadiusY = b.topRightRadiusY * t,
            bottomRightRadiusX = b.bottomRightRadiusX * t,
            bottomRightRadiusY = b.bottomRightRadiusY * t,
            bottomLeftRadiusX = b.bottomLeftRadiusX * t,
            bottomLeftRadiusY = b.bottomLeftRadiusY * t
        )
    }
    b == null -> {
        val k = 1.0f - t
        RRect(
            left = a.left * k,
            top = a.top * k,
            right = a.right * k,
            bottom = a.bottom * k,
            topLeftRadiusX = a.topLeftRadiusX * k,
            topLeftRadiusY = a.topLeftRadiusY * k,
            topRightRadiusX = a.topRightRadiusX * k,
            topRightRadiusY = a.topRightRadiusY * k,
            bottomRightRadiusX = a.bottomRightRadiusX * k,
            bottomRightRadiusY = a.bottomRightRadiusY * k,
            bottomLeftRadiusX = a.bottomLeftRadiusX * k,
            bottomLeftRadiusY = a.bottomLeftRadiusY * k
        )
    }
    else -> RRect(
        left = lerp(a.left, b.left, t),
        top = lerp(a.top, b.top, t),
        right = lerp(a.right, b.right, t),
        bottom = lerp(a.bottom, b.bottom, t),
        topLeftRadiusX = lerp(a.topLeftRadiusX, b.topLeftRadiusX, t),
        topLeftRadiusY = lerp(a.topLeftRadiusY, b.topLeftRadiusY, t),
        topRightRadiusX = lerp(a.topRightRadiusX, b.topRightRadiusX, t),
        topRightRadiusY = lerp(a.topRightRadiusY, b.topRightRadiusY, t),
        bottomRightRadiusX = lerp(a.bottomRightRadiusX, b.bottomRightRadiusX, t),
        bottomRightRadiusY = lerp(a.bottomRightRadiusY, b.bottomRightRadiusY, t),
        bottomLeftRadiusX = lerp(a.bottomLeftRadiusX, b.bottomLeftRadiusX, t),
        bottomLeftRadiusY = lerp(a.bottomLeftRadiusY, b.bottomLeftRadiusY, t)
    )
}
