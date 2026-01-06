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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.assertDepthIsEqualTo
import androidx.xr.compose.testing.assertHeightIsEqualTo
import androidx.xr.compose.testing.assertWidthIsEqualTo
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.compose.unit.DpVolumeSize
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for size modifiers. */
@RunWith(AndroidJUnit4::class)
class SizeTest {

    // Migrate to `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`,
    // available starting with v1.11.0.
    // See API docs for details.
    @Suppress("DEPRECATION")
    @get:Rule
    val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun size_individualModifiers_panelsAreSizedCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel").width(20.dp).height(20.dp).depth(20.dp)
                ) {
                    Text(text = "Panel")
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
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel").size(20.dp)) { Text(text = "Panel") }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertWidthIsEqualTo(20.dp)
            .assertHeightIsEqualTo(20.dp)
            .assertDepthIsEqualTo(20.dp)
    }

    @Test
    fun size_dpValues_panelsAreSizedCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel").size(20.dp, 30.dp, 40.dp)) {
                    Text(text = "Panel")
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertWidthIsEqualTo(20.dp)
            .assertHeightIsEqualTo(30.dp)
            .assertDepthIsEqualTo(40.dp)
    }

    @Test
    fun size_unspecifiedDpValues_doesNotOverrideParentSize() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .size(width = 40.dp, height = 50.dp, depth = 60.dp)
                        .size(
                            width = Dp.Unspecified,
                            height = Dp.Unspecified,
                            depth = Dp.Unspecified,
                        )
                ) {
                    Text(text = "Panel")
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertWidthIsEqualTo(40.dp)
            .assertHeightIsEqualTo(50.dp)
            .assertDepthIsEqualTo(60.dp)
    }

    @Test
    fun size_dpVolumeSize_panelsAreSizedCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel").size(DpVolumeSize(20.dp, 30.dp, 40.dp))
                ) {
                    Text(text = "Panel")
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertWidthIsEqualTo(20.dp)
            .assertHeightIsEqualTo(30.dp)
            .assertDepthIsEqualTo(40.dp)
    }

    @Test
    fun size_combinedModifier_panelsRespectParentSizeConstraints() {
        composeTestRule.setContent {
            Subspace {
                SpatialRow(SubspaceModifier.size(10.dp)) {
                    SpatialPanel(SubspaceModifier.testTag("panel").size(20.dp)) {
                        Text(text = "Panel")
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

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertWidthIsEqualTo(20.dp)
            .assertHeightIsEqualTo(20.dp)
            .assertDepthIsEqualTo(20.dp)
    }

    @Test
    fun size_combinedRequiredModifier_panelsAreSizedCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel").requiredSize(20.dp)) {
                    Text(text = "Panel")
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
    fun requiredSize_dpValues_panelsAreSizedCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel").requiredSize(20.dp, 30.dp, 40.dp)) {
                    Text(text = "Panel")
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertWidthIsEqualTo(20.dp)
            .assertHeightIsEqualTo(30.dp)
            .assertDepthIsEqualTo(40.dp)
    }

    @Test
    fun requiredSize_unspecifiedDpValues_doesNotOverrideParentSize() {
        composeTestRule.setContent {
            Subspace {
                SpatialRow(SubspaceModifier.size(10.dp, 20.dp, 30.dp)) {
                    SpatialPanel(
                        SubspaceModifier.testTag("panel")
                            .requiredSize(width = 40.dp, height = 50.dp, depth = 60.dp)
                            .requiredSize(
                                width = Dp.Unspecified,
                                height = Dp.Unspecified,
                                depth = Dp.Unspecified,
                            )
                    ) {
                        Text(text = "Panel")
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertWidthIsEqualTo(40.dp)
            .assertHeightIsEqualTo(50.dp)
            .assertDepthIsEqualTo(60.dp)
    }

    @Test
    fun requiredSize_dpVolumeSize_panelsAreSizedCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .requiredSize(DpVolumeSize(20.dp, 30.dp, 40.dp))
                ) {
                    Text(text = "Panel")
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertWidthIsEqualTo(20.dp)
            .assertHeightIsEqualTo(30.dp)
            .assertDepthIsEqualTo(40.dp)
    }

    @Test
    fun size_combinedRequiredModifier_panelsOverrideParentSizeConstraints() {
        composeTestRule.setContent {
            Subspace {
                SpatialRow(SubspaceModifier.size(10.dp)) {
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
    fun size_individualFillModifiers_panelsAreSizedCorrectly() {
        composeTestRule.setContent {
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

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertWidthIsEqualTo(20.dp)
            .assertHeightIsEqualTo(20.dp)
            .assertDepthIsEqualTo(20.dp)
    }

    @Test
    fun size_individualFillModifiersWithFraction_panelsAreSizedCorrectly() {
        composeTestRule.setContent {
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

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertWidthIsEqualTo(10.dp)
            .assertHeightIsEqualTo(10.dp)
            .assertDepthIsEqualTo(10.dp)
    }

    @Test
    fun size_combinedFillModifier_panelsAreSizedCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialRow(SubspaceModifier.size(20.dp)) {
                    SpatialPanel(SubspaceModifier.testTag("panel").fillMaxSize()) {
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
    fun size_combinedFillModifierWithFraction_panelsAreSizedCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialRow(SubspaceModifier.size(20.dp)) {
                    SpatialPanel(SubspaceModifier.testTag("panel").fillMaxSize(0.5f)) {
                        Text(text = "Panel")
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
    fun sizeIn_respectsUpperBounds() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .sizeIn(maxWidth = 40.dp, maxHeight = 35.dp, maxDepth = 30.dp)
                        .size(50.dp)
                ) {}
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertWidthIsEqualTo(40.dp)
            .assertHeightIsEqualTo(35.dp)
            .assertDepthIsEqualTo(30.dp)
    }

    @Test
    fun sizeIn_respectsLowerBounds() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .sizeIn(minWidth = 10.dp, minHeight = 15.dp, minDepth = 20.dp)
                        .size(5.dp)
                ) {}
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertWidthIsEqualTo(10.dp)
            .assertHeightIsEqualTo(15.dp)
            .assertDepthIsEqualTo(20.dp)
    }

    @Test
    fun sizeIn_contentWithinBounds_isUnchanged() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .sizeIn(minWidth = 10.dp, maxWidth = 40.dp)
                        .width(25.dp)
                ) {}
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertWidthIsEqualTo(25.dp)
    }

    @Test
    fun sizeIn_respectsStricterParentMaxConstraint() {
        composeTestRule.setContent {
            Subspace {
                SpatialRow(SubspaceModifier.size(30.dp)) {
                    SpatialPanel(
                        SubspaceModifier.testTag("panel")
                            .sizeIn(maxWidth = 50.dp, maxHeight = 50.dp, maxDepth = 50.dp)
                            .fillMaxSize()
                    ) {}
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertWidthIsEqualTo(30.dp)
            .assertHeightIsEqualTo(30.dp)
            .assertDepthIsEqualTo(30.dp)
    }

    @Test
    fun widthIn_respectsUpperBounds() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel").widthIn(max = 40.dp).width(50.dp)) {}
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertWidthIsEqualTo(40.dp)
    }

    @Test
    fun widthIn_respectsLowerBounds() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel").widthIn(min = 10.dp).width(5.dp)) {}
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertWidthIsEqualTo(10.dp)
    }

    @Test
    fun widthIn_contentWithinBounds_isUnchanged() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel").widthIn(min = 10.dp, max = 40.dp).width(25.dp)
                ) {}
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertWidthIsEqualTo(25.dp)
    }

    @Test
    fun widthIn_respectsStricterParentMaxConstraint() {
        composeTestRule.setContent {
            Subspace {
                SpatialRow(SubspaceModifier.width(30.dp)) {
                    SpatialPanel(
                        SubspaceModifier.testTag("panel").widthIn(max = 50.dp).fillMaxWidth()
                    ) {}
                }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertWidthIsEqualTo(30.dp)
    }

    @Test
    fun widthIn_respectsStricterModifierMaxConstraint() {
        composeTestRule.setContent {
            Subspace {
                SpatialRow(SubspaceModifier.width(50.dp)) {
                    SpatialPanel(
                        SubspaceModifier.testTag("panel").widthIn(max = 30.dp).fillMaxWidth()
                    ) {}
                }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertWidthIsEqualTo(30.dp)
    }

    @Test
    fun heightIn_respectsUpperBounds() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel").heightIn(max = 40.dp).height(50.dp)
                ) {}
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertHeightIsEqualTo(40.dp)
    }

    @Test
    fun heightIn_respectsLowerBounds() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel").heightIn(min = 10.dp).height(5.dp)
                ) {}
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertHeightIsEqualTo(10.dp)
    }

    @Test
    fun heightIn_contentWithinBounds_isUnchanged() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .heightIn(min = 10.dp, max = 40.dp)
                        .height(25.dp)
                ) {}
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertHeightIsEqualTo(25.dp)
    }

    @Test
    fun heightIn_respectsStricterParentMaxConstraint() {
        composeTestRule.setContent {
            Subspace {
                SpatialRow(SubspaceModifier.height(30.dp)) {
                    SpatialPanel(
                        SubspaceModifier.testTag("panel").heightIn(max = 50.dp).fillMaxHeight()
                    ) {}
                }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertHeightIsEqualTo(30.dp)
    }

    @Test
    fun heightIn_respectsStricterModifierMaxConstraint() {
        composeTestRule.setContent {
            Subspace {
                SpatialRow(SubspaceModifier.height(50.dp)) {
                    SpatialPanel(
                        SubspaceModifier.testTag("panel").heightIn(max = 30.dp).fillMaxHeight()
                    ) {}
                }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertHeightIsEqualTo(30.dp)
    }

    @Test
    fun depthIn_respectsUpperBounds() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel").depthIn(max = 40.dp).depth(50.dp)) {}
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertDepthIsEqualTo(40.dp)
    }

    @Test
    fun depthIn_respectsLowerBounds() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel").depthIn(min = 10.dp).depth(5.dp)) {}
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertDepthIsEqualTo(10.dp)
    }

    @Test
    fun depthIn_contentWithinBounds_isUnchanged() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel").depthIn(min = 10.dp, max = 40.dp).depth(25.dp)
                ) {}
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertDepthIsEqualTo(25.dp)
    }

    @Test
    fun depthIn_respectsStricterParentMaxConstraint() {
        composeTestRule.setContent {
            Subspace {
                SpatialRow(SubspaceModifier.depth(30.dp)) {
                    SpatialPanel(
                        SubspaceModifier.testTag("panel").depthIn(max = 50.dp).fillMaxDepth()
                    ) {}
                }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertDepthIsEqualTo(30.dp)
    }

    @Test
    fun depthIn_respectsStricterModifierMaxConstraint() {
        composeTestRule.setContent {
            Subspace {
                SpatialRow(SubspaceModifier.depth(50.dp)) {
                    SpatialPanel(
                        SubspaceModifier.testTag("panel").depthIn(max = 30.dp).fillMaxDepth()
                    ) {}
                }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertDepthIsEqualTo(30.dp)
    }

    @Test
    fun requiredSizeIn_whenContentIsLargerThanMax_coercesToMax() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .requiredSizeIn(maxWidth = 40.dp, maxHeight = 35.dp, maxDepth = 30.dp)
                        .size(50.dp)
                ) {}
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertWidthIsEqualTo(40.dp)
            .assertHeightIsEqualTo(35.dp)
            .assertDepthIsEqualTo(30.dp)
    }

    @Test
    fun requiredSizeIn_whenContentIsSmallerThanMin_coercesToMin() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .requiredSizeIn(minWidth = 10.dp, minHeight = 15.dp, minDepth = 20.dp)
                        .size(5.dp)
                ) {}
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertWidthIsEqualTo(10.dp)
            .assertHeightIsEqualTo(15.dp)
            .assertDepthIsEqualTo(20.dp)
    }

    @Test
    fun requiredSizeIn_whenLargerThanParent_overridesParentConstraints() {
        composeTestRule.setContent {
            Subspace {
                SpatialRow(SubspaceModifier.size(30.dp)) {
                    SpatialPanel(
                        SubspaceModifier.testTag("panel")
                            .requiredSizeIn(minWidth = 40.dp, minHeight = 45.dp, minDepth = 50.dp)
                    ) {}
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertWidthIsEqualTo(40.dp)
            .assertHeightIsEqualTo(45.dp)
            .assertDepthIsEqualTo(50.dp)
    }

    @Test
    fun requiredWidthIn_whenContentIsLargerThanMax_coercesToMax() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel").requiredWidthIn(max = 30.dp).width(40.dp)
                ) {}
            }
        }
        composeTestRule.onSubspaceNodeWithTag("panel").assertWidthIsEqualTo(30.dp)
    }

    @Test
    fun requiredWidthIn_whenContentIsSmallerThanMin_coercesToMin() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel").requiredWidthIn(min = 20.dp).width(10.dp)
                ) {}
            }
        }
        composeTestRule.onSubspaceNodeWithTag("panel").assertWidthIsEqualTo(20.dp)
    }

    @Test
    fun requiredWidthIn_whenLargerThanParent_overridesParentConstraints() {
        composeTestRule.setContent {
            Subspace {
                SpatialRow(SubspaceModifier.width(30.dp)) {
                    SpatialPanel(SubspaceModifier.testTag("panel").requiredWidthIn(min = 40.dp)) {}
                }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertWidthIsEqualTo(40.dp)
    }

    @Test
    fun requiredHeightIn_whenContentIsLargerThanMax_coercesToMax() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel").requiredHeightIn(max = 30.dp).height(40.dp)
                ) {}
            }
        }
        composeTestRule.onSubspaceNodeWithTag("panel").assertHeightIsEqualTo(30.dp)
    }

    @Test
    fun requiredHeightIn_whenContentIsSmallerThanMin_coercesToMin() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel").requiredHeightIn(min = 20.dp).height(10.dp)
                ) {}
            }
        }
        composeTestRule.onSubspaceNodeWithTag("panel").assertHeightIsEqualTo(20.dp)
    }

    @Test
    fun requiredHeightIn_whenLargerThanParent_overridesParentConstraints() {
        composeTestRule.setContent {
            Subspace {
                SpatialRow(SubspaceModifier.height(30.dp)) {
                    SpatialPanel(SubspaceModifier.testTag("panel").requiredHeightIn(min = 40.dp)) {}
                }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertHeightIsEqualTo(40.dp)
    }

    @Test
    fun requiredDepthIn_whenContentIsLargerThanMax_coercesToMax() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel").requiredDepthIn(max = 30.dp).depth(40.dp)
                ) {}
            }
        }
        composeTestRule.onSubspaceNodeWithTag("panel").assertDepthIsEqualTo(30.dp)
    }

    @Test
    fun requiredDepthIn_whenContentIsSmallerThanMin_coercesToMin() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel").requiredDepthIn(min = 20.dp).depth(10.dp)
                ) {}
            }
        }
        composeTestRule.onSubspaceNodeWithTag("panel").assertDepthIsEqualTo(20.dp)
    }

    @Test
    fun requiredDepthIn_whenLargerThanParent_overridesParentConstraints() {
        composeTestRule.setContent {
            Subspace {
                SpatialRow(SubspaceModifier.depth(30.dp)) {
                    SpatialPanel(SubspaceModifier.testTag("panel").requiredDepthIn(min = 40.dp)) {}
                }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertDepthIsEqualTo(40.dp)
    }
}
