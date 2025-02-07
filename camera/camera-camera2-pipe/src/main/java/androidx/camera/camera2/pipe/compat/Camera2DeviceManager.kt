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

package androidx.camera.camera2.pipe.compat

import androidx.annotation.VisibleForTesting
import androidx.camera.camera2.pipe.CameraError
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.GraphState
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.core.Permissions
import androidx.camera.camera2.pipe.core.PruningProcessingQueue
import androidx.camera.camera2.pipe.core.PruningProcessingQueue.Companion.processIn
import androidx.camera.camera2.pipe.core.Threads
import androidx.camera.camera2.pipe.core.Token
import androidx.camera.camera2.pipe.core.WakeLock
import androidx.camera.camera2.pipe.graph.GraphListener
import androidx.camera.camera2.pipe.graph.GraphRequestProcessor
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal sealed class CameraRequest

internal data class RequestOpen(
    val virtualCamera: VirtualCameraState,
    val sharedCameraIds: List<CameraId>,
    val graphListener: GraphListener,
    val isPrewarm: Boolean,
    val isForegroundObserver: (Unit) -> Boolean,
) : CameraRequest()

/**
 * Sends a request to close an active camera. Note: RequestOpen() & RequestClose() may not be
 * executed sequentially, as the camera may take a while to be fully opened, and RequestClose()
 * might execute in parallel.
 */
internal data class RequestClose(val activeCamera: ActiveCamera) : CameraRequest()

internal class RequestCloseById(val activeCameraId: CameraId) : CameraRequest() {
    val deferred: CompletableDeferred<Unit> = CompletableDeferred()

    override fun toString() = "RequestCloseById($activeCameraId)"
}

internal class RequestCloseAll() : CameraRequest() {
    val deferred: CompletableDeferred<Unit> = CompletableDeferred()

    override fun toString() = "RequestCloseAll"
}

internal object NoOpGraphListener : GraphListener {
    override fun onGraphStarted(requestProcessor: GraphRequestProcessor) {}

    override fun onGraphStopped(requestProcessor: GraphRequestProcessor?) {}

    override fun onGraphModified(requestProcessor: GraphRequestProcessor) {}

    override fun onGraphError(graphStateError: GraphState.GraphStateError) {}
}

internal interface Camera2DeviceManager {
    /**
     * Issue a request to open the specified camera. The camera will be delivered through
     * [VirtualCamera.state] when opened, and the state will continue to provide updates to the
     * state of the camera. If shared camera IDs are specified, the cameras won't be provided until
     * all cameras are opened.
     */
    fun open(
        cameraId: CameraId,
        sharedCameraIds: List<CameraId>,
        graphListener: GraphListener,
        isPrewarm: Boolean,
        isForegroundObserver: (Unit) -> Boolean,
    ): VirtualCamera?

    /**
     * Connects and starts the underlying camera. Once the active camera timeout elapses and it
     * hasn't been utilized, the camera is closed.
     */
    fun prewarm(cameraId: CameraId)

    /** Submits a request to close the underlying camera. */
    fun close(cameraId: CameraId): Deferred<Unit>

    /** Instructs Camera2DeviceManager to close all cameras. */
    fun closeAll(): Deferred<Unit>
}

internal class ActiveCamera(
    private val androidCameraState: AndroidCameraState,
    internal val allCameraIds: Set<CameraId>,
    scope: CoroutineScope,
    closeCallback: (ActiveCamera) -> Unit
) {
    val cameraId: CameraId
        get() = androidCameraState.cameraId

    private var current: VirtualCameraState? = null

    private val wakelock =
        WakeLock(
            scope,
            timeout = 1000,
            callback = { closeCallback(this) },
            // Every ActiveCamera is associated with an opened camera. We should ensure that we
            // issue a RequestClose eventually for every ActiveCamera created.
            //
            // A notable bug is b/264396089 where, because camera opens took too long, we didn't
            // acquire a WakeLockToken, and thereby not issuing the request to close camera
            // eventually.
            startTimeoutOnCreation = true
        )

    init {
        scope.launch {
            androidCameraState.state.first { it is CameraStateClosing || it is CameraStateClosed }
            wakelock.release()
        }
    }

    // Acquire this ActiveCamera. This ensures the camera stay opened for as long as the token is
    // held. This is important for camera open scenarios where the device manager should acquire
    // the ActiveCamera for the duration under which it's processing an open request.
    fun acquire() = wakelock.acquire()

    // TODO: b/389758537, b/390530866 - Make Token non-nullable. If we cannot acquire a token, the
    //  ActiveCamera has issued a RequestClose for this ActiveCamera already.
    suspend fun connectTo(virtualCameraState: VirtualCameraState, token: Token?) {
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

    override fun toString(): String =
        "ActiveCamera(cameraId=$cameraId)@${super.hashCode().toString(16)}"
}

/**
 * PruningCamera2DeviceManager is an implementation of [Camera2DeviceManager] that actively prunes
 * incoming camera requests before processing them. It does this through a proven sequence of logic
 * that reduces the pending requests to an equivalent but shorter sequence of requests, allowing the
 * internal request processing channel to stay at a size within a proven upper bound, and the actual
 * request processing to be done optimally and efficiently.
 */
@Singleton
internal class PruningCamera2DeviceManager
@Inject
constructor(
    private val permissions: Permissions,
    private val retryingCameraStateOpener: RetryingCameraStateOpener,
    private val camera2DeviceCloser: Camera2DeviceCloser,
    private val camera2ErrorProcessor: Camera2ErrorProcessor,
    val threads: Threads
) : Camera2DeviceManager {
    private val scope = threads.globalScope

    private val queue =
        PruningProcessingQueue<CameraRequest>(prune = ::prune) { process(it) }.processIn(scope)
    private val activeCameras: MutableSet<ActiveCamera> = mutableSetOf()

    // PendingRequestOpen stores the context information for the pending RequestOpens to be
    // connected in concurrent camera scenarios. It contains the request itself, the active camera
    // it should be connected with, and the usage token for the active camera. The token should
    // always be closed if the context is removed without being connected to a VirtualCamera.
    private class PendingRequestOpen(
        val request: RequestOpen,
        val activeCamera: ActiveCamera,
        val token: Token,
    )

    private val pendingRequestOpens = mutableListOf<PendingRequestOpen>()

    override fun open(
        cameraId: CameraId,
        sharedCameraIds: List<CameraId>,
        graphListener: GraphListener,
        isPrewarm: Boolean,
        isForegroundObserver: (Unit) -> Boolean
    ): VirtualCamera? {
        val result = VirtualCameraState(cameraId, graphListener, scope)
        if (
            !queue.tryEmit(
                RequestOpen(result, sharedCameraIds, graphListener, isPrewarm, isForegroundObserver)
            )
        ) {
            // This should generally not happen unless someone attempts to open a camera after
            // CameraPipe shutdown, or we somehow run out of memory, since this class lives for the
            // duration of the application process.
            Log.error { "Camera open request failed for $cameraId!" }
            graphListener.onGraphError(
                GraphState.GraphStateError(
                    CameraError.ERROR_CAMERA_OPENER,
                    willAttemptRetry = false
                )
            )
            return null
        }
        return result
    }

    override fun prewarm(cameraId: CameraId) {
        open(
            cameraId = cameraId,
            sharedCameraIds = emptyList(),
            graphListener = NoOpGraphListener,
            isPrewarm = true,
        ) { _ ->
            false
        }
    }

    override fun close(cameraId: CameraId): Deferred<Unit> {
        val request = RequestCloseById(cameraId)
        if (!queue.tryEmit(request)) {
            Log.error { "Camera close by ID request failed for $cameraId!" }
            request.deferred.complete(Unit)
        }
        return request.deferred
    }

    override fun closeAll(): Deferred<Unit> {
        val request = RequestCloseAll()
        if (!queue.tryEmit(request)) {
            Log.error { "Camera close all request failed!" }
            request.deferred.complete(Unit)
        }
        return request.deferred
    }

    @VisibleForTesting
    internal fun prune(requests: MutableList<CameraRequest>) {
        // Step 1: Prioritize RequestClose - place them at the front of the queue.
        val requestCloses = requests.filter { it is RequestClose }
        requests.removeAll(requestCloses)
        // Move the RequestCloses to the front of the queue (in order) to be processed first.
        for (request in requestCloses.reversed()) {
            requests.add(0, request)
        }

        // Step 2: Handle RequestCloseAll. The last one would nullify all preceding requests.
        val lastRequestCloseAllIdx = requests.indexOfLast { it is RequestCloseAll }
        if (lastRequestCloseAllIdx > 0) {
            val lastRequestCloseAll = requests[lastRequestCloseAllIdx] as RequestCloseAll
            repeat(lastRequestCloseAllIdx) {
                val request = requests.removeAt(0)

                // When RequestCloseById or RequestCloseAll is removed, make sure to complete their
                // deferred when the latter RequestCloseAll is completed.
                val deferredToPropagate =
                    when (request) {
                        is RequestCloseById -> request.deferred
                        is RequestCloseAll -> request.deferred
                        else -> null
                    }
                if (deferredToPropagate != null) {
                    lastRequestCloseAll.deferred.invokeOnCompletion {
                        deferredToPropagate.complete(Unit)
                    }
                }

                request.onRemoved()
            }
        }

        // Step 3: Handle RequestOpen and RequestCloseById pruning.
        val prunedIndices = mutableSetOf<Int>()
        for ((idx, request) in requests.withIndex()) {
            val prunedByIdx =
                when (request) {
                    is RequestOpen -> {
                        // There are 2 cases where a RequestOpen can be pruned by a latter request:
                        //
                        // 1. RequestCloseById: If any of its cameras (including itself and the
                        //    shared cameras) would be closed by it, then this RequestOpen could be
                        //    pruned.
                        // 2. RequestOpen: If the latter RequestOpen requests the same camera, or
                        //    it requests a different camera and it doesn't need the camera to be
                        //    opened by the current RequestOpen.
                        val cameraId = request.virtualCamera.cameraId
                        val allCameraIds = (request.sharedCameraIds + cameraId).toSet()

                        requests.firstFromIndexOrNull(idx + 1) {
                            when (it) {
                                is RequestCloseById -> allCameraIds.contains(it.activeCameraId)
                                is RequestOpen -> {
                                    // A prewarm RequestOpen should never prune a regular
                                    // RequestOpen. Here the logic is:
                                    //
                                    // - If the current request is a prewarm, it's always prunable.
                                    // - Or, if the latter request is NOT a prewarm.
                                    val isPrunableRequestOpen = request.isPrewarm || !it.isPrewarm
                                    val cameraId2 = it.virtualCamera.cameraId
                                    val allCameraIds2 = (it.sharedCameraIds + cameraId2).toSet()
                                    isPrunableRequestOpen &&
                                        (cameraId == cameraId2 || allCameraIds != allCameraIds2)
                                }
                                else -> false
                            }
                        }
                    }
                    is RequestCloseById ->
                        // If there are several RequestCloseByIds with identical camera IDs, we can
                        // just leave the latter one.
                        requests.firstFromIndexOrNull(idx + 1) {
                            it is RequestCloseById && it.activeCameraId == request.activeCameraId
                        }
                    else -> null
                }
            if (prunedByIdx != null) {
                val prunedByRequest = requests[prunedByIdx]
                Log.debug { "$request is pruned by $prunedByRequest" }
                prunedIndices.add(idx)

                // Make sure to complete the deferred of the pruned RequestCloseById when the latter
                // RequestCloseById is completed.
                if (request is RequestCloseById && prunedByRequest is RequestCloseById) {
                    prunedByRequest.deferred.invokeOnCompletion { request.deferred.complete(Unit) }
                }
            }
        }
        requests.removeIndices(prunedIndices).forEach { it.onRemoved() }
    }

    private fun CameraRequest.onRemoved() {
        if (this is RequestOpen) {
            virtualCamera.disconnect()
        }
    }

    private suspend fun process(request: CameraRequest) {
        when (request) {
            is RequestOpen -> processRequestOpen(request)
            is RequestClose -> processRequestClose(request)
            is RequestCloseById -> processRequestCloseById(request)
            is RequestCloseAll -> processRequestCloseAll(request)
        }
    }

    private suspend fun processRequestOpen(request: RequestOpen) {
        val cameraIdToOpen = request.virtualCamera.cameraId
        Log.info { "PruningCamera2DeviceManager#processRequestOpen($cameraIdToOpen)" }

        val camerasToClose =
            if (request.sharedCameraIds.isEmpty()) {
                activeCameras.filter { it.cameraId != cameraIdToOpen }
            } else {
                val allCameraIds =
                    (request.sharedCameraIds + request.virtualCamera.cameraId).toSet()
                activeCameras.filter { it.allCameraIds != allCameraIds }
            }

        // Step 1: Close the cameras needed to be closed.
        if (camerasToClose.isNotEmpty()) {
            // Shutdown of cameras should always happen first (and suspend until complete)
            activeCameras.removeAll(camerasToClose)
            disconnectPendingRequestOpens(
                pendingRequestOpens.filter { camerasToClose.contains(it.activeCamera) }
            )
            for (camera in camerasToClose) {
                camera.close()
            }
            for (realCamera in camerasToClose) {
                realCamera.awaitClosed()
            }
        }

        // Step 2: Open the camera if not opened already.
        camera2ErrorProcessor.setActiveVirtualCamera(cameraIdToOpen, request.virtualCamera)
        val result = retrieveActiveCamera(cameraIdToOpen, request)
        if (result == null) {
            Log.error { "Failed to retrieve active camera for $cameraIdToOpen" }
            return
        }
        val realCamera = result.activeCamera
        val realCameraToken = result.token

        // Step 3: Connect the opened camera(s).
        if (request.sharedCameraIds.isNotEmpty()) {
            // Both sharedCameraIds and pendingRequestOpenContexts are small collections. Looping
            // over them in what equates to nested for-loops are actually going to be more efficient
            // than say, replacing activeCameras with a hashmap.
            if (
                request.sharedCameraIds.all { cameraId ->
                    pendingRequestOpens.any { it.activeCamera.cameraId == cameraId }
                }
            ) {
                // If the camera of the request and the cameras it is shared with have been
                // opened, we can connect the ActiveCameras.
                check(!request.isPrewarm)
                realCamera.connectTo(request.virtualCamera, realCameraToken)
                connectPendingRequestOpens(request.sharedCameraIds.toSet())
            } else {
                // Else, save the request in the pending request queue, and connect the request
                // once other cameras are opened.
                pendingRequestOpens.add(PendingRequestOpen(request, realCamera, realCameraToken))
            }
        } else {
            if (!request.isPrewarm) {
                realCamera.connectTo(request.virtualCamera, realCameraToken)
            } else {
                // Since prewarm requests don't connect to VirtualCameras, make sure to release our
                // acquired token here to allow the camera to be closed if unused after a while.
                realCameraToken.release()
            }
        }
    }

    private suspend fun processRequestClose(request: RequestClose) {
        val cameraId = request.activeCamera.cameraId
        Log.info { "PruningCamera2DeviceManager#processRequestClose($cameraId)" }

        if (activeCameras.contains(request.activeCamera)) {
            activeCameras.remove(request.activeCamera)
        }

        // Edge case: There is a possibility that we receive RequestClose after a RequestOpen for
        // concurrent cameras has been processed. As such, we don't want to close the ActiveCamera
        // newly created by the RequestOpen, but only the one RequestClose is aiming to close.
        disconnectPendingRequestOpens(
            pendingRequestOpens.filter { it.activeCamera == request.activeCamera }
        )
        request.activeCamera.close()
        request.activeCamera.awaitClosed()
    }

    private suspend fun processRequestCloseById(request: RequestCloseById) {
        val cameraId = request.activeCameraId
        Log.info { "PruningCamera2DeviceManager#processRequestCloseById(${request.activeCameraId}" }

        disconnectPendingRequestOpens(
            pendingRequestOpens.filter { it.request.virtualCamera.cameraId == cameraId }
        )
        val activeCamera = activeCameras.firstOrNull { it.cameraId == cameraId }
        if (activeCamera != null) {
            activeCameras.remove(activeCamera)
            activeCamera.close()
            activeCamera.awaitClosed()
        }
        request.deferred.complete(Unit)
    }

    private suspend fun processRequestCloseAll(requestCloseAll: RequestCloseAll) {
        Log.info { "PruningCamera2DeviceManager#processRequestCloseAll()" }

        disconnectPendingRequestOpens(pendingRequestOpens)
        for (activeCamera in activeCameras) {
            activeCamera.close()
        }
        for (activeCamera in activeCameras) {
            activeCamera.awaitClosed()
        }
        activeCameras.clear()
        requestCloseAll.deferred.complete(Unit)
    }

    private suspend fun retrieveActiveCamera(
        cameraId: CameraId,
        requestOpen: RequestOpen,
    ): RetrieveActiveCameraResult? {
        var realCamera: ActiveCamera? = null
        var realCameraToken: Token? = null
        for (activeCamera in activeCameras) {
            if (activeCamera.cameraId == cameraId) {
                // Important: When we retrieve an active camera, there is a chance it has already
                // reached its idle timeout, but we haven't processed the close request and remove
                // it from the list. It's also possible that after we fetch this camera, this camera
                // then gets closed in parallel by the idle timeout. Therefore, here we should
                // acquire a token from this active camera to mark it as used and keep it opened.
                val token = activeCamera.acquire()
                if (token != null) {
                    realCamera = activeCamera
                    realCameraToken = token
                    break
                } else {
                    // This ActiveCamera is already disconnected (i.e., WakeLock closed). Make sure
                    // the camera is closed before reopening.
                    activeCamera.close()
                    activeCamera.awaitClosed()
                    activeCameras.remove(activeCamera)
                }
            }
        }
        if (realCamera == null) {
            val openResult =
                openCameraWithRetry(
                    cameraId,
                    requestOpen.sharedCameraIds,
                    requestOpen.isForegroundObserver,
                    scope,
                )
            when (openResult) {
                is OpenVirtualCameraResult.Success -> {
                    Log.info { "PruningCameraDeviceManager: $cameraId opened successfully" }
                    realCamera = openResult.activeCamera
                    // Acquire a token to mark this active camera as used.
                    realCameraToken = checkNotNull(realCamera.acquire())
                    activeCameras.add(realCamera)
                }
                is OpenVirtualCameraResult.Error -> {
                    Log.info { "PruningCameraDeviceManager: Failed to open $cameraId" }
                    requestOpen.virtualCamera.disconnect(openResult.lastCameraError)
                    return null
                }
            }
        }
        return RetrieveActiveCameraResult(realCamera, checkNotNull(realCameraToken))
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
        val result =
            retryingCameraStateOpener.openCameraWithRetry(
                cameraId,
                camera2DeviceCloser,
                isForegroundObserver
            )
        if (result.cameraState == null) {
            return OpenVirtualCameraResult.Error(result.errorCode)
        }
        return OpenVirtualCameraResult.Success(
            activeCamera =
                ActiveCamera(
                    androidCameraState = result.cameraState,
                    allCameraIds = (sharedCameraIds + cameraId).toSet(),
                    scope = scope,
                    closeCallback = { activeCamera -> queue.tryEmit(RequestClose(activeCamera)) },
                )
        )
    }

    private suspend fun connectPendingRequestOpens(cameraIds: Set<CameraId>) {
        val filteredPendingRequestOpens =
            pendingRequestOpens.filter { cameraIds.contains(it.request.virtualCamera.cameraId) }
        for (pendingRequestOpen in filteredPendingRequestOpens) {
            val request = pendingRequestOpen.request

            // If the request is shared with this pending request, then we should be able to connect
            // this pending request too, since we don't allow overlapping.
            val allCameraIds = listOf(request.virtualCamera.cameraId) + request.sharedCameraIds
            check(allCameraIds.all { cameraId -> activeCameras.any { it.cameraId == cameraId } })

            pendingRequestOpen.activeCamera.connectTo(
                request.virtualCamera,
                pendingRequestOpen.token
            )
            pendingRequestOpens.remove(pendingRequestOpen)
        }
    }

    private suspend fun disconnectPendingRequestOpens(
        pendingRequestOpensToDisconnect: List<PendingRequestOpen>,
    ) {
        for (pendingRequestOpen in pendingRequestOpensToDisconnect) {
            pendingRequestOpen.token.release()
            pendingRequestOpens.remove(pendingRequestOpen)
        }
    }

    private inline fun <T> List<T>.firstFromIndexOrNull(
        index: Int,
        predicate: (T) -> Boolean
    ): Int? {
        for (i in index..size - 1) {
            if (predicate(get(i))) {
                return i
            }
        }
        return null
    }

    private fun <T> MutableList<T>.removeIndices(indices: Set<Int>): List<T> {
        val removedElements = mutableListOf<T>()
        for (idx in indices.sorted()) {
            removedElements.add(removeAt(idx - removedElements.size))
        }
        return removedElements
    }

    private class RetrieveActiveCameraResult(val activeCamera: ActiveCamera, val token: Token)

    private sealed interface OpenVirtualCameraResult {
        data class Success(val activeCamera: ActiveCamera) : OpenVirtualCameraResult

        data class Error(val lastCameraError: CameraError?) : OpenVirtualCameraResult
    }
}

// TODO: b/307396261 - A queue depth of 64 was deemed necessary in b/276051078 and b/307396261 where
//  a flood of requests can cause the queue depth to grow larger than anticipated. Rewrite the
//  camera manager such that it handles these abnormal scenarios more robustly.
private const val requestQueueDepth = 64

@Suppress("EXPERIMENTAL_API_USAGE")
@Singleton
internal class Camera2DeviceManagerImpl
@Inject
constructor(
    private val permissions: Permissions,
    private val retryingCameraStateOpener: RetryingCameraStateOpener,
    private val camera2DeviceCloser: Camera2DeviceCloser,
    private val camera2ErrorProcessor: Camera2ErrorProcessor,
    private val threads: Threads
) : Camera2DeviceManager {
    // TODO: Consider rewriting this as a MutableSharedFlow
    private val requestQueue: Channel<CameraRequest> = Channel(requestQueueDepth)
    private val activeCameras: MutableSet<ActiveCamera> = mutableSetOf()
    private val pendingRequestOpens = mutableListOf<RequestOpen>()

    init {
        threads.globalScope.launch(CoroutineName("CXCP-Camera2DeviceManager")) { requestLoop() }
    }

    override fun open(
        cameraId: CameraId,
        sharedCameraIds: List<CameraId>,
        graphListener: GraphListener,
        isPrewarm: Boolean,
        isForegroundObserver: (Unit) -> Boolean,
    ): VirtualCamera? {
        val result = VirtualCameraState(cameraId, graphListener, threads.globalScope)
        if (
            !offerChecked(
                RequestOpen(result, sharedCameraIds, graphListener, isPrewarm, isForegroundObserver)
            )
        ) {
            Log.error { "Camera open request failed: Camera2DeviceManagerImpl queue size exceeded" }
            graphListener.onGraphError(
                GraphState.GraphStateError(
                    CameraError.ERROR_CAMERA_OPENER,
                    willAttemptRetry = false
                )
            )
            return null
        }
        return result
    }

    override fun prewarm(cameraId: CameraId) {
        open(
            cameraId = cameraId,
            sharedCameraIds = emptyList(),
            graphListener = NoOpGraphListener,
            isPrewarm = true,
        ) { _ ->
            false
        }
    }

    override fun close(cameraId: CameraId): Deferred<Unit> {
        val request = RequestCloseById(cameraId)
        offerChecked(request)
        request.deferred.complete(Unit)
        return request.deferred
    }

    override fun closeAll(): Deferred<Unit> {
        val request = RequestCloseAll()
        if (!offerChecked(request)) {
            Log.warn { "Failed to close all cameras: Close request submission failed" }
        }
        request.deferred.complete(Unit)
        return request.deferred
    }

    private fun offerChecked(request: CameraRequest): Boolean {
        return requestQueue.trySend(request).isSuccess
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

            // Ensures the closure of a camera device happens after any preceding RequestOpen().
            val closeRequestById = requests.firstOrNull()
            if (closeRequestById != null && closeRequestById is RequestCloseById) {
                requests.remove(closeRequestById)
                pendingRequestOpens.removeAll {
                    it.virtualCamera.cameraId == closeRequestById.activeCameraId
                }
                val activeCamera =
                    activeCameras.firstOrNull { it.cameraId == closeRequestById.activeCameraId }
                if (activeCamera != null) {
                    activeCameras.remove(activeCamera)
                    launch { activeCamera.close() }
                    activeCamera.awaitClosed()
                }
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
            if (request.isPrewarm) {
                check(request.sharedCameraIds.isEmpty()) {
                    "Prewarming concurrent cameras is not supported"
                }
            }

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
            val camerasToClose =
                if (request.sharedCameraIds.isEmpty()) {
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
                if (
                    request.sharedCameraIds.all { cameraId ->
                        activeCameras.any { it.cameraId == cameraId }
                    }
                ) {
                    // If the camera of the request and the cameras it is shared with have been
                    // opened, we can connect the ActiveCameras.
                    check(!request.isPrewarm)
                    realCamera.connectTo(request.virtualCamera, realCamera.acquire())
                    connectPendingRequestOpens(request.sharedCameraIds)
                } else {
                    // Else, save the request in the pending request queue, and connect the request
                    // once other cameras are opened.
                    pendingRequestOpens.add(request)
                }
            } else {
                if (!request.isPrewarm) {
                    realCamera.connectTo(request.virtualCamera, realCamera.acquire())
                }
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
        val result =
            retryingCameraStateOpener.openCameraWithRetry(
                cameraId,
                camera2DeviceCloser,
                isForegroundObserver
            )
        if (result.cameraState == null) {
            return OpenVirtualCameraResult(lastCameraError = result.errorCode)
        }
        return OpenVirtualCameraResult(
            activeCamera =
                ActiveCamera(
                    androidCameraState = result.cameraState,
                    allCameraIds = (sharedCameraIds + cameraId).toSet(),
                    scope = scope,
                    closeCallback = { activeCamera ->
                        requestQueue.trySend(RequestClose(activeCamera)).isSuccess
                    }
                )
        )
    }

    private suspend fun connectPendingRequestOpens(cameraIds: List<CameraId>) {
        val requestOpensToRemove = mutableListOf<RequestOpen>()
        val requestOpens =
            pendingRequestOpens.filter { cameraIds.contains(it.virtualCamera.cameraId) }
        for (request in requestOpens) {
            // If the request is shared with this pending request, then we should be
            // able to connect this pending request too, since we don't allow
            // overlapping.
            val allCameraIds = listOf(request.virtualCamera.cameraId) + request.sharedCameraIds
            check(allCameraIds.all { cameraId -> activeCameras.any { it.cameraId == cameraId } })

            val realCamera = activeCameras.find { it.cameraId == request.virtualCamera.cameraId }
            checkNotNull(realCamera)
            realCamera.connectTo(request.virtualCamera, realCamera.acquire())
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
