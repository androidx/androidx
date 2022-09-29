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
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraSurfaceManager.SurfaceListener
import androidx.camera.camera2.pipe.CameraSurfaceManager.SurfaceToken
import androidx.camera.camera2.pipe.core.Log
import java.io.Closeable
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.atomicfu.atomic

/**
 * CameraSurfaceManager is a utility class that manages the lifetime of [Surface]s being used in
 * CameraPipe. It is a singleton, and has the same lifetime of CameraPipe, meaning that it manages
 * the lifetime of [Surface]s across multiple [CameraGraph] instances.
 *
 * Users of CameraSurfaceManager would register a [SurfaceListener] to receive updates when a
 * [Surface] is in use ("active") or no longer in use ("inactive") at CameraPipe.
 *
 * Internally, when and after a [CameraGraph] is started, all [Surface]s belonging to the
 * [CameraGraph] will be registered at CameraSurfaceManager where each [Surface] is given a
 * [SurfaceToken]. When a [Surface] is no longer in use, we close the [SurfaceToken] associated
 * with the [Surface].
 *
 * Note if the same [Surface] is used in a subsequent [CameraGraph], it will be issued a different
 * token. Essentially each token means a single use on a [Surface].
 */
@Singleton
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
public class CameraSurfaceManager @Inject constructor() {

    private val lock = Any()

    @GuardedBy("lock")
    private val useCountMap: MutableMap<Surface, Int> = mutableMapOf()

    @GuardedBy("lock")
    private val listeners: MutableSet<SurfaceListener> = mutableSetOf()

    /**
     * A new [SurfaceToken] is issued when a [Surface] is registered in CameraSurfaceManager.
     * When all [SurfaceToken]s issued for a [Surface] is closed, the [Surface] is considered
     * "inactive".
     */
    inner class SurfaceToken(
        internal val surface: Surface
    ) : Closeable {
        private val closed = atomic(false)
        override fun close() {
            if (closed.compareAndSet(expect = false, update = true)) {
                Log.debug { "SurfaceToken $this closed" }
                onTokenClosed(this)
            }
        }
    }

    interface SurfaceListener {
        /**
         * Called when a [Surface] is active. A [Surface] is considered active when it's registered,
         * which happens when:
         *
         *   1. A [CameraGraph] is started with an initial set of Surfaces.
         *   2. A new [Surface] is set on the [CameraGraph] after it's started.
         */
        fun onSurfaceActive(surface: Surface)

        /**
         * Called when a [Surface] is no longer in use ("inactive"). This can happen when:
         *
         *   1. A [Surface] is unset from a [CameraGraph] after it's started.
         *   2. Capture session is closed or configured unsuccessfully, and the [Surface] isn't in
         *      use in another [CameraGraph].
         *   3. [CameraGraph] is stopped, and the [Surface] isn't in use in another [CameraGraph].
         */
        fun onSurfaceInactive(surface: Surface)
    }

    /**
     * Adds a [SurfaceListener] to receive [Surface] lifetime updates. When a listener is added,
     * it will receive onSurfaceActive() on all active Surfaces.
     */
    public fun addListener(listener: SurfaceListener) {
        val activeSurfaces = synchronized(lock) {
            listeners.add(listener)
            useCountMap.filter { it.value > 0 }.keys
        }

        activeSurfaces.forEach { listener.onSurfaceActive(it) }
    }

    /**
     * Removes a [SurfaceListener] to stop receiving [Surface] lifetime updates.
     */
    public fun removeListener(listener: SurfaceListener) {
        synchronized(lock) {
            listeners.remove(listener)
        }
    }

    internal fun registerSurface(surface: Surface): Closeable {
        check(surface.isValid) { "Surface $surface isn't valid!" }
        val surfaceToken: SurfaceToken
        var listenersToInvoke: List<SurfaceListener>? = null

        synchronized(lock) {
            val useCount = useCountMap[surface] ?: 0
            useCountMap[surface] = useCount + 1
            if (useCount + 1 == 1) {
                Log.debug { "Surface $surface has become active" }
                listenersToInvoke = listeners.toList()
            }
            surfaceToken = SurfaceToken(surface)
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
            useCountMap[surface] = useCount - 1
            if (useCount - 1 == 0) {
                Log.debug { "Surface $surface has become inactive" }
                listenersToInvoke = listeners.toList()
                useCountMap.remove(surface)
            }
        }

        listenersToInvoke?.forEach { it.onSurfaceInactive(surface) }
    }
}