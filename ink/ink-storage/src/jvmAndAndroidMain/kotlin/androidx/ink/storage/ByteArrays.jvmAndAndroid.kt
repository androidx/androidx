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

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Arrays
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/** Gets decompressed [ByteArray] from an [InputStream] of GZIP-compressed bytes. */
actual internal class DecompressedBytes(compressedBytesInputStream: InputStream) {

    actual constructor(compressedBytes: ByteArray) : this(ByteArrayInputStream(compressedBytes))

    /** The first [size] bytes of this contain the decompressed bytes, the rest are zero. */
    actual val buffer: ByteArray

    /** The size of the initial portion of [buffer] containing the decompressed bytes. */
    actual val size: Int

    init {
        var byteArray = ByteArray(DECOMPRESSED_BYTES_INITIAL_CAPACITY)
        var totalBytesRead = 0
        GZIPInputStream(compressedBytesInputStream).use { gzipStream ->
            // Could do gzipStream.readAllBytes(), but that requires Android T (33), since it's only
            // available as of OpenJDK 11.
            while (true) {
                val bytesRead =
                    gzipStream.read(byteArray, totalBytesRead, byteArray.size - totalBytesRead)
                if (bytesRead == -1) {
                    // The last read is allowed to use the whole range `[offset, offset + length)`
                    // for scratch
                    // work (see documentation for [GZIPInputStream.read]), so the portion between
                    // `size` and
                    // the end of the array is undefined. Avoid the cost of a last copy to resize
                    // down, but
                    // zero-fill the remainder of the array to avoid surprising behavior.
                    Arrays.fill(byteArray, totalBytesRead, byteArray.size, 0)
                    break
                }
                totalBytesRead += bytesRead
                // If the array is full, double its size.
                if (totalBytesRead == byteArray.size) {
                    byteArray = Arrays.copyOf(byteArray, totalBytesRead * 2)
                }
            }
            buffer = byteArray
            size = totalBytesRead
        }
    }

    companion object {
        const val DECOMPRESSED_BYTES_INITIAL_CAPACITY: Int = 32 * 1024
    }
}

actual internal fun ByteArray.compress(): ByteArray =
    ByteArrayOutputStream().use { byteArrayStream ->
        GZIPOutputStream(byteArrayStream).use { gzipStream -> gzipStream.write(this) }
        byteArrayStream.toByteArray()
    }

// This is a typealias to allow documenting behavior of a KMP-common method and allow writing
// exception-handling code in a KMP-common way without requiring JVM consumers to take on a
// dependency on Okio. It is capitalized for consistency with java.io.IOException.
@Suppress("TypealiasDefinition", "AcronymName")
actual public typealias IOException = java.io.IOException
