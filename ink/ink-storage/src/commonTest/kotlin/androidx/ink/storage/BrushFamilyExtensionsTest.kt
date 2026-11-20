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

package androidx.ink.storage

import androidx.ink.brush.BrushBehavior
import androidx.ink.brush.BrushCoat
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.BrushPaint
import androidx.ink.brush.BrushPaint.TilingTexture
import androidx.ink.brush.BrushTip
import androidx.ink.brush.ExperimentalInkBrushCompatibilityApi
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.Version
import androidx.ink.brush.behavior.EasingFunction
import androidx.ink.brush.behavior.IntegralNode
import androidx.ink.brush.behavior.OutOfRange
import androidx.ink.brush.behavior.ProgressDomain
import androidx.ink.brush.behavior.ResponseNode
import androidx.ink.brush.behavior.SourceNode
import androidx.ink.brush.behavior.SourceNode.Source
import androidx.ink.brush.behavior.TargetNode
import androidx.ink.brush.behavior.TargetNode.Target
import androidx.kruth.assertThat
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertFailsWith
import okio.Buffer
import okio.GzipSink
import okio.IOException
import okio.buffer
import okio.use

@OptIn(
    ExperimentalEncodingApi::class,
    ExperimentalInkBrushCompatibilityApi::class,
    ExperimentalInkCustomBrushApi::class,
)
class BrushFamilyExtensionsTest {

    private val notGzippedBytes = byteArrayOf(0)

    private val gzippedNotProtoBytes = run {
        val buffer = Buffer()
        GzipSink(buffer).buffer().use { it.write(byteArrayOf(1)) }
        buffer.readByteArray()
    }

    /**
     * Gzipped binary-proto of a BrushFamily that fails validation. Generated with:
     * ```
     * val invalidProto = brushFamily {
     *   coats += brushCoat { tip = brushTip { particleGapDurationSeconds = -1f } }
     * }
     * val invalidProtoBytes =
     *   ByteArrayOutputStream().use { byteArrayStream ->
     *     GZIPOutputStream(byteArrayStream).use { gzipStream ->
     *       gzipStream.write(invalidProto.toByteArray())
     *     }
     *     byteArrayStream.toByteArray()
     *   }
     * Base64.getEncoder().encodeToString(invalidProtoBytes)
     * ```
     */
    private val gzippedInvalidProtoBytes =
        Base64.Default.decode("H4sIAAAAAAAA/1Ni52INZWBo2A8Agg/YJAkAAAA=")

    @Test
    fun encode_decode_roundTrip() {
        // This wraps the native encode/decode, so the details are tested in the tests for the
        // underlying C++ library.
        val original = BrushFamily()
        assertThat(BrushFamily.decode(original.encode())).isEqualTo(original)
    }

    @Test
    fun encode_decode_enumObjectIdentity() {
        val original =
            BrushFamily(
                BrushTip(
                    behaviors =
                        listOf(
                            BrushBehavior(
                                TargetNode(
                                    target = Target.SIZE_MULTIPLIER,
                                    targetModifierRangeStart = 0f,
                                    targetModifierRangeEnd = 1f,
                                    input =
                                        ResponseNode(
                                            responseCurve =
                                                EasingFunction.Steps(
                                                    3,
                                                    EasingFunction.StepPosition.JUMP_BOTH,
                                                ),
                                            input =
                                                SourceNode(
                                                    source = Source.NORMALIZED_PRESSURE,
                                                    sourceValueRangeStart = 0f,
                                                    sourceValueRangeEnd = 1f,
                                                ),
                                        ),
                                )
                            )
                        )
                )
            )
        val encoded = original.encode()
        val decodedBrushFamily = BrushFamily.decode(encoded)
        assertThat(decodedBrushFamily).isEqualTo(original)
        val decodedTargetNode =
            decodedBrushFamily.coats[0].tip.behaviors[0].terminalNodes[0] as TargetNode
        assertThat(decodedTargetNode.target).isSameInstanceAs(Target.SIZE_MULTIPLIER)
        val decodedResponseNode = decodedTargetNode.input as ResponseNode
        val decodedStepFunction = decodedResponseNode.responseCurve as EasingFunction.Steps
        assertThat(decodedStepFunction.stepPosition)
            .isSameInstanceAs(EasingFunction.StepPosition.JUMP_BOTH)
        val decodedSourceNode = decodedResponseNode.input as SourceNode
        assertThat(decodedSourceNode.source).isSameInstanceAs(Source.NORMALIZED_PRESSURE)
    }

    @Test
    fun encode_decode_roundTrip_maxVersion() {
        // Just testing that we can pass the optional maxVersion parameter.
        val original = BrushFamily()
        val encoded = original.encode()
        assertThat(BrushFamily.decode(encoded, maxVersion = Version.V0)).isEqualTo(original)
    }

    @Test
    fun encode_decode_roundTrip_maxVersion_rejectsHigherVersion() {
        // Create a BrushFamily with a min_version of V1_ALPHA01 and try to decode it with
        // a maxVersion of V0.
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
        val original = BrushFamily(tip = BrushTip(behaviors = listOf(behavior)))
        val encoded = original.encode()
        assertFailsWith<IllegalArgumentException> {
            BrushFamily.decode(encoded, maxVersion = Version.V0)
        }
    }

    @Test
    fun decode_notGzippedBytes_throws() {
        assertFailsWith<IOException> {
            @Suppress("CheckReturnValue") BrushFamily.decode(notGzippedBytes)
        }
    }

    @Test
    fun decode_gzippedNotProtoBytes_throws() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                @Suppress("CheckReturnValue") BrushFamily.decode(gzippedNotProtoBytes)
            }
        assertThat(exception).hasMessageThat().contains("Failed to parse ink.proto.BrushFamily")
    }

    @Test
    fun decode_gzippedInvalidProtoBytes_throws() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                @Suppress("CheckReturnValue") BrushFamily.decode(gzippedInvalidProtoBytes)
            }
        assertThat(exception).hasMessageThat().contains("particle_gap_duration")
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

        val encoded = families.encodeMultiple()

        val decoded = BrushFamily.decodeMultiple(encoded)
        assertThat(decoded).hasSize(2)
        assertThat(decoded[0]).isEqualTo(family1)
        assertThat(decoded[0].hasFallbacks).isFalse()
        assertThat(decoded[1]).isEqualTo(family2)
        assertThat(decoded[1].hasFallbacks).isFalse()

        // Test fallbacks
        val decoded1 = BrushFamily.decode(encoded, maxVersion = Version.V0)
        assertThat(decoded1).isEqualTo(family1)
        assertThat(decoded1.hasFallbacks).isTrue()
        val encoded1 = decoded1.encode()
        val decoded2 = BrushFamily.decode(encoded1, maxVersion = Version.V1)
        assertThat(decoded2).isEqualTo(family2)
        assertThat(decoded2.hasFallbacks).isTrue()
        val encoded2 = decoded2.encode()

        // Should still round trip to the same as the original.
        val decodedAgain = BrushFamily.decodeMultiple(encoded2)
        assertThat(decodedAgain).hasSize(2)
        assertThat(decodedAgain[0]).isEqualTo(family1)
        assertThat(decodedAgain[0].hasFallbacks).isFalse()
        assertThat(decodedAgain[1]).isEqualTo(family2)
        assertThat(decodedAgain[1].hasFallbacks).isFalse()
    }

    @Test
    fun encodeMultiple_decodeMultiple_roundTrip_maxVersion_rejectsHigherVersion() {
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

        val encoded = families.encodeMultiple()

        assertFailsWith<IllegalArgumentException> {
            BrushFamily.decodeMultiple(encoded, maxVersion = Version.V0)
        }
    }

    @Test
    fun fallbacks_not_preserved_by_copy_or_toBuilder() {
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

        val encoded = families.encodeMultiple()

        val decoded = BrushFamily.decode(encoded, maxVersion = Version.V0)
        assertThat(decoded).isEqualTo(family1)
        assertThat(decoded.hasFallbacks).isTrue()
        val copied = decoded.copy(developerComment = "copied")
        assertThat(copied.hasFallbacks).isFalse()
        assertThat(copied.developerComment).isEqualTo("copied")
        val builder = decoded.toBuilder()
        builder.setDeveloperComment("built")
        val built = builder.build()
        assertThat(built.hasFallbacks).isFalse()
        assertThat(built.developerComment).isEqualTo("built")
    }

    @Test
    fun withTextures_roundTrip() {
        val textureId1: String = "texture_id_1"
        val textureId2: String = "texture_id_2"
        val testBitmap1 = byteArrayOf(1, 1, 1, 1)
        val testBitmap2 = byteArrayOf(2, 2, 2, 2)
        val textureIdToPngBytes = mapOf(textureId1 to testBitmap1, textureId2 to testBitmap2)
        val original =
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
                                )
                        ),
                        BrushCoat(
                            paint =
                                BrushPaint(
                                    textureLayers =
                                        listOf(
                                            TilingTexture(
                                                clientTextureId = textureId1,
                                                sizeX = 2f,
                                                sizeY = 2f,
                                            ),
                                            TilingTexture(
                                                clientTextureId = textureId2,
                                                sizeX = 2f,
                                                sizeY = 2f,
                                            ),
                                        )
                                )
                        ),
                    )
            )

        val encoded = original.encode(textureIdToPngBytes::get)

        val decodedTextureBitmapStore = mutableMapOf<String, ByteArray?>()
        val decodedTextureIds = mutableListOf<String>()
        val decodeCallback = OnDecodeTexturePngBytes { id: String, pngBytes: ByteArray? ->
            decodedTextureIds.add(id)
            decodedTextureBitmapStore[id] = pngBytes
            id
        }
        assertThat(BrushFamily.decode(encoded, onDecodeTexture = decodeCallback))
            .isEqualTo(original)

        // The texture is only recorded once per ID in the brush family.
        assertThat(decodedTextureIds).hasSize(2)
        val actualBitmap1 = decodedTextureBitmapStore[textureId1]
        val expectedBitmap1 = textureIdToPngBytes[textureId1]
        assertThat(actualBitmap1).apply {
            isNotNull()
            isEqualTo(expectedBitmap1)
        }

        val actualBitmap2 = decodedTextureBitmapStore[textureId2]
        val expectedBitmap2 = textureIdToPngBytes[textureId2]
        assertThat(actualBitmap2).apply {
            isNotNull()
            isEqualTo(expectedBitmap2)
        }
    }

    @Test
    fun withTextures_encodeMultiple_roundTrip() {
        val textureId1: String = "texture_id_1"
        val textureId2: String = "texture_id_2"
        val testBitmap1 = byteArrayOf(1, 1, 1, 1)
        val testBitmap2 = byteArrayOf(2, 2, 2, 2)
        val textureIdToPngBytes = mapOf(textureId1 to testBitmap1, textureId2 to testBitmap2)
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
                                )
                        )
                    )
            )
        val families = listOf(texture1Family, texture2Family)

        val encoded = families.encodeMultiple(textureIdToPngBytes::get)

        val decodedTextureBitmapStore = mutableMapOf<String, ByteArray?>()
        val decodedTextureIds = mutableListOf<String>()
        val decodeCallback = OnDecodeTexturePngBytes { id: String, pngBytes: ByteArray? ->
            decodedTextureIds.add(id)
            decodedTextureBitmapStore[id] = pngBytes
            id
        }
        val decoded = BrushFamily.decodeMultiple(encoded, onDecodeTexture = decodeCallback)

        assertThat(decoded).hasSize(1)
        assertThat(decoded[0]).isEqualTo(texture1Family)
        assertThat(decoded[0].hasFallbacks).isFalse()

        // Textures are recorded from the decoded brush families, but not any unused fallbacks.
        assertThat(decodedTextureIds).hasSize(1)
        val actualBitmap1 = decodedTextureBitmapStore[textureId1]
        val expectedBitmap1 = textureIdToPngBytes[textureId1]
        assertThat(actualBitmap1).apply {
            isNotNull()
            isEqualTo(expectedBitmap1)
        }
    }

    @Test
    fun encode_getEncodedTextureId_restoresTextureIdsRenamedByDecode() {
        val textureIdToPngBytes =
            mapOf(
                "texture_id_1" to byteArrayOf(1, 1, 1, 1),
                "texture_id_2" to byteArrayOf(2, 2, 2, 2),
            )
        val original =
            BrushFamily(
                paint =
                    BrushPaint(
                        textureLayers =
                            listOf(
                                TilingTexture(
                                    clientTextureId = "texture_id_1",
                                    sizeX = 1f,
                                    sizeY = 4f,
                                ),
                                TilingTexture(
                                    clientTextureId = "texture_id_2",
                                    sizeX = 2f,
                                    sizeY = 2f,
                                ),
                            )
                    )
            )
        val encoded = original.encode(textureIdToPngBytes::get)

        // Decode, adding a prefix to each texture ID and storing each bitmap under the new ID, as
        // an
        // app might do to avoid collisions with texture IDs that it already uses.
        val textureStore = mutableMapOf<String, ByteArray?>()
        val addPrefix = OnDecodeTexturePngBytes { id: String, pngBytes: ByteArray? ->
            "renamed/$id".also { textureStore[it] = pngBytes }
        }
        val decoded = BrushFamily.decode(encoded, onDecodeTexture = addPrefix)
        assertThat(decoded)
            .isEqualTo(
                BrushFamily(
                    paint =
                        BrushPaint(
                            textureLayers =
                                listOf(
                                    TilingTexture(
                                        clientTextureId = "renamed/texture_id_1",
                                        sizeX = 1f,
                                        sizeY = 4f,
                                    ),
                                    TilingTexture(
                                        clientTextureId = "renamed/texture_id_2",
                                        sizeX = 2f,
                                        sizeY = 2f,
                                    ),
                                )
                        )
                )
            )

        // Encoding with the inverse transformation writes the original texture IDs.
        val reencoded =
            decoded.encode(
                object : TexturePngBytesLookup {
                    override fun getTexturePngBytes(clientTextureId: String): ByteArray? =
                        textureStore[clientTextureId]

                    override fun getEncodedTextureId(clientTextureId: String): String =
                        clientTextureId.removePrefix("renamed/")
                }
            )
        val redecodedTextureStore = mutableMapOf<String, ByteArray?>()
        val recordTextures = OnDecodeTexturePngBytes { id: String, pngBytes: ByteArray? ->
            redecodedTextureStore[id] = pngBytes
            id
        }
        assertThat(BrushFamily.decode(reencoded, onDecodeTexture = recordTextures))
            .isEqualTo(original)
        assertThat(redecodedTextureStore.keys).containsExactly("texture_id_1", "texture_id_2")
        assertThat(redecodedTextureStore["texture_id_1"])
            .isEqualTo(textureIdToPngBytes["texture_id_1"])
        assertThat(redecodedTextureStore["texture_id_2"])
            .isEqualTo(textureIdToPngBytes["texture_id_2"])
    }

    @Test
    fun encode_getEncodedTextureId_appliesToTexturesWithoutBitmaps() {
        val removePrefix =
            object : TexturePngBytesLookup {
                override fun getTexturePngBytes(clientTextureId: String): ByteArray? = null

                override fun getEncodedTextureId(clientTextureId: String): String =
                    clientTextureId.removePrefix("renamed/")
            }
        val family =
            BrushFamily(
                paint =
                    BrushPaint(
                        textureLayers =
                            listOf(
                                TilingTexture(
                                    clientTextureId = "renamed/texture_id_1",
                                    sizeX = 1f,
                                    sizeY = 4f,
                                )
                            )
                    )
            )

        val encoded = family.encode(removePrefix)

        assertThat(BrushFamily.decode(encoded))
            .isEqualTo(
                BrushFamily(
                    paint =
                        BrushPaint(
                            textureLayers =
                                listOf(
                                    TilingTexture(
                                        clientTextureId = "texture_id_1",
                                        sizeX = 1f,
                                        sizeY = 4f,
                                    )
                                )
                        )
                )
            )
    }

    @Test
    fun encodeMultiple_getEncodedTextureId_appliesToAllBrushFamilies() {
        val v1Tip =
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
                                        integrateOver = ProgressDomain.TIME_IN_SECONDS,
                                        integralValueRangeStart = 0f,
                                        integralValueRangeEnd = 1f,
                                        integralOutOfRangeBehavior = OutOfRange.CLAMP,
                                        input = SourceNode(Source.NORMALIZED_PRESSURE, 0f, 1f),
                                    ),
                            )
                        )
                    )
            )
        val textureIdToPngBytes =
            mapOf(
                "texture_id_1" to byteArrayOf(1, 1, 1, 1),
                "texture_id_2" to byteArrayOf(2, 2, 2, 2),
            )
        val removePrefix =
            object : TexturePngBytesLookup {
                override fun getTexturePngBytes(clientTextureId: String): ByteArray? =
                    textureIdToPngBytes[clientTextureId.removePrefix("renamed/")]

                override fun getEncodedTextureId(clientTextureId: String): String =
                    clientTextureId.removePrefix("renamed/")
            }
        val families =
            listOf(
                BrushFamily(
                    paint =
                        BrushPaint(
                            textureLayers =
                                listOf(
                                    TilingTexture(
                                        clientTextureId = "renamed/texture_id_1",
                                        sizeX = 1f,
                                        sizeY = 4f,
                                    )
                                )
                        )
                ),
                BrushFamily(
                    tip = v1Tip,
                    paint =
                        BrushPaint(
                            textureLayers =
                                listOf(
                                    TilingTexture(
                                        clientTextureId = "renamed/texture_id_2",
                                        sizeX = 1f,
                                        sizeY = 4f,
                                    )
                                )
                        ),
                ),
            )

        val encoded = families.encodeMultiple(removePrefix)

        val decodedTextureStore = mutableMapOf<String, ByteArray?>()
        val recordTextures = OnDecodeTexturePngBytes { id: String, pngBytes: ByteArray? ->
            decodedTextureStore[id] = pngBytes
            id
        }
        val decoded = BrushFamily.decodeMultiple(encoded, onDecodeTexture = recordTextures)
        assertThat(decoded)
            .containsExactly(
                BrushFamily(
                    paint =
                        BrushPaint(
                            textureLayers =
                                listOf(
                                    TilingTexture(
                                        clientTextureId = "texture_id_1",
                                        sizeX = 1f,
                                        sizeY = 4f,
                                    )
                                )
                        )
                ),
                BrushFamily(
                    tip = v1Tip,
                    paint =
                        BrushPaint(
                            textureLayers =
                                listOf(
                                    TilingTexture(
                                        clientTextureId = "texture_id_2",
                                        sizeX = 1f,
                                        sizeY = 4f,
                                    )
                                )
                        ),
                ),
            )
            .inOrder()
        assertThat(decodedTextureStore.keys).containsExactly("texture_id_1", "texture_id_2")
        assertThat(decodedTextureStore["texture_id_1"])
            .isEqualTo(textureIdToPngBytes["texture_id_1"])
        assertThat(decodedTextureStore["texture_id_2"])
            .isEqualTo(textureIdToPngBytes["texture_id_2"])
    }
}
