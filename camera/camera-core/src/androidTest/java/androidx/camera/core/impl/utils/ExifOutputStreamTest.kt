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

import android.graphics.Bitmap
import android.os.Build
import androidx.camera.core.impl.CameraCaptureMetaData
import androidx.exifinterface.media.ExifInterface
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

@LargeTest
public class ExifOutputStreamTest {

    @Test
    public fun canSetExifOnCompressedBitmap() {
        // Arrange.
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val exifData =
            ExifData.builderForDevice()
                .setImageWidth(bitmap.width)
                .setImageHeight(bitmap.height)
                .setFlashState(CameraCaptureMetaData.FlashState.NONE)
                .setExposureTimeNanos(0)
                .build()

        val fileWithExif = File.createTempFile("testWithExif", ".jpg")
        val outputStreamWithExif = ExifOutputStream(fileWithExif.outputStream(), exifData)
        fileWithExif.deleteOnExit()
        val fileWithoutExif = File.createTempFile("testWithoutExif", ".jpg")
        val outputStreamWithoutExif = fileWithoutExif.outputStream()
        fileWithoutExif.deleteOnExit()

        // Act.
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStreamWithExif)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStreamWithoutExif)

        // Verify with ExifInterface
        val withExif = ExifInterface(fileWithExif.inputStream())
        val withoutExif = ExifInterface(fileWithoutExif.inputStream())

        // Assert.
        // Model comes from default builder
        assertThat(withExif.getAttribute(ExifInterface.TAG_MODEL)).isEqualTo(Build.MODEL)
        assertThat(withoutExif.getAttribute(ExifInterface.TAG_MODEL)).isNull()

        assertThat(withExif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)).isEqualTo("100")
        assertThat(withoutExif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)).isEqualTo("100")

        assertThat(withExif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH)).isEqualTo("100")
        assertThat(withoutExif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH)).isEqualTo("100")

        assertThat(withExif.getAttribute(ExifInterface.TAG_FLASH)?.toShort())
            .isEqualTo(ExifInterface.FLAG_FLASH_NO_FLASH_FUNCTION)
        assertThat(withoutExif.getAttribute(ExifInterface.TAG_FLASH)).isNull()

        assertThat(withExif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.toFloat()?.toInt())
            .isEqualTo(0)
        assertThat(withoutExif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)).isNull()
    }
}
