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

import androidx.ink.brush.color.Color as ComposeColor
import androidx.ink.brush.color.colorspace.ColorSpaces as ComposeColorSpaces
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ColorExtensionsTest {
    @Test
    fun composeColorToColorInInkSupportedColorSpace_withSupportedColorSpace_returnsSameColor() {
        val composeColor =
            ComposeColor(
                red = 0f,
                green = 1f,
                blue = 100f / 255f,
                alpha = 155f / 255f,
                colorSpace = ComposeColorSpaces.DisplayP3,
            )

        val convertedColor = composeColor.toColorInInkSupportedColorSpace()

        // The color space is supported, so the color is the same. It's not the same instance,
        // though,
        // since ComposeColor is a value class.
        assertThat(convertedColor).isEqualTo(composeColor)
    }

    @Test
    fun composeColorToColorInInkSupportedColorSpace_withUnsupportedColorSpace_convertsToDisplayP3() {
        val composeColor =
            ComposeColor(
                red = 0f,
                green = 1f,
                blue = 100f / 255f,
                alpha = 155f / 255f,
                colorSpace = ComposeColorSpaces.AdobeRgb,
            )

        val convertedColor = composeColor.toColorInInkSupportedColorSpace()

        // The color space got converted to DISPLAY_P3. The color is out of gamut, so it got scaled
        // into
        // the Display P3 gamut.
        assertThat(convertedColor.colorSpace).isEqualTo(ComposeColorSpaces.DisplayP3)
        assertThat(convertedColor.red).isWithin(0.001f).of(0f)
        assertThat(convertedColor.green).isWithin(0.001f).of(0.9795f)
        assertThat(convertedColor.blue).isWithin(0.001f).of(0.4204f)
        assertThat(convertedColor.alpha).isWithin(0.001f).of(155f / 255f)
    }

    @Test
    fun composeColorSpaceToInkColorSpaceId_converts() {
        assertThat(ComposeColorSpaces.Srgb.toInkColorSpaceId()).isEqualTo(0)
        assertThat(ComposeColorSpaces.DisplayP3.toInkColorSpaceId()).isEqualTo(1)
    }

    @Test
    fun composeColorSpaceToInkColorSpaceId_withUnsupportedColorSpace_throws() {
        assertFailsWith<IllegalArgumentException> {
            ComposeColorSpaces.AdobeRgb.toInkColorSpaceId()
        }
    }
}
