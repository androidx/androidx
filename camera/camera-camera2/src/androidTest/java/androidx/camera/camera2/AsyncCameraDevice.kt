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

package androidx.camera.camera2

import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Handler
import androidx.annotation.RequiresApi
import androidx.concurrent.futures.CallbackToFutureAdapter
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.atomicfu.atomic

/**
 * Utility class for opening and closing a camera with [ListenableFuture]s to determine when each
 * operation has completed.
 *
 * <p>The camera must be explicitly opened and closed, and once closed it cannot be reopened.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
internal class AsyncCameraDevice(
    private val cameraManager: CameraManager,
    private val camId: String,
    private val cameraHandler: Handler
) {

    private var closed = atomic(false)

    private val openFuture: ListenableFuture<CameraDevice> by lazy {
        CallbackToFutureAdapter.getFuture { openCompleter ->
            if (!closed.value) {
                try {
                    cameraManager.openCamera(camId, object : CameraDevice.StateCallback() {
                        override fun onOpened(cameraDevice: CameraDevice) {
                            if (!openCompleter.set(cameraDevice)) {
                                cameraDevice.close()
                            }
                        }

                        override fun onDisconnected(cameraDevice: CameraDevice) {
                            openCompleter.setException(RuntimeException("Camera disconnected"))
                            cameraDevice.close()
                        }

                        override fun onError(cameraDevice: CameraDevice, i: Int) {
                            openCompleter.setException(RuntimeException("Camera error: $i"))
                            cameraDevice.close()
                        }

                        override fun onClosed(camera: CameraDevice) {
                            closeCompleter.set(null)
                        }
                    }, cameraHandler)
                } catch (ex: Exception) {
                    openCompleter.setException(RuntimeException("Unable to open camera", ex))
                    closeCompleter.set(null)
                }
            } else {
                closeCompleter.set(null)
            }
            "open"
        }
    }

    private lateinit var closeCompleter: CallbackToFutureAdapter.Completer<Any?>
    private val closeFuture: ListenableFuture<Any?> = CallbackToFutureAdapter.getFuture {
        closeCompleter = it
        "close"
    }

    /**
     * Opens the camera device.
     *
     * <p>When the camera opens successfully, the returned [ListenableFuture] will also complete
     * successfully. If the camera fails to open or the camera is closed, a the future may fail,
     * in which case the camera doesn't need to be closed with [closeAsync].
     *
     * <p>Cancelling this future will attempt to close the camera once it is opened. If cancellation
     * is successful, the camera doesn't need to be closed with [closeAsync].
     */
    fun openAsync(): ListenableFuture<CameraDevice> {
        // Will attempt to open camera lazily or return already created future
        return openFuture
    }

    /**
     * Closes the open camera.
     *
     * <p>If the camera is already closed or encountered an error, this will be a no-op.
     *
     * <p>The returned future will complete successfully once an open camera is closed or the
     * camera cannot be opened.
     *
     * <p>The camera should always be closed if the camera was successfully opened.
     *
     * <p>Calling close while the camera is opening is equivalent to attempting to cancel the
     * future returned by [openAsync].
     */
    fun closeAsync(): ListenableFuture<Any?> {
        if (!closed.getAndSet(true)) {
            if (!openFuture.cancel(true) && !openFuture.isCancelled) {
                try {
                    // Future must be done (successful or failed). Attempt to close camera.
                    val openCameraDevice = openFuture.get()
                    openCameraDevice.close()
                } catch (e: Exception) {
                    // The camera was never opened because an error occurred.
                    // No need to do anything.
                }
            }
        }
        return closeFuture
    }
}
