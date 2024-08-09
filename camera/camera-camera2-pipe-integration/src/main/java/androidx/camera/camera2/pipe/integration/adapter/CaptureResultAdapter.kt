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
import android.hardware.camera2.TotalCaptureResult
import android.os.Build
import androidx.camera.camera2.pipe.CameraPipe
import androidx.camera.camera2.pipe.FrameInfo
import androidx.camera.camera2.pipe.FrameMetadata
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.RequestMetadata
import androidx.camera.camera2.pipe.UnsafeWrapper
import androidx.camera.camera2.pipe.core.Log
import androidx.camera.camera2.pipe.integration.impl.CAMERAX_TAG_BUNDLE
import androidx.camera.core.impl.CameraCaptureMetaData.AeMode
import androidx.camera.core.impl.CameraCaptureMetaData.AeState
import androidx.camera.core.impl.CameraCaptureMetaData.AfMode
import androidx.camera.core.impl.CameraCaptureMetaData.AfState
import androidx.camera.core.impl.CameraCaptureMetaData.AwbMode
import androidx.camera.core.impl.CameraCaptureMetaData.AwbState
import androidx.camera.core.impl.CameraCaptureMetaData.FlashState
import androidx.camera.core.impl.CameraCaptureResult
import androidx.camera.core.impl.TagBundle
import androidx.camera.core.impl.utils.ExifData
import java.nio.BufferUnderflowException
import kotlin.reflect.KClass

public class PartialCaptureResultAdapter(
    private val requestMetadata: RequestMetadata,
    private val frameNumber: FrameNumber,
    private val result: FrameMetadata,
) : CameraCaptureResult, UnsafeWrapper {
    override fun getAfMode(): AfMode = result.getAfMode()

    override fun getAfState(): AfState = result.getAfState()

    override fun getAeMode(): AeMode = result.getAeMode()

    override fun getAeState(): AeState = result.getAeState()

    override fun getAwbMode(): AwbMode = result.getAwbMode()

    override fun getAwbState(): AwbState = result.getAwbState()

    override fun getFlashState(): FlashState = result.getFlashState()

    override fun getTimestamp(): Long = result.getTimestamp()

    override fun getTagBundle(): TagBundle {
        return requestMetadata.getOrDefault(CAMERAX_TAG_BUNDLE, TagBundle.emptyBundle())
    }

    override fun populateExifData(exifBuilder: ExifData.Builder) {
        super.populateExifData(exifBuilder)
        result.populateExifData(exifBuilder)
    }

    override fun getCaptureResult(): CaptureResult =
        checkNotNull(unwrapAs(CaptureResult::class)) { "Failed to unwrap $this as CaptureResult" }

    override fun <T : Any> unwrapAs(type: KClass<T>): T? = result.unwrapAs(type)
}

/** Adapts the [CameraCaptureResult] interface to [CameraPipe]. */
public class CaptureResultAdapter(
    private val requestMetadata: RequestMetadata,
    private val frameNumber: FrameNumber,
    internal val result: FrameInfo
) : CameraCaptureResult, UnsafeWrapper {
    override fun getAfMode(): AfMode = result.metadata.getAfMode()

    override fun getAfState(): AfState = result.metadata.getAfState()

    override fun getAeMode(): AeMode = result.metadata.getAeMode()

    override fun getAeState(): AeState = result.metadata.getAeState()

    override fun getAwbMode(): AwbMode = result.metadata.getAwbMode()

    override fun getAwbState(): AwbState = result.metadata.getAwbState()

    override fun getFlashState(): FlashState = result.metadata.getFlashState()

    override fun getTimestamp(): Long = result.metadata.getTimestamp()

    override fun getTagBundle(): TagBundle {
        return requestMetadata.getOrDefault(CAMERAX_TAG_BUNDLE, TagBundle.emptyBundle())
    }

    override fun populateExifData(exifBuilder: ExifData.Builder) {
        super.populateExifData(exifBuilder)
        result.metadata.populateExifData(exifBuilder)
    }

    override fun getCaptureResult(): CaptureResult =
        checkNotNull(unwrapAs(TotalCaptureResult::class)) {
            "Failed to unwrap $this as TotalCaptureResult"
        }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> unwrapAs(type: KClass<T>): T? {
        return when (type) {
            FrameInfo::class -> result as T
            else -> result.unwrapAs(type)
        }
    }
}

private fun FrameMetadata.getAfMode(): AfMode =
    when (val mode = this[CaptureResult.CONTROL_AF_MODE]) {
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

private fun FrameMetadata.getAfState(): AfState =
    when (val state = this[CaptureResult.CONTROL_AF_STATE]) {
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

private fun FrameMetadata.getAeMode(): AeMode =
    when (val mode = this[CaptureResult.CONTROL_AE_MODE]) {
        CaptureResult.CONTROL_AE_MODE_OFF -> AeMode.OFF
        CaptureResult.CONTROL_AE_MODE_ON -> AeMode.ON
        CaptureResult.CONTROL_AE_MODE_ON_AUTO_FLASH -> AeMode.ON_AUTO_FLASH
        CaptureResult.CONTROL_AE_MODE_ON_ALWAYS_FLASH -> AeMode.ON_ALWAYS_FLASH
        CaptureResult.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE -> AeMode.ON_AUTO_FLASH_REDEYE
        null -> AeMode.UNKNOWN
        else -> {
            Log.debug { "Unknown AE mode ($mode) for $frameNumber!" }
            AeMode.UNKNOWN
        }
    }

private fun FrameMetadata.getAeState(): AeState =
    when (val state = this[CaptureResult.CONTROL_AE_STATE]) {
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

private fun FrameMetadata.getAwbMode(): AwbMode =
    when (val mode = this[CaptureResult.CONTROL_AWB_MODE]) {
        CaptureResult.CONTROL_AWB_MODE_OFF -> AwbMode.OFF
        CaptureResult.CONTROL_AWB_MODE_AUTO -> AwbMode.AUTO
        CaptureResult.CONTROL_AWB_MODE_INCANDESCENT -> AwbMode.INCANDESCENT
        CaptureResult.CONTROL_AWB_MODE_FLUORESCENT -> AwbMode.FLUORESCENT
        CaptureResult.CONTROL_AWB_MODE_WARM_FLUORESCENT -> AwbMode.WARM_FLUORESCENT
        CaptureResult.CONTROL_AWB_MODE_DAYLIGHT -> AwbMode.DAYLIGHT
        CaptureResult.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT -> AwbMode.CLOUDY_DAYLIGHT
        CaptureResult.CONTROL_AWB_MODE_TWILIGHT -> AwbMode.TWILIGHT
        CaptureResult.CONTROL_AWB_MODE_SHADE -> AwbMode.SHADE
        null -> AwbMode.UNKNOWN
        else -> {
            Log.debug { "Unknown AWB mode ($mode) for $frameNumber!" }
            AwbMode.UNKNOWN
        }
    }

private fun FrameMetadata.getAwbState(): AwbState =
    when (val state = this[CaptureResult.CONTROL_AWB_STATE]) {
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

private fun FrameMetadata.getFlashState(): FlashState =
    when (val state = this[CaptureResult.FLASH_STATE]) {
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

private fun FrameMetadata.getTimestamp(): Long = getOrDefault(CaptureResult.SENSOR_TIMESTAMP, -1L)

private fun FrameMetadata.populateExifData(exifData: ExifData.Builder) {
    // Set orientation
    try {
        this[CaptureResult.JPEG_ORIENTATION]?.let { jpegOrientation ->
            exifData.setOrientationDegrees(jpegOrientation)
        }
    } catch (exception: BufferUnderflowException) {
        // On certain devices, e.g. Pixel 3 XL API 31, getting JPEG orientation on YUV stream
        // throws BufferUnderflowException. The value will be overridden in post-processing
        // anyway, so it's safe to ignore. Please reference: b/240998057
        // TODO: b/316233308 - Handle the exception inside in CameraPipe.
        Log.warn { "Failed to get JPEG orientation." }
    }

    // Set exposure time
    this[CaptureResult.SENSOR_EXPOSURE_TIME]?.let { exposureTimeNs ->
        exifData.setExposureTimeNanos(exposureTimeNs)
    }

    // Set the aperture
    this[CaptureResult.LENS_APERTURE]?.let { aperture -> exifData.setLensFNumber(aperture) }

    // Set the ISO
    this[CaptureResult.SENSOR_SENSITIVITY]?.let { iso ->
        exifData.setIso(iso)
        if (Build.VERSION.SDK_INT >= 24) {
            this[CaptureResult.CONTROL_POST_RAW_SENSITIVITY_BOOST]?.let { postRawSensitivityBoost ->
                exifData.setIso(iso * (postRawSensitivityBoost / 100f).toInt())
            }
        }
    }

    // Set the focal length
    this[CaptureResult.LENS_FOCAL_LENGTH]?.let { focalLength ->
        exifData.setFocalLength(focalLength)
    }

    // Set white balance MANUAL/AUTO
    this[CaptureResult.CONTROL_AWB_MODE]?.let { whiteBalanceMode ->
        var wbMode = ExifData.WhiteBalanceMode.AUTO
        if (whiteBalanceMode == CameraMetadata.CONTROL_AWB_MODE_OFF) {
            wbMode = ExifData.WhiteBalanceMode.MANUAL
        }
        exifData.setWhiteBalanceMode(wbMode)
    }
}
