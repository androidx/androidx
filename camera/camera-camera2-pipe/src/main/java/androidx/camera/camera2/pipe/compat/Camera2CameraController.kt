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

import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.config.CameraGraphScope
import androidx.camera.camera2.pipe.config.ForCameraGraph
import androidx.camera.camera2.pipe.graph.GraphListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * This represents the core state loop for a Camera Graph instance.
 *
 * A camera graph will receive start / stop signals from the application. When started, it will do
 * everything possible to bring up and maintain an active camera instance with the given
 * configuration.
 *
 * TODO: Reorganize these constructor parameters.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@CameraGraphScope
internal class Camera2CameraController @Inject constructor(
    @ForCameraGraph private val scope: CoroutineScope,
    private val config: CameraGraph.Config,
    private val graphListener: GraphListener,
    private val captureSessionFactory: CaptureSessionFactory,
    private val requestProcessorFactory: Camera2RequestProcessorFactory,
    private val virtualCameraManager: VirtualCameraManager,
    private val streamGraph: Camera2StreamGraph
) : CameraController {
    private var currentCamera: VirtualCamera? = null
    private var currentSession: VirtualSessionState? = null

    override fun start() {
        val camera = virtualCameraManager.open(
            config.camera,
            config.flags.allowMultipleActiveCameras
        )
        synchronized(this) {
            check(currentCamera == null)
            check(currentSession == null)

            currentCamera = camera
            currentSession = VirtualSessionState(
                graphListener,
                captureSessionFactory,
                requestProcessorFactory,
                scope
            )
        }
        scope.launch { configure() }
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

    override fun restart() {
        val oldSession: VirtualSessionState?
        val newSession: VirtualSessionState?

        synchronized(this) {
            check(currentCamera != null) { "Cannot invoke reconfigure while stopped." }

            oldSession = currentSession
            newSession = VirtualSessionState(
                graphListener,
                captureSessionFactory,
                requestProcessorFactory,
                scope
            )
            currentSession = newSession
        }

        scope.launch {
            oldSession?.disconnect()
            configure()
        }
    }

    private suspend fun configure() {
        val camera: VirtualCamera?
        val session: VirtualSessionState?

        synchronized(this) {
            camera = currentCamera
            session = currentSession
        }

        if (camera != null && session != null) {
            streamGraph.listener = session
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