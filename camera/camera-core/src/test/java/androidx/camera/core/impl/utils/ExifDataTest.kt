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

package androidx.camera.core.impl.utils

import android.os.Build
import androidx.camera.core.impl.CameraCaptureMetaData
import androidx.exifinterface.media.ExifInterface
import androidx.exifinterface.media.ExifInterface.FLAG_FLASH_FIRED
import androidx.exifinterface.media.ExifInterface.FLAG_FLASH_NO_FLASH_FUNCTION
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class ExifDataTest {

    @Test
    public fun canSetImageWidth() {
        val exifData = ExifData.builderForDevice().setImageWidth(100).build()
        assertThat(exifData.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)).isEqualTo("100")
    }

    @Test
    public fun canSetImageHeight() {
        val exifData = ExifData.builderForDevice().setImageHeight(200).build()
        assertThat(exifData.getAttribute(ExifInterface.TAG_IMAGE_LENGTH)).isEqualTo("200")
    }

    @Test
    public fun canSetOrientationDegrees() {
        val exifData0 = ExifData.builderForDevice().setOrientationDegrees(0).build()
        val exifData90 = ExifData.builderForDevice().setOrientationDegrees(90).build()
        val exifData180 = ExifData.builderForDevice().setOrientationDegrees(180).build()
        val exifData270 = ExifData.builderForDevice().setOrientationDegrees(270).build()

        assertThat(exifData0.getAttribute(ExifInterface.TAG_ORIENTATION))
            .isEqualTo("${ExifInterface.ORIENTATION_NORMAL}")
        assertThat(exifData90.getAttribute(ExifInterface.TAG_ORIENTATION))
            .isEqualTo("${ExifInterface.ORIENTATION_ROTATE_90}")
        assertThat(exifData180.getAttribute(ExifInterface.TAG_ORIENTATION))
            .isEqualTo("${ExifInterface.ORIENTATION_ROTATE_180}")
        assertThat(exifData270.getAttribute(ExifInterface.TAG_ORIENTATION))
            .isEqualTo("${ExifInterface.ORIENTATION_ROTATE_270}")
    }

    @Test
    public fun settingInvalidOrientationIsUndefined() {
        // Only 0, 90, 180 and 270 are valid orientations. Use an invalid orientation.
        val exifData = ExifData.builderForDevice().setOrientationDegrees(42).build()

        assertThat(exifData.getAttribute(ExifInterface.TAG_ORIENTATION))
            .isEqualTo("${ExifInterface.ORIENTATION_UNDEFINED}")
    }

    @Test
    public fun canSetFlashState() {
        val exifDataFired = ExifData.builderForDevice()
            .setFlashState(CameraCaptureMetaData.FlashState.FIRED)
            .build()
        val exifDataReady = ExifData.builderForDevice()
            .setFlashState(CameraCaptureMetaData.FlashState.READY)
            .build()
        val exifDataNone = ExifData.builderForDevice()
            .setFlashState(CameraCaptureMetaData.FlashState.NONE)
            .build()

        // Unknown should not set the attribute
        val exifDataUnknown = ExifData.builderForDevice()
            .setFlashState(CameraCaptureMetaData.FlashState.UNKNOWN)
            .build()

        // Flash fired.
        assertThat(exifDataFired.getAttribute(ExifInterface.TAG_FLASH)?.toShort())
            .isEqualTo(FLAG_FLASH_FIRED)

        // Has flash but not fired.
        assertThat(exifDataReady.getAttribute(ExifInterface.TAG_FLASH)?.toShort())
            .isEqualTo(0)

        // No flash function.
        assertThat(exifDataNone.getAttribute(ExifInterface.TAG_FLASH)?.toShort())
            .isEqualTo(FLAG_FLASH_NO_FLASH_FUNCTION)

        assertThat(exifDataUnknown.getAttribute(ExifInterface.TAG_FLASH)).isNull()
    }

    @Test
    public fun canSetExposureTime() {
        val exifData = ExifData.builderForDevice()
            .setExposureTimeNanos(TimeUnit.SECONDS.toNanos(5))
            .build()
        assertThat(exifData.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.toFloat()?.toInt())
            .isEqualTo(5)
    }

    @Test
    public fun canSetLensFNumber() {
        val exifData = ExifData.builderForDevice()
            .setLensFNumber(1.2f)
            .build()
        assertThat(exifData.getAttribute(ExifInterface.TAG_F_NUMBER)).isEqualTo("1.2")
    }

    @Test
    public fun canSetIso() {
        val exifData = ExifData.builderForDevice()
            .setIso(800)
            .build()
        assertThat(exifData.getAttribute(ExifInterface.TAG_SENSITIVITY_TYPE))
            .isEqualTo("${ExifInterface.SENSITIVITY_TYPE_ISO_SPEED}")
        assertThat(exifData.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY))
            .isEqualTo("800")
    }

    @Test
    public fun canSetFocalLength() {
        val exifData = ExifData.builderForDevice()
            .setFocalLength(5400f /*millimeters*/)
            .build()
        assertThat(
            exifData.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)
                ?.split("/")
                ?.map(String::toLong)
                ?.reduce { numerator: Long, denominator: Long -> numerator / denominator }
        ).isEqualTo(5400)
    }

    @Test
    public fun canSetWhiteBalanceMode() {
        val exifDataAuto = ExifData.builderForDevice()
            .setWhiteBalanceMode(ExifData.WhiteBalanceMode.AUTO)
            .build()
        val exifDataManual = ExifData.builderForDevice()
            .setWhiteBalanceMode(ExifData.WhiteBalanceMode.MANUAL)
            .build()

        assertThat(exifDataAuto.getAttribute(ExifInterface.TAG_WHITE_BALANCE)?.toShort())
            .isEqualTo(ExifInterface.WHITE_BALANCE_AUTO)
        assertThat(exifDataManual.getAttribute(ExifInterface.TAG_WHITE_BALANCE)?.toShort())
            .isEqualTo(ExifInterface.WHITE_BALANCE_MANUAL)
    }

    @Test
    public fun makeAndModelSetByDefaultBuilder() {
        val exifDataDefault = ExifData.builderForDevice().build()

        assertThat(exifDataDefault.getAttribute(ExifInterface.TAG_MAKE))
            .isEqualTo(Build.MANUFACTURER)
        assertThat(exifDataDefault.getAttribute(ExifInterface.TAG_MODEL))
            .isEqualTo(Build.MODEL)
    }
}