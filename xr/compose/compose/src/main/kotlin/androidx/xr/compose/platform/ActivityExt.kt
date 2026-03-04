/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.compose.platform

import android.app.Activity
import android.view.View
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.window.embedding.ActivityEmbeddingController
import androidx.xr.compose.R
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.FloatSize3d
import androidx.xr.scenecore.ActivitySpace
import androidx.xr.scenecore.scene
import java.util.function.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Request that the system places the application into home space mode.
 *
 * In home space, the visible space may be shared with other applications; however, applications in
 * home space will have their spatial capabilities and physical bounds limited.
 *
 * This suspend function will complete once the application has successfully entered home space.
 * Cancelling this suspend function does not cancel the request to switch spaces. This should not
 * throw any exceptions and will do nothing on devices that do not support XR spaces. This request
 * will be cancelled if `requestHomeSpace` or [requestFullSpace] is called before this request
 * completes.
 *
 * See [modes in XR](https://developer.android.com/design/ui/xr/guides/foundations#modes).
 */
public suspend fun ComponentActivity.requestHomeSpace(): Unit = requestSpaceMode(Space.Home)

/**
 * Request that the system places the application into full space mode.
 *
 * In full space, this application will be the only application in the visible space, its spatial
 * capabilities will be expanded, and its physical bounds will expand to fill the entire virtual
 * space.
 *
 * This suspend function will complete once the application has successfully entered full space.
 * Cancelling this suspend function does not cancel the request to switch spaces. This should not
 * throw any exceptions and will do nothing on devices that do not support XR spaces. This request
 * will be cancelled if `requestFullSpace` or [requestHomeSpace] is called before this request
 * completes.
 *
 * See [modes in XR](https://developer.android.com/design/ui/xr/guides/foundations#modes).
 */
public suspend fun ComponentActivity.requestFullSpace(): Unit = requestSpaceMode(Space.Full)

private suspend fun ComponentActivity.requestSpaceMode(space: Space): Unit =
    lifecycleAwareCoroutineScope {
        val session = getOrCreateSession() ?: return@lifecycleAwareCoroutineScope
        val currentJob = coroutineContext[Job]

        spaceRequestMutex.withLock {
            currentSpaceRequest?.cancel()
            currentSpaceRequest = currentJob
        }

        currentJob?.invokeOnCompletion {
            launch {
                spaceRequestMutex.withLock {
                    if (currentSpaceRequest == currentJob) {
                        currentSpaceRequest = null
                    }
                }
            }
        }

        session.requestAndAwaitSpaceChange(space)
    }

private val Activity.spaceRequestMutex: Mutex
    get() {
        var mutex = contentView.getTag(R.id.compose_xr_space_request_mutex) as? Mutex
        if (mutex == null) {
            mutex = Mutex()
            contentView.setTag(R.id.compose_xr_space_request_mutex, mutex)
        }
        return mutex
    }

private var Activity.currentSpaceRequest: Job?
    get() = contentView.getTag(R.id.compose_xr_current_space_request) as? Job
    set(value) = contentView.setTag(R.id.compose_xr_current_space_request, value)

private suspend fun Session.requestAndAwaitSpaceChange(space: Space) {
    space.sendRequest(this)
    scene.activitySpace.awaitSpaceUpdated(space)
}

private enum class Space(val sendRequest: (Session) -> Unit) {
    Home({ session -> session.scene.requestHomeSpaceMode() }),
    Full({ session -> session.scene.requestFullSpaceMode() });

    companion object {
        fun fromBounds(bounds: FloatSize3d): Space =
            when (bounds) {
                INFINITE_BOUNDS -> Full
                else -> Home
            }

        private val INFINITE_BOUNDS =
            FloatSize3d(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    }
}

private suspend fun ActivitySpace.awaitSpaceUpdated(expectedSpace: Space) {
    if (Space.fromBounds(bounds) == expectedSpace) {
        return
    }
    suspendCancellableCoroutine { continuation ->
        val listener =
            object : Consumer<FloatSize3d> {
                override fun accept(nextBounds: FloatSize3d) {
                    if (Space.fromBounds(nextBounds) == expectedSpace) {
                        continuation.resume(Unit) { _, _, _ -> removeOnBoundsChangedListener(this) }
                        removeOnBoundsChangedListener(this)
                    }
                }
            }
        continuation.invokeOnCancellation { removeOnBoundsChangedListener(listener) }
        addOnBoundsChangedListener(listener)
    }
}

/** The main content view of the `Activity`. */
internal val Activity.contentView: View
    get() = window.decorView

/** Returns true if this activity is embedded in another activity */
internal fun Activity.isEmbedded(): Boolean =
    ActivityEmbeddingController.getInstance(this).isActivityEmbedded(this)

/**
 * Create a coroutineScope that will automatically be cancelled when the ComponentActivity is
 * destroyed; however, it solely uses the calling coroutine context's coroutine dispatcher to do its
 * work.
 */
private suspend fun <R> ComponentActivity.lifecycleAwareCoroutineScope(
    block: suspend CoroutineScope.() -> R
): R = coroutineScope {
    val currentJob = coroutineContext[Job]
    // If the lifecycle of the Activity completes then cancel the job.
    val lifecycleHandle =
        lifecycleScope.coroutineContext[Job]?.invokeOnCompletion { currentJob?.cancel() }
    currentJob?.invokeOnCompletion { lifecycleHandle?.dispose() }
    block()
}
