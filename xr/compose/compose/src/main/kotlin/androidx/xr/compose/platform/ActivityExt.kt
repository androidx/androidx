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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Represents the expected result of a space request. */
public sealed interface SpaceRequestResult {
    /** The application successfully transitioned to the requested space. */
    public object Success : SpaceRequestResult

    /** The device or environment does not support XR space transitions. */
    public object Unsupported : SpaceRequestResult

    /**
     * The space transition failed due to a system error or exception.
     *
     * @param cause the underlying exception or cause of the error
     * @property cause the underlying exception or cause of the error
     */
    public class Error(public val cause: Throwable) : SpaceRequestResult {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Error

            return cause == other.cause
        }

        override fun hashCode(): Int {
            return cause.hashCode()
        }

        override fun toString(): String {
            return "Error(cause=$cause)"
        }
    }

    /**
     * Prevents exhaustive `when` usage for Kotlin consumers, making it safe to add new public
     * result types in future releases.
     */
    private object Hidden : SpaceRequestResult
}

/**
 * Request that the system places the application into home space mode.
 *
 * In home space, the visible space may be shared with other applications; however, applications in
 * home space will have their spatial capabilities and physical bounds limited.
 *
 * This suspend function initiates an asynchronous OS-level space change and will resume with
 * [SpaceRequestResult.Success] once the application has successfully entered home space. If the
 * device does not support XR spaces, it will resume immediately with
 * [SpaceRequestResult.Unsupported].
 *
 * Note: Because Full Space Mode and Home Space Mode changes are OS-level system changes, the space
 * switch cannot be aborted mid-flight once initiated. Cancelling this coroutine unregisters the
 * bounds listener but does not interrupt the ongoing space change. If [requestFullSpace] or
 * `requestHomeSpace` is called again before this request completes, the coroutine suspended on this
 * call will be cancelled with a `CancellationException`.
 *
 * See [modes in XR](https://developer.android.com/design/ui/xr/guides/foundations#modes).
 *
 * @return [SpaceRequestResult.Success] if the application successfully enters home space,
 *   [SpaceRequestResult.Unsupported] if the device does not support XR spaces, or
 *   [SpaceRequestResult.Error] if a system error occurs.
 * @throws CancellationException if the request is cancelled before completion.
 */
public suspend fun ComponentActivity.requestHomeSpace(): SpaceRequestResult =
    requestSpaceMode(Space.Home)

/**
 * Request that the system places the application into full space mode.
 *
 * In full space, this application will be the only application in the visible space, its spatial
 * capabilities will be expanded, and its physical bounds will expand to fill the entire virtual
 * space.
 *
 * This suspend function initiates an asynchronous OS-level space change and will resume with
 * [SpaceRequestResult.Success] once the application has successfully entered full space. If the
 * device does not support XR spaces, it will resume immediately with
 * [SpaceRequestResult.Unsupported].
 *
 * Note: Because Full Space Mode and Home Space Mode changes are OS-level system changes, the space
 * switch cannot be aborted mid-flight once initiated. Cancelling this coroutine unregisters the
 * bounds listener but does not interrupt the ongoing space change. If `requestFullSpace` or
 * [requestHomeSpace] is called again before this request completes, the coroutine suspended on this
 * call will be cancelled with a `CancellationException`.
 *
 * See [modes in XR](https://developer.android.com/design/ui/xr/guides/foundations#modes).
 *
 * @return [SpaceRequestResult.Success] if the application successfully enters full space,
 *   [SpaceRequestResult.Unsupported] if the device does not support XR spaces, or
 *   [SpaceRequestResult.Error] if a system error occurs.
 * @throws CancellationException if the request is cancelled before completion.
 */
public suspend fun ComponentActivity.requestFullSpace(): SpaceRequestResult =
    requestSpaceMode(Space.Full)

private suspend fun ComponentActivity.requestSpaceMode(space: Space): SpaceRequestResult =
    lifecycleAwareCoroutineScope {
        if (!SpatialConfiguration.hasXrSpatialFeature(this@requestSpaceMode)) {
            return@lifecycleAwareCoroutineScope SpaceRequestResult.Unsupported
        }

        val session =
            getOrCreateSession()
                ?: return@lifecycleAwareCoroutineScope SpaceRequestResult.Unsupported

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

        return@lifecycleAwareCoroutineScope try {
            session.requestAndAwaitSpaceChange(space)
            SpaceRequestResult.Success
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) {
                throw e
            }

            SpaceRequestResult.Error(e)
        }
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
    Home({ session -> session.scene.requestHomeSpace() }),
    Full({ session -> session.scene.requestFullSpace() });

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
                        continuation.resume(Unit) { _, _, _ -> removeBoundsChangedListener(this) }
                        removeBoundsChangedListener(this)
                    }
                }
            }
        continuation.invokeOnCancellation { removeBoundsChangedListener(listener) }
        addBoundsChangedListener(listener)
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
