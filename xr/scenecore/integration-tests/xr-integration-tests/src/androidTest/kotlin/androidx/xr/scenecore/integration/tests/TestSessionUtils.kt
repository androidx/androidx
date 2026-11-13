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

package androidx.xr.scenecore.integration.tests

import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Scene
import androidx.xr.scenecore.SpatialCapability
import androidx.xr.scenecore.scene
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.util.function.Consumer
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Golden image directory path for SceneCore integration screenshot tests, used with
 * `XrScreenshotTestRule(GOLDEN_PATH)`.
 */
internal const val GOLDEN_PATH = "xr/scenecore/integration-tests/xr-integration-tests"

/**
 * Launches [ComponentActivity], initializes a [Session], and executes [testBody] on the main
 * thread, automatically closing the scene on completion.
 */
// TODO(b/583197288): Replace runTestWithSession with XrSessionRule.
internal inline fun runTestWithSession(
    crossinline testBody: suspend (ComponentActivity, Session) -> Unit
) = runBlocking {
    ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
        var activityRef: ComponentActivity? = null
        scenario.onActivity { activity -> activityRef = activity }
        val activity = checkNotNull(activityRef)

        withContext(Dispatchers.Main.immediate) {
            val sessionResult = Session.create(context = activity)
            assertThat(sessionResult).isInstanceOf(SessionCreateSuccess::class.java)
            val session = (sessionResult as SessionCreateSuccess).session
            session.scene.keyEntity = session.scene.mainPanelEntity

            try {
                testBody(activity, session)
            } finally {
                session.scene.close()
            }
        }
    }
}

/** Convenience overload of [runTestWithSession] when only the [Session] reference is needed. */
// TODO(b/583197288): Replace runTestWithSession with XrSessionRule.
internal inline fun runTestWithSession(crossinline testBody: suspend (Session) -> Unit) =
    runTestWithSession { _, session ->
        testBody(session)
    }

/**
 * Requests Full Space Mode via [Scene.requestFullSpace] and suspends until
 * [SpatialCapability.SPATIAL_UI] is present in [Scene.spatialCapabilities] and, when transitioning
 * from Home Space Mode, [Scene.activitySpace] has received its origin update.
 */
// TODO(b/584458287): Replace with XrSessionRule.requestFullSpace in XrSceneSessionExtensions.
internal suspend fun Scene.requestFullSpaceAndAwait() = requestSpaceModeAndAwait(fullSpace = true)

/**
 * Requests Home Space Mode via [Scene.requestHomeSpace] and suspends until
 * [SpatialCapability.SPATIAL_UI] is removed from [Scene.spatialCapabilities].
 */
// TODO(b/584458287): Replace with XrSessionRule.requestHomeSpace in XrSceneSessionExtensions.
internal suspend fun Scene.requestHomeSpaceAndAwait() = requestSpaceModeAndAwait(fullSpace = false)

private suspend fun Scene.requestSpaceModeAndAwait(fullSpace: Boolean) {
    if (spatialCapabilities.contains(SpatialCapability.SPATIAL_UI) == fullSpace) return

    val reached = CompletableDeferred<Unit>()
    val listener =
        Consumer<Set<SpatialCapability>> { capabilities ->
            if (capabilities.contains(SpatialCapability.SPATIAL_UI) == fullSpace) {
                reached.complete(Unit)
            }
        }
    // Await the initial origin update from the OS when transitioning HSM -> FSM to ensure
    // ActivitySpace has been unscaled and gravity-aligned.
    val originSettled = CompletableDeferred<Unit>()
    val originListener = Runnable { originSettled.complete(Unit) }

    addSpatialCapabilitiesChangedListener(listener)
    if (fullSpace) {
        activitySpace.addOriginChangedListener(originListener)
    }
    try {
        if (spatialCapabilities.contains(SpatialCapability.SPATIAL_UI) != fullSpace) {
            if (fullSpace) requestFullSpace() else requestHomeSpace()
            withTimeout(SPACE_MODE_TIMEOUT) { reached.await() }
            if (fullSpace) {
                withTimeoutOrNull(2.seconds) { originSettled.await() }
            }
        }
    } finally {
        if (fullSpace) {
            activitySpace.removeOriginChangedListener(originListener)
        }
        removeSpatialCapabilitiesChangedListener(listener)
    }
}

internal fun assertPose(actual: Pose, expected: Pose, epsilon: Float = 1e-5f) {
    assertVector3(actual.translation, expected.translation, epsilon)
    assertFiniteQuaternion(actual.rotation, "actual.rotation")
    assertFiniteQuaternion(expected.rotation, "expected.rotation")

    val actualNorm = quaternionNorm(actual.rotation)
    val expectedNorm = quaternionNorm(expected.rotation)
    assertWithMessage("actual.rotation norm ($actualNorm) must be positive")
        .that(actualNorm)
        .isGreaterThan(1e-6f)
    assertWithMessage("expected.rotation norm ($expectedNorm) must be positive")
        .that(expectedNorm)
        .isGreaterThan(1e-6f)

    val ax = actual.rotation.x / actualNorm
    val ay = actual.rotation.y / actualNorm
    val az = actual.rotation.z / actualNorm
    val aw = actual.rotation.w / actualNorm

    val ex = expected.rotation.x / expectedNorm
    val ey = expected.rotation.y / expectedNorm
    val ez = expected.rotation.z / expectedNorm
    val ew = expected.rotation.w / expectedNorm

    val dot = ax * ex + ay * ey + az * ez + aw * ew
    val sign = if (dot < 0f) -1f else 1f
    assertThat(ax * sign).isWithin(epsilon).of(ex)
    assertThat(ay * sign).isWithin(epsilon).of(ey)
    assertThat(az * sign).isWithin(epsilon).of(ez)
    assertThat(aw * sign).isWithin(epsilon).of(ew)
}

internal fun assertVector3(actual: Vector3, expected: Vector3, epsilon: Float = 1e-5f) {
    assertFiniteVector3(actual, "actual")
    assertFiniteVector3(expected, "expected")
    assertThat(actual.x).isWithin(epsilon).of(expected.x)
    assertThat(actual.y).isWithin(epsilon).of(expected.y)
    assertThat(actual.z).isWithin(epsilon).of(expected.z)
}

private fun assertFiniteVector3(vector: Vector3, label: String) {
    assertWithMessage("$label.x (${vector.x}) must be finite").that(vector.x.isFinite()).isTrue()
    assertWithMessage("$label.y (${vector.y}) must be finite").that(vector.y.isFinite()).isTrue()
    assertWithMessage("$label.z (${vector.z}) must be finite").that(vector.z.isFinite()).isTrue()
}

private fun assertFiniteQuaternion(rotation: Quaternion, label: String) {
    assertWithMessage("$label.x (${rotation.x}) must be finite")
        .that(rotation.x.isFinite())
        .isTrue()
    assertWithMessage("$label.y (${rotation.y}) must be finite")
        .that(rotation.y.isFinite())
        .isTrue()
    assertWithMessage("$label.z (${rotation.z}) must be finite")
        .that(rotation.z.isFinite())
        .isTrue()
    assertWithMessage("$label.w (${rotation.w}) must be finite")
        .that(rotation.w.isFinite())
        .isTrue()
}

private fun quaternionNorm(rotation: Quaternion): Float =
    sqrt(
        rotation.x * rotation.x +
            rotation.y * rotation.y +
            rotation.z * rotation.z +
            rotation.w * rotation.w
    )
