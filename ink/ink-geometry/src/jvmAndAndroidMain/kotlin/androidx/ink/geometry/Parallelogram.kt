/*
 * Copyright (C) 2024-2025 The Android Open Source Project
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
import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative
import kotlin.math.abs

/**
 * This class represents a parallelogram defined by its [center], [width], [height],
 * [rotationDegrees], and [skew].
 *
 * The shape of the parallelogram with [width] `w`, [height] `h`, and [skew] `s` is this before
 * rotation:
 * ```
 *        s*h
 *      |------|__________  Displaced horizontal edge
 *     ⎡       /         /
 *     ⎢      /         /
 *     ⎢     /         /
 *   h ⎢    /         /
 *     ⎢   /         /
 *     ⎢  /         /
 *     ⎣ /_________/  Undisplaced horizontal edge
 *       |---------|
 *            w
 * ```
 *
 * The parallelogram is then translated so that its center is in the correct position and rotated by
 * rotation `θ` (in radians in the equations below).
 *
 * These parameters of a [Parallelogram] are used to define a pair of vector semi-axes:
 * ```
 * u = {.5 * w * cos(θ), .5 * w * sin(θ)}
 * v = {.5 * h * (s * cos(θ) - sin(θ)), .5 * h * (s * sin(θ) + cos(θ))}
 * ```
 *
 * From the semi-axes, we define the shape of the parallelogram as the set of all points c + 𝛼 *
 * u + 𝛽 * v, where `c` is the center, and `𝛼` and `𝛽` are real numbers in the interval [-1, 1].
 *
 * Note: Java code should use the factory static function `from*` in [MutableParallelogram] or
 * [ImmutableParallelogram] to create [Parallelogram] instances.
 *
 * A [Parallelogram] may have a positive or negative [height]. One of the two horizontal edges
 * before rotation is vertically displaced by [height] from the other. The sign of the height
 * corresponds to the sign of the angle between the two semi-axes.
 *
 * A [Parallelogram] may have a positive or negative skew (aka shear). The horizontal edge before
 * rotation that is displaced vertically by height is also displaced horizontally by skew times
 * height. The skew can be positive or negative, a positive skew corresponds to a smaller absolute
 * angle between the two semi-axes. The skew is equal to the cotangent of the absolute angle between
 * the two semi-axes.
 *
 * A [Parallelogram] may *not* have a negative width. If an operation on a parallelogram or the
 * construction of a parallelogram would result in a negative width, it is instead normalized, by
 * negating both the width and the height, adding 180 to the angle of rotation, and normalizing
 * rotation to the range [0, 360).
 *
 * A [Parallelogram] may also be degenerate; that is, its [width] or [height], or both, may be zero.
 * Degenerate [Parallelogram]s may still have a non-zero [rotation] and/or [skew]. A [Parallelogram]
 * that has both [width] and [height] of zero is effectively a point, and so [rotation] and [skew]
 * do not affect the values of the axes or corners. A [Parallelogram] that has [height] of zero is
 * effectively a horizontal line, and so is unaffected by [skew].
 *
 * A few geometric objects can be represented as special cases of a [Parallelogram]. A rectangle is
 * a [Parallelogram] with [skew] of zero. (It can be rotated with respect to the axes, and hence
 * might have a non-zero [rotation].) A [Box], an axis-aligned rectangle, is a [Parallelogram] with
 * both [rotation] and [skew] of zero.
 */
public abstract class Parallelogram internal constructor() {

    public abstract val center: Vec

    /**
     * The width of the [Parallelogram]. A [Parallelogram] may *not* have a negative width. If an
     * operation on a parallelogram would result in a negative width, it is instead normalized, by
     * negating both the width and the height, adding 180 to [rotationDegrees] and normalizing that
     * to the range [0, 360).
     */
    @get:FloatRange(from = 0.0) public abstract val width: Float

    /**
     * The height of the [Parallelogram]. May be positive or negative, corresponding to whether the
     * angle from the first semi-axis to the second is also positive or negative.
     */
    public abstract val height: Float

    /**
     * The rotation of the [Parallelogram] in degrees from its original axis-aligned orientation in
     * the direction from the positive x-axis towards the positive y-axis.
     */
    @get:FloatRange(from = 0.0, to = 360.0, toInclusive = false)
    @get:AngleDegreesFloat
    public abstract val rotationDegrees: Float

    /**
     * The horizontal displacement between the two horizontal edges of the [Parallelogram]
     * pre-rotation, as a multiple of the height. Equivalently, this is the cotangent of the
     * absolute angle between the semi-axes. A [Parallelogram] may have a positive or negative skew,
     * a greater skew indicates a smaller absolute angle between the semi-axes.
     */
    public abstract val skew: Float

    /**
     * Returns an [ImmutableParallelogram] that is equivalent to this [Parallelogram]. If this
     * [Parallelogram] is immutable, the returned [ImmutableParallelogram] will be the same
     * instance.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public abstract fun toImmutable(): ImmutableParallelogram

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
    public fun computeBoundingBox(): ImmutableBox {
        return ParallelogramNative.createBoundingBox(
            center.x,
            center.y,
            width,
            height,
            rotationDegrees,
            skew,
        )
    }

    /** Returns the minimum bounding box containing the [Parallelogram]. */
    public fun computeBoundingBox(outBox: MutableBox): MutableBox {
        ParallelogramNative.populateBoundingBox(
            center.x,
            center.y,
            width,
            height,
            rotationDegrees,
            skew,
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
     * respectively, where w = [width], h = [height], θ = [rotation], and s = [skew]
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
    public fun computeSemiAxes(): List<ImmutableVec> {
        return ParallelogramNative.createSemiAxes(
                center.x,
                center.y,
                width,
                height,
                rotationDegrees,
                skew,
            )
            .toList()
    }

    /**
     * Fills the [MutableVec]s with the semi axes of this [Parallelogram]. For definition please see
     * [computeSemiAxes] above.
     */
    public fun computeSemiAxes(outAxis1: MutableVec, outAxis2: MutableVec) {
        ParallelogramNative.populateSemiAxes(
            center.x,
            center.y,
            width,
            height,
            rotationDegrees,
            skew,
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
     * their numbering through any skew and/or rotation applied to the base rectangle. Numerically,
     * the corners are equivalent to: `C - u - v C + u - v C + u + v C - u + v` Where `C` =
     * [center], and `u` and `v` are the [semiAxes].
     *
     * Performance-sensitive code should use the [computeCorners] overload that takes pre-allocated
     * [MutableVec]s, so that instances can be reused across multiple calls.
     */
    public fun computeCorners(): List<ImmutableVec> {
        return ParallelogramNative.createCorners(
                center.x,
                center.y,
                width,
                height,
                rotationDegrees,
                skew,
            )
            .toList()
    }

    /**
     * Populates the 4 output points with the corners of the [Parallelogram].
     *
     * For explanation of order, please see [computeCorners] above.
     */
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
            rotationDegrees,
            skew,
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
    public operator fun contains(point: ImmutableVec): Boolean {
        return ParallelogramNative.contains(
            center.x,
            center.y,
            width,
            height,
            rotationDegrees,
            skew,
            point.x,
            point.y,
        )
    }

    /**
     * Compares this [Parallelogram] with [other], and returns true if both [center] points are
     * considered almost equal with the given [tolerance], and the difference between [width] and
     * `other.width` is less than [tolerance], and likewise for [height], [rotation], and [skew].
     */
    public fun isAlmostEqual(
        other: Parallelogram,
        @FloatRange(from = 0.0) tolerance: Float,
    ): Boolean =
        this === other ||
            (abs(center.x - other.center.x) < tolerance &&
                abs(center.y - other.center.y) < tolerance &&
                abs(width - other.width) < tolerance &&
                abs(height - other.height) < tolerance &&
                abs(rotationDegrees - other.rotationDegrees) < tolerance &&
                abs(skew - other.skew) < tolerance)

    public companion object {
        /**
         * If [width] is less than zero or if [rotationDegrees] is not in
         * [0, 360), the [Parallelogram] will be normalized and the normalized values of width,
         * height, and rotation will be used to call [runBlock].
         */
        internal inline fun <T> normalizeAndRun(
            width: Float,
            height: Float,
            @AngleDegreesFloat rotationDegrees: Float,
            runBlock: (w: Float, h: Float, rDegrees: Float) -> T,
        ): T {
            return if (width < 0) {
                runBlock(
                    -width,
                    -height,
                    Angle.normalizedDegrees(rotationDegrees + Angle.HALF_TURN_DEGREES),
                )
            } else {
                runBlock(width, height, Angle.normalizedDegrees(rotationDegrees))
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
                first.rotationDegrees == second.rotationDegrees &&
                first.skew == second.skew

        /** Returns a hash code for [parallelogram] using its [Parallelogram] properties. */
        internal fun hash(parallelogram: Parallelogram): Int {
            var result = parallelogram.center.hashCode()
            result = 31 * result + parallelogram.width.hashCode()
            result = 31 * result + parallelogram.height.hashCode()
            result = 31 * result + parallelogram.rotationDegrees.hashCode()
            result = 31 * result + parallelogram.skew.hashCode()
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
                    "rotationDegrees=$rotationDegrees, " +
                    "skew=$skew)"
            }
    }
}

/** Native helper functions for Parallelogram. */
@UsedByNative
internal object ParallelogramNative {

    init {
        NativeLoader.load()
    }

    @UsedByNative
    external fun createBoundingBox(
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float,
        rotationDegrees: Float,
        skew: Float,
    ): ImmutableBox

    @UsedByNative
    external fun populateBoundingBox(
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float,
        rotationDegrees: Float,
        skew: Float,
        outBox: MutableBox,
    )

    @UsedByNative
    external fun createSemiAxes(
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float,
        rotationDegrees: Float,
        skew: Float,
    ): Array<ImmutableVec>

    @UsedByNative
    external fun populateSemiAxes(
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float,
        rotationDegrees: Float,
        skew: Float,
        outAxis1: MutableVec,
        outAxis2: MutableVec,
    )

    @UsedByNative
    external fun createCorners(
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float,
        rotationDegrees: Float,
        skew: Float,
    ): Array<ImmutableVec>

    @UsedByNative
    external fun populateCorners(
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float,
        rotationDegrees: Float,
        skew: Float,
        outCorner1: MutableVec,
        outCorner2: MutableVec,
        outCorner3: MutableVec,
        outCorner4: MutableVec,
    )

    @UsedByNative
    external fun contains(
        centerX: Float,
        centerY: Float,
        width: Float,
        height: Float,
        rotationDegrees: Float,
        skew: Float,
        pointX: Float,
        pointY: Float,
    ): Boolean
}
