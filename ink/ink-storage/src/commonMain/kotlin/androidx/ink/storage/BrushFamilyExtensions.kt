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
import androidx.ink.brush.BrushPaint
import androidx.ink.brush.Version
import androidx.ink.nativeloader.NativePointer
import androidx.ink.nativeloader.UsedByNative
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/**
 * Write a gzip-compressed `ink.proto.BrushFamily` binary proto message representing the
 * [BrushFamily] to the given [ByteArray] using the provided texture map represented in
 * corresponding arrays of keys (client texture IDs) and values (PNG bytes). If
 * [BrushFamily.hasFallbacks] is true, then the stored proto message including fallbacks for this
 * [BrushFamily] will be used instead of recomputing the proto from the [BrushFamily] object.
 *
 * @param textureIdToPngBytes A callback to retrieve the PNG bytes of the texture bitmap for a given
 *   client texture ID.
 */
public fun BrushFamily.encode(textureIdToPngBytes: TexturePngBytesLookup? = null): ByteArray =
    encodeUncompressed(textureIdToPngBytes).compress()

/**
 * Write an uncompressed `ink.proto.BrushFamily` binary proto message representing the [BrushFamily]
 * to the given [ByteArray] using the provided texture map represented in corresponding arrays of
 * keys (client texture IDs) and values (PNG bytes).
 */
internal fun BrushFamily.encodeUncompressed(
    textureIdToPngBytes: TexturePngBytesLookup?
): ByteArray {
    var textureMap: MutableMap<String, ByteArray>? = null
    if (textureIdToPngBytes != null) {
        textureMap = mutableMapOf<String, ByteArray>()
        collectTextures(textureMap, textureIdToPngBytes)
    }
    return BrushFamilySerializationNative.encode(nativePointer, textureMap)
}

/**
 * Read a serialized [BrushFamily] from the given [ByteArray] and parse it into a [BrushFamily],
 * throwing an exception if parsing or validation was not successful.
 *
 * @param input [ByteArray] providing gzip-compressed `ink.proto.BrushFamily` binary proto messages,
 *   the same as written by [encode].
 * @param maxVersion The maximum [Version] supported by the deserializer. Proto objects with a
 *   `min_version` of greater than maxVersion will be rejected.
 * @param onDecodeTexture A callback to store any decoded texture image, if any were encoded inside
 *   the serialized [BrushFamily]. This is called synchronously as part of this function call, on
 *   the same thread, once for each texture image.
 * @return The [BrushFamily] parsed from the [ByteArray].
 * @throws [IllegalArgumentException] [input] does not provide a valid `ink.proto.BrushFamily` proto
 *   message, or the corresponding [BrushFamily] is invalid.
 * @throws [IOException] if gzip-format bytes cannot be read from [input].
 */
@Throws(IOException::class)
public fun BrushFamily.Companion.decode(
    input: ByteArray,
    maxVersion: Version = Version.MAX_SUPPORTED,
    onDecodeTexture: OnDecodeTexturePngBytes? = null,
): BrushFamily = decodeUncompressed(DecompressedBytes(input), maxVersion, onDecodeTexture)

/**
 * Write an uncompressed `ink.proto.BrushFamily` binary proto message representing the [BrushFamily]
 * to the given [ByteArray] using the provided texture map represented in corresponding arrays of
 * keys (client texture IDs) and values (PNG bytes).
 */
internal fun BrushFamily.Companion.decodeUncompressed(
    decompressed: DecompressedBytes,
    maxVersion: Version,
    onDecodeTexture: OnDecodeTexturePngBytes?,
): BrushFamily =
    BrushFamily.wrapNative {
        BrushFamilySerializationNative.createFromProto(
                brushFamilyByteArray = decompressed.buffer,
                length = decompressed.size,
                onDecodeTexture = onDecodeTexture,
                maxVersion = maxVersion.value,
            )
            .also { check(it != 0L) { "Should have thrown exception if decoding failed." } }
    }

/**
 * Write a gzip-compressed serialized `ink.proto.BrushFamily` proto message representing the [List]
 * of [BrushFamily]s to the given [ByteArray].
 *
 * All [BrushFamily] objects in this [List] are encoded into a single BrushFamily proto object. The
 * order of the [BrushFamily]s in the [List] passed is irrelevant, as they will be sorted by version
 * compatibility prior to encoding. The proto is encoded such that the top-level is the
 * lowest-version compatible [BrushFamily], making the proto backwards compatible with older
 * versions of Ink which have no concept of nested brush families. Creates new fallbacks from the
 * [BrushFamily] objects passed in, overriding any existing fallbacks on any individual
 * [BrushFamily].
 *
 * @param textureIdToPngBytes A callback to retrieve the PNG bytes of the texture bitmap for a given
 *   client texture ID.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
public fun List<BrushFamily>.encodeMultiple(
    textureIdToPngBytes: TexturePngBytesLookup? = null
): ByteArray {
    return encodeMultipleUncompressed(textureIdToPngBytes).compress()
}

/**
 * Write an uncompressed `ink.proto.BrushFamily` proto message representing the [List] of
 * [BrushFamily]s to the given [ByteArray] using the provided texture map represented in
 * corresponding arrays of keys (client texture IDs) and values (PNG bytes).
 */
internal fun List<BrushFamily>.encodeMultipleUncompressed(
    textureIdToPngBytes: TexturePngBytesLookup?
): ByteArray {
    var textureMap: MutableMap<String, ByteArray>? = null
    if (textureIdToPngBytes != null) {
        textureMap = mutableMapOf<String, ByteArray>()
        for (brushFamily in this) {
            brushFamily.collectTextures(textureMap, textureIdToPngBytes)
        }
    }
    return BrushFamilySerializationNative.encodeMultiple(
        LongArray(this.size) { index -> this[index].nativePointer },
        textureMap,
    )
}

/**
 * Read a serialized [BrushFamily] from the given [ByteArray] and parse it into a [List] of
 * [BrushFamily]s, throwing an exception if parsing or validation was not successful.
 *
 * @param input [ByteArray] providing gzip-compressed `ink.proto.BrushFamily` binary proto messages,
 *   the same as written by [encodeMultiple].
 * @param maxVersion The maximum [Version] supported by the deserializer. If any of the serialized
 *   [BrushFamily]s have a `min_version` of greater than maxVersion, deserialization will fail.
 * @param onDecodeTexture A callback to store any decoded texture image, if any were encoded inside
 *   the serialized [BrushFamily]. This is called synchronously as part of this function call, on
 *   the same thread, once for each texture image.
 * @return The [List] of [BrushFamily]s parsed from the [ByteArray].
 * @throws [IllegalArgumentException] [input] does not provide a valid `ink.proto.BrushFamily` proto
 *   message, or any of the corresponding [BrushFamily]s are invalid.
 * @throws [IOException] if gzip-format bytes cannot be read from [input].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
@Throws(IOException::class)
public fun BrushFamily.Companion.decodeMultiple(
    input: ByteArray,
    maxVersion: Version = Version.MAX_SUPPORTED,
    onDecodeTexture: OnDecodeTexturePngBytes? = null,
): List<BrushFamily> =
    decodeMultipleUncompressed(DecompressedBytes(input), maxVersion, onDecodeTexture)

internal fun decodeMultipleUncompressed(
    decompressed: DecompressedBytes,
    maxVersion: Version,
    onDecodeTexture: OnDecodeTexturePngBytes?,
): List<BrushFamily> =
    MultipleBrushFamilies.decode(
        brushFamilyByteArray = decompressed.buffer,
        length = decompressed.size,
        onDecodeTexture = onDecodeTexture,
        maxVersion = maxVersion.value,
    )

/** @see [OnDecodeTexturePngBytes.onDecodeTexture] */
public fun interface OnDecodeTexturePngBytes {
    /**
     * Low-level callback for recording serialized textures when a [BrushFamily] is decoded.
     *
     * @param clientTextureId The client-provided texture ID.
     * @param pngBytes The PNG bytes of the texture bitmap, or null if none was encoded.
     * @return The texture ID to use in the decoded BrushFamily.
     */
    @UsedByNative public fun onDecodeTexture(clientTextureId: String, pngBytes: ByteArray?): String
}

/** @see [TexturePngBytesLookup.getTexturePngBytes] */
public fun interface TexturePngBytesLookup {
    /**
     * Low-level callback for retrieving texture PNG bytes to encode into a serialized
     * [BrushFamily].
     *
     * @param clientTextureId The texture ID for the texture to encode.
     * @return The PNG bytes of the texture bitmap to encode, or null to not encode any texture.
     */
    public fun getTexturePngBytes(clientTextureId: String): ByteArray?
}

private fun BrushFamily.collectTextures(
    textureMap: MutableMap<String, ByteArray>,
    textureIdToPngBytes: TexturePngBytesLookup,
) {
    for (coat in coats) {
        for (paint in coat.paintPreferences) {
            for (layer in paint.textureLayers) {
                val textureId =
                    when (layer) {
                        is BrushPaint.StampingTexture -> layer.clientTextureId
                        is BrushPaint.TilingTexture -> layer.clientTextureId
                        else -> continue
                    }
                if (textureId.isEmpty()) continue
                if (textureMap.containsKey(textureId)) continue
                textureMap[textureId] =
                    textureIdToPngBytes.getTexturePngBytes(textureId) ?: continue
            }
        }
    }
}

expect internal object BrushFamilySerializationNative {

    /**
     * Serializes a [BrushFamily] to a [ByteArray] using the provided texture map represented in
     * corresponding arrays of keys (client texture IDs) and values (PNG bytes).
     */
    fun encode(nativeBrushFamilyPointer: Long, textureMap: Map<String, ByteArray>?): ByteArray

    fun encodeMultiple(
        nativeBrushFamilyPointers: LongArray,
        textureMap: Map<String, ByteArray>?,
    ): ByteArray

    /**
     * Constructs an unowned heap-allocated native [BrushFamily] from a serialized proto, passed in
     * as a [ByteArray]. [onDecodeTexture] is called for each client texture ID in the `BrushFamily`
     * proto. `maxVersion` is used to determine the maximum version supported by the deserializer.
     * Proto objects with a `min_version` of greater than `maxVersion` will be rejected.
     */
    fun createFromProto(
        brushFamilyByteArray: ByteArray,
        length: Int,
        onDecodeTexture: OnDecodeTexturePngBytes?,
        maxVersion: Int,
    ): Long
}

internal class MultipleBrushFamilies private constructor(nativeAlloc: () -> Long) {
    private val nativePointer: Long by NativePointer(nativeAlloc, MultipleBrushFamiliesNative::free)

    /**
     * Returns a list of the decoded brush families the first time it is called, can only be called
     * once.
     */
    internal fun releaseBrushFamilies(): List<BrushFamily> =
        List(MultipleBrushFamiliesNative.getBrushFamilyCount(nativePointer)) { index ->
            BrushFamily.wrapNative {
                MultipleBrushFamiliesNative.releaseBrushFamily(nativePointer, index).also {
                    check(it != 0L) { "releaseBrushFamilies can only be called once." }
                }
            }
        }

    companion object {
        fun decode(
            brushFamilyByteArray: ByteArray,
            length: Int,
            onDecodeTexture: OnDecodeTexturePngBytes?,
            maxVersion: Int,
        ): List<BrushFamily> =
            MultipleBrushFamilies {
                    MultipleBrushFamiliesNative.createFromProto(
                            brushFamilyByteArray,
                            length,
                            onDecodeTexture,
                            maxVersion,
                        )
                        .also {
                            check(it != 0L) { "Should have thrown exception if decoding failed." }
                        }
                }
                .releaseBrushFamilies()
    }
}

expect internal object MultipleBrushFamiliesNative {

    /**
     * Returns a pointer to a heap-allocated `std::vector<std::unique_ptr<BrushFamily>>`, allowing
     * handoff to individual `BrushFamily` objects to be guarded by cleanup logic.
     */
    fun createFromProto(
        brushFamilyByteArray: ByteArray,
        length: Int,
        onDecodeTexture: OnDecodeTexturePngBytes?,
        maxVersion: Int,
    ): Long

    fun getBrushFamilyCount(nativePointer: Long): Int

    fun releaseBrushFamily(nativePointer: Long, index: Int): Long

    fun free(nativePointer: Long)
}
