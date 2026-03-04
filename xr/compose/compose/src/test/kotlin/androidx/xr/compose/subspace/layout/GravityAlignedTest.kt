/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.xr.compose.subspace.layout

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.spatial.LocalSubspaceRootNode
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialBox
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.assertPositionIsEqualTo
import androidx.xr.compose.testing.assertRotationInRootIsEqualTo
import androidx.xr.compose.testing.assertRotationIsEqualTo
import androidx.xr.compose.testing.configureFakeSession
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.compose.testing.session
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.Space
import com.google.common.truth.Truth.assertThat
import kotlin.test.Ignore
import kotlin.test.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GravityAlignedTest {

    // Migrate to `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`,
    // available starting with v1.11.0.
    // See API docs for details.
    @Suppress("DEPRECATION")
    @get:Rule
    val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun gravityAligned_parentIsLevel_appliesNoRotation() {
        composeTestRule.setContent {
            Subspace {
                SpatialBox {
                    SpatialPanel(SubspaceModifier.testTag("child").gravityAligned()) {
                        Text(text = "Panel")
                    }
                }
            }
        }

        // With an identity parent, the result is identity.
        composeTestRule
            .onSubspaceNodeWithTag("child")
            .assertPositionIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertRotationIsEqualTo(Quaternion.Identity)
            .assertRotationInRootIsEqualTo(Quaternion.Identity)
    }

    @Test
    fun gravityAligned_parentHasPitchAndRoll_appliesCounterRotation() {
        val parentRotation = Quaternion.fromEulerAngles(pitch = 30f, yaw = 0f, roll = 45f)

        composeTestRule.setContent {
            Subspace {
                SpatialBox(SubspaceModifier.rotate(parentRotation)) {
                    SpatialPanel(SubspaceModifier.testTag("child").gravityAligned()) {
                        Text(text = "Panel")
                    }
                }
            }
        }

        val yawOnlyRotation =
            Quaternion.fromEulerAngles(pitch = 0f, yaw = parentRotation.eulerAngles.y, roll = 0f)
        // Quaternion(x = -0.2391f, y = 0.099f, z = -0.3696f, w = 0.8924f)
        val expectedCounterRotation = parentRotation.inverse * yawOnlyRotation
        // Quaternion.Identity
        val expectedRootRotation = parentRotation * expectedCounterRotation

        composeTestRule
            .onSubspaceNodeWithTag("child")
            .assertPositionIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertRotationIsEqualTo(expectedCounterRotation)
            .assertRotationInRootIsEqualTo(expectedRootRotation)
    }

    @Test
    fun gravityAligned_parentHasYaw_preservesYaw() {
        val parentRotation = Quaternion.fromEulerAngles(pitch = 20f, yaw = 60f, roll = -25f)
        composeTestRule.setContent {
            Subspace {
                SpatialBox(SubspaceModifier.rotate(parentRotation)) {
                    SpatialPanel(SubspaceModifier.testTag("child").gravityAligned()) {
                        Text(text = "Panel")
                    }
                }
            }
        }

        // Using the simpler version of the gravity aligned logic to derive the counter rotation.
        // This is okay since this unit test does not hit the DEGENERATE case.
        val yawOnlyRotation =
            Quaternion.fromEulerAngles(pitch = 0f, yaw = parentRotation.eulerAngles.y, roll = 0f)
        // Quaternion(x = -0.1695f, y = -0.0376f, z = 0.2132f, w = 0.9615f)
        val expectedCounterRotation = parentRotation.inverse * yawOnlyRotation
        // Quaternion.fromEulerAngles(pitch = 0f, yaw = 60f, roll = 0f)
        val expectedRootRotation = parentRotation * expectedCounterRotation

        composeTestRule
            .onSubspaceNodeWithTag("child")
            .assertPositionIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertRotationIsEqualTo(expectedCounterRotation)
            .assertRotationInRootIsEqualTo(expectedRootRotation)
    }

    @Test
    fun gravityAligned_parentRotationChanges_updatesCounterRotation() {
        var parentRotation by
            mutableStateOf(Quaternion.fromEulerAngles(pitch = 10f, yaw = 0f, roll = 15f))

        composeTestRule.setContent {
            Subspace {
                SpatialBox(SubspaceModifier.rotate(parentRotation)) {
                    SpatialPanel(SubspaceModifier.testTag("child").gravityAligned()) {
                        Text(text = "$parentRotation")
                    }
                }
            }
        }

        var yawOnlyRotation =
            Quaternion.fromEulerAngles(pitch = 0f, yaw = parentRotation.eulerAngles.y, roll = 0f)
        // Quaternion(x = -0.0864f, y = 0.0114f, z = -0.13f, w = 0.9877f)
        var expectedCounterRotation = parentRotation.inverse * yawOnlyRotation
        // Quaternion.Identity
        var expectedRootRotation = parentRotation * expectedCounterRotation

        composeTestRule
            .onSubspaceNodeWithTag("child")
            .assertRotationIsEqualTo(expectedCounterRotation)
            .assertRotationInRootIsEqualTo(expectedRootRotation)

        // Update state
        parentRotation = Quaternion.fromEulerAngles(pitch = -40f, yaw = 20f, roll = -30f)
        composeTestRule.waitForIdle()

        yawOnlyRotation =
            Quaternion.fromEulerAngles(pitch = 0f, yaw = parentRotation.eulerAngles.y, roll = 0f)
        expectedCounterRotation = parentRotation.inverse * yawOnlyRotation
        // Quaternion.fromEulerAngles(pitch = 0f, yaw = 20f, roll = 0f)
        expectedRootRotation = parentRotation * expectedCounterRotation

        composeTestRule
            .onSubspaceNodeWithTag("child")
            .assertRotationIsEqualTo(expectedCounterRotation)
            .assertRotationInRootIsEqualTo(expectedRootRotation)
    }

    @Test
    fun gravityAligned_parentHasNegativeRotation_appliesCounterRotation() {
        val parentRotation = Quaternion.fromEulerAngles(pitch = -15f, yaw = 0f, roll = -50f)

        composeTestRule.setContent {
            Subspace {
                SpatialBox(SubspaceModifier.rotate(parentRotation)) {
                    SpatialPanel(SubspaceModifier.testTag("child").gravityAligned()) {
                        Text(text = "Panel")
                    }
                }
            }
        }

        val yawOnlyRotation =
            Quaternion.fromEulerAngles(pitch = 0f, yaw = parentRotation.eulerAngles.y, roll = 0f)
        // Quaternion(x = 0.1183f, y = 0.0552f, z = 0.419f, w = 0.8986f)
        val expectedCounterRotation = parentRotation.inverse * yawOnlyRotation
        // Quaternion.Identity
        val expectedRootRotation = parentRotation * expectedCounterRotation

        composeTestRule
            .onSubspaceNodeWithTag("child")
            .assertPositionIsEqualTo(0.dp, 0.dp, 0.dp)
            .assertRotationIsEqualTo(expectedCounterRotation)
            .assertRotationInRootIsEqualTo(expectedRootRotation)
    }

    @Test
    fun gravityAligned_chainedWithRotate_appliesCombinedRotation() {
        val parentRotation = Quaternion.fromEulerAngles(pitch = -30f, yaw = 11f, roll = 22f)
        val localRotation = Quaternion.fromEulerAngles(pitch = 17f, yaw = 29f, roll = 39f)

        composeTestRule.setContent {
            Subspace {
                SpatialBox(SubspaceModifier.rotate(parentRotation)) {
                    SpatialPanel(
                        SubspaceModifier.rotate(localRotation).gravityAligned().testTag("child")
                    ) {
                        Text(text = "Panel")
                    }
                }
            }
        }

        val totalEffectiveRotation = parentRotation * localRotation
        val yawOnlyRotation =
            Quaternion.fromEulerAngles(
                pitch = 0f,
                yaw = totalEffectiveRotation.eulerAngles.y,
                roll = 0f,
            )
        val counterRotation = totalEffectiveRotation.inverse * yawOnlyRotation
        // Quaternion(x = 0f, y = 0.38997f, z = 0f, w = 0.9208f)
        val expectedRootRotation = totalEffectiveRotation * counterRotation

        composeTestRule
            .onSubspaceNodeWithTag("child")
            .assertRotationInRootIsEqualTo(expectedRootRotation)
    }

    @Test
    fun gravityAligned_chainedBetweenTwoRotations_appliesCombinedRotation() {
        val parentRotation = Quaternion.fromEulerAngles(pitch = 20f, yaw = 0f, roll = 20f)
        val innerLocalRotation = Quaternion.fromEulerAngles(pitch = 10f, yaw = 10f, roll = 0f)
        val outerLocalRotation = Quaternion.fromEulerAngles(pitch = 0f, yaw = 30f, roll = 0f)

        composeTestRule.setContent {
            Subspace {
                SpatialBox(SubspaceModifier.rotate(parentRotation)) {
                    SpatialPanel(
                        SubspaceModifier.rotate(innerLocalRotation)
                            .gravityAligned()
                            .rotate(outerLocalRotation)
                            .testTag("child")
                    ) {
                        Text(text = "Panel")
                    }
                }
            }
        }

        val totalEffectiveRotation = parentRotation * innerLocalRotation
        val targetYaw = totalEffectiveRotation.eulerAngles.y
        val targetYawOnlyRotation =
            Quaternion.fromEulerAngles(pitch = 0f, yaw = targetYaw, roll = 0f)
        val counterRotation = totalEffectiveRotation.inverse * targetYawOnlyRotation
        val finalCombinedLocalRotation = totalEffectiveRotation * counterRotation
        // Quaternion(x = 0f, y = 0.3755f, z = 0f, w = 0.9268f)
        val expectedRotationInRoot = finalCombinedLocalRotation * outerLocalRotation

        composeTestRule
            .onSubspaceNodeWithTag("child")
            .assertRotationInRootIsEqualTo(expectedRotationInRoot)
    }

    @Test
    fun gravityAligned_chainedAfterTwoRotations_appliesCombinedRotation() {
        val parentRotation = Quaternion.fromEulerAngles(pitch = 20f, yaw = 0f, roll = 20f)
        val childInnerRotation = Quaternion.fromEulerAngles(pitch = 10f, yaw = 10f, roll = 0f)
        val childOuterRotation = Quaternion.fromEulerAngles(pitch = 0f, yaw = 30f, roll = 0f)

        composeTestRule.setContent {
            Subspace {
                SpatialBox(SubspaceModifier.rotate(parentRotation)) {
                    SpatialPanel(
                        SubspaceModifier.rotate(childInnerRotation)
                            .rotate(childOuterRotation)
                            .gravityAligned()
                            .testTag("child")
                    ) {
                        Text(text = "Panel")
                    }
                }
            }
        }

        val totalEffectiveRotation = parentRotation * childInnerRotation * childOuterRotation
        val targetYaw = totalEffectiveRotation.eulerAngles.y
        val yawOnlyRotation = Quaternion.fromEulerAngles(pitch = 0f, yaw = targetYaw, roll = 0f)
        // Quaternion(x = 0f, y = 0.3556f, z = 0f, w = 0.9346f)
        val counterRotation = totalEffectiveRotation.inverse * yawOnlyRotation
        val expectedRootRotation = totalEffectiveRotation * counterRotation

        composeTestRule
            .onSubspaceNodeWithTag("child")
            .assertRotationInRootIsEqualTo(expectedRootRotation, tolerance = 0.04f)
    }

    @Test
    fun gravityAligned_modifierIsRemoved_resetsRotation() {
        var isGravityAligned by mutableStateOf(true)
        val parentRotation = Quaternion.fromEulerAngles(pitch = 30f, yaw = 0f, roll = 45f)

        composeTestRule.setContent {
            Subspace {
                SpatialBox(SubspaceModifier.rotate(parentRotation)) {
                    val modifier =
                        if (isGravityAligned) {
                            SubspaceModifier.gravityAligned()
                        } else {
                            SubspaceModifier
                        }
                    SpatialPanel(modifier.testTag("child")) { Text(text = "Panel") }
                }
            }
        }

        val yawOnlyRotation =
            Quaternion.fromEulerAngles(pitch = 0f, yaw = parentRotation.eulerAngles.y, roll = 0f)
        // Quaternion(x = -0.2391f, y = 0.099f, z = -0.3696f, w = 0.8924f)
        val expectedCounterRotation = parentRotation.inverse * yawOnlyRotation
        // Quaternion.Identity
        val expectedRotationInRoot = parentRotation * expectedCounterRotation

        composeTestRule
            .onSubspaceNodeWithTag("child")
            .assertRotationIsEqualTo(expectedCounterRotation)
            .assertRotationInRootIsEqualTo(expectedRotationInRoot)

        isGravityAligned = false
        composeTestRule.waitForIdle()

        // Without modifier: no calculation needed, local rotation is identity.
        composeTestRule
            .onSubspaceNodeWithTag("child")
            .assertRotationIsEqualTo(Quaternion.Identity)
            .assertRotationInRootIsEqualTo(parentRotation)
    }

    @Test
    @Ignore("b/448989958 - The SceneCore Fakes need to be updated to support this test.")
    fun gravityAligned_onSubspace_alignsTiltedRootToWorld() {
        composeTestRule.configureFakeSession()
        val tiltedRootNode = Entity.create(checkNotNull(composeTestRule.session), "tiltedRootNode")
        val tiltedRootRotation = Quaternion.fromEulerAngles(pitch = 20f, yaw = 60f, roll = -25f)
        tiltedRootNode.setPose(
            relativeTo = Space.REAL_WORLD,
            pose = Pose(rotation = tiltedRootRotation),
        )

        composeTestRule.setContent {
            CompositionLocalProvider(LocalSubspaceRootNode provides tiltedRootNode) {
                Subspace(modifier = SubspaceModifier.gravityAligned()) {
                    SpatialPanel(modifier = SubspaceModifier.testTag("panel")) {
                        Text(text = "Panel")
                    }
                }
            }
        }

        val yawOnlyRotation =
            Quaternion.fromEulerAngles(
                pitch = 0f,
                yaw = tiltedRootRotation.eulerAngles.y,
                roll = 0f,
            )
        val expectedCounterRotation = tiltedRootRotation.inverse * yawOnlyRotation
        val panelEntity =
            composeTestRule.onSubspaceNodeWithTag("panel").fetchSemanticsNode().semanticsEntity
        assertNotNull(panelEntity)
        val actualFinalWorldRotation = panelEntity.getPose(relativeTo = Space.REAL_WORLD).rotation
        val angleDifference = Quaternion.angle(actualFinalWorldRotation, yawOnlyRotation)
        assertThat(angleDifference).isLessThan(0.01f)
        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertRotationIsEqualTo(Quaternion.Identity)
            .assertRotationInRootIsEqualTo(expectedCounterRotation)
    }

    @Test
    fun gravityAligned_degenerateCase_ultimateFallback() {
        // This parent rotation points straight down.
        val verticalParentRotation = Quaternion.fromEulerAngles(pitch = 90f, yaw = 45f, roll = 0f)

        composeTestRule.setContent {
            Subspace {
                SpatialBox(SubspaceModifier.rotate(verticalParentRotation)) {
                    // The child has no additional rotation.
                    SpatialPanel(SubspaceModifier.gravityAligned().testTag("child")) {
                        Text(text = "Panel")
                    }
                }
            }
        }

        val expectedRootRotation = Quaternion.Identity

        composeTestRule
            .onSubspaceNodeWithTag("child")
            .assertRotationInRootIsEqualTo(expectedRootRotation)
    }
}
