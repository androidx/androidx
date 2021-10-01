/*
 * Copyright 2021 The Android Open Source Project
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

@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe.compat

import android.annotation.SuppressLint
import android.hardware.camera2.CameraManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.core.Debug
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.core.Timestamps
import androidx.camera.camera2.pipe.core.Permissions
import androidx.camera.camera2.pipe.core.Threads
import androidx.camera.camera2.pipe.core.WakeLock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

internal sealed class CameraRequest
internal data class RequestOpen(
    val virtualCamera: VirtualCameraState,
    val share: Boolean = false
) : CameraRequest()

internal data class RequestClose(
    val activeCamera: VirtualCameraManager.ActiveCamera
) : CameraRequest()

internal object RequestCloseAll : CameraRequest()

private const val requestQueueDepth = 8

@Suppress("EXPERIMENTAL_API_USAGE")
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@Singleton
internal class VirtualCameraManager @Inject constructor(
    private val cameraManager: Provider<CameraManager>,
    private val cameraMetadata: Camera2MetadataCache,
    private val permissions: Permissions,
    private val threads: Threads
) {
    // TODO: Consider rewriting this as a MutableSharedFlow
    private val requestQueue: Channel<CameraRequest> = Channel(requestQueueDepth)
    private val activeCameras: MutableSet<ActiveCamera> = mutableSetOf()

    init {
        threads.globalScope.launch(CoroutineName("CXCP-VirtualCameraManager")) { requestLoop() }
    }

    internal fun open(cameraId: CameraId, share: Boolean = false): VirtualCamera {
        val result = VirtualCameraState(cameraId)
        offerChecked(RequestOpen(result, share))
        return result
    }

    internal fun closeAll() {
        offerChecked(RequestCloseAll)
    }

    private fun offerChecked(request: CameraRequest) {
        check(requestQueue.trySend(request).isSuccess) {
            "There are more than $requestQueueDepth requests buffered!"
        }
    }

    private suspend fun requestLoop() = coroutineScope {
        val requests = arrayListOf<CameraRequest>()

        while (true) {
            // Stage 1: We have a request, but there is a chance we have received multiple
            //   requests.
            readRequestQueue(requests)

            // Prioritize requests that remove specific cameras from the list of active cameras.
            val closeRequest = requests.firstOrNull { it is RequestClose } as? RequestClose
            if (closeRequest != null) {
                requests.remove(closeRequest)
                if (activeCameras.contains(closeRequest.activeCamera)) {
                    activeCameras.remove(closeRequest.activeCamera)
                }

                launch {
                    closeRequest.activeCamera.close()
                }
                closeRequest.activeCamera.awaitClosed()
                continue
            }

            // If we received a closeAll request, then close every request leading up to it.
            val closeAll = requests.indexOfLast { it is RequestCloseAll }
            if (closeAll >= 0) {
                for (i in 0..closeAll) {
                    val request = requests[0]
                    if (request is RequestOpen) {
                        request.virtualCamera.disconnect()
                    }
                    requests.removeAt(0)
                }

                // Close all active cameras.
                for (activeCamera in activeCameras) {
                    launch {
                        activeCamera.close()
                    }
                }
                for (camera in activeCameras) {
                    camera.awaitClosed()
                }
                activeCameras.clear()
                continue
            }

            // The only way we get to this point is if:
            // A) We received a request
            // B) That request was NOT a Close, or CloseAll request
            val request = requests[0]
            check(request is RequestOpen)

            // Sanity Check: If the camera we are attempting to open is now closed or disconnected,
            // skip this virtual camera request.
            if (request.virtualCamera.state.value !is CameraStateUnopened) {
                requests.remove(request)
                continue
            }

            // Stage 2: Intermediate requests have been discarded, and we need to evaluate the set
            //   of currently open cameras to the set of desired cameras and close ones that are not
            //   needed. Since close may block, we will re-evaluate the next request after the
            //   desired cameras are closed since new requests may have arrived.
            val cameraIdToOpen = request.virtualCamera.cameraId
            val camerasToClose = if (request.share) {
                emptyList()
            } else {
                activeCameras.filter { it.cameraId != cameraIdToOpen }
            }

            if (camerasToClose.isNotEmpty()) {
                // Shutdown of cameras should always happen first (and suspend until complete)
                activeCameras.removeAll(camerasToClose)
                for (camera in camerasToClose) {
                    // TODO: This should be a dispatcher instead of scope.launch

                    launch {
                        // TODO: Figure out if this should be blocking or not. If we are directly invoking
                        //   close this method could block for 0-1000ms
                        camera.close()
                    }
                }
                for (realCamera in camerasToClose) {
                    realCamera.awaitClosed()
                }
                continue
            }

            // Stage 3: Open or select an active camera device.
            var realCamera = activeCameras.firstOrNull { it.cameraId == cameraIdToOpen }
            if (realCamera == null) {
                realCamera = openCameraWithRetry(cameraIdToOpen, scope = this)
                activeCameras.add(realCamera)
                continue
            }

            // Stage 4: Attach camera(s)
            realCamera.connectTo(request.virtualCamera)
            requests.remove(request)
        }
    }

    private suspend fun readRequestQueue(requests: MutableList<CameraRequest>) {
        if (requests.isEmpty()) {
            requests.add(requestQueue.receive())
        }

        // We have a request, but there is a chance we have received multiple requests while we
        // were doing other things (like opening a camera).
        while (!requestQueue.isEmpty) {
            requests.add(requestQueue.receive())
        }
    }

    @SuppressLint(
        "MissingPermission", // Permissions are checked by calling methods.
    )
    private suspend fun openCameraWithRetry(
        cameraId: CameraId,
        scope: CoroutineScope
    ): ActiveCamera {
        val metadata = cameraMetadata.getMetadata(cameraId)
        val requestTimestamp = Timestamps.now()

        var cameraState: AndroidCameraState
        var attempts = 0

        // TODO: Figure out how 1-time permissions work, and see if they can be reset without
        //   causing the application process to restart.
        check(permissions.hasCameraPermission) { "Missing camera permissions!" }

        while (true) {
            attempts++
            val instance = cameraManager.get()
            cameraState = AndroidCameraState(
                cameraId,
                metadata,
                attempts,
                requestTimestamp
            )

            var exception: Throwable? = null
            try {
                Debug.trace("CameraDevice-${cameraId.value}#openCamera") {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        Api28Compat.openCamera(
                            instance,
                            cameraId.value,
                            threads.camera2Executor,
                            cameraState
                        )
                    } else {
                        instance.openCamera(
                            cameraId.value,
                            cameraState,
                            threads.camera2Handler
                        )
                    }
                }

                // Suspend until we are no longer in a "starting" state.
                val result = cameraState.state.first {
                    it !is CameraStateUnopened
                }
                if (result is CameraStateOpen) {
                    return ActiveCamera(
                        cameraState,
                        scope,
                        requestQueue
                    )
                }
            } catch (e: Throwable) {
                exception = e
                Log.warn(e) { "CameraId ${cameraId.value}: Failed to open" }
            }

            // TODO: Add logic to optimize retry handling for various error codes and exceptions.

//            var errorCode: Int? = null
//            if (lastResult is CameraClosed) {
//                errorCode = lastResult.camera2ErrorCode
//            }
//
//            if (lastException != null) {
//                when (lastException) {
//                    is CameraAccessException -> retry = true
//                    is IllegalArgumentException -> retry = true
//                    is SecurityException -> {
//                        if ()
//                    }
//                }
//            }

            if (attempts > 3) {
                if (exception != null) {
                    cameraState.closeWith(exception)
                } else {
                    cameraState.close()
                }
            }

            // Listen to availability - if we are notified that the cameraId is available then
            // retry immediately.
            awaitAvailableCameraId(cameraId, timeoutMillis = 500)
        }
    }

    /**
     * Wait for the specified duration, or until the availability callback is invoked.
     */
    private suspend fun awaitAvailableCameraId(
        cameraId: CameraId,
        timeoutMillis: Long = 200
    ): Boolean {
        val manager = cameraManager.get()

        val cameraAvailableEvent = CompletableDeferred<Boolean>()

        val availabilityCallback = object : CameraManager.AvailabilityCallback() {
            override fun onCameraAvailable(cameraIdString: String) {
                if (cameraIdString == cameraId.value) {
                    Log.debug { "$cameraId is now available. Retry." }
                    cameraAvailableEvent.complete(true)
                }
            }

            override fun onCameraAccessPrioritiesChanged() {
                Log.debug { "Access priorities changed. Retry." }
                cameraAvailableEvent.complete(true)
            }
        }

        // WARNING: Only one registerAvailabilityCallback can be set at a time.
        // TODO: Turn this into a broadcast service so that multiple listeners can be registered if
        //  needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Api28Compat.registerAvailabilityCallback(
                manager,
                threads.camera2Executor,
                availabilityCallback
            )
        } else {
            manager.registerAvailabilityCallback(availabilityCallback, threads.camera2Handler)
        }

        // Suspend until timeout fires or until availability callback fires.
        val available = withTimeoutOrNull(timeoutMillis) {
            cameraAvailableEvent.await()
        } ?: false
        manager.unregisterAvailabilityCallback(availabilityCallback)

        if (!available) {
            Log.info { "$cameraId was not available after $timeoutMillis ms!" }
        }

        return available
    }

    internal class ActiveCamera(
        private val androidCameraState: AndroidCameraState,
        scope: CoroutineScope,
        channel: SendChannel<CameraRequest>
    ) {
        val cameraId: CameraId
            get() = androidCameraState.cameraId

        private val listenerJob: Job
        private var current: VirtualCameraState? = null

        private val wakelock = WakeLock(
            scope,
            timeout = 1000,
            callback = {
                channel.trySend(RequestClose(this)).isSuccess
            }
        )

        init {
            listenerJob = scope.launch {
                androidCameraState.state.collect {
                    if (it is CameraStateClosing || it is CameraStateClosed) {
                        wakelock.release()
                        this.cancel()
                    }
                }
            }
        }

        suspend fun connectTo(virtualCameraState: VirtualCameraState) {
            val token = wakelock.acquire()
            val previous = current
            current = virtualCameraState

            previous?.disconnect()
            virtualCameraState.connect(androidCameraState.state, token)
        }

        fun close() {
            wakelock.release()
            androidCameraState.close()
        }

        suspend fun awaitClosed() {
            androidCameraState.awaitClosed()
        }
    }
}
