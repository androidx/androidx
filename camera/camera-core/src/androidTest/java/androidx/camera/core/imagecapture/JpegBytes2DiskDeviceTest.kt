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
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageCapture
import androidx.camera.core.imagecapture.Utils.CAMERA_CAPTURE_RESULT
import androidx.camera.core.imagecapture.Utils.EXIF_DESCRIPTION
import androidx.camera.core.imagecapture.Utils.HEIGHT
import androidx.camera.core.imagecapture.Utils.ROTATION_DEGREES
import androidx.camera.core.imagecapture.Utils.TEMP_FILE
import androidx.camera.core.imagecapture.Utils.WIDTH
import androidx.camera.core.impl.utils.Exif
import androidx.camera.core.processing.Packet
import androidx.camera.testing.impl.ExifUtil
import androidx.camera.testing.impl.TestImageUtil.createBitmap
import androidx.camera.testing.impl.TestImageUtil.createJpegBytes
import androidx.camera.testing.impl.TestImageUtil.createJpegrBytes
import androidx.camera.testing.impl.TestImageUtil.getAverageDiff
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
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

    @SdkSuppress(minSdkVersion = 34)
    @Test
    fun saveJpegrToFileAndOutputStream_preservesGainmap() {
        val jpegrBytes = createJpegrBytes(WIDTH, HEIGHT)
        val exif = ExifUtil.createExif(jpegrBytes).apply { description = EXIF_DESCRIPTION }
        val inputPacket =
            Packet.of(
                jpegrBytes,
                exif,
                ImageFormat.JPEG_R,
                Size(WIDTH, HEIGHT),
                Rect(0, 0, WIDTH, HEIGHT),
                ROTATION_DEGREES,
                Matrix(),
                CAMERA_CAPTURE_RESULT,
            )

        val savedFile = createTempJpgFile()
        val savedStreamFile = createTempJpgFile()
        try {
            operation.apply(
                JpegBytes2Disk.In.of(
                    inputPacket,
                    ImageCapture.OutputFileOptions.Builder(savedFile).build(),
                )
            )
            assertFileAndStreamHaveGainmap(savedFile)
            assertThat(Exif.createFromFile(savedFile).description).isEqualTo(EXIF_DESCRIPTION)

            FileOutputStream(savedStreamFile).use { out ->
                operation.apply(
                    JpegBytes2Disk.In.of(
                        inputPacket,
                        ImageCapture.OutputFileOptions.Builder(out).build(),
                    )
                )
            }
            assertFileAndStreamHaveGainmap(savedStreamFile)
        } finally {
            savedFile.delete()
            savedStreamFile.delete()
        }
    }

    @SdkSuppress(minSdkVersion = 34)
    @Test
    fun saveJpegrWithMpfBeforeApp1_preservesGainmap() {
        val compositeJpegr = moveMpfBeforeApp1Segments(createJpegrBytes(WIDTH, HEIGHT))
        ByteArrayInputStream(compositeJpegr).use { input ->
            val preSaveBitmap = BitmapFactory.decodeStream(input)
            assertThat(preSaveBitmap).isNotNull()
            try {
                assertThat(preSaveBitmap!!.hasGainmap()).isTrue()
            } finally {
                preSaveBitmap?.recycle()
            }
        }

        val exif = ExifUtil.createExif(compositeJpegr).apply { description = EXIF_DESCRIPTION }
        val inputPacket =
            Packet.of(
                compositeJpegr,
                exif,
                ImageFormat.JPEG_R,
                Size(WIDTH, HEIGHT),
                Rect(0, 0, WIDTH, HEIGHT),
                ROTATION_DEGREES,
                Matrix(),
                CAMERA_CAPTURE_RESULT,
            )

        val savedFile = createTempJpgFile()
        try {
            operation.apply(
                JpegBytes2Disk.In.of(
                    inputPacket,
                    ImageCapture.OutputFileOptions.Builder(savedFile).build(),
                )
            )
            assertFileAndStreamHaveGainmap(savedFile)
            val savedExif = Exif.createFromFile(savedFile)
            assertThat(savedExif.description).isEqualTo(EXIF_DESCRIPTION)
            assertThat(savedExif.rotation).isEqualTo(ROTATION_DEGREES)
        } finally {
            savedFile.delete()
        }
    }

    @RequiresApi(34)
    private fun assertFileAndStreamHaveGainmap(file: File) {
        val fileBitmap = BitmapFactory.decodeFile(file.path)
        assertThat(fileBitmap).isNotNull()
        try {
            assertThat(fileBitmap!!.hasGainmap()).isTrue()
        } finally {
            fileBitmap?.recycle()
        }

        FileInputStream(file).use { input ->
            val streamBitmap = BitmapFactory.decodeStream(input)
            assertThat(streamBitmap).isNotNull()
            try {
                assertThat(streamBitmap!!.hasGainmap()).isTrue()
            } finally {
                streamBitmap?.recycle()
            }
        }
    }

    private fun createTempJpgFile(): File =
        File.createTempFile("jpegr_disk_test_" + UUID.randomUUID(), ".jpg").also {
            it.deleteOnExit()
        }

    /**
     * Moves the primary `APP2 "MPF\0"` segment directly after `SOI` (ahead of `APP1 Exif` and `APP1
     * XMP`, matching Android 14 `Camera3-JpegRCompositeStream`) and adjusts dependent MP Entry
     * offsets accordingly so the input container is valid prior to [JpegBytes2Disk].
     */
    private fun moveMpfBeforeApp1Segments(jpegr: ByteArray): ByteArray {
        var pos = 2
        while (pos + 4 <= jpegr.size) {
            val segStart = pos
            check(jpegr[pos] == 0xFF.toByte())
            var marker = jpegr[pos + 1].toInt() and 0xFF
            pos += 2
            while (marker == 0xFF) {
                marker = jpegr[pos++].toInt() and 0xFF
            }
            check(marker != 0xDA) { "MPF APP2 segment not found before SOS" }
            val segLen = ((jpegr[pos].toInt() and 0xFF) shl 8) or (jpegr[pos + 1].toInt() and 0xFF)
            val segEnd = pos + segLen
            if (
                marker == 0xE2 &&
                    segLen >= 14 &&
                    String(jpegr, pos + 2, 4, Charsets.US_ASCII) == "MPF\u0000"
            ) {
                val mpfSegment = jpegr.copyOfRange(segStart, segEnd)
                val localTiffHeader = (pos + 6) - segStart
                val order =
                    when (String(mpfSegment, localTiffHeader, 4, Charsets.ISO_8859_1)) {
                        "II*\u0000" -> ByteOrder.LITTLE_ENDIAN
                        "MM\u0000*" -> ByteOrder.BIG_ENDIAN
                        else -> error("Invalid MPF byte order")
                    }
                val bb =
                    ByteBuffer.wrap(
                            mpfSegment,
                            localTiffHeader,
                            mpfSegment.size - localTiffHeader,
                        )
                        .slice()
                        .order(order)
                val ifdOffset = bb.getInt(4)
                val tagCount = bb.getShort(ifdOffset).toInt() and 0xFFFF
                val headerShift = segStart - 2
                for (i in 0 until tagCount) {
                    val tagOffset = ifdOffset + 2 + i * 12
                    if ((bb.getShort(tagOffset).toInt() and 0xFFFF) == 0xB002) {
                        val entryCount = bb.getInt(tagOffset + 4) / 16
                        val entriesOffset = bb.getInt(tagOffset + 8)
                        for (entryIndex in 1 until entryCount) {
                            val dataOffsetPos = entriesOffset + entryIndex * 16 + 8
                            bb.putInt(dataOffsetPos, bb.getInt(dataOffsetPos) + headerShift)
                        }
                    }
                }
                return jpegr.copyOfRange(0, 2) +
                    mpfSegment +
                    jpegr.copyOfRange(2, segStart) +
                    jpegr.copyOfRange(segEnd, jpegr.size)
            }
            pos = segEnd
        }
        error("MPF APP2 segment not found")
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
