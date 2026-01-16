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

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.assertPositionInRootIsEqualTo
import androidx.xr.compose.testing.assertPositionIsEqualTo
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [offset] modifier. */
@RunWith(AndroidJUnit4::class)
class OffsetTest {

    // Migrate to `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`,
    // available starting with v1.11.0.
    // See API docs for details.
    @Suppress("DEPRECATION")
    @get:Rule
    val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun offset_positiveValuesArePositionedCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel").offset(20.dp, 20.dp, 20.dp)) {
                    Text(text = "Panel")
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertPositionInRootIsEqualTo(20.dp, 20.dp, 20.dp)
    }

    @Test
    fun offset_negativeValuesArePositionedCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel").offset((-20).dp, (-20).dp, (-20).dp)
                ) {
                    Text(text = "Panel")
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertPositionInRootIsEqualTo((-20).dp, (-20).dp, (-20).dp)
    }

    @Test
    fun offset_rtlLayoutIsPositionedCorrectly() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Subspace {
                    SpatialPanel(SubspaceModifier.testTag("panel").offset(20.dp, 20.dp, 20.dp)) {
                        Text(text = "Panel")
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertPositionInRootIsEqualTo((-20).dp, 20.dp, 20.dp)
    }

    @Test
    fun absoluteOffset_ltrLayoutIsPositionedCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel").absoluteOffset(20.dp, 20.dp, 20.dp)
                ) {
                    Text(text = "Panel")
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertPositionInRootIsEqualTo(20.dp, 20.dp, 20.dp)
    }

    @Test
    fun absoluteOffset_rtlLayoutIsPositionedCorrectly() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Subspace {
                    SpatialPanel(
                        SubspaceModifier.testTag("panel").absoluteOffset(20.dp, 20.dp, 20.dp)
                    ) {
                        Text(text = "Panel")
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertPositionInRootIsEqualTo(20.dp, 20.dp, 20.dp)
    }

    @Test
    fun offset_and_absoluteOffset_combined_rtlLayoutIsPositionedCorrectly() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Subspace {
                    SpatialPanel(
                        SubspaceModifier.testTag("panel")
                            .offset(20.dp, 20.dp, 20.dp)
                            .absoluteOffset(10.dp, 10.dp, 10.dp)
                    ) {
                        Text(text = "Panel")
                    }
                }
            }
        }

        // Offset with RTL moves the panel -20 dp and absoluteOffset with RTL moves the panel 10 dp.
        // Final x = -20 + 10 = -10 dp
        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertPositionInRootIsEqualTo((-10).dp, 30.dp, 30.dp)
    }

    @Test
    fun offset_combinedWithOtherModifiersArePositionedCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel")
                        .width(100.dp)
                        .offset(10.dp, 10.dp, 10.dp)
                        .height(100.dp)
                ) {
                    Text(text = "Panel")
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertPositionInRootIsEqualTo(10.dp, 10.dp, 10.dp)
    }

    @Test
    fun offset_nestedLayoutsArePositionedCorrectly() {
        composeTestRule.setContent {
            Subspace {
                SpatialRow(SubspaceModifier.width(1000.dp)) {
                    SpatialColumn(SubspaceModifier.weight(1f)) {
                        SpatialPanel(
                            SubspaceModifier.testTag("panel1").offset(10.dp, 10.dp, 10.dp)
                        ) {
                            Text(text = "Panel 1")
                        }
                    }
                    SpatialColumn(SubspaceModifier.weight(1f)) {
                        SpatialPanel(
                            SubspaceModifier.testTag("panel2").offset(10.dp, 10.dp, 10.dp)
                        ) {
                            Text(text = "Panel 2")
                        }
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel1")
            .assertPositionInRootIsEqualTo((-240).dp, 10.dp, 10.dp) // x=-(1000/2/2) + 10
            .assertPositionIsEqualTo(10.dp, 10.dp, 10.dp)

        composeTestRule
            .onSubspaceNodeWithTag("panel2")
            .assertPositionInRootIsEqualTo(260.dp, 10.dp, 10.dp) // x=(1000/2/2) + 10
            .assertPositionIsEqualTo(10.dp, 10.dp, 10.dp)
    }

    @Test
    fun offset_updatesDynamically() {
        composeTestRule.setContent {
            Subspace {
                var offsetX by remember { mutableStateOf(0.dp) }
                SpatialPanel(SubspaceModifier.testTag("panel").offset(x = offsetX)) {
                    Button(modifier = Modifier.testTag("button"), onClick = { offsetX += 10.dp }) {
                        Text(text = "Click to change offset")
                    }
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertPositionInRootIsEqualTo(0.dp, 0.dp, 0.dp)
        composeTestRule.onNodeWithTag("button").performClick()
        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertPositionInRootIsEqualTo(10.dp, 0.dp, 0.dp)
        composeTestRule.onNodeWithTag("button").performClick().performClick().performClick()
        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertPositionInRootIsEqualTo(40.dp, 0.dp, 0.dp)
    }
}
