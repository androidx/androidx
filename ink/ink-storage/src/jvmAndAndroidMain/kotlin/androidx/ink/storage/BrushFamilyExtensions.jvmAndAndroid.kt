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
@file:JvmMultifileClass

package androidx.ink.storage

import androidx.annotation.RestrictTo
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.Version
import androidx.ink.nativeloader.NativeLoader
import androidx.ink.nativeloader.UsedByNative
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.GZIPOutputStream

/**
 * Write a gzip-compressed serialized `ink.proto.BrushFamily` proto message representing the
 * [BrushFamily] to the given [OutputStream]. If [BrushFamily.hasFallbacks] is true, then the stored
 * proto message including fallbacks for this [BrushFamily] will be used instead of recomputing the
 * proto from the [BrushFamily] object.
 */
public fun BrushFamily.encode(
    output: OutputStream,
    textureIdToPngBytes: TexturePngBytesLookup? = null,
) {
    GZIPOutputStream(output).use { it.write(encodeUncompressed(textureIdToPngBytes)) }
}

/**
 * Read a serialized [BrushFamily] from the given [InputStream] and parse it into a [BrushFamily],
 * throwing an exception if parsing or validation was not successful. Java callers should use
 * [BrushFamilySerialization.decode] instead.
 *
 * @param input [InputStream] providing gzip-compressed `ink.proto.BrushFamily` binary proto
 *   messages, the same as written by [encode].
 * @param maxVersion The maximum [Version] supported by the deserializer. Proto objects with a
 *   `min_version` of greater than maxVersion will be rejected.
 * @param onDecodeTexture A callback to store any decoded texture image, if any were encoded inside
 *   the serialized [BrushFamily]. This is called synchronously as part of this function call, on
 *   the same thread, once for each texture image.
 * @return The [BrushFamily] parsed from the [InputStream].
 * @throws [java.io.IOException] if gzip-format bytes cannot be read from [input].
 * @throws [IllegalArgumentException] [input] does not provide a valid `ink.proto.BrushFamily` proto
 *   message, or the corresponding [BrushFamily] is invalid.
 */
@Throws(IOException::class)
public fun BrushFamily.Companion.decode(
    input: InputStream,
    maxVersion: Version = Version.MAX_SUPPORTED,
    onDecodeTexture: OnDecodeTexturePngBytes? = null,
): BrushFamily = decodeUncompressed(DecompressedBytes(input), maxVersion, onDecodeTexture)

/**
 * Write a gzip-compressed serialized `ink.proto.BrushFamily` proto message representing the [List]
 * of [BrushFamily]s to the given [OutputStream].
 *
 * All [BrushFamily] objects in this [List] are encoded into a single BrushFamily proto object. The
 * order of the [BrushFamily]s in the [List] passed is irrelevant, as they will be sorted by version
 * compatibility prior to encoding. The proto is encoded such that the top-level is the
 * lowest-version compatible [BrushFamily], making the proto backwards compatible with older
 * versions of Ink which have no concept of nested brush families. Creates new fallbacks from the
 * [BrushFamily] objects passed in, overriding any existing fallbacks on any individual
 * [BrushFamily].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
public fun List<BrushFamily>.encodeMultiple(
    output: OutputStream,
    textureIdToPngBytes: TexturePngBytesLookup? = null,
): Unit = GZIPOutputStream(output).use { it.write(encodeMultipleUncompressed(textureIdToPngBytes)) }

/**
 * Read a serialized [BrushFamily] from the given [InputStream] and parse it into a [List] of
 * [BrushFamily]s, throwing an exception if parsing or validation was not successful. Java callers
 * should use [BrushFamilySerialization.decodeMultiple] instead.
 *
 * @param input [InputStream] providing gzip-compressed `ink.proto.BrushFamily` binary proto
 *   messages, the same as written by [encodeMultiple].
 * @param maxVersion The maximum [Version] supported by the deserializer. If any of the serialized
 *   [BrushFamily]s have a `min_version` of greater than maxVersion, deserialization will fail.
 * @param onDecodeTexture A callback to store any decoded texture image, if any were encoded inside
 *   the serialized [BrushFamily]. This is called synchronously as part of this function call, on
 *   the same thread, once for each texture image.
 * @return The [List] of [BrushFamily]s parsed from the [InputStream].
 * @throws [java.io.IOException] if gzip-format bytes cannot be read from [input].
 * @throws [IllegalArgumentException] [input] does not provide a valid `ink.proto.BrushFamily` proto
 *   message, or any of the corresponding [BrushFamily]s are invalid.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
@Throws(IOException::class)
public fun BrushFamily.Companion.decodeMultiple(
    input: InputStream,
    maxVersion: Version = Version.MAX_SUPPORTED,
    onDecodeTexture: OnDecodeTexturePngBytes? = null,
): List<BrushFamily> =
    decodeMultipleUncompressed(DecompressedBytes(input), maxVersion, onDecodeTexture)

// Using an explicit singleton object instead of @file:JvmName to put the static interface intended
// for use from Java in a class because otherwise there are multiple top-level functions with the
// same name and signature on the Kotlin side. If one of those were used from Kotlin, it chooses and
// overload arbitrarily, which leads to potentially very confusing behavior (e.g. decode might
// work by coincidence at one point and then suddenly stop working when more overloads are added).

public object BrushFamilySerialization {
    /**
     * Write a gzip-compressed serialized `ink.proto.BrushFamily` proto message representing the
     * [BrushFamily] to the given [OutputStream]. If [BrushFamily.hasFallbacks] is true, then the
     * stored proto message including fallbacks for this [BrushFamily] will be used instead of
     * recomputing the proto from the [BrushFamily] object.
     */
    @JvmStatic
    @JvmOverloads
    @Throws(IOException::class)
    public fun encode(
        brushFamily: BrushFamily,
        output: OutputStream,
        textureIdToPngBytes: TexturePngBytesLookup? = null,
    ): Unit = brushFamily.encode(output, textureIdToPngBytes)

    /**
     * Write a gzip-compressed serialized `ink.proto.BrushFamily` proto message representing the
     * [BrushFamily] to a [ByteArray]. If [BrushFamily.hasFallbacks] is true, then the stored proto
     * message including fallbacks for this [BrushFamily] will be used instead of recomputing the
     * proto from the [BrushFamily] object.
     */
    @JvmStatic
    @JvmOverloads
    @Throws(IOException::class)
    public fun encode(
        brushFamily: BrushFamily,
        textureIdToPngBytes: TexturePngBytesLookup? = null,
    ): ByteArray = brushFamily.encode(textureIdToPngBytes)

    /**
     * Read a serialized [BrushFamily] from the given [InputStream] and parse it into a
     * [BrushFamily], throwing an exception if parsing or validation was not successful. Kotlin
     * callers should use [BrushFamily.Companion.decode] instead.
     *
     * @param input [InputStream] providing gzip-compressed `ink.proto.BrushFamily` binary proto
     *   messages, the same as written by [encode].
     * @param maxVersion The maximum [Version] supported by the deserializer. Proto objects with a
     *   `min_version` of greater than maxVersion will be rejected.
     * @param onDecodeTexture A callback to store any decoded texture image, if any were encoded
     *   inside the serialized [BrushFamily]s. This is called synchronously as part of this function
     *   call, on the same thread, once for each texture image.
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
        onDecodeTexture: OnDecodeTexturePngBytes? = null,
    ): BrushFamily = BrushFamily.decode(input, maxVersion, onDecodeTexture)

    /** See [decode] above. This overload uses [Version.MAX_SUPPORTED]. */
    @JvmStatic
    @Throws(IOException::class)
    public fun decode(input: InputStream, onDecodeTexture: OnDecodeTexturePngBytes): BrushFamily =
        decode(input, Version.MAX_SUPPORTED, onDecodeTexture)

    /**
     * Read a serialized [BrushFamily] from the given [ByteArray] and parse it into a [BrushFamily],
     * throwing an exception if parsing or validation was not successful. Kotlin callers should use
     * [BrushFamily.Companion.decode] instead.
     *
     * @param input [ByteArray] providing gzip-compressed `ink.proto.BrushFamily` binary proto
     *   messages, the same as written by [encode].
     * @param maxVersion The maximum [Version] supported by the deserializer. Proto objects with a
     *   `min_version` of greater than maxVersion will be rejected.
     * @param onDecodeTexture A callback to store any decoded texture image, if any were encoded
     *   inside the serialized [BrushFamily]s. This is called synchronously as part of this function
     *   call, on the same thread, once for each texture image.
     * @return The [BrushFamily] parsed from the [ByteArray].
     * @throws [IllegalArgumentException] [input] does not provide a valid `ink.proto.BrushFamily`
     *   proto message, or the corresponding [BrushFamily] is invalid.
     */
    @JvmStatic
    @JvmOverloads
    @Throws(IOException::class)
    public fun decode(
        input: ByteArray,
        maxVersion: Version = Version.MAX_SUPPORTED,
        onDecodeTexture: OnDecodeTexturePngBytes? = null,
    ): BrushFamily = BrushFamily.decode(input, maxVersion, onDecodeTexture)

    /** See [decode] above. This overload uses [Version.MAX_SUPPORTED]. */
    @JvmStatic
    @Throws(IOException::class)
    public fun decode(input: ByteArray, onDecodeTexture: OnDecodeTexturePngBytes): BrushFamily =
        decode(input, Version.MAX_SUPPORTED, onDecodeTexture)

    /**
     * Write a gzip-compressed serialized `ink.proto.BrushFamily` proto message representing the
     * [List] of [BrushFamily]s to the given [OutputStream].
     *
     * All [BrushFamily] objects in this [List] are encoded into a single BrushFamily proto object.
     * The order of the [BrushFamily]s in the [List] passed is irrelevant, as they will be sorted
     * prior to encoding by version compatibility. The proto is encoded such that the top-level is
     * the lowest-version compatible [BrushFamily], making the proto backwards compatible with older
     * versions of Ink which have no concept of nested brush families. Creates new fallbacks from
     * the [BrushFamily] objects passed in, overriding any existing fallbacks on any individual
     * [BrushFamily].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
    @JvmStatic
    @JvmOverloads
    @Throws(IOException::class)
    public fun encodeMultiple(
        brushFamilies: List<BrushFamily>,
        output: OutputStream,
        textureIdToPngBytes: TexturePngBytesLookup? = null,
    ): Unit = brushFamilies.encodeMultiple(output, textureIdToPngBytes)

    /**
     * Write a gzip-compressed serialized `ink.proto.BrushFamily` proto message representing the
     * [List] of [BrushFamily]s to a [ByteArray].
     *
     * All [BrushFamily] objects in this [List] are encoded into a single BrushFamily proto object.
     * The order of the [BrushFamily]s in the [List] passed is irrelevant, as they will be sorted
     * prior to encoding by version compatibility. The proto is encoded such that the top-level is
     * the lowest-version compatible [BrushFamily], making the proto backwards compatible with older
     * versions of Ink which have no concept of nested brush families. Creates new fallbacks from
     * the [BrushFamily] objects passed in, overriding any existing fallbacks on any individual
     * [BrushFamily].
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
    @JvmStatic
    @JvmOverloads
    @Throws(IOException::class)
    public fun encodeMultiple(
        brushFamilies: List<BrushFamily>,
        textureIdToPngBytes: TexturePngBytesLookup? = null,
    ): ByteArray = brushFamilies.encodeMultiple(textureIdToPngBytes)

    /**
     * Read a serialized [BrushFamily] from the given [InputStream] and parse it into a [List] of
     * [BrushFamily]s, throwing an exception if parsing or validation was not successful. Kotlin
     * callers should use [BrushFamily.Companion.decodeMultiple] instead.
     *
     * @param input [InputStream] providing gzip-compressed `ink.proto.BrushFamily` binary proto
     *   messages, the same as written by [encodeMultiple].
     * @param maxVersion The maximum [Version] supported by the deserializer. If any of the
     *   serialized [BrushFamily]s have a `min_version` of greater than maxVersion, deserialization
     *   will fail.
     * @param onDecodeTexture A callback to store any decoded texture image, if any were encoded
     *   inside the serialized [BrushFamily]s. This is called synchronously as part of this function
     *   call, on the same thread, once for each texture image.
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
        onDecodeTexture: OnDecodeTexturePngBytes? = null,
    ): List<BrushFamily> = BrushFamily.decodeMultiple(input, maxVersion, onDecodeTexture)

    /** See [decodeMultiple] above. This overload uses [Version.MAX_SUPPORTED]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
    @JvmStatic
    @Throws(IOException::class)
    public fun decodeMultiple(
        input: InputStream,
        onDecodeTexture: OnDecodeTexturePngBytes,
    ): List<BrushFamily> = decodeMultiple(input, Version.MAX_SUPPORTED, onDecodeTexture)

    /**
     * Read a serialized [BrushFamily] from the given [InputStream] and parse it into a [List] of
     * [BrushFamily]s, throwing an exception if parsing or validation was not successful. Kotlin
     * callers should use [BrushFamily.Companion.decodeMultiple] instead.
     *
     * @param input [ByteArray] providing gzip-compressed `ink.proto.BrushFamily` binary proto
     *   messages, the same as written by [encodeMultiple].
     * @param maxVersion The maximum [Version] supported by the deserializer. If any of the
     *   serialized [BrushFamily]s have a `min_version` of greater than maxVersion, deserialization
     *   will fail.
     * @param onDecodeTexture A callback to store any decoded texture image, if any were encoded
     *   inside the serialized [BrushFamily]s. This is called synchronously as part of this function
     *   call, on the same thread, once for each texture image.
     * @return The [List] of [BrushFamily]s parsed from the [ByteArray].
     * @throws [java.io.IOException] if gzip-format bytes cannot be read from [input].
     * @throws [IllegalArgumentException] [input] does not provide a valid `ink.proto.BrushFamily`
     *   proto message, or any of the corresponding [BrushFamily]s are invalid.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
    @JvmStatic
    @JvmOverloads
    @Throws(IOException::class)
    public fun decodeMultiple(
        input: ByteArray,
        maxVersion: Version = Version.MAX_SUPPORTED,
        onDecodeTexture: OnDecodeTexturePngBytes? = null,
    ): List<BrushFamily> = BrushFamily.decodeMultiple(input, maxVersion, onDecodeTexture)

    /** See [decodeMultiple] above. This overload uses [Version.MAX_SUPPORTED]. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
    @JvmStatic
    @Throws(IOException::class)
    public fun decodeMultiple(
        input: ByteArray,
        onDecodeTexture: OnDecodeTexturePngBytes,
    ): List<BrushFamily> = decodeMultiple(input, Version.MAX_SUPPORTED, onDecodeTexture)
}

@UsedByNative
actual internal object BrushFamilySerializationNative {
    init {
        NativeLoader.load()
    }

    /**
     * Serializes a [BrushFamily] to a [ByteArray] using the provided texture map represented in
     * corresponding arrays of keys (client texture IDs) and values (PNG bytes). If
     * [BrushFamily.hasFallbacks] is true, then the stored proto message including fallbacks for
     * this [BrushFamily] will be used instead of recomputing the proto from the [BrushFamily]
     * object.
     */
    actual fun encode(
        nativeBrushFamilyPointer: Long,
        textureMap: Map<String, ByteArray>?,
    ): ByteArray =
        encode(
            nativeBrushFamilyPointer,
            textureMap?.keys?.toTypedArray(),
            // Kotlin doesn't guarantee traversal order of Map.keys is the same as Map.values.
            textureMap?.keys?.map { textureMap[it]!! }?.toTypedArray(),
        )

    @UsedByNative
    private external fun encode(
        nativeBrushFamilyPointer: Long,
        textureMapKeys: Array<String>?,
        textureMapValues: Array<ByteArray>?,
    ): ByteArray

    actual fun encodeMultiple(
        nativeBrushFamilyPointers: LongArray,
        textureMap: Map<String, ByteArray>?,
    ): ByteArray =
        encodeMultiple(
            nativeBrushFamilyPointers,
            textureMap?.keys?.toTypedArray(),
            // Kotlin doesn't guarantee traversal order of Map.keys is the same as Map.values.
            textureMap?.keys?.map { textureMap[it]!! }?.toTypedArray(),
        )

    @UsedByNative
    private external fun encodeMultiple(
        nativeBrushFamilyPointers: LongArray,
        textureMapKeys: Array<String>?,
        textureMapValues: Array<ByteArray>?,
    ): ByteArray

    /**
     * Constructs an unowned heap-allocated native [BrushFamily] from a serialized proto, passed in
     * as a [ByteArray]. [onDecodeTexture] is called for each client texture ID in the `BrushFamily`
     * proto. `maxVersion` is used to determine the maximum version supported by the deserializer.
     * Proto objects with a `min_version` of greater than `maxVersion` will be rejected.
     */
    @UsedByNative
    actual external fun createFromProto(
        brushFamilyByteArray: ByteArray,
        length: Int,
        onDecodeTexture: OnDecodeTexturePngBytes?,
        maxVersion: Int,
    ): Long
}

@UsedByNative
actual internal object MultipleBrushFamiliesNative {
    init {
        NativeLoader.load()
    }

    /**
     * Returns a pointer to a heap-allocated `std::vector<std::unique_ptr<BrushFamily>>`, allowing
     * handoff to individual `BrushFamily` objects to be guarded by cleanup logic.
     */
    @UsedByNative
    actual external fun createFromProto(
        brushFamilyByteArray: ByteArray,
        length: Int,
        onDecodeTexture: OnDecodeTexturePngBytes?,
        maxVersion: Int,
    ): Long

    @UsedByNative actual external fun getBrushFamilyCount(nativePointer: Long): Int

    @UsedByNative actual external fun releaseBrushFamily(nativePointer: Long, index: Int): Long

    @UsedByNative actual external fun free(nativePointer: Long)
}

/*
 */
