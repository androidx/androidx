/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.camera.core.impl.utils

import androidx.camera.core.impl.utils.MpfTestUtil.GAINMAP_XMP_PAYLOAD
import androidx.camera.core.impl.utils.MpfTestUtil.MpEntrySpec
import androidx.camera.core.impl.utils.MpfTestUtil.assembleMultiPictureJpeg
import androidx.camera.core.impl.utils.MpfTestUtil.buildBytes
import androidx.camera.core.impl.utils.MpfTestUtil.buildMpfPayload
import androidx.camera.core.impl.utils.MpfTestUtil.createBaseJpegWithExif
import androidx.camera.core.impl.utils.MpfTestUtil.inspectMpf
import androidx.camera.core.impl.utils.MpfTestUtil.writeSegment
import androidx.camera.testing.impl.TestImageUtil.createJpegBytes
import com.google.common.truth.Truth.assertThat
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/** Unit tests for [JpegMpfUpdater] and Multi-Picture [Exif.save] offset preservation. */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Config.TARGET_SDK])
class JpegMpfUpdaterTest {

    @get:Rule val tempFolder = TemporaryFolder()

    @Test
    fun exifSave_whenApp1ExifFollowsApp2Mpf_reconcilesGainmapOffsetBigEndian() {
        verifyExifSavePreservesGainmapMpfOffsets(littleEndian = false, placeMpfBeforeExif = true)
    }

    @Test
    fun exifSave_whenApp1ExifFollowsApp2Mpf_reconcilesGainmapOffsetLittleEndian() {
        verifyExifSavePreservesGainmapMpfOffsets(littleEndian = true, placeMpfBeforeExif = true)
    }

    @Test
    fun exifSave_whenApp1ExifPrecedesApp2MpfAndShrinks_updatesPrimaryImageSize() {
        verifyExifSavePreservesGainmapMpfOffsets(littleEndian = false, placeMpfBeforeExif = false)
    }

    @Test
    fun exifSave_whenApp1XmpFollowsApp2Mpf_reconcilesGainmapOffset() {
        verifyExifSavePreservesGainmapMpfOffsets(
            littleEndian = false,
            placeMpfBeforeExif = false,
            placeMpfBeforeXmp = true,
        )
        verifyExifSavePreservesGainmapMpfOffsets(
            littleEndian = true,
            placeMpfBeforeExif = false,
            placeMpfBeforeXmp = true,
        )
    }

    @Test
    fun exifSave_whenApp1ExifGrows_updatesPrimaryImageSizeAndGainmapOffset() {
        val grownDescription = "Expanded CameraX UltraHDR Description ".repeat(60)
        verifyExifSavePreservesGainmapMpfOffsets(
            littleEndian = false,
            placeMpfBeforeExif = false,
            updatedDescription = grownDescription,
            expectGrowth = true,
        )
        verifyExifSavePreservesGainmapMpfOffsets(
            littleEndian = true,
            placeMpfBeforeExif = true,
            updatedDescription = grownDescription,
            expectGrowth = true,
        )
    }

    @Test
    fun reconcile_whenLengthAndMpfOffsetUnchangedOrAlreadyUpdated_isIdempotent() {
        val multiPictureBytes =
            assembleMultiPictureJpeg(
                secondaryJpeg = createJpegBytes(32, 24),
                littleEndian = false,
                placeMpfBeforeExif = true,
            )
        val file = tempFolder.newFile("idempotent_mpf.jpg").apply { writeBytes(multiPictureBytes) }

        // 1. Zero-delta snapshot + reconcile leaves file byte-identical.
        val unchangedSnapshot = requireNotNull(JpegMpfUpdater.snapshot(file))
        JpegMpfUpdater.reconcile(file, unchangedSnapshot)
        assertThat(file.readBytes()).isEqualTo(multiPictureBytes)

        // 2. If MP Entries are already updated, calling reconcile() again with the pre-save
        // snapshot must not double-shift Entry[0].size or Entry[1].dataOffset.
        val preSaveSnapshot = requireNotNull(JpegMpfUpdater.snapshot(file))
        Exif.createFromFile(file).apply {
            description = "Updated once"
            save()
        }
        val onceReconciledBytes = file.readBytes()

        JpegMpfUpdater.reconcile(file, preSaveSnapshot)
        assertThat(file.readBytes()).isEqualTo(onceReconciledBytes)
    }

    @Test
    fun snapshot_singleImageSdrJpeg_returnsNull() {
        val file = tempFolder.newFile("sdr.jpg").apply { writeBytes(createJpegBytes(64, 48)) }
        assertThat(JpegMpfUpdater.snapshot(file)).isNull()
    }

    @Test
    fun exifSave_threeImagesOutOfFileOrderAndNonZeroVendorPadding_preservesOrderAndSizes() {
        val baseBodyAfterSoi = createBaseJpegWithExif().let { it.copyOfRange(2, it.size) }
        val previewJpeg = createMinimalValidJpeg(byteArrayOf(0x11, 0x22, 0x33))
        val gainmapJpeg = createMinimalValidJpeg(byteArrayOf(0x44, 0x55, 0x66, 0x77))
        val vendorPadding = ByteArray(96) { 0xA5.toByte() }
        val xmpPayload = GAINMAP_XMP_PAYLOAD.toByteArray(Charsets.UTF_8)

        val mpfSegSize = 4 + 54 + 3 * 16
        val mpHeaderOffset = 2 + (4 + xmpPayload.size) + 8
        val primaryEnd = 2 + (4 + xmpPayload.size) + mpfSegSize + baseBodyAfterSoi.size
        val previewStart = primaryEnd + vendorPadding.size
        val gainmapStart = previewStart + previewJpeg.size + vendorPadding.size
        val halPrimarySizeWithPadding = primaryEnd + vendorPadding.size

        val realMpf =
            buildMpfPayload(
                true,
                MpEntrySpec(0x030000, halPrimarySizeWithPadding),
                MpEntrySpec(0, gainmapJpeg.size, gainmapStart - mpHeaderOffset),
                MpEntrySpec(0, previewJpeg.size, previewStart - mpHeaderOffset),
            )

        val originalBytes = buildBytes { dos ->
            dos.writeShort(0xFFD8)
            writeSegment(dos, 0xFFE1, xmpPayload)
            writeSegment(dos, 0xFFE2, realMpf)
            dos.write(baseBodyAfterSoi)
            dos.write(vendorPadding)
            dos.write(previewJpeg)
            dos.write(vendorPadding)
            dos.write(gainmapJpeg)
        }
        val file = tempFolder.newFile("out_of_order_3img.jpg").apply { writeBytes(originalBytes) }

        Exif.createFromFile(file).apply {
            description = "Short desc"
            save()
        }

        val savedBytes = file.readBytes()
        val tailDelta = savedBytes.size - originalBytes.size
        val parsed = inspectMpf(savedBytes)
        assertThat(parsed.entries).hasSize(3)
        assertThat(parsed.entries[0].size).isEqualTo(halPrimarySizeWithPadding + tailDelta)
        assertThat(parsed.entries[0].dataOffset).isEqualTo(0)

        val resolvedGainmapStart = parsed.mpHeaderOffset + parsed.entries[1].dataOffset
        val resolvedPreviewStart = parsed.mpHeaderOffset + parsed.entries[2].dataOffset
        assertThat(resolvedGainmapStart).isGreaterThan(resolvedPreviewStart)
        assertThat(
                savedBytes.copyOfRange(
                    resolvedGainmapStart,
                    resolvedGainmapStart + parsed.entries[1].size,
                )
            )
            .isEqualTo(gainmapJpeg)
        assertThat(
                savedBytes.copyOfRange(
                    resolvedPreviewStart,
                    resolvedPreviewStart + parsed.entries[2].size,
                )
            )
            .isEqualTo(previewJpeg)
    }

    @Test
    fun exifSave_4096ByteZeroPaddingFillBytesAndPrecedingApp2_reconcilesValidMpf() {
        val baseBodyAfterSoi = createBaseJpegWithExif().let { it.copyOfRange(2, it.size) }
        val secondaryGainmap = createJpegBytes(32, 24)
        val iccApp2 = "ICC_PROFILE\u0000\u0001\u0001dummy".toByteArray(Charsets.US_ASCII)
        val malformedMpf = "MPF\u0000XX\u0000\u0000\u0000\u0000\u0000\u0000".toByteArray()
        val dmaZeroPadding = ByteArray(4096)
        val mpfSegSize = 4 + 54 + 2 * 16

        // Include 1 and 2 optional 0xFF fill bytes before markers (ITU T.81 B.1.1.2).
        val mpHeaderOffset = 2 + (4 + iccApp2.size) + 1 + (4 + malformedMpf.size) + 2 + 8
        val primaryEnd =
            2 +
                (4 + iccApp2.size) +
                1 +
                (4 + malformedMpf.size) +
                2 +
                mpfSegSize +
                baseBodyAfterSoi.size
        val validMpf =
            buildMpfPayload(
                false,
                MpEntrySpec(0x030000, primaryEnd),
                MpEntrySpec(
                    0,
                    secondaryGainmap.size,
                    primaryEnd + dmaZeroPadding.size - mpHeaderOffset,
                ),
            )

        val originalBytes = buildBytes { dos ->
            dos.writeShort(0xFFD8)
            writeSegment(dos, 0xFFE2, iccApp2)
            dos.writeByte(0xFF)
            writeSegment(dos, 0xFFE2, malformedMpf)
            dos.writeShort(0xFFFF)
            writeSegment(dos, 0xFFE2, validMpf)
            dos.write(baseBodyAfterSoi)
            dos.write(dmaZeroPadding)
            dos.write(secondaryGainmap)
        }
        val file = tempFolder.newFile("icc_dma_4096.jpg").apply { writeBytes(originalBytes) }

        Exif.createFromFile(file).apply {
            description = "DMA 4096 aligned"
            save()
        }

        val savedBytes = file.readBytes()
        val parsed = inspectMpf(savedBytes)
        assertThat(parsed.entries[0].size)
            .isEqualTo(primaryEnd + (savedBytes.size - originalBytes.size))
        val resolvedGainmapStart = parsed.mpHeaderOffset + parsed.entries[1].dataOffset
        assertThat(resolvedGainmapStart).isEqualTo(parsed.entries[0].size + dmaZeroPadding.size)
        assertThat(savedBytes.copyOfRange(resolvedGainmapStart, savedBytes.size))
            .isEqualTo(secondaryGainmap)
    }

    @Test
    fun snapshotAndReconcile_malformedOrTruncatedInput_isNoOpWithoutFailingExifSave() {
        val baseJpegWithExif = createBaseJpegWithExif()
        val baseBodyAfterSoi = baseJpegWithExif.copyOfRange(2, baseJpegWithExif.size)
        val validMpf = buildMpfPayload(false, MpEntrySpec(0x030000, 100), MpEntrySpec(0, 50, 80))

        fun jpegWithCustomIfd(tagCount: Int, block: ByteBuffer.() -> Unit): ByteArray {
            val ifdPayload =
                ByteBuffer.allocate(86)
                    .order(ByteOrder.BIG_ENDIAN)
                    .apply {
                        put("MPF\u0000".toByteArray(Charsets.US_ASCII))
                        put(byteArrayOf('M'.code.toByte(), 'M'.code.toByte(), 0, 0x2A))
                        putInt(8).putShort(tagCount.toShort())
                        block()
                        putInt(0)
                    }
                    .array()
            return buildBytes { dos ->
                dos.writeShort(0xFFD8)
                writeSegment(dos, 0xFFE2, ifdPayload)
                dos.write(baseBodyAfterSoi)
            }
        }

        fun jpegWithPrefixMarker(marker: Int): ByteArray = buildBytes { dos ->
            dos.writeShort(0xFFD8)
            dos.writeShort(marker)
            writeSegment(dos, 0xFFE2, validMpf)
            writeSegment(dos, 0xFFDA, byteArrayOf(0x00, 0x01, 0x00))
            dos.writeShort(0xFFD9)
        }

        val malformedCases =
            listOf(
                "dup_ifd" to
                    jpegWithCustomIfd(3) {
                        putShort(0xB001.toShort()).putShort(4).putInt(1).putInt(2)
                        putShort(0xB002.toShort()).putShort(7).putInt(32).putInt(50)
                        putShort(0xB001.toShort()).putShort(4).putInt(1).putInt(3)
                    },
                "overlap_ifd" to
                    jpegWithCustomIfd(2) {
                        putShort(0xB001.toShort()).putShort(4).putInt(1).putInt(2)
                        putShort(0xB002.toShort()).putShort(7).putInt(32).putInt(16)
                    },
                "overflow_count" to
                    jpegWithCustomIfd(2) {
                        putShort(0xB001.toShort()).putShort(4).putInt(1).putInt(0x80000002.toInt())
                        putShort(0xB002.toShort()).putShort(7).putInt(32).putInt(50)
                    },
                "ff00_hdr" to jpegWithPrefixMarker(0xFF00),
                "dupsoi_hdr" to jpegWithPrefixMarker(0xFFD8),
                "trunc_seg" to
                    byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE1.toByte(), 0x00),
            )

        for ((name, bytes) in malformedCases) {
            val file = tempFolder.newFile("$name.jpg").apply { writeBytes(bytes) }
            assertThat(JpegMpfUpdater.snapshot(file)).isNull()
        }

        val saveable =
            tempFolder.newFile("dup_save.jpg").apply { writeBytes(malformedCases[0].second) }
        Exif.createFromFile(saveable).apply {
            description = "Safe save with malformed IFD"
            save()
        }
        assertThat(Exif.createFromFile(saveable).description)
            .isEqualTo("Safe save with malformed IFD")

        // Post-snapshot truncation or deletion must no-op without recreating an orphan file.
        val truncatedOrDeleted =
            tempFolder.newFile("truncated_test.jpg").apply {
                writeBytes(
                    assembleMultiPictureJpeg(
                        secondaryJpeg = createJpegBytes(32, 24),
                        littleEndian = false,
                        placeMpfBeforeExif = true,
                        baseJpegWithExif = baseJpegWithExif,
                    )
                )
            }
        val snapshot = requireNotNull(JpegMpfUpdater.snapshot(truncatedOrDeleted))
        RandomAccessFile(truncatedOrDeleted, "rw").use { it.setLength(0) }
        JpegMpfUpdater.reconcile(truncatedOrDeleted, snapshot)
        assertThat(truncatedOrDeleted.length()).isEqualTo(0L)
        assertThat(truncatedOrDeleted.delete()).isTrue()
        JpegMpfUpdater.reconcile(truncatedOrDeleted, snapshot)
        assertThat(truncatedOrDeleted.exists()).isFalse()
    }

    @Test
    fun exifSave_truncatedMpfWithoutSecondaryImage_leavesMpfEntryUnmodified() {
        val baseBodyAfterSoi = createBaseJpegWithExif().let { it.copyOfRange(2, it.size) }
        val mpfPayload =
            buildMpfPayload(false, MpEntrySpec(0x030000, 100), MpEntrySpec(0, 50, 50000))
        val file =
            tempFolder.newFile("truncated_mpf.jpg").apply {
                writeBytes(
                    buildBytes { dos ->
                        dos.writeShort(0xFFD8)
                        writeSegment(dos, 0xFFE2, mpfPayload)
                        dos.write(baseBodyAfterSoi)
                    }
                )
            }

        Exif.createFromFile(file).apply {
            description = "Still saves valid EXIF without failing capture"
            save()
        }

        assertThat(inspectMpf(file.readBytes()).entries[1].dataOffset).isEqualTo(50000)
        assertThat(Exif.createFromFile(file).description)
            .isEqualTo("Still saves valid EXIF without failing capture")
    }

    private fun verifyExifSavePreservesGainmapMpfOffsets(
        littleEndian: Boolean,
        placeMpfBeforeExif: Boolean,
        updatedDescription: String = "Updated CameraX UltraHDR Description",
        expectGrowth: Boolean = false,
        placeMpfBeforeXmp: Boolean = false,
    ) {
        val secondaryGainmap = createJpegBytes(32, 24)
        val multiPictureBytes =
            assembleMultiPictureJpeg(
                secondaryGainmap,
                littleEndian,
                placeMpfBeforeExif,
                placeMpfBeforeXmp = placeMpfBeforeXmp,
            )

        val file =
            tempFolder
                .newFile(
                    "ultrahdr_${littleEndian}_${placeMpfBeforeExif}_" +
                        "${placeMpfBeforeXmp}_$expectGrowth.jpg"
                )
                .apply { writeBytes(multiPictureBytes) }
        Exif.createFromFile(file).apply {
            description = updatedDescription
            rotate(90)
            save()
        }

        val savedBytes = file.readBytes()
        if (expectGrowth) {
            assertThat(savedBytes.size).isGreaterThan(multiPictureBytes.size)
        } else {
            assertThat(savedBytes.size).isLessThan(multiPictureBytes.size)
        }
        val parsedMpf = inspectMpf(savedBytes)
        val resolvedGainmapStart = parsedMpf.mpHeaderOffset + parsedMpf.entries[1].dataOffset

        assertThat(resolvedGainmapStart + parsedMpf.entries[1].size).isEqualTo(savedBytes.size)
        assertThat(parsedMpf.entries[0].size).isEqualTo(resolvedGainmapStart)
        assertThat(savedBytes.copyOfRange(resolvedGainmapStart, savedBytes.size))
            .isEqualTo(secondaryGainmap)

        val reloadedExif = Exif.createFromFile(file)
        assertThat(reloadedExif.description).isEqualTo(updatedDescription)
        assertThat(reloadedExif.metadata).contains("Item:Semantic=\"GainMap\"")
    }

    private fun createMinimalValidJpeg(scanBytes: ByteArray): ByteArray = buildBytes { dos ->
        dos.writeShort(0xFFD8)
        writeSegment(dos, 0xFFDA, byteArrayOf(0x00, 0x01, 0x00))
        dos.write(scanBytes)
        dos.writeShort(0xFFD9)
    }
}
