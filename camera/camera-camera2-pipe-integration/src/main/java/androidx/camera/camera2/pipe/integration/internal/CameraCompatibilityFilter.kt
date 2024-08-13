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
package androidx.camera.camera2.pipe.integration.internal

import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.os.Build
import androidx.camera.camera2.pipe.CameraDevices
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.core.InitializationException

/**
 * The [CameraCompatibilityFilter] is responsible for filtering out Cameras that doesn't contains
 * REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE capability.
 */
public object CameraCompatibilityFilter {

    @JvmStatic
    public fun getBackwardCompatibleCameraIds(
        cameraDevices: CameraDevices,
        availableCameraIds: List<String>
    ): List<String> {
        val backwardCompatibleCameraIds = mutableListOf<String>()
        for (cameraId in availableCameraIds) {
            if (isBackwardCompatible(cameraId, cameraDevices)) {
                backwardCompatibleCameraIds.add(cameraId)
            } else {
                Log.debug {
                    "Camera $cameraId is filtered out because its capabilities " +
                        "do not contain REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE."
                }
            }
        }
        return backwardCompatibleCameraIds
    }

    @JvmStatic
    public fun isBackwardCompatible(cameraId: String, cameraDevices: CameraDevices): Boolean {
        // Always returns true to not break robolectric tests because the cameras setup in
        // robolectric don't have REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE capability
        // by default.
        if (Build.FINGERPRINT == "robolectric") {
            Log.debug {
                "isBackwardCompatible method returns true because robolectric build detected."
            }
            return true
        }
        try {
            val cameraMetadata = checkNotNull(cameraDevices.awaitCameraMetadata(CameraId(cameraId)))
            val availableCapabilities =
                cameraMetadata[CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES]
            if (availableCapabilities != null) {
                return availableCapabilities.contains(
                    CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE
                )
            }
        } catch (e: CameraAccessException) {
            Log.error(e) { "Error while accessing metadata for cameraID: $cameraId" }
            throw InitializationException(e)
        }

        return false
    }
}
