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

package androidx.wear.compose.foundation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.filters.FlakyTest
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.min
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test

// When components are laid out, position is specified by integers, so we can't expect
// much precision.
internal const val FLOAT_TOLERANCE = 1f

class CurvedLayoutTest {
    @get:Rule
    val rule = createComposeRule()

    private fun anchor_and_clockwise_test(
        anchor: Float,
        anchorType: AnchorType,
        clockwise: Boolean,
        initialAnchorType: AnchorType = anchorType
    ) {
        var rowCoords: LayoutCoordinates? = null
        var coords: LayoutCoordinates? = null
        var anchorTypeState by mutableStateOf(initialAnchorType)

        rule.setContent {
            CurvedLayout(
                modifier = Modifier.size(200.dp)
                    .onGloballyPositioned { rowCoords = it },
                anchor = anchor,
                anchorType = anchorTypeState,
                clockwise = clockwise
            ) {
                curvedComposable {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .onGloballyPositioned { coords = it }
                    )
                }
            }

            if (anchorType != initialAnchorType) {
                LaunchedEffect(true) {
                    anchorTypeState = anchorType
                }
            }
        }

        rule.runOnIdle {
            val dims = RadialDimensions(
                clockwise = clockwise,
                rowCoords!!,
                coords!!
            )

            // It's at the outer side of the CurvedRow,
            assertEquals(dims.rowRadius, dims.outerRadius, FLOAT_TOLERANCE)

            val actualAngle = if (anchorType == AnchorType.Center) {
                dims.middleAngle
            } else {
                if (anchorType == AnchorType.Start == clockwise) {
                    dims.startAngle
                } else {
                    dims.endAngle
                }
            }
            checkAngle(anchor, actualAngle)
        }
    }

    @Test
    fun correctly_uses_anchortype_start_clockwise() =
        anchor_and_clockwise_test(0f, AnchorType.Start, true)

    @Test
    fun correctly_uses_anchortype_center_clockwise() =
        anchor_and_clockwise_test(60f, AnchorType.Center, true)

    @Test
    fun correctly_uses_anchortype_end_clockwise() =
        anchor_and_clockwise_test(120f, AnchorType.End, true)

    @Test
    fun correctly_uses_anchortype_start_anticlockwise() =
        anchor_and_clockwise_test(180f, AnchorType.Start, false)

    @Test
    fun correctly_uses_anchortype_center_anticlockwise() =
        anchor_and_clockwise_test(240f, AnchorType.Center, false)

    @Test
    fun correctly_uses_anchortype_end_anticlockwise() =
        anchor_and_clockwise_test(300f, AnchorType.End, false)

    @Test
    fun switched_anchortype_center_to_end_anticlockwise() =
        anchor_and_clockwise_test(
            0f,
            AnchorType.End,
            false,
            initialAnchorType = AnchorType.Center
        )

    @Test
    fun switched_anchortype_center_to_start_anticlockwise() =
        anchor_and_clockwise_test(
            60f,
            AnchorType.Start,
            false,
            initialAnchorType = AnchorType.Center
        )

    @Test
    fun switched_anchortype_end_to_center_anticlockwise() =
        anchor_and_clockwise_test(
            120f,
            AnchorType.Center,
            false,
            initialAnchorType = AnchorType.End
        )

    @Test
    fun switched_anchortype_end_to_start_clockwise() =
        anchor_and_clockwise_test(
            180f,
            AnchorType.Start,
            true,
            initialAnchorType = AnchorType.End
        )

    @Test
    fun lays_out_multiple_children_correctly() {
        var rowCoords: LayoutCoordinates? = null
        val coords = Array<LayoutCoordinates?>(3) { null }
        rule.setContent {
            CurvedLayout(
                modifier = Modifier.onGloballyPositioned { rowCoords = it }
            ) {
                repeat(3) { ix ->
                    curvedComposable {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .onGloballyPositioned { coords[ix] = it }
                        )
                    }
                }
            }
        }

        rule.runOnIdle {
            val dims = coords.map {
                RadialDimensions(
                    clockwise = true,
                    rowCoords!!,
                    it!!
                )
            }

            dims.forEach {
                // They are all at the outer side of the CurvedRow,
                // and have the same innerRadius and sweep
                assertEquals(it.rowRadius, it.outerRadius, FLOAT_TOLERANCE)
                assertEquals(dims[0].innerRadius, it.innerRadius, FLOAT_TOLERANCE)
                assertEquals(dims[0].sweep, it.sweep, FLOAT_TOLERANCE)
            }
            // There are one after another, the middle child is centered at 12 o clock
            checkAngle(dims[0].endAngle, dims[1].startAngle)
            checkAngle(dims[1].endAngle, dims[2].startAngle)
            checkAngle(270f, dims[1].middleAngle)
        }
    }

    private fun radial_alignment_test(
        radialAlignment: CurvedAlignment.Radial,
        checker: (bigBoxDimensions: RadialDimensions, smallBoxDimensions: RadialDimensions) -> Unit
    ) {
        var rowCoords: LayoutCoordinates? = null
        var smallBoxCoords: LayoutCoordinates? = null
        var bigBoxCoords: LayoutCoordinates? = null
        // We have a big box and a small box with the specified alignment
        rule.setContent {
            CurvedLayout(
                modifier = Modifier.onGloballyPositioned { rowCoords = it }
            ) {
                curvedComposable(radialAlignment = radialAlignment) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .onGloballyPositioned { smallBoxCoords = it }
                    )
                }
                curvedComposable {
                    Box(
                        modifier = Modifier
                            .size(45.dp)
                            .onGloballyPositioned { bigBoxCoords = it }
                    )
                }
            }
        }

        rule.runOnIdle {
            val bigBoxDimensions = RadialDimensions(
                clockwise = true,
                rowCoords!!,
                bigBoxCoords!!
            )

            val smallBoxDimensions = RadialDimensions(
                clockwise = true,
                rowCoords!!,
                smallBoxCoords!!
            )

            // There are one after another
            checkAngle(smallBoxDimensions.endAngle, bigBoxDimensions.startAngle)

            checker(bigBoxDimensions, smallBoxDimensions)
        }
    }

    @Test
    fun radial_alignment_outer_works() =
        radial_alignment_test(CurvedAlignment.Radial.Outer) { bigBoxDimension, smallBoxDimension ->
            assertEquals(
                bigBoxDimension.outerRadius,
                smallBoxDimension.outerRadius,
                FLOAT_TOLERANCE
            )
        }

    @Test
    fun radial_alignment_center_works() =
        radial_alignment_test(CurvedAlignment.Radial.Center) { bigBoxDimension, smallBoxDimension ->
            assertEquals(
                bigBoxDimension.centerRadius,
                smallBoxDimension.centerRadius,
                FLOAT_TOLERANCE
            )
        }

    @Test
    fun radial_alignment_inner_works() =
        radial_alignment_test(CurvedAlignment.Radial.Inner) { bigBoxDimension, smallBoxDimension ->
            assertEquals(
                bigBoxDimension.innerRadius,
                smallBoxDimension.innerRadius,
                FLOAT_TOLERANCE
            )
        }

    private fun visibility_change_test_setup(targetVisibility: Boolean) {
        val visible = mutableStateOf(!targetVisibility)
        rule.setContent {
            CurvedLayout {
                curvedComposable { Box(modifier = Modifier.size(30.dp)) }
                if (visible.value) {
                    curvedComposable { Box(modifier = Modifier.size(30.dp).testTag(TEST_TAG)) }
                }
                curvedComposable { Box(modifier = Modifier.size(30.dp)) }
            }
        }

        rule.runOnIdle {
            visible.value = targetVisibility
        }

        rule.waitForIdle()
        if (targetVisibility) {
            rule.onNodeWithTag(TEST_TAG).assertExists()
        } else {
            rule.onNodeWithTag(TEST_TAG).assertDoesNotExist()
        }
    }

    @Test
    @FlakyTest(bugId = 227314580)
    fun showing_child_works() = visibility_change_test_setup(true)

    @Test
    @FlakyTest(bugId = 227314580)
    fun hiding_child_works() = visibility_change_test_setup(false)
}

internal const val TEST_TAG = "test-item"

fun checkAngle(expected: Float, actual: Float) {
    var d = abs(expected - actual)
    d = min(d, 360 - d)
    if (d > FLOAT_TOLERANCE) {
        fail("Angle is out of tolerance. Expected: $expected, actual: $actual")
    }
}

private fun Float.toRadians() = this * PI.toFloat() / 180f
private fun Float.toDegrees() = this * 180f / PI.toFloat()

private data class RadialPoint(val distance: Float, val angle: Float)

// Utility class to compute the dimensions of the annulus segment corresponding to a given component
// given that component's and the parent CurvedRow's LayoutCoordinates, and a boolean to indicate
// if the layout is clockwise or counterclockwise
private class RadialDimensions(
    clockwise: Boolean,
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
        assertEquals(bottomLeft.distance, bottomRight.distance, FLOAT_TOLERANCE)
        // Same with top corners
        assertEquals(topLeft.distance, topRight.distance, FLOAT_TOLERANCE)

        // Compute the four dimensions of the annulus sector
        // Note that startAngle is always before endAngle (even when going counterclockwise)
        if (clockwise) {
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

        // All sweep angles are well between 0 and 90
        assert((FLOAT_TOLERANCE..90f - FLOAT_TOLERANCE).contains(sweep)) { "sweep = $sweep" }

        // The outerRadius is greater than the innerRadius
        assert(outerRadius > innerRadius + FLOAT_TOLERANCE) {
            "innerRadius = $innerRadius, outerRadius = $outerRadius"
        }
    }

    private fun toRadialCoordinates(coords: LayoutCoordinates, x: Float, y: Float): RadialPoint {
        val vector = coords.localToRoot(Offset(x, y)) - rowCenter
        return RadialPoint(vector.getDistance(), atan2(vector.y, vector.x))
    }
}