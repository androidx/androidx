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

package androidx.xr.compose.subspace.layout

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.TestSetup
import androidx.xr.compose.testing.assertDepthIsEqualTo
import androidx.xr.compose.testing.assertHeightIsEqualTo
import androidx.xr.compose.testing.assertWidthIsEqualTo
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for size modifiers. */
@RunWith(AndroidJUnit4::class)
class SizeTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun size_individualModifiers_panelsAreSizedCorrectly() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialPanel(
                        SubspaceModifier.testTag("panel").width(20.dp).height(20.dp).depth(20.dp)
                    ) {
                        Text(text = "Panel")
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertWidthIsEqualTo(20.dp)
            .assertHeightIsEqualTo(20.dp)
            .assertDepthIsEqualTo(20.dp)
    }

    @Test
    fun size_combinedModifier_panelsAreSizedCorrectly() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialPanel(SubspaceModifier.testTag("panel").size(20.dp)) {
                        Text(text = "Panel")
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertWidthIsEqualTo(20.dp)
            .assertHeightIsEqualTo(20.dp)
            .assertDepthIsEqualTo(20.dp)
    }

    @Test
    fun size_combinedModifier_panelsRespectParentSizeConstraints() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialRow(SubspaceModifier.size(10.dp)) {
                        SpatialPanel(SubspaceModifier.testTag("panel").size(20.dp)) {
                            Text(text = "Panel")
                        }
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertWidthIsEqualTo(10.dp)
            .assertHeightIsEqualTo(10.dp)
            .assertDepthIsEqualTo(10.dp)
    }

    @Test
    fun size_individualRequiredModifiers_panelsAreSizedCorrectly() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialPanel(
                        SubspaceModifier.testTag("panel")
                            .requiredWidth(20.dp)
                            .requiredHeight(20.dp)
                            .requiredDepth(20.dp)
                    ) {
                        Text(text = "Panel")
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertWidthIsEqualTo(20.dp)
            .assertHeightIsEqualTo(20.dp)
            .assertDepthIsEqualTo(20.dp)
    }

    @Test
    fun size_combinedRequiredModifier_panelsAreSizedCorrectly() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialPanel(SubspaceModifier.testTag("panel").requiredSize(20.dp)) {
                        Text(text = "Panel")
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertWidthIsEqualTo(20.dp)
            .assertHeightIsEqualTo(20.dp)
            .assertDepthIsEqualTo(20.dp)
    }

    @Test
    fun size_combinedRequiredModifier_panelsOverrideParentSizeConstraints() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialRow(SubspaceModifier.size(10.dp)) {
                        SpatialPanel(SubspaceModifier.testTag("panel").requiredSize(20.dp)) {
                            Text(text = "Panel")
                        }
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertWidthIsEqualTo(20.dp)
            .assertHeightIsEqualTo(20.dp)
            .assertDepthIsEqualTo(20.dp)
    }

    @Test
    fun size_individualFillModifiers_panelsAreSizedCorrectly() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialRow(SubspaceModifier.size(20.dp)) {
                        SpatialPanel(
                            SubspaceModifier.testTag("panel")
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .fillMaxDepth()
                        ) {
                            Text(text = "Panel")
                        }
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertWidthIsEqualTo(20.dp)
            .assertHeightIsEqualTo(20.dp)
            .assertDepthIsEqualTo(20.dp)
    }

    @Test
    fun size_individualFillModifiersWithFraction_panelsAreSizedCorrectly() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialRow(SubspaceModifier.size(20.dp)) {
                        SpatialPanel(
                            SubspaceModifier.testTag("panel")
                                .fillMaxWidth(0.5f)
                                .fillMaxHeight(0.5f)
                                .fillMaxDepth(0.5f)
                        ) {
                            Text(text = "Panel")
                        }
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertWidthIsEqualTo(10.dp)
            .assertHeightIsEqualTo(10.dp)
            .assertDepthIsEqualTo(10.dp)
    }

    @Test
    fun size_combinedFillModifier_panelsAreSizedCorrectly() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialRow(SubspaceModifier.size(20.dp)) {
                        SpatialPanel(SubspaceModifier.testTag("panel").fillMaxSize()) {
                            Text(text = "Panel")
                        }
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertWidthIsEqualTo(20.dp)
            .assertHeightIsEqualTo(20.dp)
            .assertDepthIsEqualTo(20.dp)
    }

    @Test
    fun size_combinedFillModifierWithFraction_panelsAreSizedCorrectly() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialRow(SubspaceModifier.size(20.dp)) {
                        SpatialPanel(SubspaceModifier.testTag("panel").fillMaxSize(0.5f)) {
                            Text(text = "Panel")
                        }
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertWidthIsEqualTo(10.dp)
            .assertHeightIsEqualTo(10.dp)
            .assertDepthIsEqualTo(10.dp)
    }
}
