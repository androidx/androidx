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

package androidx.ink.brush.compose

import androidx.annotation.ColorLong
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.colorspace.ColorSpaces as ComposeColorSpaces
import androidx.ink.brush.Brush
import androidx.ink.brush.BrushFamily
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class BrushExtensionsTest {
    private val displayP3 = ComposeColorSpaces.DisplayP3
    private val adobeRgb = ComposeColorSpaces.AdobeRgb

    private val testColor = ComposeColor(0.4f, 0.6f, 0.8f, 0.2f, displayP3)
    @ColorLong private val testColorLong = testColor.value.toLong()

    @OptIn(ExperimentalInkCustomBrushApi::class)
    private val testFamily = BrushFamily(clientBrushFamilyId = "pencil")

    @Test
    fun brushCreateComposeColor_getsCorrectColor() {
        val brush = Brush.createWithColorLong(testFamily, testColorLong, 1f, 1f)

        val expectedColor = ComposeColor(testColorLong.toULong())
        assertThat(brush.composeColor).isEqualTo(expectedColor)
    }

    @Test
    fun brushCopyWithComposeColor_setsColor() {
        val brush = Brush.createWithColorIntArgb(testFamily, 0x4499bb66, 1f, 1f)

        val newBrush = brush.copyWithComposeColor(color = testColor)

        assertThat(newBrush.family).isEqualTo(brush.family)
        assertThat(newBrush.colorLong).isEqualTo(testColorLong)
        assertThat(newBrush.size).isEqualTo(brush.size)
        assertThat(newBrush.epsilon).isEqualTo(brush.epsilon)
    }

    @OptIn(ExperimentalInkCustomBrushApi::class)
    @Test
    fun brushCopyWithComposeColor_andOtherChangedValues_createsBrushWithColor() {
        val brush = Brush.createWithColorIntArgb(testFamily, 0x4499bb66, 1f, 1f)

        val newBrush =
            brush.copyWithComposeColor(
                color = testColor,
                family = BrushFamily(),
                size = 2f,
                epsilon = 0.2f,
            )

        assertThat(newBrush.family).isEqualTo(BrushFamily())
        assertThat(newBrush.colorLong).isEqualTo(testColorLong)
        assertThat(newBrush.size).isEqualTo(2f)
        assertThat(newBrush.epsilon).isEqualTo(0.2f)
    }

    @Test
    fun brushCopyWithComposeColor_withUnsupportedColorSpace_setsConvertedColor() {
        val brush = Brush.createWithColorIntArgb(testFamily, 0x4499bb66, 1f, 1f)

        val newColor = ComposeColor(0.6f, 0.7f, 0.4f, 0.3f, adobeRgb)
        val newBrush = brush.copyWithComposeColor(color = newColor)

        val expectedColor = newColor.convert(displayP3)
        assertThat(newBrush.colorLong).isEqualTo(expectedColor.value.toLong())
    }

    @Test
    fun brushBuilderSetComposeColor_setsColor() {
        val brush =
            Brush.builder()
                .setFamily(testFamily)
                .setComposeColor(testColor)
                .setSize(1f)
                .setEpsilon(1f)
                .build()

        assertThat(brush.colorLong).isEqualTo(testColorLong)
    }

    @Test
    fun brushBuilderSetComposeColor_withUnsupportedColorSpace_setsConvertedColor() {
        val unsupportedColor = ComposeColor(0.6f, 0.7f, 0.4f, 0.3f, adobeRgb)
        val brush =
            Brush.builder()
                .setFamily(testFamily)
                .setComposeColor(unsupportedColor)
                .setSize(1f)
                .setEpsilon(1f)
                .build()

        val expectedColor = unsupportedColor.convert(displayP3)
        assertThat(brush.colorLong).isEqualTo(expectedColor.value.toLong())
    }

    @Test
    fun brushCreateWithComposeColor_createsBrushWithColor() {
        val brush = Brush.createWithComposeColor(testFamily, testColor, 1f, 1f)
        assertThat(brush.colorLong).isEqualTo(testColorLong)
    }

    @Test
    fun brushCreateWithComposeColor_withUnsupportedColorSpace_createsBrushWithConvertedColor() {
        val unsupportedColor = ComposeColor(0.6f, 0.7f, 0.4f, 0.3f, adobeRgb)
        val brush = Brush.createWithComposeColor(testFamily, unsupportedColor, 1f, 1f)

        val expectedColor = unsupportedColor.convert(displayP3)
        assertThat(brush.colorLong).isEqualTo(expectedColor.value.toLong())
    }
}
