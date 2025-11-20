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
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.arcore.testing.FakePerceptionRuntime
import androidx.xr.compose.spatial.ExperimentalUserSubspaceApi
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.assertRotationInRootIsEqualTo
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.compose.testing.session
import androidx.xr.runtime.Config
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Quaternion.Companion.fromRotation
import androidx.xr.runtime.math.Vector3
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
class LookAtUserTest {
    private val testDispatcher = StandardTestDispatcher()
    @get:Rule val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity

    @Before
    @OptIn(ExperimentalUserSubspaceApi::class)
    fun setUp() {
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.get()
    }

    @Test
    fun billboard_userTranslationChanges_contentTurnsTowardsUser() =
        runTest(testDispatcher) {
            val result = Session.create(composeTestRule.activity, testDispatcher)
            val session = (result as SessionCreateSuccess).session
            session.configure(
                config = session.config.copy(headTracking = Config.HeadTrackingMode.LAST_KNOWN)
            )
            composeTestRule.session = session

            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()
            val fakePerceptionManager = fakeRuntime.perceptionManager

            composeTestRule.setContent {
                Subspace {
                    SpatialPanel(SubspaceModifier.testTag("TheWatcher").billboard()) {
                        Text(text = "Panel")
                    }
                }
            }

            composeTestRule
                .onSubspaceNodeWithTag("TheWatcher")
                .assertRotationInRootIsEqualTo(Quaternion.Identity)

            val watcherNode = composeTestRule.onSubspaceNodeWithTag("TheWatcher")
            val watcherSemanticNode = watcherNode.fetchSemanticsNode()
            val watcherEntity = assertNotNull(watcherSemanticNode.semanticsEntity)
            assertThat(watcherNode).isNotNull()

            // Update device location to where we're pretending the user moved to.
            val userLocation = Vector3(x = 1F, y = 2F, z = 3F)
            fakePerceptionManager.arDevice.devicePose =
                fakePerceptionManager.arDevice.devicePose.translate(translation = userLocation)

            // Wait for the device pose updates to be propagated and handled.
            testDispatcher.scheduler.advanceUntilIdle()

            // Wait for Compose to redraw the scene.
            composeTestRule.waitForIdle()

            val watcherWorldPose = watcherEntity.getPose(Space.REAL_WORLD)
            val expectedRotation =
                getBillboardRotationNeeded(watcherWorldPose.translation, userLocation)

            composeTestRule
                .onSubspaceNodeWithTag("TheWatcher")
                .assertRotationInRootIsEqualTo(expectedRotation, tolerance = 0.04f)
        }

    @Test
    fun lookAtUser_userTranslationChanges_contentTurnsTowardsUser() =
        runTest(testDispatcher) {
            val result = Session.create(composeTestRule.activity, testDispatcher)
            val session = (result as SessionCreateSuccess).session
            session.configure(
                config = session.config.copy(headTracking = Config.HeadTrackingMode.LAST_KNOWN)
            )
            composeTestRule.session = session

            val fakeRuntime = session.runtimes.filterIsInstance<FakePerceptionRuntime>().first()
            val fakePerceptionManager = fakeRuntime.perceptionManager

            composeTestRule.setContent {
                Subspace {
                    SpatialPanel(SubspaceModifier.testTag("TheWatcher").lookAtUser()) {
                        Text(text = "Panel")
                    }
                }
            }

            composeTestRule
                .onSubspaceNodeWithTag("TheWatcher")
                .assertRotationInRootIsEqualTo(Quaternion.Identity)

            val watcherNode = composeTestRule.onSubspaceNodeWithTag("TheWatcher")
            val watcherSemanticNode = watcherNode.fetchSemanticsNode()
            val watcherEntity = assertNotNull(watcherSemanticNode.semanticsEntity)
            assertThat(watcherNode).isNotNull()

            // Update device location to where we're pretending the user moved to.
            val userLocation = Vector3(x = 1F, y = 2F, z = 3F)
            fakePerceptionManager.arDevice.devicePose =
                fakePerceptionManager.arDevice.devicePose.translate(translation = userLocation)

            // Wait for the device pose updates to be propagated and handled.
            testDispatcher.scheduler.advanceUntilIdle()

            // Wait for Compose to redraw the scene.
            composeTestRule.waitForIdle()

            val watcherWorldPose = watcherEntity.getPose(Space.REAL_WORLD)

            // Direction vector from entity to user.
            val targetVector = (userLocation - watcherWorldPose.translation).toNormalized()
            // Calculate the rotation needed relative to the targetVector
            val expectedRotation = Quaternion.fromLookTowards(targetVector, Vector3(0f, 1f, 0f))

            composeTestRule
                .onSubspaceNodeWithTag("TheWatcher")
                .assertRotationInRootIsEqualTo(expectedRotation, tolerance = 0.04f)
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
