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
import androidx.xr.arcore.Anchor
import androidx.xr.arcore.AnchorCreateSuccess
import androidx.xr.arcore.AnchorCreateTrackingUnavailable
import androidx.xr.arcore.AnchorResult
import androidx.xr.arcore.ArDevice
import androidx.xr.arcore.CreateGeospatialPoseFromPoseSuccess
import androidx.xr.arcore.CreatePoseFromGeospatialPoseResult
import androidx.xr.arcore.CreatePoseFromGeospatialPoseSuccess
import androidx.xr.arcore.Geospatial
import androidx.xr.arcore.GeospatialSurface
import androidx.xr.arcore.TrackingState
import androidx.xr.arcore.VpsAvailabilityAvailable
import androidx.xr.arcore.VpsAvailabilityNetworkError
import androidx.xr.arcore.perceptionState
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.GeospatialMode
import androidx.xr.runtime.SessionConfigureSuccess
import androidx.xr.runtime.XrDevice
import androidx.xr.runtime.math.GeospatialPose
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.createXrSessionRule
import androidx.xr.testutils.filterSupportedPermissions
import com.google.common.truth.Truth.assertThat
import kotlin.math.hypot
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.AssumptionViolatedException
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Automated integration tests for JXR ARCore [Geospatial] tracking, VPS availability checks, pose
 * conversions, WGS84 and Terrain anchors, and disabled-mode Rooftop anchor guards.
 */
// TODO: b/578158568 - Add ungranted-location SecurityException and post-grant
// ActivityScenario.recreate() coverage (also see b/579103586) in a host-driven Mobly test or
// separate ungranted-permission APK. This non-orchestrated suite grants location upfront via
// GrantPermissionRule, and revoking a runtime permission in-process kills the instrumented app UID
// before assertions or recreate() can run.
@RunWith(AndroidJUnit4::class)
@LargeTest
class GeospatialTrackingTest {

    @get:Rule(order = 0)
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            *filterSupportedPermissions(
                "android.permission.INTERNET",
                "android.permission.ACCESS_FINE_LOCATION",
                "android.permission.ACCESS_COARSE_LOCATION",
                "android.permission.HEAD_TRACKING",
            )
        )

    @get:Rule(order = 1) val sessionRule = createXrSessionRule()

    private fun assumeSpatialGeospatialSupported() {
        val xrDevice = XrDevice.getCurrentDevice(sessionRule.session.context)
        assumeTrue(
            "Skipping test: Device does not support GeospatialMode.SPATIAL",
            xrDevice.isGeospatialModeSupported(GeospatialMode.SPATIAL),
        )
    }

    private fun isGeospatialErrorState(state: Geospatial.GeospatialTrackingState): Boolean {
        return state == Geospatial.GeospatialTrackingState.ERROR_INTERNAL ||
            state == Geospatial.GeospatialTrackingState.ERROR_NOT_AUTHORIZED ||
            state == Geospatial.GeospatialTrackingState.ERROR_RESOURCE_EXHAUSTED
    }

    private suspend fun configureSpatialGeospatialAndAwaitRunning(): Geospatial.State {
        val session = sessionRule.session
        assumeSpatialGeospatialSupported()
        val config =
            Config.Builder()
                .setDeviceTracking(DeviceTrackingMode.SPATIAL)
                .setGeospatial(GeospatialMode.SPATIAL)
                .build()
        val configResult = session.configure(config)
        check(configResult is SessionConfigureSuccess) {
            "Failed to configure GeospatialMode.SPATIAL: $configResult"
        }
        val perceptionUpdate =
            withTimeoutOrNull(FRAME_TIMEOUT_MS) {
                session.state.drop(1).first { it.perceptionState != null }
            }
        checkNotNull(perceptionUpdate) { "Timed out waiting for perception state" }
        val geospatial = Geospatial.getInstance(session)
        val terminalOrRunning =
            withTimeoutOrNull(GEOSPATIAL_TRACKING_TIMEOUT_MS) {
                geospatial.state.first {
                    it.geospatialTrackingState == Geospatial.GeospatialTrackingState.RUNNING ||
                        isGeospatialErrorState(it.geospatialTrackingState)
                }
            }
        val matchedState =
            terminalOrRunning
                ?: throw AssumptionViolatedException(
                    "Skipping test: Geospatial tracker remained in " +
                        "${geospatial.state.value.geospatialTrackingState} for " +
                        "${GEOSPATIAL_TRACKING_TIMEOUT_MS}ms (requires network connectivity, " +
                        "Google Location Accuracy opt-in, and outdoor VPS localization)"
                )
        check(matchedState.geospatialTrackingState == Geospatial.GeospatialTrackingState.RUNNING) {
            "Geospatial tracker entered error state: ${matchedState.geospatialTrackingState}"
        }
        return matchedState
    }

    private suspend fun configureSpatialGeospatialAndAwaitLocalized(): Geospatial.State {
        val runningState = configureSpatialGeospatialAndAwaitRunning()
        val geospatial = runningState.owner
        val localizedState =
            withTimeoutOrNull(GEOSPATIAL_TRACKING_TIMEOUT_MS) {
                geospatial.state.first {
                    isGeospatialErrorState(it.geospatialTrackingState) ||
                        (it.geospatialTrackingState == Geospatial.GeospatialTrackingState.RUNNING &&
                            it.horizontalAccuracy > 0.0 &&
                            it.horizontalAccuracy < MAX_LOCALIZED_HORIZONTAL_ACCURACY_METERS)
                }
            }
        check(
            localizedState == null ||
                !isGeospatialErrorState(localizedState.geospatialTrackingState)
        ) {
            "Geospatial tracker entered error state: ${localizedState?.geospatialTrackingState}"
        }
        return localizedState
            ?: throw AssumptionViolatedException(
                "Skipping test: Geospatial horizontalAccuracy=" +
                    "${geospatial.state.value.horizontalAccuracy}m did not converge below " +
                    "${MAX_LOCALIZED_HORIZONTAL_ACCURACY_METERS}m (requires network connectivity, " +
                    "Google Location Accuracy opt-in, and outdoor VPS localization)"
            )
    }

    private suspend fun awaitTrackedDevicePoseOrSkip(): Pose {
        val arDevice = ArDevice.getInstance(sessionRule.session)
        val trackedState =
            withTimeoutOrNull(FRAME_TIMEOUT_MS) {
                arDevice.state.first { it.trackingState == TrackingState.TRACKING }
            }
        val matched =
            trackedState
                ?: throw AssumptionViolatedException(
                    "Skipping test: ArDevice did not reach TRACKING state within timeout"
                )
        return matched.devicePose
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

    private fun assertFiniteGeospatialPose(geospatialPose: GeospatialPose) {
        assertThat(geospatialPose.latitude).isFinite()
        assertThat(geospatialPose.longitude).isFinite()
        assertThat(geospatialPose.altitude).isFinite()
        assertThat(geospatialPose.eastUpSouthQuaternion.x).isFinite()
        assertThat(geospatialPose.eastUpSouthQuaternion.y).isFinite()
        assertThat(geospatialPose.eastUpSouthQuaternion.z).isFinite()
        assertThat(geospatialPose.eastUpSouthQuaternion.w).isFinite()
    }

    private data class GeospatialRoundTripTrace(
        val devicePose: Pose,
        val geoPoseResult: CreateGeospatialPoseFromPoseSuccess,
        val roundTripPoseResult: CreatePoseFromGeospatialPoseResult,
        val anchorResult: AnchorResult,
        val anchorState: Anchor.State?,
    )

    private suspend fun runGeospatialRoundTripAndCreateAnchor(
        geospatial: Geospatial,
        devicePose: Pose,
    ): GeospatialRoundTripTrace {
        val geoPoseResult = geospatial.createGeospatialPoseFromPose(devicePose)
        val geoPoseSuccess =
            geoPoseResult as? CreateGeospatialPoseFromPoseSuccess
                ?: throw AssumptionViolatedException(
                    "Skipping round-trip assertions: createGeospatialPoseFromPose returned $geoPoseResult"
                )
        assumeTrue(
            "Skipping round-trip assertions: verticalAccuracy=${geoPoseSuccess.verticalAccuracy}m >= ${MAX_LOCALIZED_VERTICAL_ACCURACY_METERS}m",
            geoPoseSuccess.verticalAccuracy < MAX_LOCALIZED_VERTICAL_ACCURACY_METERS,
        )
        val roundTripPoseResult = geospatial.createPoseFromGeospatialPose(geoPoseSuccess.pose)
        val anchorResult =
            geospatial.createAnchor(
                latitude = geoPoseSuccess.pose.latitude,
                longitude = geoPoseSuccess.pose.longitude,
                altitude = geoPoseSuccess.pose.altitude,
                eastUpSouthQuaternion = geoPoseSuccess.pose.eastUpSouthQuaternion,
            )
        return try {
            val anchorState =
                if (anchorResult is AnchorCreateSuccess) {
                    withTimeoutOrNull(GEOSPATIAL_TRACKING_TIMEOUT_MS) {
                        anchorResult.anchor.state.first {
                            it.trackingState == TrackingState.TRACKING
                        }
                    }
                        ?: throw AssumptionViolatedException(
                            "Skipping WGS84 anchor pose assertions: Created anchor remained in " +
                                "${anchorResult.anchor.state.value.trackingState} for " +
                                "${GEOSPATIAL_TRACKING_TIMEOUT_MS}ms"
                        )
                } else {
                    null
                }
            GeospatialRoundTripTrace(
                devicePose = devicePose,
                geoPoseResult = geoPoseSuccess,
                roundTripPoseResult = roundTripPoseResult,
                anchorResult = anchorResult,
                anchorState = anchorState,
            )
        } finally {
            (anchorResult as? AnchorCreateSuccess)?.anchor?.detach()
        }
    }

    private data class TerrainAnchorTrace(
        val anchorResult: AnchorResult,
        val anchorState: Anchor.State?,
    )

    private suspend fun createAndAwaitTrackedTerrainAnchor(
        geospatial: Geospatial,
        geoPose: GeospatialPose,
    ): TerrainAnchorTrace {
        val anchorResult =
            withTimeoutOrNull(GEOSPATIAL_TRACKING_TIMEOUT_MS) {
                geospatial.createAnchorOnSurface(
                    latitude = geoPose.latitude,
                    longitude = geoPose.longitude,
                    altitudeAboveSurface = 0.0,
                    eastUpSouthQuaternion = geoPose.eastUpSouthQuaternion,
                    surface = GeospatialSurface.TERRAIN,
                )
            }
                ?: throw AssumptionViolatedException(
                    "Skipping terrain anchor test: createAnchorOnSurface timed out (requires network/VPS)"
                )
        return try {
            assumeFalse(
                "Skipping terrain anchor test: Surface anchor tracking unavailable",
                anchorResult is AnchorCreateTrackingUnavailable,
            )
            val anchorState =
                if (anchorResult is AnchorCreateSuccess) {
                    withTimeoutOrNull(GEOSPATIAL_TRACKING_TIMEOUT_MS) {
                        anchorResult.anchor.state.first {
                            it.trackingState == TrackingState.TRACKING
                        }
                    }
                        ?: throw AssumptionViolatedException(
                            "Skipping terrain anchor test: Created TERRAIN anchor remained in " +
                                "${anchorResult.anchor.state.value.trackingState} for " +
                                "${GEOSPATIAL_TRACKING_TIMEOUT_MS}ms"
                        )
                } else {
                    null
                }
            TerrainAnchorTrace(anchorResult = anchorResult, anchorState = anchorState)
        } finally {
            (anchorResult as? AnchorCreateSuccess)?.anchor?.detach()
        }
    }

    @Test
    @XrDeviceTest
    fun configure_whenGeospatialUnsupported_throwsUnsupportedOperationException() {
        val session = sessionRule.session
        val xrDevice = XrDevice.getCurrentDevice(session.context)
        assumeFalse(
            "Skipping test: Device supports GeospatialMode.SPATIAL",
            xrDevice.isGeospatialModeSupported(GeospatialMode.SPATIAL),
        )
        val config = Config.Builder().setGeospatial(GeospatialMode.SPATIAL).build()

        assertFailsWith<UnsupportedOperationException> { session.configure(config) }
        assertThat(session.config.geospatial).isEqualTo(GeospatialMode.DISABLED)
    }

    @Test
    @XrDeviceTest
    fun checkVpsAvailability_whenGeospatialSupported_returnsAvailable() {
        runBlocking {
            assumeSpatialGeospatialSupported()
            val geospatial = Geospatial.getInstance(sessionRule.session)

            val vpsResult =
                withTimeoutOrNull(VPS_TIMEOUT_MS) {
                    geospatial.checkVpsAvailability(TEST_LATITUDE, TEST_LONGITUDE)
                }
            assumeTrue(
                "Skipping test: checkVpsAvailability timed out (no network in CI environment)",
                vpsResult != null,
            )
            assumeFalse(
                "Skipping test: checkVpsAvailability failed due to network error",
                vpsResult is VpsAvailabilityNetworkError,
            )

            assertThat(vpsResult).isInstanceOf(VpsAvailabilityAvailable::class.java)
        }
    }

    @Test
    @XrDeviceTest
    fun geospatial_whenSpatialModeRunning_emitsRunningStateAndPositiveAccuracies() {
        runBlocking {
            val session = sessionRule.session

            val activeState = configureSpatialGeospatialAndAwaitLocalized()

            assertThat(session.config.geospatial).isEqualTo(GeospatialMode.SPATIAL)
            assertThat(activeState.owner).isEqualTo(Geospatial.getInstance(session))
            assertThat(activeState.verticalAccuracy).isFinite()
            assertThat(activeState.verticalAccuracy).isGreaterThan(0.0)
            assertThat(activeState.orientationYawAccuracy).isFinite()
            assertThat(activeState.orientationYawAccuracy).isGreaterThan(0.0)
        }
    }

    @Test
    @XrDeviceTest
    fun geospatial_whenSpatialModeRunning_roundTripsDevicePoseAndCreatesWgs84Anchor() {
        runBlocking {
            val localizedState = configureSpatialGeospatialAndAwaitLocalized()
            val geospatial = localizedState.owner
            val devicePose = awaitTrackedDevicePoseOrSkip()

            val trace = runGeospatialRoundTripAndCreateAnchor(geospatial, devicePose)

            assertFiniteGeospatialPose(trace.geoPoseResult.pose)
            assertThat(trace.geoPoseResult.horizontalAccuracy).isGreaterThan(0.0)
            assertThat(trace.geoPoseResult.verticalAccuracy).isGreaterThan(0.0)
            assertThat(trace.geoPoseResult.orientationYawAccuracy).isGreaterThan(0.0)
            val roundTripPoseSuccess =
                assertIs<CreatePoseFromGeospatialPoseSuccess>(trace.roundTripPoseResult)
            assertFinitePose(roundTripPoseSuccess.pose)
            assertThat(Pose.distance(roundTripPoseSuccess.pose, trace.devicePose))
                .isLessThan(ROUND_TRIP_POSITION_TOLERANCE_METERS)
            assertThat(
                    Quaternion.angle(roundTripPoseSuccess.pose.rotation, trace.devicePose.rotation)
                )
                .isLessThan(ROUND_TRIP_ORIENTATION_TOLERANCE_DEGREES)
            assertIs<AnchorCreateSuccess>(trace.anchorResult)
            val anchorState = checkNotNull(trace.anchorState)
            assertFinitePose(anchorState.pose)
            // Horizontal and vertical accuracies are 68th-percentile 1-sigma radii; 2 * hypot(h, v)
            // bounds expected local-pose shifts if VPS refines the transform during anchor
            // creation.
            val anchorPositionToleranceMeters =
                maxOf(
                    MIN_ANCHOR_POSITION_TOLERANCE_METERS,
                    hypot(
                            trace.geoPoseResult.horizontalAccuracy,
                            trace.geoPoseResult.verticalAccuracy,
                        )
                        .toFloat() * 2f,
                )
            assertThat(Pose.distance(anchorState.pose, trace.devicePose))
                .isLessThan(anchorPositionToleranceMeters)
        }
    }

    @Test
    @XrDeviceTest
    fun createAnchorOnSurface_whenSpatialModeRunning_createsTrackedTerrainAnchor() {
        runBlocking {
            val runningState = configureSpatialGeospatialAndAwaitRunning()
            val geospatial = runningState.owner
            val devicePose = awaitTrackedDevicePoseOrSkip()
            val geoPoseResult = geospatial.createGeospatialPoseFromPose(devicePose)
            val geoPoseSuccess =
                geoPoseResult as? CreateGeospatialPoseFromPoseSuccess
                    ?: throw AssumptionViolatedException(
                        "Skipping terrain anchor test: createGeospatialPoseFromPose returned $geoPoseResult"
                    )

            val trace = createAndAwaitTrackedTerrainAnchor(geospatial, geoPoseSuccess.pose)

            assertIs<AnchorCreateSuccess>(trace.anchorResult)
            val trackedState = checkNotNull(trace.anchorState)
            assertFinitePose(trackedState.pose)
        }
    }

    @Test
    @XrDeviceTest
    fun createAnchor_atPolarLatitude_throwsIllegalArgumentException() {
        runBlocking {
            val runningState = configureSpatialGeospatialAndAwaitRunning()
            val geospatial = runningState.owner

            assertFailsWith<IllegalArgumentException> {
                geospatial.createAnchor(
                    latitude = NORTH_POLAR_EXCLUDED_LATITUDE,
                    longitude = TEST_LONGITUDE,
                    altitude = TEST_ALTITUDE,
                    eastUpSouthQuaternion = Quaternion.Identity,
                )
            }
            assertFailsWith<IllegalArgumentException> {
                geospatial.createAnchor(
                    latitude = SOUTH_POLAR_EXCLUDED_LATITUDE,
                    longitude = TEST_LONGITUDE,
                    altitude = TEST_ALTITUDE,
                    eastUpSouthQuaternion = Quaternion.Identity,
                )
            }
        }
    }

    @Test
    @XrDeviceTest
    fun createPoseFromGeospatialPose_atPolarLatitude_throwsIllegalArgumentException() {
        runBlocking {
            val runningState = configureSpatialGeospatialAndAwaitRunning()
            val geospatial = runningState.owner
            val northPolarPose =
                GeospatialPose(
                    latitude = NORTH_POLAR_EXCLUDED_LATITUDE,
                    longitude = TEST_LONGITUDE,
                    altitude = TEST_ALTITUDE,
                    eastUpSouthQuaternion = Quaternion.Identity,
                )
            val southPolarPose =
                GeospatialPose(
                    latitude = SOUTH_POLAR_EXCLUDED_LATITUDE,
                    longitude = TEST_LONGITUDE,
                    altitude = TEST_ALTITUDE,
                    eastUpSouthQuaternion = Quaternion.Identity,
                )

            assertFailsWith<IllegalArgumentException> {
                geospatial.createPoseFromGeospatialPose(northPolarPose)
            }
            assertFailsWith<IllegalArgumentException> {
                geospatial.createPoseFromGeospatialPose(southPolarPose)
            }
        }
    }

    @Test
    @XrDeviceTest
    fun createAnchorOnSurface_whenDisabled_throwsIllegalStateException() {
        runBlocking {
            val session = sessionRule.session
            val geospatial = Geospatial.getInstance(session)
            val config = Config.Builder().setGeospatial(GeospatialMode.DISABLED).build()

            val configResult = session.configure(config)

            assertThat(configResult).isInstanceOf(SessionConfigureSuccess::class.java)
            assertFailsWith<IllegalStateException> {
                geospatial.createAnchorOnSurface(
                    latitude = TEST_LATITUDE,
                    longitude = TEST_LONGITUDE,
                    altitudeAboveSurface = 0.0,
                    eastUpSouthQuaternion = Quaternion.Identity,
                    surface = GeospatialSurface.TERRAIN,
                )
            }
            assertFailsWith<IllegalStateException> {
                geospatial.createAnchorOnSurface(
                    latitude = TEST_LATITUDE,
                    longitude = TEST_LONGITUDE,
                    altitudeAboveSurface = 0.0,
                    eastUpSouthQuaternion = Quaternion.Identity,
                    surface = GeospatialSurface.ROOFTOP,
                )
            }
        }
    }

    @Test
    @XrDeviceTest
    fun geospatial_poseConversionsAndAnchorWhenDisabled_throwIllegalStateException() {
        val session = sessionRule.session
        val geospatial = Geospatial.getInstance(session)
        val targetGeospatialPose =
            GeospatialPose(
                latitude = TEST_LATITUDE,
                longitude = TEST_LONGITUDE,
                altitude = TEST_ALTITUDE,
                eastUpSouthQuaternion = Quaternion.Identity,
            )
        val config = Config.Builder().setGeospatial(GeospatialMode.DISABLED).build()

        val configResult = session.configure(config)

        assertThat(configResult).isInstanceOf(SessionConfigureSuccess::class.java)
        assertFailsWith<IllegalStateException> {
            geospatial.createAnchor(
                latitude = TEST_LATITUDE,
                longitude = TEST_LONGITUDE,
                altitude = TEST_ALTITUDE,
                eastUpSouthQuaternion = Quaternion.Identity,
            )
        }
        assertFailsWith<IllegalStateException> {
            geospatial.createGeospatialPoseFromPose(Pose.Identity)
        }
        assertFailsWith<IllegalStateException> {
            geospatial.createPoseFromGeospatialPose(targetGeospatialPose)
        }
    }

    companion object {
        private const val FRAME_TIMEOUT_MS = 5_000L
        private const val GEOSPATIAL_TRACKING_TIMEOUT_MS = 10_000L
        private const val VPS_TIMEOUT_MS = 5_000L
        private const val MAX_LOCALIZED_HORIZONTAL_ACCURACY_METERS = 10.0
        private const val MAX_LOCALIZED_VERTICAL_ACCURACY_METERS = 20.0
        private const val ROUND_TRIP_POSITION_TOLERANCE_METERS = 0.1f
        private const val ROUND_TRIP_ORIENTATION_TOLERANCE_DEGREES = 2.0f
        private const val MIN_ANCHOR_POSITION_TOLERANCE_METERS = 0.5f
        private const val TEST_LATITUDE = 37.4220
        private const val TEST_LONGITUDE = -122.0841
        private const val TEST_ALTITUDE = 10.0
        // Geospatial API excludes latitudes within 0.1 degrees of the poles (90.0 / -90.0).
        private const val NORTH_POLAR_EXCLUDED_LATITUDE = 89.95
        private const val SOUTH_POLAR_EXCLUDED_LATITUDE = -89.95
    }
}
