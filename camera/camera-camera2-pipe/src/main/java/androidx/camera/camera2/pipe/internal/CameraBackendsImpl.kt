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

package androidx.camera.camera2.pipe.internal

import android.content.Context
import androidx.annotation.GuardedBy
import androidx.camera.camera2.pipe.CameraBackend
import androidx.camera.camera2.pipe.CameraBackendFactory
import androidx.camera.camera2.pipe.CameraBackendId
import androidx.camera.camera2.pipe.CameraBackends
import androidx.camera.camera2.pipe.CameraContext
import androidx.camera.camera2.pipe.config.CameraPipeContext
import androidx.camera.camera2.pipe.core.Threads
import kotlinx.coroutines.joinAll

/** Provides an implementation for interacting with CameraBackends. */
internal class CameraBackendsImpl(
    private val defaultBackendId: CameraBackendId,
    private val cameraBackends: Map<CameraBackendId, CameraBackendFactory>,
    @CameraPipeContext private val cameraPipeContext: Context,
    private val threads: Threads
) : CameraBackends {
    private val lock = Any()

    @GuardedBy("lock")
    private val activeCameraBackends = mutableMapOf<CameraBackendId, CameraBackend>()

    override val default: CameraBackend =
        checkNotNull(get(defaultBackendId)) {
            "Failed to load the default backend for $defaultBackendId! Available backends are " +
                "${cameraBackends.keys}"
        }

    override val allIds: Set<CameraBackendId>
        get() = cameraBackends.keys

    override val activeIds: Set<CameraBackendId>
        get() = synchronized(lock) { activeCameraBackends.keys }

    override suspend fun shutdown() {
        val shutdownJobs = activeCameraBackends.map { it.value.shutdownAsync() }
        shutdownJobs.joinAll()
    }

    override fun get(backendId: CameraBackendId): CameraBackend? {
        synchronized(lock) {
            val existing = activeCameraBackends[backendId]
            if (existing != null) return existing

            val backend =
                cameraBackends[backendId]?.create(
                    CameraBackendContext(cameraPipeContext, threads, this)
                )
            if (backend != null) {
                check(backendId == backend.id) {
                    "Unexpected backend id! Expected $backendId but it was actually ${backend.id}"
                }
                activeCameraBackends[backendId] = backend
            }
            return backend
        }
    }

    internal class CameraBackendContext(
        override val appContext: Context,
        override val threads: Threads,
        override val cameraBackends: CameraBackends
    ) : CameraContext
}
