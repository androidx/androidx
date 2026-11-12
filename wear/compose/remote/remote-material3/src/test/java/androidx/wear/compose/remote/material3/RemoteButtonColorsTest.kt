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
                containerColor = RemoteColor.Red,
                contentColor = RemoteColor.Blue,
                secondaryContentColor = RemoteColor.Green,
                iconColor = RemoteColor.Yellow,
                disabledContainerColor = RemoteColor.Gray,
                disabledContentColor = RemoteColor.White,
                disabledSecondaryContentColor = RemoteColor.DarkGray,
                disabledIconColor = RemoteColor.LightGray,
            )

        val copy = original.copy(containerColor = RemoteColor.Magenta)

        assertThat(copy.containerColor.constantValue).isEqualTo(RemoteColor.Magenta.constantValue)
        assertThat(copy.contentColor.constantValue).isEqualTo(original.contentColor.constantValue)
    }

    @Test
    fun remoteIconButtonColors_copies() {
        val original =
            RemoteIconButtonColors(
                containerColor = RemoteColor.Red,
                contentColor = RemoteColor.Blue,
                disabledContainerColor = RemoteColor.Gray,
                disabledContentColor = RemoteColor.White,
            )

        val copy = original.copy(contentColor = RemoteColor.Yellow)

        assertThat(copy.contentColor.constantValue).isEqualTo(RemoteColor.Yellow.constantValue)
        assertThat(copy.containerColor.constantValue)
            .isEqualTo(original.containerColor.constantValue)
    }

    @Test
    fun remoteTextButtonColors_copies() {
        val original =
            RemoteTextButtonColors(
                containerColor = RemoteColor.Red,
                contentColor = RemoteColor.Blue,
                disabledContainerColor = RemoteColor.Gray,
                disabledContentColor = RemoteColor.White,
            )

        val copy = original.copy(disabledContainerColor = RemoteColor.Black)

        assertThat(copy.disabledContainerColor.constantValue)
            .isEqualTo(RemoteColor.Black.constantValue)
        assertThat(copy.contentColor.constantValue).isEqualTo(original.contentColor.constantValue)
    }

    @Test
    fun buttonWithContainerPainterColors_defaultsToTransparentContainer() {
        lateinit var colors: RemoteButtonColors
        composeTestRule.setContent {
            colors = RemoteButtonDefaults.buttonWithContainerPainterColors()
        }

        assertThat(colors.containerColor.constantValue)
            .isEqualTo(RemoteColor.Transparent.constantValue)
        assertThat(colors.disabledContainerColor.constantValue)
            .isEqualTo(RemoteColor.Transparent.constantValue)
    }

    @Test
    fun buttonWithContainerPainterColors_overridesColors() {
        lateinit var customized: RemoteButtonColors

        composeTestRule.setContent {
            customized =
                RemoteButtonDefaults.buttonWithContainerPainterColors(
                    contentColor = RemoteColor.Yellow,
                    secondaryContentColor = RemoteColor.Cyan,
                )
        }

        assertThat(customized.containerColor.constantValue)
            .isEqualTo(RemoteColor.Transparent.constantValue)
        assertThat(customized.disabledContainerColor.constantValue)
            .isEqualTo(RemoteColor.Transparent.constantValue)
        assertThat(customized.contentColor.constantValue)
            .isEqualTo(RemoteColor.Yellow.constantValue)
        assertThat(customized.secondaryContentColor.constantValue)
            .isEqualTo(RemoteColor.Cyan.constantValue)
    }

    @Test
    fun childButtonColors_defaultsToTransparentContainerAndDisabledContainer() {
        lateinit var colors: RemoteButtonColors
        composeTestRule.setContent {
            colors = RemoteButtonDefaults.childButtonColors()
        }

        assertThat(colors.containerColor.constantValue)
            .isEqualTo(RemoteColor.Transparent.constantValue)
        assertThat(colors.disabledContainerColor.constantValue)
            .isEqualTo(RemoteColor.Transparent.constantValue)
    }

    @Test
    fun childButtonColors_overridesColors() {
        lateinit var customized: RemoteButtonColors
        composeTestRule.setContent {
            customized =
                RemoteButtonDefaults.childButtonColors(
                    contentColor = RemoteColor.Yellow,
                    secondaryContentColor = RemoteColor.Cyan,
                    iconColor = RemoteColor.Magenta,
                )
        }

        assertThat(customized.containerColor.constantValue)
            .isEqualTo(RemoteColor.Transparent.constantValue)
        assertThat(customized.disabledContainerColor.constantValue)
            .isEqualTo(RemoteColor.Transparent.constantValue)
        assertThat(customized.contentColor.constantValue)
            .isEqualTo(RemoteColor.Yellow.constantValue)
        assertThat(customized.secondaryContentColor.constantValue)
            .isEqualTo(RemoteColor.Cyan.constantValue)
        assertThat(customized.iconColor.constantValue).isEqualTo(RemoteColor.Magenta.constantValue)
    }

    @Test
    fun buttonColors_transparentContainer_defaultsDisabledContainerToTransparent() {
        lateinit var colors: RemoteButtonColors
        composeTestRule.setContent {
            colors = RemoteButtonDefaults.buttonColors(containerColor = RemoteColor.Transparent)
        }

        assertThat(colors.containerColor.constantValue)
            .isEqualTo(RemoteColor.Transparent.constantValue)
        assertThat(colors.disabledContainerColor.constantValue)
            .isEqualTo(RemoteColor.Transparent.constantValue)
    }

    @Test
    fun filledTonalButtonColors_defaultsIconColorToPrimary() {
        lateinit var colors: RemoteButtonColors
        val expectedPrimary = RemoteColor.Yellow
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
        val expectedPrimary = RemoteColor.Yellow
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
        val expectedPrimary = RemoteColor.Yellow
        composeTestRule.setContent {
            RemoteMaterialTheme(
                colorScheme =
                    RemoteColorScheme().copy(primary = expectedPrimary, onSurface = RemoteColor.Red)
            ) {
                colors = RemoteIconButtonDefaults.iconButtonColors()
            }
        }

        assertThat(colors.contentColor.constantValue).isEqualTo(expectedPrimary.constantValue)
    }

    @Test
    fun filledTonalIconButtonColors_defaultsContentColorToPrimary() {
        lateinit var colors: RemoteIconButtonColors
        val expectedPrimary = RemoteColor.Yellow
        composeTestRule.setContent {
            RemoteMaterialTheme(
                colorScheme =
                    RemoteColorScheme().copy(primary = expectedPrimary, onSurface = RemoteColor.Red)
            ) {
                colors = RemoteIconButtonDefaults.filledTonalIconButtonColors()
            }
        }

        assertThat(colors.contentColor.constantValue).isEqualTo(expectedPrimary.constantValue)
    }

    @Test
    fun outlinedIconButtonColors_defaultsContentColorToPrimary() {
        lateinit var colors: RemoteIconButtonColors
        val expectedPrimary = RemoteColor.Yellow
        composeTestRule.setContent {
            RemoteMaterialTheme(
                colorScheme =
                    RemoteColorScheme().copy(primary = expectedPrimary, onSurface = RemoteColor.Red)
            ) {
                colors = RemoteIconButtonDefaults.outlinedIconButtonColors()
            }
        }

        assertThat(colors.contentColor.constantValue).isEqualTo(expectedPrimary.constantValue)
    }

    @Test
    fun filledTonalTextButtonColors_defaultsToSurfaceContainerAndOnSurface() {
        lateinit var colors: RemoteTextButtonColors
        val expectedContainer = RemoteColor.Cyan
        val expectedContent = RemoteColor.Yellow
        composeTestRule.setContent {
            RemoteMaterialTheme(
                colorScheme =
                    RemoteColorScheme()
                        .copy(
                            surfaceContainer = expectedContainer,
                            onSurface = expectedContent,
                            primary = RemoteColor.Red,
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
        val defaultContainer = RemoteColor.Cyan
        val defaultContent = RemoteColor.Yellow
        composeTestRule.setContent {
            RemoteMaterialTheme(
                colorScheme =
                    RemoteColorScheme()
                        .copy(surfaceContainer = defaultContainer, onSurface = defaultContent)
            ) {
                customized =
                    RemoteTextButtonDefaults.filledTonalTextButtonColors(
                        contentColor = RemoteColor.Magenta
                    )
            }
        }

        assertThat(customized.containerColor.constantValue)
            .isEqualTo(defaultContainer.constantValue)
        assertThat(customized.contentColor.constantValue)
            .isEqualTo(RemoteColor.Magenta.constantValue)
    }
}
