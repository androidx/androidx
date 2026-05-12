/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.brush

import androidx.ink.brush.behavior.DampingNode
import androidx.ink.brush.behavior.EasingFunction
import androidx.ink.brush.behavior.OutOfRange
import androidx.ink.brush.behavior.ProgressDomain
import androidx.ink.brush.behavior.ResponseNode
import androidx.ink.brush.behavior.SourceNode
import androidx.ink.brush.behavior.SourceNode.Source
import androidx.ink.brush.behavior.TargetNode
import androidx.ink.brush.behavior.TargetNode.Target
import androidx.ink.brush.behavior.ToolTypeFilterNode
import androidx.ink.geometry.Angle
import androidx.ink.nativeloader.testing.awaitNativePointerCleanupAfter
import androidx.kruth.assertThat
import kotlin.test.Test
import kotlin.test.assertFailsWith

class BrushTipTest {
    private val customBehavior =
        BrushBehavior(
            TargetNode(
                target = Target.HEIGHT_MULTIPLIER,
                targetModifierRangeStart = 1.1f,
                targetModifierRangeEnd = 1.7f,
                input =
                    DampingNode(
                        dampingSource = ProgressDomain.TIME_IN_SECONDS,
                        dampingGap = 0.001f,
                        input =
                            ResponseNode(
                                responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
                                input =
                                    ToolTypeFilterNode(
                                        enabledToolTypes = setOf(InputToolType.STYLUS),
                                        input =
                                            SourceNode(
                                                source = Source.TILT_IN_RADIANS,
                                                sourceValueRangeStart = 0.2f,
                                                sourceValueRangeEnd = .8f,
                                                sourceOutOfRangeBehavior = OutOfRange.MIRROR,
                                            ),
                                    ),
                            ),
                    ),
            )
        )

    @Test
    fun brushTipNativePointers_cleanedUpWhenOutOfScope() {
        awaitNativePointerCleanupAfter {
            val unused = BrushTip(behaviors = listOf(customBehavior))
        }
    }

    @Test
    fun brushTip_usesPassedInBehaviors() {
        val brushTip = BrushTip(behaviors = listOf(customBehavior))
        assertThat(brushTip.behaviors).hasSize(1)
        assertThat(brushTip.behaviors[0]).isSameInstanceAs(customBehavior)
    }

    @Test
    @Suppress("DEPRECATION") // Testing deprecated fields.
    fun constructor_returnsExpectedValues() {
        val brushTip = BrushTip()
        assertThat(brushTip.scaleX).isEqualTo(1f)
        assertThat(brushTip.scaleY).isEqualTo(1f)
        assertThat(brushTip.cornerRounding).isEqualTo(1f)
        assertThat(brushTip.slantDegrees).isEqualTo(Angle.ZERO_DEGREES)
        assertThat(brushTip.pinch).isEqualTo(0.0f)
        assertThat(brushTip.rotationDegrees).isEqualTo(Angle.ZERO_DEGREES)
        assertThat(brushTip.particleGapDistanceScale).isEqualTo(0.0f)
        assertThat(brushTip.particleGapDurationMillis).isEqualTo(0L)
    }

    @Test
    @Suppress("Range") // Testing error cases.
    fun constructor_withInvalidScaleX_throws() {
        val infinityError =
            assertFailsWith<IllegalArgumentException> { BrushTip(scaleX = Float.POSITIVE_INFINITY) }
        assertThat(infinityError).hasMessageThat().contains("scale")
        assertThat(infinityError).hasMessageThat().contains("finite")

        val nanError = assertFailsWith<IllegalArgumentException> { BrushTip(scaleX = Float.NaN) }
        assertThat(nanError).hasMessageThat().contains("scale")
        assertThat(nanError).hasMessageThat().contains("finite")

        val negativeError = assertFailsWith<IllegalArgumentException> { BrushTip(scaleX = -1.0f) }
        assertThat(negativeError).hasMessageThat().contains("scale")
        assertThat(negativeError).hasMessageThat().contains("non-negative")
    }

    @Test
    @Suppress("Range") // Testing error cases.
    fun constructor_withInvalidScaleY_throws() {
        val infinityError =
            assertFailsWith<IllegalArgumentException> { BrushTip(scaleY = Float.POSITIVE_INFINITY) }
        assertThat(infinityError).hasMessageThat().contains("scale")
        assertThat(infinityError).hasMessageThat().contains("finite")

        val nanError = assertFailsWith<IllegalArgumentException> { BrushTip(scaleY = Float.NaN) }
        assertThat(nanError).hasMessageThat().contains("scale")
        assertThat(nanError).hasMessageThat().contains("finite")

        val negativeError = assertFailsWith<IllegalArgumentException> { BrushTip(scaleY = -1.0f) }
        assertThat(negativeError).hasMessageThat().contains("scale")
        assertThat(negativeError).hasMessageThat().contains("non-negative")
    }

    @Test
    fun constructor_withZeroScale_throws() {
        val zeroError =
            assertFailsWith<IllegalArgumentException> { BrushTip(scaleX = 0f, scaleY = 0f) }
        assertThat(zeroError).hasMessageThat().contains("at least one value must be positive.")
    }

    @Test
    @Suppress("Range") // Testing error cases.
    fun constructor_withInvalidCornerRounding_throws() {
        val nanError =
            assertFailsWith<IllegalArgumentException> { BrushTip(cornerRounding = Float.NaN) }
        assertThat(nanError).hasMessageThat().contains("corner_rounding")
        assertThat(nanError).hasMessageThat().contains("in the interval [0, 1]")

        val lowError =
            assertFailsWith<IllegalArgumentException> { BrushTip(cornerRounding = -0.5f) }
        assertThat(lowError).hasMessageThat().contains("corner_rounding")
        assertThat(lowError).hasMessageThat().contains("in the interval [0, 1]")

        val highError =
            assertFailsWith<IllegalArgumentException> { BrushTip(cornerRounding = 1.1f) }
        assertThat(highError).hasMessageThat().contains("corner_rounding")
        assertThat(highError).hasMessageThat().contains("in the interval [0, 1]")
    }

    @Test
    @Suppress("Range") // Testing error cases.
    fun constructor_withInvalidSlant_throws() {
        val nanError =
            assertFailsWith<IllegalArgumentException> { BrushTip(slantDegrees = Float.NaN) }
        assertThat(nanError).hasMessageThat().contains("slant")
        assertThat(nanError).hasMessageThat().contains("finite")

        val lowError =
            assertFailsWith<IllegalArgumentException> {
                BrushTip(slantDegrees = -Angle.HALF_TURN_DEGREES)
            }
        assertThat(lowError).hasMessageThat().contains("slant")
        assertThat(lowError)
            .hasMessageThat()
            .contains("interval [-π/2, π/2] radians ([-90, 90] degrees)")

        val highError =
            assertFailsWith<IllegalArgumentException> {
                BrushTip(slantDegrees = Angle.HALF_TURN_DEGREES)
            }
        assertThat(highError).hasMessageThat().contains("slant")
        assertThat(highError)
            .hasMessageThat()
            .contains("interval [-π/2, π/2] radians ([-90, 90] degrees)")
    }

    @Test
    @Suppress("Range") // Testing error cases.
    fun constructor_withInvalidPinch_throws() {
        val nanError = assertFailsWith<IllegalArgumentException> { BrushTip(pinch = Float.NaN) }
        assertThat(nanError).hasMessageThat().contains("pinch")
        assertThat(nanError).hasMessageThat().contains("interval [0, 1]")

        val lowError = assertFailsWith<IllegalArgumentException> { BrushTip(pinch = -0.1f) }
        assertThat(lowError).hasMessageThat().contains("pinch")
        assertThat(lowError).hasMessageThat().contains("interval [0, 1]")

        val highError = assertFailsWith<IllegalArgumentException> { BrushTip(pinch = 1.1f) }
        assertThat(highError).hasMessageThat().contains("pinch")
        assertThat(highError).hasMessageThat().contains("interval [0, 1]")
    }

    @Test
    @Suppress("Range") // Testing error cases.
    fun constructor_withInvalidParticleGapDistanceScale_throws() {
        val infinityError =
            assertFailsWith<IllegalArgumentException> {
                BrushTip(particleGapDistanceScale = Float.POSITIVE_INFINITY)
            }
        assertThat(infinityError).hasMessageThat().contains("particle_gap_distance_scale")
        assertThat(infinityError).hasMessageThat().contains("finite")

        val nanError =
            assertFailsWith<IllegalArgumentException> {
                BrushTip(particleGapDistanceScale = Float.NaN)
            }
        assertThat(nanError).hasMessageThat().contains("particle_gap_distance_scale")
        assertThat(nanError).hasMessageThat().contains("finite")

        val negativeError =
            assertFailsWith<IllegalArgumentException> { BrushTip(particleGapDistanceScale = -1.0f) }
        assertThat(negativeError).hasMessageThat().contains("particle_gap_distance_scale")
        assertThat(negativeError).hasMessageThat().contains("non-negative")
    }

    @Test
    @Suppress("Range") // Testing error cases.
    fun constructor_withInvalidParticleGapDurationMillis_throws() {
        val negativeError =
            assertFailsWith<IllegalArgumentException> { BrushTip(particleGapDurationMillis = -1L) }
        assertThat(negativeError).hasMessageThat().contains("particle_gap_duration")
        assertThat(negativeError).hasMessageThat().contains("non-negative")
    }

    @Test
    @Suppress("Range") // Testing error cases.
    fun constructor_withInvalidRotation_throws() {
        val nanError =
            assertFailsWith<IllegalArgumentException> { BrushTip(rotationDegrees = Float.NaN) }
        assertThat(nanError).hasMessageThat().contains("rotation")
        assertThat(nanError).hasMessageThat().contains("finite")

        val infinityError =
            assertFailsWith<IllegalArgumentException> {
                BrushTip(rotationDegrees = Float.POSITIVE_INFINITY)
            }
        assertThat(infinityError).hasMessageThat().contains("rotation")
        assertThat(infinityError).hasMessageThat().contains("finite")
    }

    @Test
    fun hashCode_withIdenticalValues_matches() {
        // same values.
        assertThat(
                BrushTip(
                        scaleX = 1f,
                        scaleY = 2f,
                        cornerRounding = 0.3f,
                        slantDegrees = Angle.QUARTER_TURN_DEGREES,
                        pinch = 0.4f,
                        rotationDegrees = Angle.ZERO_DEGREES,
                        particleGapDistanceScale = 0.5f,
                        particleGapDurationMillis = 100L,
                        behaviors = emptyList(),
                    )
                    .hashCode()
            )
            .isEqualTo(
                BrushTip(
                        scaleX = 1f,
                        scaleY = 2f,
                        cornerRounding = 0.3f,
                        slantDegrees = Angle.QUARTER_TURN_DEGREES,
                        pinch = 0.4f,
                        rotationDegrees = Angle.ZERO_DEGREES,
                        particleGapDistanceScale = 0.5f,
                        particleGapDurationMillis = 100L,
                        behaviors = emptyList(),
                    )
                    .hashCode()
            )
    }

    @Test
    fun equals_comparesValues() {
        val brushTip = BrushTip()
        // same values.
        assertThat(brushTip).isEqualTo(BrushTip())

        // different values.
        assertThat(brushTip).isNotNull()
        assertThat(brushTip).isNotEqualTo(Any())
        assertThat(brushTip).isNotEqualTo(BrushTip(scaleX = 2f))
        assertThat(brushTip).isNotEqualTo(BrushTip(scaleY = 2f))
        assertThat(brushTip).isNotEqualTo(BrushTip(cornerRounding = 0.2f))
        assertThat(brushTip).isNotEqualTo(BrushTip(slantDegrees = Angle.QUARTER_TURN_DEGREES))
        assertThat(brushTip).isNotEqualTo(BrushTip(pinch = 0.2f))
        assertThat(brushTip).isNotEqualTo(BrushTip(rotationDegrees = Angle.HALF_TURN_DEGREES))
        assertThat(brushTip).isNotEqualTo(BrushTip(behaviors = listOf(customBehavior)))
    }

    @Test
    fun toString_returnsExpectedValues() {
        assertThat(BrushTip().toString())
            .isEqualTo(
                "BrushTip(scale=(1.0, 1.0), cornerRounding=1.0, slantDegrees=0.0, " +
                    "pinch=0.0, rotationDegrees=0.0, particleGapDistanceScale=0.0, " +
                    "particleGapDurationMillis=0, behaviors=[])"
            )
    }

    @Test
    fun copy_withArguments_createsCopyWithChanges() {
        val tip1 =
            BrushTip(
                scaleX = 2f,
                scaleY = 3f,
                cornerRounding = 0.5f,
                slantDegrees = Angle.ZERO_DEGREES,
                pinch = 0.5f,
                rotationDegrees = Angle.ZERO_DEGREES,
                particleGapDistanceScale = 0.8f,
                particleGapDurationMillis = 9L,
                behaviors = listOf(customBehavior),
            )

        assertThat(tip1.copy(scaleX = 3f))
            .isEqualTo(
                BrushTip(
                    scaleX = 3f,
                    scaleY = 3f,
                    cornerRounding = 0.5f,
                    slantDegrees = Angle.ZERO_DEGREES,
                    pinch = 0.5f,
                    rotationDegrees = Angle.ZERO_DEGREES,
                    particleGapDistanceScale = 0.8f,
                    particleGapDurationMillis = 9L,
                    behaviors = listOf(customBehavior),
                )
            )
    }

    @Test
    fun copy_createsCopy() {
        val tip1 =
            BrushTip(
                scaleX = 3f,
                scaleY = 3f,
                cornerRounding = 0.5f,
                slantDegrees = Angle.ZERO_DEGREES,
                pinch = 0.5f,
                rotationDegrees = Angle.ZERO_DEGREES,
                particleGapDistanceScale = 0.8f,
                particleGapDurationMillis = 9L,
                behaviors = listOf(customBehavior),
            )

        val tip2 = tip1.copy()

        assertThat(tip2).isEqualTo(tip1)
        assertThat(tip2.nativePointer).isNotEqualTo(tip1.nativePointer)
        assertThat(tip2).isNotSameInstanceAs(tip1)
    }

    @Test
    @Suppress("DEPRECATION") // Testing deprecated setters.
    fun builder_createsExpectedBrushTip() {
        val tip =
            BrushTip.Builder()
                .setScaleX(0.1f)
                .setScaleY(0.2f)
                .setCornerRounding(0.3f)
                .setSlantDegrees(0.4f)
                .setPinch(0.5f)
                .setRotationDegrees(0.6f)
                .setParticleGapDistanceScale(0.8f)
                .setParticleGapDurationMillis(9L)
                .setBehaviors(listOf(customBehavior))
                .build()

        assertThat(tip)
            .isEqualTo(
                BrushTip(
                    scaleX = 0.1f,
                    scaleY = 0.2f,
                    cornerRounding = 0.3f,
                    slantDegrees = 0.4f,
                    pinch = 0.5f,
                    rotationDegrees = 0.6f,
                    particleGapDistanceScale = 0.8f,
                    particleGapDurationMillis = 9L,
                    behaviors = listOf(customBehavior),
                )
            )
    }
}
