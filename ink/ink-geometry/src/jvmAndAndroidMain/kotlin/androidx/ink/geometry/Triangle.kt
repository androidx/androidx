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
import androidx.annotation.IntRange
import androidx.annotation.RestrictTo
import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative

/**
 * A triangle defined by its three corners [p0], [p1] and [p2]. The order of these points matter - a
 * triangle with [p0, p1, p2] is not the same as the permuted [p1, p0, p2], or even the rotated
 * [p2, p0, p1].
 *
 * A [Triangle] may be degenerate, meaning it is constructed with its 3 points colinear. One way
 * that a [Triangle] may be degenerate is if two or three of its points are at the same location
 * (coincident).
 *
 * This is a read-only interface that has mutable and immutable implementations. See
 * [MutableTriangle] and [ImmutableTriangle].
 */
public abstract class Triangle internal constructor() {

    /** One of the three points that define the [Triangle]. */
    public abstract val p0: Vec

    /** One of the three points that define the [Triangle]. */
    public abstract val p1: Vec

    /** One of the three points that define the [Triangle]. */
    public abstract val p2: Vec

    /**
     * Return the signed area of the [Triangle]. If the [Triangle] is degenerate, meaning its 3
     * points are all colinear, then the result will be zero. If its points wind in a positive
     * direction (as defined by [Angle]), then the result will be positive. Otherwise, it will be
     * negative.
     */
    public fun computeSignedArea(): Float {
        val p1MinusP0X = p1.x - p0.x
        val p1MinusP0Y = p1.y - p0.y
        val p2MinusP1X = p2.x - p1.x
        val p2MinusP1Y = p2.y - p1.y
        return 0.5f * Vec.determinant(p1MinusP0X, p1MinusP0Y, p2MinusP1X, p2MinusP1Y)
    }

    /** Returns the minimum bounding box containing the [Triangle]. */
    public fun computeBoundingBox(): ImmutableBox = withXAndYBounds { minX, maxX, minY, maxY ->
        ImmutableBox.fromTwoPoints(minX, minY, maxX, maxY)
    }

    /**
     * Populates [outBox] with the minimum bounding box containing the [Triangle] and returns
     * [outBox].
     */
    public fun computeBoundingBox(outBox: MutableBox): MutableBox =
        withXAndYBounds { minX, maxX, minY, maxY ->
            outBox.setXBounds(minX, maxX)
            outBox.setYBounds(minY, maxY)
        }

    /**
     * Returns true if the given point is contained within the Triangle. Points that lie exactly on
     * the Triangle's boundary are considered to be contained.
     */
    public operator fun contains(point: Vec): Boolean =
        TriangleNative.contains(
            triangleP0X = p0.x,
            triangleP0Y = p0.y,
            triangleP1X = p1.x,
            triangleP1Y = p1.y,
            triangleP2X = p2.x,
            triangleP2Y = p2.y,
            pointX = point.x,
            pointY = point.y,
        )

    /**
     * Returns the segment of the Triangle between the point at [index] and the point at [index] + 1
     * modulo 3.
     */
    public fun computeEdge(@IntRange(from = 0, to = 2) index: Int): ImmutableSegment =
        ImmutableSegment(getPoint(index), getPoint(index + 1))

    /**
     * Fills [outSegment] with the segment of the Triangle between the point at [index] and the
     * point at [index] + 1 modulo 3. Returns [outSegment].
     */
    public fun computeEdge(
        @IntRange(from = 0, to = 2) index: Int,
        outSegment: MutableSegment,
    ): MutableSegment {
        outSegment.start.populateFrom(getPoint(index))
        outSegment.end.populateFrom(getPoint(index + 1))
        return outSegment
    }

    /**
     * Returns an immutable copy of this object. This will return itself if called on an immutable
     * instance.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public abstract fun toImmutable(): ImmutableTriangle

    public fun isAlmostEqual(other: Triangle, @FloatRange(from = 0.0) tolerance: Float): Boolean =
        this === other ||
            (p0.isAlmostEqual(other.p0, tolerance) &&
                p1.isAlmostEqual(other.p1, tolerance) &&
                p2.isAlmostEqual(other.p2, tolerance))

    /** Returns the point at [index] modulo 3. */
    private fun getPoint(index: Int) =
        when (index % 3) {
            0 -> p0
            1 -> p1
            2 -> p2
            else -> throw IllegalStateException("Should not arrive here when checking index % 3")
        }

    /**
     * Calls the provided callback with four floats corresponding to the (minX, maxX, minY, maxY)
     * bounds of the triangle. These bounds are used to compute the bounding rectangle of
     * [triangle].
     */
    private inline fun <R> withXAndYBounds(callback: (Float, Float, Float, Float) -> R) =
        callback(
            minOf(p0.x, p1.x, p2.x),
            maxOf(p0.x, p1.x, p2.x),
            minOf(p0.y, p1.y, p2.y),
            maxOf(p0.y, p1.y, p2.y),
        )

    public companion object {
        /**
         * Returns true if [first] and [second] have the same values for all properties of
         * [Triangle].
         */
        internal fun areEquivalent(first: Triangle, second: Triangle): Boolean =
            Vec.areEquivalent(first.p0, second.p0) &&
                Vec.areEquivalent(first.p1, second.p1) &&
                Vec.areEquivalent(first.p2, second.p2)

        /** Returns a hash code for [triangle] using its [Triangle] properties. */
        internal fun hash(triangle: Triangle): Int =
            triangle.run {
                31 * p0.x.hashCode() +
                    p0.y.hashCode() +
                    31 * p1.x.hashCode() +
                    p1.y.hashCode() +
                    31 * p2.x.hashCode() +
                    p2.y.hashCode()
            }

        /** Returns a string representation for [triangle] using its [Triangle] properties. */
        internal fun string(triangle: Triangle): String =
            triangle.run { "Triangle(p0=$p0, p1=$p1, p2=$p2)" }
    }
}

/** Helper object to contain native JNI calls. */
@UsedByNative
private object TriangleNative {

    init {
        NativeLoader.load()
    }

    /** Helper method to check if a native `ink::Triangle` contains the native `ink::Point`. */
    @UsedByNative
    external fun contains(
        triangleP0X: Float,
        triangleP0Y: Float,
        triangleP1X: Float,
        triangleP1Y: Float,
        triangleP2X: Float,
        triangleP2Y: Float,
        pointX: Float,
        pointY: Float,
    ): Boolean
}
