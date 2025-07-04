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
import androidx.xr.compose.subspace.layout.size
import androidx.xr.compose.subspace.layout.testTag
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.TestSetup
import androidx.xr.compose.testing.assertHeightIsEqualTo
import androidx.xr.compose.testing.assertPositionInRootIsEqualTo
import androidx.xr.compose.testing.assertWidthIsEqualTo
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [SpatialBox]. */
@RunWith(AndroidJUnit4::class)
class SpatialBoxTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun spatialBox_elementsAreCenteredByDefault() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialBox(SubspaceModifier.size(100.dp)) {
                        SpatialPanel(SubspaceModifier.testTag("panel1").size(50.dp)) {
                            Text(text = "Panel 1")
                        }
                        SpatialPanel(SubspaceModifier.testTag("panel2").size(50.dp)) {
                            Text(text = "Panel 2")
                        }
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel1")
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertWidthIsEqualTo(50.dp)
            .assertHeightIsEqualTo(50.dp)

        composeTestRule
            .onSubspaceNodeWithTag("panel2")
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertWidthIsEqualTo(50.dp)
            .assertHeightIsEqualTo(50.dp)
    }

    @Test
    fun spatialBox_elementsAreAlignedWithBoxSpatialAlignment_topLeft() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialBox(
                        SubspaceModifier.size(100.dp),
                        alignment = SpatialAlignment.TopLeft,
                    ) {
                        SpatialPanel(SubspaceModifier.testTag("panel1").size(50.dp)) {
                            Text(text = "Panel 1")
                        }
                        SpatialPanel(SubspaceModifier.testTag("panel2").size(50.dp)) {
                            Text(text = "Panel 2")
                        }
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel1")
            .assertPositionInRootIsEqualTo(-25.dp, 25.dp, 0.dp)
            .assertWidthIsEqualTo(50.dp)
            .assertHeightIsEqualTo(50.dp)

        composeTestRule
            .onSubspaceNodeWithTag("panel2")
            .assertPositionInRootIsEqualTo(-25.dp, 25.dp, 0.dp)
            .assertWidthIsEqualTo(50.dp)
            .assertHeightIsEqualTo(50.dp)
    }

    @Test
    fun spatialBox_elementsAreAlignedWithBoxSpatialAlignment_bottomRight() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialBox(
                        SubspaceModifier.size(100.dp),
                        alignment = SpatialAlignment.BottomRight,
                    ) {
                        SpatialPanel(SubspaceModifier.testTag("panel1").size(50.dp)) {
                            Text(text = "Panel 1")
                        }
                        SpatialPanel(SubspaceModifier.testTag("panel2").size(50.dp)) {
                            Text(text = "Panel 2")
                        }
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel1")
            .assertPositionInRootIsEqualTo(25.dp, -25.dp, 0.dp)
            .assertWidthIsEqualTo(50.dp)
            .assertHeightIsEqualTo(50.dp)

        composeTestRule
            .onSubspaceNodeWithTag("panel2")
            .assertPositionInRootIsEqualTo(25.dp, -25.dp, 0.dp)
            .assertWidthIsEqualTo(50.dp)
            .assertHeightIsEqualTo(50.dp)
    }

    @Test
    fun spatialBox_elementsAreAlignedWithModifier() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialBox(SubspaceModifier.size(100.dp)) {
                        SpatialPanel(
                            SubspaceModifier.testTag("panel1")
                                .size(50.dp)
                                .align(SpatialAlignment.BottomLeft)
                        ) {
                            Text(text = "Panel 1")
                        }
                        SpatialPanel(
                            SubspaceModifier.testTag("panel2")
                                .size(50.dp)
                                .align(SpatialAlignment.TopRight)
                        ) {
                            Text(text = "Panel 2")
                        }
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel1")
            .assertPositionInRootIsEqualTo(-25.dp, -25.dp, 0.dp)
            .assertWidthIsEqualTo(50.dp)
            .assertHeightIsEqualTo(50.dp)

        composeTestRule
            .onSubspaceNodeWithTag("panel2")
            .assertPositionInRootIsEqualTo(25.dp, 25.dp, 0.dp)
            .assertWidthIsEqualTo(50.dp)
            .assertHeightIsEqualTo(50.dp)
    }

    @Test
    fun spatialBox_elementsHonorPropagatedMinConstraints() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialBox(SubspaceModifier.size(100.dp), propagateMinConstraints = true) {
                        SpatialPanel(SubspaceModifier.testTag("panel1").size(50.dp)) {
                            Text(text = "Panel 1")
                        }
                        SpatialPanel(SubspaceModifier.testTag("panel2").size(50.dp)) {
                            Text(text = "Panel 2")
                        }
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel1")
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertWidthIsEqualTo(100.dp)
            .assertHeightIsEqualTo(100.dp)

        composeTestRule
            .onSubspaceNodeWithTag("panel2")
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertWidthIsEqualTo(100.dp)
            .assertHeightIsEqualTo(100.dp)
    }

    @Test
    fun spatialBox_elementsHonorWithoutPropagatedMinConstraints() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialBox(SubspaceModifier.size(300.dp), propagateMinConstraints = false) {
                        SpatialPanel(SubspaceModifier.testTag("panel1").size(150.dp)) {
                            Text(text = "Panel 1")
                        }
                        SpatialPanel(SubspaceModifier.testTag("panel2").size(150.dp)) {
                            Text(text = "Panel 2")
                        }
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel1")
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertWidthIsEqualTo(150.dp)
            .assertHeightIsEqualTo(150.dp)

        composeTestRule
            .onSubspaceNodeWithTag("panel2")
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertWidthIsEqualTo(150.dp)
            .assertHeightIsEqualTo(150.dp)
    }
}
