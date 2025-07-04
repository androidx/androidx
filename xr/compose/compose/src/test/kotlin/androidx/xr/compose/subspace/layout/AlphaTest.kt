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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.TestSetup
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlphaTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun alpha_shouldBeAppliedToEntity() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialPanel(SubspaceModifier.testTag("panel").alpha(0.5f)) {
                        Text(text = "Panel")
                    }
                }
            }
        }

        assertThat(
                composeTestRule
                    .onSubspaceNodeWithTag("panel")
                    .fetchSemanticsNode()
                    .semanticsEntity
                    ?.getAlpha()
            )
            .isEqualTo(0.5f)
    }

    @Test
    fun alpha_multiple_shouldBeMultipliedThenAppliedToEntity() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialPanel(SubspaceModifier.testTag("panel").alpha(0.5f).alpha(0.5f)) {
                        Text(text = "Panel")
                    }
                }
            }
        }

        assertThat(
                composeTestRule
                    .onSubspaceNodeWithTag("panel")
                    .fetchSemanticsNode()
                    .semanticsEntity
                    ?.getAlpha()
            )
            .isEqualTo(0.25f)
    }

    @Test
    fun alpha_negative_shouldBeClampedToZero() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialPanel(SubspaceModifier.testTag("panel").alpha(-1f)) {
                        Text(text = "Panel")
                    }
                }
            }
        }

        assertThat(
                composeTestRule
                    .onSubspaceNodeWithTag("panel")
                    .fetchSemanticsNode()
                    .semanticsEntity
                    ?.getAlpha()
            )
            .isEqualTo(0.0f)
    }

    @Test
    fun alpha_greaterThanOne_shouldBeClampedToOne() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialPanel(SubspaceModifier.testTag("panel").alpha(1.1f)) {
                        Text(text = "Panel")
                    }
                }
            }
        }

        assertThat(
                composeTestRule
                    .onSubspaceNodeWithTag("panel")
                    .fetchSemanticsNode()
                    .semanticsEntity
                    ?.getAlpha()
            )
            .isEqualTo(1.0f)
    }

    @Test
    fun alpha_updatesWhenValueChanges() {
        var alpha by mutableStateOf(0.1f)

        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialPanel(SubspaceModifier.testTag("panel").alpha(alpha)) {
                        Text(text = "Panel")
                    }
                }
            }
        }

        assertThat(
                composeTestRule
                    .onSubspaceNodeWithTag("panel")
                    .fetchSemanticsNode()
                    .semanticsEntity
                    ?.getAlpha()
            )
            .isEqualTo(0.1f)

        alpha = 0.5f

        assertThat(
                composeTestRule
                    .onSubspaceNodeWithTag("panel")
                    .fetchSemanticsNode()
                    .semanticsEntity
                    ?.getAlpha()
            )
            .isEqualTo(0.5f)
    }

    @Test
    fun alpha_multiple_valuesShouldBeClampedThenMultiplied() {
        composeTestRule.setContent {
            TestSetup {
                Subspace {
                    SpatialPanel(SubspaceModifier.testTag("panel").alpha(3f).alpha(0.1f)) {
                        Text(text = "Panel")
                    }
                }
            }
        }

        assertThat(
                composeTestRule
                    .onSubspaceNodeWithTag("panel")
                    .fetchSemanticsNode()
                    .semanticsEntity
                    ?.getAlpha()
            )
            .isEqualTo(0.1f)
    }
}
