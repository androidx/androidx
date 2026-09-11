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
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.compose.platform.SpatialConfiguration
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialBox
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialRow
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.gravityAligned
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.rotate
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.compose.testing.assertPositionIsEqualTo
import androidx.xr.compose.testing.assertRotationInRootIsEqualTo
import androidx.xr.compose.testing.assertRotationIsEqualTo
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.runtime.math.Quaternion
import androidx.xr.testutils.XrDeviceTest
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Automated instrumented tests for the [gravityAligned] modifier CUJ in Compose for XR.
 *
 * Covers:
 * - Level parent hierarchy applying no counter-rotation to a gravity-aligned panel
 * - Tilted parent hierarchy with pitch and roll counter-rotated to remain level with gravity
 * - Tilted parent hierarchy with yaw preserving horizontal heading while removing pitch/roll
 * - Multi-level nested rotations (SpatialRow + SpatialBox) correctly compensated by gravityAligned
 * - Chained local rotations composed with gravityAligned modifier
 * - Dynamic addition and removal of the gravityAligned modifier in response to state changes
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@XrDeviceTest
class GravityAlignedModifierTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        Assume.assumeTrue(
            "XR spatial environment is not supported on this device/emulator",
            SpatialConfiguration.hasXrSpatialFeature(context),
        )
    }

    /** Validates that a gravity-aligned panel under a level parent maintains identity rotation. */
    @Test
    fun gravityAligned_parentIsLevel_appliesNoRotation() {
        composeTestRule.setContent {
            Subspace {
                SpatialBox(modifier = SubspaceModifier.testTag("parentBox")) {
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.width(200.dp)
                                .height(150.dp)
                                .gravityAligned()
                                .testTag("gravityPanel")
                    ) {
                        Text("Level Gravity Panel")
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Level Gravity Panel").assertExists()
        composeTestRule
            .onSubspaceNodeWithTag("gravityPanel")
            .assertPositionIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertRotationIsEqualTo(Quaternion.Identity)
            .assertRotationInRootIsEqualTo(Quaternion.Identity)
    }

    /**
     * Validates that a gravity-aligned panel counter-rotates pitch and roll introduced by a tilted
     * parent to keep its up-vector aligned with world gravity.
     */
    @Test
    fun gravityAligned_parentHasPitchAndRoll_appliesCounterRotation() {
        val parentRotation = Quaternion.fromEulerAngles(pitch = 30f, yaw = 0f, roll = 45f)

        composeTestRule.setContent {
            Subspace {
                SpatialBox(
                    modifier = SubspaceModifier.rotate(parentRotation).testTag("tiltedParentBox")
                ) {
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.width(200.dp)
                                .height(150.dp)
                                .gravityAligned()
                                .testTag("gravityPanel")
                    ) {
                        Text("Compensated Panel")
                    }
                }
            }
        }

        val yawOnlyRotation =
            Quaternion.fromEulerAngles(pitch = 0f, yaw = parentRotation.eulerAngles.y, roll = 0f)
        val expectedCounterRotation = parentRotation.inverse * yawOnlyRotation
        val expectedRootRotation = parentRotation * expectedCounterRotation

        composeTestRule.onNodeWithText("Compensated Panel").assertExists()
        composeTestRule
            .onSubspaceNodeWithTag("gravityPanel")
            .assertPositionIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertRotationIsEqualTo(expectedCounterRotation)
            .assertRotationInRootIsEqualTo(expectedRootRotation)
    }

    /**
     * Validates that gravity alignment strips pitch and roll tilts while preserving the yaw
     * (heading) of the parent hierarchy.
     */
    @Test
    fun gravityAligned_parentHasYaw_preservesYawOnly() {
        val parentRotation = Quaternion.fromEulerAngles(pitch = 20f, yaw = 60f, roll = -25f)

        composeTestRule.setContent {
            Subspace {
                SpatialBox(
                    modifier = SubspaceModifier.rotate(parentRotation).testTag("tiltedParentBox")
                ) {
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.width(200.dp)
                                .height(150.dp)
                                .gravityAligned()
                                .testTag("gravityPanel")
                    ) {
                        Text("Yaw Preserved Panel")
                    }
                }
            }
        }

        val yawOnlyRotation =
            Quaternion.fromEulerAngles(pitch = 0f, yaw = parentRotation.eulerAngles.y, roll = 0f)
        val expectedCounterRotation = parentRotation.inverse * yawOnlyRotation
        val expectedRootRotation = parentRotation * expectedCounterRotation

        composeTestRule.onNodeWithText("Yaw Preserved Panel").assertExists()
        composeTestRule
            .onSubspaceNodeWithTag("gravityPanel")
            .assertPositionIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertRotationIsEqualTo(expectedCounterRotation)
            .assertRotationInRootIsEqualTo(expectedRootRotation)
    }

    /**
     * Validates that gravity alignment works across multi-layer nested rotations (e.g., tilted
     * SpatialRow containing a rotated SpatialBox).
     */
    @Test
    fun gravityAligned_nestedRotations_alignsWithGravity() {
        val rowRotation = Quaternion.fromEulerAngles(pitch = 20f, yaw = -17f, roll = -61f)
        val innerBoxRotation = Quaternion.fromEulerAngles(pitch = 15f, yaw = 10f, roll = 25f)

        composeTestRule.setContent {
            Subspace {
                SpatialRow(modifier = SubspaceModifier.rotate(rowRotation).testTag("tiltedRow")) {
                    SpatialBox(
                        modifier = SubspaceModifier.rotate(innerBoxRotation).testTag("innerBox")
                    ) {
                        SpatialPanel(
                            modifier =
                                SubspaceModifier.width(200.dp)
                                    .height(150.dp)
                                    .gravityAligned()
                                    .testTag("gravityPanel")
                        ) {
                            Text("Nested Gravity Panel")
                        }
                    }
                }
            }
        }

        val totalEffectiveRotation = rowRotation * innerBoxRotation
        val yawOnlyRotation =
            Quaternion.fromEulerAngles(
                pitch = 0f,
                yaw = totalEffectiveRotation.eulerAngles.y,
                roll = 0f,
            )
        val expectedCounterRotation = totalEffectiveRotation.inverse * yawOnlyRotation
        val expectedRootRotation = totalEffectiveRotation * expectedCounterRotation

        composeTestRule.onNodeWithText("Nested Gravity Panel").assertExists()
        composeTestRule
            .onSubspaceNodeWithTag("gravityPanel")
            .assertPositionIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertRotationIsEqualTo(expectedCounterRotation)
            .assertRotationInRootIsEqualTo(expectedRootRotation)
    }

    /**
     * Validates modifier composition when a local rotate modifier is chained before gravityAligned.
     */
    @Test
    fun gravityAligned_chainedWithLocalRotate_appliesCombinedGravityAlignment() {
        val parentRotation = Quaternion.fromEulerAngles(pitch = -30f, yaw = 11f, roll = 22f)
        val childLocalRotation = Quaternion.fromEulerAngles(pitch = 17f, yaw = 29f, roll = 39f)

        composeTestRule.setContent {
            Subspace {
                SpatialBox(
                    modifier = SubspaceModifier.rotate(parentRotation).testTag("parentBox")
                ) {
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.width(200.dp)
                                .height(150.dp)
                                .rotate(childLocalRotation)
                                .gravityAligned()
                                .testTag("chainedGravityPanel")
                    ) {
                        Text("Chained Rotate Panel")
                    }
                }
            }
        }

        val totalEffectiveRotation = parentRotation * childLocalRotation
        val yawOnlyRotation =
            Quaternion.fromEulerAngles(
                pitch = 0f,
                yaw = totalEffectiveRotation.eulerAngles.y,
                roll = 0f,
            )
        val expectedCounterRotation = totalEffectiveRotation.inverse * yawOnlyRotation
        // In Compose Subspace, node.pose reflects the placement of the innermost modifier
        // in the chain (GravityAlignedNode), which applies expectedCounterRotation.
        val expectedLocalRotation = expectedCounterRotation
        val expectedRootRotation = totalEffectiveRotation * expectedCounterRotation

        composeTestRule.onNodeWithText("Chained Rotate Panel").assertExists()
        composeTestRule
            .onSubspaceNodeWithTag("chainedGravityPanel")
            .assertRotationIsEqualTo(expectedLocalRotation)
            .assertRotationInRootIsEqualTo(expectedRootRotation)
    }

    /**
     * Validates that dynamically toggling the gravityAligned modifier switches the panel between
     * gravity alignment and inheriting parent rotation.
     */
    @Test
    fun gravityAligned_dynamicToggle_togglesAlignment() {
        var isGravityAligned by mutableStateOf(true)
        val parentRotation = Quaternion.fromEulerAngles(pitch = 30f, yaw = 0f, roll = 45f)

        composeTestRule.setContent {
            Subspace {
                SpatialBox(
                    modifier = SubspaceModifier.rotate(parentRotation).testTag("parentBox")
                ) {
                    val alignmentModifier =
                        if (isGravityAligned) {
                            SubspaceModifier.gravityAligned()
                        } else {
                            SubspaceModifier
                        }
                    SpatialPanel(
                        modifier =
                            SubspaceModifier.width(200.dp)
                                .height(150.dp)
                                .then(alignmentModifier)
                                .testTag("dynamicPanel")
                    ) {
                        Text(if (isGravityAligned) "Gravity Aligned" else "Tilted With Parent")
                    }
                }
            }
        }

        val yawOnlyRotation =
            Quaternion.fromEulerAngles(pitch = 0f, yaw = parentRotation.eulerAngles.y, roll = 0f)
        val expectedCounterRotation = parentRotation.inverse * yawOnlyRotation
        val expectedRootRotation = parentRotation * expectedCounterRotation

        // 1. Verify gravity-aligned state
        composeTestRule.onNodeWithText("Gravity Aligned").assertExists()
        composeTestRule
            .onSubspaceNodeWithTag("dynamicPanel")
            .assertRotationIsEqualTo(expectedCounterRotation)
            .assertRotationInRootIsEqualTo(expectedRootRotation)

        // 2. Disable gravity alignment
        composeTestRule.runOnUiThread { isGravityAligned = false }
        composeTestRule.waitForIdle()

        // 3. Verify unaligned state: inherits parent tilt directly
        composeTestRule.onNodeWithText("Tilted With Parent").assertExists()
        composeTestRule
            .onSubspaceNodeWithTag("dynamicPanel")
            .assertRotationIsEqualTo(Quaternion.Identity)
            .assertRotationInRootIsEqualTo(parentRotation)
    }
}
