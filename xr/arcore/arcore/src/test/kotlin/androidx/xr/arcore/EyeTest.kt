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
import androidx.xr.runtime.EyeTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.manifest.EYE_TRACKING_COARSE
import androidx.xr.runtime.manifest.EYE_TRACKING_FINE
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
class EyeTest {
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

        shadowOf(activity.application).grantPermissions(EYE_TRACKING_COARSE, EYE_TRACKING_FINE)

        activityController.create().start().resume()

        session =
            (Session.create(context = activity, coroutineContext = testDispatcher)
                    as SessionCreateSuccess)
                .session
        session.configure(Config(eyeTracking = EyeTrackingMode.FINE_TRACKING))
    }

    @Test
    fun left_eyeTrackingDisabled_throwsIllegalStateException() {
        session.configure(Config(eyeTracking = EyeTrackingMode.DISABLED))

        assertFailsWith<IllegalStateException> { Eye.left(session) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun left_trackingStateMatchesRuntime() =
        runTest(testDispatcher) {
            val underTest = Eye.left(session)
            advanceUntilIdle()

            assertThat(underTest.state.value.trackingState).isEqualTo(TrackingState.TRACKING)

            arCoreTestRule.leftEyeTester.isOpen = false
            advanceUntilIdle()

            assertThat(underTest.state.value.trackingState).isEqualTo(TrackingState.PAUSED)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun left_isOpen_poseMatchesRuntime() =
        runTest(testDispatcher) {
            val expectedPose = Pose(Vector3.Left, Quaternion.Identity)
            arCoreTestRule.leftEyeTester.pose = expectedPose
            advanceUntilIdle()
            val underTest = Eye.left(session)
            advanceUntilIdle()

            assertThat(underTest.state.value.isOpen).isTrue()
            assertThat(underTest.state.value.pose).isEqualTo(expectedPose)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun left_isClosed_poseDoesNotUpdate() =
        runTest(testDispatcher) {
            val expectedPose = Pose(Vector3.Left, Quaternion.Identity)
            arCoreTestRule.leftEyeTester.isOpen = false
            arCoreTestRule.leftEyeTester.pose = expectedPose
            advanceUntilIdle()
            val underTest = Eye.left(session)
            advanceUntilIdle()

            assertThat(underTest.state.value.isOpen).isFalse()
            assertThat(underTest.state.value.pose).isNotEqualTo(expectedPose)
        }

    @Test
    fun right_eyeTrackingDisabled_throwsIllegalStateException() {
        session.configure(Config(eyeTracking = EyeTrackingMode.DISABLED))

        assertFailsWith<IllegalStateException> { Eye.right(session) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun right_trackingStateMatchesRuntime() =
        runTest(testDispatcher) {
            val underTest = Eye.right(session)
            advanceUntilIdle()

            assertThat(underTest.state.value.trackingState).isEqualTo(TrackingState.TRACKING)

            arCoreTestRule.rightEyeTester.isOpen = false
            advanceUntilIdle()

            assertThat(underTest.state.value.trackingState).isEqualTo(TrackingState.PAUSED)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun right_isOpen_poseMatchesRuntime() =
        runTest(testDispatcher) {
            val expectedPose = Pose(Vector3.Right, Quaternion.Identity)
            arCoreTestRule.rightEyeTester.pose = expectedPose
            advanceUntilIdle()
            val underTest = Eye.right(session)
            advanceUntilIdle()

            assertThat(underTest.state.value.isOpen).isTrue()
            assertThat(underTest.state.value.pose).isEqualTo(expectedPose)
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun right_isClosed_poseDoesNotUpdate() =
        runTest(testDispatcher) {
            val expectedPose = Pose(Vector3.Right, Quaternion.Identity)
            arCoreTestRule.rightEyeTester.isOpen = false
            arCoreTestRule.rightEyeTester.pose = expectedPose
            advanceUntilIdle()
            val underTest = Eye.right(session)
            advanceUntilIdle()

            assertThat(underTest.state.value.isOpen).isFalse()
            assertThat(underTest.state.value.pose).isNotEqualTo(expectedPose)
        }
}
