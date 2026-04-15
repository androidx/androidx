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
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.BrushTip
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
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.GZIPOutputStream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@OptIn(ExperimentalInkCustomBrushApi::class, ExperimentalEncodingApi::class)
class BrushFamilyExtensionsTest {

    private val notGzippedBytes = byteArrayOf(0)

    private val gzippedNotProtoBytes =
        ByteArrayOutputStream().use { byteArrayStream ->
            GZIPOutputStream(byteArrayStream).use { it.write(notGzippedBytes) }
            byteArrayStream.toByteArray()
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
        val encoded =
            ByteArrayOutputStream().use {
                original.encode(it)
                it.toByteArray()
            }
        ByteArrayInputStream(encoded).use { assertThat(BrushFamily.decode(it)).isEqualTo(original) }
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
        val encoded =
            ByteArrayOutputStream().use {
                original.encode(it)
                it.toByteArray()
            }
        val decodedBrushFamily = ByteArrayInputStream(encoded).use { BrushFamily.decode(it) }
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
        val encoded =
            ByteArrayOutputStream().use {
                original.encode(it)
                it.toByteArray()
            }
        ByteArrayInputStream(encoded).use {
            assertFailsWith<IllegalArgumentException> {
                BrushFamily.decode(it, maxVersion = Version.V0_JETPACK1_0_0)
            }
        }
    }

    @Test
    fun decode_notGzippedBytes_throws() {
        assertFailsWith<IOException> {
            @Suppress("CheckReturnValue")
            ByteArrayInputStream(notGzippedBytes).use { BrushFamily.decode(it) }
        }
    }

    @Test
    fun decode_gzippedNotProtoBytes_throws() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                @Suppress("CheckReturnValue")
                ByteArrayInputStream(gzippedNotProtoBytes).use { BrushFamily.decode(it) }
            }
        assertThat(exception).hasMessageThat().contains("Failed to parse ink.proto.BrushFamily")
    }

    @Test
    fun decode_gzippedInvalidProtoBytes_throws() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                @Suppress("CheckReturnValue")
                ByteArrayInputStream(gzippedInvalidProtoBytes).use { BrushFamily.decode(it) }
            }
        assertThat(exception).hasMessageThat().contains("particle_gap_duration")
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
    fun encode_decode_roundTrip_maxVersion_rejectsHigherVersion_staticApi() {
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
        val encoded =
            ByteArrayOutputStream().use {
                original.encode(it)
                it.toByteArray()
            }
        ByteArrayInputStream(encoded).use {
            assertFailsWith<IllegalArgumentException> {
                BrushFamilySerialization.decode(it, maxVersion = Version.V0_JETPACK1_0_0)
            }
        }
    }

    @Test
    fun decode_notGzippedBytes_throws_staticApi() {
        assertFailsWith<IOException> {
            @Suppress("CheckReturnValue")
            ByteArrayInputStream(notGzippedBytes).use { BrushFamilySerialization.decode(it) }
        }
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

        val encoded =
            ByteArrayOutputStream().use {
                families.encodeMultiple(it)
                it.toByteArray()
            }

        ByteArrayInputStream(encoded).use {
            assertFailsWith<IllegalArgumentException> {
                BrushFamily.decodeMultiple(it, maxVersion = Version.V0_JETPACK1_0_0)
            }
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

        val encoded =
            ByteArrayOutputStream().use {
                families.encodeMultiple(it)
                it.toByteArray()
            }

        ByteArrayInputStream(encoded).use {
            val decoded = BrushFamily.decode(it, maxVersion = Version.V0_JETPACK1_0_0)
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
    }
}
