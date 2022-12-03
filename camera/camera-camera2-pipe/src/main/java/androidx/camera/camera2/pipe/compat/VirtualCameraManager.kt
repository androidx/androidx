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
import androidx.camera.camera2.pipe.CameraError
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.core.Debug
import androidx.camera.camera2.pipe.core.DurationNs
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.core.Permissions
import androidx.camera.camera2.pipe.core.SystemTimeSource
import androidx.camera.camera2.pipe.core.Threads
import androidx.camera.camera2.pipe.core.TimeSource
import androidx.camera.camera2.pipe.core.TimestampNs
import androidx.camera.camera2.pipe.core.Timestamps
import androidx.camera.camera2.pipe.core.Timestamps.formatMs
import androidx.camera.camera2.pipe.core.WakeLock
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

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

private val cameraRetryTimeout = DurationNs(10_000_000_000) // 10 seconds

@Suppress("EXPERIMENTAL_API_USAGE")
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@Singleton
internal class VirtualCameraManager @Inject constructor(
    private val cameraManager: Provider<CameraManager>,
    private val cameraMetadata: Camera2MetadataCache,
    private val permissions: Permissions,
    private val threads: Threads,
    private val timeSource: TimeSource
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
            if (request.virtualCamera.value !is CameraStateUnopened) {
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
                if (realCamera != null) {
                    activeCameras.add(realCamera)
                } else {
                    request.virtualCamera.disconnect()
                    requests.remove(request)
                }
                continue
            }

            // Stage 4: Attach camera(s)
            realCamera.connectTo(request.virtualCamera)
            requests.remove(request)
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
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

    private suspend fun openCameraWithRetry(
        cameraId: CameraId,
        scope: CoroutineScope
    ): ActiveCamera? {
        val requestTimestamp = Timestamps.now(timeSource)

        // Parser erroneously marks "attempts" as unused variable.
        @Suppress("UNUSED_VARIABLE")
        var attempts = 0

        // TODO: Figure out how 1-time permissions work, and see if they can be reset without
        //   causing the application process to restart.
        check(permissions.hasCameraPermission) { "Missing camera permissions!" }

        while (true) {
            attempts++

            val result = tryOpenCamera(cameraId, attempts, requestTimestamp, timeSource, scope)
            with(result) {
                if (activeCamera != null) {
                    return activeCamera
                }

                if (errorCode == null) {
                    // Camera open failed without an error. This can only happen if the
                    // VirtualCameraState is disconnected by the app itself. As such, we should just
                    // abandon the camera open attempt.
                    Log.warn {
                        "Camera open failed without an error. " +
                            "The CameraGraph may have been stopped or closed. " +
                            "Abandoning the camera open attempt."
                    }
                    return null
                }

                if (!shouldRetry(errorCode, attempts, requestTimestamp)) {
                    Log.error {
                        "Failed to open camera $cameraId after $attempts attempts " +
                            "and ${(Timestamps.now(timeSource) - requestTimestamp).formatMs()}. " +
                            "Last error was $errorCode."
                    }
                    return null
                }
            }

            // Listen to availability - if we are notified that the cameraId is available then
            // retry immediately.
            awaitAvailableCameraId(cameraId, timeoutMillis = 500)
        }
    }

    @SuppressLint(
        "MissingPermission", // Permissions are checked by calling methods.
    )
    private suspend fun tryOpenCamera(
        cameraId: CameraId,
        attempts: Int,
        requestTimestamp: TimestampNs,
        timeSource: TimeSource,
        scope: CoroutineScope,
    ): CameraOpenResult {
        val instance = cameraManager.get()
        val metadata = cameraMetadata.getMetadata(cameraId)
        val cameraState = AndroidCameraState(
            cameraId,
            metadata,
            attempts,
            requestTimestamp,
            timeSource
        )

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
            when (result) {
                is CameraStateOpen ->
                    return CameraOpenResult(
                        activeCamera = ActiveCamera(
                            cameraState,
                            scope,
                            requestQueue
                        )
                    )

                is CameraStateClosing -> {
                    cameraState.close()
                    return CameraOpenResult(errorCode = result.cameraErrorCode)
                }

                is CameraStateClosed -> {
                    cameraState.close()
                    return CameraOpenResult(errorCode = result.cameraErrorCode)
                }

                is CameraStateUnopened -> {
                    cameraState.close()
                    throw IllegalStateException("Unexpected CameraState: $result")
                }
            }
        } catch (exception: Throwable) {
            Log.warn(exception) { "Failed to open $cameraId" }
            cameraState.closeWith(exception)
            return CameraOpenResult(errorCode = CameraError.from(exception))
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

    internal data class CameraOpenResult(
        val activeCamera: ActiveCamera? = null,
        val errorCode: CameraError? = null,
    )

    companion object {
        internal fun shouldRetry(
            errorCode: CameraError,
            attempts: Int,
            firstAttemptTimestampNs: TimestampNs,
            timeSource: TimeSource = SystemTimeSource()
        ): Boolean {
            val elapsed = Timestamps.now(timeSource) - firstAttemptTimestampNs
            if (elapsed > cameraRetryTimeout) {
                return false
            }
            return when (errorCode) {
                CameraError.ERROR_CAMERA_IN_USE -> attempts <= 1
                CameraError.ERROR_CAMERA_LIMIT_EXCEEDED -> true
                CameraError.ERROR_CAMERA_DISABLED -> attempts <= 1
                CameraError.ERROR_CAMERA_DEVICE -> true
                CameraError.ERROR_CAMERA_SERVICE -> true
                CameraError.ERROR_CAMERA_DISCONNECTED -> true
                CameraError.ERROR_ILLEGAL_ARGUMENT_EXCEPTION -> true
                CameraError.ERROR_SECURITY_EXCEPTION -> attempts <= 1
                else -> {
                    Log.error { "Unexpected CameraError: $this" }
                    false
                }
            }
        }
    }
}