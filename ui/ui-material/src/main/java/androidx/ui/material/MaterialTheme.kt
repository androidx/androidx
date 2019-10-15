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

import androidx.annotation.CheckResult
import androidx.compose.Ambient
import androidx.compose.Composable
import androidx.compose.ambient
import androidx.compose.effectOf
import androidx.compose.memo
import androidx.compose.unaryPlus
import androidx.ui.core.CurrentTextStyleProvider
import androidx.ui.core.ambientDensity
import androidx.ui.core.dp
import androidx.ui.core.sp
import androidx.ui.core.withDensity
import androidx.ui.engine.geometry.Shape
import androidx.ui.foundation.shape.RectangleShape
import androidx.ui.foundation.shape.corner.RoundedCornerShape
import androidx.ui.graphics.Color
import androidx.ui.graphics.luminance
import androidx.ui.material.ripple.CurrentRippleTheme
import androidx.ui.material.ripple.DefaultRippleEffectFactory
import androidx.ui.material.ripple.RippleTheme
import androidx.ui.material.surface.Card
import androidx.ui.material.surface.CurrentBackground
import androidx.ui.text.font.FontWeight
import androidx.ui.text.font.FontFamily
import androidx.ui.text.TextStyle

/**
 * This component defines the styling principles from the Material design specification. It must be
 * present within a hierarchy of components that includes Material components, as it defines key
 * values such as base colors and typography.
 *
 * Material components such as [Button] and [Checkbox] use this definition to set default values.
 *
 * It defines colors as specified in the [Material Color theme creation spec]
 * [https://material.io/design/color/the-color-system.html#color-theme-creation] and the typography
 * defined in the [Material Type Scale spec]
 * [https://material.io/design/typography/the-type-system.html#type-scale].
 *
 * All values may be set by providing this component with the [colors][MaterialColors] and
 * [typography][MaterialTypography] attributes. Use this to configure the overall theme of your
 * application.
 *
 * @sample androidx.ui.material.samples.MaterialThemeSample
 */
@Composable
fun MaterialTheme(
    colors: MaterialColors = MaterialColors(),
    typography: MaterialTypography = MaterialTypography(),
    children: @Composable() () -> Unit
) {
    Colors.Provider(value = colors) {
        Typography.Provider(value = typography) {
            CurrentTextStyleProvider(value = typography.body1) {
                MaterialRippleTheme {
                    MaterialShapeTheme(children = children)
                }
            }
        }
    }
}

/**
 * This Ambient holds on to the current definition of colors for this application as described
 * by the Material spec. You can read the values in it when creating custom components that want
 * to use Material colors, as well as override the values when you want to re-style a part of your
 * hierarchy.
 *
 * To access values within this ambient, use [themeColor].
 */
val Colors = Ambient.of<MaterialColors>("colors") { error("No colors found!") }

/**
 * This Ambient holds on to the current definition of typography for this application as described
 * by the Material spec.  You can read the values in it when creating custom components that want
 * to use Material types, as well as override the values when you want to re-style a part of your
 * hierarchy. Material components related to text such as [Button] will use this Ambient
 * to set values with which to style children text components.
 *
 * To access values within this ambient, use [themeTextStyle].
 */
val Typography = Ambient.of<MaterialTypography>("typography") { error("No typography found!") }

/**
 * Data class holding color values as defined by the [Material color specification]
 * [https://material.io/design/color/the-color-system.html#color-theme-creation].
 */
data class MaterialColors(
    /**
     * The primary color is the color displayed most frequently across your app’s screens and
     * components.
     */
    val primary: Color = Color(0xFF6200EE),
    /**
     * The primary variant color is used to distinguish two elements of the app using the primary
     * color, such as the top app bar and the system bar.
     */
    val primaryVariant: Color = Color(0xFF3700B3),
    /**
     * The secondary color provides more ways to accent and distinguish your product.
     * Secondary colors are best for:
     * - Floating action buttons
     * - Selection controls, like sliders and switches
     * - Highlighting selected text
     * - Progress bars
     * - Links and headlines
     */
    val secondary: Color = Color(0xFF03DAC6),
    /**
     * The secondary variant color is used to distinguish two elements of the app using the
     * secondary color.
     */
    val secondaryVariant: Color = Color(0xFF018786),
    /**
     * The background color appears behind scrollable content.
     */
    val background: Color = Color.White,
    /**
     * The surface color is used on surfaces of components, such as cards, sheets and menus.
     */
    val surface: Color = Color.White,
    /**
     * The error color is used to indicate error within components, such as text fields.
     */
    val error: Color = Color(0xFFB00020),
    /**
     * Color used for text and icons displayed on top of the primary color.
     */
    val onPrimary: Color = Color.White,
    /**
     * Color used for text and icons displayed on top of the secondary color.
     */
    val onSecondary: Color = Color.Black,
    /**
     * Color used for text and icons displayed on top of the background color.
     */
    val onBackground: Color = Color.Black,
    /**
     * Color used for text and icons displayed on top of the surface color.
     */
    val onSurface: Color = Color.Black,
    /**
     * Color used for text and icons displayed on top of the error color.
     */
    val onError: Color = Color.White
)

/**
 * Data class holding typography definitions as defined by the [Material typography specification]
 * [https://material.io/design/typography/the-type-system.html#type-scale].
 */
data class MaterialTypography(
    // TODO(clara): case
    // TODO(clara): letter spacing (specs don't match)
    // TODO(clara): b/123001228 need a font abstraction layer
    // TODO(clara): fontSize should be a Dp, translating here will loose context changes
    val h1: TextStyle = TextStyle(
        fontFamily = FontFamily("Roboto"),
        fontWeight = FontWeight.W100,
        fontSize = 96.sp),
    val h2: TextStyle = TextStyle(
        fontFamily = FontFamily("Roboto"),
        fontWeight = FontWeight.W100,
        fontSize = 60.sp),
    val h3: TextStyle = TextStyle(
        fontFamily = FontFamily("Roboto"),
        fontWeight = FontWeight.Normal,
        fontSize = 48.sp),
    val h4: TextStyle = TextStyle(
        fontFamily = FontFamily("Roboto"),
        fontWeight = FontWeight.Normal,
        fontSize = 34.sp),
    val h5: TextStyle = TextStyle(
        fontFamily = FontFamily("Roboto"),
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp),
    val h6: TextStyle = TextStyle(
        fontFamily = FontFamily("Roboto"),
        fontWeight = FontWeight.W500,
        fontSize = 20.sp),
    val subtitle1: TextStyle = TextStyle(
        fontFamily = FontFamily("Roboto"),
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp),
    val subtitle2: TextStyle = TextStyle(
        fontFamily = FontFamily("Roboto"),
        fontWeight = FontWeight.W500,
        fontSize = 14.sp),
    val body1: TextStyle = TextStyle(
        fontFamily = FontFamily("Roboto"),
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp),
    val body2: TextStyle = TextStyle(
        fontFamily = FontFamily("Roboto"),
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp),
    val button: TextStyle = TextStyle(
        fontFamily = FontFamily("Roboto"),
        fontWeight = FontWeight.W500,
        fontSize = 14.sp),
    val caption: TextStyle = TextStyle(
        fontFamily = FontFamily("Roboto"),
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp),
    val overline: TextStyle = TextStyle(
        fontFamily = FontFamily("Roboto"),
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp)
)

/**
 * Applies the default [RippleTheme] parameters based on the Material Design
 * guidelines for descendants
 */
@Composable
private fun MaterialRippleTheme(children: @Composable() () -> Unit) {
    val defaultTheme = +memo {
        RippleTheme(
            factory = DefaultRippleEffectFactory,
            defaultColor = effectOf {
                val background = +ambient(CurrentBackground)
                val textColor = +textColorForBackground(background)
                when {
                    textColor != null -> textColor
                    background.alpha == 0f || background.luminance() >= 0.5 -> Color.Black
                    else -> Color.White
                }
            },
            opacity = effectOf {
                val isDarkTheme = (+themeColor { surface }).luminance() < 0.5f
                if (isDarkTheme) 0.24f else 0.12f
            }
        )
    }
    CurrentRippleTheme.Provider(value = defaultTheme, children = children)
}

/**
 * Helper effect that resolves [Color]s from the [Colors] ambient by applying [choosingBlock].
 *
 * @sample androidx.ui.material.samples.ThemeColorSample
 */
@CheckResult(suggest = "+")
fun themeColor(
    choosingBlock: MaterialColors.() -> Color
) = effectOf<Color> { (+ambient(Colors)).choosingBlock() }

/**
 * Helper effect that resolves [TextStyle]s from the [Typography] ambient by applying
 * [choosingBlock].
 *
 * @sample androidx.ui.material.samples.ThemeTextStyleSample
 */
@CheckResult(suggest = "+")
fun themeTextStyle(
    choosingBlock: MaterialTypography.() -> TextStyle
) = effectOf<TextStyle> {
    (+ambient(Typography)).choosingBlock()
}

// Shapes

/**
 * Data class holding current shapes for common surfaces like Button or Card.
 */
// TODO(Andrey): should have small, medium, large components categories. b/129278276
// See https://material.io/design/shape/applying-shape-to-ui.html#baseline-shape-values
data class Shapes(
    /**
     * Shape used for [Button]
     */
    val button: Shape,
    /**
     * Shape used for [Card]
     */
    val card: Shape
    // TODO(Andrey): Add shapes for other surfaces? will see what we need.
)

/**
 * Ambient used to specify the default shapes for the surfaces.
 *
 * @see [MaterialShapeTheme] for the default Material Design value
 */
val CurrentShapeAmbient = Ambient.of<Shapes> {
    throw IllegalStateException("No default shapes provided.")
}

/**
 * Applies the default [Shape]s for all the surfaces.
 */
@Composable
fun MaterialShapeTheme(children: @Composable() () -> Unit) {
    val value = withDensity(+ambientDensity()) {
        Shapes(
            button = RoundedCornerShape(4.dp),
            card = RectangleShape
        )
    }
    CurrentShapeAmbient.Provider(value = value, children = children)
}

/**
 * Helper effect to resolve [Shape]s from the [CurrentShapeAmbient] ambient by applying
 * [choosingBlock].
 */
@CheckResult(suggest = "+")
fun themeShape(
    choosingBlock: Shapes.() -> Shape
) = effectOf<Shape> { (+ambient(CurrentShapeAmbient)).choosingBlock() }
