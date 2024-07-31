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
import kotlin.math.hypot

/** Represents a directed line segment between two points. */
public abstract class Segment internal constructor() {
    public abstract val start: Vec
    public abstract val end: Vec

    /** The length of the [Segment]. */
    @FloatRange(from = 0.0)
    public fun computeLength(): Float = hypot(start.x - end.x, start.y - end.y)

    /**
     * Returns an ImmutableVec with the displacement from start to end. This is equivalent to
     * `subtract(end, start, output)`.
     *
     * For performance-sensitive code, prefer to use [computeDisplacement] with a pre-allocated
     * instance of [MutableVec].
     */
    public fun computeDisplacement(): ImmutableVec = ImmutableVec(end.x - start.x, end.y - start.y)

    /**
     * Populates [outVec] with the displacement from start to end. This is equivalent to
     * `subtract(end, start, output)`. Returns [outVec].
     */
    public fun computeDisplacement(outVec: MutableVec): MutableVec {
        outVec.x = end.x - start.x
        outVec.y = end.y - start.y
        return outVec
    }

    /**
     * Returns an [ImmutableVec] that lies halfway along the segment.
     *
     * For performance-sensitive code, prefer to use [computeMidpoint] with a pre-allocated instance
     * of [MutableVec].
     */
    public fun computeMidpoint(): ImmutableVec =
        ImmutableVec((start.x + end.x) / 2, (start.y + end.y) / 2)

    /** Populates [outVec] with the point halfway along the segment. */
    public fun computeMidpoint(outVec: MutableVec): MutableVec {
        outVec.x = (start.x + end.x) / 2
        outVec.y = (start.y + end.y) / 2
        return outVec
    }

    /**
     * Returns the minimum bounding box containing the [Segment].
     *
     * For performance-sensitive code, prefer to use [computeBoundingBox] with a pre-allocated
     * instance of [MutableBox].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
    public fun computeBoundingBox(): ImmutableBox {
        // TODO(b/354236964): Optimize unnecessary allocations
        val (minX, maxX, minY, maxY) = getBoundingXYCoordinates(this)
        return ImmutableBox.fromTwoPoints(ImmutableVec(minX, minY), ImmutableVec(maxX, maxY))
    }

    /** Populates [outBox] with the minimum bounding box containing the [Segment]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
    public fun computeBoundingBox(outBox: MutableBox): MutableBox {
        // TODO(b/354236964): Optimize unnecessary allocations
        val (minX, maxX, minY, maxY) = getBoundingXYCoordinates(this)
        outBox.setXBounds(minX, maxX)
        outBox.setYBounds(minY, maxY)
        return outBox
    }

    /**
     * Returns the point on the segment at the given ratio of the segment's length, measured from
     * the start point. You may also think of this as linearly interpolating from the start of the
     * segment to the end. Values outside the interval [0, 1] will be extrapolated along the
     * infinite line passing through this segment. This is the inverse of [project].
     *
     * For performance-sensitive code, prefer to use [computeLerpPoint] with a pre-allocated
     * instance of [MutableVec].
     */
    public fun computeLerpPoint(ratio: Float): ImmutableVec =
        ImmutableVec(
            (1.0f - ratio) * start.x + ratio * end.x,
            (1.0f - ratio) * start.y + ratio * end.y
        )

    /**
     * Fills [outVec] with the point on the segment at the given ratio of the segment's length,
     * measured from the start point. You may also think of this as linearly interpolating from the
     * start of the segment to the end. Values outside the interval [0, 1] will be extrapolated
     * along the infinite line passing through this segment. This is the inverse of [project].
     */
    public fun computeLerpPoint(ratio: Float, outVec: MutableVec): MutableVec {
        outVec.x = (1.0f - ratio) * start.x + ratio * end.x
        outVec.y = (1.0f - ratio) * start.y + ratio * end.y
        return outVec
    }

    /**
     * Returns the multiple of the segment's length at which the infinite extrapolation of this
     * segment is closest to [pointToProject]. This is the inverse of [computeLerpPoint]. If the
     * [computeLength] of this segment is zero, then the projection is undefined and this will throw
     * an error. Note that the [computeLength] may be zero even if [start] and [end] are not equal,
     * if they are sufficiently close that floating-point underflow occurs.
     */
    public fun project(pointToProject: Vec): Float {
        // TODO(b/354236964): Optimize unnecessary allocations
        if (Vec.areEquivalent(start, end)) {
            throw IllegalArgumentException("Projecting onto a segment of zero length is undefined.")
        }
        // Sometimes start is not exactly equal to the end, but close enough that the
        // magnitude-squared still is not positive due to floating-point
        // loss-of-precision.
        val magnitudeSquared = computeDisplacement().computeMagnitudeSquared()
        if (magnitudeSquared <= 0) {
            throw IllegalArgumentException("Projecting onto a segment of zero length is undefined.")
        }
        val temp = MutableVec()
        Vec.subtract(pointToProject, start, temp)
        return Vec.dotProduct(temp, computeDisplacement()) / magnitudeSquared
    }

    /**
     * Returns an immutable copy of this object. This will return itself if called on an immutable
     * instance.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public abstract fun asImmutable(): ImmutableSegment

    /**
     * Compares this [Segment] with [other], and returns true if both [start] points are considered
     * almost equal with the given [tolerance], and likewise for both [end] points.
     */
    public fun isAlmostEqual(other: Segment, @FloatRange(from = 0.0) tolerance: Float): Boolean =
        start.isAlmostEqual(other.start, tolerance) && end.isAlmostEqual(other.end, tolerance)

    public companion object {
        /**
         * Returns true if [first] and [second] have the same values for all properties of
         * [Segment].
         */
        internal fun areEquivalent(first: Segment, second: Segment): Boolean {
            return Vec.areEquivalent(first.start, second.start) &&
                Vec.areEquivalent(first.end, second.end)
        }

        /** Returns a hash code for [segment] using its [Segment] properties. */
        internal fun hash(segment: Segment): Int =
            31 * segment.start.hashCode() + segment.end.hashCode()

        /** Returns a string representation for [segment] using its [Segment] properties. */
        internal fun string(segment: Segment): String =
            "Segment(start=${segment.start}, end=${segment.end})"

        /**
         * Returns the minimum and maximum x and y coordinates for all points inside [segment].
         *
         * This function returns four floats corresponding to the (minX, maxX, minY, maxY)
         * coordinates of the segment. These coordinates are used to compute the bounding rectangle
         * of [segment].
         */
        private fun getBoundingXYCoordinates(segment: Segment) =
            arrayOf(
                minOf(segment.start.x, segment.end.x),
                maxOf(segment.start.x, segment.end.x),
                minOf(segment.start.y, segment.end.y),
                maxOf(segment.start.y, segment.end.y),
            )
    }
}
