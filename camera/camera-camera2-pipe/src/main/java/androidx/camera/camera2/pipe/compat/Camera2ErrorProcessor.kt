/*
 * Copyright 2023 The Android Open Source Project
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
import androidx.camera.camera2.pipe.CameraError
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.GraphState
import androidx.camera.camera2.pipe.internal.CameraErrorListener
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A class responsible for reporting camera errors with a particular [CameraId]. When
 * [androidx.camera.camera2.pipe.compat.VirtualCameraManager] processes a camera open request, it
 * should update CameraErrorProcessor with the [VirtualCameraState] that came with the open request.
 */
@Singleton
public class Camera2ErrorProcessor @Inject constructor() : CameraErrorListener {
    private val lock = Any()

    @GuardedBy("lock")
    private val virtualCameraStateMap = mutableMapOf<CameraId, VirtualCameraState>()

    override fun onCameraError(
        cameraId: CameraId,
        cameraError: CameraError,
        willAttemptRetry: Boolean
    ) {
        val virtualCameraState = synchronized(lock) { virtualCameraStateMap[cameraId] } ?: return
        virtualCameraState.graphListener.onGraphError(
            GraphState.GraphStateError(cameraError, willAttemptRetry)
        )
    }

    /**
     * Sets the current active [VirtualCameraState] to report the camera error to. Any attempt to
     * acquire an open camera creates a [VirtualCameraState], and it's important to keep
     * Camera2ErrorProcessor updated with the latest [VirtualCameraState].
     */
    internal fun setActiveVirtualCamera(
        cameraId: CameraId,
        virtualCameraState: VirtualCameraState
    ) = synchronized(lock) { virtualCameraStateMap[cameraId] = virtualCameraState }
}
