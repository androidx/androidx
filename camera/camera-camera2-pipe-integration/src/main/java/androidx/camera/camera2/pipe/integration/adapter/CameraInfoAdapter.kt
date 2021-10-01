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

@file:RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java

package androidx.camera.camera2.pipe.integration.adapter

import android.annotation.SuppressLint
import android.hardware.camera2.CameraCharacteristics
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.integration.config.CameraConfig
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.camera2.pipe.integration.impl.CameraCallbackMap
import androidx.camera.camera2.pipe.integration.impl.CameraProperties
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ExposureState
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ZoomState
import androidx.camera.core.impl.CamcorderProfileProvider
import androidx.camera.core.impl.CameraCaptureCallback
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.Quirks
import androidx.camera.core.impl.utils.CameraOrientationUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.concurrent.Executor
import javax.inject.Inject

internal val defaultQuirks = Quirks(emptyList())

/**
 * Adapt the [CameraInfoInternal] interface to [CameraPipe].
 */
@SuppressLint(
    "UnsafeOptInUsageError" // Suppressed due to experimental ExposureState
)
@CameraScope
class CameraInfoAdapter @Inject constructor(
    private val cameraProperties: CameraProperties,
    private val cameraConfig: CameraConfig,
    private val cameraState: CameraStateAdapter,
    private val cameraCallbackMap: CameraCallbackMap
) : CameraInfoInternal {

    override fun getCameraId(): String = cameraConfig.cameraId.value
    override fun getLensFacing(): Int? =
        cameraProperties.metadata[CameraCharacteristics.LENS_FACING]

    override fun getSensorRotationDegrees(): Int = getSensorRotationDegrees(Surface.ROTATION_0)
    override fun hasFlashUnit(): Boolean =
        cameraProperties.metadata[CameraCharacteristics.FLASH_INFO_AVAILABLE]!!

    override fun getSensorRotationDegrees(relativeRotation: Int): Int {
        val sensorOrientation: Int =
            cameraProperties.metadata[CameraCharacteristics.SENSOR_ORIENTATION]!!
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

    override fun getZoomState(): LiveData<ZoomState> = cameraState.zoomStateLiveData
    override fun getTorchState(): LiveData<Int> = cameraState.torchStateLiveData

    @SuppressLint("UnsafeOptInUsageError")
    override fun getExposureState(): ExposureState = cameraState.exposureState

    override fun getCameraState(): LiveData<CameraState> {
        Log.warn { "TODO: CameraState is not yet supported." }
        return MutableLiveData(CameraState.create(CameraState.Type.CLOSED))
    }

    override fun addSessionCaptureCallback(executor: Executor, callback: CameraCaptureCallback) =
        cameraCallbackMap.addCaptureCallback(callback, executor)

    override fun removeSessionCaptureCallback(callback: CameraCaptureCallback) =
        cameraCallbackMap.removeCaptureCallback(callback)

    override fun getImplementationType(): String = "CameraPipe"

    override fun getCamcorderProfileProvider(): CamcorderProfileProvider {
        Log.warn { "TODO: CamcorderProfileProvider is not yet supported." }
        return CamcorderProfileProvider.EMPTY
    }

    override fun toString(): String = "CameraInfoAdapter<$cameraConfig.cameraId>"

    override fun getCameraQuirks(): Quirks {
        Log.warn { "TODO: Quirks are not yet supported." }
        return defaultQuirks
    }

    override fun isFocusMeteringSupported(action: FocusMeteringAction): Boolean {
        Log.warn { "TODO: isFocusAndMeteringSupported are not yet supported." }
        return false
    }
}