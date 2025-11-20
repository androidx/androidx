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

import android.graphics.BitmapFactory.decodeByteArray
import android.graphics.Matrix
import android.media.ExifInterface.TAG_FOCAL_LENGTH
import androidx.camera.core.imagecapture.Utils.CAMERA_CAPTURE_RESULT
import androidx.camera.core.imagecapture.Utils.CROP_RECT
import androidx.camera.core.imagecapture.Utils.FOCAL_LENGTH
import androidx.camera.core.imagecapture.Utils.HEIGHT
import androidx.camera.core.imagecapture.Utils.ROTATION_DEGREES
import androidx.camera.core.imagecapture.Utils.WIDTH
import androidx.camera.core.impl.utils.Exif.createFromInputStream
import androidx.camera.core.internal.compat.quirk.DeviceQuirks
import androidx.camera.core.processing.Packet
import androidx.camera.testing.impl.TestImageUtil.createYuvFakeImageProxy
import androidx.camera.testing.impl.fakes.FakeImageInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import org.junit.Test
import org.junit.runner.RunWith

/** Instrument test for [Image2JpegBytes]. */
@SmallTest
@RunWith(AndroidJUnit4::class)
class Image2JpegBytesDeviceTest {

    private val operation = Image2JpegBytes(DeviceQuirks.getAll())

    @Test
    fun processYuvImage_assertOutput() {
        // Arrange.
        val imageInfo =
            FakeImageInfo().also {
                it.rotationDegrees = 180
                it.setFocalLength(FOCAL_LENGTH)
            }
        val yuvImage = createYuvFakeImageProxy(imageInfo, WIDTH, HEIGHT)
        val input =
            Packet.of(
                yuvImage,
                null, // YuvImage doesn't have exif info.
                CROP_RECT,
                ROTATION_DEGREES,
                Matrix().also { it.setScale(-1F, 1F, 320F, 240F) },
                CAMERA_CAPTURE_RESULT,
            )

        // Act.
        val output = operation.apply(Image2JpegBytes.In.of(input, 100))

        // Assert: the image is the same.
        // TODO(b/245940015): verify the content of the restored image.
        val bitmap = decodeByteArray(output.data, 0, output.data.size)
        assertThat(bitmap.width).isEqualTo(WIDTH)
        assertThat(bitmap.height).isEqualTo(HEIGHT / 2)
        // Assert: image is closed.
        assertThat(yuvImage.isClosed).isTrue()
        // Assert: packet rotation is kept
        assertThat(output.rotationDegrees).isEqualTo(ROTATION_DEGREES)
        // Assert: exif rotation is overwritten by packet rotation.
        val exif = createFromInputStream(ByteArrayInputStream(output.data))
        assertThat(exif.rotation).isEqualTo(ROTATION_DEGREES)
        // Assert: focal length from ImageInfo is saved to Exif.
        assertThat(exif.exifInterface.getAttributeDouble(TAG_FOCAL_LENGTH, 0.0))
            .isWithin(1E-4)
            .of(FOCAL_LENGTH.toDouble())
        // Assert: transformation is updated.
        val points = floatArrayOf(WIDTH.toFloat(), HEIGHT.toFloat())
        output.sensorToBufferTransform.mapPoints(points)
        assertThat(points).usingTolerance(1E-4).containsExactly(0, 240)
        // Assert: capture result
        assertThat(output.cameraCaptureResult).isEqualTo(CAMERA_CAPTURE_RESULT)
    }
}
