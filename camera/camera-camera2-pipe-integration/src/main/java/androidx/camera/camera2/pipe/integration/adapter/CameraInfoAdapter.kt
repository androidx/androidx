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
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.params.DynamicRangeProfiles
import android.os.Build
import android.util.Range
import android.util.Size
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.integration.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.pipe.integration.compat.quirk.CameraQuirks
import androidx.camera.camera2.pipe.integration.compat.workaround.isFlashAvailable
import androidx.camera.camera2.pipe.integration.config.CameraConfig
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.camera2.pipe.integration.impl.CameraCallbackMap
import androidx.camera.camera2.pipe.integration.impl.CameraProperties
import androidx.camera.camera2.pipe.integration.impl.DeviceInfoLogger
import androidx.camera.camera2.pipe.integration.impl.FocusMeteringControl
import androidx.camera.camera2.pipe.integration.interop.Camera2CameraInfo
import androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.DynamicRange
import androidx.camera.core.DynamicRange.BIT_DEPTH_10_BIT
import androidx.camera.core.DynamicRange.BIT_DEPTH_8_BIT
import androidx.camera.core.DynamicRange.FORMAT_DOLBY_VISION
import androidx.camera.core.DynamicRange.FORMAT_HDR10
import androidx.camera.core.DynamicRange.FORMAT_HDR10_PLUS
import androidx.camera.core.DynamicRange.FORMAT_HLG
import androidx.camera.core.ExposureState
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ZoomState
import androidx.camera.core.impl.CameraCaptureCallback
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.EncoderProfilesProvider
import androidx.camera.core.impl.Quirks
import androidx.camera.core.impl.Timebase
import androidx.camera.core.impl.utils.CameraOrientationUtil
import androidx.lifecycle.LiveData
import java.util.concurrent.Executor
import javax.inject.Inject

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
    private val cameraStateAdapter: CameraStateAdapter,
    private val cameraControlStateAdapter: CameraControlStateAdapter,
    private val cameraCallbackMap: CameraCallbackMap,
    private val focusMeteringControl: FocusMeteringControl,
    private val cameraQuirks: CameraQuirks,
    private val encoderProfilesProviderAdapter: EncoderProfilesProviderAdapter,
    private val streamConfigurationMapCompat: StreamConfigurationMapCompat,
) : CameraInfoInternal {
    init { DeviceInfoLogger.logDeviceInfo(cameraProperties) }

    @OptIn(ExperimentalCamera2Interop::class)
    internal val camera2CameraInfo: Camera2CameraInfo by lazy {
        Camera2CameraInfo.create(cameraProperties)
    }

    override fun getCameraId(): String = cameraConfig.cameraId.value
    override fun getLensFacing(): Int =
        getCameraSelectorLensFacing(cameraProperties.metadata[CameraCharacteristics.LENS_FACING]!!)

    @CameraSelector.LensFacing
    private fun getCameraSelectorLensFacing(lensFacingInt: Int): Int {
        return when (lensFacingInt) {
            CameraCharacteristics.LENS_FACING_FRONT -> CameraSelector.LENS_FACING_FRONT
            CameraCharacteristics.LENS_FACING_BACK -> CameraSelector.LENS_FACING_BACK
            CameraCharacteristics.LENS_FACING_EXTERNAL -> CameraSelector.LENS_FACING_EXTERNAL
            else -> throw IllegalArgumentException(
                "The specified lens facing integer $lensFacingInt can not be recognized."
            )
        }
    }

    override fun getSensorRotationDegrees(): Int = getSensorRotationDegrees(Surface.ROTATION_0)
    override fun hasFlashUnit(): Boolean = cameraProperties.isFlashAvailable()

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

    override fun getZoomState(): LiveData<ZoomState> = cameraControlStateAdapter.zoomStateLiveData
    override fun getTorchState(): LiveData<Int> = cameraControlStateAdapter.torchStateLiveData

    @SuppressLint("UnsafeOptInUsageError")
    override fun getExposureState(): ExposureState = cameraControlStateAdapter.exposureState

    override fun getCameraState(): LiveData<CameraState> = cameraStateAdapter.cameraState

    override fun addSessionCaptureCallback(executor: Executor, callback: CameraCaptureCallback) =
        cameraCallbackMap.addCaptureCallback(callback, executor)

    override fun removeSessionCaptureCallback(callback: CameraCaptureCallback) =
        cameraCallbackMap.removeCaptureCallback(callback)

    override fun getImplementationType(): String = "CameraPipe"

    override fun getEncoderProfilesProvider(): EncoderProfilesProvider {
        return encoderProfilesProviderAdapter
    }

    override fun getTimebase(): Timebase {
        val timeSource = cameraProperties.metadata[
            CameraCharacteristics.SENSOR_INFO_TIMESTAMP_SOURCE
        ]!!
        return when (timeSource) {
            CameraMetadata.SENSOR_INFO_TIMESTAMP_SOURCE_REALTIME -> Timebase.REALTIME
            CameraMetadata.SENSOR_INFO_TIMESTAMP_SOURCE_UNKNOWN -> Timebase.UPTIME
            else -> Timebase.UPTIME
        }
    }

    @SuppressLint("ClassVerificationFailure")
    override fun getSupportedResolutions(format: Int): List<Size> {
        return streamConfigurationMapCompat.getOutputSizes(format)?.toList() ?: emptyList()
    }

    @SuppressLint("ClassVerificationFailure")
    override fun getSupportedHighResolutions(format: Int): List<Size> {
        return streamConfigurationMapCompat.getHighResolutionOutputSizes(format)?.toList()
            ?: emptyList()
    }

    override fun toString(): String = "CameraInfoAdapter<$cameraConfig.cameraId>"

    override fun getCameraQuirks(): Quirks {
        return cameraQuirks.quirks
    }

    override fun isFocusMeteringSupported(action: FocusMeteringAction) =
        focusMeteringControl.isFocusMeteringSupported(action)

    override fun getSupportedFrameRateRanges(): List<Range<Int>> {
        return cameraProperties
            .metadata[CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES]?.toList()
            ?: listOf()
    }

    override fun isZslSupported(): Boolean {
        Log.warn { "TODO: isZslSupported are not yet supported." }
        return false
    }

    override fun isPrivateReprocessingSupported(): Boolean {
        Log.warn { "TODO: isPrivateReprocessingSupported are not yet supported." }
        return false
    }

    @SuppressLint("ClassVerificationFailure")
    override fun getSupportedDynamicRanges(): Set<DynamicRange> {
        // TODO: use DynamicRangesCompat instead after it is migrates from camera-camera2.
        if (Build.VERSION.SDK_INT >= 33) {
            val availableProfiles = cameraProperties.metadata[
                CameraCharacteristics.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES]
            if (availableProfiles != null) {
                return profileSetToDynamicRangeSet(availableProfiles.supportedProfiles)
            }
        }
        return setOf(DynamicRange.SDR)
    }

    private fun profileSetToDynamicRangeSet(profileSet: Set<Long>): Set<DynamicRange> {
        return profileSet.map { profileToDynamicRange(it) }.toSet()
    }

    private fun profileToDynamicRange(profile: Long): DynamicRange {
        return checkNotNull(PROFILE_TO_DR_MAP[profile]) {
            "Dynamic range profile cannot be converted to a DynamicRange object: $profile"
        }
    }

    companion object {
        private val DR_HLG10 = DynamicRange(FORMAT_HLG, BIT_DEPTH_10_BIT)
        private val DR_HDR10 = DynamicRange(FORMAT_HDR10, BIT_DEPTH_10_BIT)
        private val DR_HDR10_PLUS = DynamicRange(FORMAT_HDR10_PLUS, BIT_DEPTH_10_BIT)
        private val DR_DOLBY_VISION_10_BIT = DynamicRange(FORMAT_DOLBY_VISION, BIT_DEPTH_10_BIT)
        private val DR_DOLBY_VISION_8_BIT = DynamicRange(FORMAT_DOLBY_VISION, BIT_DEPTH_8_BIT)
        private val PROFILE_TO_DR_MAP: Map<Long, DynamicRange> = mapOf(
            DynamicRangeProfiles.STANDARD to DynamicRange.SDR,
            DynamicRangeProfiles.HLG10 to DR_HLG10,
            DynamicRangeProfiles.HDR10 to DR_HDR10,
            DynamicRangeProfiles.HDR10_PLUS to DR_HDR10_PLUS,
            DynamicRangeProfiles.DOLBY_VISION_10B_HDR_OEM to DR_DOLBY_VISION_10_BIT,
            DynamicRangeProfiles.DOLBY_VISION_10B_HDR_OEM_PO to DR_DOLBY_VISION_10_BIT,
            DynamicRangeProfiles.DOLBY_VISION_10B_HDR_REF to DR_DOLBY_VISION_10_BIT,
            DynamicRangeProfiles.DOLBY_VISION_10B_HDR_REF_PO to DR_DOLBY_VISION_10_BIT,
            DynamicRangeProfiles.DOLBY_VISION_8B_HDR_OEM to DR_DOLBY_VISION_8_BIT,
            DynamicRangeProfiles.DOLBY_VISION_8B_HDR_OEM_PO to DR_DOLBY_VISION_8_BIT,
            DynamicRangeProfiles.DOLBY_VISION_8B_HDR_REF to DR_DOLBY_VISION_8_BIT,
            DynamicRangeProfiles.DOLBY_VISION_8B_HDR_REF_PO to DR_DOLBY_VISION_8_BIT,
        )
    }
}
