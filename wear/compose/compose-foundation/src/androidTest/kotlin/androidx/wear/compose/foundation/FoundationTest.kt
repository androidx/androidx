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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.LayoutCoordinates
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.min
import kotlin.test.assertTrue
import org.junit.Assert

internal const val TEST_TAG = "test-item"

// When components are laid out, position is specified by integers, so we can't expect
// much precision.
internal const val FLOAT_TOLERANCE = 1f

/**
 * Checks whether [expectedColor] does not exist in current [ImageBitmap]
 *
fun ImageBitmap.assertDoesNotContainColor(expectedColor: Color) {
    val histogram = histogram()
    if (histogram.containsKey(expectedColor)) {
        throw AssertionError("Expected color $expectedColor exists in current bitmap")
    }
}*/

/**
 * Checks whether [expectedColor] exist in current [ImageBitmap], covering at least the given ratio
 * of the image
 */
fun ImageBitmap.assertDoesContainColor(expectedColor: Color, expectedRatio: Float = 0.75f) {
    val histogram = histogram()
    val ratio = (histogram.getOrDefault(expectedColor, 0L)).toFloat() / (width * height)
    if (ratio < expectedRatio) {
        throw AssertionError("Expected color $expectedColor with ratio $expectedRatio." +
            " Actual ratio = $ratio")
    }
}

private fun ImageBitmap.histogram(): MutableMap<Color, Long> {
    val pixels = this.toPixelMap()
    val histogram = mutableMapOf<Color, Long>()
    for (x in 0 until width) {
        for (y in 0 until height) {
            val color = pixels[x, y]
            histogram[color] = histogram.getOrDefault(color, 0) + 1
        }
    }
    return histogram
}

internal fun checkSpy(dimensions: RadialDimensions, capturedInfo: CapturedInfo) =
    checkCurvedLayoutInfo(dimensions.asCurvedLayoutInfo(), capturedInfo.lastLayoutInfo!!)

private fun checkCurvedLayoutInfo(expected: CurvedLayoutInfo, actual: CurvedLayoutInfo) {
    checkAngle(expected.sweepRadians.toDegrees(), actual.sweepRadians.toDegrees())
    Assert.assertEquals(expected.outerRadius, actual.outerRadius, FLOAT_TOLERANCE)
    Assert.assertEquals(expected.thickness, actual.thickness, FLOAT_TOLERANCE)
    Assert.assertEquals(expected.centerOffset.x, actual.centerOffset.x, FLOAT_TOLERANCE)
    Assert.assertEquals(expected.centerOffset.y, actual.centerOffset.y, FLOAT_TOLERANCE)
    checkAngle(expected.startAngleRadians.toDegrees(), actual.startAngleRadians.toDegrees())
}

internal data class RadialPoint(val distance: Float, val angle: Float)

// Utility class to compute the dimensions of the annulus segment corresponding to a given component
// given that component's and the parent CurvedRow's LayoutCoordinates, and a boolean to indicate
// if the layout is clockwise or counterclockwise
internal class RadialDimensions(
    absoluteClockwise: Boolean,
    rowCoords: LayoutCoordinates,
    coords: LayoutCoordinates
) {
    // Row dimmensions
    val rowCenter: Offset
    val rowRadius: Float

    // Component dimensions.
    val innerRadius: Float
    val outerRadius: Float
    val centerRadius
        get() = (innerRadius + outerRadius) / 2
    val sweep: Float
    val startAngle: Float
    val middleAngle: Float
    val endAngle: Float
    val thickness: Float

    init {
        // Find the radius and center of the CurvedRow, all radial coordinates are relative to this
        // center
        rowRadius = min(rowCoords.size.width, rowCoords.size.height) / 2f
        rowCenter = rowCoords.localToRoot(
            Offset(rowRadius, rowRadius)
        )

        // Compute the radial coordinates (relative to the center of the CurvedRow) of the found
        // corners of the component's box and its center
        val width = coords.size.width.toFloat()
        val height = coords.size.height.toFloat()

        val topLeft = toRadialCoordinates(coords, 0f, 0f)
        val topRight = toRadialCoordinates(coords, width, 0f)
        val center = toRadialCoordinates(coords, width / 2f, height / 2f)
        val bottomLeft = toRadialCoordinates(coords, 0f, height)
        val bottomRight = toRadialCoordinates(coords, width, height)

        // Ensure the bottom corners are in the same circle
        Assert.assertEquals(bottomLeft.distance, bottomRight.distance, FLOAT_TOLERANCE)
        // Same with top corners
        Assert.assertEquals(topLeft.distance, topRight.distance, FLOAT_TOLERANCE)

        // Compute the four dimensions of the annulus sector
        // Note that startAngle is always before endAngle (even when going counterclockwise)
        if (absoluteClockwise) {
            innerRadius = bottomLeft.distance
            outerRadius = topLeft.distance
            startAngle = bottomLeft.angle.toDegrees()
            endAngle = bottomRight.angle.toDegrees()
        } else {
            // When components are laid out counterclockwise, they are rotated 180 degrees
            innerRadius = topLeft.distance
            outerRadius = bottomLeft.distance
            startAngle = topRight.angle.toDegrees()
            endAngle = topLeft.angle.toDegrees()
        }

        middleAngle = center.angle.toDegrees()
        sweep = if (endAngle > startAngle) {
            endAngle - startAngle
        } else {
            endAngle + 360f - startAngle
        }

        thickness = outerRadius - innerRadius

        // All sweep angles are well between 0 and 90
        assertTrue(
            (FLOAT_TOLERANCE..90f - FLOAT_TOLERANCE).contains(sweep),
            "sweep = $sweep"
        )

        // The outerRadius is greater than the innerRadius
        assertTrue(
            outerRadius > innerRadius + FLOAT_TOLERANCE,
            "innerRadius = $innerRadius, outerRadius = $outerRadius"
        )
    }

    // TODO: When we finalize CurvedLayoutInfo's API, eliminate the RadialDimensions class and
    // inline this function to directly convert between LayoutCoordinates and CurvedLayoutInfo.
    fun asCurvedLayoutInfo() = CurvedLayoutInfo(
        sweepRadians = sweep.toRadians(),
        outerRadius = outerRadius,
        thickness = outerRadius - innerRadius,
        centerOffset = rowCenter,
        measureRadius = (outerRadius + innerRadius) / 2,
        startAngleRadians = startAngle.toRadians()
    )

    fun toRadialCoordinates(coords: LayoutCoordinates, x: Float, y: Float): RadialPoint {
        val vector = coords.localToRoot(Offset(x, y)) - rowCenter
        return RadialPoint(vector.getDistance(), atan2(vector.y, vector.x))
    }
}

internal fun checkAngle(expected: Float, actual: Float) {
    var d = abs(expected - actual)
    d = min(d, 360 - d)
    if (d > FLOAT_TOLERANCE) {
        Assert.fail("Angle is out of tolerance. Expected: $expected, actual: $actual")
    }
}
