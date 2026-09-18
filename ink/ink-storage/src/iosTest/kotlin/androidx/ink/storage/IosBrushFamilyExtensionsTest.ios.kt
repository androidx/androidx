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

package androidx.ink.storage

import androidx.ink.brush.BrushBehavior
import androidx.ink.brush.BrushCoat
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.BrushPaint
import androidx.ink.brush.BrushPaint.TilingTexture
import androidx.ink.brush.BrushTip
import androidx.ink.brush.ExperimentalInkBrushCompatibilityApi
import androidx.ink.brush.ExperimentalInkCrossPlatformRenderingApi
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.TextureImageStore
import androidx.ink.brush.Version
import androidx.ink.brush.behavior.IntegralNode
import androidx.ink.brush.behavior.OutOfRange
import androidx.ink.brush.behavior.ProgressDomain
import androidx.ink.brush.behavior.SourceNode
import androidx.ink.brush.behavior.SourceNode.Source
import androidx.ink.brush.behavior.TargetNode
import androidx.ink.brush.behavior.TargetNode.Target
import androidx.kruth.assertThat
import kotlin.test.Test
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFRelease
import platform.CoreGraphics.CGColorRenderingIntent
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGDataProviderCopyData
import platform.CoreGraphics.CGDataProviderCreateWithCFData
import platform.CoreGraphics.CGDataProviderRelease
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageCreate
import platform.CoreGraphics.CGImageGetDataProvider
import platform.CoreGraphics.CGImageRelease
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.kCGBitmapByteOrderDefault
import platform.CoreImage.CIColor
import platform.CoreImage.CIImage
import platform.UIKit.UIImage

@OptIn(
    ExperimentalInkCrossPlatformRenderingApi::class,
    ExperimentalInkBrushCompatibilityApi::class,
    ExperimentalInkCustomBrushApi::class,
    ExperimentalForeignApi::class,
)
class IosBrushFamilyExtensionsTest {

    private val textureId1: String = "texture_id_1"
    private val textureId2: String = "texture_id_2"
    private val unknownId: String = "unknown_id"

    private val testBitmap1 = examplePng(colorValue = 0.toByte())
    private val testBitmap2 = examplePng(colorValue = 255.toByte())

    val textureIdToUIImage = mapOf(textureId1 to testBitmap1, textureId2 to testBitmap2)

    private val testBrushFamilyWithTextures =
        BrushFamily(
            coats =
                listOf(
                    BrushCoat(
                        paint =
                            BrushPaint(
                                textureLayers =
                                    listOf(
                                        TilingTexture(
                                            clientTextureId = textureId1,
                                            sizeX = 1f,
                                            sizeY = 4f,
                                        ),
                                        TilingTexture(
                                            clientTextureId = textureId1,
                                            sizeX = 2f,
                                            sizeY = 2f,
                                        ),
                                    )
                            ),
                        tip =
                            BrushTip(
                                behaviors =
                                    listOf(
                                        BrushBehavior(
                                            TargetNode(
                                                target = Target.WIDTH_MULTIPLIER,
                                                targetModifierRangeStart = 1f,
                                                targetModifierRangeEnd = 2f,
                                                input =
                                                    IntegralNode(
                                                        integrateOver =
                                                            ProgressDomain.TIME_IN_SECONDS,
                                                        integralValueRangeStart = 0f,
                                                        integralValueRangeEnd = 1f,
                                                        integralOutOfRangeBehavior =
                                                            OutOfRange.CLAMP,
                                                        input =
                                                            SourceNode(
                                                                Source.NORMALIZED_PRESSURE,
                                                                0f,
                                                                1f,
                                                            ),
                                                    ),
                                            )
                                        )
                                    )
                            ),
                    ),
                    BrushCoat(
                        paint =
                            BrushPaint(
                                textureLayers =
                                    listOf(
                                        BrushPaint.TilingTexture(
                                            clientTextureId = textureId2,
                                            sizeX = 2f,
                                            sizeY = 2f,
                                        )
                                    )
                            )
                    ),
                )
        )

    fun examplePng(colorValue: Byte): UIImage {
        val pixelData = byteArrayOf(colorValue, colorValue, colorValue, colorValue)
        val cfData =
            pixelData.usePinned { pinned ->
                CFDataCreate(
                    allocator = null,
                    bytes = pinned.addressOf(0).reinterpret(),
                    length = pixelData.size.toLong(),
                )
            }!!
        val provider = CGDataProviderCreateWithCFData(cfData)!!
        val colorSpace = CGColorSpaceCreateDeviceRGB()
        val cgImage =
            CGImageCreate(
                width = 1UL,
                height = 1UL,
                bitsPerComponent = 8UL,
                bitsPerPixel = 8UL * 4UL,
                bytesPerRow = 4UL,
                space = colorSpace,
                bitmapInfo = CGImageAlphaInfo.kCGImageAlphaLast.value or kCGBitmapByteOrderDefault,
                provider = provider,
                decode = null,
                shouldInterpolate = false,
                intent = CGColorRenderingIntent.kCGRenderingIntentDefault,
            )

        return UIImage(cgImage).also {
            CGImageRelease(cgImage)
            CGColorSpaceRelease(colorSpace)
            CGDataProviderRelease(provider)
            CFRelease(cfData)
        }
    }

    fun UIImage.imagePixelData(): List<Byte> {
        val provider = CGImageGetDataProvider(CGImage!!)!!
        val cfData = CGDataProviderCopyData(provider)!!
        val bytes = CFDataGetBytePtr(cfData)!!.readBytes(CFDataGetLength(cfData).toInt())
        CFRelease(cfData)
        return bytes.toList()
    }

    @Test
    fun withTextures_roundTrip() {
        val decodedTextureBitmapStore = mutableMapOf<String, UIImage?>()
        val decodeCallback = OnDecodeTextureUiImage { id: String, uiImage: UIImage? ->
            decodedTextureBitmapStore[id] = uiImage
            id
        }
        val original = testBrushFamilyWithTextures
        val encoded = original.encode(textureIdToUIImage::get)
        assertThat(BrushFamily.decode(encoded, onDecodeTexture = decodeCallback))
            .isEqualTo(original)

        assertThat(decodedTextureBitmapStore.size).isEqualTo(2)

        val actualUiImage1 = decodedTextureBitmapStore[textureId1]!!
        val expectedUiImage1 = textureIdToUIImage[textureId1]!!
        assertThat(actualUiImage1.imagePixelData()).isEqualTo(expectedUiImage1.imagePixelData())

        val actualUiImage2 = decodedTextureBitmapStore[textureId2]!!
        val expectedUiImage2 = textureIdToUIImage[textureId2]!!
        assertThat(actualUiImage2.imagePixelData()).isEqualTo(expectedUiImage2.imagePixelData())
    }

    @Test
    fun uiImageWrappingCiImage_toPngBytes() {
        val wrappingCiImage = UIImage(CIImage(testBitmap1.CGImage!!))
        assertThat(wrappingCiImage.toPngBytes()).isNotNull()
    }

    @Test
    fun uiImageWrappingGeneratedCiImage_toPngBytes() {
        val generatedCiImage =
            CIImage.imageWithColor(CIColor.blackColor)
                .imageByCroppingToRect(CGRectMake(0.0, 0.0, 1.0, 1.0))
        assertThat(generatedCiImage.CGImage).isNull()

        val wrappingGeneratedCiImage = UIImage(generatedCiImage)
        assertThat(wrappingGeneratedCiImage.toPngBytes()).isNotNull()
    }

    @Test
    fun withTextures_writingFromCiImage_roundTrip_roundTrip() {
        val decodedTextureBitmapStore = mutableMapOf<String, UIImage?>()
        val decodeCallback = OnDecodeTextureUiImage { id: String, uiImage: UIImage? ->
            decodedTextureBitmapStore[id] = uiImage
            id
        }
        val original = testBrushFamilyWithTextures
        val encoded =
            original.encode(
                TextureImageStore { textureId ->
                    textureIdToUIImage[textureId]?.let { UIImage(CIImage(it.CGImage!!)) }
                }
            )
        assertThat(BrushFamily.decode(encoded, onDecodeTexture = decodeCallback))
            .isEqualTo(original)

        assertThat(decodedTextureBitmapStore.size).isEqualTo(2)

        val actualUiImage1 = decodedTextureBitmapStore[textureId1]!!
        val expectedUiImage1 = textureIdToUIImage[textureId1]!!
        assertThat(actualUiImage1!!.imagePixelData()).isEqualTo(expectedUiImage1!!.imagePixelData())

        val actualUiImage2 = decodedTextureBitmapStore[textureId2]!!
        val expectedUiImage2 = textureIdToUIImage[textureId2]!!
        assertThat(actualUiImage2.imagePixelData()).isEqualTo(expectedUiImage2.imagePixelData())
    }

    @Test
    fun withTextures_encodeMultiple_roundTrip() {
        val texture1Family =
            BrushFamily(
                coats =
                    listOf(
                        BrushCoat(
                            paint =
                                BrushPaint(
                                    textureLayers =
                                        listOf(
                                            TilingTexture(
                                                clientTextureId = textureId1,
                                                sizeX = 1f,
                                                sizeY = 4f,
                                            )
                                        )
                                )
                        )
                    )
            )
        val sameVersionFamily =
            BrushFamily(
                coats =
                    listOf(
                        BrushCoat(
                            paint =
                                BrushPaint(
                                    textureLayers =
                                        listOf(
                                            TilingTexture(
                                                clientTextureId = "same_version",
                                                sizeX = 1f,
                                                sizeY = 4f,
                                            )
                                        )
                                )
                        )
                    )
            )
        val v1behavior =
            BrushBehavior(
                TargetNode(
                    target = Target.WIDTH_MULTIPLIER,
                    targetModifierRangeStart = 1f,
                    targetModifierRangeEnd = 2f,
                    input =
                        IntegralNode(
                            integrateOver = ProgressDomain.TIME_IN_SECONDS,
                            integralValueRangeStart = 0f,
                            integralValueRangeEnd = 1f,
                            integralOutOfRangeBehavior = OutOfRange.CLAMP,
                            input = SourceNode(Source.NORMALIZED_PRESSURE, 0f, 1f),
                        ),
                )
            )
        val texture2Family =
            BrushFamily(
                coats =
                    listOf(
                        BrushCoat(
                            paint =
                                BrushPaint(
                                    textureLayers =
                                        listOf(
                                            TilingTexture(
                                                clientTextureId = textureId2,
                                                sizeX = 1f,
                                                sizeY = 4f,
                                            )
                                        )
                                ),
                            tip = BrushTip(behaviors = listOf(v1behavior)),
                        )
                    )
            )
        val families = listOf(texture1Family, sameVersionFamily, texture2Family)
        assertThat(texture1Family.calculateMinimumRequiredVersion()).isEqualTo(Version.V0)
        assertThat(sameVersionFamily.calculateMinimumRequiredVersion()).isEqualTo(Version.V0)
        assertThat(texture2Family.calculateMinimumRequiredVersion()).isEqualTo(Version.V1)

        val encoded = families.encodeMultiple(textureIdToUIImage::get)

        val decodedTextureImageStore = mutableMapOf<String, UIImage?>()
        val decodedTextureIds = mutableListOf<String>()
        val decodeCallback = OnDecodeTextureUiImage { id: String, uiImage: UIImage? ->
            decodedTextureIds.add(id)
            decodedTextureImageStore[id] = uiImage
            id
        }
        val decoded = BrushFamily.decodeMultiple(encoded, onDecodeTexture = decodeCallback)

        // Only the first brush family for a given version is encoded, to avoid wasting space on
        // redundant fallbacks.
        assertThat(decodedTextureIds).hasSize(2)
        assertThat(decoded).hasSize(2)
        assertThat(decoded[0]).isEqualTo(texture1Family)
        // Fallbacks aren't set on the individual brush families with decodeMultiple.
        assertThat(decoded[0].hasFallbacks).isFalse()
        assertThat(decoded[1]).isEqualTo(texture2Family)
        assertThat(decoded[1].hasFallbacks).isFalse()

        val actualUiImage1 = decodedTextureImageStore[textureId1]!!
        val expectedUiImage1 = textureIdToUIImage[textureId1]!!
        assertThat(actualUiImage1.imagePixelData()).isEqualTo(expectedUiImage1.imagePixelData())

        val actualUiImage2 = decodedTextureImageStore[textureId2]!!
        val expectedUiImage2 = textureIdToUIImage[textureId2]!!
        assertThat(actualUiImage2.imagePixelData()).isEqualTo(expectedUiImage2.imagePixelData())
    }
}
