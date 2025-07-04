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
import androidx.xr.compose.testing.assertWidthIsEqualTo
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import kotlin.test.assertFailsWith
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for AspectRatio modifiers. */
@RunWith(AndroidJUnit4::class)
class AspectRatioTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun aspectRatio_capWidth_sizesCorrectly() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialPanel(SubspaceModifier.testTag("panel").width(20.dp).aspectRatio(2f)) {
                        Text(text = "Panel")
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertWidthIsEqualTo(20.dp)
            .assertHeightIsEqualTo(10.dp)
            .assertDepthIsEqualTo(0.dp)
    }

    @Test
    fun aspectRatio_capHeight_sizesCorrectly() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialPanel(SubspaceModifier.testTag("panel").height(10.dp).aspectRatio(2f)) {
                        Text(text = "Panel")
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertWidthIsEqualTo(20.dp)
            .assertHeightIsEqualTo(10.dp)
            .assertDepthIsEqualTo(0.dp)
    }

    @Test
    fun aspectRatio_capWidthMatchHeightFirst_sizesCorrectly() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialPanel(
                        SubspaceModifier.testTag("panel")
                            .width(20.dp)
                            .aspectRatio(ratio = 2f, matchHeightConstraintsFirst = true)
                    ) {
                        Text(text = "Panel")
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertWidthIsEqualTo(20.dp)
            .assertHeightIsEqualTo(10.dp)
            .assertDepthIsEqualTo(0.dp)
    }

    @Test
    fun aspectRatio_capHeightMatchHeightFirst_sizesCorrectly() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialPanel(
                        SubspaceModifier.testTag("panel")
                            .height(10.dp)
                            .aspectRatio(ratio = 2f, matchHeightConstraintsFirst = true)
                    ) {
                        Text(text = "Panel")
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertWidthIsEqualTo(20.dp)
            .assertHeightIsEqualTo(10.dp)
            .assertDepthIsEqualTo(0.dp)
    }

    @Test
    fun aspectRatio_zeroRatio_throwsException() {
        assertFailsWith<IllegalArgumentException> { SubspaceModifier.aspectRatio(0f) }
    }

    @Test
    fun aspectRatio_negativeRatio_throwsException() {
        assertFailsWith<IllegalArgumentException> { SubspaceModifier.aspectRatio(-2f) }
    }
}
