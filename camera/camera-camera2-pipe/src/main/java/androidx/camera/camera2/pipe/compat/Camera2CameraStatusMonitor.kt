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
import androidx.camera.camera2.pipe.CameraStatusMonitor
import androidx.camera.camera2.pipe.CameraStatusMonitor.CameraStatus
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.core.Threads
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow

@Singleton
internal class Camera2CameraStatusMonitor
@Inject
constructor(cameraManager: Provider<CameraManager>, threads: Threads) : CameraStatusMonitor {
    override val cameraStatus = callbackFlow {
        val manager = cameraManager.get()
        val availabilityCallback =
            object : CameraManager.AvailabilityCallback() {
                override fun onCameraAccessPrioritiesChanged() {
                    Log.debug { "Camera access priorities have changed" }
                    trySendBlocking(CameraStatus.CameraPrioritiesChanged).onFailure {
                        Log.warn { "Failed to emit CameraPrioritiesChanged" }
                    }
                }

                override fun onCameraAvailable(cameraId: String) {
                    Log.debug { "Camera $cameraId has become available" }
                    trySendBlocking(CameraStatus.CameraAvailable(CameraId.fromCamera2Id(cameraId)))
                        .onFailure { Log.warn { "Failed to emit CameraAvailable($cameraId)" } }
                }

                override fun onCameraUnavailable(cameraId: String) {
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
                availabilityCallback
            )
        } else {
            manager.registerAvailabilityCallback(availabilityCallback, threads.camera2Handler)
        }

        awaitClose { manager.unregisterAvailabilityCallback(availabilityCallback) }
    }
}
