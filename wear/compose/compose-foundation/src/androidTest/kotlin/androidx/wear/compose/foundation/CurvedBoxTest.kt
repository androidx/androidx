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

package androidx.wear.compose.foundation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.testutils.assertDoesNotContainColor
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class CurvedBoxTest {
    @get:Rule
    val rule = createComposeRule()

    @RequiresApi(Build.VERSION_CODES.O)
    @Test
    fun first_item_covered_by_second() {
        rule.setContent {
            CurvedLayout(
                modifier = Modifier.testTag(TEST_TAG)
            ) {
                curvedBox {
                    curvedComposable {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.Red)
                        )
                    }

                    curvedComposable {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.Green)
                        )
                    }
                }
            }
        }

        rule.onNodeWithTag(TEST_TAG)
            .captureToImage()
            .assertDoesNotContainColor(Color.Red)
    }

    @Test
    fun box_with_center_alignment() {
        radial_alignment_test(
            radialAlignment = CurvedAlignment.Radial.Center,
            angularAlignment = CurvedAlignment.Angular.Center
        ) { bigBoxDimension, smallBoxDimension ->
            Assert.assertEquals(
                bigBoxDimension.centerRadius,
                smallBoxDimension.centerRadius,
                FLOAT_TOLERANCE
            )
            Assert.assertEquals(
                bigBoxDimension.middleAngle,
                smallBoxDimension.middleAngle,
                FLOAT_TOLERANCE
            )
        }
    }

    @Test
    fun box_with_radial_center_angular_start_alignment() {
        radial_alignment_test(
            radialAlignment = CurvedAlignment.Radial.Center,
            angularAlignment = CurvedAlignment.Angular.Start
        ) { bigBoxDimension, smallBoxDimension ->
            Assert.assertEquals(
                bigBoxDimension.centerRadius,
                smallBoxDimension.centerRadius,
                FLOAT_TOLERANCE
            )
            Assert.assertEquals(
                bigBoxDimension.startAngle,
                smallBoxDimension.startAngle,
                FLOAT_TOLERANCE
            )
        }
    }

    @Test
    fun box_with_radial_center_angular_end_alignment() {
        radial_alignment_test(
            radialAlignment = CurvedAlignment.Radial.Center,
            angularAlignment = CurvedAlignment.Angular.End
        ) { bigBoxDimension, smallBoxDimension ->
            Assert.assertEquals(
                bigBoxDimension.centerRadius,
                smallBoxDimension.centerRadius,
                FLOAT_TOLERANCE
            )
            Assert.assertEquals(
                bigBoxDimension.endAngle,
                smallBoxDimension.endAngle,
                FLOAT_TOLERANCE
            )
        }
    }

    @Test
    fun box_with_radial_inner_angular_start_alignment() {
        radial_alignment_test(
            radialAlignment = CurvedAlignment.Radial.Inner,
            angularAlignment = CurvedAlignment.Angular.Start
        ) { bigBoxDimension, smallBoxDimension ->
            Assert.assertEquals(
                bigBoxDimension.innerRadius,
                smallBoxDimension.innerRadius,
                FLOAT_TOLERANCE
            )
            Assert.assertEquals(
                bigBoxDimension.startAngle,
                smallBoxDimension.startAngle,
                FLOAT_TOLERANCE
            )
        }
    }

    @Test
    fun box_with_radial_outer_angular_center_alignment() {
        radial_alignment_test(
            radialAlignment = CurvedAlignment.Radial.Outer,
            angularAlignment = CurvedAlignment.Angular.Center
        ) { bigBoxDimension, smallBoxDimension ->
            Assert.assertEquals(
                bigBoxDimension.outerRadius,
                smallBoxDimension.outerRadius,
                FLOAT_TOLERANCE
            )
            Assert.assertEquals(
                bigBoxDimension.middleAngle,
                smallBoxDimension.middleAngle,
                FLOAT_TOLERANCE
            )
        }
    }

    @Test
    fun box_with_radial_outer_angular_start_alignment() {
        radial_alignment_test(
            radialAlignment = CurvedAlignment.Radial.Outer,
            angularAlignment = CurvedAlignment.Angular.Start
        ) { bigBoxDimension, smallBoxDimension ->
            Assert.assertEquals(
                bigBoxDimension.outerRadius,
                smallBoxDimension.outerRadius,
                FLOAT_TOLERANCE
            )
            Assert.assertEquals(
                bigBoxDimension.startAngle,
                smallBoxDimension.startAngle,
                FLOAT_TOLERANCE
            )
        }
    }

    private fun radial_alignment_test(
        radialAlignment: CurvedAlignment.Radial? = null,
        angularAlignment: CurvedAlignment.Angular? = null,
        checker: (bigBoxDimensions: RadialDimensions, smallBoxDimensions: RadialDimensions) -> Unit
    ) {
        var rowCoords: LayoutCoordinates? = null
        var smallBoxCoords: LayoutCoordinates? = null
        var bigBoxCoords: LayoutCoordinates? = null
        val smallSpy = CapturedInfo()
        val bigSpy = CapturedInfo()
        // We have a big box and a small box with the specified alignment
        rule.setContent {
            CurvedLayout(
                modifier = Modifier.onGloballyPositioned { rowCoords = it }
            ) {
                curvedBox(
                    radialAlignment = radialAlignment,
                    angularAlignment = angularAlignment
                ) {
                    curvedComposable(
                        modifier = CurvedModifier.spy(bigSpy),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(45.dp)
                                .onGloballyPositioned { bigBoxCoords = it }
                        )
                    }

                    curvedComposable(
                        modifier = CurvedModifier.spy(smallSpy),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .onGloballyPositioned { smallBoxCoords = it }
                        )
                    }
                }
            }
        }

        rule.runOnIdle {
            val bigBoxDimensions = RadialDimensions(
                absoluteClockwise = true,
                rowCoords!!,
                bigBoxCoords!!
            )
            checkSpy(bigBoxDimensions, bigSpy)

            val smallBoxDimensions = RadialDimensions(
                absoluteClockwise = true,
                rowCoords!!,
                smallBoxCoords!!
            )
            checkSpy(smallBoxDimensions, smallSpy)

            Assert.assertTrue(bigBoxDimensions.sweep > smallBoxDimensions.sweep)
            Assert.assertTrue(bigBoxDimensions.thickness > smallBoxDimensions.thickness)

            checker(bigBoxDimensions, smallBoxDimensions)
        }
    }
}
