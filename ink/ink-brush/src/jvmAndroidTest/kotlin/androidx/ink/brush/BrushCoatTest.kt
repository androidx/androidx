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

import androidx.ink.nativeloader.UsedByNative
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalInkCustomBrushApi::class)
@RunWith(JUnit4::class)
class BrushCoatTest {
    @Test
    fun constructor_withValidArguments_returnsABrushCoat() {
        assertThat(BrushCoat(customTip, customPaint)).isNotNull()
    }

    @Test
    fun constructor_withDefaultArguments_returnsABrushCoat() {
        assertThat(BrushCoat(BrushTip(), BrushPaint())).isNotNull()
    }

    @Test
    fun hashCode_withIdenticalValues_matches() {
        assertThat(newCustomBrushCoat().hashCode()).isEqualTo(newCustomBrushCoat().hashCode())
    }

    @Test
    fun equals_comparesValues() {
        val brushCoat = BrushCoat(customTip, customPaint)
        val differentTip = BrushTip()
        val differentPaint = BrushPaint()

        // same values are equal.
        assertThat(brushCoat).isEqualTo(BrushCoat(customTip, customPaint))

        // different values are not equal.
        assertThat(brushCoat).isNotEqualTo(null)
        assertThat(brushCoat).isNotEqualTo(Any())
        assertThat(brushCoat).isNotEqualTo(brushCoat.copy(tip = differentTip))
        assertThat(brushCoat).isNotEqualTo(brushCoat.copy(paint = differentPaint))
    }

    @Test
    fun toString_returnsExpectedValues() {
        assertThat(BrushCoat().toString())
            .isEqualTo(
                "BrushCoat(tips=[BrushTip(scale=(1.0, 1.0), " +
                    "cornerRounding=1.0, slant=0.0, pinch=0.0, rotation=0.0, opacityMultiplier=1.0, " +
                    "particleGapDistanceScale=0.0, particleGapDurationMillis=0, behaviors=[])], " +
                    "paint=BrushPaint(textureLayers=[]))"
            )
    }

    @Test
    fun copy_whenSameContents_returnsSameInstance() {
        val customCoat = BrushCoat(customTip, customPaint)

        // A pure copy returns `this`.
        val copy = customCoat.copy()
        assertThat(copy).isSameInstanceAs(customCoat)
    }

    @Test
    fun copy_withArguments_createsCopyWithChanges() {
        val brushCoat = BrushCoat(customTip, customPaint)
        val differentTip = BrushTip()
        val differentPaint = BrushPaint()

        assertThat(brushCoat.copy(tip = differentTip))
            .isEqualTo(BrushCoat(differentTip, customPaint))
        assertThat(brushCoat.copy(paint = differentPaint))
            .isEqualTo(BrushCoat(customTip, differentPaint))
    }

    @Test
    fun builder_createsExpectedBrushCoat() {
        val coat = BrushCoat.Builder().setTip(customTip).setPaint(customPaint).build()
        assertThat(coat).isEqualTo(BrushCoat(customTip, customPaint))
    }

    /**
     * Creates an expected C++ BrushCoat with defaults and returns true if every property of the
     * Kotlin BrushCoat's JNI-created C++ counterpart is equivalent to the expected C++ BrushCoat.
     */
    @UsedByNative private external fun matchesDefaultCoat(brushCoatNativePointer: Long): Boolean

    /**
     * Creates an expected C++ BrushCoat with custom values and returns true if every property of
     * the Kotlin BrushCoat's JNI-created C++ counterpart is equivalent to the expected C++
     * BrushCoat.
     */
    @UsedByNative
    private external fun matchesMultiBehaviorTipCoat(brushCoatNativePointer: Long): Boolean

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

    /** Brush tip with every field different from default values and non-empty behaviors. */
    private val customTip =
        BrushTip(
            scaleX = 0.1f,
            scaleY = 0.2f,
            cornerRounding = 0.3f,
            slant = 0.4f,
            pinch = 0.5f,
            rotation = 0.6f,
            opacityMultiplier = 0.7f,
            particleGapDistanceScale = 0.8f,
            particleGapDurationMillis = 9L,
            listOf<BrushBehavior>(customBehavior),
        )

    /**
     * Brush Paint with every field different from default values, including non-empty texture
     * layers.
     */
    private val customPaint =
        BrushPaint(
            listOf(
                BrushPaint.TextureLayer(
                    colorTextureUri = "ink://ink/texture:test-one",
                    sizeX = 123.45F,
                    sizeY = 678.90F,
                    offsetX = 0.123f,
                    offsetY = 0.678f,
                    rotation = 0.1f,
                    opacity = 0.123f,
                    BrushPaint.TextureSizeUnit.STROKE_COORDINATES,
                    BrushPaint.TextureOrigin.STROKE_SPACE_ORIGIN,
                    BrushPaint.TextureMapping.TILING,
                ),
                BrushPaint.TextureLayer(
                    colorTextureUri = "ink://ink/texture:test-two",
                    sizeX = 256F,
                    sizeY = 256F,
                    offsetX = 0.456f,
                    offsetY = 0.567f,
                    rotation = 0.2f,
                    opacity = 0.987f,
                    BrushPaint.TextureSizeUnit.STROKE_COORDINATES,
                    BrushPaint.TextureOrigin.STROKE_SPACE_ORIGIN,
                    BrushPaint.TextureMapping.TILING,
                ),
            )
        )

    /** Brush Coat with every field different from default values. */
    private fun newCustomBrushCoat(): BrushCoat = BrushCoat(customTip, customPaint)
}
