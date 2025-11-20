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

package androidx.ink.brush

import androidx.ink.brush.color.Color as ComposeColor
import com.google.common.truth.Truth.assertThat
import kotlin.IllegalArgumentException
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalInkCustomBrushApi::class)
@RunWith(JUnit4::class)
class ColorFunctionTest {

    @Test
    fun opacityMultiplierTransformColorIntArgb_multipliesOpacity() {
        val function = ColorFunction.OpacityMultiplier(0.5f)
        assertThat(function.transformColorIntArgb(0x00000000)).isEqualTo(0x00000000)
        assertThat(function.transformColorIntArgb(0x22446688)).isEqualTo(0x11446688)
    }

    @Test
    fun replaceColorTransformColorIntArgb_returnsReplacementColor() {
        val function = ColorFunction.ReplaceColor.withColorIntArgb(0x336699cc)
        assertThat(function.transformColorIntArgb(0x00000000)).isEqualTo(0x336699cc)
        assertThat(function.transformColorIntArgb(0x22446688)).isEqualTo(0x336699cc)
    }

    @Test
    @Suppress("Range") // Testing error cases.
    fun opacityMultiplierConstructor_requiresValuesInRange() {
        assertFailsWith<IllegalArgumentException> {
            ColorFunction.OpacityMultiplier(multiplier = -1f)
        }
    }

    @Test
    @Suppress("Range") // Testing error cases.
    fun opacityMultiplierConstructor_requiresFiniteValues() {
        assertFailsWith<IllegalArgumentException> {
            ColorFunction.OpacityMultiplier(multiplier = Float.POSITIVE_INFINITY)
        }
        assertFailsWith<IllegalArgumentException> {
            ColorFunction.OpacityMultiplier(multiplier = Float.NaN)
        }
    }

    @Test
    fun opacityMultiplier_multiplierGetter() {
        assertThat(ColorFunction.OpacityMultiplier(0.5f).multiplier).isEqualTo(0.5f)
        assertThat(ColorFunction.OpacityMultiplier(0.75f).multiplier).isEqualTo(0.75f)
    }

    @Test
    fun opacityMultiplierHashCode_withIdenticalValues_matches() {
        assertThat(ColorFunction.OpacityMultiplier(0.5f).hashCode())
            .isEqualTo(ColorFunction.OpacityMultiplier(0.5f).hashCode())
    }

    @Test
    fun opacityMultiplierEquals_checksEqualityOfValues() {
        val original = ColorFunction.OpacityMultiplier(0.5f)

        // Equal
        assertThat(original).isEqualTo(original) // Same instance.
        assertThat(original).isEqualTo(ColorFunction.OpacityMultiplier(0.5f)) // Same value.

        // Not equal
        assertThat(original).isNotEqualTo(null)
        assertThat(original).isNotEqualTo(ColorFunction.ReplaceColor.withColorIntArgb(0x336699cc))
        assertThat(original).isNotEqualTo(ColorFunction.OpacityMultiplier(0.25f))
    }

    @Test
    fun opacityMultiplierToString_returnsReasonableString() {
        assertThat(ColorFunction.OpacityMultiplier(0.5f).toString())
            .isEqualTo("ColorFunction.OpacityMultiplier(0.5)")
    }

    @Test
    fun replaceColor_composeColorGetter() {
        assertThat(ColorFunction.ReplaceColor.withComposeColor(ComposeColor.Magenta).internalColor)
            .isEqualTo(ComposeColor.Magenta)
        assertThat(
                ColorFunction.ReplaceColor.withColorLong(ComposeColor.Magenta.value.toLong())
                    .internalColor
            )
            .isEqualTo(ComposeColor.Magenta)
        assertThat(ColorFunction.ReplaceColor.withColorIntArgb(0xffff00ff.toInt()).internalColor)
            .isEqualTo(ComposeColor.Magenta)
    }

    @Test
    fun replaceColor_colorLongGetter() {
        assertThat(ColorFunction.ReplaceColor.withComposeColor(ComposeColor.Magenta).colorLong)
            .isEqualTo(ComposeColor.Magenta.value.toLong())
        assertThat(
                ColorFunction.ReplaceColor.withColorLong(ComposeColor.Magenta.value.toLong())
                    .colorLong
            )
            .isEqualTo(ComposeColor.Magenta.value.toLong())
        assertThat(ColorFunction.ReplaceColor.withColorIntArgb(0xffff00ff.toInt()).colorLong)
            .isEqualTo(ComposeColor.Magenta.value.toLong())
    }

    @Test
    fun replaceColor_colorIntArgbGetter() {
        assertThat(ColorFunction.ReplaceColor.withComposeColor(ComposeColor.Magenta).colorIntArgb)
            .isEqualTo(0xffff00ff.toInt())
        assertThat(
                ColorFunction.ReplaceColor.withColorLong(ComposeColor.Magenta.value.toLong())
                    .colorIntArgb
            )
            .isEqualTo(0xffff00ff.toInt())
        assertThat(ColorFunction.ReplaceColor.withColorIntArgb(0xffff00ff.toInt()).colorIntArgb)
            .isEqualTo(0xffff00ff.toInt())
    }

    @Test
    fun replaceColorHashCode_withIdenticalValues_matches() {
        assertThat(ColorFunction.ReplaceColor.withColorIntArgb(0x336699cc).hashCode())
            .isEqualTo(ColorFunction.ReplaceColor.withColorIntArgb(0x336699cc).hashCode())
    }

    @Test
    fun replaceColorEquals_checksEqualityOfValues() {
        val original = ColorFunction.ReplaceColor.withColorIntArgb(0x336699cc)

        // Equal
        assertThat(original).isEqualTo(original)
        assertThat(original).isEqualTo(ColorFunction.ReplaceColor.withColorIntArgb(0x336699cc))

        // Not equal
        assertThat(original).isNotEqualTo(null)
        assertThat(original).isNotEqualTo(ColorFunction.OpacityMultiplier(0.5f))
        assertThat(original).isNotEqualTo(ColorFunction.ReplaceColor.withColorIntArgb(0x11223344))
    }

    @Test
    fun replaceColorToString_returnsReasonableString() {
        assertThat(ColorFunction.ReplaceColor.withColorIntArgb(0x336699cc).toString())
            .isEqualTo("ColorFunction.ReplaceColor(Color(0.4, 0.6, 0.8, 0.2, sRGB IEC61966-2.1))")
    }
}
