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
import androidx.ink.geometry.internal.ParallelogramNative
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
 * Note: Java code should use the factory static function `from*` in [MutableParallelogram] or
 * [ImmutableParallelogram] to create [Parallelogram] instances.
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

    /**
     * Returns the minimum bounding box containing the [Parallelogram].
     *
     * Performance-sensitive code should use the [computeBoundingBox] overload that takes a
     * pre-allocated [MutableBox], so that instance can be reused across multiple calls.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
    public fun computeBoundingBox(): ImmutableBox {
        return ParallelogramNative.createBoundingBox(
            center.x,
            center.y,
            width,
            height,
            rotation,
            shearFactor,
            ImmutableBox::class.java,
            ImmutableVec::class.java,
        )
    }

    /** Returns the minimum bounding box containing the [Parallelogram]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
    public fun computeBoundingBox(outBox: MutableBox): MutableBox {
        ParallelogramNative.populateBoundingBox(
            center.x,
            center.y,
            width,
            height,
            rotation,
            shearFactor,
            outBox,
        )
        return outBox
    }

    /**
     * Returns the semi axes of this [Parallelogram]. These are equal to:
     * ```
     * - (.5 * w * cos(θ), .5 * w * sin(θ))
     * - (.5 * h * (s * cos(θ) - sin(θ)), .5 * h * (s * sin(θ) + cos(θ)))
     * ```
     *
     * respectively, where w = [width], h = [height], θ = [rotation], and s = [shearFactor]
     *
     * The semi-axes of a parallelogram are two vectors. Each one points from the center to the
     * midpoint of an edge. The first semi-axis points from the center to the midpoint of the edge
     * between corners 1 and 2, and the second semi-axis points from the center to the midpoint of
     * the edge between corners 2 and 3. In a Y-up coordinate system, on the base rectangle, these
     * two edges are the right and top, respectively. In a Y-down coordinate system, on the base
     * rectangle, they are the right and bottom, respectively.
     *
     * Performance-sensitive code should use the [computeSemiAxes] overload that takes a
     * pre-allocated [MutableVec]s, so that instances can be reused across multiple calls.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
    public fun computeSemiAxes(): List<ImmutableVec> {
        return ParallelogramNative.createSemiAxes(
                center.x,
                center.y,
                width,
                height,
                rotation,
                shearFactor,
                ImmutableVec::class.java,
            )
            .toList()
    }

    /**
     * Fills the [MutableVec]s with the semi axes of this [Parallelogram]. For definition please see
     * [computeSemiAxes] above.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
    public fun computeSemiAxes(outAxis1: MutableVec, outAxis2: MutableVec) {
        ParallelogramNative.populateSemiAxes(
            center.x,
            center.y,
            width,
            height,
            rotation,
            shearFactor,
            outAxis1,
            outAxis2,
        )
    }

    /**
     * Returns a list containing the 4 corners of the [Parallelogram].
     *
     * Corners are numbered 0, 1, 2, 3. In a Y-up coordinate system, the corners of the base
     * rectangle are, in order: bottom-left, bottom-right, top-right, top-left. In a Y-down
     * coordinate system, they are: top-left, top-right, bottom-right, bottom-left. The corners keep
     * their numbering through any shear and/or rotation applied to the base rectangle. Numerically,
     * the corners are equivalent to: `C - u - v C + u - v C + u + v C - u + v` Where `C` =
     * [center], and `u` and `v` are the [semiAxes].
     *
     * Performance-sensitive code should use the [computeCorners] overload that takes pre-allocated
     * [MutableVec]s, so that instances can be reused across multiple calls.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
    public fun computeCorners(): List<ImmutableVec> {
        return ParallelogramNative.createCorners(
                center.x,
                center.y,
                width,
                height,
                rotation,
                shearFactor,
                ImmutableVec::class.java,
            )
            .toList()
    }

    /**
     * Populates the 4 output points with the corners of the [Parallelogram].
     *
     * For explanation of order, please see [computeCorners] above.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
    public fun computeCorners(
        outCorner1: MutableVec,
        outCorner2: MutableVec,
        outCorner3: MutableVec,
        outCorner4: MutableVec,
    ) {
        ParallelogramNative.populateCorners(
            center.x,
            center.y,
            width,
            height,
            rotation,
            shearFactor,
            outCorner1,
            outCorner2,
            outCorner3,
            outCorner4,
        )
    }

    /**
     * Returns whether the given point is contained within the Box. Points that lie exactly on the
     * Box's boundary are considered to be contained.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
    public fun contains(point: ImmutableVec): Boolean {
        return ParallelogramNative.contains(
            center.x,
            center.y,
            width,
            height,
            rotation,
            shearFactor,
            point.x,
            point.y,
        )
    }

    /**
     * Compares this [Parallelogram] with [other], and returns true if both [center] points are
     * considered almost equal with the given [tolerance], and the difference between [width] and
     * [other.width] is less than [tolerance], and likewise for [height], [rotation], and
     * [shearFactor].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
    public fun isAlmostEqual(
        other: Parallelogram,
        @FloatRange(from = 0.0) tolerance: Float,
    ): Boolean =
        abs(center.x - other.center.x) < tolerance &&
            abs(center.y - other.center.y) < tolerance &&
            abs(width - other.width) < tolerance &&
            abs(height - other.height) < tolerance &&
            abs(rotation - other.rotation) < tolerance &&
            abs(shearFactor - other.shearFactor) < tolerance

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
