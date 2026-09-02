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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.compose.platform.SpatialConfiguration
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialColumn
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.SpatialSpacer
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.rotate
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.compose.testing.assertRotationInRootIsEqualTo
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.testutils.XrDeviceTest
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Automated instrument tests for the Panel/Layout Rotation CUJ test case in Compose XR.
 *
 * Covers:
 * - Individual SpatialPanel rotation around arbitrary axis vectors and Euler/Quaternion angles
 * - SpatialRow layout rotation with synchronized child panels
 * - SpatialColumn layout rotation with synchronized child panels
 * - Composite layout (rotated column, rotated row, and standalone rotating panel)
 * - Subspace rotation property assertion before and after dynamic state mutation
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class PanelRotationTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        Assume.assumeTrue(
            "XR spatial environment is not supported on this device/emulator",
            SpatialConfiguration.hasXrSpatialFeature(context),
        )
    }

    /** Validates that an individual SpatialPanel rotates and updates across angle changes. */
    @Test
    fun panelRotation_individualPanel_rotatesAroundAxis() {
        var rotationAngle by mutableFloatStateOf(0f)
        val axis = Vector3.Up

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    modifier =
                        SubspaceModifier.width(200.dp)
                            .height(150.dp)
                            .rotate(axisAngle = axis, rotation = rotationAngle)
                            .testTag("standalone_panel")
                ) {
                    Text("Standalone Rotated Panel: $rotationAngle")
                }
            }
        }

        // Assert initial rotation and UI
        composeTestRule
            .onSubspaceNodeWithTag("standalone_panel")
            .assertRotationInRootIsEqualTo(Quaternion.Identity)
        composeTestRule
            .onAllNodesWithText("Standalone Rotated Panel: 0.0")
            .onFirst()
            .assertIsDisplayed()

        // Dynamically update rotation angle to 45 degrees
        rotationAngle = 45f

        val expectedRotation45 = Quaternion.fromAxisAngle(axis, 45f)
        composeTestRule
            .onSubspaceNodeWithTag("standalone_panel")
            .assertRotationInRootIsEqualTo(expectedRotation45)
        composeTestRule
            .onAllNodesWithText("Standalone Rotated Panel: 45.0")
            .onFirst()
            .assertIsDisplayed()

        // Dynamically update to negative rotation angle (-90 degrees)
        rotationAngle = -90f

        val expectedRotationNeg90 = Quaternion.fromAxisAngle(axis, -90f)
        composeTestRule
            .onSubspaceNodeWithTag("standalone_panel")
            .assertRotationInRootIsEqualTo(expectedRotationNeg90)
        composeTestRule
            .onAllNodesWithText("Standalone Rotated Panel: -90.0")
            .onFirst()
            .assertIsDisplayed()
    }

    /** Validates that a SpatialRow rotates all its child panels in sync. */
    @Test
    fun panelRotation_spatialRow_rotatesAllChildrenInSync() {
        var rotationAngle by mutableFloatStateOf(30f)
        val axis = Vector3.Right

        composeTestRule.setContent {
            Subspace {
                SpatialRow(
                    modifier =
                        SubspaceModifier.rotate(axisAngle = axis, rotation = rotationAngle)
                            .offset(z = 1.dp)
                ) {
                    SpatialPanel(
                        modifier = SubspaceModifier.width(180.dp).height(120.dp).testTag("row_left")
                    ) {
                        Text("Row Left")
                    }
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.width(180.dp).height(120.dp).testTag("row_center")
                    ) {
                        Text("Row Center")
                    }
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.width(180.dp).height(120.dp).testTag("row_right")
                    ) {
                        Text("Row Right")
                    }
                }
            }
        }

        // Assert initial rotation on child panels
        val expectedRotation30 = Quaternion.fromAxisAngle(axis, 30f)
        composeTestRule
            .onSubspaceNodeWithTag("row_left")
            .assertRotationInRootIsEqualTo(expectedRotation30)
        composeTestRule
            .onSubspaceNodeWithTag("row_center")
            .assertRotationInRootIsEqualTo(expectedRotation30)
        composeTestRule
            .onSubspaceNodeWithTag("row_right")
            .assertRotationInRootIsEqualTo(expectedRotation30)

        composeTestRule.onAllNodesWithText("Row Left").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Row Center").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Row Right").onFirst().assertIsDisplayed()

        // Mutate rotation to 75 degrees
        rotationAngle = 75f

        val expectedRotation75 = Quaternion.fromAxisAngle(axis, 75f)
        composeTestRule
            .onSubspaceNodeWithTag("row_left")
            .assertRotationInRootIsEqualTo(expectedRotation75)
        composeTestRule
            .onSubspaceNodeWithTag("row_center")
            .assertRotationInRootIsEqualTo(expectedRotation75)
        composeTestRule
            .onSubspaceNodeWithTag("row_right")
            .assertRotationInRootIsEqualTo(expectedRotation75)

        composeTestRule.onAllNodesWithText("Row Left").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Row Center").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Row Right").onFirst().assertIsDisplayed()
    }

    /** Validates that a SpatialColumn rotates all its child panels in sync. */
    @Test
    fun panelRotation_spatialColumn_rotatesAllChildrenInSync() {
        var rotationAngle by mutableFloatStateOf(15f)
        val axis = Vector3.Forward

        composeTestRule.setContent {
            Subspace {
                SpatialColumn(
                    modifier = SubspaceModifier.rotate(axisAngle = axis, rotation = rotationAngle)
                ) {
                    SpatialPanel(
                        modifier = SubspaceModifier.width(180.dp).height(120.dp).testTag("col_top")
                    ) {
                        Text("Column Top")
                    }
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.width(180.dp).height(120.dp).testTag("col_middle")
                    ) {
                        Text("Column Middle")
                    }
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.width(180.dp).height(120.dp).testTag("col_bottom")
                    ) {
                        Text("Column Bottom")
                    }
                }
            }
        }

        // Assert initial rotation on child panels
        val expectedRotation15 = Quaternion.fromAxisAngle(axis, 15f)
        composeTestRule
            .onSubspaceNodeWithTag("col_top")
            .assertRotationInRootIsEqualTo(expectedRotation15)
        composeTestRule
            .onSubspaceNodeWithTag("col_middle")
            .assertRotationInRootIsEqualTo(expectedRotation15)
        composeTestRule
            .onSubspaceNodeWithTag("col_bottom")
            .assertRotationInRootIsEqualTo(expectedRotation15)

        composeTestRule.onAllNodesWithText("Column Top").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Column Middle").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Column Bottom").onFirst().assertIsDisplayed()

        // Mutate rotation to -45 degrees
        rotationAngle = -45f

        val expectedRotationNeg45 = Quaternion.fromAxisAngle(axis, -45f)
        composeTestRule
            .onSubspaceNodeWithTag("col_top")
            .assertRotationInRootIsEqualTo(expectedRotationNeg45)
        composeTestRule
            .onSubspaceNodeWithTag("col_middle")
            .assertRotationInRootIsEqualTo(expectedRotationNeg45)
        composeTestRule
            .onSubspaceNodeWithTag("col_bottom")
            .assertRotationInRootIsEqualTo(expectedRotationNeg45)

        composeTestRule.onAllNodesWithText("Column Top").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Column Middle").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Column Bottom").onFirst().assertIsDisplayed()
    }

    /**
     * Validates the composite CUJ layout containing a rotated column on the left, a rotated row in
     * the middle, a standalone rotated panel on the right, and an info panel.
     */
    @Test
    fun panelRotation_compositeLayout_rotatesColumnRowAndStandalonePanels() {
        var rotation by mutableFloatStateOf(30f)
        val axisAngle = Vector3(1f, 1f, 0f)

        composeTestRule.setContent {
            Subspace {
                SpatialColumn {
                    SpatialRow {
                        // Left: Column with 3 panels
                        SpatialColumn(modifier = SubspaceModifier.rotate(axisAngle, rotation)) {
                            SpatialPanel(
                                modifier =
                                    SubspaceModifier.width(160.dp)
                                        .height(100.dp)
                                        .testTag("panel_top")
                            ) {
                                Text("Panel top")
                            }
                            SpatialPanel(
                                modifier =
                                    SubspaceModifier.width(160.dp)
                                        .height(100.dp)
                                        .testTag("panel_middle")
                            ) {
                                Text("Panel middle")
                            }
                            SpatialPanel(
                                modifier =
                                    SubspaceModifier.width(160.dp)
                                        .height(100.dp)
                                        .testTag("panel_bottom")
                            ) {
                                Text("Panel bottom")
                            }
                        }

                        // Middle: Row with 3 panels
                        SpatialColumn {
                            SpatialRow(
                                modifier =
                                    SubspaceModifier.rotate(axisAngle, rotation).offset(z = 1.dp)
                            ) {
                                SpatialPanel(
                                    modifier =
                                        SubspaceModifier.width(160.dp)
                                            .height(100.dp)
                                            .testTag("panel_left")
                                ) {
                                    Text("Panel left")
                                }
                                SpatialPanel(
                                    modifier =
                                        SubspaceModifier.width(160.dp)
                                            .height(100.dp)
                                            .testTag("panel_center")
                                ) {
                                    Text("Panel center")
                                }
                                SpatialPanel(
                                    modifier =
                                        SubspaceModifier.width(160.dp)
                                            .height(100.dp)
                                            .testTag("panel_right")
                                ) {
                                    Text("Panel right")
                                }
                            }
                        }

                        // Right: Standalone rotating panel
                        SpatialPanel(
                            modifier =
                                SubspaceModifier.width(160.dp)
                                    .height(100.dp)
                                    .rotate(axisAngle, rotation)
                                    .testTag("panel_standalone")
                        ) {
                            Text("Standalone Rotating Panel")
                        }
                    }

                    SpatialSpacer(modifier = SubspaceModifier.height(20.dp))

                    // Bottom info panel
                    SpatialPanel(modifier = SubspaceModifier.width(400.dp).height(150.dp)) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Rotation Info: $rotation")
                        }
                    }
                }
            }
        }

        // Assert initial rotation on column panels, row panels, and standalone panel
        val expectedRotation30 = Quaternion.fromAxisAngle(axisAngle, 30f)
        composeTestRule
            .onSubspaceNodeWithTag("panel_top")
            .assertRotationInRootIsEqualTo(expectedRotation30)
        composeTestRule
            .onSubspaceNodeWithTag("panel_middle")
            .assertRotationInRootIsEqualTo(expectedRotation30)
        composeTestRule
            .onSubspaceNodeWithTag("panel_bottom")
            .assertRotationInRootIsEqualTo(expectedRotation30)
        composeTestRule
            .onSubspaceNodeWithTag("panel_left")
            .assertRotationInRootIsEqualTo(expectedRotation30)
        composeTestRule
            .onSubspaceNodeWithTag("panel_center")
            .assertRotationInRootIsEqualTo(expectedRotation30)
        composeTestRule
            .onSubspaceNodeWithTag("panel_right")
            .assertRotationInRootIsEqualTo(expectedRotation30)
        composeTestRule
            .onSubspaceNodeWithTag("panel_standalone")
            .assertRotationInRootIsEqualTo(expectedRotation30)

        // Validate all panels are rendered
        composeTestRule.onAllNodesWithText("Panel top").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Panel middle").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Panel bottom").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Panel left").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Panel center").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Panel right").onFirst().assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Standalone Rotating Panel")
            .onFirst()
            .assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Rotation Info: 30.0").onFirst().assertIsDisplayed()

        // Update shared rotation state to 60 degrees
        rotation = 60f

        val expectedRotation60 = Quaternion.fromAxisAngle(axisAngle, 60f)
        composeTestRule
            .onSubspaceNodeWithTag("panel_top")
            .assertRotationInRootIsEqualTo(expectedRotation60)
        composeTestRule
            .onSubspaceNodeWithTag("panel_middle")
            .assertRotationInRootIsEqualTo(expectedRotation60)
        composeTestRule
            .onSubspaceNodeWithTag("panel_bottom")
            .assertRotationInRootIsEqualTo(expectedRotation60)
        composeTestRule
            .onSubspaceNodeWithTag("panel_left")
            .assertRotationInRootIsEqualTo(expectedRotation60)
        composeTestRule
            .onSubspaceNodeWithTag("panel_center")
            .assertRotationInRootIsEqualTo(expectedRotation60)
        composeTestRule
            .onSubspaceNodeWithTag("panel_right")
            .assertRotationInRootIsEqualTo(expectedRotation60)
        composeTestRule
            .onSubspaceNodeWithTag("panel_standalone")
            .assertRotationInRootIsEqualTo(expectedRotation60)

        composeTestRule.onAllNodesWithText("Rotation Info: 60.0").onFirst().assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("Standalone Rotating Panel")
            .onFirst()
            .assertIsDisplayed()
    }

    /** Validates that Euler angles and Quaternion rotation overloads apply cleanly. */
    @Test
    fun panelRotation_eulerAndQuaternionOverloads_applyCleanly() {
        var eulerPitch by mutableFloatStateOf(45f)
        var quaternionRotation by mutableStateOf(Quaternion.fromAxisAngle(Vector3(0f, 1f, 0f), 90f))

        composeTestRule.setContent {
            Subspace {
                SpatialRow {
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.width(180.dp)
                                .height(120.dp)
                                .rotate(pitch = eulerPitch, yaw = 0f, roll = 0f)
                                .testTag("euler_panel")
                    ) {
                        Text("Euler Panel: $eulerPitch")
                    }

                    SpatialPanel(
                        modifier =
                            SubspaceModifier.width(180.dp)
                                .height(120.dp)
                                .rotate(quaternionRotation)
                                .testTag("quaternion_panel")
                    ) {
                        Text("Quaternion Panel")
                    }
                }
            }
        }

        // Assert initial rotations on Euler and Quaternion panels
        val expectedEulerInitial = Quaternion.fromEulerAngles(pitch = 45f, yaw = 0f, roll = 0f)
        val expectedQuaternionInitial = Quaternion.fromAxisAngle(Vector3(0f, 1f, 0f), 90f)
        composeTestRule
            .onSubspaceNodeWithTag("euler_panel")
            .assertRotationInRootIsEqualTo(expectedEulerInitial)
        composeTestRule
            .onSubspaceNodeWithTag("quaternion_panel")
            .assertRotationInRootIsEqualTo(expectedQuaternionInitial)

        composeTestRule.onAllNodesWithText("Euler Panel: 45.0").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Quaternion Panel").onFirst().assertIsDisplayed()

        // Update rotations
        eulerPitch = -30f
        quaternionRotation = Quaternion.fromAxisAngle(Vector3(1f, 0f, 0f), 45f)

        val expectedEulerUpdated = Quaternion.fromEulerAngles(pitch = -30f, yaw = 0f, roll = 0f)
        val expectedQuaternionUpdated = Quaternion.fromAxisAngle(Vector3(1f, 0f, 0f), 45f)
        composeTestRule
            .onSubspaceNodeWithTag("euler_panel")
            .assertRotationInRootIsEqualTo(expectedEulerUpdated)
        composeTestRule
            .onSubspaceNodeWithTag("quaternion_panel")
            .assertRotationInRootIsEqualTo(expectedQuaternionUpdated)

        composeTestRule.onAllNodesWithText("Euler Panel: -30.0").onFirst().assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Quaternion Panel").onFirst().assertIsDisplayed()
    }
}
