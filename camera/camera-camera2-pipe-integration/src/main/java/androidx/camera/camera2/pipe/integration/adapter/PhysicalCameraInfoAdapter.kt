/*
 * Copyright 2024 The Android Open Source Project
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
import android.hardware.camera2.CameraMetadata
import android.util.Range
import android.view.Surface
import androidx.camera.camera2.pipe.UnsafeWrapper
import androidx.camera.camera2.pipe.integration.impl.CameraProperties
import androidx.camera.camera2.pipe.integration.interop.Camera2CameraInfo
import androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.DynamicRange
import androidx.camera.core.ExperimentalZeroShutterLag
import androidx.camera.core.ExposureState
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ZoomState
import androidx.camera.core.impl.utils.CameraOrientationUtil
import androidx.lifecycle.LiveData
import kotlin.reflect.KClass

/**
 * Implementation of [CameraInfo] for physical camera. In comparison, [CameraInfoAdapter] is the
 * version of logical camera.
 */
@SuppressLint(
    "UnsafeOptInUsageError" // Suppressed due to experimental API
)
public class PhysicalCameraInfoAdapter(private val cameraProperties: CameraProperties) :
    CameraInfo, UnsafeWrapper {

    @OptIn(ExperimentalCamera2Interop::class)
    internal val camera2CameraInfo: Camera2CameraInfo by lazy {
        Camera2CameraInfo.create(cameraProperties)
    }

    override fun getSensorRotationDegrees(): Int = getSensorRotationDegrees(Surface.ROTATION_0)

    override fun getSensorRotationDegrees(relativeRotation: Int): Int {
        val sensorOrientation: Int =
            cameraProperties.metadata[CameraCharacteristics.SENSOR_ORIENTATION]!!
        val relativeRotationDegrees =
            CameraOrientationUtil.surfaceRotationToDegrees(relativeRotation)
        // Currently this assumes that a back-facing camera is always opposite to the screen.
        // This may not be the case for all devices, so in the future we may need to handle that
        // scenario.
        val lensFacing = lensFacing
        val isOppositeFacingScreen = CameraSelector.LENS_FACING_BACK == lensFacing
        return CameraOrientationUtil.getRelativeImageRotation(
            relativeRotationDegrees,
            sensorOrientation,
            isOppositeFacingScreen
        )
    }

    override fun hasFlashUnit(): Boolean {
        throw UnsupportedOperationException("Physical camera doesn't support this function")
    }

    override fun getTorchState(): LiveData<Int> {
        throw UnsupportedOperationException("Physical camera doesn't support this function")
    }

    override fun getZoomState(): LiveData<ZoomState> {
        throw UnsupportedOperationException("Physical camera doesn't support this function")
    }

    override fun getExposureState(): ExposureState {
        throw UnsupportedOperationException("Physical camera doesn't support this function")
    }

    override fun getCameraState(): LiveData<CameraState> {
        throw UnsupportedOperationException("Physical camera doesn't support this function")
    }

    override fun getImplementationType(): String {
        throw UnsupportedOperationException("Physical camera doesn't support this function")
    }

    override fun getCameraSelector(): CameraSelector {
        throw UnsupportedOperationException("Physical camera doesn't support this function")
    }

    override fun getLensFacing(): Int =
        getCameraSelectorLensFacing(cameraProperties.metadata[CameraCharacteristics.LENS_FACING]!!)

    override fun getIntrinsicZoomRatio(): Float {
        throw UnsupportedOperationException("Physical camera doesn't support this function")
    }

    override fun isFocusMeteringSupported(action: FocusMeteringAction): Boolean {
        throw UnsupportedOperationException("Physical camera doesn't support this function")
    }

    @SuppressLint("NullAnnotationGroup")
    @ExperimentalZeroShutterLag
    override fun isZslSupported(): Boolean {
        throw UnsupportedOperationException("Physical camera doesn't support this function")
    }

    override fun getSupportedFrameRateRanges(): Set<Range<Int>> {
        throw UnsupportedOperationException("Physical camera doesn't support this function")
    }

    override fun isLogicalMultiCameraSupported(): Boolean {
        throw UnsupportedOperationException("Physical camera doesn't support this function")
    }

    override fun isPrivateReprocessingSupported(): Boolean {
        throw UnsupportedOperationException("Physical camera doesn't support this function")
    }

    override fun querySupportedDynamicRanges(
        candidateDynamicRanges: Set<DynamicRange>
    ): Set<DynamicRange> {
        throw UnsupportedOperationException("Physical camera doesn't support this function")
    }

    override fun getPhysicalCameraInfos(): Set<CameraInfo> {
        throw UnsupportedOperationException("Physical camera doesn't support this function")
    }

    @OptIn(ExperimentalCamera2Interop::class)
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: KClass<T>): T? =
        when (type) {
            Camera2CameraInfo::class -> camera2CameraInfo as T
            CameraProperties::class -> cameraProperties as T
            CameraMetadata::class -> cameraProperties.metadata as T
            else -> cameraProperties.metadata.unwrapAs(type)
        }

    private fun getCameraSelectorLensFacing(lensFacingInt: Int): @CameraSelector.LensFacing Int {
        return when (lensFacingInt) {
            CameraCharacteristics.LENS_FACING_FRONT -> CameraSelector.LENS_FACING_FRONT
            CameraCharacteristics.LENS_FACING_BACK -> CameraSelector.LENS_FACING_BACK
            CameraCharacteristics.LENS_FACING_EXTERNAL -> CameraSelector.LENS_FACING_EXTERNAL
            else ->
                throw IllegalArgumentException(
                    "The specified lens facing integer $lensFacingInt can not be recognized."
                )
        }
    }
}
