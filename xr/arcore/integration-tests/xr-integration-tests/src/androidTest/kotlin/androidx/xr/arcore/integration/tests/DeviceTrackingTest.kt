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

import androidx.activity.ComponentActivity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import androidx.xr.arcore.Anchor
import androidx.xr.arcore.AnchorCreateSuccess
import androidx.xr.arcore.ArDevice
import androidx.xr.arcore.TrackingState
import androidx.xr.arcore.perceptionState
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionConfigureSuccess
import androidx.xr.runtime.SessionCreateResult
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.testutils.XrDeviceConfig
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.XrSessionRule
import androidx.xr.testutils.createXrSessionRule
import androidx.xr.testutils.filterSupportedPermissions
import androidx.xr.testutils.waitUntil
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Automated integration tests for JXR ARCore [ArDevice] 6DoF head/device pose tracking,
 * [DeviceTrackingMode] configuration, forward head-pose anchor placement, dynamic reconfiguration,
 * and Activity recreation.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class DeviceTrackingTest {

    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(*filterSupportedPermissions("android.permission.HEAD_TRACKING"))

    @get:Rule(order = 1)
    val sessionRule: XrSessionRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity> =
        createXrSessionRule()

    private fun configureDeviceTracking(
        targetSession: Session,
        deviceTrackingMode: DeviceTrackingMode,
    ) {
        val config = Config.Builder().setDeviceTracking(deviceTrackingMode).build()
        // All @XrDeviceTest targets support SPATIAL; let UnsupportedOperationException fail
        // loudly instead of skipping so native XR_ERROR_FEATURE_UNSUPPORTED regressions surface.
        val configResult = targetSession.configure(config)
        check(configResult is SessionConfigureSuccess) {
            "Failed to configure Session with $deviceTrackingMode: $configResult"
        }
    }

    private fun configureSpatialAndGetArDevice(targetSession: Session): ArDevice {
        configureDeviceTracking(targetSession, DeviceTrackingMode.SPATIAL)
        return ArDevice.getInstance(targetSession)
    }

    private fun configureDisabledAndCaptureError(targetSession: Session): IllegalStateException? {
        configureDeviceTracking(targetSession, DeviceTrackingMode.DISABLED)
        return try {
            ArDevice.getInstance(targetSession)
            null
        } catch (e: IllegalStateException) {
            e
        }
    }

    private fun isTrackedAndSynced(targetSession: Session, arDevice: ArDevice): Boolean {
        return arDevice.state.value.trackingState == TrackingState.TRACKING &&
            targetSession.state.value.perceptionState?.arDeviceState?.trackingState ==
                TrackingState.TRACKING
    }

    private fun hasPoseDelta(first: Pose, second: Pose): Boolean {
        val translationDelta = Pose.distance(first, second)
        val rotationDotDelta = 1.0f - abs(first.rotation dot second.rotation)
        return translationDelta >= MIN_POSE_TRANSLATION_DELTA ||
            rotationDotDelta >= MIN_POSE_ROTATION_DOT_DELTA
    }

    private fun assertFinitePose(pose: Pose) {
        assertThat(pose.translation.x).isFinite()
        assertThat(pose.translation.y).isFinite()
        assertThat(pose.translation.z).isFinite()
        assertThat(pose.rotation.x).isFinite()
        assertThat(pose.rotation.y).isFinite()
        assertThat(pose.rotation.z).isFinite()
        assertThat(pose.rotation.w).isFinite()
    }

    private fun assertTrackedDeviceAndPerceptionState(targetSession: Session, arDevice: ArDevice) {
        waitUntil("ArDevice and PerceptionState to reach TRACKING", PERCEPTION_TIMEOUT) {
            isTrackedAndSynced(targetSession, arDevice)
        }
        val perceptionDeviceState =
            assertNotNull(
                targetSession.state.value.perceptionState?.arDeviceState,
                "Expected non-null PerceptionState.arDeviceState while TRACKING",
            )
        assertThat(perceptionDeviceState.owner).isSameInstanceAs(arDevice)
        assertFinitePose(arDevice.state.value.devicePose)
        assertFinitePose(perceptionDeviceState.devicePose)
    }

    private fun assertPoseEquals(
        actual: Pose,
        expected: Pose,
        translationEpsilon: Float = TRANSLATION_EPSILON,
        rotationDotEpsilon: Float = ROTATION_DOT_EPSILON,
    ) {
        assertThat(actual.translation.x).isWithin(translationEpsilon).of(expected.translation.x)
        assertThat(actual.translation.y).isWithin(translationEpsilon).of(expected.translation.y)
        assertThat(actual.translation.z).isWithin(translationEpsilon).of(expected.translation.z)
        val dotProduct = actual.rotation dot expected.rotation
        assertThat(abs(dotProduct)).isWithin(rotationDotEpsilon).of(1.0f)
    }

    @Test
    @XrDeviceTest
    fun deviceTracking_whenSpatialEnabled_reachesTrackingAndSyncsPerceptionState() {
        val session = sessionRule.session

        val arDevice = configureSpatialAndGetArDevice(session)

        assertTrackedDeviceAndPerceptionState(session, arDevice)
    }

    // TODO(b/575892124): Drive deterministic head poses with FakeHeadTrackingRule and tighten
    // delta thresholds once available.
    @Test
    @XrDeviceTest(include = [XrDeviceConfig.PHYSICAL_DEVICE])
    fun deviceTracking_whenSpatialEnabledOnPhysicalDevice_deliversDistinctTrackedPoses() {
        val session = sessionRule.session

        val arDevice = configureSpatialAndGetArDevice(session)

        waitUntil("initial ArDevice TRACKING pose", PERCEPTION_TIMEOUT) {
            arDevice.state.value.trackingState == TrackingState.TRACKING
        }
        val initialPose = arDevice.state.value.devicePose
        waitUntil("updated ArDevice TRACKING pose exceeding delta threshold", PERCEPTION_TIMEOUT) {
            val currentState = arDevice.state.value
            currentState.trackingState == TrackingState.TRACKING &&
                hasPoseDelta(initialPose, currentState.devicePose)
        }
        assertFinitePose(initialPose)
        assertFinitePose(arDevice.state.value.devicePose)
    }

    @Test
    @XrDeviceTest
    fun arDevice_whenDeviceTrackingDisabled_throwsIllegalStateException() {
        val session = sessionRule.session
        configureDeviceTracking(session, DeviceTrackingMode.DISABLED)

        val error = assertFailsWith<IllegalStateException> { ArDevice.getInstance(session) }
        assertThat(error).hasMessageThat().contains("DISABLED")
    }

    // TODO(b/584314950): Verify world-locked anchor pose remains stationary as device pose moves.
    @Test
    @XrDeviceTest
    fun createAnchor_aheadOfDevicePose_placesAnchorAtComposedForwardPose() {
        val session = sessionRule.session
        val arDevice = configureSpatialAndGetArDevice(session)
        waitUntil("initial ArDevice TRACKING pose", PERCEPTION_TIMEOUT) {
            arDevice.state.value.trackingState == TrackingState.TRACKING
        }
        val targetPose =
            arDevice.state.value.devicePose.compose(
                Pose(Vector3(0f, 0f, -1.0f), Quaternion.Identity)
            )

        val anchorResult = Anchor.create(session, targetPose)

        val anchor = assertIs<AnchorCreateSuccess>(anchorResult).anchor
        try {
            waitUntil("anchor to enter TRACKING state", PERCEPTION_TIMEOUT) {
                anchor.state.value.trackingState == TrackingState.TRACKING
            }
            val trackedPose = anchor.state.value.pose
            assertFinitePose(trackedPose)
            assertPoseEquals(trackedPose, targetPose)
        } finally {
            anchor.detach()
        }
    }

    // TODO(b/584773873): Assert ArDevice leaves TRACKING while DISABLED once OpenXrDevice resets
    // trackingState on removeUpdatable.
    @Test
    @XrDeviceTest
    fun deviceTracking_dynamicModeSwitching_togglesArDeviceAvailability() {
        val session = sessionRule.session

        val initialDisabledError = configureDisabledAndCaptureError(session)
        configureSpatialAndGetArDevice(session)
        val reDisabledError = configureDisabledAndCaptureError(session)
        val reEnabledArDevice = configureSpatialAndGetArDevice(session)

        assertThat(assertNotNull(initialDisabledError)).hasMessageThat().contains("DISABLED")
        assertThat(assertNotNull(reDisabledError)).hasMessageThat().contains("DISABLED")
        assertTrackedDeviceAndPerceptionState(session, reEnabledArDevice)
    }

    @Test
    @XrDeviceTest(include = [XrDeviceConfig.PHYSICAL_DEVICE])
    fun deviceTracking_reEnablingSpatialOnPhysicalDevice_resumesPoseUpdates() {
        val session = sessionRule.session
        val arDevice = configureSpatialAndGetArDevice(session)
        waitUntil("initial ArDevice TRACKING pose", PERCEPTION_TIMEOUT) {
            arDevice.state.value.trackingState == TrackingState.TRACKING
        }
        configureDeviceTracking(session, DeviceTrackingMode.DISABLED)
        val poseWhileDisabled = arDevice.state.value.devicePose

        configureDeviceTracking(session, DeviceTrackingMode.SPATIAL)

        waitUntil("updated ArDevice TRACKING pose after re-enabling SPATIAL", PERCEPTION_TIMEOUT) {
            val currentState = arDevice.state.value
            currentState.trackingState == TrackingState.TRACKING &&
                hasPoseDelta(poseWhileDisabled, currentState.devicePose)
        }
        assertFinitePose(arDevice.state.value.devicePose)
    }

    @Test
    @XrDeviceTest
    fun deviceTracking_activityRecreation_reinitializesSessionAndArDevice() {
        val initialSession = sessionRule.session
        val initialArDevice = configureSpatialAndGetArDevice(initialSession)
        waitUntil("initial ArDevice TRACKING pose", PERCEPTION_TIMEOUT) {
            isTrackedAndSynced(initialSession, initialArDevice)
        }

        sessionRule.activityRule.scenario.recreate()
        var recreatedResult: SessionCreateResult? = null
        sessionRule.activityRule.scenario.onActivity { activity ->
            // Session.create uses MainExecutorDispatcher.immediate, which executes inline on
            // the main looper without re-dispatching or deadlocking runBlocking.
            recreatedResult = runBlocking { Session.create(context = activity) }
        }
        val recreatedSession = assertIs<SessionCreateSuccess>(recreatedResult).session
        val recreatedArDevice = configureSpatialAndGetArDevice(recreatedSession)

        val destroyedError =
            assertFailsWith<IllegalStateException> {
                initialSession.configure(
                    Config.Builder().setDeviceTracking(DeviceTrackingMode.SPATIAL).build()
                )
            }
        assertThat(destroyedError).hasMessageThat().contains("destroyed")
        assertThat(recreatedSession).isNotSameInstanceAs(initialSession)
        assertThat(recreatedArDevice).isNotSameInstanceAs(initialArDevice)
        assertTrackedDeviceAndPerceptionState(recreatedSession, recreatedArDevice)
    }

    private companion object {
        private val PERCEPTION_TIMEOUT = 5.seconds
        private const val MIN_POSE_TRANSLATION_DELTA = 1e-4f
        private const val MIN_POSE_ROTATION_DOT_DELTA = 1e-6f
        private const val TRANSLATION_EPSILON = 1e-2f
        private const val ROTATION_DOT_EPSILON = 1e-4f
    }
}
