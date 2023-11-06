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

package androidx.camera.camera2.pipe.integration.adapter

import android.graphics.Rect
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.os.Build
import androidx.camera.camera2.pipe.FrameNumber
import androidx.camera.camera2.pipe.RequestNumber
import androidx.camera.camera2.pipe.testing.FakeFrameInfo
import androidx.camera.camera2.pipe.testing.FakeFrameMetadata
import androidx.camera.camera2.pipe.testing.FakeRequestMetadata
import androidx.camera.core.impl.CameraCaptureMetaData.AeState
import androidx.camera.core.impl.CameraCaptureMetaData.AfMode
import androidx.camera.core.impl.CameraCaptureMetaData.AfState
import androidx.camera.core.impl.CameraCaptureMetaData.AwbState
import androidx.camera.core.impl.CameraCaptureMetaData.FlashState
import androidx.camera.core.impl.CameraCaptureResult
import androidx.camera.core.impl.utils.ExifData
import androidx.exifinterface.media.ExifInterface
import androidx.exifinterface.media.ExifInterface.FLAG_FLASH_FIRED
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
@DoNotInstrument
class CaptureResultAdapterTest {

    @Test
    fun getAfMode_withNull() {
        val cameraCaptureResult = createCaptureResultAdapter(
            resultMetadata = mapOf(CaptureResult.CONTROL_AF_MODE to null)
        )

        assertThat(cameraCaptureResult.afMode).isEqualTo(AfMode.UNKNOWN)
    }

    @Test
    fun getAfMode_withAfModeOff() {
        val cameraCaptureResult = createCaptureResultAdapter(
            resultMetadata = mapOf(
                CaptureResult.CONTROL_AF_MODE to CaptureResult.CONTROL_AF_MODE_OFF
            )
        )

        assertThat(cameraCaptureResult.afMode).isEqualTo(AfMode.OFF)
    }

    @Test
    fun getAfMode_withAfModeEdof() {
        val cameraCaptureResult = createCaptureResultAdapter(
            resultMetadata = mapOf(
                CaptureResult.CONTROL_AF_MODE to CaptureResult.CONTROL_AF_MODE_EDOF
            )
        )

        assertThat(cameraCaptureResult.afMode).isEqualTo(AfMode.OFF)
    }

    @Test
    fun getAfMode_withAfModeAuto() {
        val cameraCaptureResult = createCaptureResultAdapter(
            resultMetadata = mapOf(
                CaptureResult.CONTROL_AF_MODE to CaptureResult.CONTROL_AF_MODE_AUTO
            )
        )

        assertThat(cameraCaptureResult.afMode).isEqualTo(AfMode.ON_MANUAL_AUTO)
    }

    @Test
    fun getAfMode_withAfModeMacro() {
        val cameraCaptureResult = createCaptureResultAdapter(
            resultMetadata = mapOf(
                CaptureResult.CONTROL_AF_MODE to CaptureResult.CONTROL_AF_MODE_MACRO
            )
        )

        assertThat(cameraCaptureResult.afMode).isEqualTo(AfMode.ON_MANUAL_AUTO)
    }

    @Test
    fun getAfMode_withAfModeContinuousPicture() {
        val cameraCaptureResult = createCaptureResultAdapter(
            resultMetadata = mapOf(
                CaptureResult.CONTROL_AF_MODE to CaptureResult.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )
        )

        assertThat(cameraCaptureResult.afMode).isEqualTo(AfMode.ON_CONTINUOUS_AUTO)
    }

    @Test
    fun getAfMode_withAfModeContinuousVideo() {
        val cameraCaptureResult = createCaptureResultAdapter(
            resultMetadata = mapOf(
                CaptureResult.CONTROL_AF_MODE to CaptureResult.CONTROL_AF_MODE_CONTINUOUS_VIDEO
            )
        )

        assertThat(cameraCaptureResult.afMode).isEqualTo(AfMode.ON_CONTINUOUS_AUTO)
    }

    @Test
    fun getAfState_withNull() {
        val cameraCaptureResult = createCaptureResultAdapter(
            resultMetadata = mapOf(CaptureResult.CONTROL_AF_STATE to null)
        )

        assertThat(cameraCaptureResult.afState).isEqualTo(AfState.UNKNOWN)
    }

    @Test
    fun getAfState_withAfStateInactive() {
        val cameraCaptureResult = createCaptureResultAdapter(
            resultMetadata = mapOf(
                CaptureResult.CONTROL_AF_STATE to CaptureResult.CONTROL_AF_STATE_INACTIVE
            )
        )

        assertThat(cameraCaptureResult.afState).isEqualTo(AfState.INACTIVE)
    }

    @Test
    fun getAfState_withAfStateActiveScan() {
        val cameraCaptureResult = createCaptureResultAdapter(
            resultMetadata = mapOf(
                CaptureResult.CONTROL_AF_STATE to CaptureResult.CONTROL_AF_STATE_ACTIVE_SCAN
            )
        )

        assertThat(cameraCaptureResult.afState).isEqualTo(AfState.SCANNING)
    }

    @Test
    fun getAfState_withAfStatePassiveScan() {
        val cameraCaptureResult = createCaptureResultAdapter(
            resultMetadata = mapOf(
                CaptureResult.CONTROL_AF_STATE to CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN
            )
        )

        assertThat(cameraCaptureResult.afState).isEqualTo(AfState.SCANNING)
    }

    @Test
    fun getAfState_withAfStatePassiveUnfocused() {
        val cameraCaptureResult = createCaptureResultAdapter(
            resultMetadata = mapOf(
                CaptureResult.CONTROL_AF_STATE to CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED
            )
        )

        assertThat(cameraCaptureResult.afState).isEqualTo(AfState.PASSIVE_NOT_FOCUSED)
    }

    @Test
    fun getAfState_withAfStatePassiveFocused() {
        val cameraCaptureResult = createCaptureResultAdapter(
            resultMetadata = mapOf(
                CaptureResult.CONTROL_AF_STATE to CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED
            )
        )

        assertThat(cameraCaptureResult.afState).isEqualTo(AfState.PASSIVE_FOCUSED)
    }

    @Test
    fun getAfState_withAfStateFocusedLocked() {
        val cameraCaptureResult = createCaptureResultAdapter(
            resultMetadata = mapOf(
                CaptureResult.CONTROL_AF_STATE to CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
            )
        )

        assertThat(cameraCaptureResult.afState).isEqualTo(AfState.LOCKED_FOCUSED)
    }

    @Test
    fun getAfState_withAfStateNotFocusedLocked() {
        val cameraCaptureResult = createCaptureResultAdapter(
            resultMetadata = mapOf(
                CaptureResult.CONTROL_AF_STATE to CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED
            )
        )

        assertThat(cameraCaptureResult.afState).isEqualTo(AfState.LOCKED_NOT_FOCUSED)
    }

    @Test
    fun getAeState_withNull() {
        val cameraCaptureResult = createCaptureResultAdapter(
            resultMetadata = mapOf(CaptureResult.CONTROL_AE_STATE to null)
        )

        assertThat(cameraCaptureResult.aeState).isEqualTo(AeState.UNKNOWN)
    }

    @Test
    fun getAeState_withAeStateInactive() {
        val cameraCaptureResult = createCaptureResultAdapter(
            resultMetadata = mapOf(
                CaptureResult.CONTROL_AE_STATE to CaptureResult.CONTROL_AE_STATE_INACTIVE
            )
        )

        assertThat(cameraCaptureResult.aeState).isEqualTo(AeState.INACTIVE)
    }

    @Test
    fun getAeState_withAeStateSearching() {
        val cameraCaptureResult = createCaptureResultAdapter(
            resultMetadata = mapOf(
                CaptureResult.CONTROL_AE_STATE to CaptureResult.CONTROL_AE_STATE_SEARCHING
            )
        )

        assertThat(cameraCaptureResult.aeState).isEqualTo(AeState.SEARCHING)
    }

    @Test
    fun getAeState_withAeStatePrecapture() {
        val cameraCaptureResult = createCaptureResultAdapter(
            resultMetadata = mapOf(
                CaptureResult.CONTROL_AE_STATE to CaptureResult.CONTROL_AE_STATE_PRECAPTURE
            )
        )

        assertThat(cameraCaptureResult.aeState).isEqualTo(AeState.SEARCHING)
    }

    @Test
    fun getAeState_withAeStateFlashRequired() {
        val cameraCaptureResult = createCaptureResultAdapter(
            resultMetadata = mapOf(
                CaptureResult.CONTROL_AE_STATE to CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED
            )
        )

        assertThat(cameraCaptureResult.aeState).isEqualTo(AeState.FLASH_REQUIRED)
    }

    @Test
    fun getAeState_withAeStateConverged() {
        val cameraCaptureResult = createCaptureResultAdapter(
            resultMetadata = mapOf(
                CaptureResult.CONTROL_AE_STATE to CaptureResult.CONTROL_AE_STATE_CONVERGED
            )
        )

        assertThat(cameraCaptureResult.aeState).isEqualTo(AeState.CONVERGED)
    }

    @Test
    fun getAeState_withAeStateLocked() {
        val cameraCaptureResult = createCaptureResultAdapter(
            resultMetadata = mapOf(
                CaptureResult.CONTROL_AE_STATE to CaptureResult.CONTROL_AE_STATE_LOCKED
            )
        )

        assertThat(cameraCaptureResult.aeState).isEqualTo(AeState.LOCKED)
    }

    @Test
    fun getAwbState_withNull() {
        val cameraCaptureResult = createCaptureResultAdapter(
            resultMetadata = mapOf(CaptureResult.CONTROL_AWB_STATE to null)
        )

        assertThat(cameraCaptureResult.awbState).isEqualTo(AwbState.UNKNOWN)
    }

    @Test
    fun getAwbState_withAwbStateInactive() {
        val cameraCaptureResult = createCaptureResultAdapter(
            resultMetadata = mapOf(
                CaptureResult.CONTROL_AWB_STATE to CaptureResult.CONTROL_AWB_STATE_INACTIVE
            )
        )

        assertThat(cameraCaptureResult.awbState).isEqualTo(AwbState.INACTIVE)
    }

    @Test
    fun getAwbState_withAwbStateSearching() {
        val cameraCaptureResult = createCaptureResultAdapter(
            resultMetadata = mapOf(
                CaptureResult.CONTROL_AWB_STATE to CaptureResult.CONTROL_AWB_STATE_SEARCHING
            )
        )

        assertThat(cameraCaptureResult.awbState).isEqualTo(AwbState.METERING)
    }

    @Test
    fun getAwbState_withAwbStateConverged() {
        val cameraCaptureResult = createCaptureResultAdapter(
            resultMetadata = mapOf(
                CaptureResult.CONTROL_AWB_STATE to CaptureResult.CONTROL_AWB_STATE_CONVERGED
            )
        )

        assertThat(cameraCaptureResult.awbState).isEqualTo(AwbState.CONVERGED)
    }

    @Test
    fun getAwbState_withAwbStateLocked() {
        val cameraCaptureResult = createCaptureResultAdapter(
            resultMetadata = mapOf(
                CaptureResult.CONTROL_AWB_STATE to CaptureResult.CONTROL_AWB_STATE_LOCKED
            )
        )

        assertThat(cameraCaptureResult.awbState).isEqualTo(AwbState.LOCKED)
    }

    @Test
    fun getFlashState_withNull() {
        val cameraCaptureResult = createCaptureResultAdapter(
            resultMetadata = mapOf(CaptureResult.FLASH_STATE to null)
        )

        assertThat(cameraCaptureResult.flashState).isEqualTo(FlashState.UNKNOWN)
    }

    @Test
    fun getFlashState_withFlashStateUnavailable() {
        val cameraCaptureResult = createCaptureResultAdapter(
            resultMetadata = mapOf(
                CaptureResult.FLASH_STATE to CaptureResult.FLASH_STATE_UNAVAILABLE
            )
        )

        assertThat(cameraCaptureResult.flashState).isEqualTo(FlashState.NONE)
    }

    @Test
    fun getFlashState_withFlashStateCharging() {
        val cameraCaptureResult = createCaptureResultAdapter(
            resultMetadata = mapOf(CaptureResult.FLASH_STATE to CaptureResult.FLASH_STATE_CHARGING)
        )

        assertThat(cameraCaptureResult.flashState).isEqualTo(FlashState.NONE)
    }

    @Test
    fun getFlashState_withFlashStateReady() {
        val cameraCaptureResult = createCaptureResultAdapter(
            resultMetadata = mapOf(CaptureResult.FLASH_STATE to CaptureResult.FLASH_STATE_READY)
        )

        assertThat(cameraCaptureResult.flashState).isEqualTo(FlashState.READY)
    }

    @Test
    fun getFlashState_withFlashStateFired() {
        val cameraCaptureResult = createCaptureResultAdapter(
            resultMetadata = mapOf(CaptureResult.FLASH_STATE to CaptureResult.FLASH_STATE_FIRED)
        )

        assertThat(cameraCaptureResult.flashState).isEqualTo(FlashState.FIRED)
    }

    @Test
    fun getFlashState_withFlashStatePartial() {
        val cameraCaptureResult = createCaptureResultAdapter(
            resultMetadata = mapOf(CaptureResult.FLASH_STATE to CaptureResult.FLASH_STATE_PARTIAL)
        )

        assertThat(cameraCaptureResult.flashState).isEqualTo(FlashState.FIRED)
    }

    @Test
    fun canPopulateExif() {
        // Arrange
        val cropRegion = Rect(0, 0, 640, 480)
        val exposureTime = TimeUnit.SECONDS.toNanos(5)
        val aperture = 1.8f
        val iso = 200
        val postRawSensitivityBoost = if (Build.VERSION.SDK_INT >= 24) {
            200 // Add boost for API >= 24
        } else {
            100 // No boost for API < 24
        }
        val focalLength = 4200f
        val cameraCaptureResult = createCaptureResultAdapter(
            resultMetadata = mutableMapOf(
                CaptureResult.FLASH_STATE to CaptureResult.FLASH_STATE_FIRED,
                CaptureResult.SCALER_CROP_REGION to cropRegion,
                CaptureResult.JPEG_ORIENTATION to 270,
                CaptureResult.SENSOR_EXPOSURE_TIME to exposureTime,
                CaptureResult.LENS_APERTURE to aperture,
                CaptureResult.SENSOR_SENSITIVITY to iso,
                CaptureResult.LENS_FOCAL_LENGTH to focalLength,
                CaptureResult.CONTROL_AWB_MODE to CameraMetadata.CONTROL_AWB_MODE_OFF,
            ).apply {
                if (Build.VERSION.SDK_INT >= 24) {
                    put(CaptureResult.CONTROL_POST_RAW_SENSITIVITY_BOOST, postRawSensitivityBoost)
                }
            }
        )

        // Act
        val exifData = ExifData.builderForDevice().also { exifBuilder ->
            cameraCaptureResult.populateExifData(exifBuilder)
        }.build()

        // Assert
        assertThat(exifData.getAttribute(ExifInterface.TAG_FLASH)!!.toShort()).isEqualTo(
            FLAG_FLASH_FIRED
        )
        assertThat(exifData.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)).isEqualTo(
            cropRegion.width().toString()
        )
        assertThat(exifData.getAttribute(ExifInterface.TAG_IMAGE_LENGTH)).isEqualTo(
            cropRegion.height().toString()
        )
        assertThat(exifData.getAttribute(ExifInterface.TAG_ORIENTATION)).isEqualTo(
            ExifInterface.ORIENTATION_ROTATE_270.toString()
        )

        val exposureTimeString = exifData.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
        assertThat(exposureTimeString).isNotNull()
        assertThat(exposureTimeString!!.toFloat()).isWithin(0.1f)
            .of(TimeUnit.NANOSECONDS.toSeconds(exposureTime).toFloat())

        assertThat(exifData.getAttribute(ExifInterface.TAG_F_NUMBER)).isEqualTo(aperture.toString())
        assertThat(exifData.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)!!.toShort())
            .isEqualTo((iso * (postRawSensitivityBoost / 100f).toInt()).toShort())

        val focalLengthString = exifData.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)
        assertThat(focalLengthString).isNotNull()
        val fractionValues = focalLengthString!!.split("/".toRegex()).dropLastWhile {
            it.isEmpty()
        }.toTypedArray()
        val numerator = fractionValues[0].toLong()
        val denominator = fractionValues[1].toLong()
        assertThat(numerator / denominator.toFloat()).isWithin(0.1f).of(focalLength)

        assertThat(exifData.getAttribute(ExifInterface.TAG_WHITE_BALANCE)!!.toShort()).isEqualTo(
            ExifInterface.WHITE_BALANCE_MANUAL
        )
    }

    private fun createCaptureResultAdapter(
        requestParams: Map<CaptureRequest.Key<*>, Any?> = emptyMap(),
        resultMetadata: Map<CaptureResult.Key<*>, Any?> = emptyMap(),
        frameNumber: FrameNumber = FrameNumber(101L)
    ): CameraCaptureResult {
        val requestMetadata = FakeRequestMetadata(
            requestParameters = requestParams,
            requestNumber = RequestNumber(1)
        )
        val resultMetaData = FakeFrameMetadata(
            resultMetadata = resultMetadata,
            frameNumber = frameNumber,
        )
        return CaptureResultAdapter(
            requestMetadata = requestMetadata,
            frameNumber,
            FakeFrameInfo(
                metadata = resultMetaData,
                requestMetadata = requestMetadata,
            )
        )
    }
}
