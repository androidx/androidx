/*
 * Copyright 2024 The Android Open Source Project
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
import androidx.xr.runtime.Config
import androidx.xr.runtime.Config.HandTrackingMode
import androidx.xr.runtime.HandJointType
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.TrackingState
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.runtime.testing.FakeLifecycleManager
import androidx.xr.runtime.testing.FakePerceptionManager
import androidx.xr.runtime.testing.FakeRuntimeAnchor
import androidx.xr.runtime.testing.FakeRuntimeFactory
import androidx.xr.runtime.testing.FakeRuntimeHand
import com.google.common.truth.Truth.assertThat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
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
import org.robolectric.shadows.ShadowContentResolver

@RunWith(AndroidJUnit4::class)
class HandTest {

    private val handJointBufferSize: Int = HandJointType.entries.size * 7 * Float.SIZE_BYTES
    private val tolerance = 1e-4f
    private lateinit var xrResourcesManager: XrResourcesManager
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
        xrResourcesManager = XrResourcesManager()
        mockContentResolver = activity.contentResolver

        val shadowApplication = shadowOf(activity.application)
        shadowApplication.grantPermissions(
            "android.permission.SCENE_UNDERSTANDING_COARSE",
            "android.permission.HAND_TRACKING",
        )
        FakeLifecycleManager.TestPermissions.forEach { permission ->
            shadowApplication.grantPermissions(permission)
        }

        FakeRuntimeFactory.hasCreatePermission = true
        FakeRuntimeAnchor.anchorsCreatedCount = 0

        activityController.create()

        session = (Session.create(activity, testDispatcher) as SessionCreateSuccess).session
        session.configure(Config(handTracking = HandTrackingMode.BOTH))
        xrResourcesManager.lifecycleManager = session.runtime.lifecycleManager
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun left_returnsLeftHand() =
        runTest(testDispatcher) {
            val perceptionManager = session.runtime.perceptionManager as FakePerceptionManager
            val leftHand = Hand.left(session)
            check(leftHand != null)
            check(leftHand.state.value.trackingState != (TrackingState.TRACKING))
            check(leftHand.state.value.handJoints.isEmpty())
            val leftRuntimeHand = perceptionManager.leftHand!! as FakeRuntimeHand
            leftRuntimeHand.trackingState = TrackingState.TRACKING
            val expectedHandJoints: Map<HandJointType, Pose> =
                HandJointType.entries.associateWith { joint ->
                    val i = joint.ordinal.toFloat()
                    Pose(
                        Vector3(i + 0.5f, i + 0.6f, i + 0.7f),
                        Quaternion(i + 0.1f, i + 0.2f, i + 0.3f, i + 0.4f),
                    )
                }

            leftRuntimeHand.handJointsBuffer = generateTestBuffer(expectedHandJoints)
            activityController.resume()
            advanceUntilIdle()
            activityController.pause()

            assertThat(leftHand.state.value.trackingState).isEqualTo(TrackingState.TRACKING)
            for (jointType in HandJointType.entries) {
                val leftHandJoints = leftHand.state.value.handJoints
                assertThat(leftHandJoints[jointType]!!.translation)
                    .isEqualTo(expectedHandJoints[jointType]!!.translation)
                assertRotationEquals(
                    leftHandJoints[jointType]!!.rotation,
                    expectedHandJoints[jointType]!!.rotation,
                )
            }
        }

    @Test
    fun left_handTrackingDisabled_throwsIllegalStateException() {
        session.configure(Config(handTracking = HandTrackingMode.DISABLED))

        assertFailsWith<IllegalStateException> { Hand.left(session) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun right_returnsRightHand() =
        runTest(testDispatcher) {
            val perceptionManager = session.runtime.perceptionManager as FakePerceptionManager
            val rightHand = Hand.right(session)
            check(rightHand != null)
            check(rightHand.state.value.trackingState != (TrackingState.TRACKING))
            check(rightHand.state.value.handJoints.isEmpty())
            val rightRuntimeHand = perceptionManager.rightHand!! as FakeRuntimeHand
            rightRuntimeHand.trackingState = TrackingState.TRACKING
            val expectedHandJoints: Map<HandJointType, Pose> =
                HandJointType.entries.associateWith { joint ->
                    val i = joint.ordinal.toFloat()
                    Pose(
                        Vector3(i + 0.5f, i + 0.6f, i + 0.7f),
                        Quaternion(i + 0.1f, i + 0.2f, i + 0.3f, i + 0.4f),
                    )
                }

            rightRuntimeHand.handJointsBuffer = generateTestBuffer(expectedHandJoints)
            activityController.resume()
            advanceUntilIdle()
            activityController.pause()

            assertThat(rightHand.state.value.trackingState).isEqualTo(TrackingState.TRACKING)
            for (jointType in HandJointType.entries) {
                val rightHandJoints = rightHand.state.value.handJoints
                assertThat(rightHandJoints[jointType]!!.translation)
                    .isEqualTo(expectedHandJoints[jointType]!!.translation)
                assertRotationEquals(
                    rightHandJoints[jointType]!!.rotation,
                    expectedHandJoints[jointType]!!.rotation,
                )
            }
        }

    @Test
    fun right_handTrackingDisabled_throwsIllegalStateException() {
        session.configure(Config(handTracking = HandTrackingMode.DISABLED))

        assertFailsWith<IllegalStateException> { Hand.right(session) }
    }

    @Test
    fun update_stateMatchesRuntimeHand() = runBlocking {
        val runtimeHand = FakeRuntimeHand()
        val underTest = Hand(runtimeHand)
        check(underTest.state.value.trackingState != TrackingState.TRACKING)
        check(underTest.state.value.handJoints.isEmpty())
        runtimeHand.trackingState = TrackingState.TRACKING
        val expectedHandJoints: Map<HandJointType, Pose> =
            HandJointType.entries.associateWith { joint ->
                val i = joint.ordinal.toFloat()
                Pose(
                    Vector3(i + 0.5f, i + 0.6f, i + 0.7f),
                    Quaternion(i + 0.1f, i + 0.2f, i + 0.3f, i + 0.4f),
                )
            }
        runtimeHand.handJointsBuffer = generateTestBuffer(expectedHandJoints)

        underTest.update()

        assertThat(underTest.state.value.trackingState).isEqualTo(TrackingState.TRACKING)
        for (jointType in HandJointType.entries) {
            val handJoints = underTest.state.value.handJoints
            assertThat(handJoints[jointType]!!.translation)
                .isEqualTo(expectedHandJoints[jointType]!!.translation)
            assertRotationEquals(
                handJoints[jointType]!!.rotation,
                expectedHandJoints[jointType]!!.rotation,
            )
        }
    }

    @Test
    fun getHandedness_settingNotConfigured_returnsUnknown() {
        ShadowContentResolver.reset()

        assertThat(Hand.getPrimaryHandSide(mockContentResolver)).isEqualTo(Hand.HandSide.UNKNOWN)
    }

    @Test
    fun getHandedness_settingConfigured_returnsCorrectHandedness() {
        ShadowContentResolver.reset()

        Settings.System.putInt(mockContentResolver, Hand.PRIMARY_HAND_SETTING_NAME, 1)
        assertThat(Hand.getPrimaryHandSide(mockContentResolver)).isEqualTo(Hand.HandSide.RIGHT)

        Settings.System.putInt(mockContentResolver, Hand.PRIMARY_HAND_SETTING_NAME, 0)
        assertThat(Hand.getPrimaryHandSide(mockContentResolver)).isEqualTo(Hand.HandSide.LEFT)
    }

    private fun generateTestBuffer(handJoints: Map<HandJointType, Pose>): FloatBuffer {
        val buffer = ByteBuffer.allocate(handJointBufferSize).order(ByteOrder.nativeOrder())

        HandJointType.entries.forEach { handJointType ->
            val handJointPose = handJoints[handJointType]!!
            buffer.putFloat(handJointPose.rotation.x)
            buffer.putFloat(handJointPose.rotation.y)
            buffer.putFloat(handJointPose.rotation.z)
            buffer.putFloat(handJointPose.rotation.w)
            buffer.putFloat(handJointPose.translation.x)
            buffer.putFloat(handJointPose.translation.y)
            buffer.putFloat(handJointPose.translation.z)
        }

        buffer.flip()
        return buffer.asFloatBuffer()
    }

    private fun assertRotationEquals(actual: Quaternion, expected: Quaternion) {
        assertThat(actual.x).isWithin(tolerance).of(expected.x)
        assertThat(actual.y).isWithin(tolerance).of(expected.y)
        assertThat(actual.z).isWithin(tolerance).of(expected.z)
        assertThat(actual.w).isWithin(tolerance).of(expected.w)
    }
}
