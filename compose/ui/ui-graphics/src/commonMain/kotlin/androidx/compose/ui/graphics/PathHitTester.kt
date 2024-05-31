/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.graphics

import androidx.annotation.FloatRange
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/**
 * Creates a new [PathHitTester] to query whether certain x/y coordinates lie inside a given [Path].
 * A [PathHitTester] is optimized to perform multiple queries against a single path.
 *
 * The result of a query depends on the [fill type][Path.fillType] of the path.
 *
 * If the content of [path] changes, you must call [PathHitTester.updatePath] or create a new
 * [PathHitTester] as [PathHitTester] will cache precomputed values to speed up queries.
 *
 * If [path] contains conic curves, they are converted to quadratic curves during the query process.
 * The tolerance of that conversion is defined by [tolerance]. The tolerance should be appropriate
 * to the coordinate systems used by the caller. For instance if the path is defined in pixels, 0.5
 * (half a pixel) or 1.0 (a pixel) are appropriate tolerances. If the path is normalized and defined
 * in the domain 0..1, the caller should choose a more appropriate tolerance close to or equal to
 * one "query unit". The tolerance must be >= 0.
 *
 * @param path The [Path] to run queries against.
 * @param tolerance When [path] contains conic curves, defines the maximum distance between the
 *   original conic curve and its quadratic approximations. Set to 0.5 by default.
 */
fun PathHitTester(path: Path, @FloatRange(from = 0.0) tolerance: Float = 0.5f) =
    PathHitTester().apply { updatePath(path, tolerance) }

private val EmptyPath = Path()

/**
 * A [PathHitTester] is used to query whether certain x/y coordinates lie inside a given [Path]. A
 * [PathHitTester] is optimized to perform multiple queries against a single path.
 */
class PathHitTester {
    private var path = EmptyPath
    private var tolerance = 0.5f

    // The bounds of [path], precomputed
    private var bounds = Rect.Zero

    // When cached is set to true, the path's segments are cached inside an [IntervalTree]
    // to speed up hit testing by performing computations against the segments that cross
    // the y axis of the test position
    private val intervals = IntervalTree<PathSegment>()

    // Scratch buffers used to avoid allocations when performing a hit test
    private val curves = FloatArray(20)
    private val roots = FloatArray(2)

    /**
     * Sets the [Path] to run queries against.
     *
     * If [path] contains conic curves, they are converted to quadratic curves during the query
     * process. This value defines the tolerance of that conversion.
     *
     * The tolerance should be appropriate to the coordinate systems used by the caller. For
     * instance if the path is defined in pixels, 0.5 (half a pixel) or 1.0 (a pixel) are
     * appropriate tolerances. If the path is normalized and defined in the domain 0..1, the caller
     * should choose a more appropriate tolerance close to or equal to one "query unit". The
     * tolerance must be >= 0.
     *
     * @param path The [Path] to run queries against.
     * @param tolerance When [path] contains conic curves, defines the maximum distance between the
     *   original conic curve and its quadratic approximations. Set to 0.5 by default.
     */
    fun updatePath(path: Path, @FloatRange(from = 0.0) tolerance: Float = 0.5f) {
        this.path = path
        this.tolerance = tolerance
        bounds = path.getBounds()

        intervals.clear()
        // TODO: We should handle conics ourselves, which would allow us to cheaply query
        //       the number of segments in the path, which would in turn allow us to allocate
        //       all of our data structures with an appropriate size to store everything in
        //       a single array for instance
        val iterator = path.iterator(PathIterator.ConicEvaluation.AsQuadratics, tolerance)
        for (segment in iterator) {
            when (segment.type) {
                PathSegment.Type.Line,
                PathSegment.Type.Quadratic,
                PathSegment.Type.Cubic -> {
                    val (min, max) = computeVerticalBounds(segment, curves)
                    intervals.addInterval(min, max, segment)
                }
                PathSegment.Type.Done -> break
                else -> {}
            }
        }
    }

    /**
     * Queries whether the specified [position] is inside this [Path]. The
     * [path's fill type][Path.fillType] is taken into account to determine if the point lies inside
     * this path or not.
     *
     * @param position The x/y coordinates of the point to test.
     * @return True if [position] is inside this path, false otherwise.
     */
    operator fun contains(position: Offset): Boolean {
        // TODO: If/when Compose supports inverse fill types, compute this value
        val isInverse = false

        if (path.isEmpty || position !in bounds) {
            return isInverse
        }

        val (x, y) = position
        val curvesArray = curves
        val rootsArray = roots

        var winding = 0

        intervals.forEach(y) { interval ->
            val segment = interval.data!!
            val points = segment.points
            when (segment.type) {
                PathSegment.Type.Line -> {
                    winding += lineWinding(points, x, y)
                }
                PathSegment.Type.Quadratic -> {
                    winding += quadraticWinding(points, x, y, curvesArray, rootsArray)
                }
                PathSegment.Type.Cubic -> {
                    winding += cubicWinding(points, x, y, curvesArray, rootsArray)
                }
                PathSegment.Type.Done -> return@forEach
                else -> {} // Nothing to do for Move, Conic
            }
        }

        val isEvenOdd = path.fillType == PathFillType.EvenOdd
        if (isEvenOdd) {
            winding = winding and 1
        }

        if (winding != 0) {
            return !isInverse
        }

        // TODO: handle cases where the point is on the curve

        return false
    }
}
