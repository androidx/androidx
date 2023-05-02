/*
 * Copyright 2021 The Android Open Source Project
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
@file:JvmName("DisplayFeatureTesting")

package androidx.window.testing.layout

import android.app.Activity
import android.graphics.Rect
import androidx.window.layout.FoldingFeature
import androidx.window.layout.FoldingFeature.OcclusionType.Companion.FULL
import androidx.window.layout.FoldingFeature.OcclusionType.Companion.NONE
import androidx.window.layout.FoldingFeature.Orientation
import androidx.window.layout.FoldingFeature.Orientation.Companion.HORIZONTAL
import androidx.window.layout.FoldingFeature.Orientation.Companion.VERTICAL
import androidx.window.layout.FoldingFeature.State
import androidx.window.layout.FoldingFeature.State.Companion.HALF_OPENED
import androidx.window.layout.WindowMetricsCalculator

/**
 * A convenience method to get a test fold with default values provided. With the default values
 * it returns a [FoldingFeature.State.HALF_OPENED] feature that splits the screen along the
 * [FoldingFeature.Orientation.HORIZONTAL] axis.
 *
 * The bounds of the feature are calculated based on [orientation] and [size]. If the
 * feature is [VERTICAL] then the feature is centered horizontally. The top-left x-coordinate is
 * center - ([size] / 2) and the top-right x-coordinate is center + ([size] / 2). If the feature is
 * [HORIZONTAL] then the feature is centered vertically. The top-left y-coordinate is center -
 * ([size] / 2) and the bottom-left y-coordinate is center - ([size] / 2). The folding features
 * always cover the window in one dimension and that determines the other coordinates.
 *
 * @param activity that will house the [FoldingFeature].
 * @param center the center of the fold complementary to the orientation. For a [HORIZONTAL] fold,
 * this is the y-axis and for a [VERTICAL] fold this is the x-axis.
 * @param size the smaller dimension  of the fold. The larger dimension  always covers the entire
 * window.
 * @param state [State] of the fold. The default value is [HALF_OPENED]
 * @param orientation [Orientation] of the fold. The default value is [HORIZONTAL]
 * @return [FoldingFeature] that is splitting if the width is not 0 and runs parallel to the
 * [Orientation] axis.
 */
@JvmOverloads
@JvmName("createFoldingFeature")
fun FoldingFeature(
    activity: Activity,
    center: Int = -1,
    size: Int = 0,
    state: State = HALF_OPENED,
    orientation: Orientation = HORIZONTAL
): FoldingFeature {
    val metricsCalculator = WindowMetricsCalculator.getOrCreate()
    val windowBounds = metricsCalculator.computeCurrentWindowMetrics(activity).bounds
    return foldingFeatureInternal(
        windowBounds = windowBounds,
        center = center,
        size = size,
        state = state,
        orientation = orientation
    )
}

/**
 * A convenience method to get a test [FoldingFeature] with default values provided. With the
 * default values it returns a [FoldingFeature.State.HALF_OPENED] feature that splits the screen
 * along the [FoldingFeature.Orientation.HORIZONTAL] axis.
 *
 * The bounds of the feature are calculated based on [orientation] and [size]. If the
 * feature is [VERTICAL] then the feature is centered horizontally. The top-left x-coordinate is
 * center - ([size] / 2) and the top-right x-coordinate is center + ([size] / 2). If the feature is
 * [HORIZONTAL] then the feature is centered vertically. The top-left y-coordinate is center -
 * ([size] / 2) and the bottom-left y-coordinate is center - ([size] / 2). The folding features
 * always cover the window in one dimension and that determines the other coordinates.
 *
 * @param windowBounds that will contain the [FoldingFeature].
 * @param center the center of the fold complementary to the orientation. For a [HORIZONTAL] fold,
 * this is the y-axis and for a [VERTICAL] fold this is the x-axis.
 * @param size the smaller dimension  of the fold. The larger dimension  always covers the entire
 * window.
 * @param state [State] of the fold. The default value is [HALF_OPENED]
 * @param orientation [Orientation] of the fold. The default value is [HORIZONTAL]
 * @return [FoldingFeature] that is splitting if the width is not 0 and runs parallel to the
 * [Orientation] axis.
 */
@JvmOverloads
@JvmName("createFoldingFeature")
fun FoldingFeature(
    windowBounds: Rect,
    center: Int = -1,
    size: Int = 0,
    state: State = HALF_OPENED,
    orientation: Orientation = HORIZONTAL
): FoldingFeature {
    return foldingFeatureInternal(windowBounds, center, size, state, orientation)
}

private fun foldingFeatureInternal(
    windowBounds: Rect,
    center: Int = -1,
    size: Int = 0,
    state: State = HALF_OPENED,
    orientation: Orientation = HORIZONTAL
): FoldingFeature {
    val shouldTreatAsHinge = size != 0
    val isSeparating = shouldTreatAsHinge || state == HALF_OPENED
    val offset = size / 2
    val actualCenter = if (center < 0) {
        when (orientation) {
            HORIZONTAL -> windowBounds.centerY()
            VERTICAL -> windowBounds.centerX()
            else -> windowBounds.centerX()
        }
    } else {
        center
    }
    val start = actualCenter - offset
    val end = actualCenter + offset
    val bounds = if (orientation == VERTICAL) {
        val windowHeight = windowBounds.height()
        Rect(start, 0, end, windowHeight)
    } else {
        val windowWidth = windowBounds.width()
        Rect(0, start, windowWidth, end)
    }
    val occlusionType = if (shouldTreatAsHinge) {
        FULL
    } else {
        NONE
    }
    return FakeFoldingFeature(
        bounds = bounds,
        isSeparating = isSeparating,
        occlusionType = occlusionType,
        orientation = orientation,
        state = state
    )
}

private class FakeFoldingFeature(
    override val bounds: Rect,
    override val isSeparating: Boolean,
    override val occlusionType: FoldingFeature.OcclusionType,
    override val orientation: Orientation,
    override val state: State
) : FoldingFeature {
    init {
        require(!(bounds.width() == 0 && bounds.height() == 0)) { "Bounds must be non zero" }
        require(!(bounds.left != 0 && bounds.top != 0)) {
            "Bounding rectangle must start at the top or left window edge for folding features"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FakeFoldingFeature

        if (bounds != other.bounds) return false
        if (isSeparating != other.isSeparating) return false
        if (occlusionType != other.occlusionType) return false
        if (orientation != other.orientation) return false
        if (state != other.state) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bounds.hashCode()
        result = 31 * result + isSeparating.hashCode()
        result = 31 * result + occlusionType.hashCode()
        result = 31 * result + orientation.hashCode()
        result = 31 * result + state.hashCode()
        return result
    }

    override fun toString(): String {
        return "${FakeFoldingFeature::class.java.simpleName} { bounds = $bounds, isSeparating = " +
            "$isSeparating, occlusionType = $occlusionType, orientation = $orientation, state = " +
            "$state"
    }
}