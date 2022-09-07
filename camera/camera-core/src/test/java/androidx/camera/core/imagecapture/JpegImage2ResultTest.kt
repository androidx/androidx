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
import androidx.camera.core.ImageProxy
import androidx.camera.core.imagecapture.Utils.CROP_RECT
import androidx.camera.core.imagecapture.Utils.HEIGHT
import androidx.camera.core.imagecapture.Utils.ROTATION_DEGREES
import androidx.camera.core.imagecapture.Utils.SENSOR_TO_BUFFER
import androidx.camera.core.imagecapture.Utils.WIDTH
import androidx.camera.core.internal.utils.ImageUtil.jpegImageToJpegByteArray
import androidx.camera.core.processing.Packet
import androidx.camera.testing.ExifUtil.createExif
import androidx.camera.testing.TestImageUtil.createJpegBytes
import androidx.camera.testing.TestImageUtil.createJpegFakeImageProxy
import androidx.camera.testing.TestImageUtil.getAverageDiff
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [JpegImage2Result].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class JpegImage2ResultTest {

    private val processor = JpegImage2Result()

    @Test
    fun process_verifyOutput() {
        // Arrange.
        val jpegBytes = createJpegBytes(WIDTH, HEIGHT)
        val exif = createExif(jpegBytes)
        val image = createJpegFakeImageProxy(jpegBytes) as ImageProxy
        val input = Packet.of(
            image,
            exif,
            JPEG,
            Size(WIDTH, HEIGHT),
            CROP_RECT,
            ROTATION_DEGREES,
            SENSOR_TO_BUFFER
        )
        // Act.
        val output = processor.process(input)

        // Assert: image is the same.
        val restoredJpeg = jpegImageToJpegByteArray(output)
        assertThat(getAverageDiff(jpegBytes, restoredJpeg)).isEqualTo(0)
        // Assert: metadata is updated based on the Packet.
        assertThat(output.format).isEqualTo(JPEG)
        assertThat(output.width).isEqualTo(WIDTH)
        assertThat(output.height).isEqualTo(HEIGHT)
        assertThat(output.cropRect).isEqualTo(CROP_RECT)
        assertThat(output.imageInfo.rotationDegrees).isEqualTo(ROTATION_DEGREES)
        assertThat(output.imageInfo.sensorToBufferTransformMatrix).isEqualTo(SENSOR_TO_BUFFER)
    }
}