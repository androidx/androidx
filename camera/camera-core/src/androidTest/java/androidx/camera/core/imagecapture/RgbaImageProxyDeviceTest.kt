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
import androidx.camera.core.imagecapture.Utils.CAMERA_CAPTURE_RESULT
import androidx.camera.core.imagecapture.Utils.CROP_RECT
import androidx.camera.core.imagecapture.Utils.HEIGHT
import androidx.camera.core.imagecapture.Utils.ROTATION_DEGREES
import androidx.camera.core.imagecapture.Utils.SENSOR_TO_BUFFER
import androidx.camera.core.imagecapture.Utils.TIMESTAMP
import androidx.camera.core.imagecapture.Utils.WIDTH
import androidx.camera.core.processing.Packet
import androidx.camera.testing.ExifUtil
import androidx.camera.testing.TestImageUtil
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth
import java.nio.ByteBuffer
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [RgbaImageProxy].
 */

@SmallTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 21)
class RgbaImageProxyDeviceTest {

    @Test
    fun createImageFromBitmap_verifyResult() {
        // Arrange.
        val bitmap = TestImageUtil.createBitmap(WIDTH, HEIGHT)
        // Act.
        val image = RgbaImageProxy(
            Packet.of(
                bitmap,
                ExifUtil.createExif(TestImageUtil.createJpegBytes(WIDTH, HEIGHT)),
                CROP_RECT,
                ROTATION_DEGREES,
                SENSOR_TO_BUFFER,
                CAMERA_CAPTURE_RESULT
            )
        )
        // Assert.
        val restoredBitmap = Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888)
        restoredBitmap.copyPixelsFromBuffer(image.planes[0].buffer)

        Truth.assertThat(TestImageUtil.getAverageDiff(bitmap, restoredBitmap)).isEqualTo(0)
        Truth.assertThat(image.image).isNull()
        Truth.assertThat(image.width).isEqualTo(WIDTH)
        Truth.assertThat(image.height).isEqualTo(HEIGHT)
        Truth.assertThat(image.cropRect).isEqualTo(CROP_RECT)
        Truth.assertThat(image.imageInfo.rotationDegrees).isEqualTo(ROTATION_DEGREES)
        Truth.assertThat(image.imageInfo.sensorToBufferTransformMatrix).isEqualTo(SENSOR_TO_BUFFER)
        Truth.assertThat(image.imageInfo.timestamp).isEqualTo(TIMESTAMP)
    }

    @Test
    fun createImageFromByteBuffer_verifyResult() {
        // Arrange.
        val bitmap = TestImageUtil.createBitmap(WIDTH, HEIGHT)
        val byteBuffer = ByteBuffer.allocateDirect(bitmap.allocationByteCount)
        bitmap.copyPixelsToBuffer(byteBuffer)
        // Act.
        val image = RgbaImageProxy(
            byteBuffer,
            4,
            bitmap.width,
            bitmap.height,
            CROP_RECT,
            ROTATION_DEGREES,
            SENSOR_TO_BUFFER,
            TIMESTAMP
        )
        // Assert.
        val restoredBitmap = image.createBitmap()
        Truth.assertThat(TestImageUtil.getAverageDiff(bitmap, restoredBitmap)).isEqualTo(0)
        Truth.assertThat(image.width).isEqualTo(WIDTH)
        Truth.assertThat(image.height).isEqualTo(HEIGHT)
        Truth.assertThat(image.cropRect).isEqualTo(CROP_RECT)
        Truth.assertThat(image.imageInfo.rotationDegrees).isEqualTo(ROTATION_DEGREES)
        Truth.assertThat(image.imageInfo.sensorToBufferTransformMatrix).isEqualTo(SENSOR_TO_BUFFER)
        Truth.assertThat(image.imageInfo.timestamp).isEqualTo(TIMESTAMP)
    }
}