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
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import androidx.xr.arcore.AnchorCreateSuccess
import androidx.xr.arcore.Plane
import androidx.xr.arcore.PlaneLabel
import androidx.xr.arcore.PlaneType
import androidx.xr.arcore.TrackingState
import androidx.xr.arcore.perceptionState
import androidx.xr.runtime.Config
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionConfigureSuccess
import androidx.xr.runtime.SessionCreateApkRequired
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.SessionCreateTimedOut
import androidx.xr.runtime.SessionCreateUnknownError
import androidx.xr.runtime.SessionCreateUnsupportedDevice
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.filterSupportedPermissions
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.assertFailsWith
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assume.assumeTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end XR device integration tests for JXR ARCore Plane Tracking ([Plane] detection, OpenXR
 * geometric invariants, dynamic runtime reconfiguration, plane-attached anchor world pose and
 * detachment, and Activity recreation).
 */
// TODO(b/567839030): Once a mechanism for injecting synthetic plane data is available, migrate
// these tests to use deterministic plane sets to avoid dependency on the physical environment and
// timeout-based skips.
@RunWith(AndroidJUnit4::class)
@LargeTest
class PlaneTrackingTest {

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            *filterSupportedPermissions("android.permission.SCENE_UNDERSTANDING_COARSE")
        )

    private suspend fun createSessionWithPlaneTracking(
        scenario: ActivityScenario<ComponentActivity>,
        planeTrackingMode: PlaneTrackingMode = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
    ): Session {
        var activity: ComponentActivity? = null
        scenario.onActivity { activity = it }
        val result = Session.create(context = activity!!)
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
        val config = Config.Builder().setPlaneTracking(planeTrackingMode).build()
        val configResult = session.configure(config)
        check(configResult is SessionConfigureSuccess) { "Failed to configure Session" }

        val state =
            withTimeoutOrNull(SESSION_STATE_TIMEOUT_MS) {
                session.state.first { it.perceptionState != null }
            }
        checkNotNull(state) { "Timed out waiting for perception state" }
        return session
    }

    private fun assertValidQuaternion(q: Quaternion, epsilon: Float = 1e-3f) {
        val norm = sqrt(q.x * q.x + q.y * q.y + q.z * q.z + q.w * q.w)
        assertThat(norm).isWithin(epsilon).of(1.0f)
    }

    private fun assertPoseEquals(actual: Pose, expected: Pose, epsilon: Float = 1e-2f) {
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
    fun planeTracking_validatesOpenXrPlaneGeometryAndPerceptionState() {
        runBlocking {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                val session =
                    createSessionWithPlaneTracking(
                        scenario,
                        planeTrackingMode = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
                    )

                val planesFlow = Plane.subscribe(session)
                val planes =
                    withTimeoutOrNull(PLANE_DISCOVERY_TIMEOUT_MS) {
                        planesFlow.first { list ->
                            list.any { it.state.value.trackingState == TrackingState.TRACKING }
                        }
                    }
                assumeTrue(
                    "Skipping test: No tracking planes detected in environment within timeout",
                    planes != null,
                )

                // Verify consistency with Session perceptionState trackables
                val perceptionPlaneStates =
                    session.state.value.perceptionState
                        ?.trackableStates
                        ?.filterIsInstance<Plane.State>()
                assertThat(perceptionPlaneStates).isNotEmpty()
                assertThat(perceptionPlaneStates!!.map { it.owner })
                    .containsExactlyElementsIn(planes)

                for (plane in planes!!) {
                    val state = plane.state.value
                    assertThat(state.owner).isEqualTo(plane)
                    assertThat(plane.type).isNotNull()
                    assertThat(plane.type)
                        .isAnyOf(
                            PlaneType.HORIZONTAL_UPWARD_FACING,
                            PlaneType.HORIZONTAL_DOWNWARD_FACING,
                            PlaneType.VERTICAL,
                        )
                    assertThat(state.label).isNotNull()
                    assertThat(state.label)
                        .isAnyOf(
                            PlaneLabel.UNKNOWN,
                            PlaneLabel.WALL,
                            PlaneLabel.FLOOR,
                            PlaneLabel.CEILING,
                            PlaneLabel.TABLE,
                        )

                    // Real OpenXR planes must have finite center poses and normalized rotations
                    assertThat(state.centerPose.translation.x).isFinite()
                    assertThat(state.centerPose.translation.y).isFinite()
                    assertThat(state.centerPose.translation.z).isFinite()
                    assertValidQuaternion(state.centerPose.rotation)

                    // Real tracked planes must report positive bounding extents and valid polygon
                    // vertices (at least 3 vertices to form a 2D polygon)
                    if (state.trackingState == TrackingState.TRACKING) {
                        assertThat(state.extents.width).isGreaterThan(0f)
                        assertThat(state.extents.height).isGreaterThan(0f)
                        assertThat(state.vertices.size).isAtLeast(3)
                    }
                }
            }
        }
    }

    @Ignore(
        "b/572570652 - Previously tracked planes transition to PAUSED instead of STOPPED when PlaneTrackingMode.DISABLED is configured."
    )
    @Test
    @XrDeviceTest
    fun planeTracking_dynamicModeSwitching_stopsTrackedPlanesWhenDisabled() {
        runBlocking {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                val session =
                    createSessionWithPlaneTracking(
                        scenario,
                        planeTrackingMode = PlaneTrackingMode.DISABLED,
                    )

                assertFailsWith<IllegalStateException> { Plane.subscribe(session) }

                // Dynamically enable plane tracking on the active OpenXR session
                // TODO(b/567839030): Once a mechanism for injecting synthetic plane data is
                // available, migrate these tests to use deterministic plane sets to avoid
                // dependency on the physical environment and timeout-based skips.
                val enableConfig =
                    Config.Builder()
                        .setPlaneTracking(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)
                        .build()
                val enableResult = session.configure(enableConfig)
                assertThat(enableResult).isInstanceOf(SessionConfigureSuccess::class.java)
                assertThat(session.config.planeTracking)
                    .isEqualTo(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)

                val planesFlow = Plane.subscribe(session)
                val trackedPlanes =
                    withTimeoutOrNull(PLANE_DISCOVERY_TIMEOUT_MS) {
                        planesFlow.first { list ->
                            list.any { it.state.value.trackingState == TrackingState.TRACKING }
                        }
                    }
                assumeTrue(
                    "Skipping test: No tracking planes detected in environment within timeout",
                    trackedPlanes != null,
                )

                // Dynamically disable plane tracking
                val disableConfig =
                    Config.Builder().setPlaneTracking(PlaneTrackingMode.DISABLED).build()
                val disableResult = session.configure(disableConfig)
                assertThat(disableResult).isInstanceOf(SessionConfigureSuccess::class.java)
                assertThat(session.config.planeTracking).isEqualTo(PlaneTrackingMode.DISABLED)

                // Wait for perceptionState to reflect STOPPED planes after configure(DISABLED)
                val stateAfterDisable =
                    withTimeoutOrNull(SESSION_STATE_TIMEOUT_MS) {
                        session.state.first { state ->
                            val planeStates =
                                state.perceptionState
                                    ?.trackableStates
                                    ?.filterIsInstance<Plane.State>()
                            !planeStates.isNullOrEmpty() &&
                                planeStates.all { it.trackingState == TrackingState.STOPPED }
                        }
                    }
                checkNotNull(stateAfterDisable) {
                    "Timed out waiting for previously tracked planes to enter STOPPED state after disabling plane tracking"
                }

                // Subscribing or creating anchors on previously discovered planes while disabled
                // must throw IllegalStateException
                assertFailsWith<IllegalStateException> { Plane.subscribe(session) }
                for (plane in trackedPlanes!!) {
                    assertFailsWith<IllegalStateException> { plane.createAnchor(Pose.Identity) }
                }

                // Re-enabling plane tracking restores Plane.subscribe and anchor creation on the
                // same session
                val reEnableResult = session.configure(enableConfig)
                assertThat(reEnableResult).isInstanceOf(SessionConfigureSuccess::class.java)
                val planesAfterReEnable =
                    withTimeoutOrNull(PLANE_DISCOVERY_TIMEOUT_MS) {
                        Plane.subscribe(session).first { list ->
                            list.any { it.state.value.trackingState == TrackingState.TRACKING }
                        }
                    }
                assumeTrue(
                    "Skipping test: No tracking planes detected after re-enabling within timeout",
                    planesAfterReEnable != null,
                )
                val recoveredPlane =
                    planesAfterReEnable!!.first {
                        it.state.value.trackingState == TrackingState.TRACKING
                    }
                val anchorResult = recoveredPlane.createAnchor(Pose.Identity)
                assertThat(anchorResult).isInstanceOf(AnchorCreateSuccess::class.java)
                (anchorResult as AnchorCreateSuccess).anchor.detach()
            }
        }
    }

    @Test
    @XrDeviceTest
    fun planeTracking_createAnchorOnPlane_composesWorldPoseAndDetachesCleanly() {
        runBlocking {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                val session =
                    createSessionWithPlaneTracking(
                        scenario,
                        planeTrackingMode = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
                    )

                val planesFlow = Plane.subscribe(session)
                val trackingPlanes =
                    withTimeoutOrNull(PLANE_DISCOVERY_TIMEOUT_MS) {
                        planesFlow.first { planes ->
                            planes.any { it.state.value.trackingState == TrackingState.TRACKING }
                        }
                    }
                assumeTrue(
                    "Skipping test: No tracking plane detected in environment within timeout",
                    trackingPlanes != null,
                )

                val firstPlane =
                    trackingPlanes!!.first {
                        it.state.value.trackingState == TrackingState.TRACKING
                    }
                val anchorResult = firstPlane.createAnchor(Pose.Identity)

                assertThat(anchorResult).isInstanceOf(AnchorCreateSuccess::class.java)
                val anchor = (anchorResult as AnchorCreateSuccess).anchor

                try {
                    val trackingAnchorState =
                        withTimeoutOrNull(SESSION_STATE_TIMEOUT_MS) {
                            anchor.state.first { it.trackingState == TrackingState.TRACKING }
                        }
                    checkNotNull(trackingAnchorState) {
                        "Timed out waiting for plane-attached anchor to enter TRACKING state"
                    }

                    assertPoseEquals(trackingAnchorState.pose, firstPlane.state.value.centerPose)
                } finally {
                    anchor.detach()
                }
            }
        }
    }

    @Test
    @XrDeviceTest
    fun planeTracking_activityRecreation_reinitializesSessionAndPlanePipeline() {
        runBlocking {
            ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
                val initialSession =
                    createSessionWithPlaneTracking(
                        scenario,
                        planeTrackingMode = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
                    )

                val initialPlanesFlow = Plane.subscribe(initialSession)
                assertThat(initialPlanesFlow.first()).isNotNull()

                scenario.recreate()

                // Upon activity recreation, the initial Activity and its attached Session are
                // destroyed, and a new Session is created and configured for the recreated
                // Activity.
                val newSession =
                    createSessionWithPlaneTracking(
                        scenario,
                        planeTrackingMode = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
                    )

                assertThat(newSession).isNotSameInstanceAs(initialSession)

                val newPlanesFlow = Plane.subscribe(newSession)
                assertThat(newPlanesFlow).isNotSameInstanceAs(initialPlanesFlow)
                assertThat(newPlanesFlow.first()).isNotNull()
            }
        }
    }

    private companion object {
        /** Standard 5-second timeout used across JXR tests for session/anchor state updates. */
        private const val SESSION_STATE_TIMEOUT_MS = 5_000L

        /**
         * 10-second timeout for initial SLAM plane discovery, matching the upper bound used in JXR
         * integration tests while avoiding excessive skip latency on untextured scenes or
         * emulators.
         */
        private const val PLANE_DISCOVERY_TIMEOUT_MS = 10_000L
    }
}
