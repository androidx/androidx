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

import androidx.camera.camera2.pipe.internal.CameraStatusMonitor
import androidx.camera.camera2.pipe.internal.CameraStatusMonitor.CameraStatus
import androidx.camera.common.CameraId
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

internal class FakeCameraStatusMonitor(private val cameraId: CameraId) : CameraStatusMonitor {
    private val _cameraAvailability =
        MutableStateFlow<CameraStatus>(CameraStatus.CameraAvailable(cameraId))
    override val cameraAvailability: StateFlow<CameraStatus>
        get() = _cameraAvailability.asStateFlow()

    private val _cameraPriorities = MutableSharedFlow<Unit>()
    override val cameraPriorities: SharedFlow<Unit>
        get() = _cameraPriorities.asSharedFlow()

    override fun close() {}

    suspend fun simulateCameraAvailable() {
        _cameraAvailability.emit(CameraStatus.CameraAvailable(cameraId))
    }

    suspend fun simulateCameraUnavailable() {
        _cameraAvailability.emit(CameraStatus.CameraUnavailable(cameraId))
    }

    suspend fun simulateCameraPrioritiesChanged() {
        _cameraPriorities.emit(Unit)
    }
}
