/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.camera.camera2.pipe.compat

import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import android.os.Build
import androidx.annotation.RestrictTo
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.internal.CameraErrorListener

/**
 * A wrapper interface that mirrors android.hardware.camera2.CameraDevice.CameraDeviceSetup.
 *
 * This allows CameraPipe to interact with the setup object in a version-agnostic way.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal interface AndroidCameraDeviceSetupWrapper {
    fun createCaptureRequest(templateType: Int): CaptureRequest.Builder?
}

internal class AndroidCameraDeviceSetup(
    private val cameraDeviceSetup: CameraDevice.CameraDeviceSetup,
    private val cameraId: CameraId,
    private val cameraErrorListener: CameraErrorListener,
) : AndroidCameraDeviceSetupWrapper {

    override fun createCaptureRequest(templateType: Int): CaptureRequest.Builder? =
        catchAndReportCameraExceptions(cameraId, cameraErrorListener) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                Api35Compat.createCaptureRequest(cameraDeviceSetup, templateType)
            } else null
        }
}
