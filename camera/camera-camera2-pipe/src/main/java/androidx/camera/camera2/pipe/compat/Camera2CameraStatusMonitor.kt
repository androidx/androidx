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

package androidx.camera.camera2.pipe.compat

import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.core.Threads
import androidx.camera.camera2.pipe.internal.CameraStatusMonitor
import androidx.camera.camera2.pipe.internal.CameraStatusMonitor.CameraStatus
import javax.inject.Provider
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

internal class Camera2CameraStatusMonitor(
    cameraManager: Provider<CameraManager>,
    private val threads: Threads,
    private val cameraId: CameraId,
    cameraPipeJob: Job,
) : CameraStatusMonitor {
    private val manager = cameraManager.get()
    private val scope =
        CoroutineScope(
            SupervisorJob(cameraPipeJob) +
                threads.lightweightDispatcher.plus(CoroutineName("CXCP-CameraStatusMonitor"))
        )

    private val closed = atomic(false)

    private val _cameraAvailability = MutableStateFlow<CameraStatus>(CameraStatus.Unknown)
    override val cameraAvailability: StateFlow<CameraStatus> = _cameraAvailability.asStateFlow()

    private val _cameraPriorities = MutableSharedFlow<Unit>()
    override val cameraPriorities: SharedFlow<Unit> = _cameraPriorities.asSharedFlow()

    private val cameraStatus = cameraStatusFlow()
    private val cameraStatusJob =
        scope.launch {
            cameraStatus.collect { cameraStatus ->
                when (cameraStatus) {
                    is CameraStatus.CameraAvailable -> _cameraAvailability.emit(cameraStatus)
                    is CameraStatus.CameraUnavailable -> _cameraAvailability.emit(cameraStatus)
                    is CameraStatus.CameraPrioritiesChanged -> _cameraPriorities.emit(Unit)
                }
            }
        }

    private fun cameraStatusFlow() = callbackFlow {
        val availabilityCallback =
            object : CameraManager.AvailabilityCallback() {
                override fun onCameraAccessPrioritiesChanged() {
                    Log.debug { "Camera access priorities have changed" }
                    trySendBlocking(CameraStatus.CameraPrioritiesChanged).onFailure {
                        Log.warn { "Failed to emit CameraPrioritiesChanged" }
                    }
                }

                override fun onCameraAvailable(cameraId: String) {
                    if (cameraId != this@Camera2CameraStatusMonitor.cameraId.value) return
                    Log.debug { "Camera $cameraId has become available" }
                    trySendBlocking(CameraStatus.CameraAvailable(CameraId.fromCamera2Id(cameraId)))
                        .onFailure { Log.warn { "Failed to emit CameraAvailable($cameraId)" } }
                }

                override fun onCameraUnavailable(cameraId: String) {
                    if (cameraId != this@Camera2CameraStatusMonitor.cameraId.value) return
                    Log.debug { "Camera $cameraId has become unavailable" }
                    trySendBlocking(
                            CameraStatus.CameraUnavailable(CameraId.fromCamera2Id(cameraId))
                        )
                        .onFailure { Log.warn { "Failed to emit CameraUnavailable($cameraId)" } }
                }
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Api28Compat.registerAvailabilityCallback(
                manager,
                threads.lightweightExecutor,
                availabilityCallback,
            )
        } else {
            manager.registerAvailabilityCallback(availabilityCallback, threads.camera2Handler)
        }

        awaitClose { manager.unregisterAvailabilityCallback(availabilityCallback) }
    }

    override fun close() {
        if (closed.compareAndSet(expect = false, update = true)) {
            cameraStatusJob.cancel()
            scope.cancel()
        }
    }
}
