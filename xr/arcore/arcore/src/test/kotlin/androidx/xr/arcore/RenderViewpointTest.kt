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

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.arcore.testing.FakeLifecycleManager
import androidx.xr.arcore.testing.FakePerceptionManager
import androidx.xr.arcore.testing.FakePerceptionRuntimeFactory
import androidx.xr.arcore.testing.FakeRuntimeRenderViewpoint
import androidx.xr.runtime.Config
import androidx.xr.runtime.FieldOfView
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

@RunWith(AndroidJUnit4::class)
class RenderViewpointTest {

    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope
    private lateinit var session: Session
    private lateinit var arDevice: ArDevice
    private lateinit var xrResourcesManager: XrResourcesManager

    companion object {
        val EXPECTED_FOV = FieldOfView(1f, 2f, 3f, 4f)
        val EXPECTED_POSE = Pose(Vector3(1f, 2f, 3f), Quaternion(4f, 5f, 6f, 7f))
        val EXPECTED_DEVICE_POSE =
            Pose(
                translation = Vector3(2f, 0f, 0f),
                rotation = Quaternion.fromAxisAngle(Vector3.Up, 90f),
            )
        val EXPECTED_OBJECT_POSE = Pose(translation = Vector3(0f, 0f, -3f), rotation = Quaternion())

        // Rotation: 90 degrees around Y. translation: (-1, 0, 0).
        val EXPECTED_PERCEPTION_SPACE_POSE = EXPECTED_DEVICE_POSE.compose(EXPECTED_OBJECT_POSE)
    }

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.get()
        xrResourcesManager = XrResourcesManager()

        val shadowApplication = shadowOf(activity.application)
        shadowApplication.grantPermissions(Manifest.permission.CAMERA)
        FakeLifecycleManager.TestPermissions.forEach { permission ->
            shadowApplication.grantPermissions(permission)
        }
        FakePerceptionRuntimeFactory.hasCreatePermission = true

        activityController.create()

        session = (Session.create(activity, testDispatcher) as SessionCreateSuccess).session
        session.configure(Config(headTracking = Config.HeadTrackingMode.LAST_KNOWN))
        xrResourcesManager.lifecycleManager = session.perceptionRuntime.lifecycleManager
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun left_returnsPoseAndFov() =
        runTest(testDispatcher) {
            val perceptionManager =
                session.perceptionRuntime.perceptionManager as FakePerceptionManager
            val runtimeViewpoint = perceptionManager.leftRenderViewpoint
            val underTest = RenderViewpoint.left(session)!!
            check(underTest.state.value.localPose == Pose())
            check(runtimeViewpoint != null)
            runtimeViewpoint.pose = EXPECTED_POSE
            runtimeViewpoint.fieldOfView = EXPECTED_FOV

            activityController.resume()
            advanceUntilIdle()
            activityController.pause()

            assertThat(underTest.state.value.pose).isEqualTo(EXPECTED_POSE)
            assertThat(underTest.state.value.localPose).isEqualTo(EXPECTED_POSE)
            assertThat(underTest.state.value.fieldOfView).isEqualTo(EXPECTED_FOV)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun right_returnsPoseAndFov() =
        runTest(testDispatcher) {
            val perceptionManager =
                session.perceptionRuntime.perceptionManager as FakePerceptionManager
            val runtimeViewpoint = perceptionManager.rightRenderViewpoint
            val underTest = RenderViewpoint.right(session)!!
            check(underTest.state.value.localPose == Pose())
            check(runtimeViewpoint != null)
            runtimeViewpoint.pose = EXPECTED_POSE
            runtimeViewpoint.fieldOfView = EXPECTED_FOV

            activityController.resume()
            advanceUntilIdle()
            activityController.pause()

            assertThat(underTest.state.value.pose).isEqualTo(EXPECTED_POSE)
            assertThat(underTest.state.value.localPose).isEqualTo(EXPECTED_POSE)
            assertThat(underTest.state.value.fieldOfView).isEqualTo(EXPECTED_FOV)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun mono_returnsPoseAndFov() =
        runTest(testDispatcher) {
            val perceptionManager =
                session.perceptionRuntime.perceptionManager as FakePerceptionManager
            val runtimeViewpoint = perceptionManager.monoRenderViewpoint
            val underTest = RenderViewpoint.mono(session)!!
            check(underTest.state.value.localPose == Pose())
            check(runtimeViewpoint != null)
            runtimeViewpoint.pose = EXPECTED_POSE
            runtimeViewpoint.fieldOfView = EXPECTED_FOV

            activityController.resume()
            advanceUntilIdle()
            activityController.pause()

            assertThat(underTest.state.value.pose).isEqualTo(EXPECTED_POSE)
            assertThat(underTest.state.value.localPose).isEqualTo(EXPECTED_POSE)
            assertThat(underTest.state.value.fieldOfView).isEqualTo(EXPECTED_FOV)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun left_returnsPoseAndFovInPerceptionSpace() =
        runTest(testDispatcher) {
            val perceptionManager =
                session.perceptionRuntime.perceptionManager as FakePerceptionManager
            val runtimeViewpoint = perceptionManager.leftRenderViewpoint
            val underTest = RenderViewpoint.left(session)!!
            check(underTest.state.value.localPose == Pose())
            check(runtimeViewpoint != null)
            perceptionManager.arDevice.devicePose = EXPECTED_DEVICE_POSE
            runtimeViewpoint.pose = EXPECTED_OBJECT_POSE
            runtimeViewpoint.fieldOfView = EXPECTED_FOV

            activityController.resume()
            advanceUntilIdle()
            activityController.pause()

            assertThat(underTest.state.value.pose).isEqualTo(EXPECTED_PERCEPTION_SPACE_POSE)
            assertThat(underTest.state.value.localPose).isEqualTo(EXPECTED_OBJECT_POSE)
            assertThat(underTest.state.value.fieldOfView).isEqualTo(EXPECTED_FOV)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun right_returnsPoseAndFovInPerceptionSpace() =
        runTest(testDispatcher) {
            val perceptionManager =
                session.perceptionRuntime.perceptionManager as FakePerceptionManager
            val runtimeViewpoint = perceptionManager.rightRenderViewpoint
            val underTest = RenderViewpoint.right(session)!!
            check(underTest.state.value.localPose == Pose())
            check(runtimeViewpoint != null)
            perceptionManager.arDevice.devicePose = EXPECTED_DEVICE_POSE
            runtimeViewpoint.pose = EXPECTED_OBJECT_POSE
            runtimeViewpoint.fieldOfView = EXPECTED_FOV

            activityController.resume()
            advanceUntilIdle()
            activityController.pause()

            assertThat(underTest.state.value.pose).isEqualTo(EXPECTED_PERCEPTION_SPACE_POSE)
            assertThat(underTest.state.value.localPose).isEqualTo(EXPECTED_OBJECT_POSE)
            assertThat(underTest.state.value.fieldOfView).isEqualTo(EXPECTED_FOV)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun mono_returnsPoseAndFovInPerceptionSpace() =
        runTest(testDispatcher) {
            val perceptionManager =
                session.perceptionRuntime.perceptionManager as FakePerceptionManager
            val runtimeViewpoint = perceptionManager.monoRenderViewpoint
            val underTest = RenderViewpoint.mono(session)!!
            check(underTest.state.value.localPose == Pose())
            check(runtimeViewpoint != null)
            perceptionManager.arDevice.devicePose = EXPECTED_DEVICE_POSE
            runtimeViewpoint.pose = EXPECTED_OBJECT_POSE
            runtimeViewpoint.fieldOfView = EXPECTED_FOV

            activityController.resume()
            advanceUntilIdle()
            activityController.pause()

            assertThat(underTest.state.value.pose).isEqualTo(EXPECTED_PERCEPTION_SPACE_POSE)
            assertThat(underTest.state.value.localPose).isEqualTo(EXPECTED_OBJECT_POSE)
            assertThat(underTest.state.value.fieldOfView).isEqualTo(EXPECTED_FOV)
        }

    @Test
    fun update_stateMatchesRuntimeRenderViewpoint() = runBlocking {
        val runtimeRenderViewpoint = FakeRuntimeRenderViewpoint()
        val perceptionManager = session.perceptionRuntime.perceptionManager as FakePerceptionManager
        val runtimeArDevice = perceptionManager.arDevice
        val underTest = RenderViewpoint(runtimeRenderViewpoint, runtimeArDevice)
        check(underTest.state.value.pose == Pose())
        check(underTest.state.value.fieldOfView == FieldOfView(0f, 0f, 0f, 0f))
        runtimeRenderViewpoint.pose = EXPECTED_POSE
        runtimeRenderViewpoint.fieldOfView = EXPECTED_FOV

        underTest.update()

        assertThat(underTest.state.value.pose).isEqualTo(EXPECTED_POSE)
        assertThat(underTest.state.value.localPose).isEqualTo(EXPECTED_POSE)
        assertThat(underTest.state.value.fieldOfView).isEqualTo(EXPECTED_FOV)
    }

    @Test
    fun update_stateMatchesRuntimeRenderViewpointInPerceptionSpace() = runBlocking {
        val runtimeRenderViewpoint = FakeRuntimeRenderViewpoint()
        val perceptionManager = session.perceptionRuntime.perceptionManager as FakePerceptionManager
        val runtimeArDevice = perceptionManager.arDevice
        val underTest = RenderViewpoint(runtimeRenderViewpoint, runtimeArDevice)
        check(underTest.state.value.pose == Pose())
        check(underTest.state.value.fieldOfView == FieldOfView(0f, 0f, 0f, 0f))
        perceptionManager.arDevice.devicePose = EXPECTED_DEVICE_POSE
        runtimeRenderViewpoint.pose = EXPECTED_OBJECT_POSE
        runtimeRenderViewpoint.fieldOfView = EXPECTED_FOV

        underTest.update()

        assertThat(underTest.state.value.pose).isEqualTo(EXPECTED_PERCEPTION_SPACE_POSE)
        assertThat(underTest.state.value.localPose).isEqualTo(EXPECTED_OBJECT_POSE)
        assertThat(underTest.state.value.fieldOfView).isEqualTo(EXPECTED_FOV)
    }
}
