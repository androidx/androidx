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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import org.junit.Assert

/**
 * Class used to capture information regarding measure/layout.
 * This is used through CurvedModifier.spy, and captures information on that point in the modifier
 * chain.
 * This is also the single point of access to the internals of CurvedLayout, so tests are easier to
 * refactor if we change something there.
 */
internal data class CapturedInfo(
    // Counters
    var measuresCount: Int = 0,
    var layoutsCount: Int = 0,
    var drawCount: Int = 0
) {
    // Captured information
    var lastLayoutInfo: CurvedLayoutInfo? = null
    var parentOuterRadius: Float = 0f
    var parentThickness: Float = 0f
    var parentStartAngleRadians: Float = 0f
    var parentSweepRadians: Float = 0f

    fun reset() {
        measuresCount = 0
        layoutsCount = 0
        drawCount = 0
        lastLayoutInfo = null
        parentOuterRadius = 0f
        parentThickness = 0f
        parentStartAngleRadians = 0f
        parentSweepRadians = 0f
    }
}

internal const val FINE_FLOAT_TOLERANCE = 0.001f

internal fun CapturedInfo.checkDimensions(
    expectedAngleDegrees: Float? = null,
    expectedThicknessPx: Float? = null
) {
    if (expectedAngleDegrees != null) {
        Assert.assertEquals(
            expectedAngleDegrees,
            lastLayoutInfo!!.sweepRadians.toDegrees(),
            FINE_FLOAT_TOLERANCE
        )
    }
    if (expectedThicknessPx != null) {
        Assert.assertEquals(
            expectedThicknessPx,
            lastLayoutInfo!!.thickness,
            FINE_FLOAT_TOLERANCE
        )
    }
}

internal fun CapturedInfo.checkParentDimensions(
    expectedAngleDegrees: Float? = null,
    expectedThicknessPx: Float? = null,
) {
    if (expectedAngleDegrees != null) {
        Assert.assertEquals(
            expectedAngleDegrees,
            parentSweepRadians.toDegrees(),
            FINE_FLOAT_TOLERANCE
        )
    }
    if (expectedThicknessPx != null) {
        Assert.assertEquals(
            expectedThicknessPx,
            parentThickness,
            FINE_FLOAT_TOLERANCE
        )
    }
}

internal fun CapturedInfo.checkPositionOnParent(
    expectedAngularPositionDegrees: Float,
    expectedRadialPositionPx: Float
) {
    Assert.assertEquals(
        expectedAngularPositionDegrees,
        (lastLayoutInfo!!.startAngleRadians - parentStartAngleRadians).toDegrees(),
        FINE_FLOAT_TOLERANCE
    )
    Assert.assertEquals(
        expectedRadialPositionPx,
        parentOuterRadius - lastLayoutInfo!!.outerRadius,
        FINE_FLOAT_TOLERANCE
    )
}

internal fun CurvedModifier.spy(capturedInfo: CapturedInfo) =
    this.then { wrapped -> SpyCurvedChildWrapper(capturedInfo, wrapped) }

internal class SpyCurvedChildWrapper(private val capturedInfo: CapturedInfo, wrapped: CurvedChild) :
    BaseCurvedChildWrapper(wrapped) {

    override fun MeasureScope.initializeMeasure(
        measurables: List<Measurable>,
        index: Int
    ): Int = with(wrapped) {
        capturedInfo.measuresCount++
        initializeMeasure(measurables, index)
    }

    override fun doRadialPosition(
        parentOuterRadius: Float,
        parentThickness: Float,
    ): PartialLayoutInfo {
        capturedInfo.parentOuterRadius = parentOuterRadius
        capturedInfo.parentThickness = parentThickness
        return wrapped.radialPosition(
            parentOuterRadius,
            parentThickness,
        )
    }

    override fun doAngularPosition(
        parentStartAngleRadians: Float,
        parentSweepRadians: Float,
        centerOffset: Offset
    ): Float {
        capturedInfo.parentStartAngleRadians = parentStartAngleRadians
        capturedInfo.parentSweepRadians = parentSweepRadians
        return wrapped.angularPosition(
            parentStartAngleRadians,
            parentSweepRadians,
            centerOffset
        )
    }

    override fun (Placeable.PlacementScope).placeIfNeeded() = with(wrapped) {
        capturedInfo.lastLayoutInfo = layoutInfo
        capturedInfo.layoutsCount++
        placeIfNeeded()
    }

    override fun DrawScope.draw() = with(wrapped) {
        capturedInfo.lastLayoutInfo = layoutInfo
        capturedInfo.drawCount++
        draw()
    }
}
