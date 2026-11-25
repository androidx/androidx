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

import androidx.camera.testing.impl.TestImageUtil.createJpegBytes
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Shared Multi-Picture Format (CIPA DC-007 `APP2 MPF`) test fixtures and inspectors. */
internal object MpfTestUtil {

    const val GAINMAP_XMP_PAYLOAD =
        "http://ns.adobe.com/xap/1.0/\u0000" +
            "<x:xmpmeta xmlns:hdrgm=\"http://ns.adobe.com/hdr-gain-map/1.0/\" " +
            "hdrgm:Version=\"1.0\" Item:Semantic=\"GainMap\"/>"

    data class MpEntrySpec(val attr: Int, val size: Int, val dataOffset: Int = 0)

    data class ParsedMpf(val mpHeaderOffset: Int, val entries: List<MpEntrySpec>)

    fun createBaseJpegWithExif(width: Int = 64, height: Int = 48): ByteArray {
        val tmp = File.createTempFile("mpf_base_exif_", ".jpg")
        try {
            tmp.writeBytes(createJpegBytes(width, height))
            Exif.createFromFile(tmp).apply {
                description = "Initial HAL Exif Payload Padding ".repeat(40)
                save()
            }
            return tmp.readBytes()
        } finally {
            tmp.delete()
        }
    }

    fun assembleMultiPictureJpeg(
        secondaryJpeg: ByteArray,
        littleEndian: Boolean,
        placeMpfBeforeExif: Boolean,
        baseJpegWithExif: ByteArray = createBaseJpegWithExif(),
        xmpApp1Payload: ByteArray = GAINMAP_XMP_PAYLOAD.toByteArray(Charsets.UTF_8),
        placeMpfBeforeXmp: Boolean = false,
    ): ByteArray {
        require(!(placeMpfBeforeExif && placeMpfBeforeXmp)) {
            "placeMpfBeforeExif and placeMpfBeforeXmp cannot both be true"
        }
        val baseAfterSoi = baseJpegWithExif.copyOfRange(2, baseJpegWithExif.size)
        val exifSegLen =
            2 + (((baseAfterSoi[2].toInt() and 0xFF) shl 8) or (baseAfterSoi[3].toInt() and 0xFF))
        val prefixLen = if (placeMpfBeforeExif) 0 else exifSegLen
        val jfif =
            if (placeMpfBeforeExif) {
                "JFIF\u0000\u0001\u0001\u0000\u0000\u0001\u0000\u0001\u0000\u0000"
                    .toByteArray(Charsets.ISO_8859_1)
            } else {
                null
            }
        val mpfSegTotal = 4 + 54 + 2 * 16
        val xmpSegTotal = 4 + xmpApp1Payload.size
        val preMpfXmpLen = if (placeMpfBeforeXmp) 0 else xmpSegTotal
        val mpHeaderOffset = 2 + prefixLen + preMpfXmpLen + 8
        val primarySize =
            2 + xmpSegTotal + mpfSegTotal + (jfif?.let { 4 + it.size } ?: 0) + baseAfterSoi.size
        val mpf =
            buildMpfPayload(
                littleEndian,
                MpEntrySpec(0x030000, primarySize),
                MpEntrySpec(0, secondaryJpeg.size, primarySize - mpHeaderOffset),
            )

        return buildBytes { dos ->
            dos.writeShort(0xFFD8)
            dos.write(baseAfterSoi, 0, prefixLen)
            if (!placeMpfBeforeXmp) writeSegment(dos, 0xFFE1, xmpApp1Payload)
            writeSegment(dos, 0xFFE2, mpf)
            if (placeMpfBeforeXmp) writeSegment(dos, 0xFFE1, xmpApp1Payload)
            if (jfif != null) writeSegment(dos, 0xFFE0, jfif)
            dos.write(baseAfterSoi, prefixLen, baseAfterSoi.size - prefixLen)
            dos.write(secondaryJpeg)
        }
    }

    fun buildMpfPayload(littleEndian: Boolean, vararg entries: MpEntrySpec): ByteArray {
        val mpEntriesBytes = entries.size * 16
        return ByteBuffer.allocate(54 + mpEntriesBytes)
            .apply {
                put("MPF\u0000".toByteArray(Charsets.US_ASCII))
                put(
                    if (littleEndian) byteArrayOf('I'.code.toByte(), 'I'.code.toByte(), 0x2A, 0)
                    else byteArrayOf('M'.code.toByte(), 'M'.code.toByte(), 0, 0x2A)
                )
                order(if (littleEndian) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)
                putInt(8).putShort(3)
                putShort(0xB000.toShort()).putShort(7).putInt(4)
                put("0100".toByteArray(Charsets.US_ASCII))
                putShort(0xB001.toShort()).putShort(4).putInt(1).putInt(entries.size)
                putShort(0xB002.toShort()).putShort(7).putInt(mpEntriesBytes).putInt(50)
                putInt(0)
                for (e in entries) putInt(e.attr).putInt(e.size).putInt(e.dataOffset).putInt(0)
            }
            .array()
    }

    /** Parses the first valid pre-SOS `APP2 "MPF\0"` Index IFD and its MP Entry table. */
    fun inspectMpf(jpeg: ByteArray): ParsedMpf {
        var pos = 2
        while (pos + 4 <= jpeg.size) {
            assertThat(jpeg[pos]).isEqualTo(0xFF.toByte())
            var marker = jpeg[pos + 1].toInt() and 0xFF
            pos += 2
            while (marker == 0xFF) marker = jpeg[pos++].toInt() and 0xFF
            if (marker == 0xDA) break
            val segLen = ((jpeg[pos].toInt() and 0xFF) shl 8) or (jpeg[pos + 1].toInt() and 0xFF)
            val nextPos = pos + segLen
            if (marker == 0xE2 && segLen >= 14 && jpeg.ascii(pos + 2, 4) == "MPF\u0000") {
                val mpHeader = pos + 6
                val order =
                    when (jpeg.ascii(mpHeader, 4)) {
                        "II*\u0000" -> ByteOrder.LITTLE_ENDIAN
                        "MM\u0000*" -> ByteOrder.BIG_ENDIAN
                        else -> null
                    }
                val bb = order?.let {
                    ByteBuffer.wrap(jpeg, mpHeader, segLen - 6).slice().order(it)
                }
                val ifdPos = bb?.getInt(4) ?: -1
                if (bb != null && ifdPos >= 8 && ifdPos + 2 <= bb.capacity()) {
                    var count = -1
                    var entriesPos = -1
                    repeat(bb.getShort(ifdPos).toInt() and 0xFFFF) { i ->
                        val entry = ifdPos + 2 + i * 12
                        when (bb.getShort(entry).toInt() and 0xFFFF) {
                            0xB001 -> count = bb.getInt(entry + 8)
                            0xB002 -> entriesPos = bb.getInt(entry + 8)
                        }
                    }
                    if (count >= 2 && entriesPos >= 8) {
                        val entries =
                            List(count) { i ->
                                val b = entriesPos + i * 16
                                MpEntrySpec(bb.getInt(b), bb.getInt(b + 4), bb.getInt(b + 8))
                            }
                        return ParsedMpf(mpHeader, entries)
                    }
                }
            }
            pos = nextPos
        }
        throw AssertionError("Valid MPF APP2 segment not found")
    }

    private fun ByteArray.ascii(offset: Int, len: Int) =
        String(this, offset, len, Charsets.ISO_8859_1)

    inline fun buildBytes(block: (DataOutputStream) -> Unit): ByteArray {
        val out = ByteArrayOutputStream()
        DataOutputStream(out).use(block)
        return out.toByteArray()
    }

    fun writeSegment(dos: DataOutputStream, marker: Int, payload: ByteArray) {
        dos.writeShort(marker)
        dos.writeShort(payload.size + 2)
        dos.write(payload)
    }
}
