/*
 * Copyright 2023 The Android Open Source Project
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

import android.graphics.PointF
import androidx.annotation.FloatRange
import androidx.core.graphics.minus
import kotlin.math.abs

internal class MeasuredPolygon : AbstractList<MeasuredPolygon.MeasuredCubic> {
    private val measurer: Measurer
    private val cubics: List<MeasuredCubic>
    val features: List<Pair<Float, RoundedPolygon.Feature>>

    private constructor(
        measurer: Measurer,
        features: List<Pair<Float, RoundedPolygon.Feature>>,
        cubics: List<Cubic>,
        outlineProgress: List<Float>
    ) {
        require(outlineProgress.size == cubics.size + 1)
        require(outlineProgress.first() == 0f)
        require(outlineProgress.last() == 1f)
        this.measurer = measurer
        this.features = features

        if (DEBUG) {
            debugLog(LOG_TAG) {
                "CTOR: cubics = " + cubics.joinToString() +
                    "\nCTOR: op = " + outlineProgress.joinToString()
            }
        }
        val measuredCubics = mutableListOf<MeasuredCubic>()
        var startOutlineProgress = 0f
        cubics.forEachIndexed { index, cubic ->
            // Filter out "empty" cubics
            if ((outlineProgress[index + 1] - outlineProgress[index]) > DistanceEpsilon) {
                measuredCubics.add(MeasuredCubic(
                    cubic,
                    startOutlineProgress,
                    outlineProgress[index + 1]
                ))
                // The next measured cubic will start exactly where this one ends.
                startOutlineProgress = outlineProgress[index + 1]
            }
        }
        // We could have removed empty cubics at the end. Ensure the last measured cubic ends at 1f
        measuredCubics[measuredCubics.lastIndex].updateProgressRange(endOutlineProgress = 1f)
        this.cubics = measuredCubics
    }

    /**
     * A MeasuredCubic holds information about the cubic itself, the feature (if any) associated
     * with it, and the outline progress values (start and end) for the cubic. This information is
     * used to match cubics between shapes that lie at similar outline progress positions along
     * their respective shapes (after matching features and shifting).
     *
     * Outline progress is a value in [0..1) that represents the distance traveled along the overall
     * outline path of the shape.
     */
    internal inner class MeasuredCubic(
        val cubic: Cubic,
        @FloatRange(from = 0.0, to = 1.0) startOutlineProgress: Float,
        @FloatRange(from = 0.0, to = 1.0) endOutlineProgress: Float,
    ) {
        init {
            require(endOutlineProgress >= startOutlineProgress)
        }

        val measuredSize = measurer.measureCubic(cubic)

        var startOutlineProgress = startOutlineProgress
            private set

        var endOutlineProgress = endOutlineProgress
            private set

        internal fun updateProgressRange(
            startOutlineProgress: Float = this.startOutlineProgress,
            endOutlineProgress: Float = this.endOutlineProgress
        ) {
            require(endOutlineProgress >= startOutlineProgress)
            this.startOutlineProgress = startOutlineProgress
            this.endOutlineProgress = endOutlineProgress
        }

        /**
         * Cut this MeasuredCubic into two MeasuredCubics at the given outline progress value.
         */
        fun cutAtProgress(cutOutlineProgress: Float): Pair<MeasuredCubic, MeasuredCubic> {
            val outlineProgressSize = endOutlineProgress - startOutlineProgress
            val progressFromStart = positiveModule(cutOutlineProgress - startOutlineProgress, 1f)
            // progressFromStart should be in the [0 .. outlineProgressSize] range.
            // If it's not, cap to that range.
            val mid = if (progressFromStart > (1 + outlineProgressSize) / 2)
                0f
            else
                progressFromStart.coerceAtMost(outlineProgressSize)

            // Note that in earlier parts of the computation, we have empty MeasuredCubics (cubics
            // with progressSize == 0f), but those cubics are filtered out before this method is
            // called.
            val relativeMidProgress = mid / outlineProgressSize
            val t = measurer.findCubicCutPoint(cubic, relativeMidProgress * measuredSize)
            require(t in 0f..1f)

            debugLog(LOG_TAG) {
                "cutAtProgress: progress = $cutOutlineProgress / " +
                    "this = [$startOutlineProgress .. $endOutlineProgress] / " +
                    "pp = $mid / rp = $relativeMidProgress / t = $t"
            }

            // c1/c2 are the two new cubics, then we return MeasuredCubics created from them
            val (c1, c2) = cubic.split(t)
            return MeasuredCubic(c1, startOutlineProgress, cutOutlineProgress) to
                MeasuredCubic(c2, cutOutlineProgress, endOutlineProgress)
        }

        override fun toString(): String {
            return "MeasuredCubic(outlineProgress=" +
                "[$startOutlineProgress .. $endOutlineProgress], " +
                "size=$measuredSize, cubic=$cubic)"
        }
    }

    /**
     * Finds the point in the input list of measured cubics that pass the given outline progress,
     * and generates a new MeasuredPolygon (equivalent to this), that starts at that
     * point.
     * This usually means cutting the cubic that crosses the outline progress (unless the cut is
     * at one of its ends)
     * For example, given outline progress 0.4f and measured cubics on these outline progress
     * ranges:
     *
     * c1 [0 -> 0.2]
     * c2 [0.2 -> 0.5]
     * c3 [0.5 -> 1.0]
     *
     * c2 will be cut in two, at the given outline progress, we can name these c2a [0.2 -> 0.4] and
     * c2b [0.4 -> 0.5]
     *
     * The return then will have measured cubics [c2b, c3, c1, c2a], and they will have their
     * outline progress ranges adjusted so the new list starts at 0.
     * c2b [0 -> 0.1]
     * c3 [0.1 -> 0.6]
     * c1 [0.6 -> 0.8]
     * c2a [0.8 -> 1.0]
     */
    fun cutAndShift(
        cuttingPoint: Float
    ): MeasuredPolygon {
        require(cuttingPoint in 0f..1f)
        if (cuttingPoint < DistanceEpsilon) return this

        val n = cubics.size
        // Find the index of cubic we want to cut
        val targetIndex = cubics.indexOfFirst {
            cuttingPoint in it.startOutlineProgress..it.endOutlineProgress
        }
        val target = cubics[targetIndex]
        if (DEBUG) {
            cubics.forEachIndexed { index, cubic ->
                debugLog(LOG_TAG) { "cut&Shift | cubic #$index : $cubic " }
            }
            debugLog(LOG_TAG) {
                "cut&Shift, cuttingPoint = $cuttingPoint, target = ($targetIndex) $target"
            }
        }
        // Cut the target cubic.
        // b1, b2 are two resulting cubics after cut
        val (b1, b2) = target.cutAtProgress(cuttingPoint)
        debugLog(LOG_TAG) { "Split | $target -> $b1 & $b2" }

        // Construct the list of the cubics we need:
        // * The second part of the target cubic (after the cut)
        // * All cubics after the target, until the end + All cubics from the start, before the
        //   target cubic
        // * The first part of the target cubic (before the cut)
        val retCubics = mutableListOf(b2.cubic)
        for (i in 1 until n) {
            retCubics.add(cubics[(i + targetIndex) % cubics.size].cubic)
        }
        retCubics.add(b1.cubic)

        // Construct the array of outline progress.
        // For example, if we have 3 cubics with outline progress [0 .. 0.3], [0.3 .. 0.8] &
        // [0.8 .. 1.0], and we cut + shift at 0.6:
        // 0.  0123456789
        //     |--|--/-|-|
        // The outline progresses will start at 0 (the cutting point, that shifs to 0.0),
        // then 0.8 - 0.6 = 0.2, then 1 - 0.6 = 0.4, then 0.3 - 0.6 + 1 = 0.7,
        // then 1 (the cutting point again),
        // all together: (0.0, 0.2, 0.4, 0.7, 1.0)
        val retOutlineProgress = Array(cubics.size + 2) { index ->
            when (index) {
                0 -> 0f
                cubics.size + 1 -> 1f
                else -> {
                    val cubicIndex = (targetIndex + index - 1) % cubics.size
                    positiveModule(cubics[cubicIndex].endOutlineProgress - cuttingPoint, 1f)
                }
            }
        }.asList()

        // Shift the feature's outline progress too.
        val newFeatures = features.map { (outlineProgress, feature) ->
            positiveModule(outlineProgress - cuttingPoint, 1f) to feature
        }

        // Filter out all empty cubics (i.e. start and end anchor are (almost) the same point.)
        return MeasuredPolygon(measurer, newFeatures, retCubics, retOutlineProgress)
    }

    // Implementation of AbstractList.
    override val size
        get() = cubics.size

    override fun get(index: Int) = cubics[index]

    companion object {
        internal fun measurePolygon(measurer: Measurer, polygon: RoundedPolygon): MeasuredPolygon {
            val cubics = mutableListOf<Cubic>()
            val featureToCubic = mutableListOf<Pair<RoundedPolygon.Feature, Int>>()

            // Get the cubics from the polygon, at the same time, extract the features and keep a
            // reference to the representative cubic we will use.
            polygon.features.forEach { feature ->
                feature.cubics.forEachIndexed { index, cubic ->
                    if (feature is RoundedPolygon.Corner &&
                        index == feature.cubics.size / 2) {
                        featureToCubic.add(feature to cubics.size)
                    }
                    cubics.add(cubic)
                }
            }
            val measures = cubics.scan(0f) { measure, cubic ->
                measure + measurer.measureCubic(cubic).also { require(it >= 0f) }
            }
            val totalMeasure = measures.last()
            val outlineProgress = measures.map { it / totalMeasure }

            debugLog(LOG_TAG) { "Total size: $totalMeasure" }

            val features = featureToCubic.map { featureAndIndex ->
                val ix = featureAndIndex.second
                (outlineProgress[ix] + outlineProgress[ix + 1]) / 2 to
                    featureAndIndex.first
            }

            return MeasuredPolygon(measurer, features, cubics, outlineProgress)
        }
    }
}

// TODO: make this and the measurers public.
/**
 * Interface for measuring a cubic. Implementations can use whatever algorithm desired to produce
 * these measurement values.
 */
internal interface Measurer {

    /**
     * Returns size of given cubic, according to however the implementation wants to measure
     * the size (angle, length, etc). It has to be greater or equal to 0.
     */
    fun measureCubic(c: Cubic): Float

    /**
     * Given a cubic and a measure that should be between 0 and the value returned by measureCubic
     * (If not, it will be capped), finds the parameter t of the cubic at which that measure is
     * reached.
     */
    fun findCubicCutPoint(c: Cubic, m: Float): Float
}

/**
 * This measurer uses the angle of each cubic around the shape. This works well for current
 * Polygon shapes, but there are important assumptions which will break down for more general
 * shapes:
 * 1) Curves along the shape outline proceed in order; there is no reverse or self-intersecting
 * allowed. This guarantees that angle measurements are unique for every curve.
 * 2) There is a given 'center' for a shape. If the geometry is more arbitrary, there may be
 * no concept of a center, or the angles computed for an arbitrary center point might not be
 * consistent enough across the curves to work for general measurement.
 */
internal class AngleMeasurer(val center: PointF) : Measurer {
    /**
     * The measurement for a given cubic is the difference in angles between the start
     * and end points (first and last anchors) of the cubic.
     */
    override fun measureCubic(c: Cubic) =
        positiveModule(
            (c.p3 - center).angle() - (c.p0 - center).angle(),
            TwoPi
        ).let {
            // Avoid an empty cubic to measure almost TwoPi
            if (it > TwoPi - DistanceEpsilon) 0f else it
        }

    override fun findCubicCutPoint(c: Cubic, m: Float): Float {
        val angle0 = (c.p0 - center).angle()
        // TODO: use binary search.
        return findMinimum(0f, 1f, tolerance = 1e-5f) { t ->
            abs(positiveModule((c.pointOnCurve(t) - center).angle() - angle0, TwoPi) - m)
        }
    }
}

private val LOG_TAG = "PolygonMeasure"