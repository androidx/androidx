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

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.arcore.testing.FakePerceptionManager
import androidx.xr.arcore.testing.FakePerceptionRuntime
import androidx.xr.compose.spatial.ExperimentalFollowingSubspaceApi
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialBox
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.assertRotationInRootIsEqualTo
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.compose.testing.session
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Quaternion.Companion.fromRotation
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.Space
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.android.controller.ActivityController

@RunWith(AndroidJUnit4::class)
class RotateToLookAtUserTest {
    private val testDispatcher = StandardTestDispatcher()
    // Migrate to `androidx.compose.ui.test.junit4.v2.createAndroidComposeRule`,
    // available starting with v1.11.0.
    // See API docs for details.
    @Suppress("DEPRECATION")
    @get:Rule
    val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()
    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity

    @Before
    @OptIn(ExperimentalFollowingSubspaceApi::class)
    fun setUp() {
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.get()
    }

    @Test
    fun rotateToLookAtUser_userTranslationChanges_contentTurnsTowardsUser() =
        runTest(testDispatcher) {
            val fakePerceptionManager = createSessionAndGetPerceptionManager()

            composeTestRule.setContent {
                Subspace {
                    SpatialPanel(SubspaceModifier.testTag("TheWatcher").rotateToLookAtUser()) {
                        Text(text = "Panel")
                    }
                }
            }

            composeTestRule
                .onSubspaceNodeWithTag("TheWatcher")
                .assertRotationInRootIsEqualTo(Quaternion.Identity)

            val watcherEntity = composeTestRule.getTaggedEntity("TheWatcher")

            val userLocation = Vector3(x = 1F, y = 2F, z = 3F)
            fakePerceptionManager.arDevice.apply {
                devicePose = devicePose.translate(translation = userLocation)
            }

            testDispatcher.scheduler.advanceUntilIdle()
            composeTestRule.waitForIdle()

            val watcherWorldPose = watcherEntity.getPose(Space.REAL_WORLD)
            val targetVector = (userLocation - watcherWorldPose.translation).toNormalized()
            val expectedRotation = Quaternion.fromLookTowards(targetVector, Vector3(0f, 1f, 0f))

            composeTestRule
                .onSubspaceNodeWithTag("TheWatcher")
                .assertRotationInRootIsEqualTo(expectedRotation, tolerance = 0.04f)
        }

    @Test
    fun rotateToLookAtUser_withGravityAligned_ignoresPitchRotation_andContentTurnsTowardsUser() =
        runTest(testDispatcher) {
            val fakePerceptionManager = createSessionAndGetPerceptionManager()

            composeTestRule.setContent {
                Subspace {
                    SpatialPanel(
                        SubspaceModifier.testTag("TheWatcher")
                            // Apply an initial pitch rotation to test billboard behavior
                            .rotate(pitch = 30f)
                            .rotateToLookAtUser()
                            .gravityAligned()
                    ) {
                        Text(text = "Panel")
                    }
                }
            }

            composeTestRule
                .onSubspaceNodeWithTag("TheWatcher")
                .assertRotationInRootIsEqualTo(Quaternion.Identity)

            val watcherEntity =
                assertNotNull(
                    composeTestRule
                        .onSubspaceNodeWithTag("TheWatcher")
                        .fetchSemanticsNode()
                        .semanticsEntity
                )

            val userLocation = Vector3(x = 1F, y = 2F, z = 3F)
            fakePerceptionManager.arDevice.apply {
                devicePose = devicePose.translate(translation = userLocation)
            }

            testDispatcher.scheduler.advanceUntilIdle()
            composeTestRule.waitForIdle()

            val watcherWorldPose = watcherEntity.getPose(Space.REAL_WORLD)
            val expectedRotation =
                getBillboardRotationNeeded(watcherWorldPose.translation, userLocation)

            composeTestRule
                .onSubspaceNodeWithTag("TheWatcher")
                .assertRotationInRootIsEqualTo(expectedRotation, tolerance = 0.04f)
        }

    @Test
    fun rotateToLookAtUser_withRotation_retainsOffset() =
        runTest(testDispatcher) {
            val fakePerceptionManager = createSessionAndGetPerceptionManager()
            val fixedRotateOffset = Quaternion.fromEulerAngles(pitch = 40f, yaw = 30f, roll = 20f)

            composeTestRule.setContent {
                Subspace {
                    SpatialPanel(
                        SubspaceModifier.testTag("TheWatcher")
                            .rotateToLookAtUser()
                            .rotate(pitch = 40f, yaw = 30f, roll = 20f)
                    ) {
                        Text(text = "Panel")
                    }
                }
            }

            val watcherEntity = composeTestRule.getTaggedEntity("TheWatcher")

            composeTestRule
                .onSubspaceNodeWithTag("TheWatcher")
                .assertRotationInRootIsEqualTo(fixedRotateOffset)

            val userLocation = Vector3(x = 1F, y = 2F, z = 3F)
            fakePerceptionManager.arDevice.apply {
                devicePose = devicePose.translate(translation = userLocation)
            }

            testDispatcher.scheduler.advanceUntilIdle()
            composeTestRule.waitForIdle()

            val watcherWorldPose = watcherEntity.getPose(Space.REAL_WORLD)
            val targetVector = (userLocation - watcherWorldPose.translation).toNormalized()
            val lookAtUserRotationTowardsUser =
                Quaternion.fromLookTowards(targetVector, Vector3(0f, 1f, 0f))
            val expectedRotation = lookAtUserRotationTowardsUser * fixedRotateOffset

            composeTestRule
                .onSubspaceNodeWithTag("TheWatcher")
                .assertRotationInRootIsEqualTo(expectedRotation, tolerance = 0.04f)
        }

    @Test
    fun rotateToLookAtUser_withGravityAlignedAndRotation_retainsOffset() =
        runTest(testDispatcher) {
            val fakePerceptionManager = createSessionAndGetPerceptionManager()
            val fixedRotateOffset = Quaternion.fromEulerAngles(pitch = 40f, yaw = 30f, roll = 20f)

            composeTestRule.setContent {
                Subspace {
                    SpatialPanel(
                        SubspaceModifier.testTag("TheWatcher")
                            // Apply an initial pitch rotation to test billboard behavior
                            .rotate(pitch = 30f)
                            .rotateToLookAtUser()
                            .gravityAligned()
                            .rotate(pitch = 40f, yaw = 30f, roll = 20f)
                    ) {
                        Text(text = "Panel")
                    }
                }
            }

            val watcherEntity = composeTestRule.getTaggedEntity("TheWatcher")

            composeTestRule
                .onSubspaceNodeWithTag("TheWatcher")
                .assertRotationInRootIsEqualTo(fixedRotateOffset)

            val userLocation = Vector3(x = 1F, y = 2F, z = 3F)
            fakePerceptionManager.arDevice.apply {
                devicePose = devicePose.translate(translation = userLocation)
            }

            testDispatcher.scheduler.advanceUntilIdle()
            composeTestRule.waitForIdle()

            val watcherWorldPose = watcherEntity.getPose(Space.REAL_WORLD)
            val billboardRotationTowardsUser =
                getBillboardRotationNeeded(watcherWorldPose.translation, userLocation)
            val expectedRotation = billboardRotationTowardsUser * fixedRotateOffset

            composeTestRule
                .onSubspaceNodeWithTag("TheWatcher")
                .assertRotationInRootIsEqualTo(expectedRotation, tolerance = 0.04f)
        }

    @Test
    fun rotateToLookAtUser_precededByRotation_ignoresRotation() =
        runTest(testDispatcher) {
            val fakePerceptionManager = createSessionAndGetPerceptionManager()
            val localRotation = Quaternion.fromEulerAngles(pitch = 40f, yaw = 30f, roll = 20f)

            composeTestRule.setContent {
                Subspace {
                    SpatialPanel(
                        SubspaceModifier.testTag("TheWatcher")
                            .rotate(localRotation)
                            .rotateToLookAtUser()
                    ) {
                        Text(text = "Panel")
                    }
                }
            }

            val watcherEntity = composeTestRule.getTaggedEntity("TheWatcher")

            composeTestRule
                .onSubspaceNodeWithTag("TheWatcher")
                .assertRotationInRootIsEqualTo(Quaternion.Identity)

            val userLocation = Vector3(x = 1F, y = 2F, z = 3F)
            fakePerceptionManager.arDevice.apply {
                devicePose = devicePose.translate(translation = userLocation)
            }

            testDispatcher.scheduler.advanceUntilIdle()
            composeTestRule.waitForIdle()

            val watcherWorldPose = watcherEntity.getPose(Space.REAL_WORLD)
            val targetVector = (userLocation - watcherWorldPose.translation).toNormalized()
            val expectedRotation = Quaternion.fromLookTowards(targetVector, Vector3(0f, 1f, 0f))

            composeTestRule
                .onSubspaceNodeWithTag("TheWatcher")
                .assertRotationInRootIsEqualTo(expectedRotation, tolerance = 0.04f)
        }

    @Test
    fun rotateToLookAtUser_withRotatedParent_ignoresParentRotation() =
        runTest(testDispatcher) {
            val fakePerceptionManager = createSessionAndGetPerceptionManager()
            val parentRotation = Quaternion.fromEulerAngles(pitch = 40f, yaw = 30f, roll = 20f)

            composeTestRule.setContent {
                Subspace {
                    SpatialBox(SubspaceModifier.rotate(parentRotation)) {
                        SpatialPanel(SubspaceModifier.testTag("child").rotateToLookAtUser()) {
                            Text(text = "Panel")
                        }
                    }
                }
            }

            val watcherEntity = composeTestRule.getTaggedEntity("child")

            composeTestRule
                .onSubspaceNodeWithTag("child")
                .assertRotationInRootIsEqualTo(parentRotation, tolerance = 0.04f)

            val userLocation = Vector3(x = 1F, y = 2F, z = 3F)
            fakePerceptionManager.arDevice.apply {
                devicePose = devicePose.translate(translation = userLocation)
            }

            testDispatcher.scheduler.advanceUntilIdle()
            composeTestRule.waitForIdle()

            val watcherWorldPose = watcherEntity.getPose(Space.REAL_WORLD)
            val targetVector = (userLocation - watcherWorldPose.translation).toNormalized()
            val expectedWorldRotation =
                Quaternion.fromLookTowards(targetVector, Vector3(0f, 1f, 0f))

            composeTestRule
                .onSubspaceNodeWithTag("child")
                .assertRotationInRootIsEqualTo(expectedWorldRotation, tolerance = 0.04f)
        }

    private fun createSessionAndGetPerceptionManager(): FakePerceptionManager {
        val sessionCreateResult = Session.create(composeTestRule.activity, testDispatcher)
        assertThat(sessionCreateResult).isInstanceOf(SessionCreateSuccess::class.java)
        val session = (sessionCreateResult as SessionCreateSuccess).session
        session.configure(
            config = session.config.copy(deviceTracking = DeviceTrackingMode.LAST_KNOWN)
        )
        composeTestRule.session = session
        val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()
        return fakeRuntime.perceptionManager
    }

    private fun AndroidComposeTestRule<*, *>.getTaggedEntity(tag: String): Entity {
        val node = this.onSubspaceNodeWithTag(tag)
        val semantics = node.fetchSemanticsNode()
        return assertNotNull(semantics.semanticsEntity, "Entity not found for tag: $tag")
    }

    fun getBillboardRotationNeeded(billboardLocation: Vector3, userLocation: Vector3): Quaternion {
        val rawTargetVector = userLocation - billboardLocation
        // Flatten the vector to the XZ-plane to ensure Y-axis-only rotation.
        val flatTargetVector = Vector3(rawTargetVector.x, 0f, rawTargetVector.z).toNormalized()
        // Calculate the quaternion that rotates from (0,0,1) to the new XZ target.
        val initialForwardVector = Vector3(0f, 0f, 1f)
        return fromRotation(initialForwardVector, flatTargetVector)
    }
}
