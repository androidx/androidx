/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.camera.camera2.pipe.CameraError
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.compat.Camera2DeviceManager
import androidx.camera.camera2.pipe.compat.VirtualCamera
import androidx.camera.camera2.pipe.graph.GraphListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

internal class FakeCamera2DeviceManager : Camera2DeviceManager {

    private val fakeVirtualCameraMap = mutableMapOf<CameraId, FakeVirtualCamera>()

    override fun open(
        cameraId: CameraId,
        sharedCameraIds: List<CameraId>,
        graphListener: GraphListener,
        isPrewarm: Boolean,
        isForegroundObserver: (Unit) -> Boolean,
    ): VirtualCamera {
        return FakeVirtualCamera(cameraId).also { fakeVirtualCameraMap[cameraId] = it }
    }

    override fun prewarm(cameraId: CameraId) {
        // No-op.
    }

    override fun close(cameraId: CameraId): Deferred<Unit> {
        fakeVirtualCameraMap.remove(cameraId)
        return CompletableDeferred(Unit)
    }

    override fun closeAll(): Deferred<Unit> {
        fakeVirtualCameraMap.clear()
        return CompletableDeferred(Unit)
    }

    suspend fun simulateCameraOpen(cameraId: CameraId) {
        val fakeVirtualCamera = checkNotNull(fakeVirtualCameraMap[cameraId])
        fakeVirtualCamera.simulateCameraStateOpen()
    }

    suspend fun simulateCameraError(cameraId: CameraId, cameraError: CameraError) {
        val fakeVirtualCamera = checkNotNull(fakeVirtualCameraMap[cameraId])
        fakeVirtualCamera.simulateCameraStateError(cameraError)
    }
}
