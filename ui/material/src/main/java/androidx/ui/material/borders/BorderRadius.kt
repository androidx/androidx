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

package androidx.ui.material.borders

import androidx.ui.engine.geometry.RRect
import androidx.ui.engine.geometry.Radius
import androidx.ui.engine.geometry.Rect
import androidx.ui.engine.geometry.lerp
import androidx.ui.engine.text.TextDirection

/**
 * An immutable set of radii for each corner of a Rectangle.
 *
 * Used by [BoxDecoration] when the shape is a [BoxShape.Rectangle].
 *
 * The [BorderRadius] class specifies offsets in terms of visual corners, e.g.
 * [topLeft]. These values are not affected by the [TextDirection]. To support
 * both left-to-right and right-to-left layouts, consider using
 * [BorderRadiusDirectional], which is expressed in terms that are relative to
 * a [TextDirection] (typically obtained from the ambient [Directionality]).
 */
class BorderRadius(
    override val topLeft: Radius = Radius.zero,
    override val topRight: Radius = Radius.zero,
    override val bottomLeft: Radius = Radius.zero,
    override val bottomRight: Radius = Radius.zero
) : BorderRadiusGeometry() {

    override val topStart = Radius.zero
    override val topEnd = Radius.zero
    override val bottomStart = Radius.zero
    override val bottomEnd = Radius.zero

    /** Creates an [RRect] from the current border radius and a [Rect]. */
    fun toRRect(rect: Rect) = RRect(
        rect,
        topLeft = this.topLeft,
        topRight = this.topRight,
        bottomLeft = this.bottomLeft,
        bottomRight = this.bottomRight
    )

    override fun subtract(other: BorderRadiusGeometry): BorderRadiusGeometry {
        if (other is BorderRadius)
            return this - other
        return super.subtract(other)
    }

    override fun add(other: BorderRadiusGeometry): BorderRadiusGeometry {
        if (other is BorderRadius) {
            return this + other
        }
        return super.add(other)
    }

    /** Returns the sum of two [BorderRadius] objects. */
    operator fun plus(other: BorderRadius): BorderRadius {
        return BorderRadius(
            topLeft = topLeft + other.topLeft,
            topRight = topRight + other.topRight,
            bottomLeft = bottomLeft + other.bottomLeft,
            bottomRight = bottomRight + other.bottomRight
        )
    }

    /** Returns the difference between two [BorderRadius] objects. */
    operator fun minus(other: BorderRadiusGeometry): BorderRadius {
        return BorderRadius(
            topLeft = topLeft - other.topLeft,
            topRight = topRight - other.topRight,
            bottomLeft = bottomLeft - other.bottomLeft,
            bottomRight = bottomRight - other.bottomRight
        )
    }

    /**
     * Returns the [BorderRadius] object with each corner negated.
     *
     * This is the same as multiplying the object by -1.0.
     */
    override fun unaryMinus(): BorderRadius {
        return BorderRadius(
            topLeft = -topLeft,
            topRight = -topRight,
            bottomLeft = -bottomLeft,
            bottomRight = -bottomRight
        )
    }

    /** Scales each corner of the [BorderRadius] by the given factor. */
    override operator fun times(other: Float) = BorderRadius(
        topLeft = this.topLeft * other,
        topRight = this.topRight * other,
        bottomLeft = this.bottomLeft * other,
        bottomRight = this.bottomRight * other
    )

    /** Divides each corner of the [BorderRadius] by the given factor. */
    override operator fun div(other: Float): BorderRadius {
        return BorderRadius(
            topLeft = this.topLeft / other,
            topRight = this.topRight / other,
            bottomLeft = this.bottomLeft / other,
            bottomRight = this.bottomRight / other
        )
    }

    /** Integer divides each corner of the [BorderRadius] by the given factor. */
    override fun truncDiv(other: Float): BorderRadiusGeometry {
        return BorderRadius(
            topLeft = topLeft.truncDiv(other),
            topRight = topRight.truncDiv(other),
            bottomLeft = bottomLeft.truncDiv(other),
            bottomRight = bottomRight.truncDiv(other)
        )
    }

    /** Computes the remainder of each corner by the given factor. */
    override operator fun rem(other: Float) = BorderRadius(
        topLeft = this.topLeft % other,
        topRight = this.topRight % other,
        bottomLeft = this.bottomLeft % other,
        bottomRight = this.bottomRight % other
    )

    override fun resolve(direction: TextDirection?) = this

    companion object {

        /** Creates a border radius where all radii are [radius]. */
        fun all(radius: Radius) = BorderRadius(
            topLeft = radius,
            topRight = radius,
            bottomLeft = radius,
            bottomRight = radius
        )

        /** Creates a border radius where all radii are [Radius.circular(radius)]. */
        fun circular(radius: Float) = all(
            Radius.circular(radius)
        )

        /**
         * Creates a vertically symmetric border radius where the top and bottom
         * sides of the rectangle have the same radii.
         */
        fun vertical(
            top: Radius = Radius.zero,
            bottom: Radius = Radius.zero
        ) = BorderRadius(
            topLeft = top,
            topRight = top,
            bottomLeft = bottom,
            bottomRight = bottom
        )

        /**
         * Creates a horizontally symmetrical border radius where the left and right
         * sides of the rectangle have the same radii.
         */
        fun horizontal(
            left: Radius = Radius.zero,
            right: Radius = Radius.zero
        ) = BorderRadius(
            topLeft = left,
            topRight = right,
            bottomLeft = left,
            bottomRight = right
        )

        /** A border radius with all zero radii. */
        @JvmStatic
        val Zero = all(Radius.zero)
    }
}

/**
 * Linearly interpolate between two [BorderRadius] objects.
 *
 * If either is null, this function interpolates from [BorderRadius.Zero].
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
fun lerp(a: BorderRadius?, b: BorderRadius?, t: Float): BorderRadius? {
    if (a == null && b == null)
        return null
    if (a == null)
        return b!! * t
    if (b == null)
        return a * (1.0f - t)
    return BorderRadius(
        topLeft = lerp(a.topLeft, b.topLeft, t),
        topRight = lerp(a.topRight, b.topRight, t),
        bottomLeft = lerp(a.bottomLeft, b.bottomLeft, t),
        bottomRight = lerp(a.bottomRight, b.bottomRight, t)
    )
}