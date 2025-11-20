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
import android.graphics.Matrix
import androidx.camera.core.imagecapture.Utils.CAMERA_CAPTURE_RESULT
import androidx.camera.core.imagecapture.Utils.CROP_RECT
import androidx.camera.core.imagecapture.Utils.HEIGHT
import androidx.camera.core.imagecapture.Utils.ROTATION_DEGREES
import androidx.camera.core.imagecapture.Utils.SIZE
import androidx.camera.core.imagecapture.Utils.WIDTH
import androidx.camera.core.internal.utils.ImageUtil.jpegImageToJpegByteArray
import androidx.camera.core.processing.Packet
import androidx.camera.testing.impl.ExifUtil.createExif
import androidx.camera.testing.impl.ExifUtil.updateExif
import androidx.camera.testing.impl.TestImageUtil.createJpegBytes
import androidx.camera.testing.impl.TestImageUtil.getAverageDiff
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/** Instrumented tests for [JpegBytes2Image]. */
@SmallTest
@RunWith(AndroidJUnit4::class)
class JpegBytes2ImageDeviceTest {

    private val operation = JpegBytes2Image()

    @Test
    fun processInput_assertOutput() {
        val jpegBytes =
            updateExif(createJpegBytes(WIDTH, HEIGHT)) { it.description = "description" }
        val exif = createExif(jpegBytes)
        val matrix = Matrix()
        val input =
            Packet.of(
                jpegBytes,
                exif,
                JPEG,
                SIZE,
                CROP_RECT,
                ROTATION_DEGREES,
                matrix,
                CAMERA_CAPTURE_RESULT,
            )

        // Act.
        val output = operation.apply(input)

        // Assert.
        val restoredJpeg = jpegImageToJpegByteArray(output.data)
        assertThat(getAverageDiff(jpegBytes, restoredJpeg)).isEqualTo(0)
        assertThat(output.exif).isEqualTo(exif)
        assertThat(output.format).isEqualTo(JPEG)
        assertThat(output.size).isEqualTo(SIZE)
        assertThat(output.cropRect).isEqualTo(CROP_RECT)
        assertThat(output.rotationDegrees).isEqualTo(ROTATION_DEGREES)
        assertThat(output.sensorToBufferTransform).isEqualTo(matrix)
        assertThat(output.cameraCaptureResult).isEqualTo(CAMERA_CAPTURE_RESULT)
    }
}
