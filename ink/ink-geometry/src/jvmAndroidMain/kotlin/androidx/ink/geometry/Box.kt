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
 * Represents an axis-aligned rectangle. See [ImmutableBox] and [MutableBox] for concrete classes
 * implementing [Box].
 *
 * The [Box] interface is the read-only view of the underlying data which may or may not be mutable.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
public interface Box {
    /** The lower bound in the `X` direction. */
    public val xMin: Float

    /** The lower bound in the `Y` direction. */
    public val yMin: Float

    /** The upper bound in the `X` direction. */
    public val xMax: Float

    /** The upper bound in the `Y` direction. */
    public val yMax: Float

    /** The width of the rectangle. This can never be negative. */
    public val width: Float
        @FloatRange(from = 0.0) get() = xMax - xMin

    /** The height of the rectangle. This can never be negative. */
    public val height: Float
        @FloatRange(from = 0.0) get() = yMax - yMin

    /** Populates [out] with the center of the [Box]. */
    public fun center(out: MutablePoint)

    /**
     * Populates the 4 [output] points with the corners of the [Box]. The order of the corners is:
     * (x_min, y_min), (x_max, y_min), (x_max, y_max), (x_min, y_max)
     */
    public fun corners(
        outputXMinYMin: MutablePoint,
        outputXMaxYMin: MutablePoint,
        outputXMaxYMax: MutablePoint,
        outputXMinYMax: MutablePoint,
    ) {
        outputXMinYMin.x = xMin
        outputXMinYMin.y = yMin
        outputXMaxYMin.x = xMax
        outputXMaxYMin.y = yMin
        outputXMaxYMax.x = xMax
        outputXMaxYMax.y = yMax
        outputXMinYMax.x = xMin
        outputXMinYMax.y = yMax
    }

    /**
     * Returns whether the given point is contained within the Box. Points that lie exactly on the
     * Box's boundary are considered to be contained.
     */
    public operator fun contains(point: Point): Boolean =
        BoxHelper.nativeContainsPoint(xMin, yMin, xMax, yMax, point.x, point.y)

    /**
     * Returns whether the other Box is contained within this Box. Edges of the other Box that
     * overlap with this one's boundary are considered to be contained.
     */
    public operator fun contains(otherBox: Box): Boolean =
        BoxHelper.nativeContainsBox(
            xMin,
            yMin,
            xMax,
            yMax,
            otherBox.xMin,
            otherBox.yMin,
            otherBox.xMax,
            otherBox.yMax,
        )

    /**
     * Compares this [Box] with [other], and returns true if the difference between [xMin] and
     * [other.xMin] is less than [tolerance], and likewise for [xMax], [yMin], and [yMax].
     */
    public fun isAlmostEqual(other: Box, @FloatRange(from = 0.0) tolerance: Float): Boolean =
        (abs(xMin - other.xMin) < tolerance) &&
            (abs(yMin - other.yMin) < tolerance) &&
            (abs(xMax - other.xMax) < tolerance) &&
            (abs(yMax - other.yMax) < tolerance)

    public companion object {
        /**
         * Returns true if [first] and [second] have the same values for all properties of [Box].
         */
        internal fun areEquivalent(first: Box, second: Box): Boolean =
            first.xMin == second.xMin &&
                first.yMin == second.yMin &&
                first.xMax == second.xMax &&
                first.yMax == second.yMax

        /** Returns a hash code for [box] using its [Box] properties. */
        // NOMUTANTS -- not testing exact hashCode values, just that equality implies same hashCode
        internal fun hash(box: Box): Int =
            box.run {
                var result = xMin.hashCode()
                result = 31 * result + yMin.hashCode()
                result = 31 * result + xMax.hashCode()
                result = 31 * result + yMax.hashCode()
                return result
            }

        /** Returns a string representation for [box] using its [Box] properties. */
        internal fun string(box: Box): String =
            box.run { "Box(xMin=$xMin, yMin=$yMin, xMax=$xMax, yMax=$yMax)" }
    }
}
