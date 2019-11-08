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

import androidx.compose.Ambient
import androidx.compose.Composable
import androidx.compose.Model
import androidx.compose.memo
import androidx.compose.unaryPlus
import androidx.ui.graphics.Color
import kotlin.reflect.KProperty

/**
 * Collection of colors in the [Material color specification]
 * [https://material.io/design/color/the-color-system.html#color-theme-creation].
 */
interface ColorPalette {
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
     * - Selection controls, like sliders and switches
     * - Highlighting selected text
     * - Progress bars
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
}

/**
 * Creates a complete color definition for the [Material color specification]
 * [https://material.io/design/color/the-color-system.html#color-theme-creation].
 */
fun ColorPalette(
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
): ColorPalette = ObservableColorPalette(
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
    onError
)

/**
 * Default observable backing implementation for [ColorPalette].
 *
 * Typically we would just change the value of the [Colors] ambient when the theme changes, but
 * in the case of wide-reaching data such as colors in the [MaterialTheme], this will cause almost
 * every UI component on a screen to be recomposed. In reality, we only want to recompose
 * components that consume the specific color(s) that have been changed - so this default
 * implementation is intended to be memoized in the ambient, and then when a new immutable
 * [ColorPalette] is provided, we can simply diff and update any values that need to be changed.
 * Because the internal values are provided by an @Model delegate class, components consuming the
 * specific color will be recomposed, while everything else will remain the same. This allows for
 * large performance improvements when the theme is being changed, especially if it is being
 * animated.
 */
private class ObservableColorPalette(
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
    onError: Color
) : ColorPalette {

    constructor(colorPalette: ColorPalette) : this(
        primary = colorPalette.primary,
        primaryVariant = colorPalette.primaryVariant,
        secondary = colorPalette.secondary,
        secondaryVariant = colorPalette.secondaryVariant,
        background = colorPalette.background,
        surface = colorPalette.surface,
        error = colorPalette.error,
        onPrimary = colorPalette.onPrimary,
        onSecondary = colorPalette.onSecondary,
        onBackground = colorPalette.onBackground,
        onSurface = colorPalette.onSurface,
        onError = colorPalette.onError
    )

    override var primary by ObservableColor(primary)
    override var primaryVariant by ObservableColor(primaryVariant)
    override var secondary by ObservableColor(secondary)
    override var secondaryVariant by ObservableColor(secondaryVariant)
    override var background by ObservableColor(background)
    override var surface by ObservableColor(surface)
    override var error by ObservableColor(error)
    override var onPrimary by ObservableColor(onPrimary)
    override var onSecondary by ObservableColor(onSecondary)
    override var onBackground by ObservableColor(onBackground)
    override var onSurface by ObservableColor(onSurface)
    override var onError by ObservableColor(onError)
}

@Model
private class ObservableColor(var color: Color) {
    operator fun getValue(thisObj: Any?, property: KProperty<*>) = color

    operator fun setValue(thisObj: Any?, property: KProperty<*>, next: Color) {
        if (color != next) color = next
    }
}

/**
 * Updates the internal values of the given [ObservableColorPalette] with values from the [other]
 * [ColorPalette].
 */
private fun ObservableColorPalette.updateColorsFrom(other: ColorPalette): ObservableColorPalette {
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
    return this
}

/**
 * Memoizes and mutates the given [colorPalette] if it is an [ObservableColorPalette], otherwise
 * just provides the given palette in the [Colors] [Ambient].
 */
@Composable
internal fun ProvideColorPalette(colorPalette: ColorPalette, children: @Composable() () -> Unit) {
    val palette = when (colorPalette) {
        is ObservableColorPalette -> {
            (+memo<ObservableColorPalette> {
                ObservableColorPalette(colorPalette)
            }).updateColorsFrom(colorPalette)
        }
        else -> colorPalette
    }
    Colors.Provider(palette, children)
}

/**
 * Ambient used to pass [ColorPalette] down the tree.
 *
 * To retrieve the current value of this ambient, use [MaterialTheme.colors].
 */
internal val Colors = Ambient.of { ColorPalette() }
