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
import androidx.camera.camera2.pipe.compat.CameraDeviceWrapper
import androidx.camera.camera2.pipe.compat.CameraState
import androidx.camera.camera2.pipe.compat.CameraStateClosed
import androidx.camera.camera2.pipe.compat.CameraStateOpen
import androidx.camera.camera2.pipe.compat.CameraStateUnopened
import androidx.camera.camera2.pipe.compat.ClosedReason
import androidx.camera.camera2.pipe.compat.VirtualCamera
import androidx.camera.common.CameraId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

internal class FakeVirtualCamera(val cameraId: CameraId) : VirtualCamera {

    val fakeCameraDevice: CameraDeviceWrapper = mock()

    private val _state = MutableStateFlow<CameraState>(CameraStateUnopened)

    override val state: Flow<CameraState>
        get() = _state.asStateFlow()

    override val value: CameraState
        get() = _state.value

    override fun disconnect(lastCameraError: CameraError?) {
        // No-op.
    }

    suspend fun simulateCameraStateOpen() {
        whenever(fakeCameraDevice.cameraId).thenReturn(cameraId)
        val state = CameraStateOpen(fakeCameraDevice)
        _state.value = state
    }

    suspend fun simulateCameraStateError(cameraError: CameraError) {
        val state =
            CameraStateClosed(
                cameraId = cameraId,
                cameraClosedReason = ClosedReason.CAMERA2_ERROR,
                cameraErrorCode = cameraError,
            )
        _state.value = state
    }
}
