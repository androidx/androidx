/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.annotation.GuardedBy
import androidx.camera.camera2.pipe.CameraBackend
import androidx.camera.camera2.pipe.CameraBackendId
import androidx.camera.camera2.pipe.CameraController
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraStatusMonitor
import androidx.camera.camera2.pipe.CameraStatusMonitor.CameraStatus
import androidx.camera.camera2.pipe.core.Threads
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * GraphLifecycleManager is a CameraPipe-level lifecycle manager that does the following:
 * - Oversees and executes the operations of [CameraController]`s. This means it will make sure the
 *   operations are atomic, and executed based on permissible state transitions.
 * - Subscribe to [CameraStatusMonitor]s for camera status changes, basically “can attempt to
 *   restart signals”, from the respective camera backends, and then only restart
 *   [CameraController]s when the conditions are right.
 * - Once we've determined that we can restart [CameraController]s, select the “suitable”
 *   [CameraController] to restart.
 */
@Singleton
internal class GraphLifecycleManager @Inject constructor(val threads: Threads) {
    private val lock = Any()

    private val scope =
        CoroutineScope(
            threads.lightweightDispatcher.plus(CoroutineName("CXCP-GraphLifecycleManager"))
        )

    @GuardedBy("lock")
    private val backendControllerMap =
        mutableMapOf<CameraBackendId, LinkedHashSet<CameraController>>()

    @GuardedBy("lock")
    private val backendCameraStatusMap =
        mutableMapOf<CameraBackendId, MutableMap<CameraId, CameraStatus>>()

    @GuardedBy("lock") private val backendStatusCollectJobMap = mutableMapOf<CameraBackendId, Job>()

    internal fun monitorAndStart(cameraBackend: CameraBackend, cameraController: CameraController) =
        synchronized(lock) {
            startMonitoring(cameraBackend, cameraController)
            cameraController.start()
        }

    internal fun monitorAndStop(cameraBackend: CameraBackend, cameraController: CameraController) =
        synchronized(lock) {
            cameraController.stop()
            stopMonitoring(cameraBackend, cameraController)
        }

    internal fun monitorAndClose(cameraBackend: CameraBackend, cameraController: CameraController) =
        synchronized(lock) {
            cameraController.close()
            stopMonitoring(cameraBackend, cameraController)
        }

    @GuardedBy("lock")
    private fun startMonitoring(cameraBackend: CameraBackend, cameraController: CameraController) {
        // Update this camera controller with the latest camera status, if exist.
        backendCameraStatusMap[cameraBackend.id]?.get(cameraController.cameraId)?.let { status ->
            cameraController.onCameraStatusChanged(status)
        }

        if (backendControllerMap.containsKey(cameraBackend.id)) {
            backendControllerMap[cameraBackend.id]?.add(cameraController)
            return
        }
        backendControllerMap[cameraBackend.id] = linkedSetOf(cameraController)
        backendStatusCollectJobMap[cameraBackend.id] =
            scope.launch {
                cameraBackend.cameraStatus.collect { cameraStatus ->
                    when (cameraStatus) {
                        is CameraStatus.CameraPrioritiesChanged ->
                            onCameraStatusChanged(cameraBackend, cameraStatus)
                        is CameraStatus.CameraAvailable ->
                            onCameraStatusChanged(
                                cameraBackend,
                                cameraStatus,
                                cameraStatus.cameraId,
                            )
                        is CameraStatus.CameraUnavailable ->
                            onCameraStatusChanged(
                                cameraBackend,
                                cameraStatus,
                                cameraStatus.cameraId,
                            )
                    }
                }
            }
    }

    @GuardedBy("lock")
    private fun stopMonitoring(cameraBackend: CameraBackend, cameraController: CameraController) {
        if (backendControllerMap.containsKey(cameraBackend.id)) {
            val controllerSet = backendControllerMap[cameraBackend.id]
            controllerSet?.remove(cameraController)
            if (controllerSet?.size == 0) {
                backendControllerMap.remove(cameraBackend.id)
                backendStatusCollectJobMap[cameraBackend.id]?.cancel()
                backendStatusCollectJobMap.remove(cameraBackend.id)
            }
        }
    }

    private fun onCameraStatusChanged(
        cameraBackend: CameraBackend,
        cameraStatus: CameraStatus,
        cameraId: CameraId? = null,
    ) =
        synchronized(lock) {
            if (cameraId != null) {
                val cameraStatusMap =
                    backendCameraStatusMap.getOrPut(cameraBackend.id) { mutableMapOf() }
                cameraStatusMap[cameraId] = cameraStatus
            }
            // Restart the last CameraController being tracked in each backend. The last
            // CameraController would be the latest one being tracked, and should thus take priority
            // over previous CameraControllers.
            backendControllerMap[cameraBackend.id]
                ?.findLast {
                    if (cameraId != null) {
                        it.cameraId == cameraId
                    } else {
                        true
                    }
                }
                ?.onCameraStatusChanged(cameraStatus)
        }
}
