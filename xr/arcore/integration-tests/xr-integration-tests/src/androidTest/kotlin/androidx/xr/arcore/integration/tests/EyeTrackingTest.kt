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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import androidx.xr.arcore.Eye
import androidx.xr.arcore.PerceptionState
import androidx.xr.arcore.TrackingState
import androidx.xr.arcore.perceptionState
import androidx.xr.runtime.Config
import androidx.xr.runtime.EyeTrackingMode
import androidx.xr.runtime.SessionConfigureSuccess
import androidx.xr.runtime.XrDevice
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.createXrSessionRule
import androidx.xr.testutils.filterSupportedPermissions
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Automated integration tests for JXR ARCore [Eye] gaze tracking, eye state poses, and
 * [EyeTrackingMode] configuration.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class EyeTrackingTest {

    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            *filterSupportedPermissions(
                "android.permission.EYE_TRACKING_COARSE",
                "android.permission.EYE_TRACKING_FINE",
                "android.permission.HEAD_TRACKING",
            )
        )

    @get:Rule(order = 1) val sessionRule = createXrSessionRule()

    private fun assumeEyeTrackingSupported(mode: EyeTrackingMode) {
        val xrDevice = XrDevice.getCurrentDevice(sessionRule.session.context)
        assumeTrue(
            "Skipping test: Device does not support $mode",
            xrDevice.isEyeTrackingModeSupported(mode),
        )
    }

    private suspend fun configureEyeTrackingAndAwaitFrame(mode: EyeTrackingMode): PerceptionState {
        val session = sessionRule.session
        val config = Config.Builder().setEyeTracking(mode).build()
        val configResult = session.configure(config)
        check(configResult is SessionConfigureSuccess) {
            "Failed to configure $mode: $configResult"
        }
        val frame =
            checkNotNull(
                withTimeoutOrNull(FRAME_TIMEOUT_MS) {
                    session.state.drop(1).first { it.perceptionState != null }
                }
            ) {
                "Timed out waiting for perception state"
            }
        return checkNotNull(frame.perceptionState)
    }

    private fun assertEyeStateInvariant(state: Eye.State?) {
        val nonNullState = assertNotNull(state, "Expected non-null eye state")
        if (nonNullState.isOpen) {
            assertThat(nonNullState.trackingState).isEqualTo(TrackingState.TRACKING)
        }
    }

    private fun assertValidGazingEyeState(eye: Eye, state: Eye.State) {
        assertThat(state.owner).isEqualTo(eye)
        assertThat(state.pose.translation.x).isFinite()
        assertThat(state.pose.translation.y).isFinite()
        assertThat(state.pose.translation.z).isFinite()
        assertThat(state.pose.translation.length).isLessThan(MAX_HEAD_RELATIVE_EYE_OFFSET_METERS)
        val rotation = state.pose.rotation
        assertThat(rotation.x).isFinite()
        assertThat(rotation.y).isFinite()
        assertThat(rotation.z).isFinite()
        assertThat(rotation.w).isFinite()
    }

    private fun verifyTrackedEyesForMode(mode: EyeTrackingMode) {
        runBlocking {
            val session = sessionRule.session
            assumeEyeTrackingSupported(mode)

            val perceptionState = configureEyeTrackingAndAwaitFrame(mode)
            val configuredMode = session.config.eyeTracking
            val leftEye = Eye.left(session)
            val rightEye = Eye.right(session)
            val currentLeftState = leftEye.state.value
            val currentRightState = rightEye.state.value
            // TODO: b/575891923 - Migrate to synthetic/mocked eye gaze input once Pillar 5
            // perception simulation supports eye tracking so gaze pose assertions run
            // deterministically without wearer eyes.
            val gazingStates =
                withTimeoutOrNull(FRAME_TIMEOUT_MS) {
                    val left =
                        leftEye.state.first {
                            it.trackingState == TrackingState.TRACKING && it.isOpen
                        }
                    val right =
                        rightEye.state.first {
                            it.trackingState == TrackingState.TRACKING && it.isOpen
                        }
                    left to right
                }
            assumeTrue(
                "Skipping eye gaze pose assertions: Eyes did not enter TRACKING with isOpen=true",
                gazingStates != null,
            )
            val (leftGazingState, rightGazingState) = checkNotNull(gazingStates)

            assertThat(configuredMode).isEqualTo(mode)
            assertThat(perceptionState.leftEyeState?.owner).isEqualTo(leftEye)
            assertThat(perceptionState.rightEyeState?.owner).isEqualTo(rightEye)
            assertEyeStateInvariant(currentLeftState)
            assertEyeStateInvariant(currentRightState)
            assertEyeStateInvariant(perceptionState.leftEyeState)
            assertEyeStateInvariant(perceptionState.rightEyeState)
            assertValidGazingEyeState(leftEye, leftGazingState)
            assertValidGazingEyeState(rightEye, rightGazingState)
        }
    }

    @Test
    @XrDeviceTest
    fun eyeTracking_whenCoarseTrackingConfigured_leftAndRightEyesEmitTrackedGazingPose() {
        verifyTrackedEyesForMode(EyeTrackingMode.COARSE_TRACKING)
    }

    @Test
    @XrDeviceTest
    fun eyeTracking_whenFineTrackingConfigured_leftAndRightEyesEmitTrackedGazingPose() {
        verifyTrackedEyesForMode(EyeTrackingMode.FINE_TRACKING)
    }

    @Test
    @XrDeviceTest
    fun eyeTracking_whenTogglingModes_updatesConfigAndThrowsWhenDisabled() {
        runBlocking {
            val session = sessionRule.session
            assumeEyeTrackingSupported(EyeTrackingMode.COARSE_TRACKING)
            assumeEyeTrackingSupported(EyeTrackingMode.FINE_TRACKING)

            configureEyeTrackingAndAwaitFrame(EyeTrackingMode.COARSE_TRACKING)
            val coarseMode = session.config.eyeTracking
            val coarseLeftEye = Eye.left(session)
            val coarseRightEye = Eye.right(session)
            configureEyeTrackingAndAwaitFrame(EyeTrackingMode.FINE_TRACKING)
            val fineMode = session.config.eyeTracking
            val fineLeftEye = Eye.left(session)
            val fineRightEye = Eye.right(session)
            configureEyeTrackingAndAwaitFrame(EyeTrackingMode.COARSE_TRACKING)
            val restoredCoarseMode = session.config.eyeTracking
            val restoredLeftEye = Eye.left(session)
            val restoredRightEye = Eye.right(session)
            val disableResult =
                session.configure(Config.Builder().setEyeTracking(EyeTrackingMode.DISABLED).build())
            val disabledMode = session.config.eyeTracking

            assertThat(coarseMode).isEqualTo(EyeTrackingMode.COARSE_TRACKING)
            assertThat(fineMode).isEqualTo(EyeTrackingMode.FINE_TRACKING)
            assertThat(fineLeftEye).isSameInstanceAs(coarseLeftEye)
            assertThat(fineRightEye).isSameInstanceAs(coarseRightEye)
            assertThat(restoredCoarseMode).isEqualTo(EyeTrackingMode.COARSE_TRACKING)
            assertThat(restoredLeftEye).isSameInstanceAs(coarseLeftEye)
            assertThat(restoredRightEye).isSameInstanceAs(coarseRightEye)
            assertThat(disableResult).isInstanceOf(SessionConfigureSuccess::class.java)
            assertThat(disabledMode).isEqualTo(EyeTrackingMode.DISABLED)
            assertFailsWith<IllegalStateException> { Eye.left(session) }
            assertFailsWith<IllegalStateException> { Eye.right(session) }
        }
    }

    @Test
    @XrDeviceTest
    fun eyeTracking_whenDisabled_throwsIllegalStateException() {
        val session = sessionRule.session
        val config = Config.Builder().setEyeTracking(EyeTrackingMode.DISABLED).build()

        val configResult = session.configure(config)

        assertThat(configResult).isInstanceOf(SessionConfigureSuccess::class.java)
        assertFailsWith<IllegalStateException> { Eye.left(session) }
        assertFailsWith<IllegalStateException> { Eye.right(session) }
    }

    companion object {
        private const val FRAME_TIMEOUT_MS = 5_000L
        private const val MAX_HEAD_RELATIVE_EYE_OFFSET_METERS = 0.2f
    }
}
