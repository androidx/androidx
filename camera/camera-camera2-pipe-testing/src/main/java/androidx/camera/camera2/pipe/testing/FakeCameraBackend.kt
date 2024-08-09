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

package androidx.camera.camera2.pipe.testing

import androidx.camera.camera2.pipe.CameraBackend
import androidx.camera.camera2.pipe.CameraBackendId
import androidx.camera.camera2.pipe.CameraContext
import androidx.camera.camera2.pipe.CameraController
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraGraphId
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.CameraStatusMonitor
import androidx.camera.camera2.pipe.StreamGraph
import androidx.camera.camera2.pipe.graph.GraphListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/** The FakeCameraBackend implements [CameraBackend] and creates [CameraControllerSimulator]s. */
public class FakeCameraBackend(private val fakeCameras: Map<CameraId, CameraMetadata>) :
    CameraBackend {
    private val lock = Any()
    private val fakeCameraIds = fakeCameras.keys.toList()

    private val _cameraControllers = mutableListOf<CameraControllerSimulator>()
    public val cameraControllers: List<CameraControllerSimulator>
        get() = synchronized(lock) { _cameraControllers.toList() }

    override val id: CameraBackendId
        get() = FAKE_CAMERA_BACKEND_ID

    override val cameraStatus: Flow<CameraStatusMonitor.CameraStatus>
        get() = MutableSharedFlow()

    override fun awaitCameraIds(): List<CameraId> = fakeCameraIds

    override fun awaitConcurrentCameraIds(): Set<Set<CameraId>> = emptySet()

    override fun awaitCameraMetadata(cameraId: CameraId): CameraMetadata? = fakeCameras[cameraId]

    override fun disconnectAllAsync(): Deferred<Unit> {
        _cameraControllers.forEach { it.simulateCameraStopped() }
        return CompletableDeferred(Unit)
    }

    override fun shutdownAsync(): Deferred<Unit> {
        _cameraControllers.forEach { it.simulateCameraStopped() }
        return CompletableDeferred(Unit)
    }

    override fun createCameraController(
        cameraContext: CameraContext,
        graphId: CameraGraphId,
        graphConfig: CameraGraph.Config,
        graphListener: GraphListener,
        streamGraph: StreamGraph
    ): CameraController {
        val cameraController =
            CameraControllerSimulator(
                cameraContext,
                graphId,
                graphConfig,
                graphListener,
                streamGraph
            )
        synchronized(lock) { _cameraControllers.add(cameraController) }
        return cameraController
    }

    override fun prewarm(cameraId: CameraId) {
        _cameraControllers.find { it.cameraId == cameraId }?.simulateCameraStarted()
    }

    override fun disconnect(cameraId: CameraId) {
        _cameraControllers.find { it.cameraId == cameraId }?.simulateCameraStopped()
    }

    override fun disconnectAsync(cameraId: CameraId): Deferred<Unit> {
        _cameraControllers.find { it.cameraId == cameraId }?.simulateCameraStopped()
        return CompletableDeferred(Unit)
    }

    override fun disconnectAll() {
        _cameraControllers.forEach { it.simulateCameraStopped() }
    }

    public companion object {
        public val FAKE_CAMERA_BACKEND_ID: CameraBackendId =
            CameraBackendId("androidx.camera.camera2.pipe.testing.FakeCameraBackend")
    }
}
