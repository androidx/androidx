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
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraController
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraSurfaceManager
import androidx.camera.camera2.pipe.StreamId
import androidx.camera.camera2.pipe.config.Camera2ControllerScope
import androidx.camera.camera2.pipe.graph.GraphListener
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
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
internal class Camera2CameraController @Inject constructor(
    private val scope: CoroutineScope,
    private val config: CameraGraph.Config,
    private val graphListener: GraphListener,
    private val captureSessionFactory: CaptureSessionFactory,
    private val requestProcessorFactory: Camera2RequestProcessorFactory,
    private val virtualCameraManager: VirtualCameraManager,
    private val cameraSurfaceManager: CameraSurfaceManager
) : CameraController {
    private var currentCamera: VirtualCamera? = null
    private var currentSession: VirtualSessionState? = null
    private var surfaceMap: Map<StreamId, Surface>? = null

    override fun start() {
        val camera = virtualCameraManager.open(
            config.camera,
            config.flags.allowMultipleActiveCameras
        )
        synchronized(this) {
            check(currentCamera == null)
            check(currentSession == null)

            currentCamera = camera
            val session = VirtualSessionState(
                graphListener,
                captureSessionFactory,
                requestProcessorFactory,
                cameraSurfaceManager,
                scope
            )
            currentSession = session

            val surfaces: Map<StreamId, Surface>? = surfaceMap
            if (surfaces != null) {
                session.onSurfaceMapUpdated(surfaces)
            }
        }
        scope.launch { bindSessionToCamera() }
    }

    override fun stop() {
        val camera: VirtualCamera?
        val session: VirtualSessionState?
        synchronized(this) {
            camera = currentCamera
            session = currentSession

            currentCamera = null
            currentSession = null
        }

        scope.launch {
            session?.disconnect()
            camera?.disconnect()
        }
    }

    override fun close() {
        // TODO: Consider changing the behavior so that start / stop are not invokable after calling
        //   close.
        stop()
    }

    override fun updateSurfaceMap(surfaceMap: Map<StreamId, Surface>) {
        synchronized(this) {
            this.surfaceMap = surfaceMap
            currentSession?.onSurfaceMapUpdated(surfaceMap)
        }
    }

    private suspend fun bindSessionToCamera() {
        val camera: VirtualCamera?
        val session: VirtualSessionState?

        synchronized(this) {
            camera = currentCamera
            session = currentSession
        }

        if (camera != null && session != null) {
            camera.state.collect {
                if (it is CameraStateOpen) {
                    session.cameraDevice = it.cameraDevice
                } else if (it is CameraStateClosing || it is CameraStateClosed) {
                    session.disconnect()
                }
            }
        }
    }
}