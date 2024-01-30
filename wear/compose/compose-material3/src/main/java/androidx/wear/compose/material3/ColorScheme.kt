/*
 * Copyright 2023 The Android Open Source Project
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
package androidx.wear.compose.material3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.wear.compose.material3.tokens.ColorSchemeKeyTokens
import androidx.wear.compose.material3.tokens.ColorTokens

/**
 * A [ColorScheme] holds all the named color parameters for a [MaterialTheme].
 *
 * Color schemes are designed to be harmonious, ensure accessible text, and distinguish UI
 * elements and surfaces from one another.
 *
 * The Material color system and custom schemes provide default values for color as a starting point
 * for customization.
 *
 * To learn more about color schemes,
 * see [Material Design Color System](https://m3.material.io/styles/color/the-color-system/color-roles).
 *
 * @property primary The primary color is the color displayed most frequently across your app’s
 * screens and components.
 * @property primaryDim is less prominent than [primary] for component backgrounds
 * @property primaryContainer is a standout container color for key components.
 * @property onPrimary Color used for text and icons displayed on top of the primary color.
 * @property onPrimaryContainer The color (and state variants) that should be used for content on
 * top of [primaryContainer].
 * @property secondary The secondary color provides more ways to accent and distinguish your
 * product.
 * @property secondaryDim is less prominent than [secondary] for component backgrounds.
 * @property secondaryContainer A tonal color to be used in containers.
 * @property onSecondary Color used for text and icons displayed on top of the secondary color.
 * @property onSecondaryContainer The color (and state variants) that should be used for content on
 * top of [secondaryContainer].
 * @property tertiary The tertiary color that can be used to balance primary and secondary
 * colors, or bring heightened attention to an element.
 * @property tertiaryDim A less prominent tertiary color that can be used to balance
 * primary and secondary colors, or bring heightened attention to an element.
 * @property tertiaryContainer A tonal color to be used in containers.
 * @property onTertiary Color used for text and icons displayed on top of the tertiary color.
 * @property onTertiaryContainer The color (and state variants) that should be used for content on
 * top of [tertiaryContainer].
 * @property surfaceDim A surface color used for large containment components
 * such as Card and Button with low prominence.
 * @property surface The main surface color that affect surfaces of components with large
 * containment areas, such as Card and Button.
 * @property surfaceBright A surface color used for large containment components
 * such Card and Button with high prominence.
 * @property onSurface Color used for text and icons displayed on top of the surface color.
 * @property onSurfaceVariant The color for secondary text and icons on top of
 * [surface].
 * @property outline The main color for primary outline components.
 * The outline color role adds contrast for accessibility purposes.
 * @property outlineVariant The secondary color for secondary outline components.
 * @property background The background color that appears behind other content.
 * @property onBackground Color used for text and icons displayed on top of the background color.
 * @property error The error color is used to indicate errors.
 * @property onError Color used for text and icons displayed on top of the error color.
 */@Stable
public class ColorScheme(
    primary: Color = ColorTokens.Primary,
    primaryDim: Color = ColorTokens.PrimaryDim,
    primaryContainer: Color = ColorTokens.PrimaryContainer,
    onPrimary: Color = ColorTokens.OnPrimary,
    onPrimaryContainer: Color = ColorTokens.OnPrimaryContainer,
    secondary: Color = ColorTokens.Secondary,
    secondaryDim: Color = ColorTokens.SecondaryDim,
    secondaryContainer: Color = ColorTokens.SecondaryContainer,
    onSecondary: Color = ColorTokens.OnSecondary,
    onSecondaryContainer: Color = ColorTokens.OnSecondaryContainer,
    tertiary: Color = ColorTokens.Tertiary,
    tertiaryDim: Color = ColorTokens.TertiaryDim,
    tertiaryContainer: Color = ColorTokens.TertiaryContainer,
    onTertiary: Color = ColorTokens.OnTertiary,
    onTertiaryContainer: Color = ColorTokens.OnTertiaryContainer,
    surfaceDim: Color = ColorTokens.SurfaceDim,
    surface: Color = ColorTokens.Surface,
    surfaceBright: Color = ColorTokens.SurfaceBright,
    onSurface: Color = ColorTokens.OnSurface,
    onSurfaceVariant: Color = ColorTokens.OnSurfaceVariant,
    outline: Color = ColorTokens.Outline,
    outlineVariant: Color = ColorTokens.OutlineVariant,
    background: Color = ColorTokens.Background,
    onBackground: Color = ColorTokens.OnBackground,
    error: Color = ColorTokens.Error,
    onError: Color = ColorTokens.OnError,
) {
    /**
     * [primary] is the main color used across screens and components
     */
    public var primary: Color by mutableStateOf(primary)
        internal set

    /**
     * [primaryDim] is less prominent than [primary] for component backgrounds
     */
    public var primaryDim: Color by mutableStateOf(primaryDim)
        internal set

    /**
     * [primaryContainer] is a standout container color for key components
     */
    public var primaryContainer: Color by mutableStateOf(primaryContainer)
        internal set

    /**
     * [onPrimary] is for text and icons shown against the Primary and primaryDim colors
     */
    public var onPrimary: Color by mutableStateOf(onPrimary)
        internal set

    /**
     * [onPrimaryContainer] is a contrast-passing color shown against the primaryContainer
     */
    public var onPrimaryContainer: Color by mutableStateOf(onPrimaryContainer)
        internal set

    /**
     * [secondary] is an accent color used across screens and components
     */
    public var secondary: Color by mutableStateOf(secondary)
        internal set

    /**
     * [secondaryDim] is less prominent than [secondary] for component backgrounds
     */
    public var secondaryDim: Color by mutableStateOf(secondaryDim)

    /**
     * [secondaryContainer] is a less prominent container color than [primaryContainer],
     * for components like tonal buttons
     */
    public var secondaryContainer: Color by mutableStateOf(secondaryContainer)
        internal set

    /**
     * [onSecondary] is for text and icons shown against the Secondary and SecondaryDim colors
     */
    public var onSecondary: Color by mutableStateOf(onSecondary)
        internal set

    /**
     * [onSecondaryContainer] is a contrast-passing color shown against the secondaryContainer
     */
    public var onSecondaryContainer: Color by mutableStateOf(onSecondaryContainer)
        internal set

    /**
     * [tertiary] is a complementary color to create contrast and draw attention to elements
     */
    public var tertiary: Color by mutableStateOf(tertiary)
        internal set

    /**
     * [tertiaryDim] is less prominent than [tertiary] for component backgrounds
     */
    public var tertiaryDim: Color by mutableStateOf(tertiaryDim)
        internal set

    /**
     * [tertiaryContainer] is a contrasting container color for components
     */
    public var tertiaryContainer: Color by mutableStateOf(tertiaryContainer)
        internal set

    /**
     * [onTertiary] is for text and icons shown against the Tertiary and tertiaryDim colors
     */
    public var onTertiary: Color by mutableStateOf(onTertiary)
        internal set

    /**
     * [onTertiaryContainer] is a contrast-passing color shown against the tertiaryContainer
     */
    public var onTertiaryContainer: Color by mutableStateOf(onTertiaryContainer)
        internal set

    /**
     * [surfaceDim] is a surface color used for large containment components, with the
     * lowest prominence behind Surface and surfaceBright
     */
    public var surfaceDim: Color by mutableStateOf(surfaceDim)
        internal set

    /**
     * [surface] is the main color for large containment components like card and button backgrounds
     */
    public var surface: Color by mutableStateOf(surface)
        internal set

    /**
     * [surfaceBright] is a surface color used for large containment components, with
     * the highest prominence, ahead of Surface and surfaceDim
     */
    public var surfaceBright: Color by mutableStateOf(surfaceBright)
        internal set

    /**
     * [onSurface] for primary text and icons shown against the
     * [surface], [surfaceDim] and [surfaceBright]
     */
    public var onSurface: Color by mutableStateOf(onSurface)
        internal set

    /**
     * [onSurfaceVariant] for secondary text and icons on
     * [surface], [surfaceDim] and [surfaceBright]
     */
    public var onSurfaceVariant: Color by mutableStateOf(onSurfaceVariant)
        internal set

    /**
     * [outline] is the main color for primary outline components
     */
    public var outline: Color by mutableStateOf(outline)
        internal set

    /**
     * [outlineVariant] is the secondary color for secondary outline components
     */
    public var outlineVariant: Color by mutableStateOf(outlineVariant)
        internal set

    /**
     * [background] is the static color used behind all texts and components
     */
    public var background: Color by mutableStateOf(background)
        internal set

    /**
     * [onBackground] is used for text and icons shown against the background color
     */
    public var onBackground: Color by mutableStateOf(onBackground)
        internal set

    /**
     * [error] indicates errors and emergency states
     */
    public var error: Color by mutableStateOf(error)
        internal set

    /**
     * [onError] is used for text and icons on the error color
     */
    public var onError: Color by mutableStateOf(onError)
        internal set

    /**
     * Returns a copy of this Colors, optionally overriding some of the values.
     */
    public fun copy(
        primary: Color = this.primary,
        primaryDim: Color = this.primaryDim,
        primaryContainer: Color = this.primaryContainer,
        onPrimary: Color = this.onPrimary,
        onPrimaryContainer: Color = this.onPrimaryContainer,
        secondary: Color = this.secondary,
        secondaryDim: Color = this.secondaryDim,
        secondaryContainer: Color = this.secondaryContainer,
        onSecondary: Color = this.onSecondary,
        onSecondaryContainer: Color = this.onSecondaryContainer,
        tertiary: Color = this.tertiary,
        tertiaryDim: Color = this.tertiaryDim,
        tertiaryContainer: Color = this.tertiaryContainer,
        onTertiary: Color = this.onTertiary,
        onTertiaryContainer: Color = this.onTertiaryContainer,
        surfaceDim: Color = this.surfaceDim,
        surface: Color = this.surface,
        surfaceBright: Color = this.surfaceBright,
        onSurface: Color = this.onSurface,
        onSurfaceVariant: Color = this.onSurfaceVariant,
        outline: Color = this.outline,
        outlineVariant: Color = this.outlineVariant,
        background: Color = this.background,
        onBackground: Color = this.onBackground,
        error: Color = this.error,
        onError: Color = this.onError
    ): ColorScheme = ColorScheme(
        primary = primary,
        primaryDim = primaryDim,
        primaryContainer = primaryContainer,
        onPrimary = onPrimary,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        secondaryDim = secondaryDim,
        secondaryContainer = secondaryContainer,
        onSecondary = onSecondary,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        tertiaryDim = tertiaryDim,
        tertiaryContainer = tertiaryContainer,
        onTertiary = onTertiary,
        onTertiaryContainer = onTertiaryContainer,
        surfaceDim = surfaceDim,
        surface = surface,
        surfaceBright = surfaceBright,
        onSurface = onSurface,
        onSurfaceVariant = onSurfaceVariant,
        outline = outline,
        outlineVariant = outlineVariant,
        background = background,
        onBackground = onBackground,
        error = error,
        onError = onError
    )

    override fun toString(): String {
        return "Colors(" +
            "primary=$primary, " +
            "primaryDim=$primaryDim, " +
            "primaryContainer=$primaryContainer, " +
            "onPrimary=$onPrimary, " +
            "onPrimaryContainer=$onPrimaryContainer, " +
            "secondary=$secondary, " +
            "secondaryDim=$secondaryDim, " +
            "secondaryContainer=$secondaryContainer, " +
            "onSecondary=$onSecondary, " +
            "onSecondaryContainer=$onSecondaryContainer, " +
            "tertiary=$tertiary, " +
            "tertiaryDim=$tertiaryDim, " +
            "tertiaryContainer=$tertiaryContainer, " +
            "onTertiary=$onTertiary, " +
            "onTertiaryContainer=$onTertiaryContainer, " +
            "surfaceDim=$surfaceDim, " +
            "surface=$surface, " +
            "surfaceBright=$surfaceBright, " +
            "onSurface=$onSurface, " +
            "onSurfaceVariant=$onSurfaceVariant, " +
            "outline=$outline, " +
            "outlineVariant=$outlineVariant, " +
            "background=$background, " +
            "onBackground=$onBackground, " +
            "error=$error, " +
            "onError=$onError" +
            ")"
    }
}

/**
 * The Material color system contains pairs of colors that are typically used for the background
 * and content color inside a component. For example, a Button typically uses `primary` for its
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
 * the theme's [ColorScheme], then returns [Color.Unspecified].
 *
 * @see contentColorFor
 */
public fun ColorScheme.contentColorFor(backgroundColor: Color): Color {
    return when (backgroundColor) {
        primary, primaryDim -> onPrimary
        primaryContainer -> onPrimaryContainer
        secondary, secondaryDim -> onSecondary
        secondaryContainer -> onSecondaryContainer
        tertiary, tertiaryDim -> onTertiary
        tertiaryContainer -> onTertiaryContainer
        surface, surfaceDim, surfaceBright -> onSurface
        background -> onBackground
        error -> onError
        else -> Color.Unspecified
    }
}

/**
 * The Material color system contains pairs of colors that are typically used for the background
 * and content color inside a component. For example, a Button typically uses `primary` for its
 * background, and `onPrimary` for the color of its content (usually text or iconography).
 *
 * This function tries to match the provided [backgroundColor] to a 'background' color in this
 * [ColorScheme], and then will return the corresponding color used for content. For example, when
 * [backgroundColor] is [ColorScheme.primary], this will return [ColorScheme.onPrimary].
 *
 * If [backgroundColor] does not match a background color in the theme, this will return
 * the current value of [LocalContentColor] as a best-effort color.
 *
 * @return the matching content color for [backgroundColor]. If [backgroundColor] is not present in
 * the theme's [ColorScheme], then returns the current value of [LocalContentColor].
 *
 * @see ColorScheme.contentColorFor
 */
@Composable
@ReadOnlyComposable
public fun contentColorFor(backgroundColor: Color): Color =
    MaterialTheme.colorScheme
        .contentColorFor(backgroundColor)
        .takeOrElse { LocalContentColor.current }

/**
 * Updates the internal values of the given [ColorScheme] with values from the [other] [ColorScheme]. This
 * allows efficiently updating a subset of [ColorScheme], without recomposing every composable that
 * consumes values from [LocalColorScheme].
 *
 * Because [ColorScheme] is very wide-reaching, and used by many expensive composables in the
 * hierarchy, providing a new value to [LocalColorScheme] causes every composable consuming
 * [LocalColorScheme] to recompose, which is prohibitively expensive in cases such as animating one
 * color in the theme. Instead, [ColorScheme] is internally backed by [mutableStateOf], and this
 * function mutates the internal state of [this] to match values in [other]. This means that any
 * changes will mutate the internal state of [this], and only cause composables that are reading
 * the specific changed value to recompose.
 */
internal fun ColorScheme.updateColorSchemeFrom(other: ColorScheme) {
    primary = other.primary
    primaryDim = other.primaryDim
    primaryContainer = other.primaryContainer
    onPrimary = other.onPrimary
    onPrimaryContainer = other.onPrimaryContainer
    secondary = other.secondary
    secondaryDim = other.secondaryDim
    secondaryContainer = other.secondaryContainer
    onSecondary = other.onSecondary
    onSecondaryContainer = other.onSecondaryContainer
    tertiary = other.tertiary
    tertiaryDim = other.tertiaryDim
    tertiaryContainer = other.tertiaryContainer
    onTertiary = other.onTertiary
    onTertiaryContainer = other.onTertiaryContainer
    surfaceDim = other.surfaceDim
    surface = other.surface
    surfaceBright = other.surfaceBright
    onSurface = other.onSurface
    onSurfaceVariant = other.onSurfaceVariant
    outline = other.outline
    outlineVariant = other.outlineVariant
    background = other.background
    onBackground = other.onBackground
    error = other.error
    onError = other.onError
}

/**
 * Helper function for component color tokens. Here is an example on how to use component color
 * tokens:
 * ``MaterialTheme.colorScheme.fromToken(FilledButtonTokens.ContainerColor)``
 */
internal fun ColorScheme.fromToken(value: ColorSchemeKeyTokens): Color {
    return when (value) {
        ColorSchemeKeyTokens.Primary -> primary
        ColorSchemeKeyTokens.PrimaryDim -> primaryDim
        ColorSchemeKeyTokens.PrimaryContainer -> primaryContainer
        ColorSchemeKeyTokens.OnPrimary -> onPrimary
        ColorSchemeKeyTokens.OnPrimaryContainer -> onPrimaryContainer
        ColorSchemeKeyTokens.Secondary -> secondary
        ColorSchemeKeyTokens.SecondaryDim -> secondaryDim
        ColorSchemeKeyTokens.SecondaryContainer -> secondaryContainer
        ColorSchemeKeyTokens.OnSecondary -> onSecondary
        ColorSchemeKeyTokens.OnSecondaryContainer -> onSecondaryContainer
        ColorSchemeKeyTokens.Tertiary -> tertiary
        ColorSchemeKeyTokens.TertiaryDim -> tertiaryDim
        ColorSchemeKeyTokens.TertiaryContainer -> tertiaryContainer
        ColorSchemeKeyTokens.OnTertiary -> onTertiary
        ColorSchemeKeyTokens.OnTertiaryContainer -> onTertiaryContainer
        ColorSchemeKeyTokens.SurfaceDim -> surfaceDim
        ColorSchemeKeyTokens.Surface -> surface
        ColorSchemeKeyTokens.SurfaceBright -> surfaceBright
        ColorSchemeKeyTokens.OnSurface -> onSurface
        ColorSchemeKeyTokens.OnSurfaceVariant -> onSurfaceVariant
        ColorSchemeKeyTokens.Outline -> outline
        ColorSchemeKeyTokens.OutlineVariant -> outlineVariant
        ColorSchemeKeyTokens.Background -> background
        ColorSchemeKeyTokens.OnBackground -> onBackground
        ColorSchemeKeyTokens.Error -> error
        ColorSchemeKeyTokens.OnError -> onError
    }
}

internal val LocalColorScheme = staticCompositionLocalOf<ColorScheme> { ColorScheme() }

/**
 * Convert given color to disabled color.
 * @param disabledAlpha Alpha used to represent disabled colors.
 */
@Composable
internal fun Color.toDisabledColor(disabledAlpha: Float = ContentAlpha.disabled) =
    this.copy(alpha = this.alpha * disabledAlpha)

/**
 * Converts a color token key to the local color scheme provided by the theme.
 * The color references the [LocalColorScheme].
 */
internal val ColorSchemeKeyTokens.value: Color
    @ReadOnlyComposable
    @Composable
    get() = MaterialTheme.colorScheme.fromToken(this)
