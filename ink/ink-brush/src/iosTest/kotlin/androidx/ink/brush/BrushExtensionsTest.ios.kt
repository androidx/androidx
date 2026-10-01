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

package androidx.ink.brush

import androidx.ink.brush.color.colorspace.ColorSpaces as ComposeColorSpaces
import androidx.ink.nativeloader.InkInternalOnlyApi
import androidx.kruth.assertThat
import kotlin.test.Test
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.UIKit.UIColor

@OptIn(InkInternalOnlyApi::class, ExperimentalForeignApi::class)
class BrushExtensionsTest {

    private data class UiColorComponents(
        val red: Double,
        val green: Double,
        val blue: Double,
        val alpha: Double,
    )

    private fun UIColor.toComponents(): UiColorComponents {
        val components = DoubleArray(4)
        components.usePinned { pinned ->
            this@toComponents.getRed(
                red = pinned.addressOf(0),
                green = pinned.addressOf(1),
                blue = pinned.addressOf(2),
                alpha = pinned.addressOf(3),
            )
        }
        return UiColorComponents(
            red = components[0],
            green = components[1],
            blue = components[2],
            alpha = components[3],
        )
    }

    @Test
    fun brushCreateWithUiColor_srgb_getsCorrectColor() {
        val expectedRed = 0.4
        val expectedGreen = 0.6
        val expectedBlue = 0.8
        val expectedAlpha = 0.2
        val testColorSrgb =
            UIColor(
                red = expectedRed,
                green = expectedGreen,
                blue = expectedBlue,
                alpha = expectedAlpha,
            )
        val testColorComponents = testColorSrgb.toComponents()
        val brush = Brush.createWithUiColor(BrushFamily(), testColorSrgb, 1f, 1f)
        assertThat(brush.internalColor.colorSpace).isEqualTo(ComposeColorSpaces.DisplayP3)
        brush.internalColor.convert(ComposeColorSpaces.Srgb).let { internalColorSrgb ->
            assertThat(internalColorSrgb.red).isWithin(0.001f).of(expectedRed.toFloat())
            assertThat(internalColorSrgb.green).isWithin(0.001f).of(expectedGreen.toFloat())
            assertThat(internalColorSrgb.blue).isWithin(0.001f).of(expectedBlue.toFloat())
            assertThat(internalColorSrgb.alpha).isWithin(0.001f).of(expectedAlpha.toFloat())
        }
        val retrievedColorComponents = brush.createUiColor().toComponents()
        assertThat(retrievedColorComponents.red).isWithin(0.001).of(expectedRed)
        assertThat(retrievedColorComponents.green).isWithin(0.001).of(expectedGreen)
        assertThat(retrievedColorComponents.blue).isWithin(0.001).of(expectedBlue)
        assertThat(retrievedColorComponents.alpha).isWithin(0.001).of(expectedAlpha)
    }

    @Test
    fun brushCreateWithUiColor_displayP3_getsCorrectColor() {
        val expectedRed = 0.4
        val expectedGreen = 0.6
        val expectedBlue = 0.8
        val expectedAlpha = 0.2
        val testColorDisplayP3 =
            UIColor.colorWithDisplayP3Red(
                displayP3Red = expectedRed,
                green = expectedGreen,
                blue = expectedBlue,
                alpha = expectedAlpha,
            )
        val testColorComponents = testColorDisplayP3.toComponents()
        val brush = Brush.createWithUiColor(BrushFamily(), testColorDisplayP3, 1f, 1f)
        assertThat(brush.internalColor.colorSpace).isEqualTo(ComposeColorSpaces.DisplayP3)
        assertThat(brush.internalColor.red).isWithin(0.001f).of(expectedRed.toFloat())
        assertThat(brush.internalColor.green).isWithin(0.001f).of(expectedGreen.toFloat())
        assertThat(brush.internalColor.blue).isWithin(0.001f).of(expectedBlue.toFloat())
        assertThat(brush.internalColor.alpha).isWithin(0.001f).of(expectedAlpha.toFloat())
        val retrievedColorComponents = brush.createUiColor().toComponents()
        assertThat(retrievedColorComponents.red).isWithin(0.001).of(testColorComponents.red)
        assertThat(retrievedColorComponents.green).isWithin(0.001).of(testColorComponents.green)
        assertThat(retrievedColorComponents.blue).isWithin(0.001).of(testColorComponents.blue)
        assertThat(retrievedColorComponents.alpha).isWithin(0.001).of(testColorComponents.alpha)
    }

    @Test
    fun brushCopyWithUiColor_settingColor() {
        val transparentBlack = UIColor(red = 0.0, green = 0.0, blue = 0.0, alpha = 0.0)
        val brush = Brush.createWithUiColor(BrushFamily(), transparentBlack, 1f, 0.2f)
        assertThat(brush.internalColor.colorSpace).isEqualTo(ComposeColorSpaces.DisplayP3)
        brush.internalColor.convert(ComposeColorSpaces.Srgb).let { internalColorSrgb ->
            assertThat(internalColorSrgb.red).isWithin(0.001f).of(0.0f)
            assertThat(internalColorSrgb.green).isWithin(0.001f).of(0.0f)
            assertThat(internalColorSrgb.blue).isWithin(0.001f).of(0.0f)
            assertThat(internalColorSrgb.alpha).isWithin(0.001f).of(0.0f)
        }
        val newRed = 0.4
        val newGreen = 0.6
        val newBlue = 0.8
        val newAlpha = 0.2
        val newColor = UIColor(red = newRed, green = newGreen, blue = newBlue, alpha = newAlpha)
        val newBrush = brush.copyWithUiColor(color = newColor)
        assertThat(newBrush.internalColor.colorSpace).isEqualTo(ComposeColorSpaces.DisplayP3)
        newBrush.internalColor.convert(ComposeColorSpaces.Srgb).let { internalColorSrgb ->
            assertThat(internalColorSrgb.red).isWithin(0.001f).of(newRed.toFloat())
            assertThat(internalColorSrgb.green).isWithin(0.001f).of(newGreen.toFloat())
            assertThat(internalColorSrgb.blue).isWithin(0.001f).of(newBlue.toFloat())
            assertThat(internalColorSrgb.alpha).isWithin(0.001f).of(newAlpha.toFloat())
        }
        assertThat(newBrush.family).isSameInstanceAs(brush.family)
        assertThat(newBrush.size).isEqualTo(brush.size)
        assertThat(newBrush.epsilon).isEqualTo(brush.epsilon)
    }

    @Test
    fun brushCopyWithUiColor_settingAllProperties() {
        val transparentBlack = UIColor(red = 0.0, green = 0.0, blue = 0.0, alpha = 0.0)
        val brush = Brush.createWithUiColor(BrushFamily(), transparentBlack, 1f, 0.2f)
        assertThat(brush.internalColor.colorSpace).isEqualTo(ComposeColorSpaces.DisplayP3)
        brush.internalColor.convert(ComposeColorSpaces.Srgb).let { internalColorSrgb ->
            assertThat(internalColorSrgb.red).isWithin(0.001f).of(0.0f)
            assertThat(internalColorSrgb.green).isWithin(0.001f).of(0.0f)
            assertThat(internalColorSrgb.blue).isWithin(0.001f).of(0.0f)
            assertThat(internalColorSrgb.alpha).isWithin(0.001f).of(0.0f)
        }
        val newRed = 0.4
        val newGreen = 0.6
        val newBlue = 0.8
        val newAlpha = 0.2
        val newColor = UIColor(red = newRed, green = newGreen, blue = newBlue, alpha = newAlpha)
        val newSize = 2f
        val newEpsilon = 0.3f
        val newBrush =
            brush.copyWithUiColor(
                color = newColor,
                family = BrushFamily(),
                size = newSize,
                epsilon = newEpsilon,
            )
        assertThat(newBrush.internalColor.colorSpace).isEqualTo(ComposeColorSpaces.DisplayP3)
        newBrush.internalColor.convert(ComposeColorSpaces.Srgb).let { internalColorSrgb ->
            assertThat(internalColorSrgb.red).isWithin(0.001f).of(newRed.toFloat())
            assertThat(internalColorSrgb.green).isWithin(0.001f).of(newGreen.toFloat())
            assertThat(internalColorSrgb.blue).isWithin(0.001f).of(newBlue.toFloat())
            assertThat(internalColorSrgb.alpha).isWithin(0.001f).of(newAlpha.toFloat())
        }
        assertThat(newBrush.family).isNotSameInstanceAs(brush.family)
        assertThat(newBrush.size).isEqualTo(newSize)
        assertThat(newBrush.epsilon).isEqualTo(newEpsilon)
    }
}
