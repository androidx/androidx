/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.geometry

import androidx.annotation.FloatRange
import androidx.annotation.RestrictTo
import kotlin.math.abs

/**
 * This class represents a parallelogram (i.e. a quadrilateral with parallel sides), defined by its
 * [center], [width], [height], [rotation], and [shearFactor].
 *
 * Parameters of a [Parallelogram] are used to define a pair of vector semi-axes:
 * ```
 * u = {.5 * w * cos(θ), .5 * w * sin(θ)}
 * v = {.5 * h * (s * cos(θ) - sin(θ)), .5 * h * (s * sin(θ) + cos(θ))}
 * ```
 *
 * where `w` is the [width], `h` is the [height], `s` is the [shearFactor] and `θ` is the angle of
 * [rotation]. From the semi-axes, we define the shape of the parallelogram as the set of all points
 * c + 𝛼 * u + 𝛽 * v, where `c` is the center, and `𝛼` and `𝛽` are real numbers in the interval
 * [-1, 1].
 *
 * Note: Java code should use the factory function Parallelogram.from*.
 *
 * A [Parallelogram] may have a positive or negative height; a positive height indicates that the
 * angle from the first semi-axis to the second will also be positive.
 *
 * A [Parallelogram] may have a positive or negative shear factor; a positive shear factor indicates
 * a smaller absolute angle between the semi-axes (the shear factor is, in fact, the cotangent of
 * that angle).
 *
 * A [Parallelogram] may *not* have a negative width. If an operation on a parallelogram or the
 * construction of a parallelogram would result in a negative width, it is instead normalized, by
 * negating both the width and the height, adding π to the angle of rotation, and normalizing
 * rotation to the range [0, 2π).
 *
 * A [Parallelogram] may also be degenerate; that is, its [width] or [height], or both, may be zero.
 * Degenerate [Parallelogram]s may still have a non-zero [rotation] and/or [shearFactor]. A
 * [Parallelogram] that has both [width] and [height] of zero is effectively a point, and so
 * [rotation] and [shearFactor] do not affect the values of the axes or corners. A [Parallelogram]
 * that has either [width] or [height] of zero (but not both) is effectively a line segment, and so
 * is similarly unaffected by [shearFactor].
 *
 * More intuitively, you can think of the shape of the [Parallelogram], before taking the [center]
 * and [rotation] into account, like this:
 * ```
 *        s*h
 *      |------|__________
 *     ⎡       /         /
 *     ⎢      /         /
 *     ⎢     /         /
 *   h ⎢    /         /
 *     ⎢   /         /
 *     ⎢  /         /
 *     ⎣ /_________/
 *       |---------|
 *            w
 * ```
 *
 * Where `w` is the [width], `h` is the [height], and `s` is the [shearFactor]. You then rotate, and
 * translate such that the center is in the correct position.
 *
 * A few geometric objects can be represented as special cases of a [Parallelogram]. A generic
 * rectangle is a [Parallelogram] with [shearFactor] of zero. (It can be rotated with respect to the
 * axes, and hence might have a non-zero [rotation]). A [Box], an axis-aligned rectangle; is a
 * [Parallelogram] with both [rotation] and [shearFactor] of zero.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
public abstract class Parallelogram internal constructor() {

    public abstract val center: Vec

    /**
     * A [Parallelogram] may *not* have a negative width. If an operation on a parallelogram would
     * result in a negative width, it is instead normalized, by negating both the width and the
     * height, adding π to the angle of rotation, and normalizing rotation to the range [0, 2π).
     */
    @get:FloatRange(from = 0.0) public abstract val width: Float

    /**
     * A [Parallelogram] may have a positive or negative height; a positive height indicates that
     * the angle from the first semi-axis to the second will also be positive.
     */
    public abstract val height: Float

    @get:AngleRadiansFloat public abstract val rotation: Float

    /**
     * A [Parallelogram]] may have a positive or negative shear factor; a positive shear factor
     * indicates a smaller absolute angle between the semi-axes (the shear factor is, in fact, the
     * cotangent of that angle).
     */
    public abstract val shearFactor: Float

    /**
     * Returns the signed area of the [Parallelogram]. If either the width or the height is zero,
     * this will be equal to zero; if the width is non-zero, then this will have the same sign as
     * the height.
     */
    public fun computeSignedArea(): Float = width * height

    public companion object {
        /**
         * If the [width] is less than zero or if the [rotation] is not in the range
         * [0, 2π), the [Parallelogram] will be normalized and the normalized values of width,
         * height, and rotation will be used to call [runBlock].
         */
        internal inline fun <P : Parallelogram> normalizeAndRun(
            width: Float,
            height: Float,
            rotation: Float,
            runBlock: (width: Float, height: Float, rotation: Float) -> P,
        ): P {
            return if (width < 0) {
                runBlock(-width, -height, Angle.normalized(rotation + Angle.HALF_TURN_RADIANS))
            } else {
                runBlock(width, height, Angle.normalized(rotation))
            }
        }

        /**
         * Returns true if [first] and [second] have the same values for all properties of
         * [Parallelogram].
         */
        internal fun areEquivalent(first: Parallelogram, second: Parallelogram): Boolean =
            Vec.areEquivalent(first.center, second.center) &&
                first.width == second.width &&
                first.height == second.height &&
                first.rotation == second.rotation &&
                first.shearFactor == second.shearFactor

        /**
         * Returns true if the values of [first] and [second] are less than [tolerance] away from
         * each other for all properties of [Parallelogram]. [tolerance] must be a positive float.
         */
        internal fun areNear(
            first: Parallelogram,
            second: Parallelogram,
            @FloatRange(from = 0.0) tolerance: Float = 0.0001f,
        ): Boolean =
            (abs(first.center.x - second.center.x) < tolerance) &&
                (abs(first.center.y - second.center.y) < tolerance) &&
                (abs(first.width - second.width) < tolerance) &&
                (abs(first.height - second.height) < tolerance) &&
                (abs(first.rotation - second.rotation) < tolerance) &&
                (abs(first.shearFactor - second.shearFactor) < tolerance)

        /** Returns a hash code for [parallelogram] using its [Parallelogram] properties. */
        internal fun hash(parallelogram: Parallelogram): Int {
            var result = parallelogram.center.hashCode()
            result = 31 * result + parallelogram.width.hashCode()
            result = 31 * result + parallelogram.height.hashCode()
            result = 31 * result + parallelogram.rotation.hashCode()
            result = 31 * result + parallelogram.shearFactor.hashCode()
            return result
        }

        /**
         * Returns a string representation for [parallelogram] using its [Parallelogram] properties.
         */
        internal fun string(parallelogram: Parallelogram): String =
            parallelogram.run {
                "Parallelogram(center=$center, " +
                    "width=$width, " +
                    "height=$height, " +
                    "rotation=$rotation, " +
                    "shearFactor=$shearFactor)"
            }
    }
}
