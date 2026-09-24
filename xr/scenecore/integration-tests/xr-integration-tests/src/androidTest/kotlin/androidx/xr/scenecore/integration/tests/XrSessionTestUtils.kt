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
import androidx.test.platform.app.InstrumentationRegistry
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.scenecore.Scene
import androidx.xr.scenecore.SpatialCapability
import androidx.xr.scenecore.scene
import com.google.common.truth.Truth.assertWithMessage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import kotlinx.coroutines.runBlocking

/** Timeout applied when waiting for a space mode transition to be observed. */
private const val SPACE_MODE_TIMEOUT_SECONDS = 5L

/**
 * Creates an XR [Session] for [activity].
 *
 * Must be called on the main thread, for example from within [ActivityScenario.onActivity].
 */
fun createXrSession(activity: ComponentActivity): Session {
    val sessionResult = runBlocking { Session.create(context = activity) }
    check(sessionResult is SessionCreateSuccess) { "Failed to create XR session" }
    return sessionResult.session
}

/**
 * Requests full space mode and blocks until [SpatialCapability.SPATIAL_UI] has been granted.
 *
 * Fails the test if the transition is not observed within [SPACE_MODE_TIMEOUT_SECONDS].
 */
fun ActivityScenario<*>.requestAndAwaitFullSpace(session: Session) {
    awaitSpaceModeTransition(session, "full space", expectSpatialUi = true) {
        it.requestFullSpace()
    }
}

/**
 * Requests home space mode and blocks until [SpatialCapability.SPATIAL_UI] has been revoked.
 *
 * Fails the test if the transition is not observed within [SPACE_MODE_TIMEOUT_SECONDS].
 */
fun ActivityScenario<*>.requestAndAwaitHomeSpace(session: Session) {
    awaitSpaceModeTransition(session, "home space", expectSpatialUi = false) {
        it.requestHomeSpace()
    }
}

/**
 * Requests a space mode transition and blocks until [SpatialCapability.SPATIAL_UI] matches
 * [expectSpatialUi].
 *
 * This must be called from the instrumentation thread, and NOT from inside
 * [ActivityScenario.onActivity]. The capability listener is dispatched on the main thread executor,
 * so awaiting on the main thread would stall the Looper and the callback would never be delivered.
 *
 * Note that the spatial capabilities and the spatial mode change callbacks are two independent
 * paths with no ordering guarantee between them. Waiting for [SpatialCapability.SPATIAL_UI]
 * therefore does not by itself guarantee that the recommended pose and scale of the new space mode
 * have already been applied to the key entity. This function drains the main looper before
 * returning, which covers the callbacks that are already queued.
 *
 * @param spaceModeName the name of the requested space mode, used in the timeout message.
 * @param expectSpatialUi whether [SpatialCapability.SPATIAL_UI] is expected to be granted once the
 *   transition has completed.
 * @param requestSpaceMode requests the space mode transition. Invoked on the main thread, and only
 *   if the scene is not already in the requested space mode.
 */
private fun ActivityScenario<*>.awaitSpaceModeTransition(
    session: Session,
    spaceModeName: String,
    expectSpatialUi: Boolean,
    requestSpaceMode: (Scene) -> Unit,
) {
    val latch = CountDownLatch(1)
    val listener =
        Consumer<Set<SpatialCapability>> { capabilities ->
            if (capabilities.contains(SpatialCapability.SPATIAL_UI) == expectSpatialUi) {
                latch.countDown()
            }
        }

    onActivity {
        session.scene.addSpatialCapabilitiesChangedListener(listener)
        if (
            session.scene.spatialCapabilities.contains(SpatialCapability.SPATIAL_UI) ==
                expectSpatialUi
        ) {
            latch.countDown()
        } else {
            requestSpaceMode(session.scene)
        }
    }

    val transitioned = latch.await(SPACE_MODE_TIMEOUT_SECONDS, TimeUnit.SECONDS)

    onActivity { session.scene.removeSpatialCapabilitiesChangedListener(listener) }

    assertWithMessage("Timed out waiting for the transition to $spaceModeName mode")
        .that(transitioned)
        .isTrue()

    // Let the queued spatial mode change callbacks (e.g. the recommended pose and scale for the
    // new space mode) be applied before the caller continues.
    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
}
