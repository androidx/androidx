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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.wear.compose.material3.tokens.ColorSchemeKeyTokens
import androidx.wear.compose.material3.tokens.ColorTokens

/**
 * A [ColorScheme] holds all the named color parameters for a [MaterialTheme].
 *
 * Color schemes are designed to be harmonious, ensure accessible text, and distinguish UI elements
 * and surfaces from one another.
 *
 * The Material color system and custom schemes provide default values for color as a starting point
 * for customization.
 *
 * To learn more about color schemes, see
 * [Material Design Color System](https://m3.material.io/styles/color/the-color-system/color-roles).
 *
 * @property primary The primary color is the color displayed most frequently across your app’s
 *   screens and components.
 * @property primaryDim is less prominent than [primary] for component backgrounds
 * @property primaryContainer is a standout container color for key components.
 * @property onPrimary Color used for text and icons displayed on top of the primary color.
 * @property onPrimaryContainer The color (and state variants) that should be used for content on
 *   top of [primaryContainer].
 * @property secondary The secondary color provides more ways to accent and distinguish your
 *   product.
 * @property secondaryDim is less prominent than [secondary] for component backgrounds.
 * @property secondaryContainer A tonal color to be used in containers.
 * @property onSecondary Color used for text and icons displayed on top of the secondary color.
 * @property onSecondaryContainer The color (and state variants) that should be used for content on
 *   top of [secondaryContainer].
 * @property tertiary The tertiary color that can be used to balance primary and secondary colors,
 *   or bring heightened attention to an element.
 * @property tertiaryDim A less prominent tertiary color that can be used to balance primary and
 *   secondary colors, or bring heightened attention to an element.
 * @property tertiaryContainer A tonal color to be used in containers.
 * @property onTertiary Color used for text and icons displayed on top of the tertiary color.
 * @property onTertiaryContainer The color (and state variants) that should be used for content on
 *   top of [tertiaryContainer].
 * @property surfaceContainerLow A surface color used for large containment components such as Card
 *   and Button with low prominence.
 * @property surfaceContainer The main surface color that affect surfaces of components with large
 *   containment areas, such as Card and Button.
 * @property surfaceContainerHigh A surface color used for large containment components such Card
 *   and Button with high prominence.
 * @property onSurface Color used for text and icons displayed on top of the surface color.
 * @property onSurfaceVariant The color for secondary text and icons on top of [surfaceContainer].
 * @property outline The main color for primary outline components. The outline color role adds
 *   contrast for accessibility purposes.
 * @property outlineVariant The secondary color for secondary outline components.
 * @property background The background color that appears behind other content.
 * @property onBackground Color used for text and icons displayed on top of the background color.
 * @property error Color that indicates remove, delete, close or dismiss actions, such as Swipe to
 *   Reveal. Added as an errorContainer alternative that is slightly less alarming and urgent color.
 * @property onError Color used for text and icons displayed on top of the error color.
 * @property errorContainer Color that indicates errors or emergency actions, such as safety alerts.
 *   This color is for use-cases that are more alarming and urgent than the error color.
 * @property onErrorContainer Color used for text and icons on the errorContainer color.
 */
@Immutable
@Stable
class ColorScheme(
    val primary: Color = ColorTokens.Primary,
    val primaryDim: Color = ColorTokens.PrimaryDim,
    val primaryContainer: Color = ColorTokens.PrimaryContainer,
    val onPrimary: Color = ColorTokens.OnPrimary,
    val onPrimaryContainer: Color = ColorTokens.OnPrimaryContainer,
    val secondary: Color = ColorTokens.Secondary,
    val secondaryDim: Color = ColorTokens.SecondaryDim,
    val secondaryContainer: Color = ColorTokens.SecondaryContainer,
    val onSecondary: Color = ColorTokens.OnSecondary,
    val onSecondaryContainer: Color = ColorTokens.OnSecondaryContainer,
    val tertiary: Color = ColorTokens.Tertiary,
    val tertiaryDim: Color = ColorTokens.TertiaryDim,
    val tertiaryContainer: Color = ColorTokens.TertiaryContainer,
    val onTertiary: Color = ColorTokens.OnTertiary,
    val onTertiaryContainer: Color = ColorTokens.OnTertiaryContainer,
    val surfaceContainerLow: Color = ColorTokens.SurfaceContainerLow,
    val surfaceContainer: Color = ColorTokens.SurfaceContainer,
    val surfaceContainerHigh: Color = ColorTokens.SurfaceContainerHigh,
    val onSurface: Color = ColorTokens.OnSurface,
    val onSurfaceVariant: Color = ColorTokens.OnSurfaceVariant,
    val outline: Color = ColorTokens.Outline,
    val outlineVariant: Color = ColorTokens.OutlineVariant,
    val background: Color = ColorTokens.Background,
    val onBackground: Color = ColorTokens.OnBackground,
    val error: Color = ColorTokens.Error,
    val onError: Color = ColorTokens.OnError,
    val errorContainer: Color = ColorTokens.ErrorContainer,
    val onErrorContainer: Color = ColorTokens.OnErrorContainer,
) {
    /** Returns a copy of this Colors, optionally overriding some of the values. */
    fun copy(
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
        surfaceContainerLow: Color = this.surfaceContainerLow,
        surfaceContainer: Color = this.surfaceContainer,
        surfaceContainerHigh: Color = this.surfaceContainerHigh,
        onSurface: Color = this.onSurface,
        onSurfaceVariant: Color = this.onSurfaceVariant,
        outline: Color = this.outline,
        outlineVariant: Color = this.outlineVariant,
        background: Color = this.background,
        onBackground: Color = this.onBackground,
        error: Color = this.error,
        onError: Color = this.onError,
        errorContainer: Color = this.errorContainer,
        onErrorContainer: Color = this.onErrorContainer,
    ): ColorScheme =
        ColorScheme(
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
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            onSurface = onSurface,
            onSurfaceVariant = onSurfaceVariant,
            outline = outline,
            outlineVariant = outlineVariant,
            background = background,
            onBackground = onBackground,
            error = error,
            onError = onError,
            errorContainer = errorContainer,
            onErrorContainer = onErrorContainer,
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
            "surfaceContainerLow=$surfaceContainerLow, " +
            "surfaceContainer=$surfaceContainer, " +
            "surfaceContainerHigh=$surfaceContainerHigh, " +
            "onSurface=$onSurface, " +
            "onSurfaceVariant=$onSurfaceVariant, " +
            "outline=$outline, " +
            "outlineVariant=$outlineVariant, " +
            "background=$background, " +
            "onBackground=$onBackground, " +
            "onError=$onError," +
            "errorContainer=$errorContainer, " +
            "onErrorContainer=$onErrorContainer" +
            ")"
    }

    // Button Colors
    internal var defaultButtonColorsCached: ButtonColors? = null
    internal var defaultFilledVariantButtonColorsCached: ButtonColors? = null
    internal var defaultFilledTonalButtonColorsCached: ButtonColors? = null
    internal var defaultOutlinedButtonColorsCached: ButtonColors? = null
    internal var defaultChildButtonColorsCached: ButtonColors? = null

    // Icon Button
    internal var defaultIconButtonColorsCached: IconButtonColors? = null
    internal var defaultFilledIconButtonColorsCached: IconButtonColors? = null
    internal var defaultFilledVariantIconButtonColorsCached: IconButtonColors? = null
    internal var defaultFilledTonalIconButtonColorsCached: IconButtonColors? = null
    internal var defaultOutlinedIconButtonColorsCached: IconButtonColors? = null

    // Icon Toggle Button
    internal var defaultIconToggleButtonColorsCached: IconToggleButtonColors? = null

    // Text Button
    internal var defaultTextButtonColorsCached: TextButtonColors? = null
    internal var defaultFilledTextButtonColorsCached: TextButtonColors? = null
    internal var defaultFilledVariantTextButtonColorsCached: TextButtonColors? = null
    internal var defaultFilledTonalTextButtonColorsCached: TextButtonColors? = null
    internal var defaultOutlinedTextButtonColorsCached: TextButtonColors? = null

    // Text Toggle Button
    internal var defaultTextToggleButtonColorsCached: TextToggleButtonColors? = null

    // Card
    internal var defaultCardColorsCached: CardColors? = null
    internal var defaultOutlinedCardColorsCached: CardColors? = null

    // Toggle Button
    internal var defaultSwitchButtonColorsCached: SwitchButtonColors? = null
    internal var defaultSplitSwitchButtonColorsCached: SplitSwitchButtonColors? = null

    // Checkbox Button
    internal var defaultCheckboxButtonColorsCached: CheckboxButtonColors? = null
    internal var defaultSplitCheckboxButtonColorsCached: SplitCheckboxButtonColors? = null

    // Radio Button
    internal var defaultRadioButtonColorsCached: RadioButtonColors? = null
    internal var defaultSplitRadioButtonColorsCached: SplitRadioButtonColors? = null

    // Progress Indicator
    internal var defaultProgressIndicatorColorsCached: ProgressIndicatorColors? = null

    // Level Indicator
    internal var defaultLevelIndicatorColorsCached: LevelIndicatorColors? = null

    // Confirmation
    internal var defaultConfirmationColorsCached: ConfirmationColors? = null
    internal var defaultSuccessConfirmationColorsCached: ConfirmationColors? = null
    internal var defaultFailureConfirmationColorsCached: ConfirmationColors? = null

    // Open on Phone dialog
    internal var mDefaultOpenOnPhoneDialogColorsCached: OpenOnPhoneDialogColors? = null

    // Picker
    internal var defaultTimePickerColorsCached: TimePickerColors? = null
    internal var defaultDatePickerColorsCached: DatePickerColors? = null
}

/**
 * The Material color system contains pairs of colors that are typically used for the background and
 * content color inside a component. For example, a Button typically uses `primary` for its
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
fun ColorScheme.contentColorFor(backgroundColor: Color): Color {
    return when (backgroundColor) {
        primary,
        primaryDim -> onPrimary
        primaryContainer -> onPrimaryContainer
        secondary,
        secondaryDim -> onSecondary
        secondaryContainer -> onSecondaryContainer
        tertiary,
        tertiaryDim -> onTertiary
        tertiaryContainer -> onTertiaryContainer
        surfaceContainer,
        surfaceContainerLow,
        surfaceContainerHigh -> onSurface
        background -> onBackground
        error -> onError
        errorContainer -> onErrorContainer
        else -> Color.Unspecified
    }
}

/**
 * The Material color system contains pairs of colors that are typically used for the background and
 * content color inside a component. For example, a Button typically uses `primary` for its
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
fun contentColorFor(backgroundColor: Color): Color =
    MaterialTheme.colorScheme.contentColorFor(backgroundColor).takeOrElse {
        LocalContentColor.current
    }

/**
 * Helper function for component color tokens. Here is an example on how to use component color
 * tokens: ``MaterialTheme.colorScheme.fromToken(FilledButtonTokens.ContainerColor)``
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
        ColorSchemeKeyTokens.SurfaceContainerLow -> surfaceContainerLow
        ColorSchemeKeyTokens.SurfaceContainer -> surfaceContainer
        ColorSchemeKeyTokens.SurfaceContainerHigh -> surfaceContainerHigh
        ColorSchemeKeyTokens.OnSurface -> onSurface
        ColorSchemeKeyTokens.OnSurfaceVariant -> onSurfaceVariant
        ColorSchemeKeyTokens.Outline -> outline
        ColorSchemeKeyTokens.OutlineVariant -> outlineVariant
        ColorSchemeKeyTokens.Background -> background
        ColorSchemeKeyTokens.OnBackground -> onBackground
        ColorSchemeKeyTokens.Error -> error
        ColorSchemeKeyTokens.OnError -> onError
        ColorSchemeKeyTokens.ErrorContainer -> errorContainer
        ColorSchemeKeyTokens.OnErrorContainer -> onErrorContainer
    }
}

/**
 * CompositionLocal used to pass [ColorScheme] down the tree.
 *
 * Setting the value here is typically done as part of [MaterialTheme]. To retrieve the current
 * value of this CompositionLocal, use [MaterialTheme.colorScheme].
 */
internal val LocalColorScheme = staticCompositionLocalOf<ColorScheme> { ColorScheme() }

/**
 * Convert given color to disabled color.
 *
 * @param disabledAlpha Alpha used to represent disabled colors.
 */
internal fun Color.toDisabledColor(disabledAlpha: Float = DisabledContentAlpha) =
    this.copy(alpha = this.alpha * disabledAlpha)

/**
 * Converts a color token key to the local color scheme provided by the theme. The color references
 * the [LocalColorScheme].
 */
internal val ColorSchemeKeyTokens.value: Color
    @ReadOnlyComposable @Composable get() = MaterialTheme.colorScheme.fromToken(this)

internal const val DisabledContentAlpha = 0.38f
internal const val DisabledContainerAlpha = 0.12f
internal const val DisabledBorderAlpha = 0.20f
