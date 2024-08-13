/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.camera.camera2.pipe

import android.view.Surface
import androidx.annotation.GuardedBy
import androidx.annotation.RestrictTo
import androidx.camera.camera2.pipe.CameraSurfaceManager.SurfaceListener
import androidx.camera.camera2.pipe.CameraSurfaceManager.SurfaceToken
import androidx.camera.camera2.pipe.core.Log
import kotlinx.atomicfu.atomic

/**
 * CameraSurfaceManager is a utility class that manages the lifetime of [Surface]s being used in
 * CameraPipe. It is a singleton, and has the same lifetime of CameraPipe, meaning that it manages
 * the lifetime of [Surface]s across multiple [CameraGraph] instances.
 *
 * Users of CameraSurfaceManager would register a [SurfaceListener] to receive updates when a
 * [Surface] is in use ("active") or no longer in use ("inactive") at CameraPipe.
 *
 * These callbacks are managed by acquiring and closing "usage tokens" for a given Surface.
 * acquiring the first token causes [SurfaceListener.onSurfaceActive] to be invoked on all
 * registered listeners. When a surface is no longer being used, the [SurfaceToken] must be closed.
 * After all outstanding tokens have been closed, the listeners receive
 * SurfaceListener.onSurfaceInactive] for that surface.
 *
 * If the same [Surface] is used in a subsequent [CameraGraph], it will be issued a different token.
 * Essentially each token means a single use on a [Surface].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CameraSurfaceManager {

    private val lock = Any()

    @GuardedBy("lock") private val useCountMap: MutableMap<Surface, Int> = mutableMapOf()

    @GuardedBy("lock") private val listeners: MutableSet<SurfaceListener> = mutableSetOf()

    /**
     * A new [SurfaceToken] is issued when a [Surface] is registered in CameraSurfaceManager. When
     * all [SurfaceToken]s issued for a [Surface] is closed, the [Surface] is considered "inactive".
     */
    public inner class SurfaceToken(internal val surface: Surface) : AutoCloseable {
        private val debugId = surfaceTokenDebugIds.incrementAndGet()
        private val closed = atomic(false)

        override fun close() {
            if (closed.compareAndSet(expect = false, update = true)) {
                Log.debug { "SurfaceToken $this closed" }
                onTokenClosed(this)
            }
        }

        override fun toString(): String = "SurfaceToken-$debugId"
    }

    public interface SurfaceListener {
        /**
         * Called when a [Surface] is in use by a [CameraGraph]. Calling [CameraGraph.setSurface]
         * will cause [onSurfaceActive] to be called on any currently registered listener. The
         * surface will remain active until the [CameraGraph] is closed AND any underlying usage has
         * been released (Normally this means that it will remain in use until the camera device is
         * closed, or until the CaptureSession that uses it is replaced).
         */
        public fun onSurfaceActive(surface: Surface)

        /**
         * Called when a [Surface] is considered "inactive" and no longer in use by [CameraGraph].
         * This can happen under a few different scenarios:
         * 1. A [Surface] is unset or replaced in a [CameraGraph].
         * 2. A CaptureSession is closed or fails to configure and the [CameraGraph] has been
         *    closed.
         * 3. [CameraGraph] is closed, and the [Surface] isn't not in use by some other camera
         *    subsystem.
         */
        public fun onSurfaceInactive(surface: Surface)
    }

    /**
     * Adds a [SurfaceListener] to receive [Surface] lifetime updates. When a listener is added, it
     * will receive [SurfaceListener.onSurfaceActive] for all active Surfaces.
     */
    public fun addListener(listener: SurfaceListener) {
        val activeSurfaces =
            synchronized(lock) {
                listeners.add(listener)
                useCountMap.filter { it.value > 0 }.keys
            }

        activeSurfaces.forEach { listener.onSurfaceActive(it) }
    }

    /** Removes a [SurfaceListener] to stop receiving [Surface] lifetime updates. */
    public fun removeListener(listener: SurfaceListener) {
        synchronized(lock) { listeners.remove(listener) }
    }

    internal fun registerSurface(surface: Surface): AutoCloseable {
        if (!surface.isValid) {
            Log.warn { "registerSurface: Surface $surface isn't valid!" }
        }
        val surfaceToken: SurfaceToken
        var listenersToInvoke: List<SurfaceListener>? = null

        synchronized(lock) {
            surfaceToken = SurfaceToken(surface)
            val newUseCount = (useCountMap[surface] ?: 0) + 1
            useCountMap[surface] = newUseCount
            if (DEBUG) {
                Log.debug {
                    "registerSurface: surface=$surface, " +
                        "surfaceToken=$surfaceToken, newUseCount=$newUseCount" +
                        (if (DEBUG) " from ${Log.readStackTrace()}" else "")
                }
            }

            if (newUseCount == 1) {
                Log.debug { "$surface for $surfaceToken is active" }
                listenersToInvoke = listeners.toList()
            }
        }

        listenersToInvoke?.forEach { it.onSurfaceActive(surface) }
        return surfaceToken
    }

    internal fun onTokenClosed(surfaceToken: SurfaceToken) {
        val surface: Surface
        var listenersToInvoke: List<SurfaceListener>? = null

        synchronized(lock) {
            surface = surfaceToken.surface
            val useCount = useCountMap[surface]
            checkNotNull(useCount) { "Surface $surface ($surfaceToken) has no use count" }
            val newUseCount = useCount - 1
            useCountMap[surface] = newUseCount

            if (DEBUG) {
                Log.debug {
                    "onTokenClosed: surface=$surface, " +
                        "surfaceToken=$surfaceToken, newUseCount=$newUseCount" +
                        (if (DEBUG) " from ${Log.readStackTrace()}" else "")
                }
            }
            if (newUseCount == 0) {
                Log.debug { "$surface for $surfaceToken is inactive" }
                listenersToInvoke = listeners.toList()
                useCountMap.remove(surface)
            }
        }

        listenersToInvoke?.forEach { it.onSurfaceInactive(surface) }
    }

    public companion object {
        public const val DEBUG: Boolean = false

        internal val surfaceTokenDebugIds = atomic(0)
    }
}
