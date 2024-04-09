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

package androidx.compose.material3

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
    fun theme_contextMenuColors_setCorrectly() {
        val expectedSurfaceContainerColor = Color.Red
        val expectedOnSurfaceColor = Color.Blue
        val expectedOnSurfaceVariantColor = Color.Green
        val expectedDisabledContentColor =
            expectedOnSurfaceColor.copy(alpha = DisabledContextMenuContentOpacity)

        var contextMenuColors: ContextMenuColors? = null
        rule.setContent {
            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme.copy(
                    surfaceContainer = expectedSurfaceContainerColor,
                    onSurface = expectedOnSurfaceColor,
                    onSurfaceVariant = expectedOnSurfaceVariantColor,
                ),
            ) {
                contextMenuColors = LocalContextMenuColors.current
            }
        }

        contextMenuColors.assertContextMenuColors(
            expectedBackgroundColor = expectedSurfaceContainerColor,
            expectedTextColor = expectedOnSurfaceColor,
            expectedIconColor = expectedOnSurfaceVariantColor,
            expectedDisabledTextColor = expectedDisabledContentColor,
            expectedDisabledIconColor = expectedDisabledContentColor,
        )
    }

    @Test
    fun theme_contextMenuColors_updatesOnRelevantColorChanges() {
        val initialSurfaceContainerColor = Color.Black
        val expectedSurfaceContainerColor = Color.Red
        val expectedOnSurfaceColor = Color.Blue
        val expectedOnSurfaceVariantColor = Color.Green
        val expectedDisabledContentColor =
            expectedOnSurfaceColor.copy(alpha = DisabledContextMenuContentOpacity)

        var testSurfaceContainerColor by mutableStateOf(initialSurfaceContainerColor)
        var contextMenuColors: ContextMenuColors? = null
        rule.setContent {
            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme.copy(
                    surfaceContainer = testSurfaceContainerColor,
                    onSurface = expectedOnSurfaceColor,
                    onSurfaceVariant = expectedOnSurfaceVariantColor,
                ),
            ) {
                contextMenuColors = LocalContextMenuColors.current
            }
        }

        contextMenuColors.assertContextMenuColors(
            expectedBackgroundColor = initialSurfaceContainerColor,
            expectedTextColor = expectedOnSurfaceColor,
            expectedIconColor = expectedOnSurfaceVariantColor,
            expectedDisabledTextColor = expectedDisabledContentColor,
            expectedDisabledIconColor = expectedDisabledContentColor,
        )

        val initialContextMenuColors = contextMenuColors
        testSurfaceContainerColor = expectedSurfaceContainerColor
        rule.waitForIdle()

        assertThat(contextMenuColors).isNotEqualTo(initialContextMenuColors)

        contextMenuColors.assertContextMenuColors(
            expectedBackgroundColor = expectedSurfaceContainerColor,
            expectedTextColor = expectedOnSurfaceColor,
            expectedIconColor = expectedOnSurfaceVariantColor,
            expectedDisabledTextColor = expectedDisabledContentColor,
            expectedDisabledIconColor = expectedDisabledContentColor,
        )
    }

    @Test
    fun theme_contextMenuColors_doesNotUpdateOnIrrelevantColorChanges() {
        var primaryColor by mutableStateOf(Color.Black)
        var maybeContextMenuColors: ContextMenuColors? = null
        rule.setContent {
            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme.copy(
                    surfaceContainer = Color.Red,
                    onSurface = Color.Blue,
                    onSurfaceVariant = Color.Green,
                ),
            ) {
                maybeContextMenuColors = LocalContextMenuColors.current
            }
        }

        assertThat(maybeContextMenuColors).isNotNull()

        val initialContextMenuColors = maybeContextMenuColors
        primaryColor = Color.White
        rule.waitForIdle()

        assertThat(maybeContextMenuColors).isSameInstanceAs(initialContextMenuColors)
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
