/*
 * Copyright 2022 The Android Open Source Project
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

@file:JvmName("PathSegmentUtilities")
package androidx.graphics.path

import android.graphics.PointF

/**
 * A path segment represents a curve (line, cubic, quadratic or conic) or a command inside
 * a fully formed [path][android.graphics.Path] object.
 *
 * A segment is identified by a [type][PathSegment.Type] which in turns defines how many
 * [points] are available (from 0 to 3) and whether the [weight] is meaningful. Please refer
 * to the documentation of each [type][PathSegment.Type] for more information.
 *
 * A segment with the [Move][Type.Move] or [Close][Type.Close] is usually represented by
 * the singletons [DoneSegment] and [CloseSegment] respectively.
 *
 * @property type The type that identifies this segment and defines the number of points.
 * @property points An array of points describing this segment, whose size depends on [type].
 * @property weight Conic weight, only valid if [type] is [Type.Conic].
 */
class PathSegment internal constructor(
    val type: Type,
    @get:Suppress("ArrayReturn") val points: Array<PointF>,
    val weight: Float
) {

    /**
     * Type of a given segment in a [path][android.graphics.Path], either a command
     * ([Type.Move], [Type.Close], [Type.Done]) or a curve ([Type.Line], [Type.Cubic],
     * [Type.Quadratic], [Type.Conic]).
     */
    enum class Type {
        /**
         * Move command, the path segment contains 1 point indicating the move destination.
         * The weight is set 0.0f and not meaningful.
         */
        Move,
        /**
         * Line curve, the path segment contains 2 points indicating the two extremities of
         * the line. The weight is set 0.0f and not meaningful.
         */
        Line,
        /**
         * Quadratic curve, the path segment contains 3 points in the following order:
         * - Start point
         * - Control point
         * - End point
         *
         * The weight is set 0.0f and not meaningful.
         */
        Quadratic,
        /**
         * Conic curve, the path segment contains 3 points in the following order:
         * - Start point
         * - Control point
         * - End point
         *
         * The curve is weighted by the [weight][PathSegment.weight] property.
         */
        Conic,
        /**
         * Cubic curve, the path segment contains 4 points in the following order:
         * - Start point
         * - First control point
         * - Second control point
         * - End point
         *
         * The weight is set 0.0f and not meaningful.
         */
        Cubic,
        /**
         * Close command, close the current contour by joining the last point added to the
         * path with the first point of the current contour. The segment does not contain
         * any point. The weight is set 0.0f and not meaningful.
         */
        Close,
        /**
         * Done command, which indicates that no further segment will be
         * found in the path. It typically indicates the end of an iteration over a path
         * and can be ignored.
         */
        Done
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PathSegment

        if (type != other.type) return false
        if (!points.contentEquals(other.points)) return false
        if (weight != other.weight) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + points.contentHashCode()
        result = 31 * result + weight.hashCode()
        return result
    }

    override fun toString(): String {
        return "PathSegment(type=$type, points=${points.contentToString()}, weight=$weight)"
    }
}

/**
 * A [PathSegment] containing the [Done][PathSegment.Type.Done] command.
 * This static object exists to avoid allocating a new segment when returning a
 * [Done][PathSegment.Type.Done] result from [PathIterator.next].
 */
val DoneSegment = PathSegment(PathSegment.Type.Done, emptyArray(), 0.0f)

/**
 * A [PathSegment] containing the [Close][PathSegment.Type.Close] command.
 * This static object exists to avoid allocating a new segment when returning a
 * [Close][PathSegment.Type.Close] result from [PathIterator.next].
 */
val CloseSegment = PathSegment(PathSegment.Type.Close, emptyArray(), 0.0f)

/**
 * Cache of [PathSegment.Type] values to avoid internal allocation on each use.
 */
internal val pathSegmentTypes = PathSegment.Type.values()
