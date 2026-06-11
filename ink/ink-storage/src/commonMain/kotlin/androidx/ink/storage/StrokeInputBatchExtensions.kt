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

@file:JvmMultifileClass
@file:JvmName("StrokeInputBatchExtensions")

package androidx.ink.storage

import androidx.ink.strokes.ImmutableStrokeInputBatch
import androidx.ink.strokes.StrokeInputBatch
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/**
 * Write a gzip-compressed `ink.proto.CodedStrokeInputBatch` binary proto message representing the
 * [StrokeInputBatch] to the given [ByteArray].
 */
public fun StrokeInputBatch.encode(): ByteArray = encodeUncompressed().compress()

/**
 * Read a serialized gzip-compressed `ink.proto.CodedStrokeInputBatch` from the given [ByteArray]
 * and parse it into a [ImmutableStrokeInputBatch], throwing an exception if parsing or validation
 * was not successful.
 *
 * @param input [ByteArray] providing gzip-compressed `ink.proto.CodedStrokeInputBatch` binary proto
 *   messages, the same as written by [encode].
 * @return The [ImmutableStrokeInputBatch] parsed from the [ByteArray].
 * @throws [IOException] if gzip-format bytes cannot be read from [input].
 * @throws [IllegalArgumentException] [input] does not provide a valid
 *   `ink.proto.CodedStrokeInputBatch` proto message, or the corresponding [StrokeInputBatch] is
 *   invalid.
 */
@Throws(IOException::class)
public fun StrokeInputBatch.Companion.decode(input: ByteArray): ImmutableStrokeInputBatch =
    decodeUncompressed(DecompressedBytes(input))

/**
 * Internal KMP-common helper for encoding a [StrokeInputBatch] to a [ByteArray] after compression.
 */
internal fun StrokeInputBatch.encodeUncompressed(): ByteArray =
    StrokeInputBatchSerializationNative.encode(nativePointer)

/**
 * Internal KMP-common helper for decoding a [StrokeInputBatch] from a [ByteArray] after
 * decompression. Takes a size argument so an extra copy is not required to resize the array down if
 * excess capacity was allocated during decompression.
 */
@Throws(IOException::class)
internal fun StrokeInputBatch.Companion.decodeUncompressed(
    input: DecompressedBytes
): ImmutableStrokeInputBatch =
    ImmutableStrokeInputBatch.wrapNative {
        StrokeInputBatchSerializationNative.createFromProto(input.buffer, input.size)
    }

expect internal object StrokeInputBatchSerializationNative {
    // Returns a native pointer to a `StrokeInputBatch`.
    fun createFromProto(decompressedBytes: ByteArray, size: Int): Long

    fun encode(nativeStrokeInputBatchPointer: Long): ByteArray
}
