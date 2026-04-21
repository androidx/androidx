/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.arcore

import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.arcore.testing.ArCoreTestRule
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.manifest.HEAD_TRACKING
import androidx.xr.runtime.math.FieldOfView
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

@RunWith(AndroidJUnit4::class)
class RenderViewpointTest {

    @Rule @JvmField val arCoreTestRule = ArCoreTestRule()

    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope
    private lateinit var session: Session

    companion object {
        val EXPECTED_FOV = FieldOfView(1f, 2f, 3f, 4f)
        val EXPECTED_POSE = Pose(Vector3(1f, 2f, 3f), Quaternion(4f, 5f, 6f, 7f))
        val EXPECTED_DEVICE_POSE =
            Pose(
                translation = Vector3(2f, 0f, 0f),
                rotation = Quaternion.fromAxisAngle(Vector3.Up, 90f),
            )
        val EXPECTED_OBJECT_POSE = Pose(translation = Vector3(0f, 0f, -3f), rotation = Quaternion())
    }

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.get()

        shadowOf(activity.application).grantPermissions(HEAD_TRACKING)

        activityController.create().start().resume()

        session = (Session.create(activity, testDispatcher) as SessionCreateSuccess).session
        session.configure(Config(deviceTracking = DeviceTrackingMode.SPATIAL_LAST_KNOWN))

        arCoreTestRule.device.pose = Pose()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun left_returnsPoseAndFov() =
        runTest(testDispatcher) {
            arCoreTestRule.leftRenderViewpoint.pose = EXPECTED_POSE
            arCoreTestRule.leftRenderViewpoint.fieldOfView = EXPECTED_FOV

            advanceUntilIdle()

            val underTest = RenderViewpoint.left(session)!!
            assertThat(underTest.state.value.pose).isEqualTo(EXPECTED_POSE)
            assertThat(underTest.state.value.localPose).isEqualTo(EXPECTED_POSE)
            assertThat(underTest.state.value.fieldOfView.angleLeft)
                .isEqualTo(EXPECTED_FOV.angleLeft)
            assertThat(underTest.state.value.fieldOfView.angleRight)
                .isEqualTo(EXPECTED_FOV.angleRight)
            assertThat(underTest.state.value.fieldOfView.angleUp).isEqualTo(EXPECTED_FOV.angleUp)
            assertThat(underTest.state.value.fieldOfView.angleDown)
                .isEqualTo(EXPECTED_FOV.angleDown)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun right_returnsPoseAndFov() =
        runTest(testDispatcher) {
            arCoreTestRule.rightRenderViewpoint.pose = EXPECTED_POSE
            arCoreTestRule.rightRenderViewpoint.fieldOfView = EXPECTED_FOV

            advanceUntilIdle()

            val underTest = RenderViewpoint.right(session)!!
            assertThat(underTest.state.value.pose).isEqualTo(EXPECTED_POSE)
            assertThat(underTest.state.value.localPose).isEqualTo(EXPECTED_POSE)
            assertThat(underTest.state.value.fieldOfView.angleLeft)
                .isEqualTo(EXPECTED_FOV.angleLeft)
            assertThat(underTest.state.value.fieldOfView.angleRight)
                .isEqualTo(EXPECTED_FOV.angleRight)
            assertThat(underTest.state.value.fieldOfView.angleUp).isEqualTo(EXPECTED_FOV.angleUp)
            assertThat(underTest.state.value.fieldOfView.angleDown)
                .isEqualTo(EXPECTED_FOV.angleDown)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun mono_returnsPoseAndFov() =
        runTest(testDispatcher) {
            arCoreTestRule.monoRenderViewpoint.pose = EXPECTED_POSE
            arCoreTestRule.monoRenderViewpoint.fieldOfView = EXPECTED_FOV

            advanceUntilIdle()

            val underTest = RenderViewpoint.mono(session)!!
            assertThat(underTest.state.value.pose).isEqualTo(EXPECTED_POSE)
            assertThat(underTest.state.value.localPose).isEqualTo(EXPECTED_POSE)
            assertThat(underTest.state.value.fieldOfView.angleLeft)
                .isEqualTo(EXPECTED_FOV.angleLeft)
            assertThat(underTest.state.value.fieldOfView.angleRight)
                .isEqualTo(EXPECTED_FOV.angleRight)
            assertThat(underTest.state.value.fieldOfView.angleUp).isEqualTo(EXPECTED_FOV.angleUp)
            assertThat(underTest.state.value.fieldOfView.angleDown)
                .isEqualTo(EXPECTED_FOV.angleDown)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun left_returnsPoseAndFovInPerceptionSpace() =
        runTest(testDispatcher) {
            arCoreTestRule.device.pose = EXPECTED_DEVICE_POSE
            arCoreTestRule.leftRenderViewpoint.pose = EXPECTED_OBJECT_POSE
            arCoreTestRule.leftRenderViewpoint.fieldOfView = EXPECTED_FOV

            advanceUntilIdle()

            val underTest = RenderViewpoint.left(session)!!
            val poseInPerceptionSpace: Pose = EXPECTED_DEVICE_POSE.compose(EXPECTED_OBJECT_POSE)
            assertThat(underTest.state.value.pose).isEqualTo(poseInPerceptionSpace)
            assertThat(underTest.state.value.localPose).isEqualTo(EXPECTED_OBJECT_POSE)
            assertThat(underTest.state.value.fieldOfView.angleLeft)
                .isEqualTo(EXPECTED_FOV.angleLeft)
            assertThat(underTest.state.value.fieldOfView.angleRight)
                .isEqualTo(EXPECTED_FOV.angleRight)
            assertThat(underTest.state.value.fieldOfView.angleUp).isEqualTo(EXPECTED_FOV.angleUp)
            assertThat(underTest.state.value.fieldOfView.angleDown)
                .isEqualTo(EXPECTED_FOV.angleDown)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun right_returnsPoseAndFovInPerceptionSpace() =
        runTest(testDispatcher) {
            arCoreTestRule.device.pose = EXPECTED_DEVICE_POSE
            arCoreTestRule.rightRenderViewpoint.pose = EXPECTED_OBJECT_POSE
            arCoreTestRule.rightRenderViewpoint.fieldOfView = EXPECTED_FOV

            advanceUntilIdle()

            val underTest = RenderViewpoint.right(session)!!
            val poseInPerceptionSpace: Pose = EXPECTED_DEVICE_POSE.compose(EXPECTED_OBJECT_POSE)
            assertThat(underTest.state.value.pose).isEqualTo(poseInPerceptionSpace)
            assertThat(underTest.state.value.localPose).isEqualTo(EXPECTED_OBJECT_POSE)
            assertThat(underTest.state.value.fieldOfView.angleLeft)
                .isEqualTo(EXPECTED_FOV.angleLeft)
            assertThat(underTest.state.value.fieldOfView.angleRight)
                .isEqualTo(EXPECTED_FOV.angleRight)
            assertThat(underTest.state.value.fieldOfView.angleUp).isEqualTo(EXPECTED_FOV.angleUp)
            assertThat(underTest.state.value.fieldOfView.angleDown)
                .isEqualTo(EXPECTED_FOV.angleDown)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun mono_returnsPoseAndFovInPerceptionSpace() =
        runTest(testDispatcher) {
            arCoreTestRule.device.pose = EXPECTED_DEVICE_POSE
            arCoreTestRule.monoRenderViewpoint.pose = EXPECTED_OBJECT_POSE
            arCoreTestRule.monoRenderViewpoint.fieldOfView = EXPECTED_FOV

            advanceUntilIdle()

            val underTest = RenderViewpoint.mono(session)!!
            val poseInPerceptionSpace: Pose = EXPECTED_DEVICE_POSE.compose(EXPECTED_OBJECT_POSE)
            assertThat(underTest.state.value.pose).isEqualTo(poseInPerceptionSpace)
            assertThat(underTest.state.value.localPose).isEqualTo(EXPECTED_OBJECT_POSE)
            assertThat(underTest.state.value.fieldOfView.angleLeft)
                .isEqualTo(EXPECTED_FOV.angleLeft)
            assertThat(underTest.state.value.fieldOfView.angleRight)
                .isEqualTo(EXPECTED_FOV.angleRight)
            assertThat(underTest.state.value.fieldOfView.angleUp).isEqualTo(EXPECTED_FOV.angleUp)
            assertThat(underTest.state.value.fieldOfView.angleDown)
                .isEqualTo(EXPECTED_FOV.angleDown)
        }
}
