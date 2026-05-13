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
import androidx.xr.runtime.PreviewSpatialApi
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.manifest.HEAD_TRACKING
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
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
class ArDeviceTest {
    @Rule @JvmField val arCoreTestRule = ArCoreTestRule()

    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope
    private lateinit var session: Session

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.get()

        shadowOf(activity.application).grantPermissions(HEAD_TRACKING)

        activityController.create().start().resume()

        session =
            (Session.create(context = activity, coroutineContext = testDispatcher)
                    as SessionCreateSuccess)
                .session

        arCoreTestRule.deviceTester.pose = Pose()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun pose_SpatialLastKnown_tracksTranslationAndRotation() {
        session.configure(Config(deviceTracking = DeviceTrackingMode.SPATIAL))
        runTest(testDispatcher) {
            val expectedPose = Pose(Vector3(1f, 2f, 3f), Quaternion(4f, 5f, 6f, 7f))
            arCoreTestRule.deviceTester.pose = expectedPose
            advanceUntilIdle()

            val underTest = ArDevice.getInstance(session)
            advanceUntilIdle()

            assertThat(underTest.state.value.devicePose.translation)
                .isEqualTo(expectedPose.translation)
            assertThat(underTest.state.value.devicePose.rotation).isEqualTo(expectedPose.rotation)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, PreviewSpatialApi::class)
    @Test
    fun pose_InertialLastKnown_onlyTracksRotation() {
        session.configure(Config(deviceTracking = DeviceTrackingMode.INERTIAL))
        runTest(testDispatcher) {
            val expectedPose = Pose(Vector3(1f, 2f, 3f), Quaternion(4f, 5f, 6f, 7f))
            arCoreTestRule.deviceTester.pose = expectedPose
            advanceUntilIdle()

            val underTest = ArDevice.getInstance(session)
            advanceUntilIdle()

            assertThat(underTest.state.value.devicePose.translation)
                .isNotEqualTo(expectedPose.translation)
            assertThat(underTest.state.value.devicePose.translation).isEqualTo(Vector3.Zero)
            assertThat(underTest.state.value.devicePose.rotation).isEqualTo(expectedPose.rotation)
        }
    }

    @Test
    fun getInstance_deviceTrackingDisabled_throwsIllegalStateException() {
        session.configure(Config(deviceTracking = DeviceTrackingMode.DISABLED))
        runTest(testDispatcher) {
            assertFailsWith<IllegalStateException> { ArDevice.getInstance(session) }
        }
    }
}
