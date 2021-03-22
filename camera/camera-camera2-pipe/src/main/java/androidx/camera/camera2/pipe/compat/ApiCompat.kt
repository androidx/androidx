/*
 * Copyright 2021 The Android Open Source Project
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

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.OutputConfiguration
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.N)
internal object Api24Compat {
    @JvmStatic
    fun getSurfaceGroupId(outputConfiguration: OutputConfiguration): Int {
        return outputConfiguration.surfaceGroupId
    }
}

@RequiresApi(Build.VERSION_CODES.P)
@Suppress("DEPRECATION")
internal object Api28Compat {
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
    fun getPhysicalCaptureResults(
        totalCaptureResult: TotalCaptureResult
    ): Map<String, CaptureResult>? {
        return totalCaptureResult.physicalCameraResults
    }
}
