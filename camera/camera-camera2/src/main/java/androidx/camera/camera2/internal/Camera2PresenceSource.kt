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

package androidx.camera.camera2.internal

import android.hardware.camera2.CameraManager
import android.util.Log
import androidx.camera.camera2.internal.compat.CameraAccessExceptionCompat
import androidx.camera.camera2.internal.compat.CameraManagerCompat
import androidx.camera.core.CameraIdentifier
import androidx.camera.core.impl.AbstractCameraPresenceSource
import androidx.camera.core.impl.utils.futures.Futures
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor

/**
 * An [androidx.camera.core.impl.Observable] for camera availability that uses
 * [CameraManager.AvailabilityCallback].
 */
public class Camera2PresenceSource(
    private val cameraManager: CameraManagerCompat,
    private val systemCallbackExecutor: Executor,
) : AbstractCameraPresenceSource() {

    private var systemAvailabilityCallback: CameraManager.AvailabilityCallback? = null

    public override fun startMonitoring() {
        if (systemAvailabilityCallback != null) {
            Log.w(TAG, "Monitoring already started. Unregistering existing callback.")
            stopMonitoring()
        }

        Log.i(TAG, "Starting system availability monitoring.")
        systemAvailabilityCallback =
            object : CameraManager.AvailabilityCallback() {
                override fun onCameraAvailable(cameraId: String) {
                    Log.d(TAG, "System onCameraAvailable: $cameraId")
                    fetchData().fetchDataAndForget()
                }

                override fun onCameraUnavailable(cameraId: String) {
                    Log.d(TAG, "System onCameraUnavailable: $cameraId")
                    fetchData().fetchDataAndForget()
                }

                override fun onCameraAccessPrioritiesChanged() {
                    Log.d(TAG, "System onCameraAccessPrioritiesChanged.")
                    fetchData().fetchDataAndForget()
                }
            }

        cameraManager.registerAvailabilityCallback(
            systemCallbackExecutor,
            systemAvailabilityCallback!!,
        )
        // Perform an initial fetch upon starting.
        fetchData().fetchDataAndForget()
    }

    public override fun stopMonitoring() {
        Log.i(TAG, "Stopping system availability monitoring.")
        systemAvailabilityCallback?.let {
            try {
                cameraManager.unregisterAvailabilityCallback(it)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to unregister system availability callback.", e)
            } finally {
                systemAvailabilityCallback = null
            }
        }
    }

    override fun fetchData(): ListenableFuture<Set<CameraIdentifier>> {
        return CallbackToFutureAdapter.getFuture { completer ->
            systemCallbackExecutor.execute {
                try {
                    val newCameraSet =
                        cameraManager.cameraIdList.map { CameraIdentifier.create(it) }.toSet()
                    Log.d(TAG, "[FetchData] Refreshed camera list: ${newCameraSet.joinToString()}")
                    updateData(newCameraSet)
                    completer.set(newCameraSet)
                } catch (e: CameraAccessExceptionCompat) {
                    Log.e(TAG, "[FetchData] Failed to get camera list for refresh.", e)
                    val error = CameraUnavailableExceptionHelper.createFrom(e)
                    updateError(error)
                    completer.setException(error)
                }
            }
            "FetchData for CameraAvailability"
        }
    }

    /**
     * Executes the [ListenableFuture] but ignores its result.
     *
     * This extension is used to call [fetchData] for its side effect of updating the internal state
     * and notifying observers, without needing to handle the future's result. This is useful for
     * fire-and-forget scenarios like system availability callbacks.
     */
    private fun ListenableFuture<*>.fetchDataAndForget() {
        Futures.transformAsyncOnCompletion(this)
    }

    private companion object {
        private const val TAG = "Camera2PresenceSrc"
    }
}
