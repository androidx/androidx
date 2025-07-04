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
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.TestSetup
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
    @get:Rule val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun padding_settingValuesIndependentlySizesCorrectly() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialPanel(
                        SubspaceModifier.testTag("panel")
                            .size(100.dp)
                            .padding(
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
            TestSetup {
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
            TestSetup {
                Subspace {
                    SpatialPanel(
                        SubspaceModifier.testTag("panel").size(100.dp).padding(all = 20.dp)
                    ) {
                        Text(text = "Panel")
                    }
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
                TestSetup {
                    Subspace {
                        SpatialPanel(
                            SubspaceModifier.testTag("panel").size(100.dp).padding(top = -20.dp)
                        ) {
                            Text(text = "Panel")
                        }
                    }
                }
            }
        }
    }
}
