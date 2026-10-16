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
@file:OptIn(InkInternalOnlyApi::class)

package androidx.ink.storage

import androidx.ink.geometry.PartitionedMesh
import androidx.ink.nativeloader.InkInternalOnlyApi
import androidx.ink.strokes.ExperimentalInkEraserApi
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/**
 * Write a gzip-compressed `ink.proto.CodedModeledShape` binary proto message representing the
 * [PartitionedMesh] to the returned [ByteArray].
 */
@ExperimentalInkEraserApi
public fun PartitionedMesh.encode(): ByteArray = encodeUncompressed().compress()

/**
 * Read a serialized gzip-compressed `ink.proto.CodedModeledShape` from the given [ByteArray] and
 * parse it into a [PartitionedMesh], throwing an exception if parsing or validation was not
 * successful.
 *
 * @param input [ByteArray] providing gzip-compressed `ink.proto.CodedModeledShape` binary proto
 *   messages, the same as written by [encode].
 * @return The [PartitionedMesh] parsed from the [ByteArray].
 * @throws [IOException] if gzip-format bytes cannot be read from [input].
 * @throws [IllegalArgumentException] [input] does not provide a valid `ink.proto.CodedModeledShape`
 *   proto message, or the corresponding [PartitionedMesh] is invalid.
 */
@ExperimentalInkEraserApi
@Throws(IOException::class)
public fun PartitionedMesh.Companion.decode(input: ByteArray): PartitionedMesh =
    decodeUncompressed(DecompressedBytes(input))

/**
 * Internal KMP-common helper for encoding a [PartitionedMesh] to a [ByteArray] before compression.
 */
internal fun PartitionedMesh.encodeUncompressed(): ByteArray =
    PartitionedMeshSerializationNative.encode(nativePointer)

/**
 * Internal KMP-common helper for decoding a [PartitionedMesh] from a [ByteArray] after
 * decompression. Takes a size argument so an extra copy is not required to resize the array down if
 * excess capacity was allocated during decompression.
 */
@Throws(IOException::class)
internal fun PartitionedMesh.Companion.decodeUncompressed(
    input: DecompressedBytes
): PartitionedMesh = PartitionedMesh.wrapNative {
    PartitionedMeshSerializationNative.createFromProto(input.buffer, input.size)
}

expect internal object PartitionedMeshSerializationNative {
    // Returns a native pointer to a `PartitionedMesh`.
    fun createFromProto(decompressedBytes: ByteArray, size: Int): Long

    fun encode(nativePartitionedMeshPointer: Long): ByteArray
}
