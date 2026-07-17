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

package androidx.camera.common.compat

import android.hardware.HardwareBuffer
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.media.Image
import androidx.annotation.RequiresApi

@RequiresApi(28)
internal object Api28Compat {
    @JvmStatic
    fun getHardwareBuffer(image: Image): HardwareBuffer? {
        return image.hardwareBuffer
    }

    @JvmStatic
    fun getAvailablePhysicalCameraRequestKeys(
        cameraCharacteristics: CameraCharacteristics
    ): List<CaptureRequest.Key<*>>? {
        return cameraCharacteristics.availablePhysicalCameraRequestKeys
    }

    @JvmStatic
    fun getAvailableSessionKeys(
        cameraCharacteristics: CameraCharacteristics
    ): List<CaptureRequest.Key<*>>? {
        return cameraCharacteristics.availableSessionKeys
    }

    @JvmStatic
    fun getPhysicalCameraIds(cameraCharacteristics: CameraCharacteristics): Set<String> {
        return cameraCharacteristics.physicalCameraIds
    }

    @JvmStatic
    fun getKeys(captureRequest: CaptureRequest): List<CaptureRequest.Key<*>> {
        return captureRequest.keys
    }

    @JvmStatic
    fun getKeys(captureResult: CaptureResult): List<CaptureResult.Key<*>> {
        return captureResult.keys
    }
}
