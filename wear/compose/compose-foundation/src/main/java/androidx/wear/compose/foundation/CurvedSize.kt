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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Specify the dimensions of the content to be restricted between the given bounds.
 *
 * @param minSweepDegrees the minimum angle (in degrees) for the content.
 * @param maxSweepDegrees the maximum angle (in degrees) for the content.
 * @param minThickness the minimum thickness (radial size) for the content.
 * @param maxThickness the maximum thickness (radial size) for the content.
 */
public fun CurvedModifier.sizeIn(
    /* @FloatRange(from = 0f, to = 360f) */
    minSweepDegrees: Float = 0f,
    /* @FloatRange(from = 0f, to = 360f) */
    maxSweepDegrees: Float = 360f,
    minThickness: Dp = 0.dp,
    maxThickness: Dp = Dp.Infinity,
) = this.then { child ->
    SweepSizeWrapper(
        child,
        minSweepDegrees = minSweepDegrees,
        maxSweepDegrees = maxSweepDegrees,
        minThickness = minThickness,
        maxThickness = maxThickness
    )
}

/**
 * Specify the dimensions (sweep and thickness) for the content.
 *
 * @sample androidx.wear.compose.foundation.samples.CurvedFixedSize
 *
 * @param sweepDegrees Indicates the sweep (angular size) of the content.
 * @param thickness Indicates the thickness (radial size) of the content.
 */
public fun CurvedModifier.size(sweepDegrees: Float, thickness: Dp) = sizeIn(
    /* @FloatRange(from = 0f, to = 360f) */
    minSweepDegrees = sweepDegrees,
    /* @FloatRange(from = 0f, to = 360f) */
    maxSweepDegrees = sweepDegrees,
    minThickness = thickness,
    maxThickness = thickness
)

/**
 * Specify the sweep (arc length) for the content in Dp. The arc length will be measured
 * at the center of the item, except for [basicCurvedText], where it will be
 * measured at the text baseline.
 *
 * @sample androidx.wear.compose.foundation.samples.CurvedFixedSize
 *
 * @param angularWidth Indicates the arc length of the content in Dp.
 */
public fun CurvedModifier.angularSizeDp(angularWidth: Dp) = this.then { child ->
    AngularWidthSizeWrapper(
        child,
        minAngularWidth = angularWidth,
        maxAngularWidth = angularWidth,
        minThickness = 0.dp,
        maxThickness = Dp.Infinity
    )
}

/**
 * Specify the sweep (angular size) for the content.
 *
 * @param sweepDegrees Indicates the sweep (angular size) of the content.
 */
public fun CurvedModifier.angularSize(sweepDegrees: Float) = sizeIn(
    minSweepDegrees = sweepDegrees,
    maxSweepDegrees = sweepDegrees
)

/**
 * Specify the radialSize (thickness) for the content.
 *
 * @param thickness Indicates the thickness of the content.
 */
public fun CurvedModifier.radialSize(thickness: Dp) = sizeIn(
    minThickness = thickness,
    maxThickness = thickness
)

internal class SweepSizeWrapper(
    child: CurvedChild,
    val minSweepDegrees: Float,
    val maxSweepDegrees: Float,
    minThickness: Dp,
    maxThickness: Dp,
) : BaseSizeWrapper(child, minThickness, maxThickness) {
    override fun CurvedMeasureScope.initializeMeasure(
        measurables: Iterator<Measurable>
    ) {
        baseInitializeMeasure(measurables)
    }

    override fun calculateSweepRadians(partialLayoutInfo: PartialLayoutInfo): Float =
        partialLayoutInfo.sweepRadians.coerceIn(
            minSweepDegrees.toRadians(),
            maxSweepDegrees.toRadians()
        )
}

internal class AngularWidthSizeWrapper(
    child: CurvedChild,
    val minAngularWidth: Dp,
    val maxAngularWidth: Dp,
    minThickness: Dp,
    maxThickness: Dp
) : BaseSizeWrapper(child, minThickness, maxThickness) {

    private var minAngularWidthPx = 0f
    private var maxAngularWidthPx = 0f
    override fun CurvedMeasureScope.initializeMeasure(
        measurables: Iterator<Measurable>
    ) {
        minAngularWidthPx = minAngularWidth.toPx()
        maxAngularWidthPx = maxAngularWidth.toPx()

        baseInitializeMeasure(measurables)
    }

    override fun calculateSweepRadians(partialLayoutInfo: PartialLayoutInfo): Float =
        partialLayoutInfo.sweepRadians.coerceIn(
            minAngularWidthPx / partialLayoutInfo.measureRadius,
            maxAngularWidthPx / partialLayoutInfo.measureRadius
        )
}

internal abstract class BaseSizeWrapper(
    child: CurvedChild,
    val minThickness: Dp,
    val maxThickness: Dp,
) : BaseCurvedChildWrapper(child) {
    private var minThicknessPx = 0f
    private var maxThicknessPx = 0f

    protected fun CurvedMeasureScope.baseInitializeMeasure(
        measurables: Iterator<Measurable>
    ) {
        minThicknessPx = minThickness.toPx()
        maxThicknessPx = maxThickness.toPx()
        with(wrapped) {
            // Call initializeMeasure on wrapper (while still having the MeasureScope scope)
            initializeMeasure(measurables)
        }
    }

    override fun doEstimateThickness(maxRadius: Float) =
        wrapped.estimateThickness(maxRadius).coerceIn(minThicknessPx, maxThicknessPx)

    protected abstract fun calculateSweepRadians(partialLayoutInfo: PartialLayoutInfo): Float
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

    override fun doRadialPosition(
        parentOuterRadius: Float,
        parentThickness: Float
    ): PartialLayoutInfo {
        val partialLayoutInfo = wrapped.radialPosition(
            parentOuterRadius,
            estimatedThickness
        )
        return PartialLayoutInfo(
            calculateSweepRadians(partialLayoutInfo),
            parentOuterRadius,
            thickness = estimatedThickness,
            measureRadius = partialLayoutInfo.measureRadius +
                partialLayoutInfo.outerRadius - parentOuterRadius
        )
    }
}
