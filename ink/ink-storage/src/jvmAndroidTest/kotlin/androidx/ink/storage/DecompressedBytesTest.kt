/*
 * Copyright (C) 2025 The Android Open Source Project
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

package androidx.ink.storage

import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class DecompressedBytesTest {

    private fun compressedStream(bytes: ByteArray): ByteArrayInputStream {
        return ByteArrayOutputStream().use { byteArrayStream ->
            GZIPOutputStream(byteArrayStream).use { gzipStream -> gzipStream.write(bytes) }
            ByteArrayInputStream(byteArrayStream.toByteArray())
        }
    }

    @Test
    fun decompress_belowInitialCapacity() {
        val originalBytes =
            ByteArray(DecompressedBytes.DECOMPRESSED_BYTES_INITIAL_CAPACITY - 1) { 1 }
        val decompressedBytes = DecompressedBytes(compressedStream(originalBytes))
        assertThat(decompressedBytes.size)
            .isEqualTo(DecompressedBytes.DECOMPRESSED_BYTES_INITIAL_CAPACITY - 1)
        assertThat(decompressedBytes.bytes.size)
            .isEqualTo(DecompressedBytes.DECOMPRESSED_BYTES_INITIAL_CAPACITY)
        assertThat(decompressedBytes.size).isEqualTo(originalBytes.size)
        assertThat(decompressedBytes.bytes.slice(0 until decompressedBytes.size).toByteArray())
            .isEqualTo(originalBytes)
        // The decompressed array starts with the decompressed bytes, then is padded with zeros.
        assertThat(
                decompressedBytes.bytes
                    .slice(decompressedBytes.size until decompressedBytes.bytes.size)
                    .toByteArray()
            )
            .isEqualTo(ByteArray(decompressedBytes.bytes.size - decompressedBytes.size) { 0 })
    }

    @Test
    fun decompress_exactlyAtInitialCapacity() {
        val originalBytes = ByteArray(DecompressedBytes.DECOMPRESSED_BYTES_INITIAL_CAPACITY) { 1 }
        val decompressedBytes = DecompressedBytes(compressedStream(originalBytes))
        assertThat(decompressedBytes.size)
            .isEqualTo(DecompressedBytes.DECOMPRESSED_BYTES_INITIAL_CAPACITY)
        // GZIPInputStream.read() doesn't report that it's done until _after_ the last read that
        // writes decompressed bytes, so the array had to be resized in case before the last read
        // reported "all done".
        assertThat(decompressedBytes.bytes.size)
            .isEqualTo(DecompressedBytes.DECOMPRESSED_BYTES_INITIAL_CAPACITY * 2)
        assertThat(decompressedBytes.size).isEqualTo(originalBytes.size)
        // The decompressed array starts with the decompressed bytes, then is padded with zeros.
        assertThat(decompressedBytes.bytes.slice(0 until decompressedBytes.size))
            .isEqualTo(originalBytes.toList())
        assertThat(
                decompressedBytes.bytes
                    .slice(decompressedBytes.size until decompressedBytes.bytes.size)
                    .toByteArray()
            )
            .isEqualTo(ByteArray(decompressedBytes.bytes.size - decompressedBytes.size) { 0 })
    }

    @Test
    fun decompress_greaterThanInitialCapacity() {
        val originalBytes =
            ByteArray(DecompressedBytes.DECOMPRESSED_BYTES_INITIAL_CAPACITY + 1) { 1 }
        val decompressedBytes = DecompressedBytes(compressedStream(originalBytes))
        assertThat(decompressedBytes.size)
            .isEqualTo(DecompressedBytes.DECOMPRESSED_BYTES_INITIAL_CAPACITY + 1)
        assertThat(decompressedBytes.bytes.size)
            .isEqualTo(DecompressedBytes.DECOMPRESSED_BYTES_INITIAL_CAPACITY * 2)
        assertThat(decompressedBytes.size).isEqualTo(originalBytes.size)
        // The decompressed array starts with the decompressed bytes, then is padded with zeros.
        assertThat(decompressedBytes.bytes.slice(0 until decompressedBytes.size).toByteArray())
            .isEqualTo(originalBytes)
        assertThat(
                decompressedBytes.bytes
                    .slice(decompressedBytes.size until decompressedBytes.bytes.size)
                    .toByteArray()
            )
            .isEqualTo(ByteArray(decompressedBytes.bytes.size - decompressedBytes.size) { 0 })
    }
}
