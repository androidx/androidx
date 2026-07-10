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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.assertDepthIsEqualTo
import androidx.xr.compose.testing.assertHeightIsEqualTo
import androidx.xr.compose.testing.assertPositionInRootIsEqualTo
import androidx.xr.compose.testing.assertWidthIsEqualTo
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import kotlin.test.assertFailsWith
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [padding] modifier. */
@RunWith(AndroidJUnit4::class)
class PaddingTest {

    // Migrate to `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`,
    // available starting with v1.11.0.
    // See API docs for details.
    @Suppress("DEPRECATION")
    @get:Rule
    val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun padding_settingValuesIndependentlySizesCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .size(100.dp)
                        .padding(
                            start = 20.dp,
                            top = 10.dp,
                            end = 10.dp,
                            bottom = 20.dp,
                            front = 10.dp,
                            back = 20.dp,
                        )
                ) {
                    Text(text = "Panel")
                }
            }
        }

        // LTR x-position: (start - end) / 2 = (20 - 10) / 2 = 5.dp
        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertPositionInRootIsEqualTo(5.dp, 5.dp, 5.dp)
            .assertWidthIsEqualTo(70.dp)
            .assertHeightIsEqualTo(70.dp)
            .assertDepthIsEqualTo(70.dp)
    }

    @Test
    fun padding_inRtl_respectsLayoutDirection() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Subspace {
                    SpatialPanel(
                        SubspaceModifier.testTag("panel")
                            .size(100.dp)
                            .padding(
                                start = 20.dp,
                                top = 10.dp,
                                end = 10.dp,
                                bottom = 20.dp,
                                front = 10.dp,
                                back = 20.dp,
                            )
                    ) {
                        Text(text = "Panel")
                    }
                }
            }
        }

        // RTL x-position: -((start - end) / 2) = -((20 - 10) / 2) = -5.dp
        // The x-axis position is flipped for RTL.
        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertPositionInRootIsEqualTo((-5).dp, 5.dp, 5.dp)
            .assertWidthIsEqualTo(70.dp)
            .assertHeightIsEqualTo(70.dp)
            .assertDepthIsEqualTo(70.dp)
    }

    @Test
    fun absolutePadding_inRtl_ignoresLayoutDirection() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Subspace {
                    SpatialPanel(
                        SubspaceModifier.testTag("panel")
                            .size(100.dp)
                            .absolutePadding(
                                left = 20.dp,
                                top = 10.dp,
                                right = 10.dp,
                                bottom = 20.dp,
                                front = 10.dp,
                                back = 20.dp,
                            )
                    ) {
                        Text(text = "Panel")
                    }
                }
            }
        }

        // x-position: (left - right) / 2 = (20 - 10) / 2 = 5.dp
        // Position is identical to LTR because absolutePadding ignores layout direction.
        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertPositionInRootIsEqualTo(5.dp, 5.dp, 5.dp)
            .assertWidthIsEqualTo(70.dp)
            .assertHeightIsEqualTo(70.dp)
            .assertDepthIsEqualTo(70.dp)
    }

    @Test
    fun padding_settingDirectionalValuesSizesCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .size(100.dp)
                        .padding(horizontal = 20.dp, vertical = 20.dp, depth = 20.dp)
                ) {
                    Text(text = "Panel")
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertWidthIsEqualTo(60.dp)
            .assertHeightIsEqualTo(60.dp)
            .assertDepthIsEqualTo(60.dp)
    }

    @Test
    fun padding_settingAllValuesSizesCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel").size(100.dp).padding(all = 20.dp)) {
                    Text(text = "Panel")
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertWidthIsEqualTo(60.dp)
            .assertHeightIsEqualTo(60.dp)
            .assertDepthIsEqualTo(60.dp)
    }

    @Test
    fun padding_negativePaddingThrowsException() {
        assertFailsWith<IllegalArgumentException> {
            composeTestRule.setContent {
                Subspace {
                    SpatialPanel(
                        SubspaceModifier.testTag("panel").size(100.dp).padding(top = (-20).dp)
                    ) {
                        Text(text = "Panel")
                    }
                }
            }
        }
    }

    @Test
    fun padding_chained_stacksCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .size(100.dp)
                        .padding(all = 10.dp)
                        .padding(all = 10.dp)
                ) {
                    Text(text = "Panel")
                }
            }
        }

        // Total padding is 20.dp on each side.
        // Size = 100 - (20 + 20) = 60.dp
        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertWidthIsEqualTo(60.dp)
            .assertHeightIsEqualTo(60.dp)
            .assertDepthIsEqualTo(60.dp)
    }

    @Test
    fun padding_zeroPadding_isNoOp() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel").size(100.dp).padding(all = 0.dp)) {
                    Text(text = "Panel")
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertWidthIsEqualTo(100.dp)
            .assertHeightIsEqualTo(100.dp)
            .assertDepthIsEqualTo(100.dp)
    }

    @Test
    fun padding_largerThanParent_clampsToZero() {
        composeTestRule.setContent {
            Subspace {
                // The size is 50, but the padding is 60 (30 on each side)
                // The inner content should be measured with 0 constraints.
                // The final size will be constrained by the outer size modifier.
                SpatialPanel(SubspaceModifier.testTag("panel").size(50.dp).padding(all = 30.dp)) {
                    Text(text = "Panel")
                }
            }
        }

        // Final size is clamped by the .size(50.dp) modifier
        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertWidthIsEqualTo(0.dp)
            .assertHeightIsEqualTo(0.dp)
            .assertDepthIsEqualTo(0.dp)
    }
}
