/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.xr.compose.integration.tests

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.compose.platform.SpatialConfiguration
import androidx.xr.compose.spatial.Orbiter
import androidx.xr.compose.spatial.OrbiterDefaults
import androidx.xr.compose.spatial.OrbiterPosition
import androidx.xr.compose.spatial.OrbiterPosition.EdgeAlignment
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SpatialArrangement
import androidx.xr.compose.subspace.layout.SpatialRoundedCornerShape
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.aspectRatio
import androidx.xr.compose.subspace.layout.fillMaxHeight
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.padding
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.compose.testing.assertHeightIsEqualTo
import androidx.xr.compose.testing.assertWidthIsEqualTo
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.compose.unit.DpVolumeOffset
import androidx.xr.testutils.XrDeviceTest
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Automated instrument tests for the Spatial Layout CUJ test case in Compose XR.
 *
 * Covers:
 * - Multi-column spatial layout (left column, center column, right column)
 * - Spatial column sizing, alignment, and spacing (SpatialArrangement.spacedBy)
 * - Subspace-level outside Orbiters and localized Right-to-Left (RTL) Orbiters
 * - Manually offset Orbiters with custom DpVolumeOffset
 * - Dynamic AspectRatio modifier manipulation
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class SpatialLayoutTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        Assume.assumeTrue(
            "XR spatial environment is not supported on this device/emulator",
            SpatialConfiguration.hasXrSpatialFeature(context),
        )
    }

    /**
     * Validates that the primary 3-column spatial layout renders all panels and columns with
     * correct dimensions and hierarchy tags.
     */
    @Test
    fun spatialLayout_multiColumnGrid_rendersAllColumnsAndPanels() {
        composeTestRule.setContent {
            Subspace {
                SpatialRow(
                    modifier =
                        SubspaceModifier.width(1200.dp).height(600.dp).testTag("PanelGridRow"),
                    verticalAlignment = SpatialAlignment.CenterVertically,
                ) {
                    // Left Column (3 panels)
                    SpatialColumn(
                        modifier =
                            SubspaceModifier.width(200.dp).fillMaxHeight().testTag("LeftColumn"),
                        verticalArrangement = SpatialArrangement.spacedBy(20.dp),
                    ) {
                        SpatialPanel(
                            modifier =
                                SubspaceModifier.width(200.dp)
                                    .height(160.dp)
                                    .testTag("panel_top_left")
                        ) {
                            TestPanelBox("Panel Top Left")
                        }
                        SpatialPanel(
                            modifier =
                                SubspaceModifier.width(200.dp)
                                    .height(160.dp)
                                    .testTag("panel_anchor")
                        ) {
                            TestPanelBox("Anchorable Panel")
                        }
                        SpatialPanel(
                            modifier =
                                SubspaceModifier.width(200.dp)
                                    .height(160.dp)
                                    .testTag("panel_bottom_left")
                        ) {
                            TestPanelBox("Panel Bottom Left")
                        }
                    }

                    // Center Column (2 panels)
                    SpatialColumn(
                        modifier =
                            SubspaceModifier.width(600.dp)
                                .fillMaxHeight()
                                .padding(horizontal = 20.dp)
                                .testTag("CenterColumn"),
                        horizontalAlignment = SpatialAlignment.CenterHorizontally,
                        verticalArrangement = SpatialArrangement.Center,
                    ) {
                        SpatialPanel(
                            modifier =
                                SubspaceModifier.width(560.dp)
                                    .height(260.dp)
                                    .testTag("panel_main_task")
                        ) {
                            TestPanelBox("Panel Center - main task window")
                        }
                        SpatialPanel(
                            modifier =
                                SubspaceModifier.width(560.dp)
                                    .height(260.dp)
                                    .testTag("panel_activity")
                        ) {
                            TestPanelBox("Activity Panel Content")
                        }
                    }

                    // Right Column (3 panels)
                    SpatialColumn(
                        modifier =
                            SubspaceModifier.width(200.dp).fillMaxHeight().testTag("RightColumn"),
                        verticalArrangement = SpatialArrangement.spacedBy(20.dp),
                    ) {
                        SpatialPanel(
                            modifier =
                                SubspaceModifier.width(200.dp)
                                    .height(160.dp)
                                    .testTag("panel_top_right")
                        ) {
                            TestPanelBox("Panel Top Right")
                        }
                        SpatialPanel(
                            modifier =
                                SubspaceModifier.width(200.dp)
                                    .height(160.dp)
                                    .testTag("panel_aspect_ratio")
                        ) {
                            TestPanelBox("Aspect Ratio Panel")
                        }
                        SpatialPanel(
                            modifier =
                                SubspaceModifier.width(200.dp)
                                    .height(160.dp)
                                    .testTag("panel_rtl_orbiter")
                        ) {
                            TestPanelBox("RTL Layout Orbiter Panel")
                        }
                    }
                }
            }
        }

        // Verify Subspace column dimensions
        composeTestRule.onSubspaceNodeWithTag("LeftColumn").assertWidthIsEqualTo(200.dp)
        // Note: SubspaceSemanticsInfo currently reports the inner measurable layout width (560.dp)
        // after padding rather than the outer node bounds (600.dp).
        // TODO(b/557007691): SubspaceSemanticsInfo reports inner measurableLayout size instead of
        // outer layout bounds including padding.
        composeTestRule.onSubspaceNodeWithTag("CenterColumn").assertWidthIsEqualTo(560.dp)
        composeTestRule.onSubspaceNodeWithTag("RightColumn").assertWidthIsEqualTo(200.dp)

        // Verify all 2D panels are rendered across columns
        composeTestRule.onAllNodesWithText("Panel Top Left").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Anchorable Panel").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Panel Bottom Left").onFirst().assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Panel Center - main task window")
            .onFirst()
            .assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Activity Panel Content").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Panel Top Right").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Aspect Ratio Panel").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("RTL Layout Orbiter Panel").onFirst().assertIsDisplayed()
    }

    /**
     * Validates Subspace outside orbiters, localized RTL orbiters, and custom DpVolumeOffset
     * orbiters render within the hierarchy.
     */
    @Test
    fun spatialLayout_subspaceAndLocalOrbiters_rendersAllOrbitersAndPanels() {
        composeTestRule.setContent {
            Subspace {
                SpatialRow {
                    // Left panel with outside Subspace Orbiter
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.width(240.dp)
                                .height(200.dp)
                                .testTag("left_orbiter_panel")
                    ) {
                        Orbiter(
                            position =
                                OrbiterPosition.CenterStart(
                                    EdgeAlignment.Outside,
                                    offset = DpVolumeOffset(x = -8.dp),
                                ),
                            shape = SpatialRoundedCornerShape(CornerSize(16.dp)),
                        ) {
                            Surface {
                                Text(
                                    text = "Subspace Orbiter",
                                    modifier = Modifier.width(80.dp).padding(8.dp),
                                )
                            }
                        }
                        TestPanelBox("Left Content")
                    }

                    // Right panel with RTL Orbiter and custom Center Offset Orbiter
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.width(240.dp)
                                .height(200.dp)
                                .testTag("right_orbiter_panel")
                    ) {
                        // RTL Localized Orbiter
                        CompositionLocalProvider(
                            LocalLayoutDirection provides LayoutDirection.Rtl
                        ) {
                            Orbiter(
                                position =
                                    OrbiterPosition.TopStart(
                                        horizontalEdgeAlignment = EdgeAlignment.Center,
                                        verticalEdgeAlignment = EdgeAlignment.Center,
                                        offset =
                                            DpVolumeOffset(
                                                -16.dp,
                                                0.dp,
                                                OrbiterDefaults.Elevation,
                                            ),
                                    )
                            ) {
                                Surface(shape = RoundedCornerShape(CornerSize(16.dp))) {
                                    Text(text = "RTL Orbiter", modifier = Modifier.padding(8.dp))
                                }
                            }
                        }

                        // Manually offset orbiter
                        Orbiter(
                            position =
                                OrbiterPosition.BottomCenter(
                                    offset =
                                        DpVolumeOffset(120.dp, 120.dp, OrbiterDefaults.Elevation)
                                )
                        ) {
                            Surface(shape = RoundedCornerShape(CornerSize(16.dp))) {
                                Text(text = "Center Offset", modifier = Modifier.padding(8.dp))
                            }
                        }

                        TestPanelBox("RTL Layout Orbiter Panel")
                    }
                }
            }
        }

        // Verify that the orbiters exist in the hierarchy
        composeTestRule.onAllNodesWithText("Subspace Orbiter").onFirst().assertExists()
        composeTestRule.onAllNodesWithText("RTL Orbiter").onFirst().assertExists()
        composeTestRule.onAllNodesWithText("Center Offset").onFirst().assertExists()
        composeTestRule.onAllNodesWithText("Left Content").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("RTL Layout Orbiter Panel").onFirst().assertIsDisplayed()
    }

    /** Validates dynamic AspectRatio modifier adjustments on a SpatialPanel. */
    @Test
    fun spatialLayout_aspectRatioModifier_updatesPanelDimensionsDynamically() {
        var aspectRatioValue by mutableFloatStateOf(1f)

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    modifier =
                        SubspaceModifier.width(200.dp)
                            .aspectRatio(aspectRatioValue)
                            .testTag("aspect_ratio_panel")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Aspect Ratio: $aspectRatioValue")
                        Button(onClick = { aspectRatioValue = 2f }) {
                            Text("2:1 Ratio", fontSize = 11.sp)
                        }
                        Button(onClick = { aspectRatioValue = 0.5f }) {
                            Text("1:2 Ratio", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Initial 1:1 aspect ratio with 200.dp width -> 200.dp width, 200.dp height
        composeTestRule
            .onSubspaceNodeWithTag("aspect_ratio_panel")
            .assertWidthIsEqualTo(200.dp)
            .assertHeightIsEqualTo(200.dp)

        // Mutate to 2:1 aspect ratio (width 200.dp, ratio 2f -> height 100.dp)
        composeTestRule.onAllNodesWithText("2:1 Ratio").onFirst().performClick()

        composeTestRule
            .onSubspaceNodeWithTag("aspect_ratio_panel")
            .assertWidthIsEqualTo(200.dp)
            .assertHeightIsEqualTo(100.dp)

        // Mutate to 1:2 aspect ratio (width 200.dp, ratio 0.5f -> height 400.dp)
        composeTestRule.onAllNodesWithText("1:2 Ratio").onFirst().performClick()

        composeTestRule
            .onSubspaceNodeWithTag("aspect_ratio_panel")
            .assertWidthIsEqualTo(200.dp)
            .assertHeightIsEqualTo(400.dp)
    }

    /** Validates dimensions and arrangement of stacked panels within a SpatialColumn. */
    @Test
    fun spatialLayout_columnSpacingAndArrangement_rendersPanelsWithExpectedDimensions() {
        composeTestRule.setContent {
            Subspace {
                SpatialColumn(
                    modifier =
                        SubspaceModifier.width(250.dp).height(600.dp).testTag("spaced_column"),
                    verticalArrangement = SpatialArrangement.spacedBy(40.dp),
                ) {
                    SpatialPanel(
                        modifier = SubspaceModifier.width(250.dp).height(120.dp).testTag("panel_1")
                    ) {
                        TestPanelBox("Panel 1")
                    }
                    SpatialPanel(
                        modifier = SubspaceModifier.width(250.dp).height(120.dp).testTag("panel_2")
                    ) {
                        TestPanelBox("Panel 2")
                    }
                    SpatialPanel(
                        modifier = SubspaceModifier.width(250.dp).height(120.dp).testTag("panel_3")
                    ) {
                        TestPanelBox("Panel 3")
                    }
                }
            }
        }

        // Verify column size and child panels
        composeTestRule
            .onSubspaceNodeWithTag("spaced_column")
            .assertWidthIsEqualTo(250.dp)
            .assertHeightIsEqualTo(600.dp)

        composeTestRule
            .onSubspaceNodeWithTag("panel_1")
            .assertWidthIsEqualTo(250.dp)
            .assertHeightIsEqualTo(120.dp)
        composeTestRule
            .onSubspaceNodeWithTag("panel_2")
            .assertWidthIsEqualTo(250.dp)
            .assertHeightIsEqualTo(120.dp)
        composeTestRule
            .onSubspaceNodeWithTag("panel_3")
            .assertWidthIsEqualTo(250.dp)
            .assertHeightIsEqualTo(120.dp)

        composeTestRule.onAllNodesWithText("Panel 1").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Panel 2").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Panel 3").onFirst().assertIsDisplayed()
    }

    @Composable
    private fun TestPanelBox(text: String) {
        Box(
            modifier = Modifier.background(Color.LightGray).padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text)
        }
    }
}
