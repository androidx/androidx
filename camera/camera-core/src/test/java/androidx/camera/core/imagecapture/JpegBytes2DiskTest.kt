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
import android.location.Location
import android.os.Build
import android.util.Size
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OutputFileOptions
import androidx.camera.core.imagecapture.Utils.ALTITUDE
import androidx.camera.core.imagecapture.Utils.EXIF_DESCRIPTION
import androidx.camera.core.imagecapture.Utils.HEIGHT
import androidx.camera.core.imagecapture.Utils.ROTATION_DEGREES
import androidx.camera.core.imagecapture.Utils.TEMP_FILE
import androidx.camera.core.imagecapture.Utils.WIDTH
import androidx.camera.core.impl.utils.Exif
import androidx.camera.core.impl.utils.Exif.createFromFileString
import androidx.camera.core.processing.Packet
import androidx.camera.testing.ExifUtil.createExif
import androidx.camera.testing.TestImageUtil.createBitmap
import androidx.camera.testing.TestImageUtil.createJpegBytes
import androidx.camera.testing.TestImageUtil.getAverageDiff
import com.google.common.truth.Truth.assertThat
import java.io.FileOutputStream
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [JpegBytes2Disk]
 *
 * TODO: test when OutputFileOptions targets MediaStore.
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class JpegBytes2DiskTest {

    private val processor = JpegBytes2Disk()

    @Test
    fun saveToOutputStream_verifySavedImageIsIdentical() {
        // Arrange.
        val jpegBytes = createJpegBytes(WIDTH, HEIGHT)
        val inputPacket = Packet.of(
            jpegBytes,
            createExif(jpegBytes),
            ImageFormat.JPEG,
            Size(WIDTH, HEIGHT),
            Rect(0, 0, WIDTH, HEIGHT),
            ROTATION_DEGREES,
            Matrix()
        )
        // Act: save to a OutputStream.
        FileOutputStream(TEMP_FILE).use {
            val input = JpegBytes2Disk.In.of(inputPacket, OutputFileOptions.Builder(it).build())
            processor.process(input)
        }
        // Assert.
        val restoredBitmap = BitmapFactory.decodeFile(TEMP_FILE.path)
        assertThat(getAverageDiff(restoredBitmap, createBitmap(WIDTH, HEIGHT))).isEqualTo(0)
    }

    @Test
    fun saveToFile_verifySavedImageIsIdentical() {
        // Act.
        val path = saveFileAndGetPath()
        // Assert.
        val restoredBitmap = BitmapFactory.decodeFile(path)
        assertThat(getAverageDiff(restoredBitmap, createBitmap(WIDTH, HEIGHT))).isEqualTo(0)
        assertThat(createFromFileString(path).rotation).isEqualTo(ROTATION_DEGREES)
    }

    @Test
    fun saveWithExif_verifyExifIsCopied() {
        // Arrange.
        val exif = createExif(createJpegBytes(WIDTH, HEIGHT))
        exif.description = EXIF_DESCRIPTION
        // Act.
        val path = saveFileAndGetPath(exif)
        // Assert.
        val restoredExif = createFromFileString(path)
        assertThat(restoredExif.description).isEqualTo(EXIF_DESCRIPTION)
        assertThat(restoredExif.rotation).isEqualTo(ROTATION_DEGREES)
    }

    @Test
    fun saveWithHorizontalFlip_verifyExif() {
        // Arrange.
        val metadata = ImageCapture.Metadata().apply { this.isReversedHorizontal = true }
        // Act.
        val path = saveFileAndGetPath(metadata)
        // Assert.
        assertThat(createFromFileString(path).isFlippedHorizontally).isTrue()
    }

    @Test
    fun saveWithVerticalFlip_verifyExif() {
        // Arrange.
        val metadata = ImageCapture.Metadata().apply { this.isReversedVertical = true }
        // Act.
        val path = saveFileAndGetPath(metadata)
        // Assert.
        assertThat(createFromFileString(path).isFlippedVertically).isTrue()
    }

    @Test
    fun saveWithLocation_verifyExif() {
        // Arrange.
        val location = Location(null).apply { this.altitude = ALTITUDE }
        val metadata = ImageCapture.Metadata().apply { this.location = location }
        // Act.
        val path = saveFileAndGetPath(metadata)
        // Assert.
        assertThat(createFromFileString(path).location!!.altitude).isEqualTo(ALTITUDE)
    }

    private fun saveFileAndGetPath(metadata: ImageCapture.Metadata): String {
        return saveFileAndGetPath(createExif(createJpegBytes(WIDTH, HEIGHT)), metadata, 0)
    }

    private fun saveFileAndGetPath(
        exif: Exif = createExif(createJpegBytes(WIDTH, HEIGHT)),
        metadata: ImageCapture.Metadata = ImageCapture.Metadata(),
        rotation: Int = ROTATION_DEGREES
    ): String {
        val jpegBytes = createJpegBytes(WIDTH, HEIGHT)
        val inputPacket = Packet.of(
            jpegBytes,
            exif,
            ImageFormat.JPEG,
            Size(WIDTH, HEIGHT),
            Rect(0, 0, WIDTH, HEIGHT),
            rotation,
            Matrix()
        )
        val options = OutputFileOptions.Builder(TEMP_FILE).setMetadata(metadata).build()
        val input = JpegBytes2Disk.In.of(inputPacket, options)
        return processor.process(input).savedUri!!.path!!
    }
}