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

import android.view.Surface
import androidx.annotation.GuardedBy
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraController
import androidx.camera.camera2.pipe.CameraController.ControllerState
import androidx.camera.camera2.pipe.CameraError
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraStatusMonitor.CameraStatus
import androidx.camera.camera2.pipe.CameraSurfaceManager
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.config.Camera2ControllerScope
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.core.TimeSource
import androidx.camera.camera2.pipe.graph.GraphListener
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@Camera2ControllerScope
internal class Camera2CameraController
@Inject
constructor(
    private val scope: CoroutineScope,
    private val config: CameraGraph.Config,
    private val graphListener: GraphListener,
    private val captureSessionFactory: CaptureSessionFactory,
    private val captureSequenceProcessorFactory: Camera2CaptureSequenceProcessorFactory,
    private val virtualCameraManager: VirtualCameraManager,
    private val cameraSurfaceManager: CameraSurfaceManager,
    private val timeSource: TimeSource,
) : CameraController {
    override val cameraId: CameraId
        get() = config.camera

    private val lock = Any()

    @GuardedBy("lock")
    private var controllerState: ControllerState = ControllerState.STOPPED

    @GuardedBy("lock")
    private var lastCameraError: CameraError? = null

    private var currentCamera: VirtualCamera? = null
    private var currentSession: CaptureSessionState? = null
    private var currentSurfaceMap: Map<StreamId, Surface>? = null

    private var currentCameraStateJob: Job? = null

    override fun start(): Unit = synchronized(lock) {
        if (controllerState == ControllerState.CLOSED) {
            Log.info { "Ignoring start(): Camera2CameraController is already closed" }
            return
        } else if (controllerState == ControllerState.STARTED) {
            Log.warn { "Ignoring start(): Camera2CameraController is already started" }
            return
        }
        lastCameraError = null
        val camera = virtualCameraManager.open(
            config.camera,
            config.flags.allowMultipleActiveCameras,
            graphListener
        )

        check(currentCamera == null)
        check(currentSession == null)

        currentCamera = camera
        val session = CaptureSessionState(
            graphListener,
            captureSessionFactory,
            captureSequenceProcessorFactory,
            cameraSurfaceManager,
            timeSource,
            config.flags.quirkFinalizeSessionOnCloseBehavior,
            scope
        )
        currentSession = session

        val surfaces: Map<StreamId, Surface>? = currentSurfaceMap
        if (surfaces != null) {
            session.configureSurfaceMap(surfaces)
        }

        controllerState = ControllerState.STARTED
        Log.debug { "Started Camera2CameraController" }
        currentCameraStateJob = scope.launch { bindSessionToCamera() }
    }

    override fun stop(): Unit = synchronized(lock) {
        if (controllerState == ControllerState.CLOSED) {
            Log.warn { "Ignoring stop(): Camera2CameraController is already closed" }
            return
        } else if (controllerState == ControllerState.STOPPING ||
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
        scope.launch {
            session?.disconnect()
            camera?.disconnect()
        }
    }

    override fun tryRestart(cameraStatus: CameraStatus): Unit = synchronized(lock) {
        var shouldRestart = false
        when (controllerState) {
            ControllerState.DISCONNECTED ->
                if (cameraStatus is CameraStatus.CameraAvailable ||
                    cameraStatus is CameraStatus.CameraPrioritiesChanged
                ) {
                    shouldRestart = true
                }

            ControllerState.ERROR ->
                if (cameraStatus is CameraStatus.CameraAvailable &&
                    lastCameraError == CameraError.ERROR_CAMERA_DEVICE
                ) {
                    shouldRestart = true
                }
        }
        if (!shouldRestart) {
            Log.debug {
                "Ignoring tryRestart(): state = $controllerState, cameraStatus = $cameraStatus"
            }
            return
        }
        Log.debug { "Restarting Camera2CameraController" }
        stop()
        start()
    }

    override fun close(): Unit = synchronized(lock) {
        if (controllerState == ControllerState.CLOSED) {
            return
        }
        controllerState = ControllerState.CLOSED
        Log.debug { "Closed Camera2CameraController" }

        val camera = currentCamera
        val session = currentSession

        currentCamera = null
        currentSession = null

        scope.launch {
            session?.disconnect()
            camera?.disconnect()
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
        }?.configureSurfaceMap(surfaceMap)
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

    private fun onStateClosed(cameraState: CameraStateClosed) = synchronized(lock) {
        if (cameraState.cameraErrorCode != null) {
            if (cameraState.cameraErrorCode == CameraError.ERROR_CAMERA_DISCONNECTED ||
                cameraState.cameraErrorCode == CameraError.ERROR_CAMERA_IN_USE ||
                cameraState.cameraErrorCode == CameraError.ERROR_CAMERA_LIMIT_EXCEEDED
            ) {
                controllerState = ControllerState.DISCONNECTED
                Log.debug { "Camera2CameraController is disconnected" }
            } else {
                controllerState = ControllerState.ERROR
                Log.debug {
                    "Camera2CameraController encountered an " +
                        "unrecoverable error: ${cameraState.cameraErrorCode}"
                }
            }
            lastCameraError = cameraState.cameraErrorCode
        } else {
            controllerState = ControllerState.STOPPED
        }
        currentCameraStateJob?.cancel()
        currentCameraStateJob = null
    }
}
