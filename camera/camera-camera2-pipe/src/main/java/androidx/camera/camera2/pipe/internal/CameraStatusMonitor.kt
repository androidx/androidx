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

package androidx.camera.camera2.pipe.internal

import androidx.annotation.RestrictTo
import androidx.camera.camera2.pipe.CameraId
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A CameraStatusMonitor monitors the status of the cameras, and emits updates when the status of
 * cameras changes, for instance when the camera access priorities have changed or when a particular
 * camera has become available.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal interface CameraStatusMonitor : AutoCloseable {

    /** Gets the state flow of the availability of the current camera. */
    val cameraAvailability: StateFlow<CameraStatus>

    /** A shared flow that emits when camera access priorities have changed. */
    val cameraPriorities: SharedFlow<Unit>

    abstract class CameraStatus internal constructor() {
        object Unknown : CameraStatus() {
            override fun toString(): String = "UnknownCameraStatus"
        }

        object CameraPrioritiesChanged : CameraStatus() {
            override fun toString(): String = "CameraPrioritiesChanged"
        }

        class CameraAvailable(val cameraId: CameraId) : CameraStatus() {
            override fun toString(): String = "CameraAvailable(camera=$cameraId)"
        }

        class CameraUnavailable(val cameraId: CameraId) : CameraStatus() {
            override fun toString(): String = "CameraUnavailable(camera=$cameraId)"
        }
    }
}
