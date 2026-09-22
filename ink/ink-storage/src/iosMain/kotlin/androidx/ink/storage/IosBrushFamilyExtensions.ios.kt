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

@file:OptIn(ExperimentalInkCrossPlatformRenderingApi::class, ExperimentalForeignApi::class)

package androidx.ink.storage

import androidx.annotation.RestrictTo
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.ExperimentalInkBrushCompatibilityApi
import androidx.ink.brush.ExperimentalInkCrossPlatformRenderingApi
import androidx.ink.brush.TextureImageStore
import androidx.ink.brush.Version
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGImageRelease
import platform.CoreImage.CIContext
import platform.CoreImage.CIImage
import platform.CoreImage.createCGImage
import platform.CoreImage.kCIContextUseSoftwareRenderer
import platform.CoreImage.kCIFormatRGBA8
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.posix.memcpy

/** A callback to use with [decode] to manage texture image assets. */
@ExperimentalInkCrossPlatformRenderingApi
public fun interface OnDecodeTextureUiImage {
    /**
     * Called for each texture used by a BrushFamily when that BrushFamily is decoded. In the
     * implementation of this method, the returned string should be mapped to [uiImage] (or a
     * client- provided replacement) in the supporting [TextureImageStore].
     *
     * @param clientTextureId the ID for this texture in the serialized form of the BrushFamily.
     * @param uiImage the image corresponding to [clientTextureId] in the serialized form of the
     *   [BrushFamily]. `null` indicates that the serialized form did not store a bitmap for
     *   [clientTextureId]. Note that the [UIImage] passed to this callback by the functions in this
     *   module will wrap a `CGImage`, so the underlying bitmap will be pre-loaded.
     * @return The client texture ID for this texture in the in-memory format of the [BrushFamily].
     *   This can be different from [clientTextureId], in particular when there are naming
     *   collisions in the [TextureImageStore] or when the texture store uses its own scheme for
     *   ensuring unique names.
     */
    public fun onDecodeTexture(clientTextureId: String, uiImage: UIImage?): String
}

/**
 * Return a gzip-compressed `ink.proto.BrushFamily` binary proto message representing the
 * [BrushFamily] as a [ByteArray]. If `hasFallbacks` is true, then the stored proto message
 * including fallbacks for this [BrushFamily] will be used instead of recomputing the proto from the
 * [BrushFamily] object.
 *
 * Raises an [IllegalStateException] if a [UIImage] is returned from the [TextureImageStore] but it
 * cannot be encoded to PNG.
 *
 * @param textureImageStore The [TextureImageStore] to use to encode the texture images within the
 *   encoded [BrushFamily]. If this is not desired behavior, e.g. if the application has a static
 *   set of texture images that it includes as resources, then this can be a [TextureImageStore]
 *   that always returns `null`.
 * @receiver The [BrushFamily] object to encode.
 */
@ExperimentalInkCrossPlatformRenderingApi
public fun BrushFamily.encode(textureImageStore: TextureImageStore): ByteArray =
    encode(textureImageStore.toTexturePngBytesLookup())

/**
 * Return a gzip-compressed `ink.proto.BrushFamily` binary proto message representing the [List] of
 * [BrushFamily]s as a [ByteArray].
 *
 * All [BrushFamily] objects in this [List] are encoded into a single BrushFamily proto object. At
 * the top-level is the lowest-version compatible [BrushFamily], making the proto backwards
 * compatible with older versions of Ink which have no concept of nested brush families. Creates new
 * fallbacks from the [BrushFamily] objects passed in, overriding any existing fallbacks on any
 * individual [BrushFamily].
 *
 * Raises an [IllegalStateException] if a [UIImage] is returned from the [TextureImageStore] but it
 * cannot be encoded to PNG.
 *
 * @param textureImageStore The [TextureImageStore] to use to encode the texture images within the
 *   encoded [BrushFamily]s. If this is not desired behavior, e.g. if the application has a static
 *   set of texture images that it includes as resources, then this can be a [TextureImageStore]
 *   that always returns `null`.
 * @receiver The [List] of [BrushFamily] objects to encode.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
@ExperimentalInkBrushCompatibilityApi
@ExperimentalInkCrossPlatformRenderingApi
public fun List<BrushFamily>.encodeMultiple(textureImageStore: TextureImageStore): ByteArray =
    encodeMultiple(textureImageStore.toTexturePngBytesLookup())

/**
 * Read a serialized [BrushFamily] from the given [ByteArray] and parse it into a [BrushFamily],
 * throwing an exception if parsing or validation was not successful.
 *
 * @param input [ByteArray] providing gzip-compressed `ink.proto.BrushFamily` binary proto messages,
 *   the same as written by [encode].
 * @param onDecodeTexture A callback to store the decoded texture image, if one were encoded inside
 *   the serialized [BrushFamily], into a [TextureImageStore]. This is called synchronously as part
 *   of this function call on the same thread.
 * @param maxVersion The maximum [Version] to be supported by the deserializer. Proto objects with a
 *   `min_version` of greater than maxVersion will be rejected.
 * @return The [BrushFamily] parsed from the [ByteArray].
 * @throws [okio.IOException] if gzip-format bytes cannot be read from [input].
 * @throws [IllegalArgumentException] [input] does not provide a valid `ink.proto.BrushFamily` proto
 *   message, or the corresponding [BrushFamily] is invalid.
 */
@ExperimentalInkCrossPlatformRenderingApi
public fun BrushFamily.Companion.decode(
    input: ByteArray,
    maxVersion: Version,
    onDecodeTexture: OnDecodeTextureUiImage,
): BrushFamily = BrushFamily.decode(input, maxVersion, onDecodeTexture.toOnDecodeTexturePngBytes())

/** See [decode] above. This overload uses [Version.MAX_SUPPORTED]. */
@ExperimentalInkCrossPlatformRenderingApi
public fun BrushFamily.Companion.decode(
    input: ByteArray,
    onDecodeTexture: OnDecodeTextureUiImage,
): BrushFamily = BrushFamily.decode(input, Version.MAX_SUPPORTED, onDecodeTexture)

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
@ExperimentalInkBrushCompatibilityApi
@ExperimentalInkCrossPlatformRenderingApi
public fun BrushFamily.Companion.decodeMultiple(
    input: ByteArray,
    maxVersion: Version,
    onDecodeTexture: OnDecodeTextureUiImage,
): List<BrushFamily> =
    decodeMultiple(input, maxVersion, onDecodeTexture.toOnDecodeTexturePngBytes())

/** See [decodeMultiple] above. This overload uses [Version.MAX_SUPPORTED]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // FutureJetpackApi
@ExperimentalInkBrushCompatibilityApi
@ExperimentalInkCrossPlatformRenderingApi
public fun BrushFamily.Companion.decodeMultiple(
    input: ByteArray,
    onDecodeTexture: OnDecodeTextureUiImage,
): List<BrushFamily> =
    BrushFamily.decodeMultiple(
        input,
        Version.MAX_SUPPORTED,
        onDecodeTexture.toOnDecodeTexturePngBytes(),
    )

private fun TextureImageStore.toTexturePngBytesLookup() = TexturePngBytesLookup { textureId ->
    get(textureId)?.let { uiImage ->
        checkNotNull(uiImage.toPngBytes()) {
            "Failed to encode texture image for client texture ID $textureId"
        }
    }
}

internal fun UIImage.toPngBytes(): ByteArray? =
    toUiImageWrappingCgImage()
        ?.let { uiImageWrappingCgImage -> UIImagePNGRepresentation(uiImageWrappingCgImage) }
        ?.takeIf { nsData -> nsData.length > 0U }
        ?.let { nsData ->
            ByteArray(nsData.length.toInt()).apply {
                usePinned { pinned -> memcpy(pinned.addressOf(0), nsData.bytes, nsData.length) }
            }
        }

private fun UIImage.toUiImageWrappingCgImage(): UIImage? =
    // If the UIImage was directly initialized from a CGImage, use it.
    if (CGImage != null) {
        this
    } else {
        CIImage?.toUiImageWrappingCgImage()
    }

private fun CIImage.toUiImageWrappingCgImage(): UIImage? {
    // If the CIImage was directly initialized from a CGImage, wrap the CGImage more directly so
    // that
    // calling UIImagePNGRepresentation on the result works.
    CGImage?.let {
        return UIImage(it)
    }

    val colorSpace = CGColorSpaceCreateDeviceRGB()
    try {
        val cgImage =
            CIContext.contextWithOptions(null)
                .createCGImage(
                    this,
                    fromRect = extent,
                    format = kCIFormatRGBA8,
                    colorSpace = colorSpace,
                )
                ?: CIContext.contextWithOptions(mapOf(kCIContextUseSoftwareRenderer to true))
                    .createCGImage(
                        this,
                        fromRect = extent,
                        format = kCIFormatRGBA8,
                        colorSpace = colorSpace,
                    )
        return cgImage?.let { UIImage(it).also { _ -> CGImageRelease(it) } }
    } finally {
        CGColorSpaceRelease(colorSpace)
    }
}

private fun OnDecodeTextureUiImage.toOnDecodeTexturePngBytes() =
    OnDecodeTexturePngBytes { textureId: String, pngBytes: ByteArray? ->
        onDecodeTexture(
            textureId,
            pngBytes
                ?.takeIf { it.isNotEmpty() }
                ?.usePinned { pinned ->
                    UIImage.imageWithData(
                        NSData.dataWithBytes(pinned.addressOf(0), pngBytes.size.toULong())
                    )
                },
        )
    }
