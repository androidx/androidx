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
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Scene
import androidx.xr.scenecore.SpatialCapability
import androidx.xr.scenecore.scene
import com.google.common.truth.Truth.assertThat
import java.util.function.Consumer
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

/** Timeout applied when waiting for a space mode transition to be observed. */
private val SPACE_MODE_TIMEOUT = 5.seconds

/**
 * Launches [ComponentActivity], initializes a [Session], and executes [testBody], closing the scene
 * on completion.
 */
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
internal inline fun runTestWithSession(crossinline testBody: suspend (Session) -> Unit) =
    runTestWithSession { _, session ->
        testBody(session)
    }

/**
 * Requests Full Space Mode via [Scene.requestFullSpace] and suspends until
 * [SpatialCapability.SPATIAL_UI] is present in [Scene.spatialCapabilities] and, when transitioning
 * from Home Space Mode, [Scene.activitySpace] has received its origin update.
 */
internal suspend fun Scene.requestFullSpaceAndAwait() = requestSpaceModeAndAwait(fullSpace = true)

/**
 * Requests Home Space Mode via [Scene.requestHomeSpace] and suspends until
 * [SpatialCapability.SPATIAL_UI] is removed from [Scene.spatialCapabilities].
 */
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
    assertThat(actual.rotation.x).isWithin(epsilon).of(expected.rotation.x)
    assertThat(actual.rotation.y).isWithin(epsilon).of(expected.rotation.y)
    assertThat(actual.rotation.z).isWithin(epsilon).of(expected.rotation.z)
    assertThat(actual.rotation.w).isWithin(epsilon).of(expected.rotation.w)
}

internal fun assertVector3(actual: Vector3, expected: Vector3, epsilon: Float = 1e-5f) {
    assertThat(actual.x).isWithin(epsilon).of(expected.x)
    assertThat(actual.y).isWithin(epsilon).of(expected.y)
    assertThat(actual.z).isWithin(epsilon).of(expected.z)
}
