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

package androidx.xr.scenecore.testapp

import androidx.test.core.app.ActivityScenario
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.scenecore.scene
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Launches [EmptyActivity], initializes a [Session], awaits the OS ActivitySpace gravity-alignment
 * and unscaling origin update, and executes [testBody].
 *
 * ### ActivitySpace Origin Settlement
 * When an Activity launches in XR, SpaceFlinger initially attaches the Activity under the OS
 * WindowManager task leash with an uncompensated scale factor (~1.75x on standard display
 * densities). Awaiting [androidx.xr.scenecore.ActivitySpace.addOriginChangedListener] ensures that
 * `ActivitySpaceImpl` has received the platform's reference space matrix and applied the unscaling
 * and gravity-alignment node transaction before test assertions run.
 */
internal inline fun runTestWithSession(
    crossinline testBody: suspend (EmptyActivity, Session) -> Unit
) = runBlocking {
    ActivityScenario.launch(EmptyActivity::class.java).use { scenario ->
        var activityRef: EmptyActivity? = null
        scenario.onActivity { activity -> activityRef = activity }
        val activity = checkNotNull(activityRef)

        withContext(Dispatchers.Main.immediate) {
            val sessionResult = Session.create(context = activity)
            assertThat(sessionResult).isInstanceOf(SessionCreateSuccess::class.java)
            val session = (sessionResult as SessionCreateSuccess).session
            session.scene.keyEntity = session.scene.mainPanelEntity

            // Await the initial origin update from the OS to ensure ActivitySpace has been
            // unscaled and gravity-aligned. Otherwise, SpaceFlinger CPM initially reflects the
            // uncompensated ~1.75x WindowManager task scale factor until ActivitySpaceImpl
            // inverts the inherited scale.
            val originSettled = CompletableDeferred<Unit>()
            val originListener = Runnable { originSettled.complete(Unit) }
            session.scene.activitySpace.addOriginChangedListener(originListener)

            try {
                withTimeoutOrNull(2000) { originSettled.await() }
                testBody(activity, session)
            } finally {
                session.scene.activitySpace.removeOriginChangedListener(originListener)
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
