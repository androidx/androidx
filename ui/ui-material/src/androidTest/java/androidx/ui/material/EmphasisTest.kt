/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.material

import androidx.test.filters.MediumTest
import androidx.ui.core.currentTextStyle
import androidx.ui.foundation.contentColor
import androidx.ui.material.surface.Surface
import androidx.ui.test.createComposeRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Suppress("unused")
@MediumTest
@RunWith(Parameterized::class)
class EmphasisTest(private val colors: ColorPalette, private val debugParameterName: String) {
    private val HighEmphasisAlpha = 0.87f
    private val MediumEmphasisAlpha = 0.60f
    private val DisabledEmphasisAlpha = 0.38f

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{1}")
        // Mappings for elevation -> expected overlay color in dark theme
        fun initColorPalette() = arrayOf(
            arrayOf(lightColorPalette(), "Light theme"),
            arrayOf(darkColorPalette(), "Dark theme")
        )
    }

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    @Test
    fun noEmphasisSpecified_contentColorUnmodified() {
        composeTestRule.setContent {
            MaterialTheme(colors) {
                Surface {
                    val onSurface = MaterialTheme.colors().onSurface

                    assertThat(contentColor()).isEqualTo(onSurface)
                    assertThat(currentTextStyle().color).isEqualTo(onSurface)
                }
            }
        }
    }

    @Test
    fun highEmphasis_contentColorSet() {
        composeTestRule.setContent {
            MaterialTheme(colors) {
                Surface {
                    ProvideEmphasis(MaterialTheme.emphasisLevels().high) {
                        val onSurface = MaterialTheme.colors().onSurface
                        val modifiedOnSurface = onSurface.copy(alpha = HighEmphasisAlpha)

                        assertThat(contentColor()).isEqualTo(modifiedOnSurface)
                        assertThat(currentTextStyle().color).isEqualTo(modifiedOnSurface)
                    }
                }
            }
        }
    }

    @Test
    fun mediumEmphasis_contentColorSet() {
        composeTestRule.setContent {
            MaterialTheme(colors) {
                Surface {
                    ProvideEmphasis(MaterialTheme.emphasisLevels().medium) {
                        val onSurface = MaterialTheme.colors().onSurface
                        val modifiedOnSurface = onSurface.copy(alpha = MediumEmphasisAlpha)

                        assertThat(contentColor()).isEqualTo(modifiedOnSurface)
                        assertThat(currentTextStyle().color).isEqualTo(modifiedOnSurface)
                    }
                }
            }
        }
    }

    @Test
    fun lowEmphasis_contentColorSet() {
        composeTestRule.setContent {
            MaterialTheme(colors) {
                Surface {
                    ProvideEmphasis(MaterialTheme.emphasisLevels().disabled) {
                        val onSurface = MaterialTheme.colors().onSurface
                        val modifiedOnSurface = onSurface.copy(alpha = DisabledEmphasisAlpha)

                        assertThat(contentColor()).isEqualTo(modifiedOnSurface)
                        assertThat(currentTextStyle().color).isEqualTo(modifiedOnSurface)
                    }
                }
            }
        }
    }
}
