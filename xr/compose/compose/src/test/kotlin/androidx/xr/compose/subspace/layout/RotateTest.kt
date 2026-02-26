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
import androidx.xr.compose.subspace.SpatialBox
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.assertRotationInRootIsEqualTo
import androidx.xr.compose.testing.assertRotationIsEqualTo
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [rotate] modifier. */
@RunWith(AndroidJUnit4::class)
class RotateTest {

    // Migrate to `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`,
    // available starting with v1.11.0.
    // See API docs for details.
    @Suppress("DEPRECATION")
    @get:Rule
    val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun rotation_canApplySingleRotation() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel").rotate(pitch = 90f, yaw = 0f, roll = 0f)
                ) {
                    Text(text = "Panel")
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertRotationInRootIsEqualTo(Quaternion(0.70710677f, 0.0f, 0.0f, 0.70710677f))
    }

    @Test
    fun rotation_canRotateAcrossTwoAxis() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel").rotate(Vector3(0.0f, 1.0f, 1.0f), 90.0f)
                ) {
                    Text(text = "Panel")
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertRotationInRootIsEqualTo(Quaternion(0.0f, 0.49999997f, 0.49999997f, 0.70710677f))
    }

    @Test
    fun rotate_zeroRotation_appliesIdentity() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel").rotate(0f, 0f, 0f)) {
                    Text(text = "Panel")
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertRotationInRootIsEqualTo(Quaternion.Identity)
    }

    @Test
    fun rotate_defaultPitchYawRoll_appliesIdentity() {
        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel").rotate(pitch = 0f)) {
                    Text(text = "Panel")
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertRotationInRootIsEqualTo(Quaternion.Identity)
    }

    @Test
    fun rotate_quaternionOverload_isAppliedCorrectly() {
        val rotation = Quaternion.fromAxisAngle(Vector3(1f, 1f, 0f).toNormalized(), 60f)

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.testTag("panel").rotate(rotation)) {
                    Text(text = "Panel")
                }
            }
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertRotationInRootIsEqualTo(rotation)
    }

    @Test
    fun rotate_negativeAngles_areAppliedCorrectly() {
        val expectedRotation = Quaternion.fromEulerAngles(pitch = -90f, yaw = 0f, roll = -45f)

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel").rotate(pitch = -90f, yaw = 0f, roll = -45f)
                ) {
                    Text(text = "Panel")
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertRotationInRootIsEqualTo(expectedRotation)
    }

    @Test
    fun rotate_updatesWhenStateChanges() {
        var currentRotation by mutableStateOf(Quaternion.fromEulerAngles(pitch = 10f, 0f, 0f))

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(SubspaceModifier.rotate(currentRotation).testTag("panel")) {
                    Text(text = "Panel")
                }
            }
        }

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertRotationInRootIsEqualTo(currentRotation)

        currentRotation = Quaternion.fromEulerAngles(pitch = 0f, yaw = -45f, roll = 0f)

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertRotationInRootIsEqualTo(currentRotation)
    }

    @Test
    fun rotate_chainedRotations_composedInCorrectOrder() {
        val innerRotation = Quaternion.fromEulerAngles(pitch = 45f, yaw = 0f, roll = 0f)
        val outerRotation = Quaternion.fromEulerAngles(pitch = 0f, yaw = 30f, roll = 0f)

        composeTestRule.setContent {
            Subspace {
                SpatialPanel(
                    SubspaceModifier.testTag("panel").rotate(innerRotation).rotate(outerRotation)
                ) {
                    Text(text = "Panel")
                }
            }
        }

        // The final rotation should be the product of the two rotations.
        // Outer modifiers are applied after inner ones.
        val expectedRotation = innerRotation * outerRotation

        composeTestRule
            .onSubspaceNodeWithTag("panel")
            .assertRotationIsEqualTo(outerRotation)
            .assertRotationInRootIsEqualTo(expectedRotation)
    }

    @Test
    fun rotate_nestedAndChained_composedInCorrectOrder() {
        val parentRotation = Quaternion.fromEulerAngles(pitch = 30f, yaw = 0f, roll = 30f)
        val childRotation = Quaternion.fromEulerAngles(pitch = 0f, yaw = 45f, roll = 0f)

        composeTestRule.setContent {
            Subspace {
                SpatialBox(SubspaceModifier.rotate(parentRotation)) {
                    SpatialPanel(SubspaceModifier.rotate(childRotation).testTag("Panel")) {
                        Text(text = "Panel")
                    }
                }
            }
        }

        // The rotate modifier combines rotations in `Parent * Local` order.
        val expectedRotation = parentRotation * childRotation

        composeTestRule
            .onSubspaceNodeWithTag("Panel")
            .assertRotationIsEqualTo(childRotation)
            .assertRotationInRootIsEqualTo(expectedRotation)
    }

    @Test
    fun rotate_nestedAndNested_composedInCorrectOrder() {
        val grandParentRotation = Quaternion.fromEulerAngles(pitch = 30f, yaw = 0f, roll = 30f)
        val parentRotation = Quaternion.fromEulerAngles(pitch = 30f, yaw = 0f, roll = 30f)
        val childRotation = Quaternion.fromEulerAngles(pitch = 0f, yaw = 45f, roll = 0f)

        composeTestRule.setContent {
            Subspace {
                SpatialBox(SubspaceModifier.rotate(grandParentRotation)) {
                    SpatialBox(SubspaceModifier.rotate(parentRotation)) {
                        SpatialPanel(SubspaceModifier.rotate(childRotation).testTag("Panel")) {
                            Text(text = "Panel")
                        }
                    }
                }
            }
        }

        // The rotate modifier combines rotations in `Parent * Local` order.
        val expectedRotation = grandParentRotation * parentRotation * childRotation

        composeTestRule
            .onSubspaceNodeWithTag("Panel")
            .assertRotationIsEqualTo(childRotation)
            .assertRotationInRootIsEqualTo(expectedRotation)
    }
}
