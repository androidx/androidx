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

import android.content.Context
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.xr.arcore.Anchor
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
import androidx.xr.runtime.SessionCreateResult
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.SessionCreateTimedOut
import androidx.xr.runtime.SessionCreateUnknownError
import androidx.xr.runtime.SessionCreateUnsupportedDevice
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Ray
import androidx.xr.runtime.math.Vector3
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.fail
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.AssumptionViolatedException

internal const val PERCEPTION_TIMEOUT_MS = 5_000L
internal const val PLANE_DISCOVERY_TIMEOUT_MS = 10_000L
internal const val HIT_RETRY_DELAY_MS = 100L
internal const val MIN_RAY_DISTANCE_METERS = 1e-3f
internal const val CENTER_TRANSLATION_STABILITY_EPSILON = 5e-3f
internal const val CENTER_ROTATION_DOT_STABILITY_EPSILON = 1e-3f
internal const val MAX_ANCHOR_STABILITY_ATTEMPTS = 3
internal const val POSE_EPSILON = 1e-2f
internal const val QUATERNION_NORM_EPSILON = 1e-3f

internal data class PlaneHitMatch(
    val ray: Ray,
    val hitResults: List<HitResult>,
    val hit: HitResult,
    val plane: Plane,
)

internal fun runWithActivity(
    scenario: ActivityScenario<ComponentActivity>,
    testBody: suspend (ComponentActivity) -> Unit,
) {
    scenario.onActivity { activity -> runBlocking { testBody(activity) } }
}

internal suspend fun createSessionResultOrSkip(context: Context): SessionCreateResult {
    val result = Session.create(context = context)
    when (result) {
        is SessionCreateApkRequired ->
            throw AssumptionViolatedException(
                "Skipping test: Missing or outdated APK '${result.requiredApk}'"
            )
        is SessionCreateUnsupportedDevice ->
            throw AssumptionViolatedException(
                "Skipping test: Device does not support required XR features"
            )
        else -> return result
    }
}

internal fun initSessionAndRunTest(
    scenario: ActivityScenario<ComponentActivity>,
    testBody: suspend (Session) -> Unit,
) {
    runWithActivity(scenario) { activity ->
        val result = createSessionResultOrSkip(activity)
        check(result is SessionCreateSuccess) {
            when (result) {
                is SessionCreateUnknownError -> "SessionCreateUnknownError: ${result.errorMessage}"
                is SessionCreateTimedOut -> "SessionCreateTimedOut"
                else -> "Session.create failed with result: $result"
            }
        }
        testBody(result.session)
    }
}

internal fun initPlaneTrackingSessionAndRunTest(
    scenario: ActivityScenario<ComponentActivity>,
    planeTrackingMode: PlaneTrackingMode = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL,
    testBody: suspend (Session) -> Unit,
) {
    initSessionAndRunTest(scenario) { session ->
        val config =
            Config.Builder()
                .setDeviceTracking(DeviceTrackingMode.SPATIAL)
                .setPlaneTracking(planeTrackingMode)
                .build()
        val configResult = session.configure(config)
        check(configResult is SessionConfigureSuccess) {
            "Failed to configure Session with $planeTrackingMode: $configResult"
        }

        val state =
            withTimeoutOrNull(PERCEPTION_TIMEOUT_MS) {
                session.state.first { it.perceptionState != null }
            }
        checkNotNull(state) { "Timed out waiting for perception state" }

        testBody(session)
    }
}

internal fun trackedDevicePoseOrNull(arDevice: ArDevice): Pose? {
    val deviceState = arDevice.state.value
    if (
        deviceState.trackingState == TrackingState.TRACKING && isFinitePose(deviceState.devicePose)
    ) {
        return deviceState.devicePose
    }
    return null
}

internal fun sortTrackedPlanesForHitTest(
    planes: Collection<Plane>,
    referenceOrigin: Vector3?,
): List<Plane> {
    val tracked = planes.filter {
        val state = it.state.value
        state.trackingState == TrackingState.TRACKING && state.subsumedBy == null
    }
    val baseComparator = compareBy<Plane> { it.state.value.label == PlaneLabel.UNKNOWN }
    val comparator =
        if (referenceOrigin != null) {
            baseComparator.thenBy {
                (it.state.value.centerPose.translation - referenceOrigin).length
            }
        } else {
            baseComparator
        }
    return tracked.sortedWith(comparator)
}

internal fun buildCandidateRaysForPlane(plane: Plane, deviceOrigin: Vector3?): List<Ray> {
    val centerPose = plane.state.value.centerPose
    return buildList {
        // 1. Cast ray from the tracked AR device pose directly toward centerPose (matches
        // real-world head/controller raycasting and avoids collinearity with the plane normal).
        if (deviceOrigin != null) {
            val deviceToPlane = centerPose.translation - deviceOrigin
            if (deviceToPlane.length > MIN_RAY_DISTANCE_METERS) {
                add(Ray(origin = deviceOrigin, direction = deviceToPlane.toNormalized()))
            }
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

internal fun matchPlaneHit(
    ray: Ray,
    hitResults: List<HitResult>,
    aimedPlane: Plane? = null,
): PlaneHitMatch? = hitResults.firstNotNullOfOrNull { candidate ->
    val hitPlane = candidate.trackable as? Plane ?: return@firstNotNullOfOrNull null
    val planeState = hitPlane.state.value
    if (
        (aimedPlane == null || hitPlane == aimedPlane) &&
            planeState.trackingState == TrackingState.TRACKING &&
            planeState.subsumedBy == null &&
            isFinitePose(candidate.hitPose)
    ) {
        PlaneHitMatch(ray = ray, hitResults = hitResults, hit = candidate, plane = hitPlane)
    } else {
        null
    }
}

internal fun findCandidateHitOnPlanes(
    session: Session,
    planes: List<Plane>,
    deviceOrigin: Vector3?,
): PlaneHitMatch? {
    for (plane in planes) {
        for (ray in buildCandidateRaysForPlane(plane, deviceOrigin)) {
            val match = matchPlaneHit(ray, hitTest(session, ray), aimedPlane = plane)
            if (match != null) {
                return match
            }
        }
    }
    return null
}

internal fun tryFindTrackedPlaneHit(
    session: Session,
    planesFlow: StateFlow<Collection<Plane>>,
    arDevice: ArDevice,
): PlaneHitMatch? {
    val deviceOrigin = trackedDevicePoseOrNull(arDevice)?.translation
    val trackedPlanes = sortTrackedPlanesForHitTest(planesFlow.value, deviceOrigin)
    return findCandidateHitOnPlanes(session, trackedPlanes, deviceOrigin)
}

// TODO: b/575892628 - Migrate live-trackable timeouts to deterministic synthetic planes once
// perception simulation support is available.
internal suspend fun findTrackedPlaneHit(
    session: Session,
    planesFlow: StateFlow<Collection<Plane>> = Plane.subscribe(session),
    timeoutMs: Long = PLANE_DISCOVERY_TIMEOUT_MS,
): PlaneHitMatch? {
    val arDevice = ArDevice.getInstance(session)
    return withTimeoutOrNull(timeoutMs) {
        while (isActive) {
            val match = tryFindTrackedPlaneHit(session, planesFlow, arDevice)
            if (match != null) {
                return@withTimeoutOrNull match
            }
            delay(HIT_RETRY_DELAY_MS)
        }
        null
    }
}

internal fun isFinitePose(pose: Pose): Boolean =
    pose.translation.x.isFinite() &&
        pose.translation.y.isFinite() &&
        pose.translation.z.isFinite() &&
        pose.rotation.x.isFinite() &&
        pose.rotation.y.isFinite() &&
        pose.rotation.z.isFinite() &&
        pose.rotation.w.isFinite()

private fun quaternionDot(a: Quaternion, b: Quaternion): Float =
    a.x * b.x + a.y * b.y + a.z * b.z + a.w * b.w

internal fun isPoseStable(actual: Pose, expected: Pose): Boolean {
    val translationStable =
        abs(actual.translation.x - expected.translation.x) <=
            CENTER_TRANSLATION_STABILITY_EPSILON &&
            abs(actual.translation.y - expected.translation.y) <=
                CENTER_TRANSLATION_STABILITY_EPSILON &&
            abs(actual.translation.z - expected.translation.z) <=
                CENTER_TRANSLATION_STABILITY_EPSILON
    val rotationStable =
        abs(1.0f - abs(quaternionDot(actual.rotation, expected.rotation))) <=
            CENTER_ROTATION_DOT_STABILITY_EPSILON
    return translationStable && rotationStable
}

internal suspend fun awaitTrackingAnchorState(anchor: Anchor): Anchor.State {
    val trackingState =
        withTimeoutOrNull(PERCEPTION_TIMEOUT_MS) {
            anchor.state.first { it.trackingState == TrackingState.TRACKING }
        }
    return checkNotNull(trackingState) {
        "Timed out waiting for anchor to enter TRACKING state"
    }
}

internal suspend fun isPlaneCenterStableAcrossTick(plane: Plane): Boolean {
    val initialState = plane.state.value
    if (
        initialState.trackingState == TrackingState.TRACKING &&
            initialState.subsumedBy == null &&
            isFinitePose(initialState.centerPose)
    ) {
        delay(HIT_RETRY_DELAY_MS)
        val settledState = plane.state.value
        return settledState.trackingState == TrackingState.TRACKING &&
            settledState.subsumedBy == null &&
            isFinitePose(settledState.centerPose) &&
            isPoseStable(settledState.centerPose, initialState.centerPose)
    }
    return false
}

internal suspend fun awaitStabilizedPlaneHit(
    initialTarget: PlaneHitMatch?,
    findTarget: suspend () -> PlaneHitMatch?,
): PlaneHitMatch {
    var pendingTarget = initialTarget
    var sawTarget = false
    val deadlineMs = SystemClock.elapsedRealtime() + PERCEPTION_TIMEOUT_MS
    while (true) {
        val target = pendingTarget ?: findTarget()
        pendingTarget = null
        if (target != null) {
            sawTarget = true
            if (isPlaneCenterStableAcrossTick(target.plane)) {
                return target
            }
        }
        if (SystemClock.elapsedRealtime() >= deadlineMs) {
            val reason =
                if (sawTarget) {
                    "Plane center did not stabilize within ${PERCEPTION_TIMEOUT_MS}ms"
                } else {
                    "No tracked plane hit remained available within ${PERCEPTION_TIMEOUT_MS}ms"
                }
            fail(reason)
        }
        delay(HIT_RETRY_DELAY_MS)
    }
}

/**
 * Waits for a tracked plane hit with a stable center before creating anchors, then runs
 * [attemptBlock] up to [maxAttempts] times if the plane center shifts while anchoring.
 */
internal suspend fun <T : Any> runWithStabilizedPlaneHit(
    initialTarget: PlaneHitMatch?,
    maxAttempts: Int = MAX_ANCHOR_STABILITY_ATTEMPTS,
    findTarget: suspend () -> PlaneHitMatch?,
    attemptBlock: suspend (PlaneHitMatch) -> T?,
): T {
    var pendingTarget = initialTarget
    for (attempt in 1..maxAttempts) {
        val stabilizedTarget = awaitStabilizedPlaneHit(pendingTarget, findTarget)
        pendingTarget = null
        attemptBlock(stabilizedTarget)?.let {
            return it
        }
        if (attempt < maxAttempts) {
            delay(HIT_RETRY_DELAY_MS)
        }
    }
    fail("Plane center shifted during anchor tracking after $maxAttempts attempts")
}

/**
 * Executes [block] and guarantees [anchors] are detached afterward, attaching detach failures to
 * any thrown [block] exception or throwing the first detach failure if [block] succeeded.
 */
internal inline fun <T> withAnchorsDetached(vararg anchors: Anchor, block: () -> T): T {
    var primaryFailure: Throwable? = null
    return try {
        block()
    } catch (t: Throwable) {
        primaryFailure = t
        throw t
    } finally {
        detachAnchorsCleanly(primaryFailure, *anchors)
    }
}

/**
 * Detaches every anchor in [anchors], attaching any detach exception onto [primaryFailure] as
 * suppressed or throwing the first detach exception if [primaryFailure] is `null`.
 */
internal fun detachAnchorsCleanly(primaryFailure: Throwable?, vararg anchors: Anchor) {
    var accumulatedFailure = primaryFailure
    for (anchor in anchors) {
        try {
            anchor.detach()
        } catch (detachFailure: Throwable) {
            accumulatedFailure =
                if (accumulatedFailure != null) {
                    accumulatedFailure.also { it.addSuppressed(detachFailure) }
                } else {
                    detachFailure
                }
        }
    }
    if (primaryFailure == null && accumulatedFailure != null) {
        throw accumulatedFailure
    }
}

internal fun assertValidQuaternion(q: Quaternion, epsilon: Float = QUATERNION_NORM_EPSILON) {
    assertThat(q.x).isFinite()
    assertThat(q.y).isFinite()
    assertThat(q.z).isFinite()
    assertThat(q.w).isFinite()
    val norm = sqrt(q.x * q.x + q.y * q.y + q.z * q.z + q.w * q.w)
    assertThat(norm).isWithin(epsilon).of(1.0f)
}

internal fun assertPoseEquals(
    actual: Pose,
    expected: Pose,
    translationEpsilon: Float = POSE_EPSILON,
    rotationDotEpsilon: Float = CENTER_ROTATION_DOT_STABILITY_EPSILON,
) {
    assertThat(actual.translation.x).isWithin(translationEpsilon).of(expected.translation.x)
    assertThat(actual.translation.y).isWithin(translationEpsilon).of(expected.translation.y)
    assertThat(actual.translation.z).isWithin(translationEpsilon).of(expected.translation.z)
    assertThat(abs(quaternionDot(actual.rotation, expected.rotation)))
        .isWithin(rotationDotEpsilon)
        .of(1.0f)
}
