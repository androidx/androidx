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

import androidx.annotation.RestrictTo
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.Version
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.GZIPOutputStream

/**
 * Write a gzip-compressed serialized `ink.proto.BrushFamily` proto message representing the
 * [BrushFamily] to the given [OutputStream]. If `hasFallbacks` is true, then the stored, proto
 * message including fallbacks for this [BrushFamily] will be used instead of recomputing the proto
 * from the [BrushFamily] object.
 */
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
 * @param maxVersion The maximum [Version] supported by the deserializer. Proto objects with a
 *   `min_version` of greater than maxVersion will be rejected.
 * @return The [BrushFamily] parsed from the [InputStream].
 * @throws [java.io.IOException] if gzip-format bytes cannot be read from [input].
 * @throws [IllegalArgumentException] [input] does not provide a valid `ink.proto.BrushFamily` proto
 *   message, or the corresponding [BrushFamily] is invalid.
 */
public fun BrushFamily.Companion.decode(
    input: InputStream,
    maxVersion: Version = Version.MAX_SUPPORTED,
): BrushFamily {
    val decompressed = DecompressedBytes(input)
    val nativePointer =
        BrushSerializationNative.newBrushFamilyFromProto(
            brushFamilyDirectByteBuffer = null,
            brushFamilyByteArray = decompressed.bytes,
            offset = 0,
            length = decompressed.size,
            maxVersion = maxVersion.value,
        )
    check(nativePointer != 0L) { "Should have thrown exception if decoding failed." }
    return BrushFamily.wrapNative(nativePointer)
}

/**
 * Write a gzip-compressed serialized `ink.proto.BrushFamily` proto message representing the [List]
 * of [BrushFamily]s to the given [OutputStream].
 *
 * All [BrushFamily] objects in this [List] are encoded into a single BrushFamily proto object. At
 * the top-level is the lowest-version compatible [BrushFamily], making the proto backwards
 * compatible with older versions of Ink which have no concept of nested brush families. Creates new
 * fallbacks from the [BrushFamily] objects passed in, overriding any existing fallbacks on any
 * individual [BrushFamily].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
public fun List<BrushFamily>.encodeMultiple(output: OutputStream) {
    GZIPOutputStream(output).use {
        it.write(
            BrushSerializationNative.serializeMultipleBrushFamilies(
                LongArray(this.size) { index -> this[index].nativePointer },
                mapOf(),
            )
        )
    }
}

/**
 * Read a serialized [BrushFamily] from the given [InputStream] and parse it into a [List] of
 * [BrushFamily]s, throwing an exception if parsing or validation was not successful. Java callers
 * should use [BrushFamilySerialization.decodeMultiple] instead.
 *
 * @param input [InputStream] providing gzip-compressed `ink.proto.BrushFamily` binary proto
 *   messages, the same as written to [OutputStream] by [encodeMultiple].
 * @param maxVersion The maximum [Version] supported by the deserializer. If any of the serialized
 *   [BrushFamily]s have a `min_version` of greater than maxVersion, deserialization will fail.
 * @return The [List] of [BrushFamily]s parsed from the [InputStream].
 * @throws [java.io.IOException] if gzip-format bytes cannot be read from [input].
 * @throws [IllegalArgumentException] [input] does not provide a valid `ink.proto.BrushFamily` proto
 *   message, or any of the corresponding [BrushFamily]s are invalid.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
public fun BrushFamily.Companion.decodeMultiple(
    input: InputStream,
    maxVersion: Version = Version.MAX_SUPPORTED,
): List<BrushFamily> {
    val decompressed = DecompressedBytes(input)
    val nativePointers =
        BrushSerializationNative.newMultipleBrushFamiliesFromProto(
            brushFamilyDirectByteBuffer = null,
            brushFamilyByteArray = decompressed.bytes,
            offset = 0,
            length = decompressed.size,
            maxVersion = maxVersion.value,
        )
    return nativePointers.map { BrushFamily.wrapNative(it) }
}

// Using an explicit singleton object instead of @file:JvmName to put the static interface intended
// for use from Java in a class because otherwise there are multiple top-level functions with the
// same name and signature on the Kotlin side. If one of those were used from Kotlin, it chooses and
// overload arbitrarily, which leads to potentially very confusing behavior (e.g. decode might
// work by coincidence at one point and then suddenly stop working when more overloads are added).

public object BrushFamilySerialization {
    /**
     * Write a gzip-compressed serialized `ink.proto.BrushFamily` proto message representing the
     * [BrushFamily] to the given [OutputStream]. If `hasFallbacks` is true, then the stored proto
     * message including fallbacks for this [BrushFamily] will be used instead of recomputing the
     * proto from the [BrushFamily] object.
     */
    @JvmStatic
    @Throws(IOException::class)
    public fun encode(brushFamily: BrushFamily, output: OutputStream): Unit =
        brushFamily.encode(output)

    /**
     * Read a serialized [BrushFamily] from the given [InputStream] and parse it into a
     * [BrushFamily], throwing an exception if parsing or validation was not successful. Kotlin
     * callers should use [BrushFamily.Companion.decode] instead.
     *
     * @param input [InputStream] providing gzip-compressed `ink.proto.BrushFamily` binary proto
     *   messages, the same as written to [OutputStream] by [encode].
     * @param maxVersion The maximum [Version] supported by the deserializer. Proto objects with a
     *   `min_version` of greater than maxVersion will be rejected.
     * @return The [BrushFamily] parsed from the [InputStream].
     * @throws [java.io.IOException] if gzip-format bytes cannot be read from [input].
     * @throws [IllegalArgumentException] [input] does not provide a valid `ink.proto.BrushFamily`
     *   proto message, or the corresponding [BrushFamily] is invalid.
     */
    @JvmStatic
    @JvmOverloads
    @Throws(IOException::class)
    public fun decode(
        input: InputStream,
        maxVersion: Version = Version.MAX_SUPPORTED,
    ): BrushFamily = BrushFamily.decode(input, maxVersion)

    /**
     * Write a gzip-compressed serialized `ink.proto.BrushFamily` proto message representing the
     * [List] of [BrushFamily]s to the given [OutputStream].
     *
     * All [BrushFamily] objects in this [List] are encoded into a single BrushFamily proto object.
     * At the top-level is the lowest-version compatible [BrushFamily], making the proto backwards
     * compatible with older versions of Ink which have no concept of nested brush families. Creates
     * new fallbacks from the [BrushFamily] objects passed in, overriding any existing fallbacks on
     * any individual [BrushFamily].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
    @JvmStatic
    @Throws(IOException::class)
    public fun encodeMultiple(brushFamilies: List<BrushFamily>, output: OutputStream): Unit =
        brushFamilies.encodeMultiple(output)

    /**
     * Read a serialized [BrushFamily] from the given [InputStream] and parse it into a [List] of
     * [BrushFamily]s, throwing an exception if parsing or validation was not successful. Kotlin
     * callers should use [BrushFamily.Companion.decodeMultiple] instead.
     *
     * @param input [InputStream] providing gzip-compressed `ink.proto.BrushFamily` binary proto
     *   messages, the same as written to [OutputStream] by [encodeMultiple].
     * @param maxVersion The maximum [Version] supported by the deserializer. If any of the
     *   serialized [BrushFamily]s have a `min_version` of greater than maxVersion, deserialization
     *   will fail.
     * @return The [List] of [BrushFamily]s parsed from the [InputStream].
     * @throws [java.io.IOException] if gzip-format bytes cannot be read from [input].
     * @throws [IllegalArgumentException] [input] does not provide a valid `ink.proto.BrushFamily`
     *   proto message, or any of the corresponding [BrushFamily]s are invalid.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
    @JvmStatic
    @JvmOverloads
    @Throws(IOException::class)
    public fun decodeMultiple(
        input: InputStream,
        maxVersion: Version = Version.MAX_SUPPORTED,
    ): List<BrushFamily> = BrushFamily.decodeMultiple(input, maxVersion)
}
