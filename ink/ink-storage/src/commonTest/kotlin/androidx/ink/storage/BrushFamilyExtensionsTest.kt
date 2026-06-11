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

@OptIn(ExperimentalEncodingApi::class)
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
        assertThat(BrushFamily.decode(encoded, maxVersion = Version.V0_JETPACK1_0_0))
            .isEqualTo(original)
    }

    @Test
    fun encode_decode_roundTrip_maxVersion_rejectsHigherVersion() {
        // Create a BrushFamily with a min_version of V1_JETPACK1_0_0_ALPHA01 and try to decode it
        // with
        // a maxVersion of V0_JETPACK1_0_0.
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
            BrushFamily.decode(encoded, maxVersion = Version.V0_JETPACK1_0_0)
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
        val decoded1 = BrushFamily.decode(encoded, maxVersion = Version.V0_JETPACK1_0_0)
        assertThat(decoded1).isEqualTo(family1)
        assertThat(decoded1.hasFallbacks).isTrue()
        val encoded1 = decoded1.encode()
        val decoded2 = BrushFamily.decode(encoded1, maxVersion = Version.V1_JETPACK1_1_0_ALPHA01)
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
            BrushFamily.decodeMultiple(encoded, maxVersion = Version.V0_JETPACK1_0_0)
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

        val decoded = BrushFamily.decode(encoded, maxVersion = Version.V0_JETPACK1_0_0)
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
        val decodedTextureBitmapStore = mutableMapOf<String, ByteArray?>()
        val decodeCallback = OnDecodeTexturePngBytes { id: String, pngBytes: ByteArray? ->
            decodedTextureBitmapStore[id] = pngBytes
            id
        }
        val original = testBrushFamilyWithTextures
        val encoded = original.encode(textureIdToPngBytes::get)
        assertThat(BrushFamily.decode(encoded, onDecodeTexture = decodeCallback))
            .isEqualTo(original)

        assertThat(decodedTextureBitmapStore.size).isEqualTo(2)
        val actualBitmap1 = decodedTextureBitmapStore[textureId1]
        val expectedBitmap1 = textureIdToPngBytes[textureId1]
        assertThat(actualBitmap1).isEqualTo(expectedBitmap1)

        val actualBitmap2 = decodedTextureBitmapStore[textureId2]
        val expectedBitmap2 = textureIdToPngBytes[textureId2]
        assertThat(actualBitmap2).isEqualTo(expectedBitmap2)
    }
}
