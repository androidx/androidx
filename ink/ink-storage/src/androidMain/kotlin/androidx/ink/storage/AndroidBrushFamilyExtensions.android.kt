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

@file:JvmName("AndroidBrushFamilyExtensions")

package androidx.ink.storage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.TextureBitmapStore
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.GZIPOutputStream

/** A callback to use with [decode] to manage texture image assets. */
@ExperimentalInkCustomBrushApi
public fun interface BrushFamilyDecodeCallback {
    /**
     * Called for each texture used by a BrushFamily when that BrushFamily is decoded. In the
     * implementation of this method, the returned String should be mapped to [bitmap] (or a client-
     * provided replacement) in the supporting [TextureBitmapStore].
     *
     * @param clientTextureId the ID for this texture in the serialized form of the BrushFamily.
     * @param bitmap the bitmap corresponding to clientTextureId in the serialized form of the
     *   BrushFamily. Null indicates that the serialized form did not store a bitmap for
     *   clientTextureId.
     * @return The client texture ID for this texture in the in-memory format of the [BrushFamily].
     *   This will typically match [clientTextureId], but can be different, for example when there
     *   are naming collisions in the [TextureBitmapStore] and one texture needs to be renamed.
     */
    public fun onDecodeTexture(clientTextureId: String, bitmap: Bitmap?): String
}

/**
 * Write a gzip-compressed `ink.proto.BrushFamily` binary proto message representing the
 * [BrushFamily] to the given [OutputStream].
 *
 * @param output The [OutputStream] to write the gzip-compressed encoded bytes to.
 * @param textureBitmapStore The [TextureBitmapStore] to use to encode the texture images within the
 *   encoded [BrushFamily]. If this is not desired behavior, e.g. if the application has a static
 *   set of texture images that it includes as resources, then this can be a [TextureBitmapStore]
 *   that always returns `null`.
 * @receiver The [BrushFamily] object to encode.
 */
@ExperimentalInkCustomBrushApi
public fun BrushFamily.encode(output: OutputStream, textureBitmapStore: TextureBitmapStore) {
    val textureIdToNativeBitmaps: MutableMap<String, ByteArray> = mutableMapOf()

    for (coat in coats) {
        for (layer in coat.paint.textureLayers) {
            if (textureIdToNativeBitmaps.containsKey(layer.clientTextureId)) continue

            val bitmap = textureBitmapStore[layer.clientTextureId] ?: continue

            val pngBytes =
                ByteArrayOutputStream().use { outputStream ->
                    // Encode bitmap as PNG bytes. PNG is lossless, so the quality value is ignored.
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    outputStream.toByteArray()
                }
            textureIdToNativeBitmaps[layer.clientTextureId] = pngBytes
        }
    }
    GZIPOutputStream(output).use {
        it.write(
            BrushSerializationNative.serializeBrushFamily(nativePointer, textureIdToNativeBitmaps)
        )
    }
}

/**
 * Read a serialized [BrushFamily] from the given [InputStream] and parse it into a [BrushFamily],
 * throwing an exception if parsing or validation was not successful. Java callers should use
 * [AndroidBrushFamilySerialization.decode] instead.
 *
 * @param input [InputStream] providing gzip-compressed `ink.proto.BrushFamily` binary proto
 *   messages, the same as written to [OutputStream] by [encode].
 * @param getClientTextureId A callback to store the decoded texture image, if one were encoded
 *   inside the serialized [BrushFamily], into a [TextureBitmapStore]. This is called synchronously
 *   as part of this function call on the same thread.
 * @return The [BrushFamily] parsed from the [InputStream].
 * @throws [java.io.IOException] if gzip-format bytes cannot be read from [input].
 * @throws [IllegalArgumentException] [input] does not provide a valid `ink.proto.BrushFamily` proto
 *   message, or the corresponding [BrushFamily] is invalid.
 */
@SuppressWarnings("ExecutorRegistration")
@ExperimentalInkCustomBrushApi
public fun BrushFamily.Companion.decode(
    input: InputStream,
    getClientTextureId: BrushFamilyDecodeCallback,
): BrushFamily {
    val decompressed = DecompressedBytes(input)
    val convertPngBytesToAndroidBitmapAndCallAndroidCallback: (String, ByteArray?) -> String =
        { textureId: String, pngBytes: ByteArray? ->
            val bitmap: Bitmap? = pngBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
            getClientTextureId.onDecodeTexture(textureId, bitmap)
        }
    val nativePointer =
        BrushSerializationNative.newBrushFamilyFromProto(
            brushFamilyDirectByteBuffer = null,
            brushFamilyByteArray = decompressed.bytes,
            offset = 0,
            length = decompressed.size,
            callback = convertPngBytesToAndroidBitmapAndCallAndroidCallback,
        )
    check(nativePointer != 0L) { "Should have thrown exception if decoding failed." }
    return BrushFamily.wrapNative(nativePointer)
}

// Using an explicit singleton object instead of @file:JvmName to put the static interface intended
// for use from Java in a class because otherwise there are multiple top-level functions with the
// same name and signature on the Kotlin side. If one of those were used from Kotlin, it chooses and
// overload arbitrarily, which leads to potentially very confusing behavior (e.g. decode might work
// by coincidence at one point and then suddenly stop working when more overloads are added).

@ExperimentalInkCustomBrushApi
public object AndroidBrushFamilySerialization {
    /**
     * Write a gzip-compressed `ink.proto.BrushFamily` binary proto message representing the
     * [BrushFamily] to the given [OutputStream].
     *
     * @param brushFamily The [BrushFamily] object to encode.
     * @param output The [OutputStream] to write the gzip-compressed encoded bytes to.
     * @param textureBitmapStore The [TextureBitmapStore] to use to encode the texture images within
     *   the encoded [BrushFamily]. If this is not desired behavior, e.g. if the application has a
     *   static set of texture images that it includes as resources, then this can be a
     *   [TextureBitmapStore] that always returns `null`.
     */
    @JvmStatic
    public fun encode(
        brushFamily: BrushFamily,
        output: OutputStream,
        textureBitmapStore: TextureBitmapStore,
    ) {
        brushFamily.encode(output, textureBitmapStore)
    }

    /**
     * Read a serialized [BrushFamily] from the given [InputStream] and parse it into a
     * [BrushFamily], throwing an exception if parsing or validation was not successful. Kotlin
     * callers should use [BrushFamily.Companion.decode] instead.
     *
     * [getClientTextureId] is called synchronously as part of this function call, on the same
     * thread.
     *
     * @param input [InputStream] providing gzip-compressed `ink.proto.BrushFamily` binary proto
     *   messages, the same as written to [OutputStream] by [encode].
     * @param getClientTextureId A callback to store the decoded texture image, if one were encoded
     *   inside the serialized [BrushFamily], into a [TextureBitmapStore]. This is called
     *   synchronously as part of this function call on the same thread.
     * @return The [BrushFamily] parsed from the [InputStream].
     * @throws [java.io.IOException] if gzip-format bytes cannot be read from [input].
     * @throws [IllegalArgumentException] [input] does not provide a valid `ink.proto.BrushFamily`
     *   proto message, or the corresponding [BrushFamily] is invalid.
     */
    @SuppressWarnings("ExecutorRegistration")
    @JvmStatic
    public fun decode(
        input: InputStream,
        getClientTextureId: BrushFamilyDecodeCallback,
    ): BrushFamily = BrushFamily.decode(input, getClientTextureId)
}
