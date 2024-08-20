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

import androidx.ink.brush.color.Color
import androidx.ink.brush.color.colorspace.ColorSpaces
import androidx.ink.brush.color.toArgb
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalInkCustomBrushApi::class)
@RunWith(JUnit4::class)
class BrushTest {
    private val size = 10F
    private val epsilon = 1F
    private val color = Color(red = 230, green = 115, blue = 140, alpha = 255)
    private val family = BrushFamily(uri = "/brush-family:inkpen:1")

    @Test
    fun constructor_withValidArguments_returnsABrush() {
        val brush = Brush.withColorLong(family, color.value.toLong(), size, epsilon)
        assertThat(brush).isNotNull()
        assertThat(brush.family).isEqualTo(family)
        assertThat(brush.colorLong).isEqualTo(color.value.toLong())
        assertThat(brush.colorInt).isEqualTo(color.toArgb())
        assertThat(brush.colorLong).isEqualTo(color.value.toLong())
        assertThat(brush.size).isEqualTo(size)
        assertThat(brush.epsilon).isEqualTo(epsilon)
    }

    @Test
    fun constructor_withBadSize_willThrow() {
        assertFailsWith<IllegalArgumentException> {
            Brush(family, color, -2F, epsilon) // non-positive size.
        }

        assertFailsWith<IllegalArgumentException> {
            Brush(family, color, Float.POSITIVE_INFINITY, epsilon) // non-finite size.
        }

        assertFailsWith<IllegalArgumentException> {
            Brush(family, color, Float.NaN, epsilon) // non-finite size.
        }
    }

    @Test
    fun constructor_withBadEpsilon_willThrow() {
        assertFailsWith<IllegalArgumentException> {
            Brush(family, color, size, -2F) // non-positive epsilon.
        }

        assertFailsWith<IllegalArgumentException> {
            Brush(family, color, size, Float.POSITIVE_INFINITY) // non-finite epsilon.
        }

        assertFailsWith<IllegalArgumentException> {
            Brush(family, color, size, Float.NaN) // non-finite epsilon.
        }
    }

    @Test
    fun colorAccessors_areAllEquivalent() {
        val color = Color(red = 230, green = 115, blue = 140, alpha = 196)
        val brush = Brush.withColorLong(family, color.value.toLong(), size, epsilon)

        assertThat(brush.colorInt).isEqualTo(color.toArgb())
        assertThat(brush.colorLong).isEqualTo(color.value.toLong())
    }

    @Test
    fun withColorIntArgb_withLowAlpha_returnsBrushWithCorrectColor() {
        val brush = Brush.withColorIntArgb(family, 0x12345678, size, epsilon)
        assertThat(brush.colorInt).isEqualTo(0x12345678)
    }

    @Test
    fun withColorIntArgb_withHighAlpha_returnsBrushWithCorrectColor() {
        val brush = Brush.withColorIntArgb(family, 0xAA123456.toInt(), size, epsilon)
        assertThat(brush.colorInt).isEqualTo(0xAA123456.toInt())
    }

    @Test
    fun withColorLong_returnsBrushWithCorrectColor() {
        val colorLong = Color(0.9f, 0.45f, 0.55f, 0.15f, ColorSpaces.DisplayP3).value.toLong()
        val brush = Brush.withColorLong(family, colorLong, size, epsilon)
        assertThat(brush.colorLong).isEqualTo(colorLong)
    }

    @Test
    fun withColorLong_inUnsupportedColorSpace_returnsBrushWithConvertedColor() {
        val colorLong = Color(0.9f, 0.45f, 0.55f, 0.15f, ColorSpaces.AdobeRgb).value.toLong()
        val brush = Brush.withColorLong(family, colorLong, size, epsilon)

        val expectedColor = Color(colorLong.toULong()).convert(ColorSpaces.DisplayP3)
        assertThat(brush.colorLong).isEqualTo(expectedColor.value.toLong())
        assertThat(brush.colorInt).isEqualTo(expectedColor.toArgb())
    }

    @Test
    fun equals_returnsTrueForIdenticalBrushes() {
        val brush = Brush(family, color, size, epsilon)
        val otherBrush = Brush(family, color, size, epsilon)
        assertThat(brush == brush).isTrue()
        assertThat(brush == otherBrush).isTrue()
        assertThat(otherBrush == brush).isTrue()
    }

    @Test
    fun hashCode_isEqualForIdenticalBrushes() {
        val brush = Brush(family, color, size, epsilon)
        val otherBrush = Brush(family, color, size, epsilon)
        assertThat(brush == brush).isTrue()
        assertThat(brush == otherBrush).isTrue()
        assertThat(otherBrush == brush).isTrue()
    }

    @Test
    fun equals_returnsFalseIfAnyFieldsDiffer() {
        val brush = Brush(family, color, size, epsilon)

        val differentFamilyBrush =
            Brush(BrushFamily(uri = "/brush-family:pencil:1"), color, size, epsilon)
        assertThat(brush == differentFamilyBrush).isFalse()
        assertThat(differentFamilyBrush == brush).isFalse()
        assertThat(brush != differentFamilyBrush).isTrue()
        assertThat(differentFamilyBrush != brush).isTrue()

        val otherColor =
            Color(red = 1F, green = 0F, blue = 0F, alpha = 1F, colorSpace = ColorSpaces.DisplayP3)
                .value
                .toLong()
        val differentcolorBrush = Brush.withColorLong(family, otherColor, size, epsilon)
        assertThat(brush == differentcolorBrush).isFalse()
        assertThat(differentcolorBrush == brush).isFalse()
        assertThat(brush != differentcolorBrush).isTrue()
        assertThat(differentcolorBrush != brush).isTrue()

        val differentSizeBrush = Brush(family, color, 9.0f, epsilon)
        assertThat(brush == differentSizeBrush).isFalse()
        assertThat(differentSizeBrush == brush).isFalse()
        assertThat(brush != differentSizeBrush).isTrue()
        assertThat(differentSizeBrush != brush).isTrue()

        val differentEpsilonBrush = Brush(family, color, size, 1.1f)
        assertThat(brush == differentEpsilonBrush).isFalse()
        assertThat(differentEpsilonBrush == brush).isFalse()
        assertThat(brush != differentEpsilonBrush).isTrue()
        assertThat(differentEpsilonBrush != brush).isTrue()
    }

    @Test
    fun hashCode_differsIfAnyFieldsDiffer() {
        val brush = Brush(family, color, size, epsilon)

        val differentFamilyBrush =
            Brush(BrushFamily(uri = "/brush-family:pencil:1"), color, size, epsilon)
        assertThat(differentFamilyBrush.hashCode()).isNotEqualTo(brush.hashCode())

        val otherColor =
            Color(red = 1F, green = 0F, blue = 0F, alpha = 1F, colorSpace = ColorSpaces.DisplayP3)
                .value
                .toLong()
        val differentcolorBrush = Brush.withColorLong(family, otherColor, size, epsilon)
        assertThat(differentcolorBrush.hashCode()).isNotEqualTo(brush.hashCode())

        val differentSizeBrush = Brush(family, color, 9.0f, epsilon)
        assertThat(differentSizeBrush.hashCode()).isNotEqualTo(brush.hashCode())

        val differentEpsilonBrush = Brush(family, color, size, 1.1f)
        assertThat(differentEpsilonBrush.hashCode()).isNotEqualTo(brush.hashCode())
    }

    @Test
    fun copy_returnsTheSameBrush() {
        val originalBrush = buildTestBrush()

        val newBrush = originalBrush.copy()

        // A pure copy returns `this`.
        assertThat(newBrush).isSameInstanceAs(originalBrush)
    }

    @Test
    fun copy_withChangedBrushFamily_returnsCopyWithDifferentBrushFamily() {
        val originalBrush = buildTestBrush()

        val newBrush = originalBrush.copy(family = BrushFamily())

        assertThat(newBrush).isNotEqualTo(originalBrush)
        assertThat(newBrush.family).isNotEqualTo(originalBrush.family)

        // The new brush has the original color, size and epsilon.
        assertThat(newBrush.colorLong).isEqualTo(originalBrush.colorLong)
        assertThat(newBrush.size).isEqualTo(originalBrush.size)
        assertThat(newBrush.epsilon).isEqualTo(originalBrush.epsilon)
    }

    @Test
    fun copyWithColorIntArgb_withLowAlpha_returnsCopyWithThatColor() {
        val originalBrush = buildTestBrush()

        val newBrush = originalBrush.copyWithColorIntArgb(colorIntArgb = 0x12345678)

        assertThat(newBrush).isNotEqualTo(originalBrush)
        assertThat(newBrush.colorLong).isNotEqualTo(originalBrush.colorLong)
        assertThat(newBrush.colorInt).isEqualTo(0x12345678)

        // The new brush has the original family, size and epsilon.
        assertThat(newBrush.family).isSameInstanceAs(originalBrush.family)
        assertThat(newBrush.size).isEqualTo(originalBrush.size)
        assertThat(newBrush.epsilon).isEqualTo(originalBrush.epsilon)
    }

    @Test
    fun copyWithColorIntArgb_withHighAlpha_returnsCopyWithThatColor() {
        val originalBrush = buildTestBrush()

        val newBrush = originalBrush.copyWithColorIntArgb(colorIntArgb = 0xAA123456.toInt())

        assertThat(newBrush).isNotEqualTo(originalBrush)
        assertThat(newBrush.colorLong).isNotEqualTo(originalBrush.colorLong)
        assertThat(newBrush.colorInt).isEqualTo(0xAA123456.toInt())

        // The new brush has the original family, size and epsilon.
        assertThat(newBrush.family).isSameInstanceAs(originalBrush.family)
        assertThat(newBrush.size).isEqualTo(originalBrush.size)
        assertThat(newBrush.epsilon).isEqualTo(originalBrush.epsilon)
    }

    @Test
    fun copyWithColorLong_withChangedColor_returnsCopyWithThatColor() {
        val originalBrush = buildTestBrush()

        val newColor = Color(red = 255, green = 230, blue = 115, alpha = 140).value.toLong()
        val newBrush = originalBrush.copyWithColorLong(colorLong = newColor)

        assertThat(newBrush).isNotEqualTo(originalBrush)
        assertThat(newBrush.colorLong).isNotEqualTo(originalBrush.colorLong)
        assertThat(newBrush.colorLong).isEqualTo(newColor)

        // The new brush has the original family, size and epsilon.
        assertThat(newBrush.family).isSameInstanceAs(originalBrush.family)
        assertThat(newBrush.size).isEqualTo(originalBrush.size)
        assertThat(newBrush.epsilon).isEqualTo(originalBrush.epsilon)
    }

    @Test
    fun copyWithColorLong_inUnsupportedColorSpace_returnsCopyWithConvertedColor() {
        val originalBrush = buildTestBrush()

        val newColor = Color(0.9f, 0.45f, 0.55f, 0.15f, ColorSpaces.AdobeRgb).value.toLong()
        val newBrush = originalBrush.copyWithColorLong(colorLong = newColor)

        val expectedColor = Color(newColor.toULong()).convert(ColorSpaces.DisplayP3)
        assertThat(newBrush.colorLong).isEqualTo(expectedColor.value.toLong())
        assertThat(newBrush.colorInt).isEqualTo(expectedColor.toArgb())
    }

    @Test
    fun brushBuilderBuild_withColorIntWithLowAlpha_createsExpectedBrush() {
        val testBrush = buildTestBrush()

        val builtBrush =
            Brush.builder()
                .setFamily(testBrush.family)
                .setColorIntArgb(0x12345678)
                .setSize(9f)
                .setEpsilon(0.9f)
                .build()

        assertThat(builtBrush.family).isEqualTo(testBrush.family)
        assertThat(builtBrush.colorInt).isEqualTo(0x12345678)
        assertThat(builtBrush.size).isEqualTo(9f)
        assertThat(builtBrush.epsilon).isEqualTo(0.9f)
    }

    @Test
    fun brushBuilderBuild_withColorIntWithHighAlpha_createsExpectedBrush() {
        val testBrush = buildTestBrush()

        val builtBrush =
            Brush.builder()
                .setFamily(testBrush.family)
                .setColorIntArgb(0xAA123456.toInt())
                .setSize(9f)
                .setEpsilon(0.9f)
                .build()

        assertThat(builtBrush.family).isEqualTo(testBrush.family)
        assertThat(builtBrush.colorInt).isEqualTo(0xAA123456.toInt())
        assertThat(builtBrush.size).isEqualTo(9f)
        assertThat(builtBrush.epsilon).isEqualTo(0.9f)
    }

    @Test
    fun brushBuilderBuild_withColorLong_createsExpectedBrush() {
        val testBrush = buildTestBrush()
        val testColorLong = Color(0.9f, 0.45f, 0.55f, 0.15f, ColorSpaces.DisplayP3).value.toLong()

        val builtBrush =
            Brush.builder()
                .setFamily(testBrush.family)
                .setColorLong(testColorLong)
                .setSize(9f)
                .setEpsilon(0.9f)
                .build()

        assertThat(builtBrush.family).isEqualTo(testBrush.family)
        assertThat(builtBrush.colorLong).isEqualTo(testColorLong)
        assertThat(builtBrush.size).isEqualTo(9f)
        assertThat(builtBrush.epsilon).isEqualTo(0.9f)
    }

    @Test
    fun brushBuilderBuild_withUnsupportedColorSpace_createsBrushWithConvertedColor() {
        val testBrush = buildTestBrush()
        val testColorLong = Color(0.9f, 0.45f, 0.55f, 0.15f, ColorSpaces.AdobeRgb).value.toLong()

        val builtBrush =
            Brush.builder()
                .setFamily(testBrush.family)
                .setColorLong(testColorLong)
                .setSize(9f)
                .setEpsilon(0.9f)
                .build()

        val expectedColor = Color(testColorLong.toULong()).convert(ColorSpaces.DisplayP3)
        assertThat(builtBrush.family).isEqualTo(testBrush.family)
        assertThat(builtBrush.colorLong).isEqualTo(expectedColor.value.toLong())
        assertThat(builtBrush.size).isEqualTo(9f)
        assertThat(builtBrush.epsilon).isEqualTo(0.9f)
    }

    /**
     * Creates an expected C++ Brush with default brush family/color and returns true if every
     * property of the Kotlin Brush's JNI-created C++ counterpart is equivalent to the expected C++
     * Brush.
     */
    private external fun matchesDefaultBrush(
        actualBrushNativePointer: Long
    ): Boolean // TODO: b/355248266 - @Keep must go in Proguard config file instead.

    /**
     * Creates an expected C++ Brush with custom values and returns true if every property of the
     * Kotlin Brush's JNI-created C++ counterpart is equivalent to the expected C++ Brush.
     */
    private external fun matchesCustomBrush(
        actualBrushNativePointer: Long
    ): Boolean // TODO: b/355248266 - @Keep must go in Proguard config file instead.

    /** Brush with every field different from default values. */
    private fun buildTestBrush(): Brush =
        Brush(
            BrushFamily(
                tip =
                    BrushTip(
                        0.1f,
                        0.2f,
                        0.3f,
                        0.4f,
                        0.5f,
                        0.6f,
                        0.7f,
                        0.8f,
                        9L,
                        listOf(
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
                        ),
                    ),
                paint = BrushPaint(),
                uri = "/brush-family:marker:1",
            ),
            color,
            13F,
            0.1234F,
        )
}
