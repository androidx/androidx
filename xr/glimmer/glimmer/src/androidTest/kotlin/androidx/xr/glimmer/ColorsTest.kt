/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.glimmer

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlin.math.max
import kotlin.math.min
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ColorsTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun themeUpdatesWithNewColors() {
        val colors = Colors()
        val customColors = Colors(primary = Color.Magenta)
        val colorsState = mutableStateOf(colors)
        var currentColors: Colors? = null
        rule.setContent { GlimmerTheme(colorsState.value) { currentColors = GlimmerTheme.colors } }

        rule.runOnIdle {
            assertThat(currentColors).isEqualTo(colors)
            colorsState.value = customColors
        }

        rule.runOnIdle { assertThat(currentColors).isEqualTo(customColors) }
    }

    /**
     * Test to ensure that the baseline theme colors have acceptable contrast with the calculated
     * content colors. Note that primarily surface should be used to fill surfaces - other colors
     * such as primary should be used very rarely, if ever.
     */
    @Test
    fun baselineContentContrast() {
        val expectedContrastValue = 3 // Minimum 3:1 contrast ratio
        val colors = Colors()

        with(colors) {
            val primaryContentColor = calculateContentColor(primary)
            val secondaryContentColor = calculateContentColor(secondary)
            val positiveContentColor = calculateContentColor(positive)
            val negativeContentColor = calculateContentColor(negative)
            val surfaceContentColor = calculateContentColor(surface)
            assertThat(calculateContrastRatio(primaryContentColor, primary))
                .isAtLeast(expectedContrastValue)
            assertThat(calculateContrastRatio(secondaryContentColor, secondary))
                .isAtLeast(expectedContrastValue)
            assertThat(calculateContrastRatio(positiveContentColor, positive))
                .isAtLeast(expectedContrastValue)
            assertThat(calculateContrastRatio(negativeContentColor, negative))
                .isAtLeast(expectedContrastValue)
            assertThat(calculateContrastRatio(surfaceContentColor, surface))
                .isAtLeast(expectedContrastValue)
        }
    }
}

/**
 * Calculates the contrast ratio of [foreground] against [background], returning a value between 1
 * and 21. (1:1 and 21:1 ratios).
 *
 * Logic forked from
 * compose/material/material/src/commonMain/kotlin/androidx/compose/material/MaterialTextSelectionColors.kt
 *
 * Formula taken from
 * [WCAG 2.0](https://www.w3.org/TR/UNDERSTANDING-WCAG20/visual-audio-contrast-contrast.html#contrast-ratiodef)
 *
 * Note: [foreground] and [background] *must* be opaque. See [Color.compositeOver] to pre-composite
 * a translucent foreground over the background.
 *
 * @return the contrast ratio as a value between 1 and 21
 */
private fun calculateContrastRatio(foreground: Color, background: Color): Float {
    val foregroundLuminance = foreground.luminance() + 0.05f
    val backgroundLuminance = background.luminance() + 0.05f
    return max(foregroundLuminance, backgroundLuminance) /
        min(foregroundLuminance, backgroundLuminance)
}
