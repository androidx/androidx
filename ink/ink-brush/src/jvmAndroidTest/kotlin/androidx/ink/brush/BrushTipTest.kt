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

import androidx.ink.geometry.Angle
import com.google.common.truth.Truth.assertThat
import kotlin.IllegalArgumentException
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalInkCustomBrushApi::class)
@RunWith(JUnit4::class)
class BrushTipTest {
    /** Brush behavior with every field different from default values. */
    private val customBehavior =
        BrushBehavior(
            source = BrushBehavior.Source.TILT_IN_RADIANS,
            target = BrushBehavior.Target.HEIGHT_MULTIPLIER,
            sourceValueRangeLowerBound = 0.2f,
            sourceValueRangeUpperBound = .8f,
            targetModifierRangeLowerBound = 1.1f,
            targetModifierRangeUpperBound = 1.7f,
            sourceOutOfRangeBehavior = BrushBehavior.OutOfRange.MIRROR,
            responseCurve = EasingFunction.Predefined.EASE_IN_OUT,
            responseTimeMillis = 1L,
            enabledToolTypes = setOf(InputToolType.STYLUS),
            isFallbackFor = BrushBehavior.OptionalInputProperty.TILT_X_AND_Y,
        )

    @Test
    fun constructor_returnsExpectedValues() {
        val brushTip = BrushTip()
        assertThat(brushTip.scaleX).isEqualTo(1f)
        assertThat(brushTip.scaleY).isEqualTo(1f)
        assertThat(brushTip.cornerRounding).isEqualTo(1f)
        assertThat(brushTip.slant).isEqualTo(Angle.ZERO)
        assertThat(brushTip.pinch).isEqualTo(0.0f)
        assertThat(brushTip.rotation).isEqualTo(Angle.ZERO)
        assertThat(brushTip.opacityMultiplier).isEqualTo(1.0f)
        assertThat(brushTip.particleGapDistanceScale).isEqualTo(0.0f)
        assertThat(brushTip.particleGapDurationMillis).isEqualTo(0L)
    }

    @Test
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
    fun constructor_withInvalidSlant_throws() {
        val nanError = assertFailsWith<IllegalArgumentException> { BrushTip(slant = Float.NaN) }
        assertThat(nanError).hasMessageThat().contains("slant")
        assertThat(nanError).hasMessageThat().contains("finite")

        val lowError =
            assertFailsWith<IllegalArgumentException> { BrushTip(slant = -Angle.HALF_TURN_RADIANS) }
        assertThat(lowError).hasMessageThat().contains("slant")
        assertThat(lowError).hasMessageThat().contains("interval [-pi/2, pi/2]")

        val highError =
            assertFailsWith<IllegalArgumentException> { BrushTip(slant = Angle.HALF_TURN_RADIANS) }
        assertThat(highError).hasMessageThat().contains("slant")
        assertThat(highError).hasMessageThat().contains("interval [-pi/2, pi/2]")
    }

    @Test
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
    fun constructor_withInvalidOpacitiyMultiplier_throws() {
        val nanError =
            assertFailsWith<IllegalArgumentException> { BrushTip(opacityMultiplier = Float.NaN) }
        assertThat(nanError).hasMessageThat().contains("opacity_multiplier")
        assertThat(nanError).hasMessageThat().contains("interval [0, 2]")

        val lowError =
            assertFailsWith<IllegalArgumentException> { BrushTip(opacityMultiplier = -0.1f) }
        assertThat(lowError).hasMessageThat().contains("opacity_multiplier")
        assertThat(lowError).hasMessageThat().contains("interval [0, 2]")

        val highError =
            assertFailsWith<IllegalArgumentException> { BrushTip(opacityMultiplier = 2.1f) }
        assertThat(highError).hasMessageThat().contains("opacity_multiplier")
        assertThat(highError).hasMessageThat().contains("interval [0, 2]")
    }

    @Test
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
    fun constructor_withInvalidParticleGapDurationMillis_throws() {
        val negativeError =
            assertFailsWith<IllegalArgumentException> { BrushTip(particleGapDurationMillis = -1L) }
        assertThat(negativeError).hasMessageThat().contains("particle_gap_duration")
        assertThat(negativeError).hasMessageThat().contains("non-negative")
    }

    @Test
    fun constructor_withInvalidRotation_throws() {
        val nanError = assertFailsWith<IllegalArgumentException> { BrushTip(rotation = Float.NaN) }
        assertThat(nanError).hasMessageThat().contains("rotation")
        assertThat(nanError).hasMessageThat().contains("finite")

        val infinityError =
            assertFailsWith<IllegalArgumentException> {
                BrushTip(rotation = Float.POSITIVE_INFINITY)
            }
        assertThat(infinityError).hasMessageThat().contains("rotation")
        assertThat(infinityError).hasMessageThat().contains("finite")
    }

    @Test
    fun hashCode_withIdenticalValues_matches() {
        // same values.
        assertThat(
                BrushTip(
                        1f,
                        2f,
                        0.3f,
                        Angle.QUARTER_TURN_RADIANS,
                        0.4f,
                        Angle.ZERO,
                        0.7f,
                        0.5f,
                        100L,
                        emptyList(),
                    )
                    .hashCode()
            )
            .isEqualTo(
                BrushTip(
                        1f,
                        2f,
                        0.3f,
                        Angle.QUARTER_TURN_RADIANS,
                        0.4f,
                        Angle.ZERO,
                        0.7f,
                        0.5f,
                        100L,
                        emptyList(),
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
        assertThat(brushTip).isNotEqualTo(null)
        assertThat(brushTip).isNotEqualTo(Any())
        assertThat(brushTip).isNotEqualTo(BrushTip(scaleX = 2f))
        assertThat(brushTip).isNotEqualTo(BrushTip(scaleY = 2f))
        assertThat(brushTip).isNotEqualTo(BrushTip(cornerRounding = 0.2f))
        assertThat(brushTip).isNotEqualTo(BrushTip(slant = Angle.QUARTER_TURN_RADIANS))
        assertThat(brushTip).isNotEqualTo(BrushTip(pinch = 0.2f))
        assertThat(brushTip).isNotEqualTo(BrushTip(rotation = Angle.HALF_TURN_RADIANS))
        assertThat(brushTip).isNotEqualTo(BrushTip(opacityMultiplier = 0.7f))
        assertThat(brushTip).isNotEqualTo(BrushTip(behaviors = listOf(customBehavior)))
    }

    @Test
    fun toString_returnsExpectedValues() {
        assertThat(BrushTip().toString())
            .isEqualTo(
                "BrushTip(scale=(1.0, 1.0), cornerRounding=1.0, slant=0.0, " +
                    "pinch=0.0, rotation=0.0, opacityMultiplier=1.0, " +
                    "particleGapDistanceScale=0.0, particleGapDurationMillis=0, " +
                    "behaviors=[])"
            )
    }

    @Test
    fun copy_withArguments_createsCopyWithChanges() {
        val tip1 =
            BrushTip(
                scaleX = 2f,
                scaleY = 3f,
                cornerRounding = 0.5f,
                slant = Angle.ZERO,
                pinch = 0.5f,
                rotation = Angle.ZERO,
                opacityMultiplier = 0.7f,
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
                    slant = Angle.ZERO,
                    pinch = 0.5f,
                    rotation = Angle.ZERO,
                    opacityMultiplier = 0.7f,
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
                slant = Angle.ZERO,
                pinch = 0.5f,
                rotation = Angle.ZERO,
                opacityMultiplier = 0.7f,
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
    fun builder_createsExpectedBrushTip() {
        val tip =
            BrushTip.Builder()
                .setScaleX(0.1f)
                .setScaleY(0.2f)
                .setCornerRounding(0.3f)
                .setSlant(0.4f)
                .setPinch(0.5f)
                .setRotation(0.6f)
                .setOpacityMultiplier(0.7f)
                .setParticleGapDistanceScale(0.8f)
                .setParticleGapDurationMillis(9L)
                .setBehaviors(listOf(customBehavior))
                .build()

        assertThat(tip)
            .isEqualTo(
                BrushTip(0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 9L, listOf(customBehavior))
            )
    }

    /**
     * Creates an expected C++ BrushTip with no behaviors and returns true if every property of the
     * Kotlin BrushTip's JNI-created C++ counterpart is equivalent to the expected C++ BrushTip.
     */
    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    private external fun matchesNativeNoBehaviorTip(nativePointerToActualBrushTip: Long): Boolean

    /**
     * Creates an expected C++ BrushTip with a single behavior and returns true if every property of
     * the Kotlin BrushTip's JNI-created C++ counterpart is equivalent to the expected C++ BrushTip.
     */
    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    private external fun matchesNativeSingleBehaviorTip(
        nativePointerToActualBrushTip: Long
    ): Boolean

    /**
     * Creates an expected C++ BrushTip with multiple behaviors and returns true if every property
     * of the Kotlin BrushTip's JNI-created C++ counterpart is equivalent to the expected C++
     * BrushTip.
     */
    // TODO: b/355248266 - @Keep must go in Proguard config file instead.
    private external fun matchesNativeMultiBehaviorTip(nativePointerToActualBrushTip: Long): Boolean
}
