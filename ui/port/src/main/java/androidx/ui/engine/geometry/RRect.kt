package androidx.ui.engine.geometry

import androidx.ui.lerpDouble
import androidx.ui.toStringAsFixed
import kotlin.math.absoluteValue

// / An immutable rounded rectangle with the custom radii for all four corners.
data class RRect(
        // / The offset of the left edge of this rectangle from the x axis.
    val left: Double,
        // / The offset of the top edge of this rectangle from the y axis.
    val top: Double,
        // / The offset of the right edge of this rectangle from the x axis.
    val right: Double,
        // / The offset of the bottom edge of this rectangle from the y axis.
    val bottom: Double,
        // / The top-left horizontal radius.
    val tlRadiusX: Double,
        // / The top-left vertical radius.
    val tlRadiusY: Double,
        // / The top-right horizontal radius.
    val trRadiusX: Double,
        // / The top-right vertical radius.
    val trRadiusY: Double,
        // / The bottom-right horizontal radius.
    val brRadiusX: Double,
        // / The bottom-right vertical radius.
    val brRadiusY: Double,
        // / The bottom-left horizontal radius.
    val blRadiusX: Double,
        // / The bottom-left vertical radius.
    val blRadiusY: Double
) {

    // / The top-left [Radius].
    val tlRadius by lazy { Radius.elliptical(tlRadiusX, tlRadiusY) }

    // / The top-right [Radius].
    val trRadius by lazy { Radius.elliptical(trRadiusX, trRadiusY) }

    // / The bottom-right [Radius].
    val brRadius by lazy { Radius.elliptical(brRadiusX, brRadiusY) }

    // / The bottom-left [Radius].
    val blRadius by lazy { Radius.elliptical(blRadiusX, blRadiusY) }

    companion object {
        // / Construct a rounded rectangle from its left, top, right, and bottom edges,
        // / and the same radii along its horizontal axis and its vertical axis.
        fun fromLTRBXY(
            left: Double,
            top: Double,
            right: Double,
            bottom: Double,
            radiusX: Double,
            radiusY: Double
        ): RRect {
            return RRect(
                    left = left,
                    top = top,
                    right = right,
                    bottom = bottom,
                    tlRadiusX = radiusX,
                    tlRadiusY = radiusY,
                    trRadiusX = radiusX,
                    trRadiusY = radiusY,
                    brRadiusX = radiusX,
                    brRadiusY = radiusY,
                    blRadiusX = radiusX,
                    blRadiusY = radiusY
            )
        }

        // / Construct a rounded rectangle from its left, top, right, and bottom edges,
        // / and the same radius in each corner.
        fun fromLTRBR(
            left: Double,
            top: Double,
            right: Double,
            bottom: Double,
            radius: Radius
        ): RRect {
            return RRect(
                    left = left,
                    top = top,
                    right = right,
                    bottom = bottom,
                    tlRadiusX = radius.x,
                    tlRadiusY = radius.y,
                    trRadiusX = radius.x,
                    trRadiusY = radius.y,
                    brRadiusX = radius.x,
                    brRadiusY = radius.y,
                    blRadiusX = radius.x,
                    blRadiusY = radius.y
            )
        }

        // / Construct a rounded rectangle from its bounding box and the same radii
        // / along its horizontal axis and its vertical axis.
        fun fromRectXY(
            rect: Rect,
            radiusX: Double,
            radiusY: Double
        ): RRect {
            return RRect(
                    left = rect.left,
                    top = rect.top,
                    right = rect.right,
                    bottom = rect.bottom,
                    tlRadiusX = radiusX,
                    tlRadiusY = radiusY,
                    trRadiusX = radiusX,
                    trRadiusY = radiusY,
                    brRadiusX = radiusX,
                    brRadiusY = radiusY,
                    blRadiusX = radiusX,
                    blRadiusY = radiusY
            )
        }

        // / Construct a rounded rectangle from its bounding box and a radius that is
        // / the same in each corner.
        fun fromRectAndRadius(
            rect: Rect,
            radius: Radius
        ): RRect {
            return RRect(
                    left = rect.left,
                    top = rect.top,
                    right = rect.right,
                    bottom = rect.bottom,
                    tlRadiusX = radius.x,
                    tlRadiusY = radius.y,
                    trRadiusX = radius.x,
                    trRadiusY = radius.y,
                    brRadiusX = radius.x,
                    brRadiusY = radius.y,
                    blRadiusX = radius.x,
                    blRadiusY = radius.y
            )
        }

        // / Construct a rounded rectangle from its left, top, right, and bottom edges,
        // / and topLeft, topRight, bottomRight, and bottomLeft radii.
        // /
        // / The corner radii default to [Radius.zero], i.e. right-angled corners.
        fun fromLTRBAndCorners(
            left: Double,
            top: Double,
            right: Double,
            bottom: Double,
            topLeft: Radius = Radius.zero,
            topRight: Radius = Radius.zero,
            bottomRight: Radius = Radius.zero,
            bottomLeft: Radius = Radius.zero
        ): RRect {
            return RRect(
                    left = left,
                    top = top,
                    right = right,
                    bottom = bottom,
                    tlRadiusX = topLeft.x,
                    tlRadiusY = topLeft.y,
                    trRadiusX = topRight.x,
                    trRadiusY = topRight.y,
                    brRadiusX = bottomRight.x,
                    brRadiusY = bottomRight.y,
                    blRadiusX = bottomLeft.x,
                    blRadiusY = bottomLeft.y
            )
        }

        // / Construct a rounded rectangle from its bounding box and and topLeft,
        // / topRight, bottomRight, and bottomLeft radii.
        // /
        // / The corner radii default to [Radius.zero], i.e. right-angled corners
        fun fromRectAndCorners(
            rect: Rect,
            topLeft: Radius = Radius.zero,
            topRight: Radius = Radius.zero,
            bottomRight: Radius = Radius.zero,
            bottomLeft: Radius = Radius.zero
        ): RRect {

            return RRect(
                    left = rect.left,
                    top = rect.top,
                    right = rect.right,
                    bottom = rect.bottom,
                    tlRadiusX = topLeft.x,
                    tlRadiusY = topLeft.y,
                    trRadiusX = topRight.x,
                    trRadiusY = topRight.y,
                    brRadiusX = bottomRight.x,
                    brRadiusY = bottomRight.y,
                    blRadiusX = bottomLeft.x,
                    blRadiusY = bottomLeft.y
            )
        }

        fun fromList(list: List<Double>): RRect {
            return RRect(
                    left = list[0],
                    top = list[1],
                    right = list[2],
                    bottom = list[3],
                    tlRadiusX = list[4],
                    tlRadiusY = list[5],
                    trRadiusX = list[6],
                    trRadiusY = list[7],
                    brRadiusX = list[8],
                    brRadiusY = list[9],
                    blRadiusX = list[10],
                    blRadiusY = list[11]
            )
        }

        // / Linearly interpolate between two rounded rectangles.
        // /
        // / If either is null, this function substitutes [RRect.zero] instead.
        // /
        // / The `t` argument represents position on the timeline, with 0.0 meaning
        // / that the interpolation has not started, returning `a` (or something
        // / equivalent to `a`), 1.0 meaning that the interpolation has finished,
        // / returning `b` (or something equivalent to `b`), and values in between
        // / meaning that the interpolation is at the relevant point on the timeline
        // / between `a` and `b`. The interpolation can be extrapolated beyond 0.0 and
        // / 1.0, so negative values and values greater than 1.0 are valid (and can
        // / easily be generated by curves such as [Curves.elasticInOut]).
        // /
        // / Values for `t` are usually obtained from an [Animation<double>], such as
        // / an [AnimationController].
        fun lerp(a: RRect, b: RRect, t: Double): RRect? {
            assert(t != null)
            if (a == null && b == null)
                return null
            if (a == null) {
                return RRect.fromList(listOf(
                        b.left * t,
                        b.top * t,
                        b.right * t,
                        b.bottom * t,
                        b.tlRadiusX * t,
                        b.tlRadiusY * t,
                        b.trRadiusX * t,
                        b.trRadiusY * t,
                        b.brRadiusX * t,
                        b.brRadiusY * t,
                        b.blRadiusX * t,
                        b.blRadiusY * t
                ))
            }
            if (b == null) {
                val k = 1.0 - t
                return RRect.fromList(listOf(
                        a.left * k,
                        a.top * k,
                        a.right * k,
                        a.bottom * k,
                        a.tlRadiusX * k,
                        a.tlRadiusY * k,
                        a.trRadiusX * k,
                        a.trRadiusY * k,
                        a.brRadiusX * k,
                        a.brRadiusY * k,
                        a.blRadiusX * k,
                        a.blRadiusY * k
                ))
            }
            return RRect.fromList(listOf(
                    lerpDouble(a.left, b.left, t),
                    lerpDouble(a.top, b.top, t),
                    lerpDouble(a.right, b.right, t),
                    lerpDouble(a.bottom, b.bottom, t),
                    lerpDouble(a.tlRadiusX, b.tlRadiusX, t),
                    lerpDouble(a.tlRadiusY, b.tlRadiusY, t),
                    lerpDouble(a.trRadiusX, b.trRadiusX, t),
                    lerpDouble(a.trRadiusY, b.trRadiusY, t),
                    lerpDouble(a.brRadiusX, b.brRadiusX, t),
                    lerpDouble(a.brRadiusY, b.brRadiusY, t),
                    lerpDouble(a.blRadiusX, b.blRadiusX, t),
                    lerpDouble(a.blRadiusY, b.blRadiusY, t)
            ))
        }

        // / A rounded rectangle with all the values set to zero.
        val zero = RRect(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    }

    var _scaled: RRect? = null; // same RRect with scaled radii per side

    // / Returns a new [RRect] translated by the given offset.
    fun shift(offset: Offset): RRect {
        return RRect.fromLTRBAndCorners(
                left = left + offset.dx,
                top = top + offset.dy,
                right = right + offset.dx,
                bottom = bottom + offset.dy,
                topLeft = Radius.elliptical(tlRadiusX, tlRadiusY),
                topRight = Radius.elliptical(trRadiusX, trRadiusY),
                bottomRight = Radius.elliptical(brRadiusX, brRadiusY),
                bottomLeft = Radius.elliptical(blRadiusX, blRadiusY)
        )
    }

    // / Returns a new [RRect] with edges and radii moved outwards by the given
    // / delta.
    fun inflate(delta: Double): RRect {
        return RRect.fromLTRBAndCorners(
                left = left - delta,
                top = top - delta,
                right = right + delta,
                bottom = bottom + delta,
                topLeft = Radius.elliptical(tlRadiusX + delta, tlRadiusY + delta),
                topRight = Radius.elliptical(trRadiusX + delta, trRadiusY + delta),
                bottomRight = Radius.elliptical(brRadiusX + delta, brRadiusY + delta),
                bottomLeft = Radius.elliptical(blRadiusX + delta, blRadiusY + delta)
        )
    }

    // / Returns a new [RRect] with edges and radii moved inwards by the given delta.
    fun deflate(delta: Double): RRect = inflate(-delta)

    // / The distance between the left and right edges of this rectangle.
    val width = right - left

    // / The distance between the top and bottom edges of this rectangle.
    val height = bottom - top

    // / The bounding box of this rounded rectangle (the rectangle with no rounded corners).
    val outerRect: Rect by lazy { Rect.fromLTRB(left, top, right, bottom) }

    // / The non-rounded rectangle that is constrained by the smaller of the two
    // / diagonals, with each diagonal traveling through the middle of the curve
    // / corners. The middle of a corner is the intersection of the curve with its
    // / respective quadrant bisector.
    val safeInnerRect: Rect by lazy {
        val kInsetFactor = 0.29289321881; // 1-cos(pi/4)

        val leftRadius = Math.max(blRadiusX, tlRadiusX)
        val topRadius = Math.max(tlRadiusY, trRadiusY)
        val rightRadius = Math.max(trRadiusX, brRadiusX)
        val bottomRadius = Math.max(brRadiusY, blRadiusY)

        Rect.fromLTRB(
                left + leftRadius * kInsetFactor,
                top + topRadius * kInsetFactor,
                right - rightRadius * kInsetFactor,
                bottom - bottomRadius * kInsetFactor
        )
    }

    // / The rectangle that would be formed using the axis-aligned intersection of
    // / the sides of the rectangle, i.e., the rectangle formed from the
    // / inner-most centers of the ellipses that form the corners. This is the
    // / intersection of the [wideMiddleRect] and the [tallMiddleRect]. If any of
    // / the intersections are void, the resulting [Rect] will have negative width
    // / or height.
    val middleRect: Rect by lazy {
        val leftRadius = Math.max(blRadiusX, tlRadiusX)
        val topRadius = Math.max(tlRadiusY, trRadiusY)
        val rightRadius = Math.max(trRadiusX, brRadiusX)
        val bottomRadius = Math.max(brRadiusY, blRadiusY)
        Rect.fromLTRB(
                left + leftRadius,
                top + topRadius,
                right - rightRadius,
                bottom - bottomRadius
        )
    }

    // / The biggest rectangle that is entirely inside the rounded rectangle and
    // / has the full width of the rounded rectangle. If the rounded rectangle does
    // / not have an axis-aligned intersection of its left and right side, the
    // / resulting [Rect] will have negative width or height.
    val wideMiddleRect: Rect by lazy {
        val topRadius = Math.max(tlRadiusY, trRadiusY)
        val bottomRadius = Math.max(brRadiusY, blRadiusY)
        Rect.fromLTRB(
                left,
                top + topRadius,
                right,
                bottom - bottomRadius
        )
    }

    // / The biggest rectangle that is entirely inside the rounded rectangle and
    // / has the full height of the rounded rectangle. If the rounded rectangle
    // / does not have an axis-aligned intersection of its top and bottom side, the
    // / resulting [Rect] will have negative width or height.
    val tallMiddleRect: Rect by lazy {
        val leftRadius = Math.max(blRadiusX, tlRadiusX)
        val rightRadius = Math.max(trRadiusX, brRadiusX)
        Rect.fromLTRB(
                left + leftRadius,
                top,
                right - rightRadius,
                bottom
        )
    }

    // / Whether this rounded rectangle encloses a non-zero area.
    // / Negative areas are considered empty.
    fun isEmpty() = left >= right || top >= bottom

    // / Whether all coordinates of this rounded rectangle are finite.
    fun isFinite() = left.isFinite() && top.isFinite() && right.isFinite() && bottom.isFinite()

    // / Whether this rounded rectangle is a simple rectangle with zero
    // / corner radii.
    fun isRect(): Boolean {
        return (tlRadiusX == 0.0 || tlRadiusY == 0.0) &&
                (trRadiusX == 0.0 || trRadiusY == 0.0) &&
                (blRadiusX == 0.0 || blRadiusY == 0.0) &&
                (brRadiusX == 0.0 || brRadiusY == 0.0)
    }

    // / Whether this rounded rectangle has a side with no straight section.
    fun isStadium(): Boolean {
        return (
                tlRadius == trRadius && trRadius == brRadius && brRadius == blRadius &&
                        (width <= 2.0 * tlRadiusX || height <= 2.0 * tlRadiusY)
                )
    }

    // / Whether this rounded rectangle has no side with a straight section.
    fun isEllipse(): Boolean {
        return (
                tlRadius == trRadius && trRadius == brRadius && brRadius == blRadius &&
                        width <= 2.0 * tlRadiusX && height <= 2.0 * tlRadiusY
                )
    }

    // / Whether this rounded rectangle would draw as a circle.
    fun isCircle() = width == height && isEllipse()

    // / The lesser of the magnitudes of the [width] and the [height] of this
    // / rounded rectangle.
    val shortestSide: Double by lazy { Math.min(width.absoluteValue, height.absoluteValue) }

    // / The greater of the magnitudes of the [width] and the [height] of this
    // / rounded rectangle.
    val longestSide: Double by lazy { Math.max(width.absoluteValue, height.absoluteValue) }

    // / The offset to the point halfway between the left and right and the top and
    // / bottom edges of this rectangle.
    val center: Offset by lazy { Offset(left + width / 2.0, top + height / 2.0) }

    // Returns the minimum between min and scale to which radius1 and radius2
    // should be scaled with in order not to exceed the limit.
    private fun _getMin(min: Double, radius1: Double, radius2: Double, limit: Double): Double {
        val sum = radius1 + radius2
        if (sum > limit && sum != 0.0)
            return Math.min(min, limit / sum)
        return min
    }

    // Scales all radii so that on each side their sum will not pass the size of
    // the width/height.
    //
    // Inspired from:
    //   https://github.com/google/skia/blob/master/src/core/SkRRect.cpp#L164
    fun _scaleRadii() {
        if (_scaled == null) {
            var scale = 1.0
            // final List<double> scaled = new List<double>.from(_value);

            var scaled = this
            scale = _getMin(scale, blRadiusY, tlRadiusY, height)
            scale = _getMin(scale, tlRadiusX, trRadiusX, width)
            scale = _getMin(scale, trRadiusY, brRadiusY, height)
            scale = _getMin(scale, brRadiusX, blRadiusX, width)

            _scaled = RRect(
                    left = left * scale,
                    top = top * scale,
                    right = right * scale,
                    bottom = bottom * scale,
                    tlRadiusX = tlRadiusX * scale,
                    tlRadiusY = tlRadiusY * scale,
                    trRadiusX = trRadiusX * scale,
                    trRadiusY = trRadiusY * scale,
                    brRadiusX = brRadiusX * scale,
                    brRadiusY = brRadiusY * scale,
                    blRadiusX = blRadiusX * scale,
                    blRadiusY = blRadiusY * scale
            )
        }
    }

    // / Whether the point specified by the given offset (which is assumed to be
    // / relative to the origin) lies inside the rounded rectangle.
    // /
    // / This method may allocate (and cache) a copy of the object with normalized
    // / radii the first time it is called on a particular [RRect] instance. When
    // / using this method, prefer to reuse existing [RRect]s rather than
    // / recreating the object each time.
    fun contains(point: Offset): Boolean {
        if (point.dx < left || point.dx >= right || point.dy < top || point.dy >= bottom)
            return false; // outside bounding box

        _scaleRadii()
        val scaled = _scaled!! // We know it is not null

        var x: Double = 0.0
        var y: Double = 0.0
        var radiusX: Double = 0.0
        var radiusY: Double = 0.0
        // check whether point is in one of the rounded corner areas
        // x, y -> translate to ellipse center
        if (point.dx < left + scaled.tlRadiusX &&
                point.dy < top + scaled.tlRadiusY) {
            x = point.dx - left - scaled.tlRadiusX
            y = point.dy - top - scaled.tlRadiusY
            radiusX = scaled.tlRadiusX
            radiusY = scaled.tlRadiusY
        } else if (point.dx > right - scaled.trRadiusX &&
                point.dy < top + scaled.trRadiusY) {
            x = point.dx - right + scaled.trRadiusX
            y = point.dy - top - scaled.trRadiusY
            radiusX = scaled.trRadiusX
            radiusY = scaled.trRadiusY
        } else if (point.dx > right - scaled.brRadiusX &&
                point.dy > bottom - scaled.brRadiusY) {
            x = point.dx - right + scaled.brRadiusX
            y = point.dy - bottom + scaled.brRadiusY
            radiusX = scaled.brRadiusX
            radiusY = scaled.brRadiusY
        } else if (point.dx < left + scaled.blRadiusX &&
                point.dy > bottom - scaled.blRadiusY) {
            x = point.dx - left - scaled.blRadiusX
            y = point.dy - bottom + scaled.blRadiusY
            radiusX = scaled.blRadiusX
            radiusY = scaled.blRadiusY
        } else {
            return true; // inside and not within the rounded corner area
        }

        x = x / radiusX
        y = y / radiusY
        // check if the point is outside the unit circle
        if (x * x + y * y > 1.0)
            return false
        return true
    }

// TODO(Migration/Filip): Not needed for data class
//    @override
//    bool operator ==(dynamic other) {
//        if (identical(this, other))
//            return true;
//        if (runtimeType != other.runtimeType)
//            return false;
//        final RRect typedOther = other;
//        for (int i = 0; i < _kDataSize; i += 1) {
//            if (_value[i] != typedOther._value[i])
//                return false;
//        }
//        return true;
//    }
//
//    @override
//    int get hashCode => hashList(_value);

    override fun toString(): String {
        val rect =
                "${left.toStringAsFixed(1)}, " +
                "${top.toStringAsFixed(1)}, " +
                "${right.toStringAsFixed(1)}, " +
                "${bottom.toStringAsFixed(1)}"
        if (tlRadius == trRadius &&
                trRadius == brRadius &&
                brRadius == blRadius) {
            if (tlRadius.x == tlRadius.y)
                return "RRect.fromLTRBR($rect, ${tlRadius.x.toStringAsFixed(1)})"
            return "RRect.fromLTRBXY($rect, ${tlRadius.x.toStringAsFixed(1)}, ${tlRadius.y.toStringAsFixed(1)})"
        }
        return "RRect.fromLTRBAndCorners(" +
                "$rect, " +
                "topLeft: $tlRadius, " +
                "topRight: $trRadius, " +
                "bottomRight: $brRadius, " +
                "bottomLeft: $blRadius" +
                ")"
    }
}