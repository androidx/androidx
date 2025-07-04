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

package androidx.camera.camera2.pipe.compat

import androidx.annotation.GuardedBy
import androidx.camera.camera2.pipe.CameraBackend
import androidx.camera.camera2.pipe.CameraBackendId
import androidx.camera.camera2.pipe.CameraContext
import androidx.camera.camera2.pipe.CameraController
import androidx.camera.camera2.pipe.CameraGraph
import androidx.camera.camera2.pipe.CameraGraphId
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.StreamGraph
import androidx.camera.camera2.pipe.SurfaceTracker
import androidx.camera.camera2.pipe.config.Camera2ControllerComponent
import androidx.camera.camera2.pipe.config.Camera2ControllerConfig
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.core.Threads
import androidx.camera.camera2.pipe.graph.GraphListener
import androidx.camera.camera2.pipe.graph.StreamGraphImpl
import javax.inject.Inject
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow

/** This is the default [CameraBackend] implementation for CameraPipe based on Camera2. */
internal class Camera2Backend
@Inject
constructor(
    private val threads: Threads,
    private val camera2DeviceCache: Camera2DeviceCache,
    private val camera2MetadataCache: Camera2MetadataCache,
    private val camera2DeviceManager: Camera2DeviceManager,
    private val camera2CameraControllerComponent: Camera2ControllerComponent.Builder,
) : CameraBackend, Camera2CameraController.ShutdownListener {
    private val lock = Any()

    @GuardedBy("lock") private val activeCameraControllers = mutableSetOf<CameraController>()

    override val id: CameraBackendId
        get() = CameraBackendId("CXCP-Camera2")

    override val cameraIds: Flow<List<CameraId>>
        get() = camera2DeviceCache.cameraIds

    override suspend fun getCameraIds(): List<CameraId> = camera2DeviceCache.getCameraIds()

    override fun awaitCameraIds(): List<CameraId>? = camera2DeviceCache.awaitCameraIds()

    override fun awaitConcurrentCameraIds(): Set<Set<CameraId>>? =
        camera2DeviceCache.awaitConcurrentCameraIds()

    override suspend fun getCameraMetadata(cameraId: CameraId): CameraMetadata =
        camera2MetadataCache.getCameraMetadata(cameraId)

    override fun awaitCameraMetadata(cameraId: CameraId): CameraMetadata =
        camera2MetadataCache.awaitCameraMetadata(cameraId)

    override fun disconnect(cameraId: CameraId) {
        camera2DeviceManager.close(cameraId)
    }

    override fun disconnectAsync(cameraId: CameraId): Deferred<Unit> {
        return camera2DeviceManager.close(cameraId)
    }

    override fun disconnectAll() {
        camera2DeviceManager.closeAll()
    }

    override fun disconnectAllAsync(): Deferred<Unit> {
        return camera2DeviceManager.closeAll()
    }

    override fun shutdownAsync(): Deferred<Unit> {
        Log.debug { "Camera2Backend#shutdownAsync" }
        camera2DeviceCache.shutdown()
        return threads.cameraPipeScope.async {
            val controllers = synchronized(lock) { activeCameraControllers }
            for (controller in controllers) {
                Log.debug { "Camera2Backend#shutdownAsync: Awaiting closure from $controller" }
                if (!controller.awaitClosed()) {
                    Log.warn { "Failed to await closure from $controller!" }
                }
            }

            Log.debug { "Camera2Backend#shutdownAsync: Closing all cameras (if any)" }
            camera2DeviceManager.closeAll().await()
        }
    }

    override fun createCameraController(
        cameraContext: CameraContext,
        graphId: CameraGraphId,
        graphConfig: CameraGraph.Config,
        graphListener: GraphListener,
        streamGraph: StreamGraph,
        surfaceTracker: SurfaceTracker,
    ): CameraController {
        // Use Dagger to create the camera2 controller component, then create the CameraController.
        val cameraControllerComponent =
            camera2CameraControllerComponent
                .camera2ControllerConfig(
                    Camera2ControllerConfig(
                        this,
                        graphId,
                        graphConfig,
                        graphListener,
                        streamGraph as StreamGraphImpl,
                        surfaceTracker,
                        this,
                    )
                )
                .build()

        // Create and return a Camera2 CameraController object.
        val cameraController = cameraControllerComponent.cameraController()
        synchronized(lock) { activeCameraControllers.add(cameraController) }
        return cameraController
    }

    override fun prewarm(cameraId: CameraId) {
        camera2DeviceManager.prewarm(cameraId)
    }

    override fun onControllerClosed(cameraController: CameraController) {
        Log.debug { "$cameraController finalized" }
        synchronized(lock) { activeCameraControllers.remove(cameraController) }
    }
}
