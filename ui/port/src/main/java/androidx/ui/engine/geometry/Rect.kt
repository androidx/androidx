package androidx.ui.engine.geometry

import androidx.ui.lerpDouble
import androidx.ui.toStringAsFixed
import kotlin.math.absoluteValue

// / An immutable, 2D, axis-aligned, floating-point rectangle whose coordinates
// / are relative to a given origin.
// /
// / A Rect can be created with one its constructors or from an [Offset] and a
// / [Size] using the `&` operator:
// /
// / ```dart
// / Rect myRect = const Offset(1.0, 2.0) & const Size(3.0, 4.0);
// / ```
data class Rect(
        // The offset of the left edge of this rectangle from the x axis.
    val left: Double,
        // The offset of the top edge of this rectangle from the y axis.
    val top: Double,
        // The offset of the right edge of this rectangle from the x axis.
    val right: Double,
        // The offset of the bottom edge of this rectangle from the y axis.
    val bottom: Double
) {

    companion object {
        // / Construct a rectangle from its left, top, right, and bottom edges.
        fun fromLTRB(left: Double, top: Double, right: Double, bottom: Double): Rect {
            return Rect(left, top, right, bottom)
        }

        // / Construct a rectangle from its left and top edges, its width, and its
        // / height.
        // /
        // / To construct a [Rect] from an [Offset] and a [Size], you can use the
        // / rectangle constructor operator `&`. See [Offset.&].
        fun fromLTWH(left: Double, top: Double, width: Double, height: Double): Rect {
            return Rect(left, top, left + width, top + height)
        }

        // / Construct a rectangle that bounds the given circle.
        // /
        // / The `center` argument is assumed to be an offset from the origin.
        fun fromCircle(center: Offset, radius: Double): Rect {
            return Rect(
                    center.dx - radius,
                    center.dy - radius,
                    center.dx + radius,
                    center.dy + radius
            )
        }

        // / Construct the smallest rectangle that encloses the given offsets, treating
        // / them as vectors from the origin.
        fun fromPoints(a: Offset, b: Offset): Rect {
            return Rect(
                    Math.min(a.dx, b.dx),
                    Math.min(a.dy, b.dy),
                    Math.max(a.dx, b.dx),
                    Math.max(a.dy, b.dy)
            )
        }

        // / A rectangle with left, top, right, and bottom edges all at zero.
        val zero: Rect = Rect(0.0, 0.0, 0.0, 0.0)

        val _giantScalar: Double = 1.0E+9 // matches kGiantRect from default_layer_builder.cc

        // / A rectangle that covers the entire coordinate space.
        // /
        // / This covers the space from -1e9,-1e9 to 1e9,1e9.
        // / This is the space over which graphics operations are valid.
        val largest: Rect = fromLTRB(-_giantScalar, -_giantScalar, _giantScalar, _giantScalar)
    }

    // / The distance between the left and right edges of this rectangle.
    val width = right - left

    // / The distance between the top and bottom edges of this rectangle.
    val height = bottom - top

    // static const int _kDataSize = 4;
    // final Float32List _value = new Float32List(_kDataSize);
    // double get left => _value[0];
    // double get top => _value[1];
    //
    // double get right => _value[2];
    // / The offset of the bottom edge of this rectangle from the y axis.
    // double get bottom => _value[3];

    // / The distance between the upper-left corner and the lower-right corner of
    // / this rectangle.
    fun getSize() = Size(width, height)

    // / Whether any of the coordinates of this rectangle are equal to positive infinity.
    // included for consistency with Offset and Size
    fun isInfinite(): Boolean {
        return left >= Double.POSITIVE_INFINITY ||
                top >= Double.POSITIVE_INFINITY ||
                right >= Double.POSITIVE_INFINITY ||
                bottom >= Double.POSITIVE_INFINITY
    }

    // / Whether all coordinates of this rectangle are finite.
    fun isFinite(): Boolean =
            left.isFinite() &&
            top.isFinite() &&
            right.isFinite() &&
            bottom.isFinite()

    // / Whether this rectangle encloses a non-zero area. Negative areas are
    // / considered empty.
    fun isEmpty(): Boolean = left >= right || top >= bottom

    // / Returns a new rectangle translated by the given offset.
    // /
    // / To translate a rectangle by separate x and y components rather than by an
    // / [Offset], consider [translate].
    fun shift(offset: Offset): Rect {
        return fromLTRB(left + offset.dx, top + offset.dy, right + offset.dx, bottom + offset.dy)
    }

    // / Returns a new rectangle with translateX added to the x components and
    // / translateY added to the y components.
    // /
    // / To translate a rectangle by an [Offset] rather than by separate x and y
    // / components, consider [shift].
    fun translate(translateX: Double, translateY: Double): Rect {
        return fromLTRB(
                left + translateX,
                top + translateY,
                right + translateX,
                bottom + translateY
        )
    }

    // / Returns a new rectangle with edges moved outwards by the given delta.
    fun inflate(delta: Double): Rect {
        return fromLTRB(left - delta, top - delta, right + delta, bottom + delta)
    }

    // / Returns a new rectangle with edges moved inwards by the given delta.
    fun deflate(delta: Double): Rect = inflate(-delta)

    // / Returns a new rectangle that is the intersection of the given
    // / rectangle and this rectangle. The two rectangles must overlap
    // / for this to be meaningful. If the two rectangles do not overlap,
    // / then the resulting Rect will have a negative width or height.
    fun intersect(other: Rect): Rect {
        return fromLTRB(
                Math.max(left, other.left),
                Math.max(top, other.top),
                Math.min(right, other.right),
                Math.min(bottom, other.bottom)
        )
    }

    // / Returns a new rectangle which is the bounding box containing this
    // / rectangle and the given rectangle.
    fun expandToInclude(other: Rect): Rect {
        return fromLTRB(
                Math.min(left, other.left),
                Math.min(top, other.top),
                Math.max(right, other.right),
                Math.max(bottom, other.bottom)
        )
    }

    // / Whether `other` has a nonzero area of overlap with this rectangle.
    fun overlaps(other: Rect): Boolean {
        if (right <= other.left || other.right <= left)
            return false
        if (bottom <= other.top || other.bottom <= top)
            return false
        return true
    }

    // / The lesser of the magnitudes of the [width] and the [height] of this
    // / rectangle.
    fun getShortestSide(): Double = Math.min(width.absoluteValue, height.absoluteValue)

    // / The greater of the magnitudes of the [width] and the [height] of this
    // / rectangle.
    fun getLongestSide(): Double = Math.max(width.absoluteValue, height.absoluteValue)

    // / The offset to the intersection of the top and left edges of this rectangle.
    // /
    // / See also [Size.topLeft].
    fun getTopLeft(): Offset = Offset(left, top)

    // / The offset to the center of the top edge of this rectangle.
    // /
    // / See also [Size.topCenter].
    fun getTopCenter(): Offset = Offset(left + width / 2.0, top)

    // / The offset to the intersection of the top and right edges of this rectangle.
    // /
    // / See also [Size.topRight].
    fun getTopRight(): Offset = Offset(right, top)

    // / The offset to the center of the left edge of this rectangle.
    // /
    // / See also [Size.centerLeft].
    fun getCenterLeft(): Offset = Offset(left, top + height / 2.0)

    // / The offset to the point halfway between the left and right and the top and
    // / bottom edges of this rectangle.
    // /
    // / See also [Size.center].
    fun getCenter(): Offset = Offset(left + width / 2.0, top + height / 2.0)

    // / The offset to the center of the right edge of this rectangle.
    // /
    // / See also [Size.centerLeft].
    fun getCenterRight(): Offset = Offset(right, top + height / 2.0)

    // / The offset to the intersection of the bottom and left edges of this rectangle.
    // /
    // / See also [Size.bottomLeft].
    fun getBottomLeft(): Offset = Offset(left, bottom)

    // / The offset to the center of the bottom edge of this rectangle.
    // /
    // / See also [Size.bottomLeft].
    fun getBottomCenter(): Offset = Offset(left + width / 2.0, bottom)

    // / The offset to the intersection of the bottom and right edges of this rectangle.
    // /
    // / See also [Size.bottomRight].
    fun getBottomRight(): Offset = Offset(right, bottom)

    // / Whether the point specified by the given offset (which is assumed to be
    // / relative to the origin) lies between the left and right and the top and
    // / bottom edges of this rectangle.
    // /
    // / Rectangles include their top and left edges but exclude their bottom and
    // / right edges.
    fun contains(offset: Offset): Boolean {
        return offset.dx >= left && offset.dx < right && offset.dy >= top && offset.dy < bottom
    }

    // / Linearly interpolate between two rectangles.
    // /
    // / If either rect is null, [Rect.zero] is used as a substitute.
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
    fun lerp(a: Rect, b: Rect, t: Double): Rect? {
        assert(t != null)
        if (a == null && b == null)
            return null
        if (a == null)
            return fromLTRB(b.left * t, b.top * t, b.right * t, b.bottom * t)
        if (b == null) {
            val k = 1.0 - t
            return fromLTRB(a.left * k, a.top * k, a.right * k, a.bottom * k)
        }
        return fromLTRB(
                lerpDouble(a.left, b.left, t),
                lerpDouble(a.top, b.top, t),
                lerpDouble(a.right, b.right, t),
                lerpDouble(a.bottom, b.bottom, t)
        )
    }

// TODO(Migration/Filip): I think data class handles this no need to re-implement
//    @override
//    bool operator ==(dynamic other) {
//        if (identical(this, other))
//            return true;
//        if (runtimeType != other.runtimeType)
//            return false;
//        final Rect typedOther = other;
//        for (int i = 0; i < _kDataSize; i += 1) {
//            if (_value[i] != typedOther._value[i])
//                return false;
//        }
//        return true;
//    }
//
//    @override
//    int get hashCode => hashList(_value);

    override fun toString() = "Rect.fromLTRB(" +
            "${left.toStringAsFixed(1)}, " +
            "${top.toStringAsFixed(1)}, " +
            "${right.toStringAsFixed(1)}, " +
            "${bottom.toStringAsFixed(1)})"
}