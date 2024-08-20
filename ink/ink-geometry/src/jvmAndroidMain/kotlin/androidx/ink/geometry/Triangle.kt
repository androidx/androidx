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

/**
 * A triangle defined by its three corners [p0], [p1] and [p2] in order. This is a read-only
 * interface that has mutable and immutable implementations. See [MutableTriangle] and
 * [ImmutableTriangle].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
public interface Triangle {

    /** The three points that define the [Triangle]. */
    public val p0: Vec
    public val p1: Vec
    public val p2: Vec

    /**
     * The signed area of the Triangle. If the Triangle's points wind in a positive direction (as
     * defined by [Angle]), then the Triangle's area will be positive. Otherwise, it will be
     * negative.
     */
    public val signedArea: Float
        get() = run {
            // TODO(b/354236964): Optimize unnecessary allocations
            val p1MinusP0 = MutableVec()
            val p2MinusP1 = MutableVec()
            Vec.subtract(p1, p0, p1MinusP0)
            Vec.subtract(p2, p1, p2MinusP1)
            return 0.5f * Vec.determinant(p1MinusP0, p2MinusP1)
        }

    /** Returns the minimum bounding box containing the [Triangle]. */
    public val boundingBox: ImmutableBox
        get() = run {
            // TODO(b/354236964): Optimize unnecessary allocations
            val (minX, maxX, minY, maxY) = getBoundingXYCoordinates(this)
            ImmutableBox.fromTwoPoints(ImmutableVec(minX, minY), ImmutableVec(maxX, maxY))
        }

    /** Populates [output] with the minimum bounding box containing the [Triangle]. */
    public fun populateBoundingBox(output: MutableBox) {
        // TODO(b/354236964): Optimize unnecessary allocations
        val (minX, maxX, minY, maxY) = getBoundingXYCoordinates(this)
        output.setXBounds(minX, maxX)
        output.setYBounds(minY, maxY)
    }

    /**
     * Returns true if the given point is contained within the Triangle. Points that lie exactly on
     * the Triangle's boundary are considered to be contained.
     */
    public operator fun contains(point: Vec): Boolean =
        TriangleNative.nativeContains(
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
    public fun edge(@IntRange(from = 0, to = 2) index: Int): ImmutableSegment {
        val modIndex = index % 3
        return when (modIndex) {
            0 -> ImmutableSegment(p0, p1)
            1 -> ImmutableSegment(p1, p2)
            2 -> ImmutableSegment(p2, p0)
            else -> throw IllegalArgumentException("Invalid index: $index")
        }
    }

    /**
     * Fills [output] with the segment of the Triangle between the point at [index] and the point at
     * [index] + 1 modulo 3.
     */
    public fun populateEdge(@IntRange(from = 0, to = 2) index: Int, output: MutableSegment) {
        val modIndex = index % 3
        when (modIndex) {
            0 -> {
                output.start(p0)
                output.end(p1)
            }
            1 -> {
                output.start(p1)
                output.end(p2)
            }
            2 -> {
                output.start(p2)
                output.end(p0)
            }
            else -> throw IllegalArgumentException("Invalid index: $index")
        }
    }

    /**
     * Returns an immutable copy of [this] object. This will return itself if called on an immutable
     * instance.
     */
    public fun asImmutable(): ImmutableTriangle

    /**
     * Returns an [ImmutableTriangle] with some or all of its values taken from `this`. For each
     * value, the returned [ImmutableTriangle] will use the given value; if no value is given, it
     * will instead be set to the value on `this`. If `this` is an [ImmutableTriangle], and the
     * result would be an identical [ImmutableTriangle], then `this` is returned. This occurs when
     * either no values are given, or when all given values are structurally equal to the values in
     * `this`.
     */
    @JvmSynthetic
    public fun asImmutable(
        p0: Vec = this.p0,
        p1: Vec = this.p1,
        p2: Vec = this.p2
    ): ImmutableTriangle

    public fun isAlmostEqual(other: Triangle, @FloatRange(from = 0.0) tolerance: Float): Boolean =
        p0.isAlmostEqual(other.p0, tolerance) &&
            p1.isAlmostEqual(other.p1, tolerance) &&
            p2.isAlmostEqual(other.p2, tolerance)

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

        /**
         * Returns the minimum and maximum x and y coordinates for all points inside [triangle].
         *
         * This function returns four float corresponding to the (minX, maxX, minY, maxY)
         * coordinates of the triangle. These coordinates are used to compute the bounding rectangle
         * of [triangle].
         */
        private fun getBoundingXYCoordinates(triangle: Triangle) =
            arrayOf(
                minOf(triangle.p0.x, triangle.p1.x, triangle.p2.x),
                maxOf(triangle.p0.x, triangle.p1.x, triangle.p2.x),
                minOf(triangle.p0.y, triangle.p1.y, triangle.p2.y),
                maxOf(triangle.p0.y, triangle.p1.y, triangle.p2.y),
            )
    }
}

/** Helper object to contain native JNI calls */
private object TriangleNative {

    init {
        NativeLoader.load()
    }

    /**
     * Helper method to construct a native C++ [Triangle] and [Point], check if the native
     * [Triangle] contains the native [Point], and return the result.
     */
    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    external fun nativeContains(
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
