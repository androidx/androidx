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

import android.os.Build
import android.view.Surface
import androidx.annotation.GuardedBy
import androidx.camera.camera2.pipe.CameraController
import androidx.camera.camera2.pipe.CameraController.ControllerState
import androidx.camera.camera2.pipe.CameraError
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraGraphId
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraSurfaceManager
import androidx.camera.camera2.pipe.GraphState
import androidx.camera.camera2.pipe.StreamGraph
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.config.Camera2ControllerScope
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.core.Threading.runBlockingCheckedOrNull
import androidx.camera.camera2.pipe.core.Threads
import androidx.camera.camera2.pipe.core.TimeSource
import androidx.camera.camera2.pipe.graph.GraphListener
import androidx.camera.camera2.pipe.internal.CameraStatusMonitor
import androidx.camera.camera2.pipe.internal.CameraStatusMonitor.CameraStatus
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * This represents the core state loop for a CameraGraph instance.
 *
 * A camera graph will receive start / stop signals from the application. When started, it will do
 * everything possible to bring up and maintain an active camera instance with the given
 * configuration.
 *
 * TODO: Reorganize these constructor parameters.
 */
@Camera2ControllerScope
internal class Camera2CameraController
@Inject
constructor(
    private val scope: CoroutineScope,
    private val threads: Threads,
    private val graphConfig: CameraGraph.Config,
    private val graphListener: GraphListener,
    private val cameraStatusMonitor: CameraStatusMonitor,
    private val captureSessionFactory: CaptureSessionFactory,
    private val captureSequenceProcessorFactory: Camera2CaptureSequenceProcessorFactory,
    private val virtualCameraManager: VirtualCameraManager,
    private val cameraSurfaceManager: CameraSurfaceManager,
    private val timeSource: TimeSource,
    override val cameraGraphId: CameraGraphId
) : CameraController {
    override val cameraId: CameraId
        get() = graphConfig.camera

    private val lock = Any()

    override var isForeground: Boolean
        get() = synchronized(lock) { _isForeground }
        set(value) = synchronized(lock) { _isForeground = value }

    @GuardedBy("lock") private var _isForeground: Boolean = true

    @GuardedBy("lock") private var controllerState: ControllerState = ControllerState.STOPPED

    @GuardedBy("lock")
    private var cameraStatus: CameraStatus = CameraStatus.CameraUnavailable(cameraId)

    @GuardedBy("lock") private var lastCameraError: CameraError? = null

    @GuardedBy("lock") private var restartJob: Job? = null

    private var currentCamera: VirtualCamera? = null
    private var currentSession: CaptureSessionState? = null
    private var currentSurfaceMap: Map<StreamId, Surface>? = null

    private var currentCameraStateJob: Job? = null
    private var cameraAvailabilityJob: Job? = null
    private var cameraPrioritiesJob: Job? = null

    init {
        cameraAvailabilityJob =
            scope.launch {
                cameraStatusMonitor.cameraAvailability.collect { cameraStatus ->
                    when (cameraStatus) {
                        is CameraStatus.CameraAvailable -> {
                            check(cameraStatus.cameraId == cameraId)
                            onCameraStatusChanged(cameraStatus)
                        }
                        is CameraStatus.CameraUnavailable -> {
                            check(cameraStatus.cameraId == cameraId)
                            onCameraStatusChanged(cameraStatus)
                        }
                    }
                }
            }

        cameraPrioritiesJob =
            scope.launch {
                cameraStatusMonitor.cameraPriorities.collect {
                    onCameraStatusChanged(CameraStatus.CameraPrioritiesChanged)
                }
            }
    }

    override fun start() {
        synchronized(lock) { startLocked() }
    }

    override fun stop() {
        synchronized(lock) { stopLocked() }
    }

    private fun restart(delayMs: Long) {
        synchronized(lock) {
            restartJob?.cancel()
            restartJob =
                scope.launch {
                    delay(delayMs)
                    synchronized(lock) {
                        if (
                            controllerState != ControllerState.CLOSED &&
                                controllerState != ControllerState.STOPPING &&
                                controllerState != ControllerState.STOPPED
                        ) {
                            controllerState
                            stopLocked()
                            startLocked()
                        }
                    }
                }
        }
    }

    @GuardedBy("lock")
    private fun startLocked() {
        if (controllerState == ControllerState.CLOSED) {
            Log.info { "Ignoring start(): Camera2CameraController is already closed" }
            return
        } else if (controllerState == ControllerState.STARTED) {
            Log.warn { "Ignoring start(): Camera2CameraController is already started" }
            return
        }
        lastCameraError = null
        val camera =
            virtualCameraManager.open(
                graphConfig.camera,
                graphConfig.sharedCameraIds,
                graphListener,
            ) { _ ->
                isForeground
            }
        if (camera == null) {
            Log.error { "Failed to start Camera2CameraController: Open request submission failed" }
            return
        }

        check(currentCamera == null)
        check(currentSession == null)

        currentCamera = camera
        val session =
            CaptureSessionState(
                graphListener,
                captureSessionFactory,
                captureSequenceProcessorFactory,
                cameraSurfaceManager,
                timeSource,
                graphConfig.flags,
                scope
            )
        currentSession = session

        val surfaces: Map<StreamId, Surface>? = currentSurfaceMap
        if (surfaces != null) {
            session.configureSurfaceMap(surfaces)
        }

        controllerState = ControllerState.STARTED
        Log.debug { "Started Camera2CameraController" }
        currentCameraStateJob?.cancel()
        currentCameraStateJob = scope.launch { bindSessionToCamera() }
    }

    @GuardedBy("lock")
    private fun stopLocked() {
        if (controllerState == ControllerState.CLOSED) {
            Log.warn { "Ignoring stop(): Camera2CameraController is already closed" }
            return
        } else if (
            controllerState == ControllerState.STOPPING ||
                controllerState == ControllerState.STOPPED
        ) {
            Log.warn { "Ignoring stop(): CameraController already stopping or stopped" }
            return
        }

        val camera = currentCamera
        val session = currentSession

        currentCamera = null
        currentSession = null

        controllerState = ControllerState.STOPPING
        Log.debug { "Stopping Camera2CameraController" }
        disconnectSessionAndCamera(session, camera)
    }

    private fun onCameraStatusChanged(cameraStatus: CameraStatus) {
        val shouldRestart =
            synchronized(lock) {
                Log.debug { "$this ($cameraId) camera status changed to $cameraStatus" }
                if (
                    cameraStatus is CameraStatus.CameraAvailable ||
                        cameraStatus is CameraStatus.CameraUnavailable
                ) {
                    this@Camera2CameraController.cameraStatus = cameraStatus
                }

                var shouldRestart = false
                when (controllerState) {
                    ControllerState.DISCONNECTED ->
                        if (
                            cameraStatus is CameraStatus.CameraAvailable ||
                                cameraStatus is CameraStatus.CameraPrioritiesChanged
                        ) {
                            shouldRestart = true
                        }
                    ControllerState.ERROR ->
                        if (
                            cameraStatus is CameraStatus.CameraAvailable &&
                                lastCameraError != CameraError.ERROR_GRAPH_CONFIG
                        ) {
                            shouldRestart = true
                        }
                }
                shouldRestart
            }
        if (!shouldRestart) {
            Log.debug {
                "Camera status changed but not restarting: " +
                    "Controller state = $controllerState, camera status = $cameraStatus."
            }
            return
        }
        Log.debug { "Restarting Camera2CameraController" }
        val delayMs = if (graphConfig.flags.enableRestartDelays) 700L else 0L
        restart(delayMs)
    }

    override fun close(): Unit =
        synchronized(lock) {
            if (controllerState == ControllerState.CLOSED) {
                return
            }
            controllerState = ControllerState.CLOSED
            Log.debug { "Closed Camera2CameraController" }

            val camera = currentCamera
            val session = currentSession

            currentCamera = null
            currentSession = null

            currentCameraStateJob?.cancel()
            currentCameraStateJob = null
            cameraAvailabilityJob?.cancel()
            cameraAvailabilityJob = null
            cameraPrioritiesJob?.cancel()
            cameraPrioritiesJob = null
            cameraStatusMonitor.close()

            disconnectSessionAndCamera(session, camera)
            if (graphConfig.flags.closeCameraDeviceOnClose) {
                Log.debug { "Quirk: Closing all camera devices" }
                virtualCameraManager.closeAll()
            }
        }

    override fun updateSurfaceMap(surfaceMap: Map<StreamId, Surface>) {
        // TODO: Add logic to decide if / when to re-configure the Camera2 CaptureSession.
        synchronized(lock) {
                if (controllerState == ControllerState.CLOSED) {
                    return
                }
                currentSurfaceMap = surfaceMap
                currentSession
            }
            ?.configureSurfaceMap(surfaceMap)
    }

    override fun getOutputLatency(streamId: StreamId?): StreamGraph.OutputLatency? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return currentSession?.getRealtimeCaptureLatency()?.let {
                // Convert output latency to ns for consistency with stall duration.
                val captureLatencyNs = it.captureLatency * MS_TO_NS
                val processingLatencyNs = it.processingLatency * MS_TO_NS
                StreamGraph.OutputLatency(captureLatencyNs, processingLatencyNs)
            }
        }
        return null
    }

    private suspend fun bindSessionToCamera() {
        val camera: VirtualCamera?
        val session: CaptureSessionState?

        synchronized(lock) {
            camera = currentCamera
            session = currentSession
        }

        if (camera != null && session != null) {
            camera.state.collect { cameraState ->
                when (cameraState) {
                    is CameraStateOpen -> {
                        session.cameraDevice = cameraState.cameraDevice
                    }
                    is CameraStateClosing -> {
                        session.disconnect()
                    }
                    is CameraStateClosed -> {
                        session.disconnect()
                        onStateClosed(cameraState)
                    }
                    else -> {
                        // Do nothing
                    }
                }
            }
        }
    }

    private fun onStateClosed(cameraState: CameraStateClosed) =
        synchronized(lock) {
            if (cameraState.cameraErrorCode != null) {
                if (
                    cameraState.cameraErrorCode == CameraError.ERROR_CAMERA_DISCONNECTED ||
                        cameraState.cameraErrorCode == CameraError.ERROR_CAMERA_IN_USE ||
                        cameraState.cameraErrorCode == CameraError.ERROR_CAMERA_LIMIT_EXCEEDED
                ) {
                    controllerState = ControllerState.DISCONNECTED
                    Log.debug { "Camera2CameraController is disconnected" }
                    if (
                        Build.VERSION.SDK_INT in
                            (Build.VERSION_CODES.Q..Build.VERSION_CODES.S_V2) && _isForeground
                    ) {
                        Log.debug {
                            "Quirk for multi-resume activated: " +
                                "Emulating camera priorities changed to kickoff potential restart."
                        }
                        onCameraStatusChanged(CameraStatus.CameraPrioritiesChanged)
                    }
                } else {
                    controllerState = ControllerState.ERROR
                    Log.debug {
                        "Camera2CameraController encountered error: ${cameraState.cameraErrorCode}"
                    }

                    // When camera is closed under error, it is possible for the camera availability
                    // callback to indicate camera as available, before we finish processing
                    // (receiving) the camera error. Therefore, if we have an error, but we think
                    // the camera is available, we should attempt a retry.
                    // Please refer to b/362902859 for details.
                    if (
                        cameraStatus is CameraStatus.CameraAvailable &&
                            cameraState.cameraErrorCode != CameraError.ERROR_GRAPH_CONFIG
                    ) {
                        onCameraStatusChanged(cameraStatus)
                    }
                }
                lastCameraError = cameraState.cameraErrorCode
            } else {
                controllerState = ControllerState.STOPPED
            }
        }

    private fun disconnectSessionAndCamera(session: CaptureSessionState?, camera: VirtualCamera?) {
        val deferred =
            scope.async {
                session?.disconnect()
                camera?.disconnect()
            }
        if (
            graphConfig.flags.abortCapturesOnStop ||
                graphConfig.flags.closeCaptureSessionOnDisconnect
        ) {
            // It seems that on certain devices, CameraCaptureSession.close() can block for an
            // extended period of time [1]. Wrap the await call with a timeout to prevent us from
            // getting blocked for too long.
            //
            // [1] b/307594946 - [ANR] at Camera2CameraController.disconnectSessionAndCamera
            runBlockingCheckedOrNull(threads.backgroundDispatcher, DISCONNECT_TIMEOUT_MS) {
                deferred.await()
            }
                ?: run {
                    Log.warn { "Timeout when disconnecting session and camera for $session" }
                    Log.info { "Force finalizing current capture session" }
                    session?.finalizeSession(delayMs = 0)
                    graphListener.onGraphError(
                        GraphState.GraphStateError(
                            CameraError.ERROR_CAMERA_DEVICE,
                            willAttemptRetry = false
                        )
                    )
                }
        }
    }

    companion object {
        private const val DISCONNECT_TIMEOUT_MS = 5000L // 5s
        private const val MS_TO_NS = 1_000_000
    }
}
