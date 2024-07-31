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
 * Represents a location in 2-dimensional space. See [ImmutablePoint] and [MutablePoint] for
 * concrete classes implementing [Point].
 *
 * The [Point] interface is the read-only view of the underlying data which may or may not be
 * mutable. Use the following concrete classes depending on the application requirement:
 *
 * For the [ImmutablePoint], the underlying data like the [x] and [y] coordinates is set once during
 * construction and does not change afterwards. Use this class for a simple [Point] that is
 * inherently thread-safe because of its immutability. A different value of an immutable object can
 * only be obtained by allocating a new one, and allocations can be expensive due to the risk of
 * garbage collection.
 *
 * For the [MutablePoint], the underlying data might change (e.g. by writing the [x] property). Use
 * this class to hold transient data in a performance critical situation, such as the input or
 * render path --- allocate the underlying [MutablePoint] once, perform operations on it and
 * overwrite it with new data.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // PublicApiNotReadyForJetpackReview
public interface Point {
    /** The x-coordinate of the [Point] */
    public val x: Float

    /** The y-coordinate of the [Point] */
    public val y: Float

    /** Fills the x and y values of [output] with the x and y coordinates of this [Point] */
    public fun getVec(output: MutableVec) {
        output.x = this.x
        output.y = this.y
    }

    /**
     * Compares this [Point] with [other], and returns true if the difference between [x] and
     * [other.x] is less than [tolerance], and likewise for [y].
     */
    public fun isAlmostEqual(
        other: Point,
        @FloatRange(from = 0.0) tolerance: Float = 0.0001f,
    ): Boolean = (abs(x - other.x) < tolerance) && (abs(y - other.y) < tolerance)

    public companion object {
        /**
         * Adds the x and y values of [lhs] to the x and y values of [rhs] and stores the result in
         * [output].
         */
        @JvmStatic
        public fun add(lhs: Point, rhs: Vec, output: MutablePoint) {
            output.x = lhs.x + rhs.x
            output.y = lhs.y + rhs.y
        }

        /**
         * Adds the x and y values of [lhs] to the x and y values of [rhs] and stores the result in
         * [output].
         */
        @JvmStatic
        public fun add(lhs: Vec, rhs: Point, output: MutablePoint) {
            output.x = lhs.x + rhs.x
            output.y = lhs.y + rhs.y
        }

        /**
         * Subtracts the x and y values of [rhs] from the x and y values of [lhs] and stores the
         * result in [output].
         */
        @JvmStatic
        public fun subtract(lhs: Point, rhs: Vec, output: MutablePoint) {
            output.x = lhs.x - rhs.x
            output.y = lhs.y - rhs.y
        }

        /**
         * Subtracts the x and y values of [rhs] from the x and y values of [lhs] and stores the
         * result in [output].
         */
        @JvmStatic
        public fun subtract(lhs: Point, rhs: Point, output: MutableVec) {
            output.x = lhs.x - rhs.x
            output.y = lhs.y - rhs.y
        }

        /**
         * Returns true if [first] and [second] have the same values for all properties of [Point].
         */
        internal fun areEquivalent(first: Point, second: Point): Boolean {
            return first.x == second.x && first.y == second.y
        }

        /** Returns a hash code for [point] using its [Point] properties. */
        internal fun hash(point: Point): Int = 31 * point.x.hashCode() + point.y.hashCode()

        /** Returns a string representation for [point] using its [Point] properties. */
        internal fun string(point: Point): String = "Point(x=${point.x}, y=${point.y})"
    }
}
