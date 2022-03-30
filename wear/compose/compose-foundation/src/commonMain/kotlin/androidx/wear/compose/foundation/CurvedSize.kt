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

package androidx.wear.compose.foundation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Specify the dimensions of the content to be restricted between the given bounds.
 *
 * @param angularMinDegrees the minimum angle (in degrees) for the content.
 * @param angularMaxDegrees the maximum angle (in degrees) for the content.
 * @param radialSizeMin the minimum radialSize (thickness) for the content.
 * @param radialSizeMax the maximum radialSize (thickness) for the content.
 */
public fun CurvedModifier.sizeIn(
    /* @FloatRange(from = 0f, to = 360f) */
    angularMinDegrees: Float = 0f,
    /* @FloatRange(from = 0f, to = 360f) */
    angularMaxDegrees: Float = 360f,
    radialSizeMin: Dp = 0.dp,
    radialSizeMax: Dp = Dp.Infinity,
) = this.then { child -> SizeWrapper(
    child,
    angularMinDegrees = angularMinDegrees,
    angularMaxDegrees = angularMaxDegrees,
    thicknessMin = radialSizeMin,
    thicknessMax = radialSizeMax
) }

/**
 * Specify the angular size and thickness for the content.
 *
 * @sample androidx.wear.compose.foundation.samples.CurvedFixedSize
 *
 * @param angularSizeDegrees Indicates the angular size (sweep) of the content.
 * @param radialSize Indicates the radialSize (thickness) of the content.
 */
public fun CurvedModifier.size(angularSizeDegrees: Float, radialSize: Dp) = sizeIn(
    /* @FloatRange(from = 0f, to = 360f) */
    angularMinDegrees = angularSizeDegrees,
    /* @FloatRange(from = 0f, to = 360f) */
    angularMaxDegrees = angularSizeDegrees,
    radialSizeMin = radialSize,
    radialSizeMax = radialSize
)

/**
 * Specify the angular size (sweep) for the content.
 *
 * @param angularSizeDegrees Indicates the angular size of the content.
 */
public fun CurvedModifier.angularSize(angularSizeDegrees: Float) = sizeIn(
    angularMinDegrees = angularSizeDegrees,
    angularMaxDegrees = angularSizeDegrees
)

/**
 * Specify the radialSize (thickness) for the content.
 *
 * @param radialSize Indicates the thickness of the content.
 */
public fun CurvedModifier.radialSize(radialSize: Dp) = sizeIn(
    radialSizeMin = radialSize,
    radialSizeMax = radialSize
)

internal class SizeWrapper(
    child: CurvedChild,
    val angularMinDegrees: Float,
    val angularMaxDegrees: Float,
    val thicknessMin: Dp,
    val thicknessMax: Dp,
) : BaseCurvedChildWrapper(child) {
    private var thicknessMinPx = 0f
    private var thicknessMaxPx = 0f

    override fun MeasureScope.initializeMeasure(
        measurables: List<Measurable>,
        index: Int
    ): Int {
        thicknessMinPx = thicknessMin.toPx()
        thicknessMaxPx = thicknessMax.toPx()
        return with(wrapped) {
            // Call initializeMeasure on wrapper (while still having the MeasureScope scope)
            initializeMeasure(measurables, index)
        }
    }

    override fun doEstimateThickness(maxRadius: Float) =
        wrapped.estimateThickness(maxRadius).coerceIn(thicknessMinPx, thicknessMaxPx)

    override fun doRadialPosition(
        parentOuterRadius: Float,
        parentThickness: Float
    ): PartialLayoutInfo {
        val partialLayoutInfo = wrapped.radialPosition(
            parentOuterRadius,
            estimatedThickness
        )
        return PartialLayoutInfo(
            partialLayoutInfo.sweepRadians.coerceIn(
                angularMinDegrees.toRadians(),
                angularMaxDegrees.toRadians()
            ),
            parentOuterRadius,
            thickness = estimatedThickness,
            measureRadius = partialLayoutInfo.measureRadius +
                partialLayoutInfo.outerRadius - parentOuterRadius
        )
    }

    override fun doAngularPosition(
        parentStartAngleRadians: Float,
        parentSweepRadians: Float,
        centerOffset: Offset
    ): Float {
        wrapped.angularPosition(
            parentStartAngleRadians,
            parentSweepRadians = sweepRadians,
            centerOffset
        )
        return parentStartAngleRadians
    }
}