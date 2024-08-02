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
import android.hardware.camera2.CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_ON
import android.hardware.camera2.CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION
import android.hardware.camera2.params.DynamicRangeProfiles
import android.os.Build
import android.util.Range
import android.util.Size
import android.view.Surface
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.CameraMetadata
import androidx.camera.camera2.pipe.CameraMetadata.Companion.isHardwareLevelLegacy
import androidx.camera.camera2.pipe.CameraMetadata.Companion.supportsLogicalMultiCamera
import androidx.camera.camera2.pipe.CameraMetadata.Companion.supportsPrivateReprocessing
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.UnsafeWrapper
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.integration.compat.DynamicRangeProfilesCompat
import androidx.camera.camera2.pipe.integration.compat.StreamConfigurationMapCompat
import androidx.camera.camera2.pipe.integration.compat.quirk.CameraQuirks
import androidx.camera.camera2.pipe.integration.compat.quirk.DeviceQuirks
import androidx.camera.camera2.pipe.integration.compat.quirk.ZslDisablerQuirk
import androidx.camera.camera2.pipe.integration.compat.workaround.isFlashAvailable
import androidx.camera.camera2.pipe.integration.config.CameraConfig
import androidx.camera.camera2.pipe.integration.config.CameraScope
import androidx.camera.camera2.pipe.integration.impl.CameraCallbackMap
import androidx.camera.camera2.pipe.integration.impl.CameraPipeCameraProperties
import androidx.camera.camera2.pipe.integration.impl.CameraProperties
import androidx.camera.camera2.pipe.integration.impl.DeviceInfoLogger
import androidx.camera.camera2.pipe.integration.impl.FocusMeteringControl
import androidx.camera.camera2.pipe.integration.internal.CameraFovInfo
import androidx.camera.camera2.pipe.integration.interop.Camera2CameraInfo
import androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.DynamicRange
import androidx.camera.core.DynamicRange.DOLBY_VISION_10_BIT
import androidx.camera.core.DynamicRange.DOLBY_VISION_8_BIT
import androidx.camera.core.DynamicRange.HDR10_10_BIT
import androidx.camera.core.DynamicRange.HDR10_PLUS_10_BIT
import androidx.camera.core.DynamicRange.HLG_10_BIT
import androidx.camera.core.DynamicRange.SDR
import androidx.camera.core.ExposureState
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ZoomState
import androidx.camera.core.impl.CameraCaptureCallback
import androidx.camera.core.impl.CameraInfoInternal
import androidx.camera.core.impl.DynamicRanges
import androidx.camera.core.impl.EncoderProfilesProvider
import androidx.camera.core.impl.Quirks
import androidx.camera.core.impl.Timebase
import androidx.camera.core.impl.utils.CameraOrientationUtil
import androidx.lifecycle.LiveData
import java.util.concurrent.Executor
import javax.inject.Inject
import kotlin.reflect.KClass

/** Adapt the [CameraInfoInternal] interface to [CameraPipe]. */
@SuppressLint(
    "UnsafeOptInUsageError" // Suppressed due to experimental ExposureState
)
@CameraScope
class CameraInfoAdapter
@Inject
constructor(
    private val cameraProperties: CameraProperties,
    private val cameraConfig: CameraConfig,
    private val cameraStateAdapter: CameraStateAdapter,
    private val cameraControlStateAdapter: CameraControlStateAdapter,
    private val cameraCallbackMap: CameraCallbackMap,
    private val focusMeteringControl: FocusMeteringControl,
    private val cameraQuirks: CameraQuirks,
    private val encoderProfilesProviderAdapter: EncoderProfilesProviderAdapter,
    private val streamConfigurationMapCompat: StreamConfigurationMapCompat,
    private val cameraFovInfo: CameraFovInfo,
) : CameraInfoInternal, UnsafeWrapper {
    init {
        DeviceInfoLogger.logDeviceInfo(cameraProperties)
    }

    private val _physicalCameraInfos by lazy {
        cameraProperties.metadata.physicalCameraIds.mapTo(mutableSetOf<CameraInfo>()) {
            physicalCameraId ->
            val cameraProperties =
                CameraPipeCameraProperties(
                    CameraConfig(physicalCameraId),
                    cameraProperties.metadata.awaitPhysicalMetadata(physicalCameraId)
                )
            PhysicalCameraInfoAdapter(cameraProperties)
        }
    }

    private val isLegacyDevice by lazy { cameraProperties.metadata.isHardwareLevelLegacy }

    @OptIn(ExperimentalCamera2Interop::class)
    internal val camera2CameraInfo: Camera2CameraInfo by lazy {
        Camera2CameraInfo.create(cameraProperties)
    }

    override fun isLogicalMultiCameraSupported(): Boolean {
        return cameraProperties.metadata.supportsLogicalMultiCamera
    }

    override fun getPhysicalCameraInfos(): Set<CameraInfo> = _physicalCameraInfos

    override fun getCameraId(): String = cameraConfig.cameraId.value

    override fun getLensFacing(): Int =
        getCameraSelectorLensFacing(cameraProperties.metadata[CameraCharacteristics.LENS_FACING]!!)

    override fun getCameraCharacteristics() =
        cameraProperties.metadata.unwrapAs(CameraCharacteristics::class)!!

    override fun getPhysicalCameraCharacteristics(physicalCameraId: String): Any? {
        val cameraId = CameraId.fromCamera2Id(physicalCameraId)
        if (!cameraProperties.metadata.physicalCameraIds.contains(cameraId)) {
            return null
        }
        return cameraProperties.metadata
            .awaitPhysicalMetadata(cameraId)
            .unwrapAs(CameraCharacteristics::class)
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

    override fun getImplementationType(): String =
        if (isLegacyDevice) CameraInfo.IMPLEMENTATION_TYPE_CAMERA2_LEGACY
        else CameraInfo.IMPLEMENTATION_TYPE_CAMERA2

    override fun getEncoderProfilesProvider(): EncoderProfilesProvider {
        return encoderProfilesProviderAdapter
    }

    override fun getTimebase(): Timebase {
        val timeSource =
            cameraProperties.metadata[CameraCharacteristics.SENSOR_INFO_TIMESTAMP_SOURCE]!!
        return when (timeSource) {
            CameraCharacteristics.SENSOR_INFO_TIMESTAMP_SOURCE_REALTIME -> Timebase.REALTIME
            CameraCharacteristics.SENSOR_INFO_TIMESTAMP_SOURCE_UNKNOWN -> Timebase.UPTIME
            else -> Timebase.UPTIME
        }
    }

    override fun getSupportedOutputFormats(): Set<Int> {
        return streamConfigurationMapCompat.getOutputFormats()?.toSet() ?: emptySet()
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

    @Suppress("UNCHECKED_CAST")
    @OptIn(ExperimentalCamera2Interop::class)
    override fun <T : Any> unwrapAs(type: KClass<T>): T? =
        when (type) {
            Camera2CameraInfo::class -> camera2CameraInfo as T
            CameraProperties::class -> cameraProperties as T
            CameraMetadata::class -> cameraProperties.metadata as T
            else -> cameraProperties.metadata.unwrapAs(type)
        }

    override fun toString(): String = "CameraInfoAdapter<$cameraConfig.cameraId>"

    override fun getCameraQuirks(): Quirks {
        return cameraQuirks.quirks
    }

    override fun isFocusMeteringSupported(action: FocusMeteringAction) =
        focusMeteringControl.isFocusMeteringSupported(action)

    override fun getSupportedFrameRateRanges(): Set<Range<Int>> =
        cameraProperties.metadata[CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES]
            ?.toSet() ?: emptySet()

    override fun isZslSupported(): Boolean {
        return Build.VERSION.SDK_INT >= 23 &&
            isPrivateReprocessingSupported &&
            DeviceQuirks[ZslDisablerQuirk::class.java] == null
    }

    override fun isPrivateReprocessingSupported(): Boolean {
        return cameraProperties.metadata.supportsPrivateReprocessing
    }

    override fun getSupportedDynamicRanges(): Set<DynamicRange> {
        return DynamicRangeProfilesCompat.fromCameraMetaData(cameraProperties.metadata)
            .supportedDynamicRanges
    }

    override fun querySupportedDynamicRanges(
        candidateDynamicRanges: Set<DynamicRange>
    ): Set<DynamicRange> {
        return DynamicRanges.findAllPossibleMatches(candidateDynamicRanges, supportedDynamicRanges)
    }

    override fun isPreviewStabilizationSupported(): Boolean {
        val availableVideoStabilizationModes =
            cameraProperties.metadata[
                    CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES]
        return availableVideoStabilizationModes != null &&
            availableVideoStabilizationModes.contains(
                CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION
            )
    }

    override fun isVideoStabilizationSupported(): Boolean {
        val availableVideoStabilizationModes =
            cameraProperties.metadata[
                    CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES]
        return availableVideoStabilizationModes != null &&
            availableVideoStabilizationModes.contains(CONTROL_VIDEO_STABILIZATION_MODE_ON)
    }

    override fun getIntrinsicZoomRatio(): Float {
        var intrinsicZoomRatio = CameraInfo.INTRINSIC_ZOOM_RATIO_UNKNOWN
        try {
            intrinsicZoomRatio =
                cameraFovInfo.getDefaultCameraDefaultViewAngleDegrees().toFloat() /
                    cameraFovInfo.getDefaultViewAngleDegrees().toFloat()
        } catch (e: Exception) {
            Log.error(e) { "Failed to get the intrinsic zoom ratio" }
        }

        return intrinsicZoomRatio
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
        private val PROFILE_TO_DR_MAP: Map<Long, DynamicRange> =
            mapOf(
                DynamicRangeProfiles.STANDARD to SDR,
                DynamicRangeProfiles.HLG10 to HLG_10_BIT,
                DynamicRangeProfiles.HDR10 to HDR10_10_BIT,
                DynamicRangeProfiles.HDR10_PLUS to HDR10_PLUS_10_BIT,
                DynamicRangeProfiles.DOLBY_VISION_10B_HDR_OEM to DOLBY_VISION_10_BIT,
                DynamicRangeProfiles.DOLBY_VISION_10B_HDR_OEM_PO to DOLBY_VISION_10_BIT,
                DynamicRangeProfiles.DOLBY_VISION_10B_HDR_REF to DOLBY_VISION_10_BIT,
                DynamicRangeProfiles.DOLBY_VISION_10B_HDR_REF_PO to DOLBY_VISION_10_BIT,
                DynamicRangeProfiles.DOLBY_VISION_8B_HDR_OEM to DOLBY_VISION_8_BIT,
                DynamicRangeProfiles.DOLBY_VISION_8B_HDR_OEM_PO to DOLBY_VISION_8_BIT,
                DynamicRangeProfiles.DOLBY_VISION_8B_HDR_REF to DOLBY_VISION_8_BIT,
                DynamicRangeProfiles.DOLBY_VISION_8B_HDR_REF_PO to DOLBY_VISION_8_BIT,
            )

        fun <T : Any> CameraInfo.unwrapAs(type: KClass<T>): T? =
            when (this) {
                is UnsafeWrapper -> this.unwrapAs(type)
                is CameraInfoInternal -> {
                    if (this.implementation !== this) {
                        this.implementation.unwrapAs(type)
                    } else {
                        null
                    }
                }
                else -> null
            }

        val CameraInfo.cameraId: CameraId?
            get() = this.unwrapAs(CameraMetadata::class)?.camera
    }
}
