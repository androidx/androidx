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

package androidx.camera.camera2.pipe.integration.adapter

import android.content.Context
import android.hardware.camera2.CameraManager
import android.util.Log
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.core.CameraIdentifier
import androidx.camera.core.impl.AbstractCameraPresenceSource
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * An [androidx.camera.core.impl.Observable] for camera availability that sources its data from a
 * [Flow] of CameraPipe IDs.
 */
public class PipeCameraPresenceSource(
    private val idFlow: Flow<List<CameraId>>,
    private val coroutineScope: CoroutineScope,
    context: Context,
) : AbstractCameraPresenceSource() {

    private var flowCollectionJob: Job? = null
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    override fun startMonitoring() {
        Log.i(TAG, "Starting to collect camera ID flow.")
        flowCollectionJob?.cancel()
        flowCollectionJob =
            idFlow
                .map { pipeCameraIdList ->
                    pipeCameraIdList.mapNotNull { pipeId ->
                        try {
                            CameraIdentifier.create(pipeId.value)
                        } catch (ex: Exception) {
                            Log.w(
                                TAG,
                                "Failed to create CameraIdentifier for pipeId: ${pipeId.value}",
                                ex,
                            )
                            null
                        }
                    }
                }
                .onEach { identifiers ->
                    Log.d(TAG, "Flow emitted new camera set: ${identifiers.joinToString()}")
                    updateData(identifiers)
                }
                .catch { e ->
                    Log.e(TAG, "Error in camera ID flow collection.", e)
                    updateError(e)
                }
                .launchIn(coroutineScope)
    }

    public override fun stopMonitoring() {
        Log.i(TAG, "Stopping camera ID flow collection.")
        flowCollectionJob?.cancel()
        flowCollectionJob = null
    }

    override fun fetchData(): ListenableFuture<List<CameraIdentifier>> {
        return CallbackToFutureAdapter.getFuture { completer ->
            coroutineScope.launch {
                try {
                    val systemCameraIds = cameraManager.cameraIdList
                    val newIdentifiers =
                        systemCameraIds.mapNotNull {
                            try {
                                CameraIdentifier.create(it)
                            } catch (e: IllegalArgumentException) {
                                Log.w(
                                    TAG,
                                    "Could not create CameraIdentifier for system ID: $it",
                                    e,
                                )
                                null
                            }
                        }

                    Log.d(TAG, "[FetchData] Refreshed camera list from hardware: $newIdentifiers")
                    updateData(newIdentifiers) // Update state and notify
                    completer.set(newIdentifiers)
                } catch (e: Exception) { // Catch CameraAccessException or other errors
                    Log.e(TAG, "[FetchData] Failed to refresh camera list from hardware.", e)
                    updateError(e) // Update state and notify
                    completer.setException(e)
                }
            }
            "FetchData for PipeCameraPresence0"
        }
    }

    public companion object {
        private const val TAG = "PipePresenceSrc"
    }
}
