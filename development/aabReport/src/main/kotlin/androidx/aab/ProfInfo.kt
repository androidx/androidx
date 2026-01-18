/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.aab

import java.io.InputStream
import java.lang.Byte
import java.nio.charset.StandardCharsets
import java.util.zip.Inflater

/**
 * Bundle information captured from `baseline.prof`
 *
 * This implementation was lovingly lifted from
 * profgen/src/main/kotlin/com/android/tools/profgen/ArtProfileSerializer.kt and friends.
 *
 * Ideally, we'd use Profgen to extract dex checksums and other similar metadata so we wouldn't need
 * to reimplement this.
 */
data class ProfInfo(val profDexInfoList: List<ProfDexInfo>) {
    data class ProfDexInfo(val checksumCrc32: String)

    companion object {
        const val BUNDLE_LOCATION = "BUNDLE-METADATA/com.android.tools.build.profiles/baseline.prof"
        const val APK_LOCATION = "assets/dexopt/baseline.prof"

        internal fun byteArrayOf(vararg chars: Char) =
            ByteArray(chars.size) { chars[it].code.toByte() }

        val MAGIC_PROF = byteArrayOf('p', 'r', 'o', '\u0000')
        val VERSION_P = byteArrayOf('0', '1', '0', '\u0000')

        internal fun InputStream.readAndCheckProfileVersion() {
            val fileMagic = read(MAGIC_PROF.size)
            check(fileMagic.contentEquals(MAGIC_PROF))
            val version = read(VERSION_P.size)
            check(version.contentEquals(VERSION_P))
        }

        /**
         * Attempts to read {@param length} bytes from the input stream. If not enough bytes are
         * available it throws [IllegalStateException].
         */
        internal fun InputStream.read(length: Int): ByteArray {
            val buffer = ByteArray(length)
            var offset = 0
            while (offset < length) {
                val result = read(buffer, offset, length - offset)
                if (result < 0) {
                    error("Not enough bytes to read: $length")
                }
                offset += result
            }
            return buffer
        }

        internal fun InputStream.readUInt8(): Int = readUInt(1).toInt()

        /** Reads the equivalent of an 16 bit unsigned integer (uint16_t in c++). */
        internal fun InputStream.readUInt16(): Int = readUInt(2).toInt()

        /** Reads the equivalent of an 32 bit unsigned integer (uint32_t in c++). */
        internal fun InputStream.readUInt32(): Long = readUInt(4)

        internal fun InputStream.readUInt(numberOfBytes: Int): Long {
            val buffer = read(numberOfBytes)
            // We use a long to cover for unsigned integer.
            var value: Long = 0
            for (k in 0 until numberOfBytes) {
                val next = buffer[k].toUByte().toLong()
                value += next shl k * Byte.SIZE
            }
            return value
        }

        /**
         * Reads bytes from the stream and converts them to a string using UTF-8.
         *
         * @param size the number of bytes to read
         */
        internal fun InputStream.readString(size: Int): String =
            String(read(size), StandardCharsets.UTF_8)

        /**
         * Reads a compressed data region from the stream.
         *
         * @param compressedDataSize the size of the compressed data (bytes)
         * @param uncompressedDataSize the expected size of the uncompressed data (bytes)
         */
        internal fun InputStream.readCompressed(
            compressedDataSize: Int,
            uncompressedDataSize: Int,
        ): ByteArray {
            // Read the expected compressed data size.
            val inf = Inflater()
            val result = ByteArray(uncompressedDataSize)
            var totalBytesRead = 0
            var totalBytesInflated = 0
            val input = ByteArray(2048) // 2KB read window size;
            while (
                !inf.finished() && !inf.needsDictionary() && totalBytesRead < compressedDataSize
            ) {
                val bytesRead = read(input)
                if (bytesRead < 0) {
                    error(
                        "Invalid zip data. Stream ended after $totalBytesRead bytes. Expected $compressedDataSize bytes"
                    )
                }
                inf.setInput(input, 0, bytesRead)
                totalBytesInflated +=
                    inf.inflate(
                        result,
                        totalBytesInflated,
                        uncompressedDataSize - totalBytesInflated,
                    )
                totalBytesRead += bytesRead
            }
            if (totalBytesRead != compressedDataSize) {
                error(
                    "Didn't read enough bytes during decompression. expected=$compressedDataSize actual=$totalBytesRead"
                )
            }
            if (!inf.finished()) {
                error("Inflater did not finish")
            }
            return result
        }

        private fun InputStream.readUncompressedBody(numberOfDexFiles: Int): ProfInfo {
            // If the uncompressed profile data stream is empty then we have nothing more to do.
            if (available() == 0) {
                return ProfInfo(emptyList())
            }
            // Read the dex file line headers.
            return ProfInfo(
                List(numberOfDexFiles) {
                    val profileKeySize = readUInt16()
                    val typeIdSetSize = readUInt16()
                    val hotMethodRegionSize = readUInt32()
                    val dexChecksum = readUInt32()
                    val numMethodIds = readUInt32()
                    val profileKey = readString(profileKeySize)
                    ProfDexInfo(
                        // profileKeySize = profileKeySize,
                        // typeIdSetSize = typeIdSetSize,
                        // hotMethodRegionSize = hotMethodRegionSize,
                        checksumCrc32 = dexChecksum.toInt().toHexString()
                        // numMethodIds = numMethodIds,
                        // profileKey = profileKey,
                    )
                }
            )

            // TODO: consider more verification of profiles here!
        }

        fun readFromProfile(src: InputStream): ProfInfo =
            with(src) {
                readAndCheckProfileVersion() // read 8
                val numberOfDexFiles = readUInt8()
                val uncompressedDataSize = readUInt32()
                val compressedDataSize = readUInt32()
                val uncompressedData =
                    readCompressed(compressedDataSize.toInt(), uncompressedDataSize.toInt())
                if (read() > 0) error("Content found after the end of file")

                val dataStream = uncompressedData.inputStream()

                dataStream.readUncompressedBody(numberOfDexFiles)
            }

        val CSV_COLUMNS =
            listOf(
                CsvColumn<ProfInfo?>(
                    "prof_present",
                    description = "Whether baseline profile is present",
                    calculate = { (it != null).toString() },
                ),
                CsvColumn<ProfInfo?>(
                    "prof_dexSortedChecksumsCrc32",
                    description =
                        "Sorted list of dex crc checksums from baseline profile - any of these that are missing from dex indicate profile corruption",
                    requiresVerbose = true,
                    calculate = { profInfo ->
                        (profInfo
                                ?.profDexInfoList
                                ?.map { it.checksumCrc32 }
                                ?.sorted()
                                ?.joinToString(INTERNAL_CSV_SEPARATOR))
                            .toString()
                    },
                ),
            )
    }
}
