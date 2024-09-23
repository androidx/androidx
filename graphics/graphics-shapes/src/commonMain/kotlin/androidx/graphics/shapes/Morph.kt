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

package androidx.graphics.shapes

import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic
import kotlin.math.max
import kotlin.math.min

/**
 * This class is used to animate between start and end polygons objects.
 *
 * Morphing between arbitrary objects can be problematic because it can be difficult to determine
 * how the points of a given shape map to the points of some other shape. [Morph] simplifies the
 * problem by only operating on [RoundedPolygon] objects, which are known to have similar,
 * contiguous structures. For one thing, the shape of a polygon is contiguous from start to end
 * (compared to an arbitrary Path object, which could have one or more `moveTo` operations in the
 * shape). Also, all edges of a polygon shape are represented by [Cubic] objects, thus the start and
 * end shapes use similar operations. Two Polygon shapes then only differ in the quantity and
 * placement of their curves. The morph works by determining how to map the curves of the two shapes
 * together (based on proximity and other information, such as distance to polygon vertices and
 * concavity), and splitting curves when the shapes do not have the same number of curves or when
 * the curve placement within the shapes is very different.
 */
class Morph(private val start: RoundedPolygon, private val end: RoundedPolygon) {
    /**
     * The structure which holds the actual shape being morphed. It contains all cubics necessary to
     * represent the start and end shapes (the original cubics in the shapes may be cut to align the
     * start/end shapes), matched one to one in each Pair.
     */
    @PublishedApi
    internal val morphMatch: List<Pair<Cubic, Cubic>>
        get() = _morphMatch

    private val _morphMatch: List<Pair<Cubic, Cubic>> = match(start, end)

    /**
     * Calculates the axis-aligned bounds of the object.
     *
     * @param approximate when true, uses a faster calculation to create the bounding box based on
     *   the min/max values of all anchor and control points that make up the shape. Default value
     *   is true.
     * @param bounds a buffer to hold the results. If not supplied, a temporary buffer will be
     *   created.
     * @return The axis-aligned bounding box for this object, where the rectangles left, top, right,
     *   and bottom values will be stored in entries 0, 1, 2, and 3, in that order.
     */
    @JvmOverloads
    fun calculateBounds(
        bounds: FloatArray = FloatArray(4),
        approximate: Boolean = true
    ): FloatArray {
        start.calculateBounds(bounds, approximate)
        val minX = bounds[0]
        val minY = bounds[1]
        val maxX = bounds[2]
        val maxY = bounds[3]
        end.calculateBounds(bounds, approximate)
        bounds[0] = min(minX, bounds[0])
        bounds[1] = min(minY, bounds[1])
        bounds[2] = max(maxX, bounds[2])
        bounds[3] = max(maxY, bounds[3])
        return bounds
    }

    /**
     * Like [calculateBounds], this function calculates the axis-aligned bounds of the object and
     * returns that rectangle. But this function determines the max dimension of the shape (by
     * calculating the distance from its center to the start and midpoint of each curve) and returns
     * a square which can be used to hold the object in any rotation. This function can be used, for
     * example, to calculate the max size of a UI element meant to hold this shape in any rotation.
     *
     * @param bounds a buffer to hold the results. If not supplied, a temporary buffer will be
     *   created.
     * @return The axis-aligned max bounding box for this object, where the rectangles left, top,
     *   right, and bottom values will be stored in entries 0, 1, 2, and 3, in that order.
     */
    fun calculateMaxBounds(bounds: FloatArray = FloatArray(4)): FloatArray {
        start.calculateMaxBounds(bounds)
        val minX = bounds[0]
        val minY = bounds[1]
        val maxX = bounds[2]
        val maxY = bounds[3]
        end.calculateMaxBounds(bounds)
        bounds[0] = min(minX, bounds[0])
        bounds[1] = min(minY, bounds[1])
        bounds[2] = max(maxX, bounds[2])
        bounds[3] = max(maxY, bounds[3])
        return bounds
    }

    /**
     * Returns a representation of the morph object at a given [progress] value as a list of Cubics.
     * Note that this function causes a new list to be created and populated, so there is some
     * overhead.
     *
     * @param progress a value from 0 to 1 that determines the morph's current shape, between the
     *   start and end shapes provided at construction time. A value of 0 results in the start
     *   shape, a value of 1 results in the end shape, and any value in between results in a shape
     *   which is a linear interpolation between those two shapes. The range is generally [0..1] and
     *   values outside could result in undefined shapes, but values close to (but outside) the
     *   range can be used to get an exaggerated effect (e.g., for a bounce or overshoot animation).
     */
    fun asCubics(progress: Float): List<Cubic> {
        return buildList {
            // The first/last mechanism here ensures that the final anchor point in the shape
            // exactly matches the first anchor point. There can be rendering artifacts introduced
            // by those points being slightly off, even by much less than a pixel
            var firstCubic: Cubic? = null
            var lastCubic: Cubic? = null
            for (i in _morphMatch.indices) {
                val cubic =
                    Cubic(
                        FloatArray(8) {
                            interpolate(
                                _morphMatch[i].first.points[it],
                                _morphMatch[i].second.points[it],
                                progress
                            )
                        }
                    )
                if (firstCubic == null) firstCubic = cubic
                if (lastCubic != null) add(lastCubic)
                lastCubic = cubic
            }
            if (lastCubic != null && firstCubic != null)
                add(
                    Cubic(
                        lastCubic.anchor0X,
                        lastCubic.anchor0Y,
                        lastCubic.control0X,
                        lastCubic.control0Y,
                        lastCubic.control1X,
                        lastCubic.control1Y,
                        firstCubic.anchor0X,
                        firstCubic.anchor0Y
                    )
                )
        }
    }

    /**
     * Returns a representation of the morph object at a given [progress] value, iterating over the
     * cubics and calling the callback. This function is faster than [asCubics], since it doesn't
     * allocate new [Cubic] instances, but to do this it reuses the same [MutableCubic] instance
     * during iteration.
     *
     * @param progress a value from 0 to 1 that determines the morph's current shape, between the
     *   start and end shapes provided at construction time. A value of 0 results in the start
     *   shape, a value of 1 results in the end shape, and any value in between results in a shape
     *   which is a linear interpolation between those two shapes. The range is generally [0..1] and
     *   values outside could result in undefined shapes, but values close to (but outside) the
     *   range can be used to get an exaggerated effect (e.g., for a bounce or overshoot animation).
     * @param mutableCubic An instance of [MutableCubic] that will be used to set each cubic in
     *   time.
     * @param callback The function to be called for each Cubic
     */
    @JvmOverloads
    inline fun forEachCubic(
        progress: Float,
        mutableCubic: MutableCubic = MutableCubic(),
        callback: (MutableCubic) -> Unit
    ) {
        for (i in morphMatch.indices) {
            mutableCubic.interpolate(morphMatch[i].first, morphMatch[i].second, progress)
            callback(mutableCubic)
        }
    }

    internal companion object {
        /**
         * [match], called at Morph construction time, creates the structure used to animate between
         * the start and end shapes. The technique is to match geometry (curves) between the shapes
         * when and where possible, and to create new/placeholder curves when necessary (when one of
         * the shapes has more curves than the other). The result is a list of pairs of Cubic
         * curves. Those curves are the matched pairs: the first of each pair holds the geometry of
         * the start shape, the second holds the geometry for the end shape. Changing the progress
         * of a Morph object simply interpolates between all pairs of curves for the morph shape.
         *
         * Curves on both shapes are matched by running the [Measurer] to determine where the points
         * are in each shape (proportionally, along the outline), and then running [featureMapper]
         * which decides how to map (match) all of the curves with each other.
         */
        @JvmStatic
        internal fun match(p1: RoundedPolygon, p2: RoundedPolygon): List<Pair<Cubic, Cubic>> {
            // TODO Commented out due to the use of javaClass ("Error: Platform reference in a
            //  common module")
            /*
            if (DEBUG) {
               repeat(2) { polyIndex ->
                   debugLog(LOG_TAG) {
                       listOf("Initial start:\n", "Initial end:\n")[polyIndex] +
                           listOf(p1, p2)[polyIndex].features.joinToString("\n") { feature ->
                               "${feature.javaClass.name.split("$").last()} - " +
                                   ((feature as? Feature.Corner)?.convex?.let {
                                       if (it) "Convex - " else "Concave - " } ?: "") +
                                   feature.cubics.joinToString("|")
                           }
                   }
               }
            }
            */

            // Measure polygons, returns lists of measured cubics for each polygon, which
            // we then use to match start/end curves
            val measuredPolygon1 = MeasuredPolygon.measurePolygon(LengthMeasurer(), p1)
            val measuredPolygon2 = MeasuredPolygon.measurePolygon(LengthMeasurer(), p2)

            // features1 and 2 will contain the list of corners (just the inner circular curve)
            // along with the progress at the middle of those corners. These measurement values
            // are then used to compare and match between the two polygons
            val features1 = measuredPolygon1.features
            val features2 = measuredPolygon2.features

            // Map features: doubleMapper is the result of mapping the features in each shape to the
            // closest feature in the other shape.
            // Given a progress in one of the shapes it can be used to find the corresponding
            // progress in the other shape (in both directions)
            val doubleMapper = featureMapper(features1, features2)

            // cut point on poly2 is the mapping of the 0 point on poly1
            val polygon2CutPoint = doubleMapper.map(0f)
            debugLog(LOG_TAG) { "polygon2CutPoint = $polygon2CutPoint" }

            // Cut and rotate.
            // Polygons start at progress 0, and the featureMapper has decided that we want to match
            // progress 0 in the first polygon to `polygon2CutPoint` on the second polygon.
            // So we need to cut the second polygon there and "rotate it", so as we walk through
            // both polygons we can find the matching.
            // The resulting bs1/2 are MeasuredPolygons, whose MeasuredCubics start from
            // outlineProgress=0 and increasing until outlineProgress=1
            val bs1 = measuredPolygon1
            val bs2 = measuredPolygon2.cutAndShift(polygon2CutPoint)

            if (DEBUG) {
                (0 until bs1.size).forEach { index ->
                    debugLog(LOG_TAG) { "start $index: ${bs1.getOrNull(index)}" }
                }
                (0 until bs2.size).forEach { index ->
                    debugLog(LOG_TAG) { "End $index: ${bs2.getOrNull(index)}" }
                }
            }

            // Match
            // Now we can compare the two lists of measured cubics and create a list of pairs
            // of cubics [ret], which are the start/end curves that represent the Morph object
            // and the start and end shapes, and which can be interpolated to animate the
            // between those shapes.
            val ret = mutableListOf<Pair<Cubic, Cubic>>()
            // i1/i2 are the indices of the current cubic on the start (1) and end (2) shapes
            var i1 = 0
            var i2 = 0
            // b1, b2 are the current measured cubic for each polygon
            var b1 = bs1.getOrNull(i1++)
            var b2 = bs2.getOrNull(i2++)
            // Iterate until all curves are accounted for and matched
            while (b1 != null && b2 != null) {
                // Progresses are in shape1's perspective
                // b1a, b2a are ending progress values of current measured cubics in [0,1] range
                val b1a = if (i1 == bs1.size) 1f else b1.endOutlineProgress
                val b2a =
                    if (i2 == bs2.size) 1f
                    else
                        doubleMapper.mapBack(
                            positiveModulo(b2.endOutlineProgress + polygon2CutPoint, 1f)
                        )
                val minb = min(b1a, b2a)
                debugLog(LOG_TAG) { "$b1a $b2a | $minb" }
                // min b is the progress at which the curve that ends first ends.
                // If both curves ends roughly there, no cutting is needed, we have a match.
                // If one curve extends beyond, we need to cut it.
                val (seg1, newb1) =
                    if (b1a > minb + AngleEpsilon) {
                        debugLog(LOG_TAG) { "Cut 1" }
                        b1.cutAtProgress(minb)
                    } else {
                        b1 to bs1.getOrNull(i1++)
                    }
                val (seg2, newb2) =
                    if (b2a > minb + AngleEpsilon) {
                        debugLog(LOG_TAG) { "Cut 2" }
                        b2.cutAtProgress(
                            positiveModulo(doubleMapper.map(minb) - polygon2CutPoint, 1f)
                        )
                    } else {
                        b2 to bs2.getOrNull(i2++)
                    }
                debugLog(LOG_TAG) { "Match: $seg1 -> $seg2" }
                ret.add(seg1.cubic to seg2.cubic)
                b1 = newb1
                b2 = newb2
            }
            require(b1 == null && b2 == null) {
                "Expected both Polygon's Cubic to be fully matched"
            }

            if (DEBUG) {
                // Export as SVG path.
                val showPoint: (Point) -> String = {
                    "${(it.x * 100).toStringWithLessPrecision()} ${(it.y * 100).toStringWithLessPrecision()}"
                }
                repeat(2) { listIx ->
                    val points = ret.map { if (listIx == 0) it.first else it.second }
                    debugLog(LOG_TAG) {
                        "M " +
                            showPoint(Point(points.first().anchor0X, points.first().anchor0Y)) +
                            " " +
                            points.joinToString(" ") {
                                "C " +
                                    showPoint(Point(it.control0X, it.control0Y)) +
                                    ", " +
                                    showPoint(Point(it.control1X, it.control1Y)) +
                                    ", " +
                                    showPoint(Point(it.anchor1X, it.anchor1Y))
                            } +
                            " Z"
                    }
                }
            }
            return ret
        }
    }
}

private val LOG_TAG = "Morph"
