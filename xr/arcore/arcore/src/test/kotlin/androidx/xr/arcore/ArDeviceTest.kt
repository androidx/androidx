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
import androidx.xr.arcore.testing.FakeLifecycleManager
import androidx.xr.arcore.testing.FakePerceptionManager
import androidx.xr.arcore.testing.FakePerceptionRuntimeFactory
import androidx.xr.arcore.testing.FakeRuntimeArDevice
import androidx.xr.runtime.Config
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionConfigureSuccess
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
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
class ArDeviceTest {

    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope
    private lateinit var session: Session
    private lateinit var xrResourcesManager: XrResourcesManager

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.get()
        xrResourcesManager = XrResourcesManager()

        val shadowApplication = shadowOf(activity.application)
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
    fun getInstance_returnsHeadPose() =
        runTest(testDispatcher) {
            val expectedDevicePose = Pose(Vector3(1f, 2f, 3f), Quaternion(4f, 5f, 6f, 7f))
            val underTest = ArDevice.getInstance(session)
            check(underTest.state.value.devicePose == Pose())
            val perceptionManager = getFakePerceptionManager()
            val runtimeArDevice = perceptionManager.arDevice

            runtimeArDevice.devicePose = expectedDevicePose
            activityController.resume()
            advanceUntilIdle()
            activityController.pause()

            assertThat(underTest.state.value.devicePose).isEqualTo(expectedDevicePose)
        }

    @Test
    fun getInstance_headTrackingDisabled_throwsIllegalStateException() {
        val configureResult =
            session.configure(Config(headTracking = Config.HeadTrackingMode.DISABLED))
        check(configureResult is SessionConfigureSuccess)

        assertFailsWith<IllegalStateException> { ArDevice.getInstance(session) }
    }

    @Test
    fun update_stateMatchesRuntimeArDevice() = runBlocking {
        val expectedDevicePose = Pose(Vector3(1f, 2f, 3f), Quaternion(4f, 5f, 6f, 7f))
        val runtimeArDevice = FakeRuntimeArDevice()
        val underTest = ArDevice(runtimeArDevice)
        check(underTest.state.value.devicePose == Pose())
        runtimeArDevice.devicePose = expectedDevicePose

        underTest.update()

        assertThat(underTest.state.value.devicePose).isEqualTo(expectedDevicePose)
    }

    private fun getFakePerceptionManager(): FakePerceptionManager {
        return session.perceptionRuntime.perceptionManager as FakePerceptionManager
    }
}
