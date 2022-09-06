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

import android.graphics.ImageFormat
import android.os.Build
import android.util.Size
import androidx.camera.core.imagecapture.Utils.CROP_RECT
import androidx.camera.core.imagecapture.Utils.EXIF_DESCRIPTION
import androidx.camera.core.imagecapture.Utils.HEIGHT
import androidx.camera.core.imagecapture.Utils.OUTPUT_FILE_OPTIONS
import androidx.camera.core.imagecapture.Utils.ROTATION_DEGREES
import androidx.camera.core.imagecapture.Utils.SENSOR_TO_BUFFER
import androidx.camera.core.imagecapture.Utils.WIDTH
import androidx.camera.core.internal.utils.ImageUtil.jpegImageToJpegByteArray
import androidx.camera.testing.ExifUtil.updateExif
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
 * Unit tests for [ProcessingInput2Packet]
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class ProcessingInput2PacketTest {

    private val processor = ProcessingInput2Packet()

    @Test
    fun processInput_assertOutput() {
        // Arrange: create input
        val jpegBytes = updateExif(createJpegBytes(640, 480)) {
            it.description = EXIF_DESCRIPTION
        }
        val image = createJpegFakeImageProxy(jpegBytes)
        val processingRequest = ProcessingRequest(
            { listOf() },
            OUTPUT_FILE_OPTIONS,
            CROP_RECT,
            ROTATION_DEGREES,
            /*jpegQuality=*/100,
            SENSOR_TO_BUFFER,
            FakeTakePictureCallback()
        )
        val input = ProcessingNode.InputPacket.of(processingRequest, image)

        // Act.
        val output = processor.process(input)

        // Assert: buffer is rewound after reading Exif data.
        val buffer = output.data.planes[0].buffer
        assertThat(buffer.position()).isEqualTo(0)
        // Assert: image is the same.
        val restoredJpeg = jpegImageToJpegByteArray(output.data)
        assertThat(getAverageDiff(jpegBytes, restoredJpeg)).isEqualTo(0)
        // Assert: the Exif is extracted correctly.
        assertThat(output.exif!!.description).isEqualTo(EXIF_DESCRIPTION)
        // Assert: the metadata are correct.
        assertThat(output.cropRect).isEqualTo(CROP_RECT)
        assertThat(output.rotationDegrees).isEqualTo(ROTATION_DEGREES)
        assertThat(output.format).isEqualTo(ImageFormat.JPEG)
        assertThat(output.size).isEqualTo(Size(WIDTH, HEIGHT))
        assertThat(output.sensorToBufferTransform).isEqualTo(SENSOR_TO_BUFFER)
    }
}