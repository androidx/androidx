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

package androidx.xr.compose.subspace

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.size
import androidx.xr.compose.subspace.layout.testTag
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.TestSetup
import androidx.xr.compose.testing.assertHeightIsEqualTo
import androidx.xr.compose.testing.assertLeftPositionInRootIsEqualTo
import androidx.xr.compose.testing.assertRotationInRootIsEqualTo
import androidx.xr.compose.testing.assertTopPositionInRootIsEqualTo
import androidx.xr.compose.testing.assertWidthIsEqualTo
import androidx.xr.compose.testing.assertXPositionInRootIsEqualTo
import androidx.xr.compose.testing.assertYPositionInRootIsEqualTo
import androidx.xr.compose.testing.assertZPositionInRootIsEqualTo
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.runtime.math.Quaternion
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [SpatialRow] and [SpatialColumn]. */
@RunWith(AndroidJUnit4::class)
class SpatialRowColumnTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun spatialRowColumn_internalElementsAreLaidOutProperly() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialRow(SubspaceModifier.testTag("row1").width(10.dp)) {
                        // This column will get the first 7dp
                        SpatialColumn(SubspaceModifier.testTag("column1").width(7.dp)) {
                            SpatialPanel { Text(text = "Column 1") }
                        }
                        // There are only 3dp left, so this column will end up being 3dp
                        SpatialColumn(SubspaceModifier.testTag("column2").width(7.dp)) {
                            SpatialPanel { Text(text = "Column 2") }
                        }
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("row1")
            .assertLeftPositionInRootIsEqualTo(-5.dp)
            .assertXPositionInRootIsEqualTo(0.dp)
            .assertWidthIsEqualTo(10.dp)

        composeTestRule
            .onSubspaceNodeWithTag("column1")
            .assertLeftPositionInRootIsEqualTo(-5.dp)
            .assertWidthIsEqualTo(7.dp)

        composeTestRule
            .onSubspaceNodeWithTag("column2")
            .assertLeftPositionInRootIsEqualTo(2.dp)
            .assertWidthIsEqualTo(3.dp)
    }

    @Test
    fun spatialRow_internalElementsAreAligned() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialRow(
                        SubspaceModifier.testTag("row1").size(20.dp),
                        alignment = SpatialAlignment.CenterLeft,
                    ) {
                        SpatialColumn(SubspaceModifier.testTag("column1").size(5.dp)) {
                            SpatialPanel { Text(text = "Column 1") }
                        }
                        SpatialColumn(SubspaceModifier.testTag("column2").size(5.dp)) {
                            SpatialPanel { Text(text = "Column 2") }
                        }
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("row1")
            .assertLeftPositionInRootIsEqualTo(-10.dp)
            .assertXPositionInRootIsEqualTo(0.dp)
            .assertWidthIsEqualTo(20.dp)

        composeTestRule
            .onSubspaceNodeWithTag("column1")
            .assertLeftPositionInRootIsEqualTo(-10.dp)
            .assertWidthIsEqualTo(5.dp)

        composeTestRule
            .onSubspaceNodeWithTag("column2")
            .assertLeftPositionInRootIsEqualTo(-5.dp)
            .assertWidthIsEqualTo(5.dp)
    }

    @Test
    fun spatialRow_internalElementsAreAligned_withModifier() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialRow(
                        SubspaceModifier.testTag("row1").size(20.dp),
                        alignment = SpatialAlignment.CenterLeft,
                    ) {
                        SpatialColumn(
                            SubspaceModifier.testTag("column1")
                                .size(10.dp)
                                .align(SpatialAlignment.Top)
                        ) {
                            SpatialPanel { Text(text = "Column 1") }
                        }
                        SpatialColumn(
                            SubspaceModifier.testTag("column2")
                                .size(10.dp)
                                .align(SpatialAlignment.Front)
                        ) {
                            SpatialPanel { Text(text = "Column 2") }
                        }
                    }
                }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("column1").assertYPositionInRootIsEqualTo(5.dp)

        composeTestRule.onSubspaceNodeWithTag("column2").assertZPositionInRootIsEqualTo(5.dp)
    }

    @Test
    fun spatialColumn_internalElementsAreAligned() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialColumn(
                        SubspaceModifier.testTag("column1").size(20.dp),
                        alignment = SpatialAlignment.TopCenter,
                    ) {
                        SpatialRow(SubspaceModifier.testTag("row1").size(5.dp)) {
                            SpatialPanel { Text(text = "SpatialRow 1") }
                        }
                        SpatialRow(SubspaceModifier.testTag("row2").size(5.dp)) {
                            SpatialPanel { Text(text = "SpatialRow 2") }
                        }
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("column1")
            .assertTopPositionInRootIsEqualTo(10.dp)
            .assertYPositionInRootIsEqualTo(0.dp)
            .assertHeightIsEqualTo(20.dp)

        composeTestRule
            .onSubspaceNodeWithTag("row1")
            .assertTopPositionInRootIsEqualTo(10.dp)
            .assertHeightIsEqualTo(5.dp)

        composeTestRule
            .onSubspaceNodeWithTag("row2")
            .assertTopPositionInRootIsEqualTo(5.dp)
            .assertHeightIsEqualTo(5.dp)
    }

    @Test
    fun spatialColumn_internalElementsAreAligned_withModifier() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialColumn(
                        SubspaceModifier.testTag("column1").size(20.dp),
                        alignment = SpatialAlignment.TopCenter,
                    ) {
                        SpatialRow(
                            SubspaceModifier.testTag("row1")
                                .size(10.dp)
                                .align(SpatialAlignment.Left)
                        ) {
                            SpatialPanel { Text(text = "SpatialRow 1") }
                        }
                        SpatialRow(
                            SubspaceModifier.testTag("row2")
                                .size(10.dp)
                                .align(SpatialAlignment.Back)
                        ) {
                            SpatialPanel { Text(text = "SpatialRow 2") }
                        }
                    }
                }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("row1").assertXPositionInRootIsEqualTo(-5.dp)

        composeTestRule.onSubspaceNodeWithTag("row2").assertZPositionInRootIsEqualTo(-5.dp)
    }

    @Test
    fun spatialRowColumn_twoWeightBasedChildren_internalElementsAreLaidOutProperly() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialRow(SubspaceModifier.testTag("row1").width(1000.dp)) {
                        // 25% width (250dp)
                        SpatialColumn(SubspaceModifier.testTag("column1").weight(1f)) {
                            SpatialPanel { Text(text = "Column 1") }
                        }
                        // 75% width (750dp)
                        SpatialColumn(SubspaceModifier.testTag("column2").weight(3f)) {
                            SpatialPanel { Text(text = "Column 2") }
                        }
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("row1")
            .assertLeftPositionInRootIsEqualTo(-500.dp)
            .assertXPositionInRootIsEqualTo(0.dp)
            .assertWidthIsEqualTo(1000.dp)

        composeTestRule
            .onSubspaceNodeWithTag("column1")
            .assertLeftPositionInRootIsEqualTo(-500.dp)
            .assertWidthIsEqualTo(250.dp)

        composeTestRule
            .onSubspaceNodeWithTag("column2")
            .assertLeftPositionInRootIsEqualTo(-250.dp)
            .assertWidthIsEqualTo(750.dp)
    }

    @Test
    fun spatialRowColumn_oneFixedAndTwoWeightBasedChildren_internalElementsAreLaidOutProperly() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialRow(SubspaceModifier.testTag("row1").width(1000.dp)) {
                        // 250dp fixed width
                        SpatialColumn(SubspaceModifier.testTag("column1").width(250.dp)) {
                            SpatialPanel { Text(text = "Column 1") }
                        }
                        // 1/5th of the remaining 750dp (150dp)
                        SpatialColumn(SubspaceModifier.testTag("column2").weight(1f)) {
                            SpatialPanel { Text(text = "Column 2") }
                        }
                        // 4/5th of the remaining 750dp (600dp)
                        SpatialColumn(SubspaceModifier.testTag("column3").weight(4f)) {
                            SpatialPanel { Text(text = "Column 3") }
                        }
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("row1")
            .assertLeftPositionInRootIsEqualTo(-500.dp)
            .assertXPositionInRootIsEqualTo(0.dp)
            .assertWidthIsEqualTo(1000.dp)

        composeTestRule
            .onSubspaceNodeWithTag("column1")
            .assertLeftPositionInRootIsEqualTo(-500.dp)
            .assertWidthIsEqualTo(250.dp)

        composeTestRule
            .onSubspaceNodeWithTag("column2")
            .assertLeftPositionInRootIsEqualTo(-250.dp)
            .assertWidthIsEqualTo(150.dp)

        composeTestRule
            .onSubspaceNodeWithTag("column3")
            .assertLeftPositionInRootIsEqualTo(-100.dp)
            .assertWidthIsEqualTo(600.dp)
    }

    @Test
    fun spatialRowColumn_weightCalculationRemainderIsAppliedCorrectly() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    // 200dp row, 7 children:
                    // 200 / 7 = 28.57, which gets rounded to 29dp.
                    // 29 * 7 = 203dp, so the first 3 children should have 1dp removed from each of
                    // them.
                    SpatialRow(SubspaceModifier.testTag("row1").width(200.dp)) {
                        // 28dp (1dp remainder removed)
                        SpatialColumn(SubspaceModifier.testTag("column1").weight(1f)) {
                            SpatialPanel {}
                        }
                        // 28dp (1dp remainder removed)
                        SpatialColumn(SubspaceModifier.testTag("column2").weight(1f)) {
                            SpatialPanel {}
                        }
                        // 28dp (1dp remainder removed)
                        SpatialColumn(SubspaceModifier.testTag("column3").weight(1f)) {
                            SpatialPanel {}
                        }
                        // 29dp
                        SpatialColumn(SubspaceModifier.testTag("column4").weight(1f)) {
                            SpatialPanel {}
                        }
                        // 29dp
                        SpatialColumn(SubspaceModifier.testTag("column5").weight(1f)) {
                            SpatialPanel {}
                        }
                        // 29dp
                        SpatialColumn(SubspaceModifier.testTag("column6").weight(1f)) {
                            SpatialPanel {}
                        }
                        // 29dp
                        SpatialColumn(SubspaceModifier.testTag("column7").weight(1f)) {
                            SpatialPanel {}
                        }
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("row1")
            .assertLeftPositionInRootIsEqualTo(-100.dp)
            .assertXPositionInRootIsEqualTo(0.dp)
            .assertWidthIsEqualTo(200.dp)

        var currentLeftPosition = -100.dp
        composeTestRule
            .onSubspaceNodeWithTag("column1")
            .assertLeftPositionInRootIsEqualTo(currentLeftPosition)
            .assertWidthIsEqualTo(28.dp)
        currentLeftPosition += 28.dp

        composeTestRule
            .onSubspaceNodeWithTag("column2")
            .assertLeftPositionInRootIsEqualTo(currentLeftPosition)
            .assertWidthIsEqualTo(28.dp)
        currentLeftPosition += 28.dp

        composeTestRule
            .onSubspaceNodeWithTag("column3")
            .assertLeftPositionInRootIsEqualTo(currentLeftPosition)
            .assertWidthIsEqualTo(28.dp)
        currentLeftPosition += 28.dp

        composeTestRule
            .onSubspaceNodeWithTag("column4")
            .assertLeftPositionInRootIsEqualTo(currentLeftPosition)
            .assertWidthIsEqualTo(29.dp)
        currentLeftPosition += 29.dp

        composeTestRule
            .onSubspaceNodeWithTag("column5")
            .assertLeftPositionInRootIsEqualTo(currentLeftPosition)
            .assertWidthIsEqualTo(29.dp)
        currentLeftPosition += 29.dp

        composeTestRule
            .onSubspaceNodeWithTag("column6")
            .assertLeftPositionInRootIsEqualTo(currentLeftPosition)
            .assertWidthIsEqualTo(29.dp)
        currentLeftPosition += 29.dp

        composeTestRule
            .onSubspaceNodeWithTag("column7")
            .assertLeftPositionInRootIsEqualTo(currentLeftPosition)
            .assertWidthIsEqualTo(29.dp)
    }

    @Test
    fun spatialRowColumn_negativeCurvatureIsIgnored() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialCurvedRow(
                        SubspaceModifier.testTag("row1").width(500.dp),
                        curveRadius = -100.dp,
                    ) {
                        SpatialColumn(SubspaceModifier.testTag("column1").width(250.dp)) {
                            SpatialPanel { Text(text = "Column 1") }
                        }
                        SpatialColumn(SubspaceModifier.testTag("column2").width(250.dp)) {
                            SpatialPanel { Text(text = "Column 2") }
                        }
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("column1")
            .assertXPositionInRootIsEqualTo(-125.dp)
            .assertYPositionInRootIsEqualTo(0.dp)
            .assertZPositionInRootIsEqualTo(0.dp) // No curvature.
            .assertRotationInRootIsEqualTo(Quaternion.Identity)
        composeTestRule
            .onSubspaceNodeWithTag("column2")
            .assertXPositionInRootIsEqualTo(125.dp)
            .assertYPositionInRootIsEqualTo(0.dp)
            .assertZPositionInRootIsEqualTo(0.dp) // No curvature.
            .assertRotationInRootIsEqualTo(Quaternion.Identity)
    }

    @Test
    fun spatialRowColumn_zeroCurvatureIsIgnored() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialCurvedRow(
                        SubspaceModifier.testTag("row1").width(500.dp),
                        curveRadius = 0.dp,
                    ) {
                        SpatialColumn(SubspaceModifier.testTag("column1").width(250.dp)) {
                            SpatialPanel { Text(text = "Column 1") }
                        }
                        SpatialColumn(SubspaceModifier.testTag("column2").width(250.dp)) {
                            SpatialPanel { Text(text = "Column 2") }
                        }
                    }
                }
            }
        }

        // Verify that the row is flat, i.e., it has no curvature.
        composeTestRule
            .onSubspaceNodeWithTag("column1")
            .assertXPositionInRootIsEqualTo(-125.dp)
            .assertYPositionInRootIsEqualTo(0.dp)
            .assertZPositionInRootIsEqualTo(0.dp) // No curvature.
            .assertRotationInRootIsEqualTo(Quaternion.Identity)
        composeTestRule
            .onSubspaceNodeWithTag("column2")
            .assertXPositionInRootIsEqualTo(125.dp)
            .assertYPositionInRootIsEqualTo(0.dp)
            .assertZPositionInRootIsEqualTo(0.dp) // No curvature.
            .assertRotationInRootIsEqualTo(Quaternion.Identity)
    }

    @Test
    fun spatialRowColumn_positiveCurvatureCreatesCurvature() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialCurvedRow(
                        SubspaceModifier.testTag("row1").width(500.dp),
                        curveRadius = 100.dp,
                    ) {
                        SpatialColumn(SubspaceModifier.testTag("column1").width(250.dp)) {
                            SpatialPanel { Text(text = "Column 1") }
                        }
                        SpatialColumn(SubspaceModifier.testTag("column2").width(250.dp)) {
                            SpatialPanel { Text(text = "Column 2") }
                        }
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("column1")
            .assertXPositionInRootIsEqualTo(-94.dp)
            .assertYPositionInRootIsEqualTo(0.dp)
            .assertZPositionInRootIsEqualTo(68.dp)
            .assertRotationInRootIsEqualTo(Quaternion(0.0f, 0.58509725f, 0.0f, 0.8109631f))

        composeTestRule
            .onSubspaceNodeWithTag("column2")
            .assertXPositionInRootIsEqualTo(94.dp)
            .assertYPositionInRootIsEqualTo(0.dp)
            .assertZPositionInRootIsEqualTo(68.dp)
            .assertRotationInRootIsEqualTo(Quaternion(0.0f, -0.58509725f, 0.0f, 0.8109631f))
    }

    @Test
    fun spatialRowColumn_zOffsetIsRespected() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialCurvedRow(
                        SubspaceModifier.testTag("row1").width(500.dp).offset(0.dp, 0.dp, -50.dp),
                        curveRadius = 100.dp,
                    ) {
                        SpatialColumn(SubspaceModifier.testTag("column1").width(250.dp)) {
                            SpatialPanel { Text(text = "Column 1") }
                        }
                        SpatialColumn(SubspaceModifier.testTag("column2").width(250.dp)) {
                            SpatialPanel { Text(text = "Column 2") }
                        }
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("column1")
            .assertXPositionInRootIsEqualTo(-94.dp)
            .assertYPositionInRootIsEqualTo(0.dp)
            .assertZPositionInRootIsEqualTo(18.dp) // Offset by -50.dp.
            .assertRotationInRootIsEqualTo(Quaternion(0.0f, 0.58509725f, 0.0f, 0.8109631f))

        composeTestRule
            .onSubspaceNodeWithTag("column2")
            .assertXPositionInRootIsEqualTo(94.dp)
            .assertYPositionInRootIsEqualTo(0.dp)
            .assertZPositionInRootIsEqualTo(18.dp) // Offset by -50.dp.
            .assertRotationInRootIsEqualTo(Quaternion(0.0f, -0.58509725f, 0.0f, 0.8109631f))
    }
}
