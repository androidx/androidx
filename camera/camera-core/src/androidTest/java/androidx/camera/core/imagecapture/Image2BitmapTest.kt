/*
 * Copyright 2023 The Android Open Source Project
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

import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import androidx.camera.core.internal.utils.ImageUtil
import androidx.camera.core.processing.Packet
import androidx.camera.testing.impl.ExifUtil
import androidx.camera.testing.impl.TestImageUtil
import androidx.camera.testing.impl.fakes.FakeImageInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class Image2BitmapTest {
    private val operation = Image2Bitmap()

    @Test
    fun processYuvImage_rotation0_assertOutput() {
        processYuvImage_assertOutput(rotationDegrees = 0)
    }

    @Test
    fun processYuvImage_rotation90_assertOutput() {
        processYuvImage_assertOutput(rotationDegrees = 90)
    }

    @Test
    fun processYuvImage_rotation180_assertOutput() {
        processYuvImage_assertOutput(rotationDegrees = 180)
    }

    @Test
    fun processYuvImage_rotation270_assertOutput() {
        processYuvImage_assertOutput(rotationDegrees = 270)
    }

    private fun processYuvImage_assertOutput(rotationDegrees: Int) {
        // Arrange.
        val imageInfo = FakeImageInfo()
        val yuvImage = TestImageUtil.createYuvFakeImageProxy(imageInfo, Utils.WIDTH, Utils.HEIGHT)
        val input =
            Packet.of(
                yuvImage,
                null, // YuvImage doesn't have exif info.
                Rect(0, 0, Utils.WIDTH, Utils.HEIGHT),
                rotationDegrees,
                Matrix(),
                Utils.CAMERA_CAPTURE_RESULT,
            )
        val inputDecodedBitmap = ImageUtil.createBitmapFromImageProxy(yuvImage)

        // Act.
        val outputBitmap = operation.apply(input)

        // Assert: the image is the same.
        val inputRotatedBitmap = TestImageUtil.rotateBitmap(inputDecodedBitmap, rotationDegrees)
        Truth.assertThat(TestImageUtil.getAverageDiff(inputRotatedBitmap, outputBitmap))
            .isEqualTo(0)

        // Assert: image is closed.
        Truth.assertThat(yuvImage.isClosed).isTrue()
    }

    @Test
    fun processJpegImage_rotation0_assertOutput() {
        processJpegImage_assertOutput(rotationDegrees = 0)
    }

    @Test
    fun processJpegImage_rotation90_assertOutput() {
        processJpegImage_assertOutput(rotationDegrees = 90)
    }

    @Test
    fun processJpegImage_rotation180_assertOutput() {
        processJpegImage_assertOutput(rotationDegrees = 180)
    }

    @Test
    fun processJpegImage_rotation270_assertOutput() {
        processJpegImage_assertOutput(rotationDegrees = 270)
    }

    private fun processJpegImage_assertOutput(rotationDegrees: Int) {
        // Arrange.
        val imageInfo = FakeImageInfo()
        val jpegBytes = TestImageUtil.createJpegBytes(Utils.WIDTH, Utils.HEIGHT)
        val jpegImage = TestImageUtil.createJpegFakeImageProxy(imageInfo, jpegBytes)
        val exif = ExifUtil.createExif(jpegBytes)
        val input =
            Packet.of(
                jpegImage,
                exif,
                Rect(0, 0, Utils.WIDTH, Utils.HEIGHT),
                rotationDegrees,
                Matrix(),
                Utils.CAMERA_CAPTURE_RESULT,
            )
        val inputDecodedBitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)

        // Act.
        val outputBitmap = operation.apply(input)

        // Assert: the image is the same.
        val inputRotatedBitmap = TestImageUtil.rotateBitmap(inputDecodedBitmap, rotationDegrees)
        Truth.assertThat(TestImageUtil.getAverageDiff(inputRotatedBitmap, outputBitmap))
            .isEqualTo(0)

        // Assert: image is closed.
        Truth.assertThat(jpegImage.isClosed).isTrue()
    }
}
