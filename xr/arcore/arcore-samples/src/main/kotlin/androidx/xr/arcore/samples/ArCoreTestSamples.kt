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

package androidx.xr.arcore.samples

import androidx.activity.ComponentActivity
import androidx.annotation.Sampled
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.arcore.ArDevice
import androidx.xr.arcore.Plane
import androidx.xr.arcore.PlaneLabel
import androidx.xr.arcore.PlaneType
import androidx.xr.arcore.testing.ArCoreTestRule
import androidx.xr.arcore.testing.TestPlane
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.manifest.SCENE_UNDERSTANDING_COARSE
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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

@OptIn(ExperimentalCoroutinesApi::class)
@Sampled
fun arCoreTestSamples() {
    @RunWith(AndroidJUnit4::class)
    class ArCoreTestSamples {
        // Declare an instance of ArCoreTestRule with @Rule
        @Rule @JvmField val arCoreTestRule = ArCoreTestRule()

        private lateinit var activityController: ActivityController<ComponentActivity>
        private lateinit var activity: ComponentActivity
        private lateinit var testDispatcher: TestDispatcher
        private lateinit var testScope: TestScope
        private lateinit var session: Session

        // Configure the TestScope, Dispatcher, and running Session before each test
        @Before
        fun setUp(): Unit = runBlocking {
            testDispatcher = StandardTestDispatcher()
            testScope = TestScope(testDispatcher)
            activityController = Robolectric.buildActivity(ComponentActivity::class.java)
            activity = activityController.get()

            // Grant the application required permissions
            shadowOf(activity.application).grantPermissions(SCENE_UNDERSTANDING_COARSE)

            // Start the Activity and ensure it's running
            activityController.create().start().resume()

            session =
                (Session.create(context = activity, coroutineContext = testDispatcher)
                        as SessionCreateSuccess)
                    .session
            session.configure(
                Config.Builder()
                    .setPlaneTracking(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)
                    .setDeviceTracking(DeviceTrackingMode.SPATIAL)
                    .build()
            )
        }

        @Test
        fun plane_subscribe_detectsPlane() =
            runTest(testDispatcher) {
                val plane1 = TestPlane(PlaneType.HORIZONTAL_UPWARD_FACING, PlaneLabel.FLOOR)
                arCoreTestRule.addTrackables(plane1)
                advanceUntilIdle()

                var foundPlanes = emptyList<Plane>()
                testScope.launch {
                    Plane.subscribe(session).collect { foundPlanes = it.toList() }
                }
                advanceUntilIdle()

                assertThat(foundPlanes.single().type).isEqualTo(PlaneType.HORIZONTAL_UPWARD_FACING)
            }

        @Test
        fun arDevice_getInstance_pose_updates() =
            runTest(testDispatcher) {
                val expectedPose = Pose(Vector3(1f, 2f, 3f), Quaternion(4f, 5f, 6f, 7f))
                arCoreTestRule.deviceTester.pose = expectedPose
                advanceUntilIdle()

                val underTest = ArDevice.getInstance(session)

                assertThat(underTest.state.value.devicePose).isEqualTo(expectedPose)

                arCoreTestRule.deviceTester.pose = Pose.Identity
                advanceUntilIdle()

                assertThat(underTest.state.value.devicePose).isEqualTo(Pose.Identity)
            }
    }
}
