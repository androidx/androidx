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

package androidx.xr.arcore.integration.tests

import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import androidx.xr.arcore.Hand
import androidx.xr.arcore.HandJointType
import androidx.xr.arcore.HandSide
import androidx.xr.arcore.TrackingState
import androidx.xr.arcore.perceptionState
import androidx.xr.runtime.Config
import androidx.xr.runtime.HandTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionConfigureSuccess
import androidx.xr.runtime.SessionCreateApkRequired
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.SessionCreateTimedOut
import androidx.xr.runtime.SessionCreateUnknownError
import androidx.xr.runtime.SessionCreateUnsupportedDevice
import androidx.xr.runtime.XrDevice
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.filterSupportedPermissions
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.assertFailsWith
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end XR device integration tests for JXR ARCore Hand Tracking ([Hand.left], [Hand.right],
 * [Hand.getPrimaryHandSide], OpenXR 26-joint buffer and skeleton geometry invariants, dynamic
 * runtime reconfiguration, and Activity recreation).
 */
// TODO(b/567839739): Once a mechanism for injecting synthetic hand data is available, migrate
// these tests to use deterministic hand poses to avoid dependency on the physical environment and
// timeout-based skips.
@RunWith(AndroidJUnit4::class)
@LargeTest
class HandTrackingTest {

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(*filterSupportedPermissions("android.permission.HAND_TRACKING"))

    private suspend fun createSessionWithHandTracking(
        scenario: ActivityScenario<ComponentActivity>,
        handTrackingMode: HandTrackingMode = HandTrackingMode.BOTH,
    ): Session {
        lateinit var activity: ComponentActivity
        scenario.onActivity { activity = it }
        val result = Session.create(context = activity)
        assumeTrue(
            "Skipping test: Missing or outdated APK '${(result as? SessionCreateApkRequired)?.requiredApk}'",
            result !is SessionCreateApkRequired,
        )
        assumeTrue(
            "Skipping test: Device does not support required XR features",
            result !is SessionCreateUnsupportedDevice,
        )
        check(result is SessionCreateSuccess) {
            when (result) {
                is SessionCreateUnknownError -> "SessionCreateUnknownError: ${result.errorMessage}"
                is SessionCreateTimedOut -> "SessionCreateTimedOut"
                else -> "Session.create failed with result: $result"
            }
        }
        val session = result.session
        assumeTrue(
            "Skipping test: Device does not support HandTrackingMode.BOTH",
            XrDevice.getCurrentDevice(activity).isHandTrackingModeSupported(HandTrackingMode.BOTH),
        )
        val config = Config.Builder().setHandTracking(handTrackingMode).build()
        val configResult = session.configure(config)
        check(configResult is SessionConfigureSuccess) {
            "Failed to configure Session: $configResult"
        }

        val state =
            withTimeoutOrNull(SESSION_STATE_TIMEOUT_MS) {
                session.state.first { it.perceptionState != null }
            }
        checkNotNull(state) { "Timed out waiting for perception state" }
        return session
    }

    private fun assertNormalizedQuaternion(
        qx: Float,
        qy: Float,
        qz: Float,
        qw: Float,
        epsilon: Float = 1e-3f,
    ) {
        val norm = sqrt(qx * qx + qy * qy + qz * qz + qw * qw)
        assertThat(norm).isWithin(epsilon).of(1.0f)
    }

    private fun assertPoseEquals(actual: Pose, expected: Pose, epsilon: Float = 1e-4f) {
        assertThat(actual.translation.x).isWithin(epsilon).of(expected.translation.x)
        assertThat(actual.translation.y).isWithin(epsilon).of(expected.translation.y)
        assertThat(actual.translation.z).isWithin(epsilon).of(expected.translation.z)
        val dotProduct =
            actual.rotation.x * expected.rotation.x +
                actual.rotation.y * expected.rotation.y +
                actual.rotation.z * expected.rotation.z +
                actual.rotation.w * expected.rotation.w
        assertThat(abs(dotProduct)).isWithin(epsilon).of(1.0f)
    }

    @Test
    @XrDeviceTest
    fun handTracking_leftAndRightHands_synchronizeWithPerceptionStateAndSystemHandedness() {
        runBlocking {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                val session =
                    createSessionWithHandTracking(
                        scenario,
                        handTrackingMode = HandTrackingMode.BOTH,
                    )

                val resolver = session.context.contentResolver
                val expectedHandSide =
                    HandSide.entries[
                            Settings.System.getInt(
                                resolver,
                                "primary_hand",
                                HandSide.UNKNOWN.ordinal,
                            )]
                assertThat(Hand.getPrimaryHandSide(resolver)).isEqualTo(expectedHandSide)

                val leftHand = Hand.left(session)
                val rightHand = Hand.right(session)
                assertThat(leftHand).isNotEqualTo(rightHand)

                val perceptionState = session.state.value.perceptionState
                checkNotNull(perceptionState)
                assertThat(perceptionState.leftHandState?.owner).isEqualTo(leftHand)
                assertThat(perceptionState.rightHandState?.owner).isEqualTo(rightHand)
            }
        }
    }

    @Test
    @XrDeviceTest
    fun handTracking_whenHandIsTracked_validatesJointBufferAndSkeletonGeometry() {
        runBlocking {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                val session =
                    createSessionWithHandTracking(
                        scenario,
                        handTrackingMode = HandTrackingMode.BOTH,
                    )

                val leftHand = Hand.left(session)
                val rightHand = Hand.right(session)

                // TODO(b/567839739): Inject synthetic tracked hand pose once available instead of
                // relying on the environment or skipping on timeout.
                val trackedState =
                    withTimeoutOrNull(HAND_DISCOVERY_TIMEOUT_MS) {
                        merge(leftHand.state, rightHand.state).first {
                            it.trackingState == TrackingState.TRACKING
                        }
                    }
                assumeTrue(
                    "Skipping test: No active tracked hand detected in environment within timeout",
                    trackedState != null,
                )

                val state = checkNotNull(trackedState)
                val floatsPerPose = 7
                val expectedFloatCount = HandJointType.entries.size * floatsPerPose
                assertThat(state.handJointsBuffer.limit()).isAtLeast(expectedFloatCount)

                val buffer = state.handJointsBuffer
                for (jointType in HandJointType.entries) {
                    val jointPose = state.handJoints[jointType]
                    checkNotNull(jointPose) { "Missing pose for joint $jointType" }

                    assertThat(jointPose.translation.x).isFinite()
                    assertThat(jointPose.translation.y).isFinite()
                    assertThat(jointPose.translation.z).isFinite()

                    // Verify raw OpenXR FloatBuffer layout [qx, qy, qz, qw, px, py, pz] at
                    // jointType.ordinal * 7 has a unit-length quaternion before Quaternion(...)
                    // normalizes its input, and matches Hand.State.handJoints.
                    val baseOffset = jointType.ordinal * floatsPerPose
                    val rawQx = buffer.get(baseOffset + 0)
                    val rawQy = buffer.get(baseOffset + 1)
                    val rawQz = buffer.get(baseOffset + 2)
                    val rawQw = buffer.get(baseOffset + 3)
                    assertNormalizedQuaternion(rawQx, rawQy, rawQz, rawQw)

                    val expectedFromBuffer =
                        Pose(
                            Vector3(
                                buffer.get(baseOffset + 4),
                                buffer.get(baseOffset + 5),
                                buffer.get(baseOffset + 6),
                            ),
                            Quaternion(rawQx, rawQy, rawQz, rawQw),
                        )
                    assertPoseEquals(jointPose, expectedFromBuffer)
                }

                // Anatomical invariant: wrist and palm joints must be distinct and within a
                // realistic physical hand distance (0m < distance < 0.3m)
                val wristPosition =
                    checkNotNull(state.handJoints[HandJointType.WRIST]) {
                            "Missing WRIST joint pose"
                        }
                        .translation
                val palmPosition =
                    checkNotNull(state.handJoints[HandJointType.PALM]) { "Missing PALM joint pose" }
                        .translation
                val wristToPalmDistance = (palmPosition - wristPosition).length
                assertThat(wristToPalmDistance).isGreaterThan(0f)
                assertThat(wristToPalmDistance).isLessThan(0.3f)
            }
        }
    }

    @Test
    @XrDeviceTest
    fun handTracking_dynamicModeSwitching_enablesAndDisablesCleanly() {
        runBlocking {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                val session =
                    createSessionWithHandTracking(
                        scenario,
                        handTrackingMode = HandTrackingMode.DISABLED,
                    )

                assertFailsWith<IllegalStateException> { Hand.left(session) }
                assertFailsWith<IllegalStateException> { Hand.right(session) }

                // Dynamically enable hand tracking on the active OpenXR session
                val enableConfig = Config.Builder().setHandTracking(HandTrackingMode.BOTH).build()
                val enableResult = session.configure(enableConfig)
                assertThat(enableResult).isInstanceOf(SessionConfigureSuccess::class.java)
                assertThat(session.config.handTracking).isEqualTo(HandTrackingMode.BOTH)

                val leftHand = Hand.left(session)
                val rightHand = Hand.right(session)

                // Dynamically disable hand tracking
                val disableConfig =
                    Config.Builder().setHandTracking(HandTrackingMode.DISABLED).build()
                val disableResult = session.configure(disableConfig)
                assertThat(disableResult).isInstanceOf(SessionConfigureSuccess::class.java)
                assertThat(session.config.handTracking).isEqualTo(HandTrackingMode.DISABLED)

                // Accessing Hand.left or Hand.right while disabled must throw IllegalStateException
                assertFailsWith<IllegalStateException> { Hand.left(session) }
                assertFailsWith<IllegalStateException> { Hand.right(session) }

                // Re-enabling hand tracking restores Hand.left and Hand.right on the same session
                val reEnableResult = session.configure(enableConfig)
                assertThat(reEnableResult).isInstanceOf(SessionConfigureSuccess::class.java)
                assertThat(Hand.left(session)).isSameInstanceAs(leftHand)
                assertThat(Hand.right(session)).isSameInstanceAs(rightHand)

                val updatedState =
                    checkNotNull(
                        withTimeoutOrNull(SESSION_STATE_TIMEOUT_MS) {
                            session.state.drop(1).first { it.perceptionState != null }
                        }
                    ) {
                        "Timed out waiting for perception state after re-enabling hand tracking"
                    }
                val perceptionState = checkNotNull(updatedState.perceptionState)
                assertThat(perceptionState.leftHandState?.owner).isEqualTo(leftHand)
                assertThat(perceptionState.rightHandState?.owner).isEqualTo(rightHand)
            }
        }
    }

    @Test
    @XrDeviceTest
    fun handTracking_activityRecreation_reinitializesSessionAndHandPipeline() {
        runBlocking {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                val initialSession =
                    createSessionWithHandTracking(
                        scenario,
                        handTrackingMode = HandTrackingMode.BOTH,
                    )

                val initialLeftHand = Hand.left(initialSession)
                val initialRightHand = Hand.right(initialSession)

                scenario.recreate()

                // Upon activity recreation, the initial Activity and its attached Session are
                // destroyed, and a new Session is created and configured for the recreated
                // Activity.
                assertFailsWith<IllegalStateException> {
                    initialSession.configure(initialSession.config)
                }

                val newSession =
                    createSessionWithHandTracking(
                        scenario,
                        handTrackingMode = HandTrackingMode.BOTH,
                    )

                assertThat(newSession).isNotSameInstanceAs(initialSession)

                val newLeftHand = Hand.left(newSession)
                val newRightHand = Hand.right(newSession)
                assertThat(newLeftHand).isNotEqualTo(initialLeftHand)
                assertThat(newRightHand).isNotEqualTo(initialRightHand)
                val perceptionState = checkNotNull(newSession.state.value.perceptionState)
                assertThat(perceptionState.leftHandState?.owner).isEqualTo(newLeftHand)
                assertThat(perceptionState.rightHandState?.owner).isEqualTo(newRightHand)
            }
        }
    }

    private companion object {
        /** Standard 5-second timeout used across JXR tests for session state updates. */
        private const val SESSION_STATE_TIMEOUT_MS = 5_000L

        /**
         * 10-second timeout for initial active hand tracking discovery, matching the upper bound
         * used in JXR integration tests while avoiding excessive skip latency on headless scenes or
         * emulators.
         */
        private const val HAND_DISCOVERY_TIMEOUT_MS = 10_000L
    }
}
