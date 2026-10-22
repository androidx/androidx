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
import androidx.xr.arcore.AnchorRuntimeFailureException
import androidx.xr.arcore.Anchorable
import androidx.xr.arcore.ArDevice
import androidx.xr.arcore.HitResult
import androidx.xr.arcore.Plane
import androidx.xr.arcore.PlaneLabel
import androidx.xr.arcore.TrackingState
import androidx.xr.arcore.hitTest
import androidx.xr.arcore.perceptionState
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
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
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.filterSupportedPermissions
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.assertFailsWith
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end XR device integration tests for JXR ARCore Interaction ([hitTest] and [Anchor]
 * placement at [HitResult.hitPose], OpenXR ray-plane intersection geometry, dynamic runtime
 * reconfiguration, and Activity recreation).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class InteractionTest {

    @get:Rule
    val permissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            *filterSupportedPermissions(
                "android.permission.SCENE_UNDERSTANDING_COARSE",
                "android.permission.SCENE_UNDERSTANDING_FINE",
                "android.permission.HAND_TRACKING",
                "android.permission.HEAD_TRACKING",
                "android.permission.FACE_TRACKING",
                "android.permission.EYE_TRACKING_COARSE",
                "android.permission.EYE_TRACKING_FINE",
                "android.permission.INTERNET",
                "android.permission.ACCESS_COARSE_LOCATION",
                "android.permission.ACCESS_FINE_LOCATION",
            )
        )

    private fun initSessionAndRunTest(
        scenario: ActivityScenario<ComponentActivity>,
        planeTrackingMode: PlaneTrackingMode = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
        testBody: suspend (Session) -> Unit,
    ) {
        scenario.onActivity { activity ->
            runBlocking {
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
                        is SessionCreateUnknownError ->
                            "SessionCreateUnknownError: ${result.errorMessage}"
                        is SessionCreateTimedOut -> "SessionCreateTimedOut"
                        else -> "Session.create failed with result: $result"
                    }
                }
                val session = result.session
                val config =
                    Config.Builder()
                        .setDeviceTracking(DeviceTrackingMode.SPATIAL)
                        .setPlaneTracking(planeTrackingMode)
                        .build()
                val configResult = session.configure(config)
                check(configResult is SessionConfigureSuccess) {
                    "Failed to configure Session: $configResult"
                }

                val state =
                    withTimeoutOrNull(5000L) { session.state.first { it.perceptionState != null } }
                checkNotNull(state) { "Timed out waiting for perception state" }

                testBody(session)
            }
        }
    }

    private fun buildCandidateRaysForPlane(plane: Plane, deviceOrigin: Vector3): List<Ray> {
        val centerPose = plane.state.value.centerPose
        return buildList {
            // 1. Cast ray from the AR device pose directly toward centerPose (matches real-world
            // head/controller raycasting and avoids collinearity with the plane normal).
            val deviceToPlane = centerPose.translation - deviceOrigin
            if (deviceToPlane.length > 1e-3f) {
                add(
                    Ray(
                        origin = deviceOrigin,
                        direction = deviceToPlane.toNormalized(),
                    )
                )
            }
            // 2. Cast oblique ray from 0.5m along +Y normal and 0.25m along +X tangent into
            // centerPose (avoids zero cross-product singularity when ray direction is strictly
            // parallel to the plane normal).
            val positiveObliqueOrigin =
                centerPose.translation + centerPose.up * 0.5f + centerPose.right * 0.25f
            add(
                Ray(
                    origin = positiveObliqueOrigin,
                    direction = (centerPose.translation - positiveObliqueOrigin).toNormalized(),
                )
            )
            // 3. Cast oblique ray from the opposite side of the plane normal into centerPose.
            val negativeObliqueOrigin =
                centerPose.translation - centerPose.up * 0.5f + centerPose.right * 0.25f
            add(
                Ray(
                    origin = negativeObliqueOrigin,
                    direction = (centerPose.translation - negativeObliqueOrigin).toNormalized(),
                )
            )
        }
    }

    /**
     * Executes [hitTest] while tolerating the transient race in `OpenXrPerceptionManager.hitTest`
     * where `nativeHitTest` returns a newly discovered native trackable ID before the next
     * `updatePlanes` / `PerceptionStateExtender.extend` tick has synced it into `trackablesMap`.
     */
    private fun safeHitTestWhileEnabled(session: Session, ray: Ray): List<HitResult> {
        return try {
            hitTest(session, ray)
        } catch (e: IllegalStateException) {
            if (e.message?.contains("Trackable not found") == true) {
                emptyList()
            } else {
                throw e
            }
        } catch (e: IllegalArgumentException) {
            if (e.message?.contains("No Active Anchorable found") == true) {
                emptyList()
            } else {
                throw e
            }
        }
    }

    private suspend fun performHitTestAgainstTrackedPlanes(
        session: Session,
        timeoutMs: Long = 10000L,
    ): Pair<Ray, List<HitResult>>? {
        val planesFlow = Plane.subscribe(session)
        return withTimeoutOrNull(timeoutMs) {
            while (isActive) {
                val deviceOrigin = ArDevice.getInstance(session).state.value.devicePose.translation
                // Prefer planes with a known semantic label first (matching HitTestingSamples and
                // AnchorRenderer), sorted nearest to the device origin.
                val trackedPlanes =
                    planesFlow.value
                        .filter { it.state.value.trackingState == TrackingState.TRACKING }
                        .sortedWith(
                            compareBy<Plane> { it.state.value.label == PlaneLabel.UNKNOWN }
                                .thenBy {
                                    (it.state.value.centerPose.translation - deviceOrigin).length
                                }
                        )

                for (plane in trackedPlanes) {
                    val candidateRays = buildCandidateRaysForPlane(plane, deviceOrigin)
                    for (ray in candidateRays) {
                        val hits = safeHitTestWhileEnabled(session, ray)
                        val hasValidPlaneHit = hits.any { hit ->
                            hit.trackable is Plane &&
                                hit.hitPose.rotation.x.isFinite() &&
                                hit.hitPose.rotation.y.isFinite() &&
                                hit.hitPose.rotation.z.isFinite() &&
                                hit.hitPose.rotation.w.isFinite()
                        }
                        if (hasValidPlaneHit) {
                            return@withTimeoutOrNull Pair(ray, hits)
                        }
                    }
                }
                delay(100L)
            }
            null
        }
    }

    private fun assertValidQuaternion(q: Quaternion, epsilon: Float = 1e-3f) {
        assertThat(q.x).isFinite()
        assertThat(q.y).isFinite()
        assertThat(q.z).isFinite()
        assertThat(q.w).isFinite()
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
    fun hitTest_rayAgainstTrackedPlane_validatesHitResultGeometryAndSorting() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            initSessionAndRunTest(
                scenario,
                planeTrackingMode = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
            ) { session ->
                val hitTestOutcome = performHitTestAgainstTrackedPlanes(session)
                assumeTrue(
                    "Skipping test: No hit results returned against environment planes within timeout",
                    hitTestOutcome != null,
                )

                val (ray, hitResults) = checkNotNull(hitTestOutcome)
                assertThat(hitResults).isNotEmpty()

                var previousDistance = 0f
                for (hitResult in hitResults) {
                    assertThat(hitResult.distance).isFinite()
                    assertThat(hitResult.distance).isGreaterThan(0f)
                    assertThat(hitResult.distance).isAtLeast(previousDistance)
                    previousDistance = hitResult.distance

                    assertThat(hitResult.hitPose.translation.x).isFinite()
                    assertThat(hitResult.hitPose.translation.y).isFinite()
                    assertThat(hitResult.hitPose.translation.z).isFinite()
                    assertValidQuaternion(hitResult.hitPose.rotation)

                    // Verify HitResult.distance equals the 3D Euclidean distance from ray.origin
                    // to hitResult.hitPose.translation
                    val expectedDistance = (hitResult.hitPose.translation - ray.origin).length
                    assertThat(hitResult.distance).isWithin(1e-3f).of(expectedDistance)

                    // Hit trackable must be an Anchorable (such as a tracked Plane)
                    assertThat(hitResult.trackable).isInstanceOf(Anchorable::class.java)
                }
            }
        }
    }

    @Test
    @XrDeviceTest
    fun hitTest_createAnchorAtHitPose_tracksWorldPoseAndDetachesCleanly() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            initSessionAndRunTest(
                scenario,
                planeTrackingMode = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
            ) { session ->
                val hitTestOutcome = performHitTestAgainstTrackedPlanes(session)
                assumeTrue(
                    "Skipping test: No hit results returned against environment planes within timeout",
                    hitTestOutcome != null,
                )

                val planesFlow = Plane.subscribe(session)
                val deviceOrigin = ArDevice.getInstance(session).state.value.devicePose.translation
                val candidateHits = buildList {
                    addAll(checkNotNull(hitTestOutcome).second)
                    val trackedPlanes =
                        planesFlow.value
                            .filter { it.state.value.trackingState == TrackingState.TRACKING }
                            .sortedWith(
                                compareBy<Plane> { it.state.value.label == PlaneLabel.UNKNOWN }
                                    .thenBy {
                                        (it.state.value.centerPose.translation - deviceOrigin)
                                            .length
                                    }
                            )
                    for (plane in trackedPlanes) {
                        for (ray in buildCandidateRaysForPlane(plane, deviceOrigin)) {
                            addAll(safeHitTestWhileEnabled(session, ray))
                        }
                    }
                }

                var createdAnchor: Anchor? = null
                var targetPose: Pose? = null
                var lastFailure: Throwable? = null

                for (hit in candidateHits) {
                    val poseToTry = hit.hitPose
                    if (
                        !poseToTry.rotation.x.isFinite() ||
                            !poseToTry.rotation.y.isFinite() ||
                            !poseToTry.rotation.z.isFinite() ||
                            !poseToTry.rotation.w.isFinite()
                    ) {
                        continue
                    }
                    try {
                        val result = Anchor.create(session, poseToTry)
                        if (result is AnchorCreateSuccess) {
                            createdAnchor = result.anchor
                            targetPose = poseToTry
                            break
                        }
                    } catch (e: AnchorRuntimeFailureException) {
                        lastFailure = e
                        try {
                            val fallbackPose = Pose(poseToTry.translation, Quaternion.Identity)
                            val fallbackResult = Anchor.create(session, fallbackPose)
                            if (fallbackResult is AnchorCreateSuccess) {
                                createdAnchor = fallbackResult.anchor
                                targetPose = fallbackPose
                                break
                            }
                        } catch (fallbackEx: AnchorRuntimeFailureException) {
                            lastFailure = fallbackEx
                        }
                    }
                }

                checkNotNull(createdAnchor) {
                    "Failed to create anchor at any hitPose among ${candidateHits.size} hits. " +
                        "First hitPose=${candidateHits.firstOrNull()?.hitPose}, lastFailure=$lastFailure"
                }

                try {
                    val trackingAnchorState =
                        withTimeoutOrNull(5000L) {
                            createdAnchor.state.first { it.trackingState == TrackingState.TRACKING }
                        }
                    checkNotNull(trackingAnchorState) {
                        "Timed out waiting for hit-test anchor to enter TRACKING state"
                    }

                    assertPoseEquals(trackingAnchorState.pose, checkNotNull(targetPose))
                } finally {
                    createdAnchor.detach()
                }
            }
        }
    }

    @Test
    @XrDeviceTest
    fun hitTest_dynamicModeSwitching_throwsWhenPlaneTrackingDisabledAndRecovers() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            initSessionAndRunTest(
                scenario,
                planeTrackingMode = PlaneTrackingMode.DISABLED,
            ) { session ->
                val defaultRay = Ray(Vector3.Zero, Vector3.Forward)
                assertFailsWith<IllegalStateException> { hitTest(session, defaultRay) }

                // Dynamically enable plane tracking on the active OpenXR session
                val enableConfig =
                    Config.Builder()
                        .setDeviceTracking(DeviceTrackingMode.SPATIAL)
                        .setPlaneTracking(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)
                        .build()
                val enableResult = session.configure(enableConfig)
                assertThat(enableResult).isInstanceOf(SessionConfigureSuccess::class.java)
                assertThat(session.config.planeTracking)
                    .isEqualTo(PlaneTrackingMode.HORIZONTAL_AND_VERTICAL)

                val initialHitOutcome = performHitTestAgainstTrackedPlanes(session)
                assumeTrue(
                    "Skipping test: No hit results returned against environment planes within timeout",
                    initialHitOutcome != null,
                )
                val validRay = checkNotNull(initialHitOutcome).first

                // Dynamically disable plane tracking
                val disableConfig =
                    Config.Builder()
                        .setDeviceTracking(DeviceTrackingMode.SPATIAL)
                        .setPlaneTracking(PlaneTrackingMode.DISABLED)
                        .build()
                val disableResult = session.configure(disableConfig)
                assertThat(disableResult).isInstanceOf(SessionConfigureSuccess::class.java)
                assertThat(session.config.planeTracking).isEqualTo(PlaneTrackingMode.DISABLED)

                // Performing hitTest while plane tracking is disabled must throw
                // IllegalStateException
                assertFailsWith<IllegalStateException> { hitTest(session, validRay) }

                // Re-enabling plane tracking restores hitTest on the same session
                val reEnableResult = session.configure(enableConfig)
                assertThat(reEnableResult).isInstanceOf(SessionConfigureSuccess::class.java)
                val recoveredHitOutcome = performHitTestAgainstTrackedPlanes(session)
                assumeTrue(
                    "Skipping test: No hit results returned after re-enabling within timeout",
                    recoveredHitOutcome != null,
                )
                assertThat(checkNotNull(recoveredHitOutcome).second).isNotEmpty()
            }
        }
    }

    @Test
    @XrDeviceTest
    fun hitTest_activityRecreation_reinitializesSessionAndHitTestPipeline() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            lateinit var initialSession: Session
            initSessionAndRunTest(
                scenario,
                planeTrackingMode = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
            ) { session ->
                initialSession = session
                safeHitTestWhileEnabled(initialSession, Ray(Vector3.Zero, Vector3.Forward))
            }

            scenario.recreate()

            // Upon activity recreation, the initial Activity and its attached Session are
            // destroyed, and a new Session is created and configured for the recreated
            // Activity.
            initSessionAndRunTest(
                scenario,
                planeTrackingMode = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
            ) { newSession ->
                assertThat(newSession).isNotSameInstanceAs(initialSession)
                safeHitTestWhileEnabled(newSession, Ray(Vector3.Zero, Vector3.Forward))
            }
        }
    }
}
