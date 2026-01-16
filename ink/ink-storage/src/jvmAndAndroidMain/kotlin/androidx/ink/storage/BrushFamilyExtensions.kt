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

@file:JvmName("BrushFamilyExtensions")

package androidx.ink.storage

import androidx.ink.brush.BrushFamily
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.GZIPOutputStream

/**
 * Write a gzip-compressed serialized `ink.proto.BrushFamily` proto message representing the
 * [BrushFamily] to the given [OutputStream].
 */
@ExperimentalInkCustomBrushApi
public fun BrushFamily.encode(output: OutputStream) {
    GZIPOutputStream(output).use {
        it.write(BrushSerializationNative.serializeBrushFamily(nativePointer, mapOf()))
    }
}

/**
 * Read a serialized [BrushFamily] from the given [InputStream] and parse it into a [BrushFamily],
 * throwing an exception if parsing or validation was not successful. Java callers should use
 * [BrushFamilySerialization.decode] instead.
 *
 * @param input [InputStream] providing gzip-compressed `ink.proto.BrushFamily` binary proto
 *   messages, the same as written to [OutputStream] by [encode].
 * @return The [BrushFamily] parsed from the [InputStream].
 * @throws [java.io.IOException] if gzip-format bytes cannot be read from [input].
 * @throws [IllegalArgumentException] [input] does not provide a valid `ink.proto.BrushFamily` proto
 *   message, or the corresponding [BrushFamily] is invalid.
 */
@ExperimentalInkCustomBrushApi
public fun BrushFamily.Companion.decode(input: InputStream): BrushFamily {
    val decompressed = DecompressedBytes(input)
    val nativePointer =
        BrushSerializationNative.newBrushFamilyFromProto(
            brushFamilyDirectByteBuffer = null,
            brushFamilyByteArray = decompressed.bytes,
            offset = 0,
            length = decompressed.size,
        )
    check(nativePointer != 0L) { "Should have thrown exception if decoding failed." }
    return BrushFamily.wrapNative(nativePointer)
}

// Using an explicit singleton object instead of @file:JvmName to put the static interface intended
// for use from Java in a class because otherwise there are multiple top-level functions with the
// same name and signature on the Kotlin side. If one of those were used from Kotlin, it chooses and
// overload arbitrarily, which leads to potentially very confusing behavior (e.g. decode might
// work by coincidence at one point and then suddenly stop working when more overloads are added).

@ExperimentalInkCustomBrushApi
public object BrushFamilySerialization {
    /**
     * Write a gzip-compressed serialized `ink.proto.BrushFamily` proto message representing the
     * [BrushFamily] to the given [OutputStream].
     */
    @JvmStatic
    public fun encode(brushFamily: BrushFamily, output: OutputStream): Unit =
        brushFamily.encode(output)

    /**
     * Read a serialized [BrushFamily] from the given [InputStream] and parse it into a
     * [BrushFamily], throwing an exception if parsing or validation was not successful. Kotlin
     * callers should use [BrushFamily.Companion.decode] instead.
     *
     * @param input [InputStream] providing gzip-compressed `ink.proto.BrushFamily` binary proto
     *   messages, the same as written to [OutputStream] by [encode].
     * @return The [BrushFamily] parsed from the [InputStream].
     * @throws [java.io.IOException] if gzip-format bytes cannot be read from [input].
     * @throws [IllegalArgumentException] [input] does not provide a valid `ink.proto.BrushFamily`
     *   proto message, or the corresponding [BrushFamily] is invalid.
     */
    @JvmStatic public fun decode(input: InputStream): BrushFamily = BrushFamily.decode(input)
}
