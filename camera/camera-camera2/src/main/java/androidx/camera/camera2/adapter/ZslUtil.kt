/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.camera2.adapter

import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.util.Size
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.CameraMetadata.Companion.supportsPrivateReprocessing

internal object ZslUtil {
    private val EMPTY_INT_ARRAY = IntArray(0)

    fun computeZslIntersectionSizes(cameraMetadata: CameraMetadata, format: Int): List<Size> {
        val physicalCameraIds = cameraMetadata.physicalCameraIds
        if (physicalCameraIds.isEmpty()) {
            return cameraMetadata[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]
                ?.getInputSizes(format)
                ?.toList() ?: emptyList()
        }

        var intersectedSizes: MutableSet<Size>? = null
        for (id in physicalCameraIds) {
            val physicalMetadata = cameraMetadata.awaitPhysicalMetadata(id)
            if (!physicalMetadata.supportsPrivateReprocessing) {
                return emptyList()
            }
            val physicalStreamMap =
                physicalMetadata[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]
                    ?: return emptyList()

            val inputSizes = physicalStreamMap.getInputSizes(format) ?: emptyArray()
            val validOutputFormats =
                physicalStreamMap.getValidOutputFormatsForInput(format) ?: EMPTY_INT_ARRAY
            if (!validOutputFormats.contains(ImageFormat.JPEG)) {
                return emptyList()
            }

            val inputSet = inputSizes.toSet()
            if (intersectedSizes == null) {
                intersectedSizes = inputSet.toMutableSet()
            } else {
                intersectedSizes.retainAll(inputSet)
            }

            if (intersectedSizes.isEmpty()) {
                return emptyList()
            }
        }

        return intersectedSizes?.toList() ?: emptyList()
    }
}
