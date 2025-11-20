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

import android.graphics.Rect
import androidx.camera.core.imagecapture.Utils.HEIGHT
import androidx.camera.core.imagecapture.Utils.TIMESTAMP
import androidx.camera.core.imagecapture.Utils.WIDTH
import androidx.camera.core.processing.InternalImageProcessor
import androidx.camera.core.processing.Packet
import androidx.camera.testing.impl.ExifUtil
import androidx.camera.testing.impl.TestImageUtil
import androidx.camera.testing.impl.TestImageUtil.getAverageDiff
import androidx.camera.testing.impl.fakes.GrayscaleImageEffect
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

/** Instrument test for [BitmapEffect]. */
@SmallTest
@RunWith(AndroidJUnit4::class)
class BitmapEffectDeviceTest {

    private val imageEffect = GrayscaleImageEffect()
    private val operation = BitmapEffect(InternalImageProcessor(imageEffect))

    @Test
    fun applyEffect_imageIsPropagatedToAndFromProcessor() {
        // Arrange.
        val bitmap = TestImageUtil.createBitmap(WIDTH, HEIGHT)
        val input =
            Packet.of(
                bitmap,
                ExifUtil.createExif(TestImageUtil.createJpegBytes(WIDTH, HEIGHT)),
                Utils.CROP_RECT,
                Utils.ROTATION_DEGREES,
                Utils.SENSOR_TO_BUFFER,
                Utils.CAMERA_CAPTURE_RESULT,
            )

        val output = operation.apply(input)

        // Assert: processor receives correct metadata
        val inputImage = imageEffect.inputImage!!
        assertThat(inputImage.cropRect).isEqualTo(Utils.CROP_RECT)
        assertThat(inputImage.imageInfo.timestamp).isEqualTo(TIMESTAMP)
        assertThat(inputImage.imageInfo.rotationDegrees).isEqualTo(Utils.ROTATION_DEGREES)
        assertThat(inputImage.imageInfo.sensorToBufferTransformMatrix)
            .isEqualTo(Utils.SENSOR_TO_BUFFER)
        // Assert: processor receives the test image
        val outputBitmap = (inputImage as RgbaImageProxy).createBitmap()
        assertThat(TestImageUtil.getAverageDiff(outputBitmap, bitmap)).isEqualTo(0)
        // Assert: the output is a grayscale version of the test image.
        assertThat(getAverageDiff(output.data, Rect(0, 0, 320, 240), 0X555555)).isEqualTo(0)
        assertThat(getAverageDiff(output.data, Rect(321, 0, WIDTH, 240), 0X555555)).isEqualTo(0)
        assertThat(getAverageDiff(output.data, Rect(0, 240, 320, 480), 0X555555)).isEqualTo(0)
        assertThat(getAverageDiff(output.data, Rect(321, 240, WIDTH, 480), 0XAAAAAA)).isEqualTo(0)
    }
}
