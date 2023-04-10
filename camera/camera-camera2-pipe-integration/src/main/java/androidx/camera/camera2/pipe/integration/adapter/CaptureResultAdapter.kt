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

import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureResult
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.integration.impl.CAMERAX_TAG_BUNDLE
import androidx.camera.core.impl.CameraCaptureMetaData.AeState
import androidx.camera.core.impl.CameraCaptureMetaData.AfMode
import androidx.camera.core.impl.CameraCaptureMetaData.AfState
import androidx.camera.core.impl.CameraCaptureMetaData.AwbState
import androidx.camera.core.impl.CameraCaptureMetaData.FlashState
import androidx.camera.core.impl.CameraCaptureResult
import androidx.camera.core.impl.TagBundle
import androidx.camera.core.impl.utils.ExifData
import java.nio.BufferUnderflowException

/**
 * Adapts the [CameraCaptureResult] interface to [CameraPipe].
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
class CaptureResultAdapter(
    private val requestMetadata: RequestMetadata,
    private val frameNumber: FrameNumber,
    private val result: FrameInfo
) : CameraCaptureResult {
    override fun getAfMode(): AfMode =
        when (val mode = result.metadata[CaptureResult.CONTROL_AF_MODE]) {
            CaptureResult.CONTROL_AF_MODE_OFF,
            CaptureResult.CONTROL_AF_MODE_EDOF -> AfMode.OFF
            CaptureResult.CONTROL_AF_MODE_AUTO,
            CaptureResult.CONTROL_AF_MODE_MACRO -> AfMode.ON_MANUAL_AUTO
            CaptureResult.CONTROL_AF_MODE_CONTINUOUS_PICTURE,
            CaptureResult.CONTROL_AF_MODE_CONTINUOUS_VIDEO -> AfMode.ON_CONTINUOUS_AUTO
            null -> AfMode.UNKNOWN
            else -> {
                Log.debug { "Unknown AF mode ($mode) for $frameNumber!" }
                AfMode.UNKNOWN
            }
        }

    override fun getAfState(): AfState =
        when (val state = result.metadata[CaptureResult.CONTROL_AF_STATE]) {
            CaptureResult.CONTROL_AF_STATE_INACTIVE -> AfState.INACTIVE
            CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN,
            CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN -> AfState.SCANNING
            CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED -> AfState.LOCKED_FOCUSED
            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED -> AfState.LOCKED_NOT_FOCUSED
            CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED -> AfState.PASSIVE_FOCUSED
            CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED -> AfState.PASSIVE_NOT_FOCUSED
            null -> AfState.UNKNOWN
            else -> {
                Log.debug { "Unknown AF state ($state) for $frameNumber!" }
                AfState.UNKNOWN
            }
        }

    override fun getAeState(): AeState =
        when (val state = result.metadata[CaptureResult.CONTROL_AE_STATE]) {
            CaptureResult.CONTROL_AE_STATE_INACTIVE -> AeState.INACTIVE
            CaptureResult.CONTROL_AE_STATE_SEARCHING,
            CaptureResult.CONTROL_AE_STATE_PRECAPTURE -> AeState.SEARCHING
            CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED -> AeState.FLASH_REQUIRED
            CaptureResult.CONTROL_AE_STATE_CONVERGED -> AeState.CONVERGED
            CaptureResult.CONTROL_AE_STATE_LOCKED -> AeState.LOCKED
            null -> AeState.UNKNOWN
            else -> {
                Log.debug { "Unknown AE state ($state) for $frameNumber!" }
                AeState.UNKNOWN
            }
        }

    override fun getAwbState(): AwbState =
        when (val state = result.metadata[CaptureResult.CONTROL_AWB_STATE]) {
            CaptureResult.CONTROL_AWB_STATE_INACTIVE -> AwbState.INACTIVE
            CaptureResult.CONTROL_AWB_STATE_SEARCHING -> AwbState.METERING
            CaptureResult.CONTROL_AWB_STATE_CONVERGED -> AwbState.CONVERGED
            CaptureResult.CONTROL_AWB_STATE_LOCKED -> AwbState.LOCKED
            null -> AwbState.UNKNOWN
            else -> {
                Log.debug { "Unknown AWB state ($state) for $frameNumber!" }
                AwbState.UNKNOWN
            }
        }

    override fun getFlashState(): FlashState =
        when (val state = result.metadata[CaptureResult.FLASH_STATE]) {
            CaptureResult.FLASH_STATE_UNAVAILABLE,
            CaptureResult.FLASH_STATE_CHARGING -> FlashState.NONE
            CaptureResult.FLASH_STATE_READY -> FlashState.READY
            CaptureResult.FLASH_STATE_FIRED,
            CaptureResult.FLASH_STATE_PARTIAL -> FlashState.FIRED
            null -> FlashState.UNKNOWN
            else -> {
                Log.debug { "Unknown flash state ($state) for $frameNumber!" }
                FlashState.UNKNOWN
            }
        }

    override fun getTimestamp(): Long {
        return result.metadata.getOrDefault(CaptureResult.SENSOR_TIMESTAMP, -1L)
    }

    override fun getTagBundle(): TagBundle {
        return requestMetadata.getOrDefault(CAMERAX_TAG_BUNDLE, TagBundle.emptyBundle())
    }

    override fun populateExifData(exifData: ExifData.Builder) {
        // Call interface default to set flash mode
        super.populateExifData(exifData)

        // Set dimensions
        result.metadata[CaptureResult.SCALER_CROP_REGION]?.let { cropRegion ->
            exifData.setImageWidth(cropRegion.width()).setImageHeight(cropRegion.height())
        }

        // Set orientation
        try {
            result.metadata[CaptureResult.JPEG_ORIENTATION]?.let { jpegOrientation ->
                exifData.setOrientationDegrees(jpegOrientation)
            }
        } catch (exception: BufferUnderflowException) {
            // On certain devices, e.g. Pixel 3 XL API 31, getting JPEG orientation on YUV stream
            // throws BufferUnderflowException. The value will be overridden in post-processing
            // anyway, so it's safe to ignore. Please reference: b/240998057
            Log.warn { "Failed to get JPEG orientation." }
        }

        // Set exposure time
        result.metadata[CaptureResult.SENSOR_EXPOSURE_TIME]?.let { exposureTimeNs ->
            exifData.setExposureTimeNanos(exposureTimeNs)
        }

        // Set the aperture
        result.metadata[CaptureResult.LENS_APERTURE]?.let { aperture ->
            exifData.setLensFNumber(aperture)
        }

        // Set the ISO
        result.metadata[CaptureResult.SENSOR_SENSITIVITY]?.let { iso ->
            exifData.setIso(iso)
            if (Build.VERSION.SDK_INT >= 24) {
                result.metadata[
                    CaptureResult.CONTROL_POST_RAW_SENSITIVITY_BOOST
                ]?.let { postRawSensitivityBoost ->
                    exifData.setIso(iso * (postRawSensitivityBoost / 100f).toInt())
                }
            }
        }

        // Set the focal length
        result.metadata[CaptureResult.LENS_FOCAL_LENGTH]?.let { focalLength ->
            exifData.setFocalLength(focalLength)
        }

        // Set white balance MANUAL/AUTO
        result.metadata[CaptureResult.CONTROL_AWB_MODE]?.let { whiteBalanceMode ->
            var wbMode = ExifData.WhiteBalanceMode.AUTO
            if (whiteBalanceMode == CameraMetadata.CONTROL_AWB_MODE_OFF) {
                wbMode = ExifData.WhiteBalanceMode.MANUAL
            }
            exifData.setWhiteBalanceMode(wbMode)
        }
    }
}
