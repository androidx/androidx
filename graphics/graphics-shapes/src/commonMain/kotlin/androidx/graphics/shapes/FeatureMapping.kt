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

/**
 * MeasuredFeatures contains a list of all features in a polygon along with the [0..1] progress at
 * that feature
 */
internal typealias MeasuredFeatures = List<ProgressableFeature>

internal data class ProgressableFeature(val progress: Float, val feature: Feature)

/** featureMapper creates a mapping between the "features" (rounded corners) of two shapes */
internal fun featureMapper(features1: MeasuredFeatures, features2: MeasuredFeatures): DoubleMapper {
    // We only use corners for this mapping.
    val filteredFeatures1 = buildList {
        // Performance: Builds the list by avoiding creating an unnecessary Iterator to iterate
        // through the features1 List.
        for (i in features1.indices) {
            if (features1[i].feature is Feature.Corner) {
                add(features1[i])
            }
        }
    }
    val filteredFeatures2 = buildList {
        // Performance: Builds the list by avoiding creating an unnecessary Iterator to iterate
        // through the features2 List.
        for (i in features2.indices) {
            if (features2[i].feature is Feature.Corner) {
                add(features2[i])
            }
        }
    }

    val featureProgressMapping = doMapping(filteredFeatures1, filteredFeatures2)

    debugLog(LOG_TAG) { featureProgressMapping.joinToString { "${it.first} -> ${it.second}" } }
    return DoubleMapper(*featureProgressMapping.toTypedArray()).also { dm ->
        debugLog(LOG_TAG) {
            val N = 10
            "Map: " +
                (0..N).joinToString { i -> (dm.map(i.toFloat() / N)).toStringWithLessPrecision() } +
                "\nMb : " +
                (0..N).joinToString { i ->
                    (dm.mapBack(i.toFloat() / N)).toStringWithLessPrecision()
                }
        }
    }
}

internal data class DistanceVertex(
    val distance: Float,
    val f1: ProgressableFeature,
    val f2: ProgressableFeature,
)

/**
 * Returns a mapping of the features between features1 and features2. The return is a list of pairs
 * in which the first element is the progress of a feature in features1 and the second element is
 * the progress of the feature in features2 that we mapped it to. The list is sorted by the first
 * element. To do this:
 * 1) Compute the distance for all pairs of features in (features1 x features2),
 * 2) Sort ascending by by such distance
 * 3) Try to add them, from smallest distance to biggest, ensuring that: a) The features we are
 *    mapping haven't been mapped yet. b) We are not adding a crossing in the mapping. Since the
 *    mapping is sorted by the first element of each pair, this means that the second elements of
 *    each pair are monotonically increasing, except maybe one time (Counting all pair of
 *    consecutive elements, and the last element to first element).
 */
internal fun doMapping(
    features1: List<ProgressableFeature>,
    features2: List<ProgressableFeature>,
): List<Pair<Float, Float>> {
    debugLog(LOG_TAG) { "Shape1 progresses: " + features1.map { it.progress }.joinToString() }
    debugLog(LOG_TAG) { "Shape2 progresses: " + features2.map { it.progress }.joinToString() }
    val distanceVertexList =
        buildList {
                for (f1 in features1) {
                    for (f2 in features2) {
                        val d = featureDistSquared(f1.feature, f2.feature)
                        if (d != Float.MAX_VALUE) add(DistanceVertex(d, f1, f2))
                    }
                }
            }
            .sortedBy { it.distance }

    // Special cases.
    if (distanceVertexList.isEmpty()) return IdentityMapping
    if (distanceVertexList.size == 1)
        return distanceVertexList.first().let {
            val f1 = it.f1.progress
            val f2 = it.f2.progress
            listOf(f1 to f2, (f1 + 0.5f) % 1f to (f2 + 0.5f) % 1f)
        }

    return MappingHelper().apply { distanceVertexList.forEach { addMapping(it.f1, it.f2) } }.mapping
}

private val IdentityMapping = listOf(0f to 0f, 0.5f to 0.5f)

private class MappingHelper() {
    // List of mappings from progress in the start shape to progress in the end shape.
    // We keep this list sorted by the first element.
    val mapping = mutableListOf<Pair<Float, Float>>()

    // Which features in the start shape have we used and which in the end shape.
    private val usedF1 = mutableSetOf<ProgressableFeature>()
    private val usedF2 = mutableSetOf<ProgressableFeature>()

    fun addMapping(f1: ProgressableFeature, f2: ProgressableFeature) {
        // We don't want to map the same feature twice.
        if (f1 in usedF1 || f2 in usedF2) return

        // Ret is sorted, find where we need to insert this new mapping.
        val index = mapping.binarySearchBy(f1.progress) { it.first }
        require(index < 0) { "There can't be two features with the same progress" }

        val insertionIndex = -index - 1
        val n = mapping.size

        // We can always add the first 1 element
        if (n >= 1) {
            val (before1, before2) = mapping[(insertionIndex + n - 1) % n]
            val (after1, after2) = mapping[insertionIndex % n]

            // We don't want features that are way too close to each other, that will make the
            // DoubleMapper unstable
            if (
                progressDistance(f1.progress, before1) < DistanceEpsilon ||
                    progressDistance(f1.progress, after1) < DistanceEpsilon ||
                    progressDistance(f2.progress, before2) < DistanceEpsilon ||
                    progressDistance(f2.progress, after2) < DistanceEpsilon
            ) {
                return
            }

            // When we have 2 or more elements, we need to ensure we are not adding extra crossings.
            if (n > 1 && !progressInRange(f2.progress, before2, after2)) return
        }

        // All good, we can add the mapping.
        mapping.add(insertionIndex, f1.progress to f2.progress)
        usedF1.add(f1)
        usedF2.add(f2)
    }
}

/**
 * Returns distance along overall shape between two Features on the two different shapes. This
 * information is used to determine how to map features (and the curves that make up those
 * features).
 */
internal fun featureDistSquared(f1: Feature, f2: Feature): Float {
    // TODO: We might want to enable concave-convex matching in some situations. If so, the
    //  approach below will not work
    if (f1 is Feature.Corner && f2 is Feature.Corner && f1.convex != f2.convex) {
        // Simple hack to force all features to map only to features of the same concavity, by
        // returning an infinitely large distance in that case
        debugLog(LOG_TAG) { "*** Feature distance ∞ for convex-vs-concave corners" }
        return Float.MAX_VALUE
    }
    return (featureRepresentativePoint(f1) - featureRepresentativePoint(f2)).getDistanceSquared()
}

// TODO: b/378441547 - Move to explicit parameter / expose?
internal fun featureRepresentativePoint(feature: Feature): Point {
    val x = (feature.cubics.first().anchor0X + feature.cubics.last().anchor1X) / 2f
    val y = (feature.cubics.first().anchor0Y + feature.cubics.last().anchor1Y) / 2f
    return Point(x, y)
}

private val LOG_TAG = "FeatureMapping"
