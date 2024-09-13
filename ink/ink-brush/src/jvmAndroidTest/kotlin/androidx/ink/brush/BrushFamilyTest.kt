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

import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalInkCustomBrushApi::class)
@RunWith(JUnit4::class)
class BrushFamilyTest {
    @Test
    fun constructor_withValidArguments_returnsABrushFamily() {
        assertThat(BrushFamily(customTip, customPaint, customUri)).isNotNull()
    }

    @Test
    fun constructor_withDefaultArguments_returnsABrushFamily() {
        assertThat(BrushFamily(BrushTip(), BrushPaint(), uri = null)).isNotNull()
        assertThat(BrushFamily(BrushTip(), BrushPaint(), uri = "")).isNotNull()
    }

    @Test
    fun constructor_withBadUri_throws() {
        assertFailsWith<IllegalArgumentException> { BrushFamily(customTip, customPaint, "baduri") }
    }

    @Test
    fun hashCode_withIdenticalValues_matches() {
        assertThat(newCustomBrushFamily().hashCode()).isEqualTo(newCustomBrushFamily().hashCode())
    }

    @Test
    fun inputModelHashCode_isSameForIdenticalModels() {
        assertThat(BrushFamily.LEGACY_SPRING_MODEL.hashCode())
            .isEqualTo(BrushFamily.LEGACY_SPRING_MODEL.hashCode())
        assertThat(BrushFamily.SPRING_MODEL.hashCode())
            .isEqualTo(BrushFamily.SPRING_MODEL.hashCode())

        assertThat(BrushFamily.LEGACY_SPRING_MODEL.hashCode())
            .isNotEqualTo(BrushFamily.SPRING_MODEL.hashCode())
    }

    @Test
    fun equals_comparesValues() {
        val brushFamily =
            BrushFamily(customTip, customPaint, customUri, BrushFamily.LEGACY_SPRING_MODEL)
        val differentCoat = BrushCoat(BrushTip(), BrushPaint())
        val differentUri = null

        // same values are equal.
        assertThat(brushFamily)
            .isEqualTo(
                BrushFamily(customTip, customPaint, customUri, BrushFamily.LEGACY_SPRING_MODEL)
            )

        // different values are not equal.
        assertThat(brushFamily).isNotEqualTo(null)
        assertThat(brushFamily).isNotEqualTo(Any())
        assertThat(brushFamily).isNotEqualTo(brushFamily.copy(coat = differentCoat))
        assertThat(brushFamily).isNotEqualTo(brushFamily.copy(uri = differentUri))
        assertThat(brushFamily)
            .isNotEqualTo(brushFamily.copy(inputModel = BrushFamily.SPRING_MODEL))
    }

    @Test
    fun inputModelEquals_comparesModels() {
        assertThat(BrushFamily.LEGACY_SPRING_MODEL).isEqualTo(BrushFamily.LEGACY_SPRING_MODEL)
        assertThat(BrushFamily.SPRING_MODEL).isEqualTo(BrushFamily.SPRING_MODEL)

        assertThat(BrushFamily.LEGACY_SPRING_MODEL).isNotEqualTo(BrushFamily.SPRING_MODEL)
        assertThat(BrushFamily.SPRING_MODEL).isNotEqualTo(BrushFamily.LEGACY_SPRING_MODEL)
    }

    @Test
    fun toString_returnsExpectedValues() {
        assertThat(BrushFamily(inputModel = BrushFamily.LEGACY_SPRING_MODEL).toString())
            .isEqualTo(
                "BrushFamily(coats=[BrushCoat(tips=[BrushTip(scale=(1.0, 1.0), " +
                    "cornerRounding=1.0, slant=0.0, pinch=0.0, rotation=0.0, opacityMultiplier=1.0, " +
                    "particleGapDistanceScale=0.0, particleGapDurationMillis=0, " +
                    "behaviors=[])], paint=BrushPaint(textureLayers=[]))], uri=null, " +
                    "inputModel=LegacySpringModel)"
            )
    }

    @Test
    fun inputModelToString_returnsExpectedValues() {
        assertThat(BrushFamily.LEGACY_SPRING_MODEL.toString()).isEqualTo("LegacySpringModel")
        assertThat(BrushFamily.SPRING_MODEL.toString()).isEqualTo("SpringModel")
    }

    @Test
    fun copy_whenSameContents_returnsSameInstance() {
        val customFamily = BrushFamily(customTip, customPaint, customUri)

        // A pure copy returns `this`.
        val copy = customFamily.copy()
        assertThat(copy).isSameInstanceAs(customFamily)
    }

    @Test
    fun copy_withArguments_createsCopyWithChanges() {
        val brushFamily = BrushFamily(customTip, customPaint, customUri)
        val differentCoats = listOf(BrushCoat(BrushTip(), BrushPaint()))
        val differentUri = null

        assertThat(brushFamily.copy(coats = differentCoats))
            .isEqualTo(BrushFamily(differentCoats, customUri))
        assertThat(brushFamily.copy(uri = differentUri))
            .isEqualTo(BrushFamily(customTip, customPaint, differentUri))
    }

    @Test
    fun builder_createsExpectedBrushFamily() {
        val family = BrushFamily.Builder().setCoat(customTip, customPaint).setUri(customUri).build()
        assertThat(family).isEqualTo(BrushFamily(customTip, customPaint, customUri))
    }

    /**
     * Creates an expected C++ BrushFamily with defaults and returns true if every property of the
     * Kotlin BrushFamily's JNI-created C++ counterpart is equivalent to the expected C++
     * BrushFamily.
     */
    private external fun matchesDefaultFamily(
        brushFamilyNativePointer: Long
    ): Boolean // TODO: b/355248266 - @Keep must go in Proguard config file instead.

    /**
     * Creates an expected C++ BrushFamily with custom values and returns true if every property of
     * the Kotlin BrushFamily's JNI-created C++ counterpart is equivalent to the expected C++
     * BrushFamily.
     */
    private external fun matchesMultiBehaviorTipFamily(
        brushFamilyNativePointer: Long
    ): Boolean // TODO: b/355248266 - @Keep must go in Proguard config file instead.

    private val customUri = "/brush-family:inkpen:1"

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
            listOf(customBehavior),
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

    /** Brush Family with every field different from default values. */
    private fun newCustomBrushFamily(): BrushFamily = BrushFamily(customTip, customPaint, customUri)
}
