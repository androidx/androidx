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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.spatial.ExperimentalFollowingSubspaceApi
import androidx.xr.compose.spatial.LocalSubspaceRootNode
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialBox
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.semantics.testTag
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.assertRotationInRootIsEqualTo
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.compose.testing.session
import androidx.xr.compose.unit.Meter.Companion.meters
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Quaternion.Companion.fromRotation
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.Space
import androidx.xr.scenecore.scene
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
    @Suppress("DEPRECATION")
    // TODO: b/494305963 Remove references to arcore-testing Fakes
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

            val watcherWorldPose = watcherEntity.getPose(Space.ACTIVITY)
            val targetVector = userLocation - watcherWorldPose.translation
            val expectedRotation = Quaternion.fromLookTowards(targetVector, Vector3(0f, 1f, 0f))

            composeTestRule
                .onSubspaceNodeWithTag("TheWatcher")
                .assertRotationInRootIsEqualTo(expectedRotation)
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

            val watcherWorldPose = watcherEntity.getPose(Space.ACTIVITY)
            val expectedRotation =
                getBillboardRotationNeeded(watcherWorldPose.translation, userLocation)

            composeTestRule
                .onSubspaceNodeWithTag("TheWatcher")
                .assertRotationInRootIsEqualTo(expectedRotation)
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

            val watcherWorldPose = watcherEntity.getPose(Space.ACTIVITY)
            val targetVector = userLocation - watcherWorldPose.translation
            val lookAtUserRotationTowardsUser =
                Quaternion.fromLookTowards(targetVector, Vector3(0f, 1f, 0f))
            val expectedRotation = lookAtUserRotationTowardsUser * fixedRotateOffset

            composeTestRule
                .onSubspaceNodeWithTag("TheWatcher")
                .assertRotationInRootIsEqualTo(expectedRotation)
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

            val watcherWorldPose = watcherEntity.getPose(Space.ACTIVITY)
            val billboardRotationTowardsUser =
                getBillboardRotationNeeded(watcherWorldPose.translation, userLocation)
            val expectedRotation = billboardRotationTowardsUser * fixedRotateOffset

            composeTestRule
                .onSubspaceNodeWithTag("TheWatcher")
                .assertRotationInRootIsEqualTo(expectedRotation)
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

            val watcherWorldPose = watcherEntity.getPose(Space.ACTIVITY)
            val targetVector = userLocation - watcherWorldPose.translation
            val expectedRotation = Quaternion.fromLookTowards(targetVector, Vector3(0f, 1f, 0f))

            composeTestRule
                .onSubspaceNodeWithTag("TheWatcher")
                .assertRotationInRootIsEqualTo(expectedRotation)
        }

    @Test
    fun rotateToLookAtUser_withRotatedParent_ignoresParentRotation() =
        runTest(testDispatcher) {
            val fakePerceptionManager = createSessionAndGetPerceptionManager()
            val parentRotation = Quaternion.fromEulerAngles(pitch = 40f, yaw = 30f, roll = 20f)

            val userLocation = Vector3(x = 1F, y = 2F, z = 3F)
            fakePerceptionManager.arDevice.apply {
                devicePose = devicePose.translate(translation = userLocation)
            }

            composeTestRule.setContent {
                Subspace {
                    SpatialBox(SubspaceModifier.rotate(parentRotation)) {
                        SpatialPanel(SubspaceModifier.testTag("child").rotateToLookAtUser()) {
                            Text(text = "Panel")
                        }
                    }
                }
            }

            testDispatcher.scheduler.advanceUntilIdle()
            composeTestRule.waitForIdle()

            val watcherEntity = composeTestRule.getTaggedEntity("child")
            val watcherWorldPose = watcherEntity.getPose(Space.ACTIVITY)
            val targetVector = userLocation - watcherWorldPose.translation
            val expectedWorldRotation =
                Quaternion.fromLookTowards(targetVector, Vector3(0f, 1f, 0f))

            composeTestRule
                .onSubspaceNodeWithTag("child")
                .assertRotationInRootIsEqualTo(expectedWorldRotation)
        }

    @Test
    fun rotateToLookAtUser_withTranslatedRoot_calculatesCorrectLookDirection() =
        runTest(testDispatcher) {
            val fakePerceptionManager = createSessionAndGetPerceptionManager()

            val userLocation = Vector3(x = 1F, y = 0F, z = 3F)

            // Pre-initialize before composition to ensure the first tracking tick captures the
            // target geometry and avoids simulation deadlock.
            fakePerceptionManager.arDevice.apply {
                devicePose = devicePose.translate(translation = userLocation)
            }

            composeTestRule.setContent {
                Subspace {
                    // Nest inside a container offset by exactly 1.0m to provide the parent
                    // translation.
                    SpatialBox(SubspaceModifier.offset(x = 1.meters.toDp())) {
                        // Node has no local offset. It should perfectly inherit the parent offset.
                        SpatialPanel(SubspaceModifier.testTag("TheWatcher").rotateToLookAtUser()) {
                            Text(text = "Panel")
                        }
                    }
                }
            }

            testDispatcher.scheduler.advanceUntilIdle()
            composeTestRule.waitForIdle()

            // Mathematical Verification:
            // Parent Offset: +1m X
            // User Location: +1m X, +3m Z
            // Direction Vector: [1,0,3] - [1,0,0] = [0,0,3] (+Z Forward)
            // Therefore, final local rotation MUST be Identity.
            val expectedRotation = Quaternion.Identity

            // Verify that the look-at calculation uses the correct absolute position.
            // Because the Root is at X=1m and the Node has no local offset, the Node's absolute
            // position is X=1m.
            // With the User placed at X=1m, Z=3m, the Node and User share the exact same X-axis.
            // Therefore, the mathematically correct look direction points purely along the positive
            // Z-axis. In this right-handed coordinate system, +Z maps to Vector3.Backward.
            composeTestRule
                .onSubspaceNodeWithTag("TheWatcher")
                .assertRotationInRootIsEqualTo(expectedRotation)
        }

    @Test
    fun rotateToLookAtUser_withOffset_contentTurnsTowardsUser() =
        runTest(testDispatcher) {
            val fakePerceptionManager = createSessionAndGetPerceptionManager()
            val offsetDp = 500.dp

            composeTestRule.setContent {
                Subspace {
                    SpatialPanel(
                        SubspaceModifier.testTag("TheWatcher")
                            .offset(x = offsetDp, y = offsetDp, z = offsetDp)
                            .rotateToLookAtUser()
                    ) {
                        Text(text = "Offset Panel")
                    }
                }
            }

            val watcherEntity = composeTestRule.getTaggedEntity("TheWatcher")

            val userLocation = Vector3(x = 1F, y = 2F, z = 3F)
            fakePerceptionManager.arDevice.apply {
                devicePose = devicePose.translate(translation = userLocation)
            }

            testDispatcher.scheduler.advanceUntilIdle()
            composeTestRule.waitForIdle()

            val watcherWorldPose = watcherEntity.getPose(Space.ACTIVITY)
            val targetVector = (userLocation - watcherWorldPose.translation)
            val expectedRotation = Quaternion.fromLookTowards(targetVector, Vector3(0f, 1f, 0f))

            composeTestRule
                .onSubspaceNodeWithTag("TheWatcher")
                .assertRotationInRootIsEqualTo(expectedRotation)
        }

    @Test
    fun rotateToLookAtUser_withOffsetParent_contentTurnsTowardsUser() =
        runTest(testDispatcher) {
            val fakePerceptionManager = createSessionAndGetPerceptionManager()
            val parentOffsetDp = 300.dp

            composeTestRule.setContent {
                Subspace {
                    SpatialBox(SubspaceModifier.offset(x = parentOffsetDp)) {
                        SpatialPanel(
                            SubspaceModifier.testTag("TheWatcherChild").rotateToLookAtUser()
                        ) {
                            Text(text = "Child Panel")
                        }
                    }
                }
            }

            val watcherEntity = composeTestRule.getTaggedEntity("TheWatcherChild")

            val userLocation = Vector3(x = 1F, y = 2F, z = 3F)
            fakePerceptionManager.arDevice.apply {
                devicePose = devicePose.translate(translation = userLocation)
            }

            testDispatcher.scheduler.advanceUntilIdle()
            composeTestRule.waitForIdle()

            val watcherWorldPose = watcherEntity.getPose(Space.ACTIVITY)
            val targetVector = (userLocation - watcherWorldPose.translation)
            val expectedRotation = Quaternion.fromLookTowards(targetVector, Vector3.Up)

            composeTestRule
                .onSubspaceNodeWithTag("TheWatcherChild")
                .assertRotationInRootIsEqualTo(expectedRotation)
        }

    @Test
    fun rotateToLookAtUser_userDirectlyAbove_handlesSingularityWithoutCrash() =
        runTest(testDispatcher) {
            val fakePerceptionManager = createSessionAndGetPerceptionManager()

            // Place the user directly above the root origin to trigger the singularity.
            val userLocation = Vector3(x = 0F, y = 3F, z = 0F)
            fakePerceptionManager.arDevice.apply {
                devicePose = devicePose.translate(translation = userLocation)
            }

            val customRootNode =
                Entity.create(
                    session = assertNotNull(composeTestRule.session),
                    name = "customRootNode",
                    parent = assertNotNull(composeTestRule.session).scene.activitySpace,
                )
            customRootNode.setPose(relativeTo = Space.ACTIVITY, pose = Pose.Identity)

            composeTestRule.setContent {
                CompositionLocalProvider(LocalSubspaceRootNode provides customRootNode) {
                    Subspace {
                        // Node is directly at origin, directly underneath the user.
                        SpatialPanel(SubspaceModifier.testTag("TheWatcher").rotateToLookAtUser()) {
                            Text(text = "Target")
                        }
                    }
                }
            }

            testDispatcher.scheduler.advanceUntilIdle()
            composeTestRule.waitForIdle()

            // Mathematically verify behavior. When directly beneath, forward = +Y.
            // The fallback up is Vector3.Forward [0, 0, -1].
            // The resulting math translates this to exactly -90 degrees around the X-axis.
            val expectedRotation = Quaternion(x = -0.7071068f, y = 0f, z = 0f, w = 0.7071068f)

            composeTestRule
                .onSubspaceNodeWithTag("TheWatcher")
                .assertRotationInRootIsEqualTo(expectedRotation)
        }

    @Test
    fun rotateToLookAtUser_userAlignedWithCustomUpDirection_remainsStable() =
        runTest(testDispatcher) {
            val fakePerceptionManager = createSessionAndGetPerceptionManager()

            // Position the user in front of the node along the Z axis.
            val userLocation = Vector3(x = 0F, y = 0F, z = 3F)

            fakePerceptionManager.arDevice.apply {
                devicePose = devicePose.translate(translation = userLocation)
            }

            // Explicitly set a custom upDirection that is collinear with the target vector (Gimbal
            // Lock condition)
            // We set the upDirection to Vector3.Backward [0,0,1], matching the target direction to
            // the user.
            val customUpDirection = Vector3.Backward

            composeTestRule.setContent {
                Subspace {
                    SpatialPanel(
                        SubspaceModifier.testTag("TheWatcher")
                            .rotateToLookAtUser(upDirection = customUpDirection)
                    ) {
                        Text(text = "Target")
                    }
                }
            }

            testDispatcher.scheduler.advanceUntilIdle()
            composeTestRule.waitForIdle()

            // Verify it does not collapse into NaN. The logic correctly derives
            // up = [0, 1, 0] from the plane, and atan2(0, 0) safely yields 0 degrees.
            // So we should settle precisely back into the default Identity orientation.
            val expectedRotation = Quaternion.Identity

            composeTestRule
                .onSubspaceNodeWithTag("TheWatcher")
                .assertRotationInRootIsEqualTo(expectedRotation)
        }

    @Suppress("DEPRECATION")
    // TODO: b/494305963 Remove references to arcore-testing Fakes
    private fun createSessionAndGetPerceptionManager():
        androidx.xr.arcore.testing.FakePerceptionManager {
        val sessionCreateResult = Session.create(composeTestRule.activity, testDispatcher)
        assertThat(sessionCreateResult).isInstanceOf(SessionCreateSuccess::class.java)
        val session = (sessionCreateResult as SessionCreateSuccess).session
        session.configure(config = session.config.copy(deviceTracking = DeviceTrackingMode.SPATIAL))
        composeTestRule.session = session
        val fakeRuntime =
            session.runtimes
                .filterIsInstance<androidx.xr.arcore.testing.FakePerceptionRuntime>()
                .first()
        return fakeRuntime.perceptionManager
    }

    private fun AndroidComposeTestRule<*, *>.getTaggedEntity(tag: String): Entity {
        val node = this.onSubspaceNodeWithTag(tag)
        val semantics = node.fetchSemanticsNode()
        return assertNotNull(semantics.semanticsEntity, "Entity not found for tag: $tag")
    }

    private fun getBillboardRotationNeeded(
        billboardLocation: Vector3,
        userLocation: Vector3,
    ): Quaternion {
        val rawTargetVector = userLocation - billboardLocation
        // Flatten the vector to the XZ-plane to ensure Y-axis-only rotation.
        val flatTargetVector = Vector3(rawTargetVector.x, 0f, rawTargetVector.z).toNormalized()
        // Calculate the quaternion that rotates from (0,0,1) to the new XZ target.
        val initialForwardVector = Vector3(0f, 0f, 1f)
        return fromRotation(initialForwardVector, flatTargetVector)
    }
}
