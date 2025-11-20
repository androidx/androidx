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

import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.util.Size
import androidx.camera.core.ImageCapture
import androidx.camera.core.imagecapture.Utils.CAMERA_CAPTURE_RESULT
import androidx.camera.core.imagecapture.Utils.HEIGHT
import androidx.camera.core.imagecapture.Utils.ROTATION_DEGREES
import androidx.camera.core.imagecapture.Utils.TEMP_FILE
import androidx.camera.core.imagecapture.Utils.WIDTH
import androidx.camera.core.impl.utils.Exif
import androidx.camera.core.processing.Packet
import androidx.camera.testing.impl.ExifUtil
import androidx.camera.testing.impl.TestImageUtil.createBitmap
import androidx.camera.testing.impl.TestImageUtil.createJpegBytes
import androidx.camera.testing.impl.TestImageUtil.getAverageDiff
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.io.FileOutputStream
import org.junit.Test
import org.junit.runner.RunWith

/** Instrumented tests for [JpegBytes2Disk]. */
@SmallTest
@RunWith(AndroidJUnit4::class)
class JpegBytes2DiskDeviceTest {

    private val operation = JpegBytes2Disk()

    @Test
    fun saveToOutputStream_verifySavedImageIsIdentical() {
        // Arrange.
        val jpegBytes = createJpegBytes(WIDTH, HEIGHT)
        val inputPacket =
            Packet.of(
                jpegBytes,
                ExifUtil.createExif(jpegBytes),
                ImageFormat.JPEG,
                Size(WIDTH, HEIGHT),
                Rect(0, 0, WIDTH, HEIGHT),
                ROTATION_DEGREES,
                Matrix(),
                CAMERA_CAPTURE_RESULT,
            )
        // Act: save to a OutputStream.
        FileOutputStream(TEMP_FILE).use {
            val input =
                JpegBytes2Disk.In.of(
                    inputPacket,
                    ImageCapture.OutputFileOptions.Builder(it).build(),
                )
            operation.apply(input)
        }
        // Assert.
        val restoredBitmap = BitmapFactory.decodeFile(TEMP_FILE.path)
        assertThat(getAverageDiff(restoredBitmap, createBitmap(WIDTH, HEIGHT))).isEqualTo(0)
    }

    @Test
    fun saveToFile_verifySavedImageIsIdentical() {
        // Act.
        val path = saveFileAndGetPath()
        // Assert: image is identical.
        val restoredBitmap = BitmapFactory.decodeFile(path)
        assertThat(getAverageDiff(restoredBitmap, createBitmap(WIDTH, HEIGHT))).isEqualTo(0)
        // Assert: exif rotation matches the packet rotation.
        val restoredExif = Exif.createFromFileString(path)
        assertThat(restoredExif.rotation).isEqualTo(ROTATION_DEGREES)
    }

    private fun saveFileAndGetPath(metadata: ImageCapture.Metadata): String {
        return saveFileAndGetPath(ExifUtil.createExif(createJpegBytes(WIDTH, HEIGHT)), metadata, 0)
    }

    private fun saveFileAndGetPath(
        exif: Exif = ExifUtil.createExif(createJpegBytes(WIDTH, HEIGHT)),
        metadata: ImageCapture.Metadata = ImageCapture.Metadata(),
        rotation: Int = ROTATION_DEGREES,
    ): String {
        val jpegBytes = createJpegBytes(WIDTH, HEIGHT)
        val inputPacket =
            Packet.of(
                jpegBytes,
                exif,
                ImageFormat.JPEG,
                Size(WIDTH, HEIGHT),
                Rect(0, 0, WIDTH, HEIGHT),
                rotation,
                Matrix(),
                CAMERA_CAPTURE_RESULT,
            )
        val options =
            ImageCapture.OutputFileOptions.Builder(TEMP_FILE).setMetadata(metadata).build()
        val input = JpegBytes2Disk.In.of(inputPacket, options)
        return operation.apply(input).savedUri!!.path!!
    }
}
