/*
 * Copyright 2026 The Android Open Source Project
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

import android.R
import android.content.Context
import androidx.annotation.ColorRes
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteRow
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.height
import androidx.compose.remote.creation.compose.modifier.width
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import androidx.compose.remote.player.compose.test.utils.ComposableWrappers
import androidx.compose.remote.player.compose.test.utils.GridScreenshotUI
import androidx.compose.remote.player.compose.test.utils.RemoteEmbeddedScreenshotTestRule
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.matchers.MSSIMMatcher
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.remote.material3.util.SCREENSHOT_GOLDEN_DIRECTORY
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Screenshot tests for the default (named) [RemoteColorScheme] and [dynamicRemoteColorScheme],
 * played with the embedded player.
 *
 * Wear is always dark, so dynamic colors resolve to the dark Android system color tokens.
 *
 * Each cell shows the color the player rendered next to a swatch of the color the test expects,
 * read directly from [ColorScheme] or Android `Resources` rather than through the player. A
 * mismatch shows up as a split cell. Fallback cases use [DistinctColorScheme] so they can't be
 * confused with system colors.
 */
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@RunWith(JUnit4::class)
class RemoteColorSchemeScreenshotTest {
    @get:Rule
    val remoteComposeTestRule =
        RemoteEmbeddedScreenshotTestRule(
            moduleDirectory = SCREENSHOT_GOLDEN_DIRECTORY,
            context = ApplicationProvider.getApplicationContext(),
            matcher = MSSIMMatcher(threshold = 0.999),
        )

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val creationDisplayInfo = createCreationDisplayInfo(context, Size(800f, 2400f))

    @Test
    fun dynamicColorScheme() {
        runColorGridTest(
            expected = ::systemColor,
            colorScheme = { dynamicRemoteColorScheme(fallbackColorScheme = DistinctColorScheme) },
        )
    }

    @Test
    fun dynamicColorScheme_wearWidgetsProfile_fallsBackToNamedColors() {
        runColorGridTest(
            expected = fallbackColor(DistinctColorScheme, source = "fallback"),
            profile = RcPlatformProfiles.WEAR_WIDGETS,
            colorScheme = { dynamicRemoteColorScheme(fallbackColorScheme = DistinctColorScheme) },
        )
    }

    @Test
    fun defaultColorScheme_usesNamedColors() {
        runColorGridTest(expected = fallbackColor(ColorScheme(), source = "default"))
    }

    @Test
    fun explicitColorScheme_usesNamedColors() {
        runColorGridTest(
            expected = fallbackColor(DistinctColorScheme, source = "explicit"),
            colorScheme = { RemoteColorScheme(DistinctColorScheme) },
        )
    }

    private fun runColorGridTest(
        expected: (ColorRole) -> ExpectedColor,
        profile: Profile = RcPlatformProfiles.ANDROIDX,
        colorScheme: (@Composable () -> RemoteColorScheme)? = null,
    ) {
        remoteComposeTestRule.runScreenshotTest(
            remoteCreationDisplayInfo = creationDisplayInfo,
            profile = profile,
            playComposableWrapper = ComposableWrappers.blackBackground,
        ) {
            if (colorScheme != null) {
                RemoteMaterialTheme(colorScheme = colorScheme()) { ColorGrid(expected) }
            } else {
                RemoteMaterialTheme { ColorGrid(expected) }
            }
        }
    }

    private fun systemColor(role: ColorRole): ExpectedColor =
        ExpectedColor(
            color = Color(context.getColor(role.systemColor)),
            source =
                context.resources
                    .getResourceEntryName(role.systemColor)
                    .removePrefix("system_")
                    .removeSuffix("_dark"),
        )

    private fun fallbackColor(
        colorScheme: ColorScheme,
        source: String,
    ): (ColorRole) -> ExpectedColor = { role ->
        ExpectedColor(color = role.fallbackColor(colorScheme), source = source)
    }

    @Composable
    @RemoteComposable
    private fun ColorGrid(expected: (ColorRole) -> ExpectedColor) {
        val gridUI = GridScreenshotUI(itemsPerRow = 4, Padding = 4.rdp, ContainerSize = CellSize)
        val colorScheme = RemoteMaterialTheme.colorScheme
        gridUI.GridContent(
            innerContentList =
                ColorRoles.map { role ->
                    role.name to
                        @Composable @RemoteComposable {
                            ColorCell(
                                actual = role.remoteColor(colorScheme),
                                expected = expected(role),
                            )
                        }
                }
        )
    }

    /**
     * Draws the color the player resolved (left) next to a hardcoded swatch of the expected color
     * (right), above the expected hex value and its source. The expected swatch is a literal color,
     * so it doesn't go through the themed color lookup; any mismatch shows as a split cell.
     */
    @Composable
    @RemoteComposable
    private fun ColorCell(actual: RemoteColor, expected: ExpectedColor) {
        RemoteColumn {
            RemoteRow {
                RemoteBox(
                    modifier =
                        RemoteModifier.width(SwatchWidth).height(SwatchHeight).background(actual)
                )
                RemoteBox(
                    modifier =
                        RemoteModifier.width(SwatchWidth)
                            .height(SwatchHeight)
                            .background(RemoteColor(expected.color))
                )
            }
            RemoteBox(
                modifier =
                    RemoteModifier.width(CellSize)
                        .height(LabelHeight)
                        .background(RemoteColor(Color.Black)),
                contentAlignment = RemoteAlignment.Center,
            ) {
                RemoteText(
                    text = "${expected.hex}\n${expected.source}".rs,
                    color = RemoteColor(Color.White),
                    fontSize = 6.rsp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    /** A color role, how to read it from each scheme, and the system token it should map to. */
    private class ColorRole(
        val name: String,
        val remoteColor: RemoteColorScheme.() -> RemoteColor,
        val fallbackColor: ColorScheme.() -> Color,
        @ColorRes val systemColor: Int,
    )

    /** The color a cell should render, and where that expectation comes from. */
    private class ExpectedColor(val color: Color, val source: String) {
        val hex: String
            get() = "#%06X".format(color.toArgb() and 0xFFFFFF)
    }

    private companion object {
        val CellSize = 72.rdp
        val SwatchWidth = 36.rdp
        val SwatchHeight = 48.rdp
        val LabelHeight = 24.rdp

        /**
         * Deliberately artificial colors, distinct from both the Wear Material 3 defaults and
         * Android system colors, so a fallback is never mistaken for a dynamic color.
         */
        val DistinctColorScheme =
            ColorScheme(
                primary = Color(0xFFFF0000),
                primaryDim = Color(0xFF990000),
                primaryContainer = Color(0xFFFF8080),
                onPrimary = Color(0xFFFFFF00),
                onPrimaryContainer = Color(0xFF400000),
                secondary = Color(0xFF00FF00),
                secondaryDim = Color(0xFF009900),
                secondaryContainer = Color(0xFF80FF80),
                onSecondary = Color(0xFFFF00FF),
                onSecondaryContainer = Color(0xFF004000),
                tertiary = Color(0xFF0000FF),
                tertiaryDim = Color(0xFF000099),
                tertiaryContainer = Color(0xFF8080FF),
                onTertiary = Color(0xFF00FFFF),
                onTertiaryContainer = Color(0xFF000040),
                surfaceContainerLow = Color(0xFFFF8000),
                surfaceContainer = Color(0xFFFFA040),
                surfaceContainerHigh = Color(0xFFFFC080),
                onSurface = Color(0xFF8000FF),
                onSurfaceVariant = Color(0xFFB060FF),
                outline = Color(0xFF008080),
                outlineVariant = Color(0xFF80C0C0),
                background = Color(0xFF404000),
                onBackground = Color(0xFFFFFF80),
                error = Color(0xFFFF0080),
                errorDim = Color(0xFF990050),
                errorContainer = Color(0xFFFF80C0),
                onError = Color(0xFF80FF00),
                onErrorContainer = Color(0xFF400020),
            )

        val ColorRoles =
            listOf(
                ColorRole("primary", { primary }, { primary }, R.color.system_primary_dark),
                ColorRole(
                    "primaryDim",
                    { primaryDim },
                    { primaryDim },
                    R.color.system_primary_fixed_dim,
                ),
                ColorRole(
                    "primaryContainer",
                    { primaryContainer },
                    { primaryContainer },
                    R.color.system_primary_container_dark,
                ),
                ColorRole(
                    "onPrimary",
                    { onPrimary },
                    { onPrimary },
                    R.color.system_on_primary_dark,
                ),
                ColorRole(
                    "onPrimaryContainer",
                    { onPrimaryContainer },
                    { onPrimaryContainer },
                    R.color.system_on_primary_container_dark,
                ),
                ColorRole("secondary", { secondary }, { secondary }, R.color.system_secondary_dark),
                ColorRole(
                    "secondaryDim",
                    { secondaryDim },
                    { secondaryDim },
                    R.color.system_secondary_fixed_dim,
                ),
                ColorRole(
                    "secondaryContainer",
                    { secondaryContainer },
                    { secondaryContainer },
                    R.color.system_secondary_container_dark,
                ),
                ColorRole(
                    "onSecondary",
                    { onSecondary },
                    { onSecondary },
                    R.color.system_on_secondary_dark,
                ),
                ColorRole(
                    "onSecondaryContainer",
                    { onSecondaryContainer },
                    { onSecondaryContainer },
                    R.color.system_on_secondary_container_dark,
                ),
                ColorRole("tertiary", { tertiary }, { tertiary }, R.color.system_tertiary_dark),
                ColorRole(
                    "tertiaryDim",
                    { tertiaryDim },
                    { tertiaryDim },
                    R.color.system_tertiary_fixed_dim,
                ),
                ColorRole(
                    "tertiaryContainer",
                    { tertiaryContainer },
                    { tertiaryContainer },
                    R.color.system_tertiary_container_dark,
                ),
                ColorRole(
                    "onTertiary",
                    { onTertiary },
                    { onTertiary },
                    R.color.system_on_tertiary_dark,
                ),
                ColorRole(
                    "onTertiaryContainer",
                    { onTertiaryContainer },
                    { onTertiaryContainer },
                    R.color.system_on_tertiary_container_dark,
                ),
                ColorRole(
                    "surfaceContainerLow",
                    { surfaceContainerLow },
                    { surfaceContainerLow },
                    R.color.system_surface_container_low_dark,
                ),
                ColorRole(
                    "surfaceContainer",
                    { surfaceContainer },
                    { surfaceContainer },
                    R.color.system_surface_container_dark,
                ),
                ColorRole(
                    "surfaceContainerHigh",
                    { surfaceContainerHigh },
                    { surfaceContainerHigh },
                    R.color.system_surface_container_high_dark,
                ),
                ColorRole(
                    "onSurface",
                    { onSurface },
                    { onSurface },
                    R.color.system_on_surface_dark,
                ),
                ColorRole(
                    "onSurfaceVariant",
                    { onSurfaceVariant },
                    { onSurfaceVariant },
                    R.color.system_on_surface_variant_dark,
                ),
                ColorRole("outline", { outline }, { outline }, R.color.system_outline_dark),
                ColorRole(
                    "outlineVariant",
                    { outlineVariant },
                    { outlineVariant },
                    R.color.system_outline_variant_dark,
                ),
                ColorRole(
                    "background",
                    { background },
                    { background },
                    R.color.system_background_dark,
                ),
                ColorRole(
                    "onBackground",
                    { onBackground },
                    { onBackground },
                    R.color.system_on_background_dark,
                ),
                ColorRole("error", { error }, { error }, R.color.system_error_dark),
                ColorRole("errorDim", { errorDim }, { errorDim }, R.color.system_error_300),
                ColorRole(
                    "errorContainer",
                    { errorContainer },
                    { errorContainer },
                    R.color.system_error_container_dark,
                ),
                ColorRole("onError", { onError }, { onError }, R.color.system_on_error_dark),
                ColorRole(
                    "onErrorContainer",
                    { onErrorContainer },
                    { onErrorContainer },
                    R.color.system_on_error_container_dark,
                ),
            )
    }
}
