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
package androidx.wear.compose.remote.material3

import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.asConstantColor
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RemoteButtonColorsTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun remoteButtonColors_copies() {
        val original =
            RemoteButtonColors(
                containerColor = RemoteColor(Color.Red),
                contentColor = RemoteColor(Color.Blue),
                secondaryContentColor = RemoteColor(Color.Green),
                iconColor = RemoteColor(Color.Yellow),
                disabledContainerColor = RemoteColor(Color.Gray),
                disabledContentColor = RemoteColor(Color.White),
                disabledSecondaryContentColor = RemoteColor(Color.DarkGray),
                disabledIconColor = RemoteColor(Color.LightGray),
            )

        val copy = original.copy(containerColor = RemoteColor(Color.Magenta))

        assertThat(copy.containerColor.constantValue)
            .isEqualTo(RemoteColor(Color.Magenta).constantValue)
        assertThat(copy.contentColor.constantValue).isEqualTo(original.contentColor.constantValue)
    }

    @Test
    fun remoteIconButtonColors_copies() {
        val original =
            RemoteIconButtonColors(
                containerColor = RemoteColor(Color.Red),
                contentColor = RemoteColor(Color.Blue),
                disabledContainerColor = RemoteColor(Color.Gray),
                disabledContentColor = RemoteColor(Color.White),
            )

        val copy = original.copy(contentColor = RemoteColor(Color.Yellow))

        assertThat(copy.contentColor.constantValue)
            .isEqualTo(RemoteColor(Color.Yellow).constantValue)
        assertThat(copy.containerColor.constantValue)
            .isEqualTo(original.containerColor.constantValue)
    }

    @Test
    fun remoteTextButtonColors_copies() {
        val original =
            RemoteTextButtonColors(
                containerColor = RemoteColor(Color.Red),
                contentColor = RemoteColor(Color.Blue),
                disabledContainerColor = RemoteColor(Color.Gray),
                disabledContentColor = RemoteColor(Color.White),
            )

        val copy = original.copy(disabledContainerColor = RemoteColor(Color.Black))

        assertThat(copy.disabledContainerColor.constantValue)
            .isEqualTo(RemoteColor(Color.Black).constantValue)
        assertThat(copy.contentColor.constantValue).isEqualTo(original.contentColor.constantValue)
    }

    @Test
    fun buttonWithContainerPainterColors_defaultsToTransparentContainer() {
        lateinit var colors: RemoteButtonColors
        composeTestRule.setContent {
            colors = RemoteButtonDefaults.buttonWithContainerPainterColors()
        }

        assertThat(colors.containerColor.asConstantColor()).isEqualTo(Color.Transparent)
        assertThat(colors.disabledContainerColor.asConstantColor()).isEqualTo(Color.Transparent)
    }

    @Test
    fun buttonWithContainerPainterColors_overridesColors() {
        lateinit var customized: RemoteButtonColors

        composeTestRule.setContent {
            customized =
                RemoteButtonDefaults.buttonWithContainerPainterColors(
                    contentColor = RemoteColor(Color.Yellow),
                    secondaryContentColor = RemoteColor(Color.Cyan),
                )
        }

        assertThat(customized.containerColor.asConstantColor()).isEqualTo(Color.Transparent)
        assertThat(customized.disabledContainerColor.asConstantColor()).isEqualTo(Color.Transparent)
        assertThat(customized.contentColor.asConstantColor()).isEqualTo(Color.Yellow)
        assertThat(customized.secondaryContentColor.asConstantColor()).isEqualTo(Color.Cyan)
    }

    @Test
    fun childButtonColors_defaultsToTransparentContainerAndDisabledContainer() {
        lateinit var colors: RemoteButtonColors
        composeTestRule.setContent {
            colors = RemoteButtonDefaults.childButtonColors()
        }

        assertThat(colors.containerColor.asConstantColor()).isEqualTo(Color.Transparent)
        assertThat(colors.disabledContainerColor.asConstantColor()).isEqualTo(Color.Transparent)
    }

    @Test
    fun childButtonColors_overridesColors() {
        lateinit var customized: RemoteButtonColors
        composeTestRule.setContent {
            customized =
                RemoteButtonDefaults.childButtonColors(
                    contentColor = RemoteColor(Color.Yellow),
                    secondaryContentColor = RemoteColor(Color.Cyan),
                    iconColor = RemoteColor(Color.Magenta),
                )
        }

        assertThat(customized.containerColor.asConstantColor()).isEqualTo(Color.Transparent)
        assertThat(customized.disabledContainerColor.asConstantColor()).isEqualTo(Color.Transparent)
        assertThat(customized.contentColor.asConstantColor()).isEqualTo(Color.Yellow)
        assertThat(customized.secondaryContentColor.asConstantColor()).isEqualTo(Color.Cyan)
        assertThat(customized.iconColor.asConstantColor()).isEqualTo(Color.Magenta)
    }

    @Test
    fun buttonColors_transparentContainer_defaultsDisabledContainerToTransparent() {
        lateinit var colors: RemoteButtonColors
        composeTestRule.setContent {
            colors = RemoteButtonDefaults.buttonColors(containerColor = Color.Transparent.rc)
        }

        assertThat(colors.containerColor.asConstantColor()).isEqualTo(Color.Transparent)
        assertThat(colors.disabledContainerColor.asConstantColor()).isEqualTo(Color.Transparent)
    }

    @Test
    fun filledTonalButtonColors_defaultsIconColorToPrimary() {
        lateinit var colors: RemoteButtonColors
        val expectedPrimary = Color.Yellow.rc
        composeTestRule.setContent {
            RemoteMaterialTheme(colorScheme = RemoteColorScheme().copy(primary = expectedPrimary)) {
                colors = RemoteButtonDefaults.filledTonalButtonColors()
            }
        }

        assertThat(colors.iconColor.constantValue).isEqualTo(expectedPrimary.constantValue)
    }

    @Test
    fun outlinedButtonColors_defaultsIconColorToPrimary() {
        lateinit var colors: RemoteButtonColors
        val expectedPrimary = Color.Yellow.rc
        composeTestRule.setContent {
            RemoteMaterialTheme(colorScheme = RemoteColorScheme().copy(primary = expectedPrimary)) {
                colors = RemoteButtonDefaults.outlinedButtonColors()
            }
        }

        assertThat(colors.iconColor.constantValue).isEqualTo(expectedPrimary.constantValue)
    }

    @Test
    fun iconButtonColors_defaultsContentColorToPrimary() {
        lateinit var colors: RemoteIconButtonColors
        val expectedPrimary = Color.Yellow.rc
        composeTestRule.setContent {
            RemoteMaterialTheme(
                colorScheme =
                    RemoteColorScheme().copy(primary = expectedPrimary, onSurface = Color.Red.rc)
            ) {
                colors = RemoteIconButtonDefaults.iconButtonColors()
            }
        }

        assertThat(colors.contentColor.constantValue).isEqualTo(expectedPrimary.constantValue)
    }

    @Test
    fun filledTonalIconButtonColors_defaultsContentColorToPrimary() {
        lateinit var colors: RemoteIconButtonColors
        val expectedPrimary = Color.Yellow.rc
        composeTestRule.setContent {
            RemoteMaterialTheme(
                colorScheme =
                    RemoteColorScheme().copy(primary = expectedPrimary, onSurface = Color.Red.rc)
            ) {
                colors = RemoteIconButtonDefaults.filledTonalIconButtonColors()
            }
        }

        assertThat(colors.contentColor.constantValue).isEqualTo(expectedPrimary.constantValue)
    }

    @Test
    fun outlinedIconButtonColors_defaultsContentColorToPrimary() {
        lateinit var colors: RemoteIconButtonColors
        val expectedPrimary = Color.Yellow.rc
        composeTestRule.setContent {
            RemoteMaterialTheme(
                colorScheme =
                    RemoteColorScheme().copy(primary = expectedPrimary, onSurface = Color.Red.rc)
            ) {
                colors = RemoteIconButtonDefaults.outlinedIconButtonColors()
            }
        }

        assertThat(colors.contentColor.constantValue).isEqualTo(expectedPrimary.constantValue)
    }

    @Test
    fun filledTonalTextButtonColors_defaultsToSurfaceContainerAndOnSurface() {
        lateinit var colors: RemoteTextButtonColors
        val expectedContainer = Color.Cyan.rc
        val expectedContent = Color.Yellow.rc
        composeTestRule.setContent {
            RemoteMaterialTheme(
                colorScheme =
                    RemoteColorScheme()
                        .copy(
                            surfaceContainer = expectedContainer,
                            onSurface = expectedContent,
                            primary = Color.Red.rc,
                        )
            ) {
                colors = RemoteTextButtonDefaults.filledTonalTextButtonColors()
            }
        }

        assertThat(colors.containerColor.constantValue).isEqualTo(expectedContainer.constantValue)
        assertThat(colors.contentColor.constantValue).isEqualTo(expectedContent.constantValue)
    }

    @Test
    fun filledTonalTextButtonColors_overridesColors() {
        lateinit var customized: RemoteTextButtonColors
        val defaultContainer = Color.Cyan.rc
        val defaultContent = Color.Yellow.rc
        composeTestRule.setContent {
            RemoteMaterialTheme(
                colorScheme =
                    RemoteColorScheme()
                        .copy(surfaceContainer = defaultContainer, onSurface = defaultContent)
            ) {
                customized =
                    RemoteTextButtonDefaults.filledTonalTextButtonColors(
                        contentColor = Color.Magenta.rc
                    )
            }
        }

        assertThat(customized.containerColor.constantValue)
            .isEqualTo(defaultContainer.constantValue)
        assertThat(customized.contentColor.asConstantColor()).isEqualTo(Color.Magenta)
    }
}
