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
import androidx.xr.arcore.testing.TestPlane
import androidx.xr.runtime.Config
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.manifest.SCENE_UNDERSTANDING_COARSE
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

@RunWith(AndroidJUnit4::class)
@Suppress("DEPRECATION")
class InteractionTest {
    companion object {
        @ClassRule @JvmField val arCoreTestRule = ArCoreTestRule()
    }

    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var session: Session

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.get()

        shadowOf(activity.application).grantPermissions(SCENE_UNDERSTANDING_COARSE)

        activityController.create().start().resume()

        session = (Session.create(activity, testDispatcher) as SessionCreateSuccess).session
        session.configure(Config(planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun hitTest_successWithOneHitResult() =
        runTest(testDispatcher) {
            // 1. Place a plane 1 unit in front of the device pose
            val devicePose = Pose()
            val expectedHitPose = Pose(Vector3.Forward, Quaternion.Identity)
            val testPlane = TestPlane(PlaneType.VERTICAL, PlaneLabel.WALL)
            arCoreTestRule.deviceTester.pose = devicePose
            arCoreTestRule.addTrackables(testPlane)
            testPlane.centerPose = expectedHitPose
            advanceUntilIdle()

            // 2. Detect the Plane
            var foundPlanes = emptyList<Plane>()
            backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
                Plane.subscribe(session).collect { foundPlanes = it.toList() }
            }
            advanceUntilIdle()

            // 3. Perform a hitTest to find Planes along ray from device in forward direction
            val plane = foundPlanes.first()
            val ray = Ray(origin = devicePose.translation, direction = devicePose.forward)
            val hitResults = hitTest(session, ray)
            val expectedDistance =
                (plane.state.value.centerPose.translation - devicePose.translation).length

            assertThat(hitResults.size).isEqualTo(1)

            val underTest = hitResults.single()
            assertThat(underTest.distance).isEqualTo(expectedDistance)
            assertThat(underTest.hitPose).isEqualTo(expectedHitPose)
            assertThat(underTest.trackable).isEqualTo(plane)
        }

    @Test
    fun hitTest_planeTrackingDisabled_throwsIllegalStateException() {
        session.configure(Config(planeTracking = PlaneTrackingMode.DISABLED))
        runTest(testDispatcher) {
            assertFailsWith<IllegalStateException> { hitTest(session, Ray()) }
        }
    }
}
