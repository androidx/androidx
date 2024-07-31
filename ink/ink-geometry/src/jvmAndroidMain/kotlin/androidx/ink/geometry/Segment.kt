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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
public interface Segment {
    public val start: Vec
    public val end: Vec

    /** The length of the [Segment]. */
    public val length: Float
        @FloatRange(from = 0.0) get() = hypot(start.x - end.x, start.y - end.y)

    /**
     * Returns an ImmutableVec with the displacement from start to end. This is equivalent to
     * subtract(end, start, output).
     */
    public val vec: ImmutableVec

    /**
     * Populates [output] with the displacement from start to end. This is equivalent to
     * subtract(end, start, output).
     */
    public fun populateVec(output: MutableVec) {
        output.x = end.x - start.x
        output.y = end.y - start.y
    }

    /** Returns an [ImmutableVec] that lies halfway along the segment. */
    public val midpoint: ImmutableVec

    /** Populates [output] with the point halfway along the segment. */
    public fun populateMidpoint(output: MutableVec) {
        output.x = (start.x + end.x) / 2
        output.y = (start.y + end.y) / 2
    }

    /** Returns the minimum bounding box containing the [Segment]. */
    public val boundingBox: ImmutableBox
        get() = run {
            // TODO(b/354236964): Optimize unnecessary allocations
            val (minX, maxX, minY, maxY) = getBoundingXYCoordinates(this)
            ImmutableBox.fromTwoPoints(ImmutablePoint(minX, minY), ImmutablePoint(maxX, maxY))
        }

    /** Populates [output] with the minimum bounding box containing the [Segment]. */
    public fun populateBoundingBox(output: MutableBox) {
        // TODO(b/354236964): Optimize unnecessary allocations
        val (minX, maxX, minY, maxY) = getBoundingXYCoordinates(this)
        output.setXBounds(minX, maxX)
        output.setYBounds(minY, maxY)
    }

    /**
     * Returns the point on the segment at the given ratio of the segment's length, measured from
     * the start point. You may also think of this as linearly interpolating from the start of the
     * segment to the end. Values outside the interval [0, 1] will be extrapolated along the
     * infinite line passing through this segment. This is the inverse of [project].
     */
    public fun lerpPoint(ratio: Float): ImmutableVec =
        ImmutableVec(
            (1.0f - ratio) * start.x + ratio * end.x,
            (1.0f - ratio) * start.y + ratio * end.y
        )

    /**
     * Fills [output] with the point on the segment at the given ratio of the segment's length,
     * measured from the start point. You may also think of this as linearly interpolating from the
     * start of the segment to the end. Values outside the interval [0, 1] will be extrapolated
     * along the infinite line passing through this segment. This is the inverse of [project].
     */
    public fun populateLerpPoint(ratio: Float, output: MutableVec) {
        output.x = (1.0f - ratio) * start.x + ratio * end.x
        output.y = (1.0f - ratio) * start.y + ratio * end.y
    }

    /**
     * Returns the multiple of the segment's length at which the infinite extrapolation of this
     * segment is closest to [pointToProject]. This is the inverse of [populateLerpPoint]. If the
     * [length] of this segment is zero, then the projection is undefined and this will throw an
     * error. Note that the [length] may be zero even if [start] and [end] are not equal, if they
     * are sufficiently close that floating-point underflow occurs.
     */
    public fun project(pointToProject: Vec): Float {
        // TODO(b/354236964): Optimize unnecessary allocations
        if (Vec.areEquivalent(start, end)) {
            throw IllegalArgumentException("Projecting onto a segment of zero length is undefined.")
        }
        // Sometimes start is not exactly equal to the end, but close enough that the
        // magnitude-squared still is not positive due to floating-point
        // loss-of-precision.
        val magnitudeSquared = vec.magnitudeSquared
        if (magnitudeSquared <= 0) {
            throw IllegalArgumentException("Projecting onto a segment of zero length is undefined.")
        }
        val temp = MutableVec()
        Vec.subtract(pointToProject, start, temp)
        return Vec.dotProduct(temp, vec) / magnitudeSquared
    }

    /**
     * Returns an immutable copy of this object. This will return itself if called on an immutable
     * instance.
     */
    public fun asImmutable(): ImmutableSegment

    /**
     * Returns an [ImmutableSegment] with some or all of its values taken from `this`. For each
     * value, the returned [ImmutableSegment] will use the given value; if no value is given, it
     * will instead be set to the value on `this`. If `this` is an [ImmutableSegment], and the
     * result would be an identical [ImmutableSegment], then `this` is returned. This occurs when
     * either no values are given, or when all given values are structurally equal to the values in
     * `this`.
     */
    @JvmSynthetic
    public fun asImmutable(start: Vec = this.start, end: Vec = this.end): ImmutableSegment

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
