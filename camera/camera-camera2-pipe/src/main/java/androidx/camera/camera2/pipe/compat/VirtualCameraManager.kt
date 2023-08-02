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

import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraError
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.core.Permissions
import androidx.camera.camera2.pipe.core.Threads
import androidx.camera.camera2.pipe.core.WakeLock
import androidx.camera.camera2.pipe.graph.GraphListener
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal sealed class CameraRequest

internal data class RequestOpen(
    val virtualCamera: VirtualCameraState,
    val sharedCameraIds: List<CameraId>,
    val graphListener: GraphListener,
    val isForegroundObserver: (Unit) -> Boolean,
) : CameraRequest()

internal data class RequestClose(val activeCamera: VirtualCameraManager.ActiveCamera) :
    CameraRequest()

internal object RequestCloseAll : CameraRequest()

// A queue depth of 32 was deemed necessary in b/276051078 where a flood of requests can cause the
// queue depth to go over 8. In the long run, we can perhaps look into refactoring and
// reimplementing the request queue in a more robust way.
private const val requestQueueDepth = 32

@Suppress("EXPERIMENTAL_API_USAGE")
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@Singleton
internal class VirtualCameraManager
@Inject
constructor(
    private val permissions: Permissions,
    private val retryingCameraStateOpener: RetryingCameraStateOpener,
    private val camera2ErrorProcessor: Camera2ErrorProcessor,
    private val threads: Threads
) {
    // TODO: Consider rewriting this as a MutableSharedFlow
    private val requestQueue: Channel<CameraRequest> = Channel(requestQueueDepth)
    private val activeCameras: MutableSet<ActiveCamera> = mutableSetOf()
    private val pendingRequestOpens = mutableListOf<RequestOpen>()

    init {
        threads.globalScope.launch(CoroutineName("CXCP-VirtualCameraManager")) { requestLoop() }
    }

    internal fun open(
        cameraId: CameraId,
        sharedCameraIds: List<CameraId>,
        graphListener: GraphListener,
        isForegroundObserver: (Unit) -> Boolean,
    ): VirtualCamera {
        val result = VirtualCameraState(cameraId, graphListener, threads.globalScope)
        offerChecked(
            RequestOpen(
                result,
                sharedCameraIds,
                graphListener,
                isForegroundObserver
            )
        )
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
                pendingRequestOpens.removeAll {
                    it.virtualCamera.cameraId == closeRequest.activeCamera.cameraId
                }

                launch { closeRequest.activeCamera.close() }
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
                    launch { activeCamera.close() }
                }
                for (camera in activeCameras) {
                    camera.awaitClosed()
                }
                activeCameras.clear()
                pendingRequestOpens.clear()
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
            val camerasToClose = if (request.sharedCameraIds.isEmpty()) {
                activeCameras.filter { it.cameraId != cameraIdToOpen }
            } else {
                val allCameraIds =
                    (request.sharedCameraIds + request.virtualCamera.cameraId).toSet()
                activeCameras.filter { it.allCameraIds != allCameraIds }
            }

            if (camerasToClose.isNotEmpty()) {
                // Shutdown of cameras should always happen first (and suspend until complete)
                activeCameras.removeAll(camerasToClose)
                pendingRequestOpens.removeAll { requestOpen ->
                    camerasToClose.any { it.cameraId == requestOpen.virtualCamera.cameraId }
                }
                for (camera in camerasToClose) {
                    // TODO: This should be a dispatcher instead of scope.launch

                    launch {
                        // TODO: Figure out if this should be blocking or not. If we are directly
                        // invoking
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
            camera2ErrorProcessor.setActiveVirtualCamera(cameraIdToOpen, request.virtualCamera)
            var realCamera = activeCameras.firstOrNull { it.cameraId == cameraIdToOpen }
            if (realCamera == null) {
                val openResult =
                    openCameraWithRetry(
                        cameraIdToOpen,
                        request.sharedCameraIds,
                        request.isForegroundObserver,
                        scope = this
                    )
                if (openResult.activeCamera != null) {
                    realCamera = openResult.activeCamera
                    activeCameras.add(realCamera)
                } else {
                    request.virtualCamera.disconnect(openResult.lastCameraError)
                    requests.remove(request)
                }
                continue
            }

            // Stage 4: Attach camera(s)
            if (request.sharedCameraIds.isNotEmpty()) {
                // Both sharedCameraIds and activeCameras are small collections. Looping over them
                // in what equates to nested for-loops are actually going to be more efficient than
                // say, replacing activeCameras with a hashmap.
                if (request.sharedCameraIds.all { cameraId ->
                        activeCameras.any { it.cameraId == cameraId }
                    }) {
                    // If the camera of the request and the cameras it is shared with have been
                    // opened, we can connect the ActiveCameras.
                    realCamera.connectTo(request.virtualCamera)
                    connectPendingRequestOpens(request.sharedCameraIds)
                } else {
                    // Else, save the request in the pending request queue, and connect the request
                    // once other cameras are opened.
                    pendingRequestOpens.add(request)
                }
            } else {
                realCamera.connectTo(request.virtualCamera)
            }
            requests.remove(request)
        }
    }

    private suspend fun openCameraWithRetry(
        cameraId: CameraId,
        sharedCameraIds: List<CameraId>,
        isForegroundObserver: (Unit) -> Boolean,
        scope: CoroutineScope
    ): OpenVirtualCameraResult {
        // TODO: Figure out how 1-time permissions work, and see if they can be reset without
        //   causing the application process to restart.
        check(permissions.hasCameraPermission) { "Missing camera permissions!" }

        Log.debug { "Opening $cameraId with retries..." }
        val result = retryingCameraStateOpener.openCameraWithRetry(cameraId, isForegroundObserver)
        if (result.cameraState == null) {
            return OpenVirtualCameraResult(lastCameraError = result.errorCode)
        }
        return OpenVirtualCameraResult(
            activeCamera =
            ActiveCamera(
                androidCameraState = result.cameraState,
                allCameraIds = (sharedCameraIds + cameraId).toSet(),
                scope = scope,
                channel = requestQueue
            )
        )
    }

    private suspend fun connectPendingRequestOpens(cameraIds: List<CameraId>) {
        val requestOpensToRemove = mutableListOf<RequestOpen>()
        val requestOpens = pendingRequestOpens.filter {
            cameraIds.contains(it.virtualCamera.cameraId)
        }
        for (request in requestOpens) {
            // If the request is shared with this pending request, then we should be
            // able to connect this pending request too, since we don't allow
            // overlapping.
            val allCameraIds =
                listOf(request.virtualCamera.cameraId) + request.sharedCameraIds
            check(allCameraIds.all { cameraId -> activeCameras.any { it.cameraId == cameraId } })

            val realCamera =
                activeCameras.find { it.cameraId == request.virtualCamera.cameraId }
            checkNotNull(realCamera)
            realCamera.connectTo(request.virtualCamera)
            requestOpensToRemove.add(request)
        }
        pendingRequestOpens.removeAll(requestOpensToRemove)
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

    internal class ActiveCamera(
        private val androidCameraState: AndroidCameraState,
        internal val allCameraIds: Set<CameraId>,
        scope: CoroutineScope,
        channel: SendChannel<CameraRequest>
    ) {
        val cameraId: CameraId
            get() = androidCameraState.cameraId

        private val listenerJob: Job
        private var current: VirtualCameraState? = null

        private val wakelock =
            WakeLock(
                scope,
                timeout = 1000,
                callback = { channel.trySend(RequestClose(this)).isSuccess },
                // Every ActiveCamera is associated with an opened camera. We should ensure that we
                // issue a RequestClose eventually for every ActiveCamera created.
                //
                // A notable bug is b/264396089 where, because camera opens took too long, we didn't
                // acquire a WakeLockToken, and thereby not issuing the request to close camera
                // eventually.
                startTimeoutOnCreation = true
            )

        init {
            listenerJob =
                scope.launch {
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

    /**
     * There are 3 possible scenarios with [OpenVirtualCameraResult]. Suppose we denote the values
     * in pairs of ([activeCamera], [lastCameraError]):
     * - ([activeCamera], null): Camera opened without an issue.
     * - (null, [lastCameraError]): Camera opened failed and the last error was [lastCameraError].
     * - (null, null): Camera open didn't complete, likely due to CameraGraph being stopped or
     *   closed during the process.
     */
    private data class OpenVirtualCameraResult(
        val activeCamera: ActiveCamera? = null,
        val lastCameraError: CameraError? = null
    )
}
