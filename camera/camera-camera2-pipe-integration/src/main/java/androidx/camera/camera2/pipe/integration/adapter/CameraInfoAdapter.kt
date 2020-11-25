/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.adapter

import android.annotation.SuppressLint
import android.hardware.camera2.CameraCharacteristics
import android.view.Surface
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.integration.config.CameraConfig
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.camera2.pipe.integration.impl.CameraCallbackMap
import androidx.camera.camera2.pipe.integration.impl.CameraState
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExposureState
import androidx.camera.core.ZoomState
import androidx.camera.core.impl.CameraCaptureCallback
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.utils.CameraOrientationUtil
import androidx.lifecycle.LiveData
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Provider

/**
 * Adapt the [CameraInfoInternal] interface to [CameraPipe].
 */
@SuppressLint(
    "UnsafeExperimentalUsageError" // Suppressed due to experimental ExposureState
)
@CameraScope
class CameraInfoAdapter @Inject constructor(
    private val lazyCameraMetadata: Provider<CameraMetadata>,
    private val cameraConfig: CameraConfig,
    private val cameraState: CameraState,
    private val cameraCallbackMap: CameraCallbackMap
) : CameraInfoInternal {

    private val cameraMetadata: CameraMetadata
        get() = lazyCameraMetadata.get()

    override fun getCameraId(): String = cameraConfig.cameraId.value
    override fun getLensFacing(): Int? = cameraMetadata[CameraCharacteristics.LENS_FACING]
    override fun getSensorRotationDegrees(): Int = getSensorRotationDegrees(Surface.ROTATION_0)
    override fun hasFlashUnit(): Boolean =
        cameraMetadata[CameraCharacteristics.FLASH_INFO_AVAILABLE]!!

    override fun getSensorRotationDegrees(relativeRotation: Int): Int {
        val sensorOrientation: Int = cameraMetadata[CameraCharacteristics.SENSOR_ORIENTATION]!!
        val relativeRotationDegrees =
            CameraOrientationUtil.surfaceRotationToDegrees(relativeRotation)
        // Currently this assumes that a back-facing camera is always opposite to the screen.
        // This may not be the case for all devices, so in the future we may need to handle that
        // scenario.
        val lensFacing = lensFacing
        val isOppositeFacingScreen =
            lensFacing != null && CameraSelector.LENS_FACING_BACK == lensFacing
        return CameraOrientationUtil.getRelativeImageRotation(
            relativeRotationDegrees,
            sensorOrientation,
            isOppositeFacingScreen
        )
    }

    override fun getZoomState(): LiveData<ZoomState> = cameraState.zoomState
    override fun getTorchState(): LiveData<Int> = cameraState.torchState
    @SuppressLint("UnsafeExperimentalUsageError")
    override fun getExposureState(): ExposureState = cameraState.exposureState.value!!

    override fun addSessionCaptureCallback(executor: Executor, callback: CameraCaptureCallback) =
        cameraCallbackMap.addCaptureCallback(callback, executor)
    override fun removeSessionCaptureCallback(callback: CameraCaptureCallback) =
        cameraCallbackMap.removeCaptureCallback(callback)

    override fun getImplementationType(): String = "CameraPipe"
    override fun toString(): String = "CameraInfoAdapter<$cameraConfig.cameraId>"
}