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
import androidx.ink.brush.Version
import androidx.ink.brush.behavior.IntegralNode
import androidx.ink.brush.behavior.OutOfRange
import androidx.ink.brush.behavior.ProgressDomain
import androidx.ink.brush.behavior.SourceNode
import androidx.ink.brush.behavior.SourceNode.Source
import androidx.ink.brush.behavior.TargetNode
import androidx.ink.brush.behavior.TargetNode.Target
import androidx.kruth.assertThat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.Test

/**
 * Tests for parts of [BrushFamily] serialization and deserialization logic with JVM-specific
 * interfaces. (Specifically, the Java-specific static API and the interfaces which use
 * InputStream/OutputStream instead of ByteArray.) These compose over a common implementation, so
 * this just covers basic plumbing, the tests for detailed error behavior are in the KMP-common
 * tests.
 */
class JvmBrushFamilyExtensionsTest {

    private val textureId1: String = "texture_id_1"
    private val textureId2: String = "texture_id_2"
    private val unknownId: String = "unknown_id"

    private val testBitmap1 = byteArrayOf(1, 1, 1, 1)
    private val testBitmap2 = byteArrayOf(2, 2, 2, 2)

    val textureIdToPngBytes = mapOf(textureId1 to testBitmap1, textureId2 to testBitmap2)

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

    private val anotherBrushFamilyWithTextures =
        BrushFamily(
            coats =
                listOf(
                    BrushCoat(
                        paint =
                            BrushPaint(
                                textureLayers =
                                    listOf(
                                        TilingTexture(
                                            clientTextureId = unknownId,
                                            sizeX = 1f,
                                            sizeY = 4f,
                                        )
                                    )
                            )
                    )
                )
        )

    @Test
    fun encode_decode_roundTrip() {
        // This wraps the native encode/decode, so the details are tested in the tests for the
        // underlying C++ library.
        val original = BrushFamily()
        val encoded =
            ByteArrayOutputStream().use {
                original.encode(it)
                it.toByteArray()
            }
        ByteArrayInputStream(encoded).use { assertThat(BrushFamily.decode(it)).isEqualTo(original) }
    }

    @Test
    fun encode_decode_roundTrip_maxVersion() {
        // Just testing that we can pass the optional maxVersion parameter.
        val original = BrushFamily()
        val encoded =
            ByteArrayOutputStream().use {
                original.encode(it)
                it.toByteArray()
            }
        ByteArrayInputStream(encoded).use {
            assertThat(BrushFamily.decode(it, maxVersion = Version.V0_JETPACK1_0_0))
                .isEqualTo(original)
        }
    }

    @Test
    fun encode_decode_roundTrip_staticApi() {
        // Kotlin callers should prefer the extension methods, but the static wrappers do work.
        val original = BrushFamily()
        val encoded =
            ByteArrayOutputStream().use {
                original.encode(it)
                it.toByteArray()
            }
        ByteArrayInputStream(encoded).use {
            assertThat(BrushFamilySerialization.decode(it)).isEqualTo(original)
        }
    }

    @Test
    fun encode_decode_roundTrip_byteArray_staticApi() {
        // Kotlin callers should prefer the extension methods, but the static wrappers do work.
        val original = BrushFamily()
        assertThat(BrushFamilySerialization.decode(BrushFamilySerialization.encode(original)))
            .isEqualTo(original)
    }

    @Test
    fun encode_decode_roundTrip_maxVersion_staticApi() {
        // Just testing that we can pass the optional maxVersion parameter.
        val original = BrushFamily()
        val encoded =
            ByteArrayOutputStream().use {
                original.encode(it)
                it.toByteArray()
            }
        ByteArrayInputStream(encoded).use {
            assertThat(BrushFamilySerialization.decode(it, maxVersion = Version.V0_JETPACK1_0_0))
                .isEqualTo(original)
        }
    }

    @Test
    fun encode_decode_roundTrip_byteArray_maxVersion_staticApi() {
        // Just testing that we can pass the optional maxVersion parameter.
        val original = BrushFamily()
        assertThat(
                BrushFamilySerialization.decode(
                    BrushFamilySerialization.encode(original),
                    maxVersion = Version.V0_JETPACK1_0_0,
                )
            )
            .isEqualTo(original)
    }

    @Test
    fun encodeMultiple_decodeMultiple_roundTrip() {
        val family1 = BrushFamily()
        val behavior =
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
        val family2 = BrushFamily(tip = BrushTip(behaviors = listOf(behavior)))
        val families = listOf(family1, family2)

        val encoded =
            ByteArrayOutputStream().use {
                families.encodeMultiple(it)
                it.toByteArray()
            }

        ByteArrayInputStream(encoded).use {
            val decoded = BrushFamily.decodeMultiple(it)
            assertThat(decoded).hasSize(2)
            assertThat(decoded[0]).isEqualTo(family1)
            assertThat(decoded[0].hasFallbacks).isFalse()
            assertThat(decoded[1]).isEqualTo(family2)
            assertThat(decoded[1].hasFallbacks).isFalse()
        }
        // Test fallbacks
        val decoded1: BrushFamily
        ByteArrayInputStream(encoded).use {
            decoded1 = BrushFamily.decode(it, maxVersion = Version.V0_JETPACK1_0_0)
            assertThat(decoded1).isEqualTo(family1)
            assertThat(decoded1.hasFallbacks).isTrue()
        }
        val encoded1 =
            ByteArrayOutputStream().use {
                decoded1.encode(it)
                it.toByteArray()
            }
        val decoded2: BrushFamily
        ByteArrayInputStream(encoded1).use {
            decoded2 = BrushFamily.decode(it, maxVersion = Version.V1_JETPACK1_1_0_ALPHA01)
            assertThat(decoded2).isEqualTo(family2)
            assertThat(decoded2.hasFallbacks).isTrue()
        }
        val encoded2 =
            ByteArrayOutputStream().use {
                decoded2.encode(it)
                it.toByteArray()
            }
        // Should still round trip to the same as the original.
        ByteArrayInputStream(encoded2).use {
            val decoded = BrushFamily.decodeMultiple(it)
            assertThat(decoded).hasSize(2)
            assertThat(decoded[0]).isEqualTo(family1)
            assertThat(decoded[0].hasFallbacks).isFalse()
            assertThat(decoded[1]).isEqualTo(family2)
            assertThat(decoded[1].hasFallbacks).isFalse()
        }
    }

    @Test
    fun encodeMultiple_decodeMultiple_roundTrip_staticApi() {
        val family1 = BrushFamily()
        val behavior =
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
        val family2 = BrushFamily(tip = BrushTip(behaviors = listOf(behavior)))
        val families = listOf(family1, family2)

        val encoded =
            ByteArrayOutputStream().use {
                BrushFamilySerialization.encodeMultiple(families, it)
                it.toByteArray()
            }

        ByteArrayInputStream(encoded).use {
            val decoded = BrushFamilySerialization.decodeMultiple(it)
            assertThat(decoded).hasSize(2)
            assertThat(decoded[0]).isEqualTo(family1)
            assertThat(decoded[0].hasFallbacks).isFalse()
            assertThat(decoded[1]).isEqualTo(family2)
            assertThat(decoded[1].hasFallbacks).isFalse()
        }
        // Test fallbacks
        val decoded1: BrushFamily
        ByteArrayInputStream(encoded).use {
            decoded1 = BrushFamilySerialization.decode(it, maxVersion = Version.V0_JETPACK1_0_0)
            assertThat(decoded1).isEqualTo(family1)
            assertThat(decoded1.hasFallbacks).isTrue()
        }
        val encoded1 =
            ByteArrayOutputStream().use {
                decoded1.encode(it)
                it.toByteArray()
            }
        val decoded2: BrushFamily
        ByteArrayInputStream(encoded1).use {
            decoded2 =
                BrushFamilySerialization.decode(it, maxVersion = Version.V1_JETPACK1_1_0_ALPHA01)
            assertThat(decoded2).isEqualTo(family2)
            assertThat(decoded2.hasFallbacks).isTrue()
        }
        val encoded2 =
            ByteArrayOutputStream().use {
                decoded2.encode(it)
                it.toByteArray()
            }
        // Should still round trip to the same as the original.
        ByteArrayInputStream(encoded2).use {
            val decoded = BrushFamilySerialization.decodeMultiple(it)
            assertThat(decoded).hasSize(2)
            assertThat(decoded[0]).isEqualTo(family1)
            assertThat(decoded[0].hasFallbacks).isFalse()
            assertThat(decoded[1]).isEqualTo(family2)
            assertThat(decoded[1].hasFallbacks).isFalse()
        }
    }

    @Test
    fun encodeMultiple_decodeMultiple_roundTrip_byteArray_staticApi() {
        val family1 = BrushFamily()
        val behavior =
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
        val family2 = BrushFamily(tip = BrushTip(behaviors = listOf(behavior)))
        val families = listOf(family1, family2)

        val encoded =
            ByteArrayOutputStream().use {
                BrushFamilySerialization.encodeMultiple(families, it)
                it.toByteArray()
            }

        ByteArrayInputStream(encoded).use {
            val decoded = BrushFamilySerialization.decodeMultiple(it)
            assertThat(decoded).hasSize(2)
            assertThat(decoded[0]).isEqualTo(family1)
            assertThat(decoded[0].hasFallbacks).isFalse()
            assertThat(decoded[1]).isEqualTo(family2)
            assertThat(decoded[1].hasFallbacks).isFalse()
        }
        // Test fallbacks
        val decoded1 =
            BrushFamilySerialization.decode(encoded, maxVersion = Version.V0_JETPACK1_0_0)
        assertThat(decoded1).isEqualTo(family1)
        assertThat(decoded1.hasFallbacks).isTrue()
        val encoded1 = BrushFamilySerialization.encode(decoded1)
        val decoded2 =
            BrushFamilySerialization.decode(encoded1, maxVersion = Version.V1_JETPACK1_1_0_ALPHA01)
        assertThat(decoded2).isEqualTo(family2)
        assertThat(decoded2.hasFallbacks).isTrue()
        val encoded2 = BrushFamilySerialization.encode(decoded2)
        // Should still round trip to the same as the original.
        val decoded = BrushFamilySerialization.decodeMultiple(encoded2)
        assertThat(decoded).hasSize(2)
        assertThat(decoded[0]).isEqualTo(family1)
        assertThat(decoded[0].hasFallbacks).isFalse()
        assertThat(decoded[1]).isEqualTo(family2)
        assertThat(decoded[1].hasFallbacks).isFalse()
    }

    @Test
    fun withTextures_roundTrip_staticApi() {
        val decodedTextureBitmapStore = mutableMapOf<String, ByteArray?>()
        val decodeCallback = OnDecodeTexturePngBytes { id: String, pngBytes: ByteArray? ->
            decodedTextureBitmapStore[id] = pngBytes
            id
        }
        // Kotlin callers should prefer the extension methods, but the static wrappers do work.
        val original = testBrushFamilyWithTextures
        val encoded =
            ByteArrayOutputStream().use {
                BrushFamilySerialization.encode(original, it, textureIdToPngBytes::get)
                it.toByteArray()
            }
        ByteArrayInputStream(encoded).use {
            assertThat(BrushFamilySerialization.decode(it, decodeCallback)).isEqualTo(original)
        }
        assertThat(decodedTextureBitmapStore.size).isEqualTo(2)
        val actualBitmap1 = decodedTextureBitmapStore[textureId1]
        val expectedBitmap1 = textureIdToPngBytes[textureId1]
        assertThat(actualBitmap1).isEqualTo(expectedBitmap1)

        val actualBitmap2 = decodedTextureBitmapStore[textureId2]
        val expectedBitmap2 = textureIdToPngBytes[textureId2]
        assertThat(actualBitmap2).isEqualTo(expectedBitmap2)
    }

    @Test
    fun withTextures_roundTrip_byteArray_staticApi() {
        val decodedTextureBitmapStore = mutableMapOf<String, ByteArray?>()
        val decodeCallback = OnDecodeTexturePngBytes { id: String, pngBytes: ByteArray? ->
            decodedTextureBitmapStore[id] = pngBytes
            id
        }
        // Kotlin callers should prefer the extension methods, but the static wrappers do work.
        val original = testBrushFamilyWithTextures
        val encoded = BrushFamilySerialization.encode(original, textureIdToPngBytes::get)
        assertThat(BrushFamilySerialization.decode(encoded, decodeCallback)).isEqualTo(original)
        assertThat(decodedTextureBitmapStore.size).isEqualTo(2)
        val actualBitmap1 = decodedTextureBitmapStore[textureId1]
        val expectedBitmap1 = textureIdToPngBytes[textureId1]
        assertThat(actualBitmap1).isEqualTo(expectedBitmap1)

        val actualBitmap2 = decodedTextureBitmapStore[textureId2]
        val expectedBitmap2 = textureIdToPngBytes[textureId2]
        assertThat(actualBitmap2).isEqualTo(expectedBitmap2)
    }

    @Test
    fun withTextures_roundTrip() {
        val decodedTextureBitmapStore = mutableMapOf<String, ByteArray?>()
        val decodeCallback = OnDecodeTexturePngBytes { id: String, pngBytes: ByteArray? ->
            decodedTextureBitmapStore[id] = pngBytes
            id
        }
        // Kotlin callers should prefer the extension methods, but the static wrappers do work.
        val original = testBrushFamilyWithTextures
        val encoded =
            ByteArrayOutputStream().use {
                original.encode(it, textureIdToPngBytes::get)
                it.toByteArray()
            }
        ByteArrayInputStream(encoded).use {
            assertThat(BrushFamily.decode(it, onDecodeTexture = decodeCallback)).isEqualTo(original)
        }
        assertThat(decodedTextureBitmapStore.size).isEqualTo(2)
        val actualBitmap1 = decodedTextureBitmapStore[textureId1]
        val expectedBitmap1 = textureIdToPngBytes[textureId1]
        assertThat(actualBitmap1).isEqualTo(expectedBitmap1)

        val actualBitmap2 = decodedTextureBitmapStore[textureId2]
        val expectedBitmap2 = textureIdToPngBytes[textureId2]
        assertThat(actualBitmap2).isEqualTo(expectedBitmap2)
    }

    @Test
    fun multiple_withTextures_roundTrip_staticApi() {
        val decodedTextureBitmapStore = mutableMapOf<String, ByteArray?>()
        val decodeCallback = OnDecodeTexturePngBytes { id: String, pngBytes: ByteArray? ->
            decodedTextureBitmapStore[id] = pngBytes
            id
        }
        // Kotlin callers should prefer the extension methods, but the static wrappers do work.
        val original = listOf(testBrushFamilyWithTextures, anotherBrushFamilyWithTextures)
        val encoded =
            ByteArrayOutputStream().use {
                BrushFamilySerialization.encodeMultiple(original, it, textureIdToPngBytes::get)
                it.toByteArray()
            }
        ByteArrayInputStream(encoded).use {
            assertThat(BrushFamilySerialization.decodeMultiple(it, decodeCallback))
                .containsExactlyElementsIn(original)
        }
        assertThat(decodedTextureBitmapStore.size).isEqualTo(3)
        val actualBitmap1 = decodedTextureBitmapStore[textureId1]
        val expectedBitmap1 = textureIdToPngBytes[textureId1]
        assertThat(actualBitmap1).isEqualTo(expectedBitmap1)

        val actualBitmap2 = decodedTextureBitmapStore[textureId2]
        val expectedBitmap2 = textureIdToPngBytes[textureId2]
        assertThat(actualBitmap2).isEqualTo(expectedBitmap2)

        assertThat(decodedTextureBitmapStore).containsKey(unknownId)
        assertThat(decodedTextureBitmapStore[unknownId]).isNull()
    }

    @Test
    fun multiple_withTextures_roundTrip_byteArray_staticApi() {
        val decodedTextureBitmapStore = mutableMapOf<String, ByteArray?>()
        val decodeCallback = OnDecodeTexturePngBytes { id: String, pngBytes: ByteArray? ->
            decodedTextureBitmapStore[id] = pngBytes
            id
        }
        // Kotlin callers should prefer the extension methods, but the static wrappers do work.
        val original = listOf(testBrushFamilyWithTextures, anotherBrushFamilyWithTextures)
        val encoded = BrushFamilySerialization.encodeMultiple(original, textureIdToPngBytes::get)
        assertThat(BrushFamilySerialization.decodeMultiple(encoded, decodeCallback))
            .containsExactlyElementsIn(original)
        assertThat(decodedTextureBitmapStore.size).isEqualTo(3)
        val actualBitmap1 = decodedTextureBitmapStore[textureId1]
        val expectedBitmap1 = textureIdToPngBytes[textureId1]
        assertThat(actualBitmap1).isEqualTo(expectedBitmap1)

        val actualBitmap2 = decodedTextureBitmapStore[textureId2]
        val expectedBitmap2 = textureIdToPngBytes[textureId2]
        assertThat(actualBitmap2).isEqualTo(expectedBitmap2)

        assertThat(decodedTextureBitmapStore).containsKey(unknownId)
        assertThat(decodedTextureBitmapStore[unknownId]).isNull()
    }
}
