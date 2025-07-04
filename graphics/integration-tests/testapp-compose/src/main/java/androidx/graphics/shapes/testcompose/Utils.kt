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

package androidx.graphics.shapes.testcompose

import androidx.annotation.FloatRange
import androidx.compose.ui.geometry.Offset
import androidx.graphics.shapes.Cubic
import androidx.graphics.shapes.Feature
import androidx.graphics.shapes.RoundedPolygon
import kotlin.math.ceil
import kotlin.math.max

internal fun RoundedPolygon.split(
    @FloatRange(from = 0.0, to = 1.0, fromInclusive = false) threshold: Float = 1f
): RoundedPolygon {
    if (threshold == 1f || threshold == 0f) {
        return this
    }

    val measurer = LengthMeasurer()

    val longestLength = features.maxOf { measurer.measureFeature(it) }
    val splitLength = threshold * longestLength

    val newFeatures = features.flatMap { split(it, measurer, splitLength) }
    return RoundedPolygon(newFeatures, this.centerX, this.centerY)
}

internal fun split(feature: Feature, measurer: LengthMeasurer, splitLength: Float): List<Feature> {
    val splitCubics = buildList {
        feature.cubics.forEach { cubic ->
            val cubicLength = measurer.measureCubic(cubic)
            val numberOfSegments = max(1, ceil(cubicLength / splitLength).toInt())
            addAll(
                splitInEvenSegments(
                    cubic,
                    numberOfSegments,
                    Array(numberOfSegments) { feature.cubics.first() },
                )
            )
        }
    }

    val performedSplits = splitCubics.size - feature.cubics.size

    if (performedSplits == 0) {
        return listOf(feature)
    }

    // Divide the feature into even parts according to the splits.
    val cubicsPerSplit = splitCubics.size / max(2, performedSplits)

    // Chunked would normally make the last chunk smaller if the size doesn't add up.
    // However, this encourages small-ish cubics that were e.g. used for roundings being their
    // own feature. We prefer matching them with their neighbors and having the middle one
    // stand for itself, as this cubic will typically be longer.
    val cubicSegments =
        if (cubicsPerSplit != 1 && splitCubics.size % cubicsPerSplit == 1) {
            buildList {
                addAll(splitCubics.take(splitCubics.size / 2).chunked(cubicsPerSplit))
                add(listOf(splitCubics[splitCubics.size / 2]))
                addAll(splitCubics.drop(splitCubics.size / 2 + 1).chunked(cubicsPerSplit))
            }
        } else {
            splitCubics.chunked(cubicsPerSplit)
        }

    return if (feature.isConvexCorner) {
        cubicSegments.map { Feature.buildConvexCorner(cubics = it) }
    } else if (feature.isConcaveCorner) {
        cubicSegments.map { Feature.buildConcaveCorner(cubics = it) }
    } else {
        cubicSegments.map { Feature.buildIgnorableFeature(cubics = it) }
    }
}

/**
 * Recursively split a Cubic into even segments. We split a Cubic into two parts whereas the latter
 * will be the required size. The first part then gets split in segments-1 parts, which will create
 * Cubics of equal length to the latter part of the split. Example: --------------- into 3 segments
 * first split: ----------, -----. Split first half into 2 segments second split: -----, -----.
 * Split the first half into one segment third split: -----. Done
 */
internal fun splitInEvenSegments(
    cubicToSplit: Cubic,
    numberSegments: Int,
    finishedSplits: Array<Cubic>,
): Array<Cubic> {
    if (numberSegments == 1) {
        finishedSplits[0] = cubicToSplit
        return finishedSplits
    }

    val relativeSegmentLength = 1.0f / numberSegments
    val splitPoint = ((numberSegments - 1) * relativeSegmentLength)

    val splitCubics = cubicToSplit.split(splitPoint)
    finishedSplits[numberSegments - 1] = splitCubics.second
    return splitInEvenSegments(splitCubics.first, numberSegments - 1, finishedSplits)
}

internal fun Cubic.pointOnCurve(t: Float): Offset {
    val u = 1 - t
    return Offset(
        anchor0X * (u * u * u) +
            control0X * (3 * t * u * u) +
            control1X * (3 * t * t * u) +
            anchor1X * (t * t * t),
        anchor0Y * (u * u * u) +
            control0Y * (3 * t * u * u) +
            control1Y * (3 * t * t * u) +
            anchor1Y * (t * t * t),
    )
}
