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
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.unit.Dp
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
import androidx.xr.compose.subspace.SpatialBox
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.SpatialSpacer
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SpatialArrangement
import androidx.xr.compose.subspace.layout.SpatialBiasAlignment
import androidx.xr.compose.subspace.layout.SpatialRoundedCornerShape
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.aspectRatio
import androidx.xr.compose.subspace.layout.depth
import androidx.xr.compose.subspace.layout.fillMaxHeight
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.padding
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.compose.testing.assertHeightIsEqualTo
import androidx.xr.compose.testing.assertPositionInRootIsEqualTo
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
 * - 3D positioning of sibling panels along the X axis in SpatialRow (including SpatialSpacer gaps)
 * - 3D positioning of stacked panels along the Y axis in SpatialColumn
 * - Uniform spacing between panels in a grid built with SpatialArrangement.spacedBy
 * - Z-axis depth alignment in SpatialBox and 3D translation via SubspaceModifier.offset
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

        // Verify 3D column positions. The row is 1200.dp wide and its children occupy
        // 200.dp + 600.dp + 200.dp = 1000.dp. With the default centered arrangement, the columns
        // span x = -500.dp..500.dp, so their centers are at x = -400.dp, 0.dp, and 400.dp.
        assertPositionInRoot("LeftColumn", x = (-400).dp, y = 0.dp, z = 0.dp)
        assertPositionInRoot("CenterColumn", x = 0.dp, y = 0.dp, z = 0.dp)
        assertPositionInRoot("RightColumn", x = 400.dp, y = 0.dp, z = 0.dp)

        // Side columns stack 3 x 160.dp panels with 20.dp spacing (520.dp in total), centered
        // within the 600.dp column height, so panel centers are at y = 180.dp, 0.dp, -180.dp.
        assertPositionInRoot("panel_top_left", x = (-400).dp, y = 180.dp, z = 0.dp)
        assertPositionInRoot("panel_anchor", x = (-400).dp, y = 0.dp, z = 0.dp)
        assertPositionInRoot("panel_bottom_left", x = (-400).dp, y = (-180).dp, z = 0.dp)
        assertPositionInRoot("panel_top_right", x = 400.dp, y = 180.dp, z = 0.dp)
        assertPositionInRoot("panel_aspect_ratio", x = 400.dp, y = 0.dp, z = 0.dp)
        assertPositionInRoot("panel_rtl_orbiter", x = 400.dp, y = (-180).dp, z = 0.dp)

        // The center column stacks 2 x 260.dp panels (520.dp in total), centered within the
        // 600.dp column height, so panel centers are at y = 130.dp and -130.dp.
        assertPositionInRoot("panel_main_task", x = 0.dp, y = 130.dp, z = 0.dp)
        assertPositionInRoot("panel_activity", x = 0.dp, y = (-130).dp, z = 0.dp)

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

        // Verify vertical stacking along the Y axis. The panels occupy 3 x 120.dp + 2 x 40.dp =
        // 440.dp, centered within the 600.dp column, so panel centers are at y = 160.dp, 0.dp,
        // and -160.dp. Y increases upwards, so the first panel is the highest one.
        assertPositionInRoot("spaced_column", x = 0.dp, y = 0.dp, z = 0.dp)
        assertPositionInRoot("panel_1", x = 0.dp, y = 160.dp, z = 0.dp)
        assertPositionInRoot("panel_2", x = 0.dp, y = 0.dp, z = 0.dp)
        assertPositionInRoot("panel_3", x = 0.dp, y = (-160).dp, z = 0.dp)

        composeTestRule.onAllNodesWithText("Panel 1").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Panel 2").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Panel 3").onFirst().assertIsDisplayed()
    }

    /**
     * Validates that sibling panels in a SpatialRow are ordered along the X axis and that a
     * SpatialSpacer introduces a gap of its width between them.
     */
    @Test
    fun spatialLayout_rowWithSpacer_ordersPanelsAlongXAxisWithSpacerGap() {
        composeTestRule.setContent {
            Subspace {
                SpatialRow(
                    modifier = SubspaceModifier.width(450.dp).height(200.dp).testTag("spacer_row")
                ) {
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.width(200.dp).height(200.dp).testTag("row_left_panel")
                    ) {
                        TestPanelBox("Row Left Panel")
                    }
                    SpatialSpacer(modifier = SubspaceModifier.width(50.dp).testTag("row_spacer"))
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.width(200.dp).height(200.dp).testTag("row_right_panel")
                    ) {
                        TestPanelBox("Row Right Panel")
                    }
                }
            }
        }

        composeTestRule.onAllNodesWithText("Row Left Panel").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Row Right Panel").onFirst().assertIsDisplayed()

        // The row spans x = -225.dp..225.dp: the left panel occupies -225.dp..-25.dp, the spacer
        // -25.dp..25.dp and the right panel 25.dp..225.dp. The right panel center is offset from
        // the left panel center by 200.dp (panel width) + 50.dp (spacer width).
        composeTestRule.onSubspaceNodeWithTag("spacer_row").assertWidthIsEqualTo(450.dp)
        composeTestRule.onSubspaceNodeWithTag("row_spacer").assertWidthIsEqualTo(50.dp)
        assertPositionInRoot("spacer_row", x = 0.dp, y = 0.dp, z = 0.dp)
        assertPositionInRoot("row_spacer", x = 0.dp, y = 0.dp, z = 0.dp)
        assertPositionInRoot("row_left_panel", x = (-125).dp, y = 0.dp, z = 0.dp)
        assertPositionInRoot("row_right_panel", x = 125.dp, y = 0.dp, z = 0.dp)
    }

    /**
     * Validates that a panel grid built with SpatialArrangement.spacedBy keeps the same gap between
     * all neighboring panels along both the X and Y axes.
     */
    @Test
    fun spatialLayout_spacedByGrid_appliesUniformSpacingBetweenPanels() {
        composeTestRule.setContent {
            Subspace {
                SpatialRow(
                    modifier = SubspaceModifier.width(440.dp).height(440.dp).testTag("grid_row"),
                    horizontalArrangement = SpatialArrangement.spacedBy(40.dp),
                ) {
                    SpatialColumn(
                        modifier =
                            SubspaceModifier.width(200.dp).fillMaxHeight().testTag("grid_left"),
                        verticalArrangement = SpatialArrangement.spacedBy(40.dp),
                    ) {
                        GridPanel(tag = "grid_top_left", text = "Grid Top Left")
                        GridPanel(tag = "grid_bottom_left", text = "Grid Bottom Left")
                    }
                    SpatialColumn(
                        modifier =
                            SubspaceModifier.width(200.dp).fillMaxHeight().testTag("grid_right"),
                        verticalArrangement = SpatialArrangement.spacedBy(40.dp),
                    ) {
                        GridPanel(tag = "grid_top_right", text = "Grid Top Right")
                        GridPanel(tag = "grid_bottom_right", text = "Grid Bottom Right")
                    }
                }
            }
        }

        composeTestRule.onAllNodesWithText("Grid Top Left").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Grid Bottom Left").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Grid Top Right").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Grid Bottom Right").onFirst().assertIsDisplayed()

        // A 2 x 2 grid of 200.dp panels with 40.dp gaps spans 440.dp in each direction, so panel
        // centers are at +/-120.dp on both axes (200.dp panel + 40.dp gap = 240.dp apart).
        assertPositionInRoot("grid_left", x = (-120).dp, y = 0.dp, z = 0.dp)
        assertPositionInRoot("grid_right", x = 120.dp, y = 0.dp, z = 0.dp)
        assertPositionInRoot("grid_top_left", x = (-120).dp, y = 120.dp, z = 0.dp)
        assertPositionInRoot("grid_bottom_left", x = (-120).dp, y = (-120).dp, z = 0.dp)
        assertPositionInRoot("grid_top_right", x = 120.dp, y = 120.dp, z = 0.dp)
        assertPositionInRoot("grid_bottom_right", x = 120.dp, y = (-120).dp, z = 0.dp)
    }

    /**
     * Validates that SpatialBox positions children along the Z axis according to their depth
     * alignment, in addition to the horizontal and vertical alignment.
     */
    @Test
    fun spatialLayout_spatialBoxDepthAlignment_positionsPanelsAlongZAxis() {
        composeTestRule.setContent {
            Subspace {
                SpatialBox(
                    modifier =
                        SubspaceModifier.width(600.dp)
                            .height(400.dp)
                            .depth(200.dp)
                            .testTag("depth_box")
                ) {
                    // Top-start-front corner of the box.
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.testTag("box_front_panel")
                                .width(200.dp)
                                .height(200.dp)
                                .align(
                                    SpatialBiasAlignment(
                                        horizontalBias = -1f,
                                        verticalBias = 1f,
                                        depthBias = 1f,
                                    )
                                )
                    ) {
                        TestPanelBox("Front Panel")
                    }
                    // Default (centered) alignment.
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.testTag("box_center_panel")
                                .width(200.dp)
                                .height(200.dp)
                    ) {
                        TestPanelBox("Center Panel")
                    }
                    // Bottom-end-back corner of the box.
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.testTag("box_back_panel")
                                .width(200.dp)
                                .height(200.dp)
                                .align(
                                    SpatialBiasAlignment(
                                        horizontalBias = 1f,
                                        verticalBias = -1f,
                                        depthBias = -1f,
                                    )
                                )
                    ) {
                        TestPanelBox("Back Panel")
                    }
                }
            }
        }

        composeTestRule.onAllNodesWithText("Front Panel").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Center Panel").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Back Panel").onFirst().assertIsDisplayed()

        // Panels are flat (0.dp depth), so aligning them to the front/back of the 200.dp deep box
        // moves them by +/-100.dp along Z. Horizontally they move by (600.dp - 200.dp) / 2 and
        // vertically by (400.dp - 200.dp) / 2.
        assertPositionInRoot("depth_box", x = 0.dp, y = 0.dp, z = 0.dp)
        assertPositionInRoot("box_front_panel", x = (-200).dp, y = 100.dp, z = 100.dp)
        assertPositionInRoot("box_center_panel", x = 0.dp, y = 0.dp, z = 0.dp)
        assertPositionInRoot("box_back_panel", x = 200.dp, y = (-100).dp, z = (-100).dp)
    }

    /**
     * Validates that SubspaceModifier.offset translates a panel along all three axes relative to
     * its layout slot, dynamically reacts to updates, and does not affect sibling panels.
     */
    @Test
    fun spatialLayout_offsetModifier_translatesPanelIn3dWithoutAffectingSiblings() {
        var offsetZ by mutableStateOf((-100).dp)

        composeTestRule.setContent {
            Subspace {
                SpatialRow(
                    modifier = SubspaceModifier.width(450.dp).height(200.dp),
                    horizontalArrangement = SpatialArrangement.spacedBy(50.dp),
                ) {
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.width(200.dp)
                                .height(200.dp)
                                .testTag("offset_reference_panel")
                    ) {
                        TestPanelBox("Reference Panel")
                    }
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.width(200.dp)
                                .height(200.dp)
                                .offset(x = 20.dp, y = (-40).dp, z = offsetZ)
                                .testTag("offset_panel")
                    ) {
                        TestPanelBox("Offset Panel")
                    }
                }
            }
        }

        composeTestRule.onAllNodesWithText("Reference Panel").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Offset Panel").onFirst().assertIsDisplayed()

        // Without offsets the panels would be centered at x = -125.dp and x = 125.dp. The offset
        // panel is additionally translated by (20.dp, -40.dp, -100.dp).
        assertPositionInRoot("offset_reference_panel", x = (-125).dp, y = 0.dp, z = 0.dp)
        assertPositionInRoot("offset_panel", x = 145.dp, y = (-40).dp, z = (-100).dp)

        // Push the panel further away from the user.
        composeTestRule.runOnUiThread { offsetZ = (-200).dp }
        composeTestRule.waitForIdle()

        assertPositionInRoot("offset_reference_panel", x = (-125).dp, y = 0.dp, z = 0.dp)
        assertPositionInRoot("offset_panel", x = 145.dp, y = (-40).dp, z = (-200).dp)
    }

    /**
     * Asserts that the subspace node with the given [tag] is positioned at ([x], [y], [z]) relative
     * to the root of the Subspace, within [PositionTolerance].
     */
    private fun assertPositionInRoot(tag: String, x: Dp, y: Dp, z: Dp) {
        composeTestRule
            .onSubspaceNodeWithTag(tag)
            .assertPositionInRootIsEqualTo(x, y, z, PositionTolerance)
    }

    @Composable
    @SubspaceComposable
    private fun GridPanel(tag: String, text: String) {
        SpatialPanel(modifier = SubspaceModifier.width(200.dp).height(200.dp).testTag(tag)) {
            TestPanelBox(text)
        }
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

    private companion object {
        /**
         * Tolerance used for 3D position assertions to absorb Dp-to-pixel rounding performed by
         * subspace layouts on device.
         */
        val PositionTolerance = 1.dp
    }
}
