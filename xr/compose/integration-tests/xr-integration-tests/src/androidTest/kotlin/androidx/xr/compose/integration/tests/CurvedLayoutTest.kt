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
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.compose.platform.SpatialConfiguration
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialCurvedRow
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SpatialAlignment
import androidx.xr.compose.subspace.layout.SpatialArrangement
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.compose.testing.assertRotationInRootIsEqualTo
import androidx.xr.compose.testing.assertXPositionInRootIsEqualTo
import androidx.xr.compose.testing.assertYPositionInRootIsEqualTo
import androidx.xr.compose.testing.assertZPositionInRootIsEqualTo
import androidx.xr.compose.testing.getPositionInRoot
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.runtime.math.Quaternion
import androidx.xr.testutils.XrDeviceTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Automated instrumented tests for the Curved Row Layout CUJ in Compose for XR.
 *
 * Covers:
 * - Flat row behavior when curveRadius is zero, negative, or Dp.Infinity
 * - Inward angling and z-offset positioning for positive curve radii
 * - Sharper curvature (greater z-offset and angle) with smaller curve radii
 * - Dynamic runtime adjustment of curve radius (slider interaction simulation)
 * - Vertical alignment preservation along the curved trajectory
 * - Multi-column and multi-panel structured layouts in SpatialCurvedRow
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class CurvedLayoutTest {

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
     * Validates that non-positive (zero and negative) curve radii produce a completely flat row
     * with zero z-displacement and identity rotation.
     */
    @Test
    fun curvedLayout_zeroOrNegativeRadius_rendersFlatRow() {
        var curveRadius by mutableStateOf(0.dp)

        composeTestRule.setContent {
            Subspace {
                SpatialCurvedRow(
                    modifier = SubspaceModifier.width(600.dp).height(200.dp).testTag("curvedRow"),
                    curveRadius = curveRadius,
                    horizontalArrangement = SpatialArrangement.SpaceEvenly,
                ) {
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.width(180.dp).height(120.dp).testTag("leftPanel")
                    ) {
                        Text("Left Panel")
                    }
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.width(180.dp).height(120.dp).testTag("rightPanel")
                    ) {
                        Text("Right Panel")
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Left Panel").assertIsDisplayed()
        composeTestRule.onNodeWithText("Right Panel").assertIsDisplayed()

        // 1. When radius is 0.dp, left and right panels have 0 z-displacement and Identity rotation
        composeTestRule
            .onSubspaceNodeWithTag("leftPanel")
            .assertZPositionInRootIsEqualTo(0.dp)
            .assertRotationInRootIsEqualTo(Quaternion.Identity)

        composeTestRule
            .onSubspaceNodeWithTag("rightPanel")
            .assertZPositionInRootIsEqualTo(0.dp)
            .assertRotationInRootIsEqualTo(Quaternion.Identity)

        // 2. When radius is negative (-100.dp), row also renders flat
        composeTestRule.runOnUiThread { curveRadius = (-100).dp }
        composeTestRule.waitForIdle()

        composeTestRule
            .onSubspaceNodeWithTag("leftPanel")
            .assertZPositionInRootIsEqualTo(0.dp)
            .assertRotationInRootIsEqualTo(Quaternion.Identity)

        composeTestRule
            .onSubspaceNodeWithTag("rightPanel")
            .assertZPositionInRootIsEqualTo(0.dp)
            .assertRotationInRootIsEqualTo(Quaternion.Identity)
    }

    /**
     * Validates that a positive curve radius curves the row, placing side panels at positive z
     * depths and angling them inward towards the user.
     */
    @Test
    fun curvedLayout_positiveRadius_curvesPanelsInward() {
        composeTestRule.setContent {
            Subspace {
                SpatialCurvedRow(
                    modifier = SubspaceModifier.width(500.dp).height(200.dp).testTag("curvedRow"),
                    curveRadius = 100.dp,
                ) {
                    SpatialColumn(modifier = SubspaceModifier.width(250.dp).testTag("leftColumn")) {
                        SpatialPanel { Text("Left Content") }
                    }
                    SpatialColumn(
                        modifier = SubspaceModifier.width(250.dp).testTag("rightColumn")
                    ) {
                        SpatialPanel { Text("Right Content") }
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Left Content").assertIsDisplayed()
        composeTestRule.onNodeWithText("Right Content").assertIsDisplayed()

        // The expected geometric values are derived from circular arc geometry (radius R = 100.dp,
        // arc length s = ±125.dp, angle θ = s/R = ±1.25 rad):
        //   - X = R * sin(θ) ≈ ±94.dp
        //   - Z = R * (1 - cos(θ)) ≈ 68.dp
        //   - Y-rotation tangent = (qx=0, qy=sin(-θ/2), qz=0, qw=cos(-θ/2))
        // Left column is angled inward (positive Y-quaternion component) with positive Z offset
        composeTestRule
            .onSubspaceNodeWithTag("leftColumn")
            .assertXPositionInRootIsEqualTo(-94.dp)
            .assertYPositionInRootIsEqualTo(0.dp)
            .assertZPositionInRootIsEqualTo(68.dp)
            .assertRotationInRootIsEqualTo(Quaternion(0.0f, 0.58509725f, 0.0f, 0.8109631f))

        // Right column is angled inward (negative Y-quaternion component) with positive Z offset
        composeTestRule
            .onSubspaceNodeWithTag("rightColumn")
            .assertXPositionInRootIsEqualTo(94.dp)
            .assertYPositionInRootIsEqualTo(0.dp)
            .assertZPositionInRootIsEqualTo(68.dp)
            .assertRotationInRootIsEqualTo(Quaternion(0.0f, -0.58509725f, 0.0f, 0.8109631f))
    }

    /**
     * Validates that decreasing curve radius sharpens the curve (larger Z offset and larger inward
     * angle).
     */
    @Test
    fun curvedLayout_decreasingRadius_increasesCurvature() {
        var curveRadius by mutableStateOf(1000.dp)

        composeTestRule.setContent {
            Subspace {
                SpatialCurvedRow(
                    modifier = SubspaceModifier.width(600.dp).height(200.dp).testTag("curvedRow"),
                    curveRadius = curveRadius,
                    horizontalArrangement = SpatialArrangement.SpaceBetween,
                ) {
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.width(180.dp).height(120.dp).testTag("leftPanel")
                    ) {
                        Text("Left")
                    }
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.width(180.dp).height(120.dp).testTag("centerPanel")
                    ) {
                        Text("Center")
                    }
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.width(180.dp).height(120.dp).testTag("rightPanel")
                    ) {
                        Text("Right")
                    }
                }
            }
        }

        // Center panel is always at center with zero z and identity rotation
        composeTestRule
            .onSubspaceNodeWithTag("centerPanel")
            .assertXPositionInRootIsEqualTo(0.dp)
            .assertZPositionInRootIsEqualTo(0.dp)
            .assertRotationInRootIsEqualTo(Quaternion.Identity)

        val initialZ = composeTestRule.onSubspaceNodeWithTag("leftPanel").getPositionInRoot().z

        // Switch to a sharper curve (smaller radius)
        composeTestRule.runOnUiThread { curveRadius = 300.dp }
        composeTestRule.waitForIdle()

        val sharpZ = composeTestRule.onSubspaceNodeWithTag("leftPanel").getPositionInRoot().z

        // Sharper curvature displaces side panels further forward in Z
        assertThat(sharpZ).isGreaterThan(initialZ)

        // Center panel still remains at origin
        composeTestRule
            .onSubspaceNodeWithTag("centerPanel")
            .assertXPositionInRootIsEqualTo(0.dp)
            .assertZPositionInRootIsEqualTo(0.dp)
            .assertRotationInRootIsEqualTo(Quaternion.Identity)
    }

    /**
     * Validates dynamic runtime switching between curved and flat configurations (simulating
     * interactive slider adjustments).
     */
    @Test
    fun curvedLayout_dynamicRadiusAdjustment_updatesPanelCurvature() {
        var curveRadius by mutableStateOf(100.dp)

        composeTestRule.setContent {
            Subspace {
                SpatialCurvedRow(
                    modifier = SubspaceModifier.width(500.dp).height(200.dp).testTag("curvedRow"),
                    curveRadius = curveRadius,
                ) {
                    SpatialColumn(modifier = SubspaceModifier.width(250.dp).testTag("leftCol")) {
                        SpatialPanel { Text("Left Dynamic") }
                    }
                    SpatialColumn(modifier = SubspaceModifier.width(250.dp).testTag("rightCol")) {
                        SpatialPanel { Text("Right Dynamic") }
                    }
                }
            }
        }

        // 1. Initial curved state (radius = 100.dp)
        composeTestRule.onNodeWithText("Left Dynamic").assertIsDisplayed()
        composeTestRule
            .onSubspaceNodeWithTag("leftCol")
            .assertZPositionInRootIsEqualTo(68.dp)
            .assertRotationInRootIsEqualTo(Quaternion(0.0f, 0.58509725f, 0.0f, 0.8109631f))

        // 2. Adjust radius to flat (Dp.Infinity)
        composeTestRule.runOnUiThread { curveRadius = Dp.Infinity }
        composeTestRule.waitForIdle()

        // 3. Verify flat layout (z = 0, rotation = Identity)
        composeTestRule
            .onSubspaceNodeWithTag("leftCol")
            .assertXPositionInRootIsEqualTo(-125.dp)
            .assertZPositionInRootIsEqualTo(0.dp)
            .assertRotationInRootIsEqualTo(Quaternion.Identity)

        composeTestRule
            .onSubspaceNodeWithTag("rightCol")
            .assertXPositionInRootIsEqualTo(125.dp)
            .assertZPositionInRootIsEqualTo(0.dp)
            .assertRotationInRootIsEqualTo(Quaternion.Identity)
    }

    /**
     * Validates that vertical alignment (Bottom) is preserved properly while panels follow the
     * curve trajectory.
     */
    @Test
    fun curvedLayout_verticalAlignment_preservesAlignmentAlongCurve() {
        composeTestRule.setContent {
            Subspace {
                SpatialCurvedRow(
                    modifier = SubspaceModifier.width(500.dp).height(300.dp).testTag("curvedRow"),
                    curveRadius = 100.dp,
                    verticalAlignment = SpatialAlignment.Bottom,
                ) {
                    SpatialColumn(
                        modifier =
                            SubspaceModifier.width(250.dp).height(100.dp).testTag("bottomLeftCol")
                    ) {
                        SpatialPanel { Text("Bottom Left") }
                    }
                    SpatialColumn(
                        modifier =
                            SubspaceModifier.width(250.dp).height(100.dp).testTag("bottomRightCol")
                    ) {
                        SpatialPanel { Text("Bottom Right") }
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Bottom Left").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bottom Right").assertIsDisplayed()

        // Height is 300.dp, child height is 100.dp, aligned to Bottom -> y = -100.dp
        composeTestRule
            .onSubspaceNodeWithTag("bottomLeftCol")
            .assertYPositionInRootIsEqualTo(-100.dp)
            .assertZPositionInRootIsEqualTo(68.dp)

        composeTestRule
            .onSubspaceNodeWithTag("bottomRightCol")
            .assertYPositionInRootIsEqualTo(-100.dp)
            .assertZPositionInRootIsEqualTo(68.dp)
    }

    /**
     * Validates multi-column layout structure with stacked panels inside SpatialCurvedRow, matching
     * the sample app layout architecture.
     */
    @Test
    fun curvedLayout_multiColumnStructure_rendersAllPanels() {
        composeTestRule.setContent {
            Subspace {
                SpatialCurvedRow(
                    modifier = SubspaceModifier.width(800.dp).height(400.dp).testTag("curvedRow"),
                    curveRadius = 0.dp, // Flat
                    horizontalArrangement = SpatialArrangement.SpaceEvenly,
                ) {
                    SpatialColumn(modifier = SubspaceModifier.width(200.dp).testTag("leftStack")) {
                        SpatialPanel(modifier = SubspaceModifier.height(150.dp)) {
                            Text("Top Left")
                        }
                        SpatialPanel(modifier = SubspaceModifier.height(150.dp)) {
                            Text("Bottom Left")
                        }
                    }
                    SpatialColumn(
                        modifier = SubspaceModifier.width(300.dp).testTag("centerColumn")
                    ) {
                        SpatialPanel(modifier = SubspaceModifier.height(300.dp)) {
                            Text("Main Center Panel")
                        }
                    }
                    SpatialColumn(modifier = SubspaceModifier.width(200.dp).testTag("rightStack")) {
                        SpatialPanel(modifier = SubspaceModifier.height(150.dp)) {
                            Text("Top Right")
                        }
                        SpatialPanel(modifier = SubspaceModifier.height(150.dp)) {
                            Text("Bottom Right")
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Top Left").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bottom Left").assertIsDisplayed()
        composeTestRule.onNodeWithText("Main Center Panel").assertIsDisplayed()
        composeTestRule.onNodeWithText("Top Right").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bottom Right").assertIsDisplayed()
    }
}
