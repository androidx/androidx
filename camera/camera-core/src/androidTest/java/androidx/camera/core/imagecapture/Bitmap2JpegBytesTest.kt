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

import android.graphics.Bitmap
import android.graphics.BitmapFactory.decodeByteArray
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.os.Build
import androidx.camera.core.imagecapture.Utils.CAMERA_CAPTURE_RESULT
import androidx.camera.core.imagecapture.Utils.HEIGHT
import androidx.camera.core.imagecapture.Utils.WIDTH
import androidx.camera.core.processing.Packet
import androidx.camera.testing.impl.ExifUtil.createExif
import androidx.camera.testing.impl.TestImageUtil.createBitmap
import androidx.camera.testing.impl.TestImageUtil.createGainmap
import androidx.camera.testing.impl.TestImageUtil.createJpegBytes
import androidx.camera.testing.impl.TestImageUtil.createJpegrBytes
import androidx.camera.testing.impl.TestImageUtil.getAverageDiff
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/** Instrumented tests for [JpegBytes2CroppedBitmap]. */
@SmallTest
@RunWith(AndroidJUnit4::class)
class Bitmap2JpegBytesTest {

    private val operation = Bitmap2JpegBytes()

    @Test
    fun process_verifyOutput() {
        // Arrange.
        val bitmap = createBitmap(WIDTH, HEIGHT)
        val inputPacket =
            Packet.of(
                bitmap,
                createExif(createJpegBytes(WIDTH, HEIGHT)),
                Rect(0, 0, WIDTH, HEIGHT),
                90,
                Matrix(),
                CAMERA_CAPTURE_RESULT,
            )
        val input = Bitmap2JpegBytes.In.of(inputPacket, 100)

        // Act.
        val output = operation.apply(input)

        // Assert
        assertThat(output.format).isEqualTo(ImageFormat.JPEG)
        verifyOutputData(output, bitmap)
    }

    @SdkSuppress(minSdkVersion = 34)
    @Test
    fun process_withJpegR_verifyOutput() {
        // Arrange.
        val bitmap =
            createBitmap(WIDTH, HEIGHT).apply { this.gainmap = createGainmap(WIDTH, HEIGHT) }
        assertThat(bitmap.hasGainmap()).isTrue()
        val inputPacket =
            Packet.of(
                bitmap,
                createExif(createJpegrBytes(WIDTH, HEIGHT)),
                Rect(0, 0, WIDTH, HEIGHT),
                90,
                Matrix(),
                CAMERA_CAPTURE_RESULT,
            )
        val input = Bitmap2JpegBytes.In.of(inputPacket, 100)

        // Act.
        val output = operation.apply(input)

        // Assert
        assertThat(output.format).isEqualTo(ImageFormat.JPEG_R)
        verifyOutputData(output, bitmap)
    }

    private fun verifyOutputData(output: Packet<ByteArray>, expectedBitmap: Bitmap) {
        assertThat(output.cameraCaptureResult).isEqualTo(CAMERA_CAPTURE_RESULT)

        // Verify bitmap content.
        val restoredBitmap = decodeByteArray(output.data, 0, output.data.size)
        assertThat(getAverageDiff(expectedBitmap, restoredBitmap)).isEqualTo(0)

        // Verify gainmap content.
        if (Build.VERSION.SDK_INT >= 34 && expectedBitmap.hasGainmap()) {
            assertThat(restoredBitmap.hasGainmap()).isEqualTo(true)
            val sourceGainmap = expectedBitmap.gainmap!!.gainmapContents
            val restoredGainmap = restoredBitmap.gainmap!!.gainmapContents
            assertThat(getAverageDiff(restoredGainmap, sourceGainmap)).isEqualTo(0)
        }
    }
}
