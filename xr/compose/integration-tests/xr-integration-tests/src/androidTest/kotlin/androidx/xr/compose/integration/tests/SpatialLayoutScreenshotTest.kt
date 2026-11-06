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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.screenshot.assertAgainstGolden
import androidx.test.screenshot.matchers.MSSIMMatcher
import androidx.xr.compose.subspace.SpatialBox
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.SubspaceComposable
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SpatialArrangement
import androidx.xr.compose.subspace.layout.SpatialBiasAlignment
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.depth
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.testutils.DEFAULT_XR_MSSIM_THRESHOLD
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.XrScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Reference visual screenshot tests for 3D Compose for XR `Subspace` spatial layouts.
 *
 * Uses standard [createAndroidComposeRule] and [setXrScreenshotContent], captures the settled 3D
 * compositor viewport via [captureToBitmap], and asserts against golden images with
 * `androidx.test.screenshot.assertAgainstGolden` and [XrScreenshotTestRule].
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class SpatialLayoutScreenshotTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule val screenshotRule = XrScreenshotTestRule(GOLDEN_PATH)

    @Test
    @XrDeviceTest
    fun spatialRowAndColumn_multiPanelLayout_rendersInFullSpace() {
        composeTestRule.setXrScreenshotContent {
            SpatialRow(
                modifier = SubspaceModifier.width(720.dp).height(360.dp).testTag("screenshot_row"),
                horizontalArrangement = SpatialArrangement.spacedBy(24.dp),
                verticalAlignment = SpatialAlignment.CenterVertically,
            ) {
                ColoredSpatialPanel(
                    tag = "left_panel",
                    label = "Left",
                    width = 200.dp,
                    height = 240.dp,
                    color = Color.Blue,
                )
                SpatialColumn(
                    modifier = SubspaceModifier.width(220.dp).height(320.dp),
                    verticalArrangement = SpatialArrangement.spacedBy(20.dp),
                    horizontalAlignment = SpatialAlignment.CenterHorizontally,
                ) {
                    ColoredSpatialPanel(
                        tag = "center_top_panel",
                        label = "Top",
                        width = 220.dp,
                        height = 140.dp,
                        color = Color.Green,
                    )
                    ColoredSpatialPanel(
                        tag = "center_bottom_panel",
                        label = "Bottom",
                        width = 220.dp,
                        height = 140.dp,
                        color = Color.Yellow,
                    )
                }
                ColoredSpatialPanel(
                    tag = "right_panel",
                    label = "Right",
                    width = 200.dp,
                    height = 240.dp,
                    color = Color.Red,
                )
            }
        }

        composeTestRule
            .captureToBitmap()
            .assertAgainstGolden(
                screenshotRule,
                "compose_spatial_row_and_column_multi_panel_layout",
                MSSIMMatcher(threshold = DEFAULT_XR_MSSIM_THRESHOLD),
            )
    }

    @Test
    @XrDeviceTest
    fun spatialBox_depthAlignedPanels_rendersInFullSpace() {
        composeTestRule.setXrScreenshotContent {
            SpatialBox(modifier = SubspaceModifier.width(560.dp).height(360.dp).depth(180.dp)) {
                ColoredSpatialPanel(
                    tag = "back_panel",
                    label = "Back",
                    width = 220.dp,
                    height = 160.dp,
                    color = Color.Blue,
                    modifier =
                        SubspaceModifier.align(
                            SpatialBiasAlignment(
                                horizontalBias = 1f,
                                verticalBias = -1f,
                                depthBias = -1f,
                            )
                        ),
                )
                ColoredSpatialPanel(
                    tag = "center_panel",
                    label = "Center",
                    width = 220.dp,
                    height = 160.dp,
                    color = Color.Green,
                )
                ColoredSpatialPanel(
                    tag = "front_panel",
                    label = "Front",
                    width = 220.dp,
                    height = 160.dp,
                    color = Color.Red,
                    modifier =
                        SubspaceModifier.align(
                            SpatialBiasAlignment(
                                horizontalBias = -1f,
                                verticalBias = 1f,
                                depthBias = 1f,
                            )
                        ),
                )
            }
        }

        composeTestRule
            .captureToBitmap()
            .assertAgainstGolden(
                screenshotRule,
                "compose_spatial_box_depth_aligned_panels",
                MSSIMMatcher(threshold = DEFAULT_XR_MSSIM_THRESHOLD),
            )
    }

    @Composable
    @SubspaceComposable
    private fun ColoredSpatialPanel(
        tag: String,
        label: String,
        width: Dp,
        height: Dp,
        color: Color,
        textColor: Color =
            if (color == Color.Yellow || color == Color.Cyan) Color.Black else Color.White,
        modifier: SubspaceModifier = SubspaceModifier,
    ) {
        SpatialPanel(modifier = modifier.width(width).height(height).testTag(tag)) {
            Box(
                modifier = Modifier.fillMaxSize().background(color).padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = label, color = textColor, fontSize = 20.sp)
            }
        }
    }
}
