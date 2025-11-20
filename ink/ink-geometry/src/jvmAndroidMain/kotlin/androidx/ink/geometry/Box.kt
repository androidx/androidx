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
 * Represents an axis-aligned rectangle. See [ImmutableBox] and [MutableBox] for concrete classes
 * implementing [Box].
 *
 * The [Box] interface is the read-only view of the underlying data which may or may not be mutable.
 */
public abstract class Box internal constructor() {
    /** The lower bound in the `X` direction. */
    public abstract val xMin: Float

    /** The lower bound in the `Y` direction. */
    public abstract val yMin: Float

    /** The upper bound in the `X` direction. */
    public abstract val xMax: Float

    /** The upper bound in the `Y` direction. */
    public abstract val yMax: Float

    /** The width of the rectangle. This can never be negative. */
    public val width: Float
        @FloatRange(from = 0.0) get() = xMax - xMin

    /** The height of the rectangle. This can never be negative. */
    public val height: Float
        @FloatRange(from = 0.0) get() = yMax - yMin

    /**
     * Returns the center of the [Box].
     *
     * Performance-sensitive code should use the [computeCenter] overload that takes a pre-allocated
     * [MutableVec], so that instance can be reused across multiple calls.
     */
    public fun computeCenter(): ImmutableVec {
        return BoxNative.createCenter(xMin, yMin, xMax, yMax)
    }

    /** Populates [outVec] with the center of the [Box], and returns [outVec]. */
    public fun computeCenter(outVec: MutableVec): MutableVec {
        BoxNative.populateCenter(xMin, yMin, xMax, yMax, outVec)
        return outVec
    }

    /**
     * Returns a list containing the 4 corners of the [Box]. The order of the corners is: (x_min,
     * y_min), (x_max, y_min), (x_max, y_max), (x_min, y_max).
     */
    public fun computeCorners(): List<ImmutableVec> =
        listOf(
            ImmutableVec(xMin, yMin),
            ImmutableVec(xMax, yMin),
            ImmutableVec(xMax, yMax),
            ImmutableVec(xMin, yMax),
        )

    /**
     * Populates the 4 output points with the corners of the [Box]. The order of the corners is:
     * (x_min, y_min), (x_max, y_min), (x_max, y_max), (x_min, y_max)
     */
    public fun computeCorners(
        outVecXMinYMin: MutableVec,
        outVecXMaxYMin: MutableVec,
        outVecXMaxYMax: MutableVec,
        outVecXMinYMax: MutableVec,
    ) {
        outVecXMinYMin.x = xMin
        outVecXMinYMin.y = yMin
        outVecXMaxYMin.x = xMax
        outVecXMaxYMin.y = yMin
        outVecXMaxYMax.x = xMax
        outVecXMaxYMax.y = yMax
        outVecXMinYMax.x = xMin
        outVecXMinYMax.y = yMax
    }

    /**
     * Returns whether the given point is contained within the Box. Points that lie exactly on the
     * Box's boundary are considered to be contained.
     */
    public operator fun contains(point: Vec): Boolean =
        BoxNative.containsPoint(xMin, yMin, xMax, yMax, point.x, point.y)

    /**
     * Returns whether the other Box is contained within this Box. Edges of the other Box that
     * overlap with this one's boundary are considered to be contained.
     */
    public operator fun contains(otherBox: Box): Boolean =
        BoxNative.containsBox(
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
     * Returns an immutable copy of this object. This will return itself if called on an immutable
     * instance.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public abstract fun toImmutable(): ImmutableBox

    /**
     * Compares this [Box] with [other], and returns true if the difference between [xMin] and
     * [other.xMin] is less than [tolerance], and likewise for [xMax], [yMin], and [yMax].
     */
    public fun isAlmostEqual(other: Box, @FloatRange(from = 0.0) tolerance: Float): Boolean =
        this === other ||
            (abs(xMin - other.xMin) < tolerance &&
                abs(yMin - other.yMin) < tolerance &&
                abs(xMax - other.xMax) < tolerance &&
                abs(yMax - other.yMax) < tolerance)

    internal companion object {
        /**
         * Returns true if [first] and [second] have the same values for all properties of [Box].
         */
        fun areEquivalent(first: Box, second: Box): Boolean =
            first.xMin == second.xMin &&
                first.yMin == second.yMin &&
                first.xMax == second.xMax &&
                first.yMax == second.yMax

        /** Returns a hash code for [box] using its [Box] properties. */
        // NOMUTANTS -- not testing exact hashCode values, just that equality implies same hashCode
        fun hash(box: Box): Int =
            box.run {
                var result = xMin.hashCode()
                result = 31 * result + yMin.hashCode()
                result = 31 * result + xMax.hashCode()
                result = 31 * result + yMax.hashCode()
                return result
            }

        /** Returns a string representation for [box] using its [Box] properties. */
        fun string(box: Box): String =
            box.run { "Box(xMin=$xMin, yMin=$yMin, xMax=$xMax, yMax=$yMax)" }
    }
}

@UsedByNative
internal object BoxNative {

    init {
        NativeLoader.load()
    }

    @UsedByNative
    external fun createCenter(
        rectXMin: Float,
        rectYMin: Float,
        rectXMax: Float,
        rectYMax: Float,
    ): ImmutableVec

    @UsedByNative
    external fun populateCenter(
        rectXMin: Float,
        rectYMin: Float,
        rectXMax: Float,
        rectYMax: Float,
        out: MutableVec,
    )

    @UsedByNative
    external fun containsPoint(
        rectXMin: Float,
        rectYMin: Float,
        rectXMax: Float,
        rectYMax: Float,
        pointX: Float,
        pointY: Float,
    ): Boolean

    @UsedByNative
    external fun containsBox(
        rectXMin: Float,
        rectYMin: Float,
        rectXMax: Float,
        rectYMax: Float,
        otherXMin: Float,
        otherYMin: Float,
        otherXMax: Float,
        otherYMax: Float,
    ): Boolean
}
