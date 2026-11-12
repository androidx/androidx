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
import androidx.xr.arcore.Anchor
import androidx.xr.arcore.AnchorCreateSuccess
import androidx.xr.arcore.ArDevice
import androidx.xr.arcore.HitResult
import androidx.xr.arcore.Plane
import androidx.xr.arcore.TrackingState
import androidx.xr.arcore.hitTest
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Ray
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.filterSupportedPermissions
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.AssumptionViolatedException
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Automated end-to-end integration tests for the Plane Finder workflow, verifying [ArDevice]
 * tracking, [Plane] discovery gating, [hitTest] ray casting from the [ArDevice] forward gaze ray,
 * and plane-local anchor placement.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class PlaneFinderSmokeTest {

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            *filterSupportedPermissions(
                "android.permission.SCENE_UNDERSTANDING_COARSE",
                "android.permission.HEAD_TRACKING",
            )
        )

    private data class PlaneFinderSpec(
        val target: PlaneHitMatch,
        val centerBeforeCreate: Pose,
        val planeLocalHitPose: Pose,
    )

    private data class AnchoredPlaneFinderResult(
        val hitResults: List<HitResult>,
        val anchorTrackingState: Anchor.State,
        val expectedWorldPose: Pose,
    )

    private suspend fun awaitAnyTrackingPlanes(
        planesFlow: StateFlow<Collection<Plane>>
    ): Collection<Plane>? =
        withTimeoutOrNull(PLANE_DISCOVERY_TIMEOUT_MS) {
            planesFlow.first { list ->
                list.any {
                    val state = it.state.value
                    state.trackingState == TrackingState.TRACKING && state.subsumedBy == null
                }
            }
        }

    private fun tryGazePlaneHit(session: Session, arDevice: ArDevice): PlaneHitMatch? {
        val trackedPose = trackedDevicePoseOrNull(arDevice) ?: return null
        val gazeRay = Ray(trackedPose.translation, trackedPose.forward)
        return matchPlaneHit(gazeRay, hitTest(session, gazeRay))
    }

    // TODO: b/575892628 - Migrate gaze/live-plane discovery timeouts to deterministic synthetic
    // planes once perception simulation support is available.
    private suspend fun findTrackedGazePlaneHit(
        session: Session,
        planesFlow: StateFlow<Collection<Plane>>,
        arDevice: ArDevice,
    ): PlaneHitMatch? {
        awaitTrackedDevicePose(arDevice)
        awaitAnyTrackingPlanes(planesFlow) ?: return null
        return withTimeoutOrNull(PLANE_DISCOVERY_TIMEOUT_MS) {
            while (isActive) {
                tryGazePlaneHit(session, arDevice)?.let {
                    return@withTimeoutOrNull it
                }
                delay(HIT_RETRY_DELAY_MS)
            }
            null
        }
    }

    // TODO: b/581124038 - Skip instead of failing while Aura perception restarts paused mid-suite;
    // restore a hard failure once the perception playback fix lands.
    private suspend fun awaitTrackedDevicePose(arDevice: ArDevice): Pose {
        val trackingState =
            withTimeoutOrNull(PERCEPTION_TIMEOUT_MS) {
                arDevice.state.first { it.trackingState == TrackingState.TRACKING }
            }
                ?: throw AssumptionViolatedException(
                    "Skipping test: ArDevice did not reach TRACKING within " +
                        "${PERCEPTION_TIMEOUT_MS}ms"
                )
        return trackingState.devicePose
    }

    private suspend fun findPlaneFinderTarget(
        session: Session,
        planesFlow: StateFlow<Collection<Plane>>,
        arDevice: ArDevice,
    ): PlaneHitMatch =
        findTrackedGazePlaneHit(session, planesFlow, arDevice)
            ?: throw AssumptionViolatedException(
                "Skipping test: Forward gaze ray from ArDevice did not intersect any tracked " +
                    "plane within timeout"
            )

    private fun isPlaneFinderTargetStable(
        spec: PlaneFinderSpec,
        centerAfterTracking: Pose,
    ): Boolean =
        isPoseStable(centerAfterTracking, spec.centerBeforeCreate) &&
            isPoseStable(
                centerAfterTracking.compose(spec.planeLocalHitPose),
                spec.target.hit.hitPose,
            )

    private suspend fun awaitStablePlaneFinderAnchor(
        spec: PlaneFinderSpec,
        anchor: Anchor,
    ): AnchoredPlaneFinderResult? =
        withAnchorsDetached(anchor) {
            val anchorTrackingState = awaitTrackingAnchorState(anchor)
            val centerAfterTracking = spec.target.plane.state.value.centerPose
            if (isPlaneFinderTargetStable(spec, centerAfterTracking)) {
                AnchoredPlaneFinderResult(
                    hitResults = spec.target.hitResults,
                    anchorTrackingState = anchorTrackingState,
                    expectedWorldPose = spec.target.hit.hitPose,
                )
            } else {
                null
            }
        }

    private suspend fun tryCreateStablePlaneFinderAnchor(
        target: PlaneHitMatch
    ): AnchoredPlaneFinderResult? {
        val centerBeforeCreate = target.plane.state.value.centerPose
        val planeLocalHitPose = centerBeforeCreate.inverse.compose(target.hit.hitPose)
        val anchorResult = target.plane.createAnchor(planeLocalHitPose)
        check(anchorResult is AnchorCreateSuccess) {
            "Expected AnchorCreateSuccess when placing Plane Finder anchor: $anchorResult"
        }
        return awaitStablePlaneFinderAnchor(
            PlaneFinderSpec(
                target = target,
                centerBeforeCreate = centerBeforeCreate,
                planeLocalHitPose = planeLocalHitPose,
            ),
            anchorResult.anchor,
        )
    }

    private suspend fun createStablePlaneFinderAnchor(session: Session): AnchoredPlaneFinderResult {
        val planesFlow = Plane.subscribe(session)
        val arDevice = ArDevice.getInstance(session)
        val initialTarget = findPlaneFinderTarget(session, planesFlow, arDevice)
        return runWithStabilizedPlaneHit(
            initialTarget = initialTarget,
            findTarget = { tryGazePlaneHit(session, arDevice) },
        ) { stabilizedTarget ->
            tryCreateStablePlaneFinderAnchor(stabilizedTarget)
        }
    }

    @Test
    @XrDeviceTest
    fun arDevice_whenSpatialTracking_emitsTrackedFinitePose() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            initPlaneTrackingSessionAndRunTest(scenario) { session ->
                val arDevice = ArDevice.getInstance(session)

                val devicePose = awaitTrackedDevicePose(arDevice)

                assertThat(devicePose.translation.x).isFinite()
                assertThat(devicePose.translation.y).isFinite()
                assertThat(devicePose.translation.z).isFinite()
                assertThat(devicePose.rotation.x).isFinite()
                assertThat(devicePose.rotation.y).isFinite()
                assertThat(devicePose.rotation.z).isFinite()
                assertThat(devicePose.rotation.w).isFinite()
            }
        }
    }

    @Test
    @XrDeviceTest
    fun hitTest_fromDeviceGazeRayOnTrackedPlane_placesAnchorAtHitWorldPose() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            initPlaneTrackingSessionAndRunTest(scenario) { session ->
                val result = createStablePlaneFinderAnchor(session)

                assertThat(result.hitResults.map { it.distance }).isInOrder()
                assertPoseEquals(result.anchorTrackingState.pose, result.expectedWorldPose)
            }
        }
    }

    @Test
    @XrDeviceTest
    fun plane_subscribeWhenPlaneTrackingDisabled_throwsIllegalStateException() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            initPlaneTrackingSessionAndRunTest(scenario, PlaneTrackingMode.DISABLED) { session ->
                assertFailsWith<IllegalStateException> { Plane.subscribe(session) }
            }
        }
    }
}
