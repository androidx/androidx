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
package androidx.camera.camera2.internal

import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata as Camera2Metadata
import android.os.Build
import androidx.camera.camera2.impl.Camera2Logger
import androidx.camera.camera2.pipe.CameraDevices
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.core.InitializationException

/**
 * The [CameraCompatibilityFilter] is responsible for filtering out Cameras that don't contain
 * REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE capability.
 */
public object CameraCompatibilityFilter {

    @JvmStatic
    public fun getBackwardCompatibleCameraIds(
        cameraDevices: CameraDevices,
        availableCameraIds: List<String>,
    ): List<String> =
        availableCameraIds.filter { cameraId ->
            // Heuristic: Always include camera IDs "0" and "1" to align with camera-camera2
            // behavior, assuming they are the default back and front cameras.
            if (cameraId == "0" || cameraId == "1") {
                true
            } else if (isBackwardCompatible(cameraId, cameraDevices)) {
                true
            } else {
                Camera2Logger.debug {
                    "Camera $cameraId is filtered out because its capabilities " +
                        "do not contain REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE."
                }
                false
            }
        }

    @JvmStatic
    public fun isBackwardCompatible(cameraMetadata: CameraMetadata): Boolean {
        // Always returns true to not break robolectric tests because the cameras setup in
        // robolectric don't have REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE capability
        // by default.
        if (Build.FINGERPRINT == "robolectric") {
            Camera2Logger.debug {
                "isBackwardCompatible method returns true because robolectric build detected."
            }
            return true
        }
        val availableCapabilities =
            cameraMetadata[CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES]
        return availableCapabilities?.contains(
            Camera2Metadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE
        ) == true
    }

    @JvmStatic
    public fun isBackwardCompatible(cameraId: String, cameraDevices: CameraDevices): Boolean {
        if (Build.FINGERPRINT == "robolectric") {
            Camera2Logger.debug {
                "isBackwardCompatible method returns true because robolectric build detected."
            }
            return true
        }
        try {
            val cameraMetadata =
                cameraDevices.awaitCameraMetadata(CameraId(cameraId))
                    ?: throw InitializationException(
                        CameraAccessException(
                            CameraAccessException.CAMERA_ERROR,
                            "Failed to get camera metadata for cameraID: $cameraId",
                        )
                    )
            return isBackwardCompatible(cameraMetadata)
        } catch (e: CameraAccessException) {
            Camera2Logger.error(e) { "Error while accessing metadata for cameraID: $cameraId" }
            throw InitializationException(e)
        }
    }
}
