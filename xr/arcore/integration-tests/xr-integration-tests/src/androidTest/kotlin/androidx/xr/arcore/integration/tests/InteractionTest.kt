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
import androidx.xr.arcore.hitTest
import androidx.xr.runtime.Config
import androidx.xr.runtime.DeviceTrackingMode
import androidx.xr.runtime.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionConfigureSuccess
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import androidx.xr.testutils.XrDeviceTest
import androidx.xr.testutils.filterSupportedPermissions
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.assertFailsWith
import kotlinx.coroutines.flow.StateFlow
import org.junit.AssumptionViolatedException
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end XR device integration tests for JXR ARCore Interaction ([hitTest] and single or
 * multiple [Anchor] placement on tracked planes at [HitResult.hitPose], OpenXR ray-plane
 * intersection geometry, dynamic runtime reconfiguration, and Activity recreation).
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

    private data class AnchorPair(val primary: Anchor, val secondary: Anchor)

    private data class AnchorPairSpec(
        val target: PlaneHitMatch,
        val centerBeforeCreate: Pose,
        val primaryLocalPose: Pose,
        val secondaryLocalPose: Pose,
    )

    private data class MultiAnchorPlaneResult(
        val primaryTrackingState: Anchor.State,
        val secondaryTrackingState: Anchor.State,
        val expectedPrimaryWorldPose: Pose,
        val expectedSecondaryWorldPose: Pose,
    )

    private fun createAnchorPair(
        plane: Plane,
        primaryLocalPose: Pose,
        secondaryLocalPose: Pose,
    ): AnchorPair {
        val primaryResult = plane.createAnchor(primaryLocalPose)
        check(primaryResult is AnchorCreateSuccess) {
            "Expected AnchorCreateSuccess for primary anchor, got $primaryResult"
        }
        val primaryAnchor = primaryResult.anchor
        try {
            val secondaryResult = plane.createAnchor(secondaryLocalPose)
            check(secondaryResult is AnchorCreateSuccess) {
                "Expected AnchorCreateSuccess for secondary anchor, got $secondaryResult"
            }
            return AnchorPair(primaryAnchor, secondaryResult.anchor)
        } catch (t: Throwable) {
            detachAnchorsCleanly(t, primaryAnchor)
            throw t
        }
    }

    private suspend fun findAnchorablePlaneTarget(
        session: Session,
        planesFlow: StateFlow<Collection<Plane>>,
    ): PlaneHitMatch =
        findTrackedPlaneHit(session, planesFlow)
            ?: throw AssumptionViolatedException(
                "Skipping test: No hit results returned against tracked planes within timeout"
            )

    private fun buildAnchorPairSpec(target: PlaneHitMatch): AnchorPairSpec {
        val centerBeforeCreate = target.plane.state.value.centerPose
        val primaryLocalPose = centerBeforeCreate.inverse.compose(target.hit.hitPose)
        val secondaryLocalPose =
            Pose(
                primaryLocalPose.translation + Vector3(SECONDARY_ANCHOR_OFFSET_METERS, 0f, 0f),
                primaryLocalPose.rotation,
            )
        return AnchorPairSpec(
            target = target,
            centerBeforeCreate = centerBeforeCreate,
            primaryLocalPose = primaryLocalPose,
            secondaryLocalPose = secondaryLocalPose,
        )
    }

    private fun areAnchorEndpointsStable(
        spec: AnchorPairSpec,
        centerAfterTracking: Pose,
    ): Boolean =
        isPoseStable(centerAfterTracking, spec.centerBeforeCreate) &&
            isPoseStable(
                centerAfterTracking.compose(spec.primaryLocalPose),
                spec.target.hit.hitPose,
            ) &&
            isPoseStable(
                centerAfterTracking.compose(spec.secondaryLocalPose),
                spec.centerBeforeCreate.compose(spec.secondaryLocalPose),
            )

    private suspend fun awaitStableAnchorPair(
        spec: AnchorPairSpec,
        anchorPair: AnchorPair,
    ): MultiAnchorPlaneResult? =
        withAnchorsDetached(anchorPair.primary, anchorPair.secondary) {
            val primaryTrackingState = awaitTrackingAnchorState(anchorPair.primary)
            val secondaryTrackingState = awaitTrackingAnchorState(anchorPair.secondary)
            val centerAfterTracking = spec.target.plane.state.value.centerPose
            if (areAnchorEndpointsStable(spec, centerAfterTracking)) {
                MultiAnchorPlaneResult(
                    primaryTrackingState = primaryTrackingState,
                    secondaryTrackingState = secondaryTrackingState,
                    expectedPrimaryWorldPose = spec.target.hit.hitPose,
                    expectedSecondaryWorldPose =
                        spec.centerBeforeCreate.compose(spec.secondaryLocalPose),
                )
            } else {
                null
            }
        }

    private suspend fun tryCreateStableAnchorPair(spec: AnchorPairSpec): MultiAnchorPlaneResult? {
        val anchorPair =
            createAnchorPair(spec.target.plane, spec.primaryLocalPose, spec.secondaryLocalPose)
        return awaitStableAnchorPair(spec, anchorPair)
    }

    private suspend fun createStableMultiAnchorTarget(session: Session): MultiAnchorPlaneResult {
        val planesFlow = Plane.subscribe(session)
        val initialTarget = findAnchorablePlaneTarget(session, planesFlow)
        val arDevice = ArDevice.getInstance(session)
        return runWithStabilizedPlaneHit(
            initialTarget = initialTarget,
            findTarget = { tryFindTrackedPlaneHit(session, planesFlow, arDevice) },
        ) { stabilizedTarget ->
            tryCreateStableAnchorPair(buildAnchorPairSpec(stabilizedTarget))
        }
    }

    private fun assertValidPlaneHitGeometry(ray: Ray, hitResult: HitResult) {
        assertThat(hitResult.hitPose.translation.x).isFinite()
        assertThat(hitResult.hitPose.translation.y).isFinite()
        assertThat(hitResult.hitPose.translation.z).isFinite()
        assertValidQuaternion(hitResult.hitPose.rotation)

        // Verify HitResult.hitPose lies on the cast ray at HitResult.distance from ray.origin
        val expectedPositionOnRay = ray.origin + ray.direction.toNormalized() * hitResult.distance
        assertThat((hitResult.hitPose.translation - expectedPositionOnRay).length)
            .isLessThan(RAY_PROJECTION_EPSILON_METERS)
        val expectedDistance = (hitResult.hitPose.translation - ray.origin).length
        assertThat(hitResult.distance).isWithin(RAY_PROJECTION_EPSILON_METERS).of(expectedDistance)

        // Plane-only config must return a Plane hitPose aligned and coplanar with centerPose
        assertThat(hitResult.trackable).isInstanceOf(Plane::class.java)
        val planeState = (hitResult.trackable as Plane).state.value
        assertThat(hitResult.hitPose.up dot planeState.centerPose.up)
            .isAtLeast(MIN_PLANE_NORMAL_ALIGNMENT_DOT)
        val planeSurfaceOffset =
            abs(
                (hitResult.hitPose.translation - planeState.centerPose.translation) dot
                    planeState.centerPose.up
            )
        assertThat(planeSurfaceOffset).isAtMost(MAX_PLANE_SURFACE_OFFSET_METERS)
    }

    @Test
    @XrDeviceTest
    fun hitTest_rayAgainstTrackedPlane_validatesHitResultGeometryAndSorting() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            initPlaneTrackingSessionAndRunTest(
                scenario,
                planeTrackingMode = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
            ) { session ->
                val matchedHit =
                    findTrackedPlaneHit(session)
                        ?: throw AssumptionViolatedException(
                            "Skipping test: No hit results returned against environment planes " +
                                "within timeout"
                        )
                val ray = matchedHit.ray
                val hitResults = matchedHit.hitResults
                assertThat(hitResults).isNotEmpty()

                var previousDistance = 0f
                for (hitResult in hitResults) {
                    assertThat(hitResult.distance).isFinite()
                    assertThat(hitResult.distance).isGreaterThan(0f)
                    assertThat(hitResult.distance).isAtLeast(previousDistance)
                    previousDistance = hitResult.distance

                    assertValidPlaneHitGeometry(ray, hitResult)
                }
            }
        }
    }

    @Test
    @XrDeviceTest
    fun hitTest_createAnchorAtHitPose_tracksWorldPoseAndDetachesCleanly() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            initPlaneTrackingSessionAndRunTest(
                scenario,
                planeTrackingMode = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
            ) { session ->
                val hitMatch =
                    findTrackedPlaneHit(session)
                        ?: throw AssumptionViolatedException(
                            "Skipping test: No hit results returned against environment planes " +
                                "within timeout"
                        )

                val anchorResult = Anchor.create(session, hitMatch.hit.hitPose)
                assertWithMessage(
                        "Expected AnchorCreateSuccess at hitPose=${hitMatch.hit.hitPose}, " +
                            "got $anchorResult"
                    )
                    .that(anchorResult)
                    .isInstanceOf(AnchorCreateSuccess::class.java)
                val anchor = (anchorResult as AnchorCreateSuccess).anchor
                withAnchorsDetached(anchor) {
                    val trackingAnchorState = awaitTrackingAnchorState(anchor)

                    assertPoseEquals(trackingAnchorState.pose, hitMatch.hit.hitPose)
                    assertThat(
                            trackingAnchorState.pose.up dot hitMatch.plane.state.value.centerPose.up
                        )
                        .isAtLeast(MIN_PLANE_NORMAL_ALIGNMENT_DOT)
                }
            }
        }
    }

    @Test
    @XrDeviceTest
    fun hitTest_multipleAnchorsOnTrackedPlane_trackIndependentWorldPoses() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            initPlaneTrackingSessionAndRunTest(scenario) { session ->
                val result = createStableMultiAnchorTarget(session)

                assertPoseEquals(
                    result.primaryTrackingState.pose,
                    result.expectedPrimaryWorldPose,
                )
                assertPoseEquals(
                    result.secondaryTrackingState.pose,
                    result.expectedSecondaryWorldPose,
                )
                assertThat(
                        Vector3.distance(
                            result.primaryTrackingState.pose.translation,
                            result.secondaryTrackingState.pose.translation,
                        )
                    )
                    .isWithin(2f * sqrt(3f) * POSE_EPSILON)
                    .of(SECONDARY_ANCHOR_OFFSET_METERS)
            }
        }
    }

    @Test
    @XrDeviceTest
    fun hitTest_dynamicModeSwitching_throwsWhenPlaneTrackingDisabledAndRecovers() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            initPlaneTrackingSessionAndRunTest(
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

                val planesFlow = Plane.subscribe(session)
                val initialHitMatch =
                    findTrackedPlaneHit(session, planesFlow)
                        ?: throw AssumptionViolatedException(
                            "Skipping test: No hit results returned against environment planes " +
                                "within timeout"
                        )
                val validRay = initialHitMatch.ray

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
                val recoveredHitMatch = findTrackedPlaneHit(session, planesFlow)

                assertWithMessage("hitTest did not recover after re-enabling plane tracking")
                    .that(recoveredHitMatch)
                    .isNotNull()
            }
        }
    }

    @Test
    @XrDeviceTest
    fun hitTest_activityRecreation_reinitializesSessionAndHitTestPipeline() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            initPlaneTrackingSessionAndRunTest(
                scenario,
                planeTrackingMode = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
            ) { session ->
                hitTest(session, Ray(Vector3.Zero, Vector3.Forward))
            }

            scenario.recreate()

            // Upon activity recreation, the initial Activity and its attached Session are
            // destroyed, and a new Session is created and configured for the recreated
            // Activity.
            initPlaneTrackingSessionAndRunTest(
                scenario,
                planeTrackingMode = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
            ) { newSession ->
                val matchedHit =
                    findTrackedPlaneHit(newSession)
                        ?: throw AssumptionViolatedException(
                            "Skipping test: No hit results returned against environment planes " +
                                "after Activity recreation"
                        )

                assertValidPlaneHitGeometry(matchedHit.ray, matchedHit.hit)
            }
        }
    }

    private companion object {
        const val SECONDARY_ANCHOR_OFFSET_METERS = 0.25f
        const val MIN_PLANE_NORMAL_ALIGNMENT_DOT = 0.99f
        const val RAY_PROJECTION_EPSILON_METERS = 1e-3f
        const val MAX_PLANE_SURFACE_OFFSET_METERS = 2e-2f
    }
}
