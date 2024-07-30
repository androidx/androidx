/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.compose.material3.tokens.ColorDarkTokens
import androidx.compose.material3.tokens.ColorLightTokens
import androidx.compose.material3.tokens.ColorSchemeKeyTokens
import androidx.compose.material3.tokens.PaletteTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ln

/**
 * A color scheme holds all the named color parameters for a [MaterialTheme].
 *
 * Color schemes are designed to be harmonious, ensure accessible text, and distinguish UI elements
 * and surfaces from one another. There are two built-in baseline schemes, [lightColorScheme] and a
 * [darkColorScheme], that can be used as-is or customized.
 *
 * The Material color system and custom schemes provide default values for color as a starting point
 * for customization.
 *
 * Example of extending color scheme, including implementing Fixed Accent colors:
 *
 * @sample androidx.compose.material3.samples.ColorSchemeFixedAccentColorSample
 *
 * To learn more about colors, see
 * [Material Design colors](https://m3.material.io/styles/color/overview).
 *
 * @property primary The primary color is the color displayed most frequently across your app’s
 *   screens and components.
 * @property onPrimary Color used for text and icons displayed on top of the primary color.
 * @property primaryContainer The preferred tonal color of containers.
 * @property onPrimaryContainer The color (and state variants) that should be used for content on
 *   top of [primaryContainer].
 * @property inversePrimary Color to be used as a "primary" color in places where the inverse color
 *   scheme is needed, such as the button on a SnackBar.
 * @property secondary The secondary color provides more ways to accent and distinguish your
 *   product. Secondary colors are best for:
 * - Floating action buttons
 * - Selection controls, like checkboxes and radio buttons
 * - Highlighting selected text
 * - Links and headlines
 *
 * @property onSecondary Color used for text and icons displayed on top of the secondary color.
 * @property secondaryContainer A tonal color to be used in containers.
 * @property onSecondaryContainer The color (and state variants) that should be used for content on
 *   top of [secondaryContainer].
 * @property tertiary The tertiary color that can be used to balance primary and secondary colors,
 *   or bring heightened attention to an element such as an input field.
 * @property onTertiary Color used for text and icons displayed on top of the tertiary color.
 * @property tertiaryContainer A tonal color to be used in containers.
 * @property onTertiaryContainer The color (and state variants) that should be used for content on
 *   top of [tertiaryContainer].
 * @property background The background color that appears behind scrollable content.
 * @property onBackground Color used for text and icons displayed on top of the background color.
 * @property surface The surface color that affect surfaces of components, such as cards, sheets,
 *   and menus.
 * @property onSurface Color used for text and icons displayed on top of the surface color.
 * @property surfaceVariant Another option for a color with similar uses of [surface].
 * @property onSurfaceVariant The color (and state variants) that can be used for content on top of
 *   [surface].
 * @property surfaceTint This color will be used by components that apply tonal elevation and is
 *   applied on top of [surface]. The higher the elevation the more this color is used.
 * @property inverseSurface A color that contrasts sharply with [surface]. Useful for surfaces that
 *   sit on top of other surfaces with [surface] color.
 * @property inverseOnSurface A color that contrasts well with [inverseSurface]. Useful for content
 *   that sits on top of containers that are [inverseSurface].
 * @property error The error color is used to indicate errors in components, such as invalid text in
 *   a text field.
 * @property onError Color used for text and icons displayed on top of the error color.
 * @property errorContainer The preferred tonal color of error containers.
 * @property onErrorContainer The color (and state variants) that should be used for content on top
 *   of [errorContainer].
 * @property outline Subtle color used for boundaries. Outline color role adds contrast for
 *   accessibility purposes.
 * @property outlineVariant Utility color used for boundaries for decorative elements when strong
 *   contrast is not required.
 * @property scrim Color of a scrim that obscures content.
 * @property surfaceBright A [surface] variant that is always brighter than [surface], whether in
 *   light or dark mode.
 * @property surfaceDim A [surface] variant that is always dimmer than [surface], whether in light
 *   or dark mode.
 * @property surfaceContainer A [surface] variant that affects containers of components, such as
 *   cards, sheets, and menus.
 * @property surfaceContainerHigh A [surface] variant for containers with higher emphasis than
 *   [surfaceContainer]. Use this role for content which requires more emphasis than
 *   [surfaceContainer].
 * @property surfaceContainerHighest A [surface] variant for containers with higher emphasis than
 *   [surfaceContainerHigh]. Use this role for content which requires more emphasis than
 *   [surfaceContainerHigh].
 * @property surfaceContainerLow A [surface] variant for containers with lower emphasis than
 *   [surfaceContainer]. Use this role for content which requires less emphasis than
 *   [surfaceContainer].
 * @property surfaceContainerLowest A [surface] variant for containers with lower emphasis than
 *   [surfaceContainerLow]. Use this role for content which requires less emphasis than
 *   [surfaceContainerLow].
 */
@Immutable
class ColorScheme(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val inversePrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val surfaceTint: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val outline: Color,
    val outlineVariant: Color,
    val scrim: Color,
    val surfaceBright: Color,
    val surfaceDim: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainerLowest: Color,
) {
    @Deprecated(
        level = DeprecationLevel.WARNING,
        message = "Use constructor with additional 'surfaceContainer' roles.",
        replaceWith =
            ReplaceWith(
                "ColorScheme(primary,\n" +
                    "onPrimary,\n" +
                    "primaryContainer,\n" +
                    "onPrimaryContainer,\n" +
                    "inversePrimary,\n" +
                    "secondary,\n" +
                    "onSecondary,\n" +
                    "secondaryContainer,\n" +
                    "onSecondaryContainer,\n" +
                    "tertiary,\n" +
                    "onTertiary,\n" +
                    "tertiaryContainer,\n" +
                    "onTertiaryContainer,\n" +
                    "background,\n" +
                    "onBackground,\n" +
                    "surface,\n" +
                    "onSurface,\n" +
                    "surfaceVariant,\n" +
                    "onSurfaceVariant,\n" +
                    "surfaceTint,\n" +
                    "inverseSurface,\n" +
                    "inverseOnSurface,\n" +
                    "error,\n" +
                    "onError,\n" +
                    "errorContainer,\n" +
                    "onErrorContainer,\n" +
                    "outline,\n" +
                    "outlineVariant,\n" +
                    "scrim,\n" +
                    "surfaceBright,\n" +
                    "surfaceDim,\n" +
                    "surfaceContainer,\n" +
                    "surfaceContainerHigh,\n" +
                    "surfaceContainerHighest,\n" +
                    "surfaceContainerLow,\n" +
                    "surfaceContainerLowest,)"
            )
    )
    constructor(
        primary: Color,
        onPrimary: Color,
        primaryContainer: Color,
        onPrimaryContainer: Color,
        inversePrimary: Color,
        secondary: Color,
        onSecondary: Color,
        secondaryContainer: Color,
        onSecondaryContainer: Color,
        tertiary: Color,
        onTertiary: Color,
        tertiaryContainer: Color,
        onTertiaryContainer: Color,
        background: Color,
        onBackground: Color,
        surface: Color,
        onSurface: Color,
        surfaceVariant: Color,
        onSurfaceVariant: Color,
        surfaceTint: Color,
        inverseSurface: Color,
        inverseOnSurface: Color,
        error: Color,
        onError: Color,
        errorContainer: Color,
        onErrorContainer: Color,
        outline: Color,
        outlineVariant: Color,
        scrim: Color,
    ) : this(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = inversePrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = surfaceTint,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        outline = outline,
        outlineVariant = outlineVariant,
        scrim = scrim,
        surfaceBright = Color.Unspecified,
        surfaceDim = Color.Unspecified,
        surfaceContainer = Color.Unspecified,
        surfaceContainerHigh = Color.Unspecified,
        surfaceContainerHighest = Color.Unspecified,
        surfaceContainerLow = Color.Unspecified,
        surfaceContainerLowest = Color.Unspecified,
    )

    /** Returns a copy of this ColorScheme, optionally overriding some of the values. */
    fun copy(
        primary: Color = this.primary,
        onPrimary: Color = this.onPrimary,
        primaryContainer: Color = this.primaryContainer,
        onPrimaryContainer: Color = this.onPrimaryContainer,
        inversePrimary: Color = this.inversePrimary,
        secondary: Color = this.secondary,
        onSecondary: Color = this.onSecondary,
        secondaryContainer: Color = this.secondaryContainer,
        onSecondaryContainer: Color = this.onSecondaryContainer,
        tertiary: Color = this.tertiary,
        onTertiary: Color = this.onTertiary,
        tertiaryContainer: Color = this.tertiaryContainer,
        onTertiaryContainer: Color = this.onTertiaryContainer,
        background: Color = this.background,
        onBackground: Color = this.onBackground,
        surface: Color = this.surface,
        onSurface: Color = this.onSurface,
        surfaceVariant: Color = this.surfaceVariant,
        onSurfaceVariant: Color = this.onSurfaceVariant,
        surfaceTint: Color = this.surfaceTint,
        inverseSurface: Color = this.inverseSurface,
        inverseOnSurface: Color = this.inverseOnSurface,
        error: Color = this.error,
        onError: Color = this.onError,
        errorContainer: Color = this.errorContainer,
        onErrorContainer: Color = this.onErrorContainer,
        outline: Color = this.outline,
        outlineVariant: Color = this.outlineVariant,
        scrim: Color = this.scrim,
        surfaceBright: Color = this.surfaceBright,
        surfaceDim: Color = this.surfaceDim,
        surfaceContainer: Color = this.surfaceContainer,
        surfaceContainerHigh: Color = this.surfaceContainerHigh,
        surfaceContainerHighest: Color = this.surfaceContainerHighest,
        surfaceContainerLow: Color = this.surfaceContainerLow,
        surfaceContainerLowest: Color = this.surfaceContainerLowest,
    ): ColorScheme =
        ColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            inversePrimary = inversePrimary,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            surfaceTint = surfaceTint,
            inverseSurface = inverseSurface,
            inverseOnSurface = inverseOnSurface,
            error = error,
            onError = onError,
            errorContainer = errorContainer,
            onErrorContainer = onErrorContainer,
            outline = outline,
            outlineVariant = outlineVariant,
            scrim = scrim,
            surfaceBright = surfaceBright,
            surfaceDim = surfaceDim,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainerLowest = surfaceContainerLowest,
        )

    @Deprecated(
        message =
            "Maintained for binary compatibility. Use overload with additional surface roles " +
                "instead",
        level = DeprecationLevel.HIDDEN
    )
    fun copy(
        primary: Color = this.primary,
        onPrimary: Color = this.onPrimary,
        primaryContainer: Color = this.primaryContainer,
        onPrimaryContainer: Color = this.onPrimaryContainer,
        inversePrimary: Color = this.inversePrimary,
        secondary: Color = this.secondary,
        onSecondary: Color = this.onSecondary,
        secondaryContainer: Color = this.secondaryContainer,
        onSecondaryContainer: Color = this.onSecondaryContainer,
        tertiary: Color = this.tertiary,
        onTertiary: Color = this.onTertiary,
        tertiaryContainer: Color = this.tertiaryContainer,
        onTertiaryContainer: Color = this.onTertiaryContainer,
        background: Color = this.background,
        onBackground: Color = this.onBackground,
        surface: Color = this.surface,
        onSurface: Color = this.onSurface,
        surfaceVariant: Color = this.surfaceVariant,
        onSurfaceVariant: Color = this.onSurfaceVariant,
        surfaceTint: Color = this.surfaceTint,
        inverseSurface: Color = this.inverseSurface,
        inverseOnSurface: Color = this.inverseOnSurface,
        error: Color = this.error,
        onError: Color = this.onError,
        errorContainer: Color = this.errorContainer,
        onErrorContainer: Color = this.onErrorContainer,
        outline: Color = this.outline,
        outlineVariant: Color = this.outlineVariant,
        scrim: Color = this.scrim,
    ): ColorScheme =
        copy(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            inversePrimary = inversePrimary,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            surfaceTint = surfaceTint,
            inverseSurface = inverseSurface,
            inverseOnSurface = inverseOnSurface,
            error = error,
            onError = onError,
            errorContainer = errorContainer,
            onErrorContainer = onErrorContainer,
            outline = outline,
            outlineVariant = outlineVariant,
            scrim = scrim,
        )

    override fun toString(): String {
        return "ColorScheme(" +
            "primary=$primary" +
            "onPrimary=$onPrimary" +
            "primaryContainer=$primaryContainer" +
            "onPrimaryContainer=$onPrimaryContainer" +
            "inversePrimary=$inversePrimary" +
            "secondary=$secondary" +
            "onSecondary=$onSecondary" +
            "secondaryContainer=$secondaryContainer" +
            "onSecondaryContainer=$onSecondaryContainer" +
            "tertiary=$tertiary" +
            "onTertiary=$onTertiary" +
            "tertiaryContainer=$tertiaryContainer" +
            "onTertiaryContainer=$onTertiaryContainer" +
            "background=$background" +
            "onBackground=$onBackground" +
            "surface=$surface" +
            "onSurface=$onSurface" +
            "surfaceVariant=$surfaceVariant" +
            "onSurfaceVariant=$onSurfaceVariant" +
            "surfaceTint=$surfaceTint" +
            "inverseSurface=$inverseSurface" +
            "inverseOnSurface=$inverseOnSurface" +
            "error=$error" +
            "onError=$onError" +
            "errorContainer=$errorContainer" +
            "onErrorContainer=$onErrorContainer" +
            "outline=$outline" +
            "outlineVariant=$outlineVariant" +
            "scrim=$scrim" +
            "surfaceBright=$surfaceBright" +
            "surfaceDim=$surfaceDim" +
            "surfaceContainer=$surfaceContainer" +
            "surfaceContainerHigh=$surfaceContainerHigh" +
            "surfaceContainerHighest=$surfaceContainerHighest" +
            "surfaceContainerLow=$surfaceContainerLow" +
            "surfaceContainerLowest=$surfaceContainerLowest" +
            ")"
    }

    internal var defaultButtonColorsCached: ButtonColors? = null
    internal var defaultElevatedButtonColorsCached: ButtonColors? = null
    internal var defaultFilledTonalButtonColorsCached: ButtonColors? = null
    internal var defaultOutlinedButtonColorsCached: ButtonColors? = null
    internal var defaultTextButtonColorsCached: ButtonColors? = null

    internal var defaultCardColorsCached: CardColors? = null
    internal var defaultElevatedCardColorsCached: CardColors? = null
    internal var defaultOutlinedCardColorsCached: CardColors? = null

    internal var defaultAssistChipColorsCached: ChipColors? = null
    internal var defaultElevatedAssistChipColorsCached: ChipColors? = null
    internal var defaultSuggestionChipColorsCached: ChipColors? = null
    internal var defaultElevatedSuggestionChipColorsCached: ChipColors? = null
    internal var defaultFilterChipColorsCached: SelectableChipColors? = null
    internal var defaultElevatedFilterChipColorsCached: SelectableChipColors? = null
    internal var defaultInputChipColorsCached: SelectableChipColors? = null

    @OptIn(ExperimentalMaterial3Api::class)
    internal var defaultTopAppBarColorsCached: TopAppBarColors? = null
    @OptIn(ExperimentalMaterial3Api::class)
    internal var defaultCenterAlignedTopAppBarColorsCached: TopAppBarColors? = null
    @OptIn(ExperimentalMaterial3Api::class)
    internal var defaultMediumTopAppBarColorsCached: TopAppBarColors? = null
    @OptIn(ExperimentalMaterial3Api::class)
    internal var defaultLargeTopAppBarColorsCached: TopAppBarColors? = null

    internal var defaultCheckboxColorsCached: CheckboxColors? = null

    @OptIn(ExperimentalMaterial3Api::class)
    internal var defaultDatePickerColorsCached: DatePickerColors? = null

    internal var defaultIconButtonColorsCached: IconButtonColors? = null
    internal var defaultIconToggleButtonColorsCached: IconToggleButtonColors? = null
    internal var defaultFilledIconButtonColorsCached: IconButtonColors? = null
    internal var defaultFilledIconToggleButtonColorsCached: IconToggleButtonColors? = null
    internal var defaultFilledTonalIconButtonColorsCached: IconButtonColors? = null
    internal var defaultFilledTonalIconToggleButtonColorsCached: IconToggleButtonColors? = null
    internal var defaultOutlinedIconButtonColorsCached: IconButtonColors? = null
    internal var defaultOutlinedIconToggleButtonColorsCached: IconToggleButtonColors? = null

    internal var defaultListItemColorsCached: ListItemColors? = null

    internal var defaultMenuItemColorsCached: MenuItemColors? = null

    internal var defaultNavigationBarItemColorsCached: NavigationBarItemColors? = null
    internal var defaultShortNavigationBarItemColorsCached: NavigationItemColors? = null

    internal var defaultNavigationRailItemColorsCached: NavigationRailItemColors? = null
    internal var mDefaultWideWideNavigationRailColorsCached: WideNavigationRailColors? = null
    internal var defaultWideNavigationRailItemColorsCached: NavigationItemColors? = null

    internal var defaultRadioButtonColorsCached: RadioButtonColors? = null

    @OptIn(ExperimentalMaterial3Api::class)
    internal var defaultSegmentedButtonColorsCached: SegmentedButtonColors? = null

    internal var defaultSliderColorsCached: SliderColors? = null

    internal var defaultSwitchColorsCached: SwitchColors? = null

    internal var defaultOutlinedTextFieldColorsCached: TextFieldColors? = null
    internal var defaultTextFieldColorsCached: TextFieldColors? = null

    @OptIn(ExperimentalMaterial3Api::class)
    internal var defaultTimePickerColorsCached: TimePickerColors? = null

    @OptIn(ExperimentalMaterial3Api::class)
    internal var defaultRichTooltipColorsCached: RichTooltipColors? = null
}

/** Returns a light Material color scheme. */
fun lightColorScheme(
    primary: Color = ColorLightTokens.Primary,
    onPrimary: Color = ColorLightTokens.OnPrimary,
    primaryContainer: Color = ColorLightTokens.PrimaryContainer,
    onPrimaryContainer: Color = ColorLightTokens.OnPrimaryContainer,
    inversePrimary: Color = ColorLightTokens.InversePrimary,
    secondary: Color = ColorLightTokens.Secondary,
    onSecondary: Color = ColorLightTokens.OnSecondary,
    secondaryContainer: Color = ColorLightTokens.SecondaryContainer,
    onSecondaryContainer: Color = ColorLightTokens.OnSecondaryContainer,
    tertiary: Color = ColorLightTokens.Tertiary,
    onTertiary: Color = ColorLightTokens.OnTertiary,
    tertiaryContainer: Color = ColorLightTokens.TertiaryContainer,
    onTertiaryContainer: Color = ColorLightTokens.OnTertiaryContainer,
    background: Color = ColorLightTokens.Background,
    onBackground: Color = ColorLightTokens.OnBackground,
    surface: Color = ColorLightTokens.Surface,
    onSurface: Color = ColorLightTokens.OnSurface,
    surfaceVariant: Color = ColorLightTokens.SurfaceVariant,
    onSurfaceVariant: Color = ColorLightTokens.OnSurfaceVariant,
    surfaceTint: Color = primary,
    inverseSurface: Color = ColorLightTokens.InverseSurface,
    inverseOnSurface: Color = ColorLightTokens.InverseOnSurface,
    error: Color = ColorLightTokens.Error,
    onError: Color = ColorLightTokens.OnError,
    errorContainer: Color = ColorLightTokens.ErrorContainer,
    onErrorContainer: Color = ColorLightTokens.OnErrorContainer,
    outline: Color = ColorLightTokens.Outline,
    outlineVariant: Color = ColorLightTokens.OutlineVariant,
    scrim: Color = ColorLightTokens.Scrim,
    surfaceBright: Color = ColorLightTokens.SurfaceBright,
    surfaceContainer: Color = ColorLightTokens.SurfaceContainer,
    surfaceContainerHigh: Color = ColorLightTokens.SurfaceContainerHigh,
    surfaceContainerHighest: Color = ColorLightTokens.SurfaceContainerHighest,
    surfaceContainerLow: Color = ColorLightTokens.SurfaceContainerLow,
    surfaceContainerLowest: Color = ColorLightTokens.SurfaceContainerLowest,
    surfaceDim: Color = ColorLightTokens.SurfaceDim,
): ColorScheme =
    ColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = inversePrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = surfaceTint,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        outline = outline,
        outlineVariant = outlineVariant,
        scrim = scrim,
        surfaceBright = surfaceBright,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainerLowest = surfaceContainerLowest,
        surfaceDim = surfaceDim,
    )

@Deprecated(
    message =
        "Maintained for binary compatibility. Use overload with additional surface roles instead",
    level = DeprecationLevel.HIDDEN
)
fun lightColorScheme(
    primary: Color = ColorLightTokens.Primary,
    onPrimary: Color = ColorLightTokens.OnPrimary,
    primaryContainer: Color = ColorLightTokens.PrimaryContainer,
    onPrimaryContainer: Color = ColorLightTokens.OnPrimaryContainer,
    inversePrimary: Color = ColorLightTokens.InversePrimary,
    secondary: Color = ColorLightTokens.Secondary,
    onSecondary: Color = ColorLightTokens.OnSecondary,
    secondaryContainer: Color = ColorLightTokens.SecondaryContainer,
    onSecondaryContainer: Color = ColorLightTokens.OnSecondaryContainer,
    tertiary: Color = ColorLightTokens.Tertiary,
    onTertiary: Color = ColorLightTokens.OnTertiary,
    tertiaryContainer: Color = ColorLightTokens.TertiaryContainer,
    onTertiaryContainer: Color = ColorLightTokens.OnTertiaryContainer,
    background: Color = ColorLightTokens.Background,
    onBackground: Color = ColorLightTokens.OnBackground,
    surface: Color = ColorLightTokens.Surface,
    onSurface: Color = ColorLightTokens.OnSurface,
    surfaceVariant: Color = ColorLightTokens.SurfaceVariant,
    onSurfaceVariant: Color = ColorLightTokens.OnSurfaceVariant,
    surfaceTint: Color = primary,
    inverseSurface: Color = ColorLightTokens.InverseSurface,
    inverseOnSurface: Color = ColorLightTokens.InverseOnSurface,
    error: Color = ColorLightTokens.Error,
    onError: Color = ColorLightTokens.OnError,
    errorContainer: Color = ColorLightTokens.ErrorContainer,
    onErrorContainer: Color = ColorLightTokens.OnErrorContainer,
    outline: Color = ColorLightTokens.Outline,
    outlineVariant: Color = ColorLightTokens.OutlineVariant,
    scrim: Color = ColorLightTokens.Scrim,
): ColorScheme =
    lightColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = inversePrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = surfaceTint,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        outline = outline,
        outlineVariant = outlineVariant,
        scrim = scrim,
    )

/** Returns a dark Material color scheme. */
fun darkColorScheme(
    primary: Color = ColorDarkTokens.Primary,
    onPrimary: Color = ColorDarkTokens.OnPrimary,
    primaryContainer: Color = ColorDarkTokens.PrimaryContainer,
    onPrimaryContainer: Color = ColorDarkTokens.OnPrimaryContainer,
    inversePrimary: Color = ColorDarkTokens.InversePrimary,
    secondary: Color = ColorDarkTokens.Secondary,
    onSecondary: Color = ColorDarkTokens.OnSecondary,
    secondaryContainer: Color = ColorDarkTokens.SecondaryContainer,
    onSecondaryContainer: Color = ColorDarkTokens.OnSecondaryContainer,
    tertiary: Color = ColorDarkTokens.Tertiary,
    onTertiary: Color = ColorDarkTokens.OnTertiary,
    tertiaryContainer: Color = ColorDarkTokens.TertiaryContainer,
    onTertiaryContainer: Color = ColorDarkTokens.OnTertiaryContainer,
    background: Color = ColorDarkTokens.Background,
    onBackground: Color = ColorDarkTokens.OnBackground,
    surface: Color = ColorDarkTokens.Surface,
    onSurface: Color = ColorDarkTokens.OnSurface,
    surfaceVariant: Color = ColorDarkTokens.SurfaceVariant,
    onSurfaceVariant: Color = ColorDarkTokens.OnSurfaceVariant,
    surfaceTint: Color = primary,
    inverseSurface: Color = ColorDarkTokens.InverseSurface,
    inverseOnSurface: Color = ColorDarkTokens.InverseOnSurface,
    error: Color = ColorDarkTokens.Error,
    onError: Color = ColorDarkTokens.OnError,
    errorContainer: Color = ColorDarkTokens.ErrorContainer,
    onErrorContainer: Color = ColorDarkTokens.OnErrorContainer,
    outline: Color = ColorDarkTokens.Outline,
    outlineVariant: Color = ColorDarkTokens.OutlineVariant,
    scrim: Color = ColorDarkTokens.Scrim,
    surfaceBright: Color = ColorDarkTokens.SurfaceBright,
    surfaceContainer: Color = ColorDarkTokens.SurfaceContainer,
    surfaceContainerHigh: Color = ColorDarkTokens.SurfaceContainerHigh,
    surfaceContainerHighest: Color = ColorDarkTokens.SurfaceContainerHighest,
    surfaceContainerLow: Color = ColorDarkTokens.SurfaceContainerLow,
    surfaceContainerLowest: Color = ColorDarkTokens.SurfaceContainerLowest,
    surfaceDim: Color = ColorDarkTokens.SurfaceDim,
): ColorScheme =
    ColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = inversePrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = surfaceTint,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        outline = outline,
        outlineVariant = outlineVariant,
        scrim = scrim,
        surfaceBright = surfaceBright,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainerLowest = surfaceContainerLowest,
        surfaceDim = surfaceDim,
    )

@Deprecated(
    message =
        "Maintained for binary compatibility. Use overload with additional surface roles instead",
    level = DeprecationLevel.HIDDEN
)
fun darkColorScheme(
    primary: Color = ColorDarkTokens.Primary,
    onPrimary: Color = ColorDarkTokens.OnPrimary,
    primaryContainer: Color = ColorDarkTokens.PrimaryContainer,
    onPrimaryContainer: Color = ColorDarkTokens.OnPrimaryContainer,
    inversePrimary: Color = ColorDarkTokens.InversePrimary,
    secondary: Color = ColorDarkTokens.Secondary,
    onSecondary: Color = ColorDarkTokens.OnSecondary,
    secondaryContainer: Color = ColorDarkTokens.SecondaryContainer,
    onSecondaryContainer: Color = ColorDarkTokens.OnSecondaryContainer,
    tertiary: Color = ColorDarkTokens.Tertiary,
    onTertiary: Color = ColorDarkTokens.OnTertiary,
    tertiaryContainer: Color = ColorDarkTokens.TertiaryContainer,
    onTertiaryContainer: Color = ColorDarkTokens.OnTertiaryContainer,
    background: Color = ColorDarkTokens.Background,
    onBackground: Color = ColorDarkTokens.OnBackground,
    surface: Color = ColorDarkTokens.Surface,
    onSurface: Color = ColorDarkTokens.OnSurface,
    surfaceVariant: Color = ColorDarkTokens.SurfaceVariant,
    onSurfaceVariant: Color = ColorDarkTokens.OnSurfaceVariant,
    surfaceTint: Color = primary,
    inverseSurface: Color = ColorDarkTokens.InverseSurface,
    inverseOnSurface: Color = ColorDarkTokens.InverseOnSurface,
    error: Color = ColorDarkTokens.Error,
    onError: Color = ColorDarkTokens.OnError,
    errorContainer: Color = ColorDarkTokens.ErrorContainer,
    onErrorContainer: Color = ColorDarkTokens.OnErrorContainer,
    outline: Color = ColorDarkTokens.Outline,
    outlineVariant: Color = ColorDarkTokens.OutlineVariant,
    scrim: Color = ColorDarkTokens.Scrim,
): ColorScheme =
    darkColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = inversePrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = surfaceTint,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        outline = outline,
        outlineVariant = outlineVariant,
        scrim = scrim,
    )

/**
 * The Material color system contains pairs of colors that are typically used for the background and
 * content color inside a component. For example, a [Button] typically uses `primary` for its
 * background, and `onPrimary` for the color of its content (usually text or iconography).
 *
 * This function tries to match the provided [backgroundColor] to a 'background' color in this
 * [ColorScheme], and then will return the corresponding color used for content. For example, when
 * [backgroundColor] is [ColorScheme.primary], this will return [ColorScheme.onPrimary].
 *
 * If [backgroundColor] does not match a background color in the theme, this will return
 * [Color.Unspecified].
 *
 * @return the matching content color for [backgroundColor]. If [backgroundColor] is not present in
 *   the theme's [ColorScheme], then returns [Color.Unspecified].
 * @see contentColorFor
 */
@Stable
fun ColorScheme.contentColorFor(backgroundColor: Color): Color =
    when (backgroundColor) {
        primary -> onPrimary
        secondary -> onSecondary
        tertiary -> onTertiary
        background -> onBackground
        error -> onError
        primaryContainer -> onPrimaryContainer
        secondaryContainer -> onSecondaryContainer
        tertiaryContainer -> onTertiaryContainer
        errorContainer -> onErrorContainer
        inverseSurface -> inverseOnSurface
        surface -> onSurface
        surfaceVariant -> onSurfaceVariant
        surfaceBright -> onSurface
        surfaceContainer -> onSurface
        surfaceContainerHigh -> onSurface
        surfaceContainerHighest -> onSurface
        surfaceContainerLow -> onSurface
        surfaceContainerLowest -> onSurface
        else -> Color.Unspecified
    }

/**
 * The Material color system contains pairs of colors that are typically used for the background and
 * content color inside a component. For example, a [Button] typically uses `primary` for its
 * background, and `onPrimary` for the color of its content (usually text or iconography).
 *
 * This function tries to match the provided [backgroundColor] to a 'background' color in this
 * [ColorScheme], and then will return the corresponding color used for content. For example, when
 * [backgroundColor] is [ColorScheme.primary], this will return [ColorScheme.onPrimary].
 *
 * If [backgroundColor] does not match a background color in the theme, this will return the current
 * value of [LocalContentColor] as a best-effort color.
 *
 * @return the matching content color for [backgroundColor]. If [backgroundColor] is not present in
 *   the theme's [ColorScheme], then returns the current value of [LocalContentColor].
 * @see ColorScheme.contentColorFor
 */
@Composable
@ReadOnlyComposable
fun contentColorFor(backgroundColor: Color) =
    MaterialTheme.colorScheme.contentColorFor(backgroundColor).takeOrElse {
        LocalContentColor.current
    }

/**
 * Returns [ColorScheme.surfaceColorAtElevation] with the provided elevation if
 * [LocalTonalElevationEnabled] is set to true, and the provided background color matches
 * [ColorScheme.surface]. Otherwise, the provided color is returned unchanged.
 *
 * @param backgroundColor The background color to compare to [ColorScheme.surface]
 * @param elevation The elevation provided to [ColorScheme.surfaceColorAtElevation] if
 *   [backgroundColor] matches surface.
 * @return [ColorScheme.surfaceColorAtElevation] at [elevation] if [backgroundColor] ==
 *   [ColorScheme.surface] and [LocalTonalElevationEnabled] is set to true. Else [backgroundColor]
 */
@Composable
@ReadOnlyComposable
internal fun ColorScheme.applyTonalElevation(backgroundColor: Color, elevation: Dp): Color {
    val tonalElevationEnabled = LocalTonalElevationEnabled.current
    return if (backgroundColor == surface && tonalElevationEnabled) {
        surfaceColorAtElevation(elevation)
    } else {
        backgroundColor
    }
}

/**
 * Computes the surface tonal color at different elevation levels e.g. surface1 through surface5.
 *
 * @param elevation Elevation value used to compute alpha of the color overlay layer.
 * @return the [ColorScheme.surface] color with an alpha of the [ColorScheme.surfaceTint] color
 *   overlaid on top of it.
 */
@Stable
fun ColorScheme.surfaceColorAtElevation(
    elevation: Dp,
): Color {
    if (elevation == 0.dp) return surface
    val alpha = ((4.5f * ln(elevation.value + 1)) + 2f) / 100f
    return surfaceTint.copy(alpha = alpha).compositeOver(surface)
}

/**
 * Returns a light Material color scheme.
 *
 * The default color scheme for [MaterialExpressiveTheme]. For dark mode, use [darkColorScheme].
 *
 * Example of MaterialExpressiveTheme toggling expressiveLightColorScheme and darkTheme.
 *
 * @sample androidx.compose.material3.samples.MaterialExpressiveThemeColorSchemeSample
 */
@ExperimentalMaterial3ExpressiveApi
fun expressiveLightColorScheme() =
    lightColorScheme(
        // TODO: Replace palette references with color token references when available.
        onPrimaryContainer = PaletteTokens.Primary30,
        onSecondaryContainer = PaletteTokens.Secondary30,
        onTertiaryContainer = PaletteTokens.Secondary30,
        onErrorContainer = PaletteTokens.Error30
    )

/**
 * Helper function for component color tokens. Here is an example on how to use component color
 * tokens: ``MaterialTheme.colorScheme.fromToken(ExtendedFabBranded.BrandedContainerColor)``
 */
@Stable
internal fun ColorScheme.fromToken(value: ColorSchemeKeyTokens): Color {
    return when (value) {
        ColorSchemeKeyTokens.Background -> background
        ColorSchemeKeyTokens.Error -> error
        ColorSchemeKeyTokens.ErrorContainer -> errorContainer
        ColorSchemeKeyTokens.InverseOnSurface -> inverseOnSurface
        ColorSchemeKeyTokens.InversePrimary -> inversePrimary
        ColorSchemeKeyTokens.InverseSurface -> inverseSurface
        ColorSchemeKeyTokens.OnBackground -> onBackground
        ColorSchemeKeyTokens.OnError -> onError
        ColorSchemeKeyTokens.OnErrorContainer -> onErrorContainer
        ColorSchemeKeyTokens.OnPrimary -> onPrimary
        ColorSchemeKeyTokens.OnPrimaryContainer -> onPrimaryContainer
        ColorSchemeKeyTokens.OnSecondary -> onSecondary
        ColorSchemeKeyTokens.OnSecondaryContainer -> onSecondaryContainer
        ColorSchemeKeyTokens.OnSurface -> onSurface
        ColorSchemeKeyTokens.OnSurfaceVariant -> onSurfaceVariant
        ColorSchemeKeyTokens.SurfaceTint -> surfaceTint
        ColorSchemeKeyTokens.OnTertiary -> onTertiary
        ColorSchemeKeyTokens.OnTertiaryContainer -> onTertiaryContainer
        ColorSchemeKeyTokens.Outline -> outline
        ColorSchemeKeyTokens.OutlineVariant -> outlineVariant
        ColorSchemeKeyTokens.Primary -> primary
        ColorSchemeKeyTokens.PrimaryContainer -> primaryContainer
        ColorSchemeKeyTokens.Scrim -> scrim
        ColorSchemeKeyTokens.Secondary -> secondary
        ColorSchemeKeyTokens.SecondaryContainer -> secondaryContainer
        ColorSchemeKeyTokens.Surface -> surface
        ColorSchemeKeyTokens.SurfaceVariant -> surfaceVariant
        ColorSchemeKeyTokens.SurfaceBright -> surfaceBright
        ColorSchemeKeyTokens.SurfaceContainer -> surfaceContainer
        ColorSchemeKeyTokens.SurfaceContainerHigh -> surfaceContainerHigh
        ColorSchemeKeyTokens.SurfaceContainerHighest -> surfaceContainerHighest
        ColorSchemeKeyTokens.SurfaceContainerLow -> surfaceContainerLow
        ColorSchemeKeyTokens.SurfaceContainerLowest -> surfaceContainerLowest
        ColorSchemeKeyTokens.SurfaceDim -> surfaceDim
        ColorSchemeKeyTokens.Tertiary -> tertiary
        ColorSchemeKeyTokens.TertiaryContainer -> tertiaryContainer
        else -> Color.Unspecified
    }
}

/**
 * CompositionLocal used to pass [ColorScheme] down the tree.
 *
 * Setting the value here is typically done as part of [MaterialTheme]. To retrieve the current
 * value of this CompositionLocal, use [MaterialTheme.colorScheme].
 */
internal val LocalColorScheme = staticCompositionLocalOf { lightColorScheme() }

/**
 * Composition Local used to check if [ColorScheme.applyTonalElevation] will be applied down the
 * tree.
 *
 * Setting this value to false will cause all subsequent surfaces down the tree to not apply
 * tonalElevation.
 */
val LocalTonalElevationEnabled = staticCompositionLocalOf { true }

/**
 * A low level of alpha used to represent disabled components, such as text in a disabled Button.
 */
internal const val DisabledAlpha = 0.38f

/**
 * Converts a color token key to the local color scheme provided by the theme The color is
 * subscribed to [LocalColorScheme] changes.
 */
internal val ColorSchemeKeyTokens.value: Color
    @ReadOnlyComposable @Composable get() = MaterialTheme.colorScheme.fromToken(this)
