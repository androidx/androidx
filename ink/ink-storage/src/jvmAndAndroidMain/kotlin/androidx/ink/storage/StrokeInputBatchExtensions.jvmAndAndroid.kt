/*
 * Copyright (C) 2026 The Android Open Source Project
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

import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative
import androidx.ink.strokes.ImmutableStrokeInputBatch
import androidx.ink.strokes.StrokeInputBatch
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.GZIPOutputStream

/**
 * Write a gzip-compressed `ink.proto.CodedStrokeInputBatch` binary proto message representing the
 * [StrokeInputBatch] to the given [OutputStream].
 */
public fun StrokeInputBatch.encode(output: OutputStream) {
    GZIPOutputStream(output).use { it.write(encodeUncompressed()) }
}

/**
 * Read a serialized gzip-compressed `ink.proto.CodedStrokeInputBatch` from the given [InputStream]
 * and parse it into a [ImmutableStrokeInputBatch], throwing an exception if parsing or validation
 * was not successful. Java callers should use [StrokeInputBatchSerialization.decode] instead.
 *
 * @param input [InputStream] providing gzip-compressed `ink.proto.CodedStrokeInputBatch` binary
 *   proto messages, the same as written by [encode].
 * @return The [ImmutableStrokeInputBatch] parsed from the [InputStream].
 * @throws [java.io.IOException] if gzip-format bytes cannot be read from [input].
 * @throws [IllegalArgumentException] [input] does not provide a valid
 *   `ink.proto.CodedStrokeInputBatch` proto message, or the corresponding [StrokeInputBatch] is
 *   invalid.
 */
@Throws(IOException::class)
public fun StrokeInputBatch.Companion.decode(input: InputStream): ImmutableStrokeInputBatch =
    decodeUncompressed(DecompressedBytes(input))

// Using an explicit singleton object instead of @file:JvmName to put the static interface intended
// for use from Java in a class because otherwise there are multiple top-level functions with the
// same name and signature on the Kotlin side. If one of those were used from Kotlin, it chooses and
// overload arbitrarily, which leads to potentially very confusing behavior (e.g. decode might work
// by coincidence at one point and then suddenly stop working when more overloads are added).

public object StrokeInputBatchSerialization {
    /**
     * Write a gzip-compressed `ink.proto.CodedStrokeInputBatch` binary proto message representing
     * the [StrokeInputBatch] to the given [OutputStream]. Kotlin callers should use
     * [StrokeInputBatch.encode] instead.
     */
    @JvmStatic
    public fun encode(strokeInputBatch: StrokeInputBatch, output: OutputStream): Unit =
        strokeInputBatch.encode(output)

    /**
     * Write a gzip-compressed `ink.proto.CodedStrokeInputBatch` binary proto message representing
     * the [StrokeInputBatch] to a [ByteArray]. Kotlin callers should use [StrokeInputBatch.encode]
     * instead.
     */
    @JvmStatic
    public fun encode(strokeInputBatch: StrokeInputBatch): ByteArray = strokeInputBatch.encode()

    /**
     * Read a serialized gzip-compressed `ink.proto.CodedStrokeInputBatch` from the given
     * [InputStream] and parse it into a [ImmutableStrokeInputBatch], throwing an exception if
     * parsing or validation was not successful. Kotlin callers should use
     * [StrokeInputBatch.Companion.decode] instead.
     *
     * @param input [InputStream] providing gzip-compressed `ink.proto.CodedStrokeInputBatch` binary
     *   proto messages, the same as written to [OutputStream] by [encode].
     * @return The [ImmutableStrokeInputBatch] parsed from the [InputStream].
     * @throws [java.io.IOException] if gzip-format bytes cannot be read from [input].
     * @throws [IllegalArgumentException] [input] does not provide a valid
     *   `ink.proto.CodedStrokeInputBatch` proto message, or the corresponding [StrokeInputBatch] is
     *   invalid.
     */
    @JvmStatic
    @Throws(IOException::class)
    public fun decode(input: InputStream): ImmutableStrokeInputBatch =
        StrokeInputBatch.decode(input)

    /**
     * Read a serialized gzip-compressed `ink.proto.CodedStrokeInputBatch` from the given
     * [ByteArray] and parse it into a [ImmutableStrokeInputBatch], throwing an exception if parsing
     * or validation was not successful. Kotlin callers should use
     * [StrokeInputBatch.Companion.decode] instead.
     *
     * @param input [ByteArray] providing gzip-compressed `ink.proto.CodedStrokeInputBatch` binary
     *   proto messages, the same as written to [OutputStream] by [encode].
     * @return The [ImmutableStrokeInputBatch] parsed from the [ByteArray].
     * @throws [IllegalArgumentException] [input] does not provide a valid
     *   `ink.proto.CodedStrokeInputBatch` proto message, or the corresponding [StrokeInputBatch] is
     *   invalid.
     */
    @JvmStatic
    @Throws(IOException::class)
    public fun decode(input: ByteArray): ImmutableStrokeInputBatch = StrokeInputBatch.decode(input)
}

@UsedByNative
actual internal object StrokeInputBatchSerializationNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative actual external fun createFromProto(decompressedBytes: ByteArray, size: Int): Long

    @UsedByNative actual external fun encode(nativeStrokeInputBatchPointer: Long): ByteArray
}
