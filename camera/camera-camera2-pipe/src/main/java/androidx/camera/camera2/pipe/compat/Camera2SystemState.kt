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

package androidx.camera.camera2.pipe.compat

import androidx.annotation.GuardedBy
import androidx.camera.camera2.pipe.CameraGraphId
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.core.Debug
import androidx.camera.camera2.pipe.core.Threads
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.launch

@Singleton
internal class Camera2SystemState
@Inject
constructor(
    private val cameraInteropConfig: CameraPipe.CameraInteropConfig,
    private val threads: Threads,
) {
    private enum class State {
        STOPPED,
        STARTED,
    }

    private val lock = Any()
    private var systemState = State.STOPPED
    private val activeCameras = mutableSetOf<CameraId>()
    private val activeGraphs = mutableSetOf<CameraGraphId>()

    /** Invoked before attempting to open a Camera2 CameraDevice */
    fun onCameraOpening(cameraId: CameraId) {
        synchronized(lock) {
            activeCameras.add(cameraId)
            if (systemState != State.STOPPED) {
                return
            }

            invokeStarting()
        }
    }

    fun onCameraClosed(cameraId: CameraId) {
        synchronized(lock) {
            val removed = activeCameras.remove(cameraId)

            // Stop can be invoked synchronously if there is no active graphs when the
            // camera is closed.
            if (removed && activeCameras.isEmpty() && activeGraphs.isEmpty()) {
                invokeStopped()
            }
        }
    }

    fun onGraphStarting(graphId: CameraGraphId) {
        synchronized(lock) {
            // Track active CameraGraph, and do not invoke shutdown until both the camera and the
            // active graphs are also closed.
            activeGraphs.add(graphId)
        }
    }

    fun onGraphStopped(graphId: CameraGraphId) {
        synchronized(lock) {
            // Idempotent: Active graphs can be removed multiple times due to overlapping
            // teardown sequences (e.g., stop followed by close).
            activeGraphs.remove(graphId)

            // If the camera device was previously closed, we may need to invoke the shutdown when
            // the graph is closed.
            if (activeGraphs.isEmpty() && activeCameras.isEmpty()) {
                threads.cameraPipeScope.launch {
                    synchronized(lock) {
                        if (activeGraphs.isEmpty() && activeCameras.isEmpty()) {
                            invokeStopped()
                        }
                    }
                }
            }
        }
    }

    @GuardedBy("lock")
    private fun invokeStarting() {
        systemState = State.STARTED
        Debug.trace("onCameraSystemStarting") {
            cameraInteropConfig.cameraSystemCallbacks?.onCameraSystemStarting()
        }
    }

    @GuardedBy("lock")
    private fun invokeStopped() {
        systemState = State.STOPPED
        Debug.trace("onCameraSystemStopped") {
            cameraInteropConfig.cameraSystemCallbacks?.onCameraSystemStopped()
        }
    }
}
