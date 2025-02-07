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
import androidx.annotation.VisibleForTesting
import androidx.camera.camera2.pipe.CameraController
import androidx.camera.camera2.pipe.CameraController.ControllerState
import androidx.camera.camera2.pipe.CameraError
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraGraphId
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraSurfaceManager
import androidx.camera.camera2.pipe.StreamGraph
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.SurfaceTracker
import androidx.camera.camera2.pipe.config.Camera2ControllerScope
import androidx.camera.camera2.pipe.core.DurationNs
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.core.Threads
import androidx.camera.camera2.pipe.core.TimeSource
import androidx.camera.camera2.pipe.core.TimestampNs
import androidx.camera.camera2.pipe.graph.GraphListener
import androidx.camera.camera2.pipe.internal.CameraStatusMonitor
import androidx.camera.camera2.pipe.internal.CameraStatusMonitor.CameraStatus
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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
    private val surfaceTracker: SurfaceTracker,
    private val cameraStatusMonitor: CameraStatusMonitor,
    private val captureSessionFactory: CaptureSessionFactory,
    private val captureSequenceProcessorFactory: Camera2CaptureSequenceProcessorFactory,
    private val camera2DeviceManager: Camera2DeviceManager,
    private val cameraSurfaceManager: CameraSurfaceManager,
    private val camera2Quirks: Camera2Quirks,
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

    @VisibleForTesting
    @GuardedBy("lock")
    internal var controllerState: ControllerState = ControllerState.STOPPED

    @GuardedBy("lock")
    private var cameraAvailability: CameraStatus = CameraStatus.CameraUnavailable(cameraId)

    @GuardedBy("lock") private var lastCameraError: CameraError? = null
    @GuardedBy("lock") private var lastCameraPrioritiesChangedTs: TimestampNs? = null

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

    @GuardedBy("lock")
    private fun tryRestart() {
        val currentTimestampTs = timeSource.now()
        if (
            !shouldRestart(
                controllerState,
                lastCameraError,
                cameraAvailability,
                lastCameraPrioritiesChangedTs,
                currentTimestampTs,
            )
        ) {
            Log.debug {
                "$this: Not restarting. " +
                    "Controller state = $controllerState, last camera error = $lastCameraError, " +
                    "camera availability = $cameraAvailability, " +
                    "last camera priorities changed = $lastCameraPrioritiesChangedTs, " +
                    "current timestamp = $currentTimestampTs."
            }
            return
        }

        val delayMs =
            if (graphConfig.flags.enableRestartDelays) RESTART_TIMEOUT_WHEN_ENABLED_MS else 0L
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
                        Log.debug { "Restarting $this..." }
                        surfaceTracker.registerAllSurfaces()
                        stopLocked()
                        startLocked()
                    }
                }
            }
    }

    @GuardedBy("lock")
    private fun startLocked() {
        if (controllerState == ControllerState.CLOSED) {
            Log.info { "Ignoring start(): $this is already closed" }
            return
        } else if (controllerState == ControllerState.STARTED) {
            Log.warn { "Ignoring start(): $this is already started" }
            return
        }
        lastCameraError = null
        val camera =
            camera2DeviceManager.open(
                cameraId = graphConfig.camera,
                sharedCameraIds = graphConfig.sharedCameraIds,
                graphListener = graphListener,
                isPrewarm = false,
            ) { _ ->
                isForeground
            }
        if (camera == null) {
            Log.error { "Failed to start $this: Open request submission failed" }
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
                threads.blockingDispatcher,
                threads.backgroundDispatcher,
                scope
            )
        currentSession = session

        val surfaces: Map<StreamId, Surface>? = currentSurfaceMap
        if (surfaces != null) {
            session.configureSurfaceMap(surfaces)
        }

        controllerState = ControllerState.STARTED
        Log.debug { "Started $this" }
        currentCameraStateJob?.cancel()
        currentCameraStateJob = scope.launch { bindSessionToCamera() }
    }

    @GuardedBy("lock")
    private fun stopLocked() {
        if (controllerState == ControllerState.CLOSED) {
            Log.warn { "Ignoring stop(): $this is already closed" }
            return
        } else if (
            controllerState == ControllerState.STOPPING ||
                controllerState == ControllerState.STOPPED
        ) {
            Log.warn { "Ignoring stop(): $this already stopping or stopped" }
            return
        }

        val camera = currentCamera
        val session = currentSession

        currentCamera = null
        currentSession = null

        controllerState = ControllerState.STOPPING
        Log.debug { "Stopping $this" }
        detachSessionAndCamera(session, camera)
    }

    private fun onCameraStatusChanged(cameraStatus: CameraStatus) {
        Log.debug { "$this ($cameraId) camera status changed: $cameraStatus" }
        synchronized(lock) {
            if (controllerState == ControllerState.CLOSED) {
                return
            }
            when (cameraStatus) {
                is CameraStatus.CameraAvailable -> cameraAvailability = cameraStatus
                is CameraStatus.CameraUnavailable -> cameraAvailability = cameraStatus
                is CameraStatus.CameraPrioritiesChanged ->
                    lastCameraPrioritiesChangedTs = timeSource.now()
            }
            tryRestart()
        }
    }

    override fun close(): Unit =
        synchronized(lock) {
            if (controllerState == ControllerState.CLOSED) {
                return
            }
            controllerState = ControllerState.CLOSED
            Log.debug { "Closed $this" }

            val camera = currentCamera
            val session = currentSession

            currentCamera = null
            currentSession = null

            restartJob?.cancel()
            currentCameraStateJob?.cancel()
            currentCameraStateJob = null
            cameraAvailabilityJob?.cancel()
            cameraAvailabilityJob = null
            cameraPrioritiesJob?.cancel()
            cameraPrioritiesJob = null
            cameraStatusMonitor.close()

            detachSessionAndCamera(session, camera)
            if (
                graphConfig.flags.closeCameraDeviceOnClose ||
                    camera2Quirks.shouldCloseCameraBeforeCreatingCaptureSession(cameraId)
            ) {
                Log.debug { "Quirk: Closing $cameraId during $this#close" }
                camera2DeviceManager.close(cameraId)
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

    override fun toString(): String = "Camera2CameraController($cameraGraphId)"

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
                        session.shutdown()
                    }
                    is CameraStateClosed -> {
                        session.shutdown()
                        onStateClosed(cameraState)
                    }
                    else -> {
                        // Do nothing
                    }
                }
            }
        }
    }

    private fun onStateClosed(cameraState: CameraStateClosed) {
        synchronized(lock) {
            if (controllerState == ControllerState.CLOSED) {
                return
            }
            if (cameraState.cameraErrorCode != null) {
                lastCameraError = cameraState.cameraErrorCode
                if (cameraState.cameraErrorCode.isDisconnected()) {
                    controllerState = ControllerState.DISCONNECTED
                    Log.debug { "$this is disconnected" }
                } else {
                    controllerState = ControllerState.ERROR
                    Log.debug { "$this encountered error: ${cameraState.cameraErrorCode}" }
                }
            } else {
                controllerState = ControllerState.STOPPED
            }

            surfaceTracker.unregisterAllSurfaces()
            tryRestart()
        }
    }

    private fun detachSessionAndCamera(session: CaptureSessionState?, camera: VirtualCamera?) {
        scope.launch {
            session?.shutdown()
            camera?.disconnect()
        }
    }

    companion object {
        private const val RESTART_TIMEOUT_WHEN_ENABLED_MS = 700L // 0.7s
        private const val MS_TO_NS = 1_000_000
        private val PRIORITIES_CHANGED_THRESHOLD_NS = DurationNs(200_000_000L) // 200ms

        @VisibleForTesting
        internal fun shouldRestart(
            controllerState: ControllerState,
            lastCameraError: CameraError?,
            cameraAvailability: CameraStatus,
            lastCameraPrioritiesChangedTs: TimestampNs?,
            currentTs: TimestampNs,
        ): Boolean {
            val cameraAvailable = cameraAvailability is CameraStatus.CameraAvailable

            // Camera priorities changed is a on-the-spot signal that doesn't actually indicate
            // whether we do have camera priority. The signal may come in early or late, and other
            // associated signals (e.g., camera disconnect) may also be processed slightly later in
            // CameraPipe. To address the racey nature of these camera signals, here we consider
            // camera priorities changed if we've received such a signal within the last 200ms.
            val prioritiesChanged =
                if (lastCameraPrioritiesChangedTs == null) false
                else (currentTs - lastCameraPrioritiesChangedTs) <= PRIORITIES_CHANGED_THRESHOLD_NS

            when (controllerState) {
                ControllerState.DISCONNECTED ->
                    if (cameraAvailable || prioritiesChanged) {
                        return true
                    } else if (
                        Build.VERSION.SDK_INT in (Build.VERSION_CODES.Q..Build.VERSION_CODES.S_V2)
                    ) {
                        // The camera priorities changed signal experiences issues during [Q, S_V2]
                        // where it might not be invoked as expected. Hence we restart whenever
                        // an opportunity arises.
                        Log.debug { "Quirk for multi-resume activated: Kicking off restart." }
                        return true
                    }
                ControllerState.ERROR ->
                    // If the camera is available, we should restart, provided that we didn't get
                    // an error during graph (session) configuration, since restarting here would
                    // likely not help if it's a problem with graph configuration settings.
                    if (cameraAvailable && lastCameraError != CameraError.ERROR_GRAPH_CONFIG) {
                        return true
                    }
            }

            return false
        }
    }
}
