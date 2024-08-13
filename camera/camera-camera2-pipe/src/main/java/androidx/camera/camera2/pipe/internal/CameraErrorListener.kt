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

package androidx.camera.camera2.pipe.internal

import androidx.camera.camera2.pipe.CameraError
import androidx.camera.camera2.pipe.CameraId

/**
 * Interface intended to be used to report camera errors. It will ensure only the current
 * [androidx.camera.camera2.pipe.graph.GraphListener] is notified of the error.
 */
@JvmDefaultWithCompatibility
public interface CameraErrorListener {
    public fun onCameraError(
        cameraId: CameraId,
        cameraError: CameraError,
        willAttemptRetry: Boolean = false
    )
}
