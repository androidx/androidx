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
@file:JvmName("PartitionedMeshExtensions")

package androidx.ink.storage

import androidx.ink.geometry.PartitionedMesh
import androidx.ink.nativeloader.InkInternalOnlyApi
import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative
import androidx.ink.strokes.ExperimentalInkEraserApi
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.GZIPOutputStream

/**
 * Write a gzip-compressed `ink.proto.CodedModeledShape` binary proto message representing the
 * [PartitionedMesh] to the given [OutputStream].
 */
@ExperimentalInkEraserApi
@Throws(IOException::class)
public fun PartitionedMesh.encode(output: OutputStream) {
    GZIPOutputStream(output).use { it.write(encodeUncompressed()) }
}

/**
 * Read a serialized gzip-compressed `ink.proto.CodedModeledShape` from the given [InputStream] and
 * parse it into a [PartitionedMesh], throwing an exception if parsing or validation was not
 * successful. Java callers should use [PartitionedMeshSerialization.decode] instead.
 *
 * @param input [InputStream] providing gzip-compressed `ink.proto.CodedModeledShape` binary proto
 *   messages, the same as written by [encode].
 * @return The [PartitionedMesh] parsed from the [InputStream].
 * @throws [java.io.IOException] if gzip-format bytes cannot be read from [input].
 * @throws [IllegalArgumentException] [input] does not provide a valid `ink.proto.CodedModeledShape`
 *   proto message, or the corresponding [PartitionedMesh] is invalid.
 */
@ExperimentalInkEraserApi
@Throws(IOException::class)
public fun PartitionedMesh.Companion.decode(input: InputStream): PartitionedMesh =
    decodeUncompressed(DecompressedBytes(input))

// Using an explicit singleton object instead of @file:JvmName to put the static interface intended
// for use from Java in a class because otherwise there are multiple top-level functions with the
// same name and signature on the Kotlin side. If one of those were used from Kotlin, it chooses an
// overload arbitrarily, which leads to potentially very confusing behavior (e.g. decode might work
// by coincidence at one point and then suddenly stop working when more overloads are added).

@ExperimentalInkEraserApi
public object PartitionedMeshSerialization {
    /**
     * Write a gzip-compressed `ink.proto.CodedModeledShape` binary proto message representing the
     * [PartitionedMesh] to the given [OutputStream]. Kotlin callers should use
     * [PartitionedMesh.encode] instead.
     */
    @JvmStatic
    @Throws(IOException::class)
    public fun encode(partitionedMesh: PartitionedMesh, output: OutputStream) {
        partitionedMesh.encode(output)
    }

    /**
     * Write a gzip-compressed `ink.proto.CodedModeledShape` binary proto message representing the
     * [PartitionedMesh] to a [ByteArray]. Kotlin callers should use [PartitionedMesh.encode]
     * instead.
     */
    @JvmStatic
    public fun encode(partitionedMesh: PartitionedMesh): ByteArray = partitionedMesh.encode()

    /**
     * Read a serialized gzip-compressed `ink.proto.CodedModeledShape` from the given [InputStream]
     * and parse it into a [PartitionedMesh], throwing an exception if parsing or validation was not
     * successful. Kotlin callers should use [PartitionedMesh.Companion.decode] instead.
     *
     * @param input [InputStream] providing gzip-compressed `ink.proto.CodedModeledShape` binary
     *   proto messages, the same as written to [OutputStream] by [encode].
     * @return The [PartitionedMesh] parsed from the [InputStream].
     * @throws [java.io.IOException] if gzip-format bytes cannot be read from [input].
     * @throws [IllegalArgumentException] [input] does not provide a valid
     *   `ink.proto.CodedModeledShape` proto message, or the corresponding [PartitionedMesh] is
     *   invalid.
     */
    @JvmStatic
    @Throws(IOException::class)
    public fun decode(input: InputStream): PartitionedMesh = PartitionedMesh.decode(input)

    /**
     * Read a serialized gzip-compressed `ink.proto.CodedModeledShape` from the given [ByteArray]
     * and parse it into a [PartitionedMesh], throwing an exception if parsing or validation was not
     * successful. Kotlin callers should use [PartitionedMesh.Companion.decode] instead.
     *
     * @param input [ByteArray] providing gzip-compressed `ink.proto.CodedModeledShape` binary proto
     *   messages, the same as written by [encode].
     * @return The [PartitionedMesh] parsed from the [ByteArray].
     * @throws [java.io.IOException] if gzip-format bytes cannot be read from [input].
     * @throws [IllegalArgumentException] [input] does not provide a valid
     *   `ink.proto.CodedModeledShape` proto message, or the corresponding [PartitionedMesh] is
     *   invalid.
     */
    @JvmStatic
    @Throws(IOException::class)
    public fun decode(input: ByteArray): PartitionedMesh = PartitionedMesh.decode(input)
}

@UsedByNative
@OptIn(InkInternalOnlyApi::class)
actual internal object PartitionedMeshSerializationNative {
    init {
        NativeLoader.load()
    }

    @UsedByNative actual external fun createFromProto(decompressedBytes: ByteArray, size: Int): Long

    @UsedByNative actual external fun encode(nativePartitionedMeshPointer: Long): ByteArray
}
