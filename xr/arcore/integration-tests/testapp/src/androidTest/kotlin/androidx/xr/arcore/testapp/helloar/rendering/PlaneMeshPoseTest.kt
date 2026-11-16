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

package androidx.xr.arcore.testapp.helloar.rendering

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import androidx.xr.arcore.TrackingState
import androidx.xr.arcore.perceptionState
import androidx.xr.arcore.testapp.flags.RuntimeFlagsActivity
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionConfigureSuccess
import androidx.xr.runtime.SessionCreateApkRequired
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.SessionCreateUnsupportedDevice
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.SpatialCapability
import androidx.xr.scenecore.scene
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.filterSupportedPermissions
import androidx.xr.testutils.waitUntil
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.abs
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assume.assumeTrue
import org.junit.AssumptionViolatedException
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Live XR integration smoke test verifying SceneCore [PlaneRenderer] mesh alignment against tracked
 * plane center poses. Hosted on lightweight [RuntimeFlagsActivity] to isolate [PlaneRenderer]
 * without duplicate plane subscriptions or extra UI overlays.
 *
 * Synthetic plane playback injection is not yet available in instrumentation tests, so
 * deterministic anchor-drift and lifecycle transitions live in `PlaneMeshPoseTrackerTest`, while
 * live runs skip via `assumeTrue` when no tracked plane drifts beyond [MIN_PLANE_DRIFT_METERS].
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class PlaneMeshPoseTest {

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            *filterSupportedPermissions(
                "android.permission.SCENE_UNDERSTANDING_COARSE",
                "android.permission.HEAD_TRACKING",
                "android.permission.CAMERA",
            )
        )

    private data class RenderedPlanePoseSnapshot(
        val renderedWorldPose: Pose,
        val centerPose: Pose,
        val anchorToCenterTranslationDelta: Float,
    )

    @Test
    @XrDeviceTest
    fun planeRenderer_whenTrackingLivePlanes_alignsMeshPoseWithPlaneCenterPose() {
        ActivityScenario.launch(RuntimeFlagsActivity::class.java).use { scenario ->
            var launchedActivity: RuntimeFlagsActivity? = null
            scenario.onActivity { launchedActivity = it }
            val activity =
                checkNotNull(launchedActivity) {
                    "ActivityScenario did not deliver RuntimeFlagsActivity"
                }
            val session = createPlaneTrackingSession(activity)
            awaitFullSpaceMode(session)
            val planeRenderer = PlaneRenderer(session)
            runBlocking(Dispatchers.Main.immediate) {
                activity.lifecycle.addObserver(planeRenderer)
            }

            // Leave the observer attached so ActivityScenario.close() dispatches ON_PAUSE to
            // cancel renderScope and clear rendered entities.
            val hasDriftedPlane = runBlocking { awaitDriftedActivePlanes(session, planeRenderer) }
            assumeTrue(
                "Skipping test: No drifted plane (delta > ${MIN_PLANE_DRIFT_METERS}m) " +
                    "detected within timeout",
                hasDriftedPlane,
            )

            var latestSnapshots: List<RenderedPlanePoseSnapshot> = emptyList()
            try {
                waitUntil(
                    description = "rendered plane meshes aligned with center poses",
                    timeout = ALIGNMENT_WAIT_TIMEOUT,
                ) {
                    val snapshots = captureRenderedPlaneSnapshots(session, planeRenderer)
                    latestSnapshots = snapshots
                    snapshots.any { it.anchorToCenterTranslationDelta > MIN_PLANE_DRIFT_METERS } &&
                        snapshots.all(::isSnapshotAligned)
                }
            } catch (e: AssertionError) {
                assertWithMessage("Timed out waiting for aligned plane poses: ${e.message}")
                    .that(latestSnapshots)
                    .isNotEmpty()
                latestSnapshots.forEachIndexed { index, snapshot ->
                    assertPoseWithin(
                        planeLabel =
                            "plane[$index] (drift=${snapshot.anchorToCenterTranslationDelta}m)",
                        actual = snapshot.renderedWorldPose,
                        expected = snapshot.centerPose,
                        tolerance = DEVICE_POSE_TOLERANCE,
                    )
                }
                assertWithMessage(
                        "Timed out waiting for a plane retaining drift > " +
                            "${MIN_PLANE_DRIFT_METERS}m: ${e.message}"
                    )
                    .that(latestSnapshots.maxOf { it.anchorToCenterTranslationDelta })
                    .isGreaterThan(MIN_PLANE_DRIFT_METERS)
                throw e
            }
        }
    }

    private fun createPlaneTrackingSession(activity: RuntimeFlagsActivity): Session =
        runBlocking(Dispatchers.Main.immediate) {
            val session =
                when (
                    val createResult = Session.create(context = activity, lifecycleOwner = activity)
                ) {
                    is SessionCreateSuccess -> createResult.session
                    is SessionCreateApkRequired ->
                        throw AssumptionViolatedException(
                            "Skipping test: XR runtime APK required on device"
                        )
                    is SessionCreateUnsupportedDevice ->
                        throw AssumptionViolatedException("Skipping test: XR unsupported on device")
                    else -> throw AssertionError("Session.create failed with result: $createResult")
                }
            val config =
                Config.Builder()
                    .setDeviceTracking(DeviceTrackingMode.SPATIAL)
                    .setPlaneTracking(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)
                    .build()
            val configResult = session.configure(config)
            check(configResult is SessionConfigureSuccess) {
                "Session.configure failed with result: $configResult"
            }
            session.scene.requestFullSpace()
            val initialPerceptionState =
                withTimeoutOrNull(SESSION_STATE_TIMEOUT) {
                    session.state.first { it.perceptionState != null }
                }
            checkNotNull(initialPerceptionState) {
                "Timed out waiting for initial perceptionState after configuring Session"
            }
            session
        }

    private fun awaitFullSpaceMode(session: Session) {
        waitUntil(
            description = "session enters Full Space mode",
            timeout = FULL_SPACE_WAIT_TIMEOUT,
        ) {
            runBlocking(Dispatchers.Main.immediate) {
                SpatialCapability.SPATIAL_UI in session.scene.spatialCapabilities
            }
        }
    }

    private suspend fun awaitDriftedActivePlanes(
        session: Session,
        planeRenderer: PlaneRenderer,
    ): Boolean =
        withTimeoutOrNull(PLANE_DISCOVERY_TIMEOUT) {
            combine(planeRenderer.renderedPlanes, session.state) { models, _ ->
                    models.any { model ->
                        isTrackedPlaneWithLocatedAnchor(model) &&
                            Pose.distance(
                                model.stateFlow.value.centerPose,
                                model.anchor.state.value.pose,
                            ) > MIN_PLANE_DRIFT_METERS
                    }
                }
                .first { hasDriftedPlane -> hasDriftedPlane }
        } != null

    private fun isTrackedPlaneWithLocatedAnchor(model: PlaneModel): Boolean {
        val planeState = model.stateFlow.value
        val anchorState = model.anchor.state.value
        return planeState.trackingState == TrackingState.TRACKING &&
            planeState.subsumedBy == null &&
            anchorState.trackingState == TrackingState.TRACKING
    }

    private fun captureRenderedPlaneSnapshots(
        session: Session,
        planeRenderer: PlaneRenderer,
    ): List<RenderedPlanePoseSnapshot> =
        runBlocking(Dispatchers.Main.immediate) {
            planeRenderer.renderedPlanes.value.filter(::isTrackedPlaneWithLocatedAnchor).map {
                createRenderedPlaneSnapshot(session, it)
            }
        }

    private fun createRenderedPlaneSnapshot(
        session: Session,
        model: PlaneModel,
    ): RenderedPlanePoseSnapshot {
        val planeState = model.stateFlow.value
        val anchorPose = model.anchor.state.value.pose
        // Read SceneCore's composed world pose directly rather than recomputing
        // anchorPose compose meshLocalPose from anchor state.
        val renderedWorldPose =
            model.modelEntity.transformPoseTo(Pose.Identity, session.scene.perceptionSpace)
        return RenderedPlanePoseSnapshot(
            renderedWorldPose = renderedWorldPose,
            centerPose = planeState.centerPose,
            anchorToCenterTranslationDelta = Pose.distance(planeState.centerPose, anchorPose),
        )
    }

    private fun isSnapshotAligned(snapshot: RenderedPlanePoseSnapshot): Boolean =
        isPoseWithin(
            actual = snapshot.renderedWorldPose,
            expected = snapshot.centerPose,
            tolerance = DEVICE_POSE_TOLERANCE,
        )

    private fun poseMaxComponentDelta(actual: Pose, expected: Pose): Float {
        val sign = if (actual.rotation.dot(expected.rotation) < 0f) -1f else 1f
        return maxOf(
            abs(actual.translation.x - expected.translation.x),
            abs(actual.translation.y - expected.translation.y),
            abs(actual.translation.z - expected.translation.z),
            abs(actual.rotation.x * sign - expected.rotation.x),
            abs(actual.rotation.y * sign - expected.rotation.y),
            abs(actual.rotation.z * sign - expected.rotation.z),
            abs(actual.rotation.w * sign - expected.rotation.w),
        )
    }

    private fun isPoseWithin(actual: Pose, expected: Pose, tolerance: Float): Boolean =
        poseMaxComponentDelta(actual, expected) <= tolerance

    private fun assertPoseWithin(
        planeLabel: String,
        actual: Pose,
        expected: Pose,
        tolerance: Float,
    ) {
        assertWithMessage("$planeLabel translation.x")
            .that(actual.translation.x)
            .isWithin(tolerance)
            .of(expected.translation.x)
        assertWithMessage("$planeLabel translation.y")
            .that(actual.translation.y)
            .isWithin(tolerance)
            .of(expected.translation.y)
        assertWithMessage("$planeLabel translation.z")
            .that(actual.translation.z)
            .isWithin(tolerance)
            .of(expected.translation.z)
        val sign = if (actual.rotation.dot(expected.rotation) < 0f) -1f else 1f
        assertWithMessage("$planeLabel rotation.x")
            .that(actual.rotation.x * sign)
            .isWithin(tolerance)
            .of(expected.rotation.x)
        assertWithMessage("$planeLabel rotation.y")
            .that(actual.rotation.y * sign)
            .isWithin(tolerance)
            .of(expected.rotation.y)
        assertWithMessage("$planeLabel rotation.z")
            .that(actual.rotation.z * sign)
            .isWithin(tolerance)
            .of(expected.rotation.z)
        assertWithMessage("$planeLabel rotation.w")
            .that(actual.rotation.w * sign)
            .isWithin(tolerance)
            .of(expected.rotation.w)
    }

    private companion object {
        private const val DEVICE_POSE_TOLERANCE = 1e-2f
        private const val MIN_PLANE_DRIFT_METERS = 3 * DEVICE_POSE_TOLERANCE
        private val PLANE_DISCOVERY_TIMEOUT = 10.seconds
        private val SESSION_STATE_TIMEOUT = 5.seconds
        private val FULL_SPACE_WAIT_TIMEOUT = 5.seconds
        private val ALIGNMENT_WAIT_TIMEOUT = 10.seconds
    }
}
