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

package androidx.camera.integration.core.util

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CaptureRequest
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.camera2.pipe.integration.CameraPipeConfig
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.ExtendableBuilder
import com.google.common.util.concurrent.ListenableFuture

object CameraPipeUtil {

    @kotlin.OptIn(
        androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop::class
    )
    @OptIn(
        markerClass = [ExperimentalCamera2Interop::class]
    )
    @JvmStatic
    fun <T> setCameraCaptureSessionCallback(
        implName: String,
        builder: ExtendableBuilder<T>,
        captureCallback: CameraCaptureSession.CaptureCallback
    ) {
        if (implName == CameraPipeConfig::class.simpleName) {
            androidx.camera.camera2.pipe.integration.interop.Camera2Interop.Extender(builder)
                .setSessionCaptureCallback(captureCallback)
        } else {
            Camera2Interop.Extender(builder).setSessionCaptureCallback(captureCallback)
        }
    }

    @kotlin.OptIn(
        androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop::class
    )
    @OptIn(markerClass = [ExperimentalCamera2Interop::class])
    @JvmStatic
    fun <T> setDeviceStateCallback(
        implName: String,
        builder: ExtendableBuilder<T>,
        stateCallback: CameraDevice.StateCallback
    ) {
        if (implName == CameraPipeConfig::class.simpleName) {
            androidx.camera.camera2.pipe.integration.interop.Camera2Interop.Extender(
                builder
            ).setDeviceStateCallback(stateCallback)
        } else {
            Camera2Interop.Extender(builder).setDeviceStateCallback(stateCallback)
        }
    }

    @kotlin.OptIn(
        androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop::class
    )
    @OptIn(markerClass = [ExperimentalCamera2Interop::class])
    @JvmStatic
    fun <T> setSessionStateCallback(
        implName: String,
        builder: ExtendableBuilder<T>,
        stateCallback: CameraCaptureSession.StateCallback
    ) {
        if (implName == CameraPipeConfig::class.simpleName) {
            androidx.camera.camera2.pipe.integration.interop.Camera2Interop.Extender(
                builder
            ).setSessionStateCallback(stateCallback)
        } else {
            Camera2Interop.Extender(builder).setSessionStateCallback(stateCallback)
        }
    }

    @kotlin.OptIn(
        androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop::class
    )
    @OptIn(markerClass = [ExperimentalCamera2Interop::class])
    @JvmStatic
    fun getCameraId(implName: String, cameraInfo: CameraInfo): String {
        return if (implName == CameraPipeConfig::class.simpleName) {
            androidx.camera.camera2.pipe.integration.interop.Camera2CameraInfo.from(
                cameraInfo
            ).getCameraId()
        } else {
            Camera2CameraInfo.from(cameraInfo).cameraId
        }
    }

    @kotlin.OptIn(
        androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop::class
    )
    @OptIn(markerClass = [ExperimentalCamera2Interop::class])
    @JvmStatic
    fun setRequestOptions(
        implName: String,
        cameraControl: CameraControl,
        parameter: Map<CaptureRequest.Key<Int>, Int>
    ): ListenableFuture<Void?> {
        return if (implName == CameraPipeConfig::class.simpleName) {
            androidx.camera.camera2.pipe.integration.interop.Camera2CameraControl.from(
                cameraControl
            ).setCaptureRequestOptions(
                androidx.camera.camera2.pipe.integration.interop.CaptureRequestOptions.Builder()
                    .apply {
                        parameter.forEach { (key, value) -> setCaptureRequestOption(key, value) }
                    }.build()
            )
        } else {
            Camera2CameraControl.from(cameraControl).setCaptureRequestOptions(
                CaptureRequestOptions.Builder().apply {
                    parameter.forEach { (key, value) -> setCaptureRequestOption(key, value) }
                }.build()
            )
        }
    }
}
