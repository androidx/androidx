/*
 * Copyright (C) 2024 The Android Open Source Project
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

@file:JvmName("StrokeInputBatchSerialization")

package androidx.ink.storage

import androidx.ink.strokes.ImmutableStrokeInputBatch
import androidx.ink.strokes.StrokeInputBatch
import java.io.InputStream
import java.io.OutputStream
import java.util.Arrays
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

private const val DECOMPRESSED_BYTES_INITIAL_CAPACITY = 32 * 1024

/**
 * Read a serialized [CodedStrokeInputBatch] from the given [InputStream] and parse it into an
 * [ImmutableStrokeInputBatch], throwing an exception if parsing was not successful. The serialized
 * representation is gzip-compressed `ink.proto.CodedStrokeInputBatch` binary proto messages, the
 * same as written to [OutputStream] by [StrokeInputBatch.encode]. Java callers should use
 * [StrokeInputBatchSerialization.decodeStrokeInputBatchOrThrow].
 */
public fun StrokeInputBatch.Companion.decodeOrThrow(input: InputStream): ImmutableStrokeInputBatch =
    decode(input, throwOnParseError = true)!!

/**
 * Read a serialized [CodedStrokeInputBatch] from the given [InputStream] and parse it into an
 * [ImmutableStrokeInputBatch], returning `null` if parsing was not successful. The serialized
 * representation is gzip-compressed `ink.proto.CodedStrokeInputBatch` binary proto messages, the
 * same as written to [OutputStream] by [StrokeInputBatch.encode]. Java callers should use
 * [StrokeInputBatchSerialization.decodeStrokeInputBatchOrNull].
 */
public fun StrokeInputBatch.Companion.decodeOrNull(input: InputStream): ImmutableStrokeInputBatch? =
    decode(input, throwOnParseError = false)

/**
 * Write the gzip-compressed serialized representation of the [CodedStrokeInputBatch] to the given
 * [OutputStream].
 */
public fun StrokeInputBatch.encode(output: OutputStream) {
    check(nativePointer != 0L) { "the StrokeInputBatch is already closed" }
    GZIPOutputStream(output).use {
        it.write(StrokeInputBatchSerializationJni.nativeSerializeStrokeInputBatch(nativePointer))
    }
}

/**
 * Read a serialized [CodedStrokeInputBatch] from the given [InputStream] and parse it into an
 * [ImmutableStrokeInputBatch], throwing an exception if parsing was not successful. The serialized
 * representation is gzip-compressed `ink.proto.CodedStrokeInputBatch` binary proto messages, the
 * same as written to [OutputStream] by [StrokeInputBatch.encode]. Kotlin callers should use
 * [StrokeInputBatch.Companion.decodeOrThrow] instead.
 */
public fun decodeOrThrow(input: InputStream): ImmutableStrokeInputBatch =
    StrokeInputBatch.decodeOrThrow(input)

/**
 * Read a serialized [CodedStrokeInputBatch] from the given [InputStream] and parse it into an
 * [ImmutableStrokeInputBatch], returning `null` if parsing was not successful. The serialized
 * representation is gzip-compressed `ink.proto.CodedStrokeInputBatch` binary proto messages, the
 * same as written to [OutputStream] by [StrokeInputBatch.encode]. Kotlin callers should use
 * [StrokeInputBatch.Companion.decodeOrNull] instead.
 */
public fun decodeOrNull(input: InputStream): ImmutableStrokeInputBatch? =
    StrokeInputBatch.decodeOrNull(input)

/**
 * A helper for the public functions for decoding a [CodedStrokeInputBatch] from an [InputStream]
 * providing the serialized representation put to an [OutputStream] by [StrokeInputBatch.encode].
 * The serialized representation is gzip-compressed `ink.proto.CodedStrokeInputBatch` binary proto
 * messages.
 *
 * @param throwOnParseError Configuration flag for whether to throw (`true`) or return null
 *   (`false`) when the underlying parsing fails. If an exception is thrown, it should have a
 *   descriptive error message.
 */
private fun decode(input: InputStream, throwOnParseError: Boolean): ImmutableStrokeInputBatch? {
    var decompressedBytes = ByteArray(DECOMPRESSED_BYTES_INITIAL_CAPACITY)
    var totalBytesRead = 0
    GZIPInputStream(input).use { gzipStream ->
        // Could do gzipStream.readAllBytes(), but that requires Android T (33), since it's only
        // available as of OpenJDK 11.
        while (true) {
            val bytesRead =
                gzipStream.read(
                    decompressedBytes,
                    totalBytesRead,
                    decompressedBytes.size - totalBytesRead
                )
            if (bytesRead == -1) {
                break
            }
            totalBytesRead += bytesRead
            if (totalBytesRead == decompressedBytes.size) {
                decompressedBytes = Arrays.copyOf(decompressedBytes, decompressedBytes.size * 2)
            }
        }
    }
    val nativeAddress =
        StrokeInputBatchSerializationJni.nativeNewStrokeInputBatchFromProto(
            strokeInputBatchDirectBuffer = null,
            strokeInputBatchBytes = decompressedBytes,
            strokeInputBatchOffset = 0,
            strokeInputBatchLength = totalBytesRead,
            throwOnParseError = throwOnParseError,
        )
    if (nativeAddress == 0L) {
        check(!throwOnParseError) {
            "throwOnParseError is set and the native call returned a zero memory address."
        }
        return null
    }
    return ImmutableStrokeInputBatch(nativeAddress)
}
