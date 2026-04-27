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

import android.content.ContentResolver
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.arcore.runtime.TrackingState
import androidx.xr.arcore.testing.ArCoreTestRule
import androidx.xr.runtime.Config
import androidx.xr.runtime.HandTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.manifest.HAND_TRACKING
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
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.shadows.ShadowContentResolver

@RunWith(AndroidJUnit4::class)
class HandTest {
    @Rule @JvmField val arCoreTestRule = ArCoreTestRule()

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope
    private lateinit var session: Session
    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var mockContentResolver: ContentResolver

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.get()
        mockContentResolver = activity.contentResolver

        shadowOf(activity.application).grantPermissions(HAND_TRACKING)

        activityController.create().start().resume()

        session = (Session.create(activity, testDispatcher) as SessionCreateSuccess).session
        session.configure(Config(handTracking = HandTrackingMode.BOTH))
    }

    @After
    fun cleanUp() {
        arCoreTestRule.leftHand.isVisible = false
        arCoreTestRule.rightHand.isVisible = false
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun left_returnsLeftHand() =
        runTest(testDispatcher) {
            arCoreTestRule.leftHand.isVisible = true
            advanceUntilIdle()

            val leftHand = Hand.left(session)

            assertThat(leftHand.state.value.trackingState.toRuntimeTrackingState())
                .isEqualTo(TrackingState.TRACKING)

            arCoreTestRule.leftHand.isVisible = false
            advanceUntilIdle()

            assertThat(leftHand.state.value.trackingState.toRuntimeTrackingState())
                .isEqualTo(TrackingState.PAUSED)
        }

    @Test
    fun left_handTrackingDisabled_throwsIllegalStateException() {
        session.configure(Config(handTracking = HandTrackingMode.DISABLED))

        assertFailsWith<IllegalStateException> { Hand.left(session) }
    }

    @Test
    fun left_handNotAvailable_throwsIllegalStateException() {
        val perceptionStateExtender =
            session.stateExtenders.filterIsInstance<PerceptionStateExtender>().first()
        perceptionStateExtender.xrResourcesManager.initiateHands(null, null)

        assertFailsWith<IllegalStateException> { Hand.left(session) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun right_returnsRightHand() =
        runTest(testDispatcher) {
            arCoreTestRule.rightHand.isVisible = true
            advanceUntilIdle()

            val rightHand = Hand.right(session)

            assertThat(rightHand.state.value.trackingState.toRuntimeTrackingState())
                .isEqualTo(TrackingState.TRACKING)

            arCoreTestRule.rightHand.isVisible = false
            advanceUntilIdle()

            assertThat(rightHand.state.value.trackingState.toRuntimeTrackingState())
                .isEqualTo(TrackingState.PAUSED)
        }

    @Test
    fun right_handTrackingDisabled_throwsIllegalStateException() {
        session.configure(Config(handTracking = HandTrackingMode.DISABLED))

        assertFailsWith<IllegalStateException> { Hand.right(session) }
    }

    @Test
    fun right_handNotAvailable_throwsIllegalStateException() {
        val perceptionStateExtender =
            session.stateExtenders.filterIsInstance<PerceptionStateExtender>().first()
        perceptionStateExtender.xrResourcesManager.initiateHands(null, null)

        assertFailsWith<IllegalStateException> { Hand.right(session) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun update_stateMatchesRuntimeHand() =
        runTest(testDispatcher) {
            val expectedHandJoints: Map<HandJointType, Pose> =
                HandJointType.entries.associateWith { joint ->
                    val i = joint.ordinal.toFloat()
                    Pose(
                        Vector3(i + 0.5f, i + 0.6f, i + 0.7f),
                        Quaternion(i + 0.1f, i + 0.2f, i + 0.3f, i + 0.4f),
                    )
                }
            arCoreTestRule.leftHand.isVisible = true
            arCoreTestRule.leftHand.handJointMap = expectedHandJoints
            advanceUntilIdle()

            val underTest = Hand.left(session)
            val rotationTolerance = 1e-4f

            for ((jointType, pose) in underTest.state.value.handJoints) {
                val expectedPose = expectedHandJoints[jointType]!!
                assertThat(pose.translation).isEqualTo(expectedPose.translation)
                assertThat(pose.rotation.x).isWithin(rotationTolerance).of(expectedPose.rotation.x)
                assertThat(pose.rotation.y).isWithin(rotationTolerance).of(expectedPose.rotation.y)
                assertThat(pose.rotation.z).isWithin(rotationTolerance).of(expectedPose.rotation.z)
                assertThat(pose.rotation.w).isWithin(rotationTolerance).of(expectedPose.rotation.w)
            }
        }

    @Test
    fun getHandedness_settingNotConfigured_returnsUnknown() {
        ShadowContentResolver.reset()

        assertThat(Hand.getPrimaryHandSide(mockContentResolver)).isEqualTo(HandSide.UNKNOWN)
    }

    @Test
    fun getHandedness_settingConfigured_returnsCorrectHandedness() {
        ShadowContentResolver.reset()

        Settings.System.putInt(mockContentResolver, Hand.PRIMARY_HAND_SETTING_NAME, 1)
        assertThat(Hand.getPrimaryHandSide(mockContentResolver)).isEqualTo(HandSide.RIGHT)

        Settings.System.putInt(mockContentResolver, Hand.PRIMARY_HAND_SETTING_NAME, 0)
        assertThat(Hand.getPrimaryHandSide(mockContentResolver)).isEqualTo(HandSide.LEFT)
    }
}
