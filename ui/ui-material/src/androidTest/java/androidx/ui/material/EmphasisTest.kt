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
import androidx.ui.graphics.Color
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
    private val SurfaceHighEmphasisAlpha = 0.87f
    private val SurfaceMediumEmphasisAlpha = 0.60f
    private val SurfaceDisabledEmphasisAlpha = 0.38f

    private val PrimaryHighEmphasisAlpha = 1.00f
    private val PrimaryMediumEmphasisAlpha = 0.74f
    private val PrimaryDisabledEmphasisAlpha = 0.38f

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{1}")
        fun initColorPalette() = arrayOf(
            arrayOf(lightColorPalette(), "Light theme"),
            arrayOf(darkColorPalette(), "Dark theme")
        )
    }

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    @Test
    fun noEmphasisSpecified_contentColorUnmodified_surface() {
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
    fun highEmphasis_contentColorSet_surface() {
        composeTestRule.setContent {
            MaterialTheme(colors) {
                Surface {
                    ProvideEmphasis(MaterialTheme.emphasisLevels().high) {
                        val onSurface = MaterialTheme.colors().onSurface
                        val modifiedOnSurface = onSurface.copy(alpha = SurfaceHighEmphasisAlpha)

                        assertThat(contentColor()).isEqualTo(modifiedOnSurface)
                        assertThat(currentTextStyle().color).isEqualTo(modifiedOnSurface)
                    }
                }
            }
        }
    }

    @Test
    fun mediumEmphasis_contentColorSet_surface() {
        composeTestRule.setContent {
            MaterialTheme(colors) {
                Surface {
                    ProvideEmphasis(MaterialTheme.emphasisLevels().medium) {
                        val onSurface = MaterialTheme.colors().onSurface
                        val modifiedOnSurface = onSurface.copy(alpha = SurfaceMediumEmphasisAlpha)

                        assertThat(contentColor()).isEqualTo(modifiedOnSurface)
                        assertThat(currentTextStyle().color).isEqualTo(modifiedOnSurface)
                    }
                }
            }
        }
    }

    @Test
    fun lowEmphasis_contentColorSet_surface() {
        composeTestRule.setContent {
            MaterialTheme(colors) {
                Surface {
                    ProvideEmphasis(MaterialTheme.emphasisLevels().disabled) {
                        val onSurface = MaterialTheme.colors().onSurface
                        val modifiedOnSurface = onSurface.copy(alpha = SurfaceDisabledEmphasisAlpha)

                        assertThat(contentColor()).isEqualTo(modifiedOnSurface)
                        assertThat(currentTextStyle().color).isEqualTo(modifiedOnSurface)
                    }
                }
            }
        }
    }

    @Test
    fun noEmphasisSpecified_contentColorUnmodified_primary() {
        composeTestRule.setContent {
            MaterialTheme(colors) {
                Surface(color = colors.primary) {
                    val onPrimary = MaterialTheme.colors().onPrimary

                    assertThat(contentColor()).isEqualTo(onPrimary)
                    assertThat(currentTextStyle().color).isEqualTo(onPrimary)
                }
            }
        }
    }

    @Test
    fun highEmphasis_contentColorSet_primary() {
        composeTestRule.setContent {
            MaterialTheme(colors) {
                Surface(color = colors.primary) {
                    ProvideEmphasis(MaterialTheme.emphasisLevels().high) {
                        val onPrimary = MaterialTheme.colors().onPrimary
                        val modifiedOnPrimary = onPrimary.copy(alpha = PrimaryHighEmphasisAlpha)

                        assertThat(contentColor()).isEqualTo(modifiedOnPrimary)
                        assertThat(currentTextStyle().color).isEqualTo(modifiedOnPrimary)
                    }
                }
            }
        }
    }

    @Test
    fun mediumEmphasis_contentColorSet_primary() {
        composeTestRule.setContent {
            MaterialTheme(colors) {
                Surface(color = colors.primary) {
                    ProvideEmphasis(MaterialTheme.emphasisLevels().medium) {
                        val onPrimary = MaterialTheme.colors().onPrimary
                        val modifiedOnPrimary = onPrimary.copy(alpha = PrimaryMediumEmphasisAlpha)

                        assertThat(contentColor()).isEqualTo(modifiedOnPrimary)
                        assertThat(currentTextStyle().color).isEqualTo(modifiedOnPrimary)
                    }
                }
            }
        }
    }

    @Test
    fun lowEmphasis_contentColorSet_primary() {
        composeTestRule.setContent {
            MaterialTheme(colors) {
                Surface(color = colors.primary) {
                    ProvideEmphasis(MaterialTheme.emphasisLevels().disabled) {
                        val onPrimary = MaterialTheme.colors().onPrimary
                        val modifiedOnPrimary = onPrimary.copy(alpha = PrimaryDisabledEmphasisAlpha)

                        assertThat(contentColor()).isEqualTo(modifiedOnPrimary)
                        assertThat(currentTextStyle().color).isEqualTo(modifiedOnPrimary)
                    }
                }
            }
        }
    }

    @Test
    fun noEmphasisSpecified_contentColorUnmodified_colorNotFromTheme() {
        composeTestRule.setContent {
            MaterialTheme(colors) {
                Surface(contentColor = Color.Yellow) {
                    assertThat(contentColor()).isEqualTo(Color.Yellow)
                    assertThat(currentTextStyle().color).isEqualTo(Color.Yellow)
                }
            }
        }
    }

    @Test
    fun highEmphasis_contentColorSet_colorNotFromTheme() {
        composeTestRule.setContent {
            MaterialTheme(colors) {
                Surface(contentColor = Color.Yellow) {
                    ProvideEmphasis(MaterialTheme.emphasisLevels().high) {
                        val modifiedYellow = Color.Yellow.copy(alpha = SurfaceHighEmphasisAlpha)

                        assertThat(contentColor()).isEqualTo(modifiedYellow)
                        assertThat(currentTextStyle().color).isEqualTo(modifiedYellow)
                    }
                }
            }
        }
    }

    @Test
    fun mediumEmphasis_contentColorSet_colorNotFromTheme() {
        composeTestRule.setContent {
            MaterialTheme(colors) {
                Surface(contentColor = Color.Yellow) {
                    ProvideEmphasis(MaterialTheme.emphasisLevels().medium) {
                        val modifiedYellow = Color.Yellow.copy(alpha = SurfaceMediumEmphasisAlpha)

                        assertThat(contentColor()).isEqualTo(modifiedYellow)
                        assertThat(currentTextStyle().color).isEqualTo(modifiedYellow)
                    }
                }
            }
        }
    }

    @Test
    fun lowEmphasis_contentColorSet_colorNotFromTheme() {
        composeTestRule.setContent {
            MaterialTheme(colors) {
                Surface(contentColor = Color.Yellow) {
                    ProvideEmphasis(MaterialTheme.emphasisLevels().disabled) {
                        val modifiedYellow = Color.Yellow.copy(alpha = SurfaceDisabledEmphasisAlpha)

                        assertThat(contentColor()).isEqualTo(modifiedYellow)
                        assertThat(currentTextStyle().color).isEqualTo(modifiedYellow)
                    }
                }
            }
        }
    }
}
