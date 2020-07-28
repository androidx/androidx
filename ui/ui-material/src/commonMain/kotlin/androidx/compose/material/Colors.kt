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

package androidx.compose.material

import androidx.compose.runtime.Ambient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Providers
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticAmbientOf
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.foundation.contentColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.useOrElse

/**
 * Collection of colors in the
 * [Material color specification](https://material.io/design/color/the-color-system.html#color-theme-creation)
 *
 * To create a light set of colors, use [lightColors]
 * To create a dark set of colors, use [darkColors]
 */
interface Colors {
    /**
     * The primary color is the color displayed most frequently across your app’s screens and
     * components.
     */
    val primary: Color
    /**
     * The primary variant color is used to distinguish two elements of the app using the primary
     * color, such as the top app bar and the system bar.
     */
    val primaryVariant: Color
    /**
     * The secondary color provides more ways to accent and distinguish your product.
     * Secondary colors are best for:
     * - Floating action buttons
     * - Selection controls, like checkboxes and radio buttons
     * - Highlighting selected text
     * - Links and headlines
     */
    val secondary: Color
    /**
     * The secondary variant color is used to distinguish two elements of the app using the
     * secondary color.
     */
    val secondaryVariant: Color
    /**
     * The background color appears behind scrollable content.
     */
    val background: Color
    /**
     * The surface color is used on surfaces of components, such as cards, sheets and menus.
     */
    val surface: Color
    /**
     * The error color is used to indicate error within components, such as text fields.
     */
    val error: Color
    /**
     * Color used for text and icons displayed on top of the primary color.
     */
    val onPrimary: Color
    /**
     * Color used for text and icons displayed on top of the secondary color.
     */
    val onSecondary: Color
    /**
     * Color used for text and icons displayed on top of the background color.
     */
    val onBackground: Color
    /**
     * Color used for text and icons displayed on top of the surface color.
     */
    val onSurface: Color
    /**
     * Color used for text and icons displayed on top of the error color.
     */
    val onError: Color
    /**
     * Whether this Colors is considered as a 'light' or 'dark' set of colors. This affects
     * default behavior for some components: for example, in a light theme a [TopAppBar] will use
     * [primary] by default for its background color, when in a dark theme it will use [surface].
     */
    val isLight: Boolean
}

/**
 * Creates a complete color definition for the
 * [Material color specification](https://material.io/design/color/the-color-system.html#color-theme-creation)
 * using the default light theme values.
 *
 * @see darkColors
 */
fun lightColors(
    primary: Color = Color(0xFF6200EE),
    primaryVariant: Color = Color(0xFF3700B3),
    secondary: Color = Color(0xFF03DAC6),
    secondaryVariant: Color = Color(0xFF018786),
    background: Color = Color.White,
    surface: Color = Color.White,
    error: Color = Color(0xFFB00020),
    onPrimary: Color = Color.White,
    onSecondary: Color = Color.Black,
    onBackground: Color = Color.Black,
    onSurface: Color = Color.Black,
    onError: Color = Color.White
): Colors = ObservableColors(
    primary,
    primaryVariant,
    secondary,
    secondaryVariant,
    background,
    surface,
    error,
    onPrimary,
    onSecondary,
    onBackground,
    onSurface,
    onError,
    true
)

/**
 * Creates a complete color definition for the
 * [Material color specification](https://material.io/design/color/the-color-system.html#color-theme-creation)
 * using the default dark theme values.
 *
 * @see lightColors
 */
fun darkColors(
    primary: Color = Color(0xFFBB86FC),
    primaryVariant: Color = Color(0xFF3700B3),
    secondary: Color = Color(0xFF03DAC6),
    background: Color = Color(0xFF121212),
    surface: Color = Color(0xFF121212),
    error: Color = Color(0xFFCF6679),
    onPrimary: Color = Color.Black,
    onSecondary: Color = Color.Black,
    onBackground: Color = Color.White,
    onSurface: Color = Color.White,
    onError: Color = Color.Black
): Colors = ObservableColors(
    primary,
    primaryVariant,
    secondary,
    // Secondary and secondary variant are the same in dark mode, as contrast should be
    // higher so there is no need for the variant.
    secondary,
    background,
    surface,
    error,
    onPrimary,
    onSecondary,
    onBackground,
    onSurface,
    onError,
    false
)

/**
 * Tries to match [color] to a color in this Colors, and then returns the corresponding
 * `on` color.
 *
 * For example, when [color] is [Colors.primary], this will return
 * [Colors.onPrimary]. If [color] is not present in the theme, this will return `null`.
 *
 * @return the matching `on` color for [color]. If [color] is not part of the theme's
 * [Colors], then returns [Color.Unset].
 *
 * @see contentColorFor
 */
fun Colors.contentColorFor(color: Color): Color {
    return when (color) {
        primary -> onPrimary
        primaryVariant -> onPrimary
        secondary -> onSecondary
        secondaryVariant -> onSecondary
        background -> onBackground
        surface -> onSurface
        error -> onError
        else -> Color.Unset
    }
}

/**
 * Tries to match [color] to a color in the current [Colors], and then returns the
 * corresponding `on` color. If [color] can not be matched to the palette, then this will return
 * the existing value for [contentColor] at this point in the tree.
 *
 * @see Colors.contentColorFor
 */
@Composable
fun contentColorFor(color: Color) =
    MaterialTheme.colors.contentColorFor(color).useOrElse { contentColor() }

/**
 * Default observable backing implementation for [Colors].
 *
 * Typically we would just change the value of the [ColorAmbient] ambient when the theme changes, but
 * in the case of wide-reaching data such as colors in the [MaterialTheme], this will cause almost
 * every UI component on a screen to be recomposed. In reality, we only want to recompose
 * components that consume the specific color(s) that have been changed - so this default
 * implementation is intended to be memoized in the ambient, and then when a new immutable
 * [Colors] is provided, we can simply diff and update any values that need to be changed.
 * Because the internal values are provided by an State delegate class, components consuming the
 * specific color will be recomposed, while everything else will remain the same. This allows for
 * large performance improvements when the theme is being changed, especially if it is being
 * animated.
 */
private class ObservableColors(
    primary: Color,
    primaryVariant: Color,
    secondary: Color,
    secondaryVariant: Color,
    background: Color,
    surface: Color,
    error: Color,
    onPrimary: Color,
    onSecondary: Color,
    onBackground: Color,
    onSurface: Color,
    onError: Color,
    isLight: Boolean
) : Colors {

    constructor(colors: Colors) : this(
        primary = colors.primary,
        primaryVariant = colors.primaryVariant,
        secondary = colors.secondary,
        secondaryVariant = colors.secondaryVariant,
        background = colors.background,
        surface = colors.surface,
        error = colors.error,
        onPrimary = colors.onPrimary,
        onSecondary = colors.onSecondary,
        onBackground = colors.onBackground,
        onSurface = colors.onSurface,
        onError = colors.onError,
        isLight = colors.isLight
    )

    override var primary by mutableStateOf(primary, structuralEqualityPolicy())
    override var primaryVariant by mutableStateOf(primaryVariant, structuralEqualityPolicy())
    override var secondary by mutableStateOf(secondary, structuralEqualityPolicy())
    override var secondaryVariant by mutableStateOf(secondaryVariant, structuralEqualityPolicy())
    override var background by mutableStateOf(background, structuralEqualityPolicy())
    override var surface by mutableStateOf(surface, structuralEqualityPolicy())
    override var error by mutableStateOf(error, structuralEqualityPolicy())
    override var onPrimary by mutableStateOf(onPrimary, structuralEqualityPolicy())
    override var onSecondary by mutableStateOf(onSecondary, structuralEqualityPolicy())
    override var onBackground by mutableStateOf(onBackground, structuralEqualityPolicy())
    override var onSurface by mutableStateOf(onSurface, structuralEqualityPolicy())
    override var onError by mutableStateOf(onError, structuralEqualityPolicy())
    override var isLight by mutableStateOf(isLight, structuralEqualityPolicy())
}

/**
 * Updates the internal values of the given [ObservableColors] with values from the [other]
 * [Colors].
 */
private fun ObservableColors.updateColorsFrom(other: Colors): ObservableColors {
    primary = other.primary
    primaryVariant = other.primaryVariant
    secondary = other.secondary
    secondaryVariant = other.secondaryVariant
    background = other.background
    surface = other.surface
    error = other.error
    onPrimary = other.onPrimary
    onSecondary = other.onSecondary
    onBackground = other.onBackground
    onSurface = other.onSurface
    onError = other.onError
    isLight = other.isLight
    return this
}

/**
 * Memoizes and mutates the given [colors] if it is an [ObservableColors], otherwise
 * just provides the given [colors] through the [ColorAmbient] [Ambient].
 */
@Composable
internal fun ProvideColors(colors: Colors, content: @Composable () -> Unit) {
    val palette = when (colors) {
        is ObservableColors -> {
            (remember { ObservableColors(colors) }).updateColorsFrom(colors)
        }
        else -> colors
    }
    Providers(ColorAmbient provides palette, children = content)
}

/**
 * Ambient used to pass [Colors] down the tree.
 *
 * To retrieve the current value of this ambient, use [MaterialTheme.colors].
 */
internal val ColorAmbient = staticAmbientOf { lightColors() }
