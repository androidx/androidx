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

package androidx.camera.core.imagecapture

import android.graphics.ImageFormat.JPEG
import android.os.Build
import android.util.Size
import androidx.camera.core.imagecapture.Utils.CAMERA_CAPTURE_RESULT
import androidx.camera.core.imagecapture.Utils.CROP_RECT
import androidx.camera.core.imagecapture.Utils.HEIGHT
import androidx.camera.core.imagecapture.Utils.ROTATION_DEGREES
import androidx.camera.core.imagecapture.Utils.SENSOR_TO_BUFFER
import androidx.camera.core.imagecapture.Utils.WIDTH
import androidx.camera.core.processing.Packet
import androidx.camera.testing.impl.ExifUtil.createExif
import androidx.camera.testing.impl.TestImageUtil.createJpegBytes
import androidx.camera.testing.impl.TestImageUtil.createJpegFakeImageProxy
import androidx.camera.testing.impl.TestImageUtil.getAverageDiff
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [Image2JpegBytes]
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class Image2JpegBytesTest {

    private val operation = Image2JpegBytes()

    @Test
    fun processJpegImage_assertOutput() {
        // Arrange: create input
        val jpegBytes = createJpegBytes(WIDTH, HEIGHT)
        val exif = createExif(jpegBytes)
        val image = createJpegFakeImageProxy(jpegBytes)
        val input = Packet.of(
            image,
            exif,
            CROP_RECT,
            ROTATION_DEGREES,
            SENSOR_TO_BUFFER,
            CAMERA_CAPTURE_RESULT
        )

        // Act.
        val output = operation.apply(Image2JpegBytes.In.of(input, 100))

        // Assert: the image is the same.
        assertThat(getAverageDiff(jpegBytes, output.data)).isEqualTo(0)
        // Assert: the Exif is extracted correctly.
        assertThat(output.exif).isEqualTo(exif)
        // Assert: the image is closed.
        assertThat(image.isClosed).isTrue()
        // Assert: metadata is correct.
        assertThat(output.cropRect).isEqualTo(CROP_RECT)
        assertThat(output.rotationDegrees).isEqualTo(ROTATION_DEGREES)
        assertThat(output.format).isEqualTo(JPEG)
        assertThat(output.size).isEqualTo(Size(WIDTH, HEIGHT))
        assertThat(output.sensorToBufferTransform).isEqualTo(SENSOR_TO_BUFFER)
        assertThat(output.cameraCaptureResult).isEqualTo(CAMERA_CAPTURE_RESULT)
    }
}
