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

import androidx.compose.unaryPlus
import androidx.test.filters.MediumTest
import androidx.ui.core.currentTextStyle
import androidx.ui.foundation.contentColor
import androidx.ui.material.surface.Surface
import androidx.ui.test.createComposeRule
import org.junit.Assert.assertEquals
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
                    val onSurface = (+MaterialTheme.colors()).onSurface
                    assertEquals(
                        onSurface,
                        contentColor()
                    )

                    assertEquals(
                        onSurface,
                        (+currentTextStyle()).color
                    )
                }
            }
        }
    }

    @Test
    fun highEmphasis_contentColorSet() {
        composeTestRule.setContent {
            MaterialTheme(colors) {
                Surface {
                    ProvideEmphasis((+MaterialTheme.emphasisLevels()).high) {
                        val onSurface = (+MaterialTheme.colors()).onSurface
                        val modifiedOnSurface = onSurface.copy(alpha = HighEmphasisAlpha)
                        assertEquals(
                            modifiedOnSurface,
                            contentColor()
                        )

                        assertEquals(
                            modifiedOnSurface,
                            (+currentTextStyle()).color
                        )
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
                    ProvideEmphasis((+MaterialTheme.emphasisLevels()).medium) {
                        val onSurface = (+MaterialTheme.colors()).onSurface
                        val modifiedOnSurface = onSurface.copy(alpha = MediumEmphasisAlpha)
                        assertEquals(
                            modifiedOnSurface,
                            contentColor()
                        )

                        assertEquals(
                            modifiedOnSurface,
                            (+currentTextStyle()).color
                        )
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
                    ProvideEmphasis((+MaterialTheme.emphasisLevels()).disabled) {
                        val onSurface = (+MaterialTheme.colors()).onSurface
                        val modifiedOnSurface = onSurface.copy(alpha = DisabledEmphasisAlpha)
                        assertEquals(
                            modifiedOnSurface,
                            contentColor()
                        )

                        assertEquals(
                            modifiedOnSurface,
                            (+currentTextStyle()).color
                        )
                    }
                }
            }
        }
    }
}
