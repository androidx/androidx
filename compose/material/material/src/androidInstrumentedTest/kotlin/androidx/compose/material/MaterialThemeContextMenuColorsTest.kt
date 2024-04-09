/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.material

import androidx.compose.foundation.contextmenu.ContextMenuColors
import androidx.compose.foundation.contextmenu.LocalContextMenuColors
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Fact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class MaterialThemeContextMenuColorsTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theme_light_contextMenuColors_setCorrectly() {
        val expectedSurfaceColor = Color.Red
        val onSurfaceColor = Color.Blue

        var contextMenuColors: ContextMenuColors? = null
        rule.setContent {
            MaterialTheme(
                colors = MaterialTheme.colors.copy(
                    surface = expectedSurfaceColor,
                    onSurface = onSurfaceColor,
                    isLight = true
                ),
            ) {
                contextMenuColors = LocalContextMenuColors.current
            }
        }

        val expectedEnabledContentColor =
            onSurfaceColor.copy(alpha = Alpha.Content.LowContrast.ENABLED)
        val expectedEnabledIconColor = onSurfaceColor.copy(alpha = Alpha.Icon.Light.ENABLED)
        val expectedDisabledContentColor =
            onSurfaceColor.copy(alpha = Alpha.Content.LowContrast.DISABLED)
        val expectedDisabledIconColor = onSurfaceColor.copy(alpha = Alpha.Icon.Light.DISABLED)

        contextMenuColors.assertContextMenuColors(
            expectedBackgroundColor = expectedSurfaceColor,
            expectedTextColor = expectedEnabledContentColor,
            expectedIconColor = expectedEnabledIconColor,
            expectedDisabledTextColor = expectedDisabledContentColor,
            expectedDisabledIconColor = expectedDisabledIconColor,
        )
    }

    @Test
    fun theme_dark_contextMenuColors_setCorrectly() {
        val expectedSurfaceColor = Color.Red
        val onSurfaceColor = Color.Blue

        var contextMenuColors: ContextMenuColors? = null
        rule.setContent {
            MaterialTheme(
                colors = MaterialTheme.colors.copy(
                    surface = expectedSurfaceColor,
                    onSurface = onSurfaceColor,
                    isLight = false
                ),
            ) {
                contextMenuColors = LocalContextMenuColors.current
            }
        }

        val expectedEnabledContentColor =
            onSurfaceColor.copy(alpha = Alpha.Content.HighContrast.ENABLED)
        val expectedEnabledIconColor = onSurfaceColor.copy(alpha = Alpha.Icon.Dark.ENABLED)
        val expectedDisabledContentColor =
            onSurfaceColor.copy(alpha = Alpha.Content.HighContrast.DISABLED)
        val expectedDisabledIconColor = onSurfaceColor.copy(alpha = Alpha.Icon.Dark.DISABLED)

        contextMenuColors.assertContextMenuColors(
            expectedBackgroundColor = expectedSurfaceColor,
            expectedTextColor = expectedEnabledContentColor,
            expectedIconColor = expectedEnabledIconColor,
            expectedDisabledTextColor = expectedDisabledContentColor,
            expectedDisabledIconColor = expectedDisabledIconColor,
        )
    }

    @Test
    fun theme_contextMenuColors_updatesOnRelevantColorChanges() {
        val initialSurfaceColor = Color.Red
        val expectedSurfaceColor = Color.Green
        val onSurfaceColor = Color.Blue

        var testSurfaceColor by mutableStateOf(initialSurfaceColor)
        var contextMenuColors: ContextMenuColors? = null
        rule.setContent {
            MaterialTheme(
                colors = MaterialTheme.colors.copy(
                    surface = testSurfaceColor,
                    onSurface = onSurfaceColor,
                    isLight = true,
                ),
            ) {
                contextMenuColors = LocalContextMenuColors.current
            }
        }

        val expectedEnabledContentColor =
            onSurfaceColor.copy(alpha = Alpha.Content.LowContrast.ENABLED)
        val expectedEnabledIconColor = onSurfaceColor.copy(alpha = Alpha.Icon.Light.ENABLED)
        val expectedDisabledContentColor =
            onSurfaceColor.copy(alpha = Alpha.Content.LowContrast.DISABLED)
        val expectedDisabledIconColor = onSurfaceColor.copy(alpha = Alpha.Icon.Light.DISABLED)

        contextMenuColors.assertContextMenuColors(
            expectedBackgroundColor = initialSurfaceColor,
            expectedTextColor = expectedEnabledContentColor,
            expectedIconColor = expectedEnabledIconColor,
            expectedDisabledTextColor = expectedDisabledContentColor,
            expectedDisabledIconColor = expectedDisabledIconColor,
        )

        val initialContextMenuColors = contextMenuColors
        testSurfaceColor = expectedSurfaceColor
        rule.waitForIdle()

        assertThat(contextMenuColors).isNotEqualTo(initialContextMenuColors)

        contextMenuColors.assertContextMenuColors(
            expectedBackgroundColor = expectedSurfaceColor,
            expectedTextColor = expectedEnabledContentColor,
            expectedIconColor = expectedEnabledIconColor,
            expectedDisabledTextColor = expectedDisabledContentColor,
            expectedDisabledIconColor = expectedDisabledIconColor,
        )
    }

    @Test
    fun theme_contextMenuColors_updatesOnIsLightChanges() {
        val expectedSurfaceColor = Color.Red
        val onSurfaceColor = Color.Blue

        var isLight by mutableStateOf(true)
        var contextMenuColors: ContextMenuColors? = null
        rule.setContent {
            MaterialTheme(
                colors = MaterialTheme.colors.copy(
                    surface = expectedSurfaceColor,
                    onSurface = onSurfaceColor,
                    isLight = isLight,
                ),
            ) {
                contextMenuColors = LocalContextMenuColors.current
            }
        }

        val initialContextMenuColors = contextMenuColors
        isLight = false
        rule.waitForIdle()

        assertThat(contextMenuColors).isNotEqualTo(initialContextMenuColors)

        val expectedEnabledContentColor =
            onSurfaceColor.copy(alpha = Alpha.Content.HighContrast.ENABLED)
        val expectedEnabledIconColor = onSurfaceColor.copy(alpha = Alpha.Icon.Dark.ENABLED)
        val expectedDisabledContentColor =
            onSurfaceColor.copy(alpha = Alpha.Content.HighContrast.DISABLED)
        val expectedDisabledIconColor = onSurfaceColor.copy(alpha = Alpha.Icon.Dark.DISABLED)

        contextMenuColors.assertContextMenuColors(
            expectedBackgroundColor = expectedSurfaceColor,
            expectedTextColor = expectedEnabledContentColor,
            expectedIconColor = expectedEnabledIconColor,
            expectedDisabledTextColor = expectedDisabledContentColor,
            expectedDisabledIconColor = expectedDisabledIconColor,
        )
    }

    @Test
    fun theme_contextMenuColors_doesNotUpdateOnIrrelevantColorChanges() {
        val expectedSurfaceColor = Color.Red
        val testOnSurfaceColor = Color.Blue

        var primaryColor by mutableStateOf(Color.Green)

        var contextMenuColors: ContextMenuColors? = null
        rule.setContent {
            MaterialTheme(
                colors = MaterialTheme.colors.copy(
                    primary = primaryColor,
                    surface = expectedSurfaceColor,
                    onSurface = testOnSurfaceColor,
                ),
            ) {
                contextMenuColors = LocalContextMenuColors.current
            }
        }

        val firstContextMenuColors = contextMenuColors
        primaryColor = Color.Cyan
        rule.waitForIdle()

        val secondContextMenuColors = contextMenuColors
        assertThat(secondContextMenuColors).isSameInstanceAs(firstContextMenuColors)
    }
}

private fun ContextMenuColors?.assertContextMenuColors(
    expectedBackgroundColor: Color,
    expectedTextColor: Color,
    expectedIconColor: Color,
    expectedDisabledTextColor: Color,
    expectedDisabledIconColor: Color,
) {
    assertThat(this).isNotNull()
    this!!
    assertThatColor(backgroundColor).isFuzzyEqualTo(expectedBackgroundColor)
    assertThatColor(textColor).isFuzzyEqualTo(expectedTextColor)
    assertThatColor(iconColor).isFuzzyEqualTo(expectedIconColor)
    assertThatColor(disabledTextColor).isFuzzyEqualTo(expectedDisabledTextColor)
    assertThatColor(disabledIconColor).isFuzzyEqualTo(expectedDisabledIconColor)
}

private fun assertThatColor(actual: Color): ColorSubject =
    Truth.assertAbout(ColorSubject.INSTANCE).that(actual)

private class ColorSubject(
    failureMetadata: FailureMetadata?,
    private val subject: Color,
) : Subject(failureMetadata, subject) {
    companion object {
        val INSTANCE = Factory<ColorSubject, Color> { failureMetadata, subject ->
            ColorSubject(failureMetadata, subject)
        }
    }

    fun isFuzzyEqualTo(expected: Color, tolerance: Float = 0.001f) {
        try {
            assertThat(subject.red).isWithin(tolerance).of(expected.red)
            assertThat(subject.green).isWithin(tolerance).of(expected.green)
            assertThat(subject.blue).isWithin(tolerance).of(expected.blue)
            assertThat(subject.alpha).isWithin(tolerance).of(expected.alpha)
        } catch (e: AssertionError) {
            failWithActual(
                Fact.simpleFact("Colors are not equal."),
                Fact.fact("expected", expected.toString()),
                Fact.fact("with tolerance", tolerance),
            )
        }
    }
}
