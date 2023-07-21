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

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import kotlin.math.min

/**
 * This class is used to animate between start and end polygons objects.
 *
 * Morphing between arbitrary objects can be problematic because it can be difficult to
 * determine how the points of a given shape map to the points of some other shape.
 * [Morph] simplifies the problem by only operating on [RoundedPolygon] objects, which
 * are known to have similar, contiguous structures. For one thing, the shape of a polygon
 * is contiguous from start to end (compared to an arbitrary [Path] object, which could have
 * one or more `moveTo` operations in the shape). Also, all edges of a polygon shape are
 * represented by [Cubic] objects, thus the start and end shapes use similar operations. Two
 * Polygon shapes then only differ in the quantity and placement of their curves.
 * The morph works by determining how to map the curves of the two shapes together (based on
 * proximity and other information, such as distance to polygon vertices and concavity),
 * and splitting curves when the shapes do not have the same number of curves or when the
 * curve placement within the shapes is very different.
 */
class Morph(
    start: RoundedPolygon,
    end: RoundedPolygon
) {
    // morphMatch is the structure which holds the actual shape being morphed. It contains
    // all cubics necessary to represent the start and end shapes (the original cubics in the
    // shapes may be cut to align the start/end shapes)
    private var morphMatch = match(start, end)

    // path is used to draw the object
    private val path = Path()

    // These temp anchor/control points are used when progress changes to hold interpolated values
    // Using these structures avoids allocations during morph animation
    private val tempA0 = PointF()
    private val tempC0 = PointF()
    private val tempC1 = PointF()
    private val tempA1 = PointF()

    /**
     * The bounds of the morph object are estimated by control and anchor points of all cubic curves
     * representing the shape.
     */
    val bounds = RectF()

    init {
        calculateBounds(bounds)
        updatePath()
    }

    /**
     * Rough bounds of the object, based on the min/max bounds of all cubics points in morphMatch
     */
    private fun calculateBounds(bounds: RectF) {
        // TODO: Maybe using just the anchors (p0 and p3) is sufficient and more correct than
        // also using the control points (p1 and p2)
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        for (pair in morphMatch) {
            with(pair.first.p0) {
                if (x < minX) minX = x
                if (y < minY) minY = y
                if (x > maxX) maxX = x
                if (y > maxY) maxY = y
            }
            with(pair.second.p0) {
                if (x < minX) minX = x
                if (y < minY) minY = y
                if (x > maxX) maxX = x
                if (y > maxY) maxY = y
            }
            with(pair.first.p1) {
                if (x < minX) minX = x
                if (y < minY) minY = y
                if (x > maxX) maxX = x
                if (y > maxY) maxY = y
            }
            with(pair.second.p1) {
                if (x < minX) minX = x
                if (y < minY) minY = y
                if (x > maxX) maxX = x
                if (y > maxY) maxY = y
            }
            with(pair.first.p2) {
                if (x < minX) minX = x
                if (y < minY) minY = y
                if (x > maxX) maxX = x
                if (y > maxY) maxY = y
            }
            with(pair.second.p2) {
                if (x < minX) minX = x
                if (y < minY) minY = y
                if (x > maxX) maxX = x
                if (y > maxY) maxY = y
            }
            // Skip p3 since every p3 is the next curve's p0
        }
        bounds.set(minX, minY, maxX, maxY)
    }

    /**
     * The progress of a [Morph] object is a value from 0 to 1 that determines its current
     * shape, between the start and end shapes provided at construction time. A value of 0 results
     * in the start shape, a value of 1 results in the end shape, and any value in between
     * results in a shape which is a linear interpolation between those two shapes.
     *
     * The range is generally [0..1] and values outside could result in undefined shapes, but
     * values close to (but outside) the range can be used to get an exaggerated effect
     * (e.g., for a bounce or overshoot animation).
     */
    var progress: Float = 0.0f
        set(value) {
            field = value
            updatePath()
        }

    /**
     * This function updates the [path] object which holds the rendering information for the
     * morph shape, using the current [progress] property for the morph.
     */
    private fun updatePath() {
        // In a future release, Path interpolation may be possible through the Path API
        // itself. Until then, we have to rewind and repopulate with the new/interpolated
        // values
        path.rewind()

        // If the list is not empty, do an initial moveTo using the first element of the match.
        morphMatch.firstOrNull()?. let { first ->
            interpolate(first.first.p0, first.second.p0, tempA0, progress)
            path.moveTo(tempA0.x, tempA0.y)
        }

        // And one cubicTo for each element, including the first.
        for (element in morphMatch) {
            interpolate(element.first.p1, element.second.p1, tempC0, progress)
            interpolate(element.first.p2, element.second.p2, tempC1, progress)
            interpolate(element.first.p3, element.second.p3, tempA1, progress)
            path.cubicTo(tempC0.x, tempC0.y, tempC1.x, tempC1.y, tempA1.x, tempA1.y)
        }
    }

    /**
     * Transforms (scales, rotates, and translates) the shape by the given matrix.
     * Note that this operation alters the points in the shape directly; the original
     * points are not retained, nor is the matrix itself. Thus calling this function
     * twice with the same matrix will composite the effect. For example, a matrix which
     * scales by 2 will scale the shape by 2. Calling transform twice with that matrix
     * will have the effect of scaling the original shape by 4.
     */
    fun transform(matrix: Matrix) {
        for (pair in morphMatch) {
            pair.first.transform(matrix)
            pair.second.transform(matrix)
        }
        calculateBounds(bounds)
        updatePath()
    }

    /**
     * Morph is rendered as a [Path]. A copy of the underlying [Path] object can be
     * retrieved for use outside of this class. Note that this function returns a copy of
     * the internal [Path] to maintain immutability, thus there is some overhead in retrieving
     * the path with this function.
     */
    fun asPath(): Path {
        return Path(path)
    }

    /**
     * Returns a view of the current state of this morph as a list of Cubics.
     * Note that this function causes a new list to be created and populated, so there is some
     * overhead.
     */
    fun asCubics() =
        mutableListOf<Cubic>().apply {
            clear()
            for (pair in morphMatch) {
                add(Cubic.interpolate(pair.first, pair.second, progress))
            }
        }

    internal companion object {
        /**
         * [match], called at Morph construction time, creates the structure used to animate between
         * the start and end shapes. The technique is to match geometry (curves) between the shapes
         * when and where possible, and to create new/placeholder curves when necessary (when
         * one of the shapes has more curves than the other). The result is a list of pairs of
         * Cubic curves. Those curves are the matched pairs: the first of each pair holds the
         * geometry of the start shape, the second holds the geometry for the end shape.
         * Changing the progress of a Morph object simply interpolates between all pairs of
         * curves for the morph shape.
         *
         * Curves on both shapes are matched by running the [Measurer] to determine where
         * the points are in each shape (proportionally, along the outline), and then running
         * [featureMapper] which decides how to map (match) all of the curves with each other.
         */
        @JvmStatic
        internal fun match(
            p1: RoundedPolygon,
            p2: RoundedPolygon
        ): List<Pair<Cubic, Cubic>> {
            if (DEBUG) {
                repeat(2) { polyIndex ->
                    debugLog(LOG_TAG) {
                        listOf("Initial start:\n", "Initial end:\n")[polyIndex] +
                            listOf(p1, p2)[polyIndex].features.joinToString("\n") { feature ->
                                "${feature.javaClass.name.split("$").last()} - " +
                                    ((feature as? RoundedPolygon.Corner)?.convex?.let {
                                        if (it) "Convex - " else "Concave - " } ?: "") +
                                    feature.cubics.joinToString("|")
                            }
                    }
                }
            }

            // Measure polygons, returns lists of measured cubics for each polygon, which
            // we then use to match start/end curves
            val measuredPolygon1 = MeasuredPolygon.measurePolygon(AngleMeasurer(p1.center), p1)
            val measuredPolygon2 = MeasuredPolygon.measurePolygon(AngleMeasurer(p2.center), p2)

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
                val b2a = if (i2 == bs2.size) 1f else doubleMapper.mapBack(
                    positiveModule(b2.endOutlineProgress + polygon2CutPoint, 1f)
                )
                val minb = min(b1a, b2a)
                debugLog(LOG_TAG) { "$b1a $b2a | $minb" }
                // minb is the progress at which the curve that ends first ends.
                // If both curves ends roughly there, no cutting is needed, we have a match.
                // If one curve extends beyond, we need to cut it.
                val (seg1, newb1) = if (b1a > minb + AngleEpsilon) {
                    debugLog(LOG_TAG) { "Cut 1" }
                    b1.cutAtProgress(minb)
                } else {
                    b1 to bs1.getOrNull(i1++)
                }
                val (seg2, newb2) = if (b2a > minb + AngleEpsilon) {
                    debugLog(LOG_TAG) { "Cut 2" }
                    b2.cutAtProgress(positiveModule(doubleMapper.map(minb) - polygon2CutPoint, 1f))
                } else {
                    b2 to bs2.getOrNull(i2++)
                }
                debugLog(LOG_TAG) { "Match: $seg1 -> $seg2" }
                ret.add(Cubic(seg1.cubic) to Cubic(seg2.cubic))
                b1 = newb1
                b2 = newb2
            }
            require(b1 == null && b2 == null)

            if (DEBUG) {
                // Export as SVG path. Temporary while working with the Koru team.
                val showPoint: (PointF) -> String = {
                    "%.3f %.3f".format(it.x * 100, it.y * 100)
                }
                repeat(2) { listIx ->
                    val points = ret.map { if (listIx == 0) it.first else it.second }
                    debugLog(LOG_TAG) {
                        "M " + showPoint(points.first().p0) + " " +
                            points.joinToString(" ") {
                                "C " + showPoint(it.p1) + ", " +
                                    showPoint(it.p2) + ", " +
                                    showPoint(it.p3)
                            } + " Z"
                    }
                }
            }
            return ret
        }
    }

    /**
     * Draws the Morph object. This is called by the public extension function
     * [Canvas.drawMorph]. By default, it simply calls [Canvas.drawPath].
     */
    internal fun draw(canvas: Canvas, paint: Paint) {
        canvas.drawPath(path, paint)
    }
}

/**
 * Extension function which draws the given [Morph] object into this [Canvas]. Rendering
 * occurs by drawing the underlying path for the object; callers can optionally retrieve the
 * path and draw it directly via [Morph.asPath] (though that function copies the underlying
 * path. This extension function avoids that overhead when rendering).
 *
 * @param morph The object to be drawn
 * @param paint The attributes
 */
fun Canvas.drawMorph(morph: Morph, paint: Paint) {
    morph.draw(this, paint)
}

private val LOG_TAG = "Morph"