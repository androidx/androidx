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
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.ambient
import androidx.compose.effectOf
import androidx.compose.memo
import androidx.compose.unaryPlus
import androidx.ui.baseui.shape.Shape
import androidx.ui.baseui.shape.corner.CornerSizes
import androidx.ui.baseui.shape.corner.RoundedCornerShape
import androidx.ui.core.CurrentTextStyleProvider
import androidx.ui.core.dp
import androidx.ui.core.withDensity
import androidx.ui.engine.text.FontWeight
import androidx.ui.engine.text.font.FontFamily
import androidx.ui.graphics.Color
import androidx.ui.material.borders.ShapeBorder
import androidx.ui.material.ripple.CurrentRippleTheme
import androidx.ui.material.ripple.DefaultRippleEffectFactory
import androidx.ui.material.ripple.RippleTheme
import androidx.ui.painting.TextStyle

/**
 * This Component defines the styling principles from the Material design specification. It must be
 * present within a hierarchy of components that includes Material components, as it defines key
 * values such as base colors and typography.
 *
 * By default, it defines the colors as specified in the Color theme creation spec
 * (https://material.io/design/color/the-color-system.html#color-theme-creation) and the typography
 * defined in the Type Scale spec
 * (https://material.io/design/typography/the-type-system.html#type-scale).
 *
 * All values may be overriden by providing this component with the [colors] and [typography]
 * attributes. Use this to configure the overall theme of your application.
 */
@Composable
fun MaterialTheme(
    colors: MaterialColors = MaterialColors(),
    typography: MaterialTypography = MaterialTypography(),
    @Children
    children: @Composable() () -> Unit
) {
    Colors.Provider(value = colors) {
        Typography.Provider(value = typography) {
            CurrentTextStyleProvider(value = typography.body1) {
                MaterialRippleTheme {
                    MaterialButtonShapeTheme(children = children)
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
 */
val Colors = Ambient.of<MaterialColors>("colors") { error("No colors found!") }

/**
 * This Ambient holds on to the current definiton of typography for this application as described
 * by the Material spec.  You can read the values in it when creating custom components that want
 * to use Material types, as well as override the values when you want to re-style a part of your
 * hierarchy. Material components related to text such as [H1TextStyle] will refer to this Ambient
 * to obtain the values with which to style text.
 */
val Typography = Ambient.of<MaterialTypography>("typography") { error("No typography found!") }

/**
 * Data class holding color values as defined by the Material specification
 * (https://material.io/design/color/the-color-system.html#color-theme-creation).
 */
data class MaterialColors(
    /**
     * The primary color is the color displayed most frequently across your app’s screens and
     * components.
     */
    val primary: Color = Color(0xFF6200EE.toInt()),
    /**
     * The primary variant is used to distinguish two elements of the app using the primary color,
     * such as the top app bar and the system bar.
     */
    val primaryVariant: Color = Color(0xFF3700B3.toInt()),
    /**
     * The secondary color provides more ways to accent and distinguish your product.
     * Secondary colors are best for:
     * <ul>
     *     <li>Floating action buttons</li>
     *     <li>Selection controls, like sliders and switches</li>
     *     <li>Highlighting selected text</li>
     *     <li>Progress bars</li>
     *     <li>Links and headlines</li>
     * </ul>
     */
    val secondary: Color = Color(0xFF03DAC6.toInt()),
    /**
     * The secondary variant is used to distinguish two elements of the app using the secondary
     * color.
     */
    val secondaryVariant: Color = Color(0xFF018786.toInt()),
    /**
     * The background color appears behind scrollable content.
     */
    val background: Color = Color(0xFFFFFFFF.toInt()),
    /**
     * The surface color is used on surfaces of components, such as cards, sheets and menus.
     */
    val surface: Color = Color(0xFFFFFFFF.toInt()),
    /**
     * The error color is used to indicate error within components, such as text fields.
     */
    val error: Color = Color(0xFFB00020.toInt()),
    /**
     * Color used for text and icons displayed on top of the primary color.
     */
    val onPrimary: Color = Color(0xFFFFFFFF.toInt()),
    /**
     * Color used for text and icons displayed on top of the secondary color.
     */
    val onSecondary: Color = Color(0xFF000000.toInt()),
    /**
     * Color used for text and icons displayed on top of the background color.
     */
    val onBackground: Color = Color(0xFF000000.toInt()),
    /**
     * Color used for text and icons displayed on top of the surface color.
     */
    val onSurface: Color = Color(0xFF000000.toInt()),
    /**
     * Color used for text and icons displayed on top of the error color.
     */
    val onError: Color = Color(0xFFFFFFFF.toInt())
)

/**
 * Data class holding typography definitions as defined by the Material specification
 * (https://material.io/design/typography/the-type-system.html#type-scale).
 */
data class MaterialTypography(
    // TODO(clara): case
    // TODO(clara): letter spacing (specs don't match)
    // TODO(clara): b/123001228 need a font abstraction layer
    // TODO(clara): fontSize should be a Dp, translating here will loose context changes
    val h1: TextStyle = TextStyle(
        fontFamily = FontFamily("Roboto"),
        fontWeight = FontWeight.w100,
        fontSize = 96f),
    val h2: TextStyle = TextStyle(
        fontFamily = FontFamily("Roboto"),
        fontWeight = FontWeight.w100,
        fontSize = 60f),
    val h3: TextStyle = TextStyle(
        fontFamily = FontFamily("Roboto"),
        fontWeight = FontWeight.normal,
        fontSize = 48f),
    val h4: TextStyle = TextStyle(
        fontFamily = FontFamily("Roboto"),
        fontWeight = FontWeight.normal,
        fontSize = 34f),
    val h5: TextStyle = TextStyle(
        fontFamily = FontFamily("Roboto"),
        fontWeight = FontWeight.normal,
        fontSize = 24f),
    val h6: TextStyle = TextStyle(
        fontFamily = FontFamily("Roboto"),
        fontWeight = FontWeight.w500,
        fontSize = 20f),
    val subtitle1: TextStyle = TextStyle(
        fontFamily = FontFamily("Roboto"),
        fontWeight = FontWeight.normal,
        fontSize = 16f),
    val subtitle2: TextStyle = TextStyle(
        fontFamily = FontFamily("Roboto"),
        fontWeight = FontWeight.w500,
        fontSize = 14f),
    val body1: TextStyle = TextStyle(
        fontFamily = FontFamily("Roboto"),
        fontWeight = FontWeight.normal,
        fontSize = 16f),
    val body2: TextStyle = TextStyle(
        fontFamily = FontFamily("Roboto"),
        fontWeight = FontWeight.normal,
        fontSize = 14f),
    val button: TextStyle = TextStyle(
        fontFamily = FontFamily("Roboto"),
        fontWeight = FontWeight.w500,
        fontSize = 14f),
    val caption: TextStyle = TextStyle(
        fontFamily = FontFamily("Roboto"),
        fontWeight = FontWeight.normal,
        fontSize = 12f),
    val overline: TextStyle = TextStyle(
        fontFamily = FontFamily("Roboto"),
        fontWeight = FontWeight.normal,
        fontSize = 10f)
)

/**
 * Applies the default [RippleTheme] parameters based on the Material Design
 * guidelines for descendants
 */
@Composable
fun MaterialRippleTheme(@Children children: @Composable() () -> Unit) {
    val materialColors = +ambient(Colors)
    val defaultTheme = +memo {
        RippleTheme(
            factory = DefaultRippleEffectFactory,
            colorCallback = { background ->
                if (background == null || background.alpha == 0f ||
                    background.luminance() >= 0.5
                ) { // light bg
                    materialColors.primary.copy(alpha = 0.12f)
                } else { // dark bg
                    Color(0xFFFFFFFF.toInt()).copy(alpha = 0.24f)
                }
            }
        )
    }
    CurrentRippleTheme.Provider(value = defaultTheme, children = children)
}

/**
 * Helps to resolve the [Color] by applying [choosingBlock] for the [MaterialColors].
 */
@CheckResult(suggest = "+")
fun themeColor(
    choosingBlock: MaterialColors.() -> Color
) = effectOf<Color> { (+ambient(Colors)).choosingBlock() }

/**
 * Helps to resolve the [TextStyle] by applying [choosingBlock] for the [MaterialTypography].
 */
@CheckResult(suggest = "+")
fun themeTextStyle(
    choosingBlock: MaterialTypography.() -> TextStyle
) = effectOf<TextStyle> {
    var style = (+ambient(Typography)).choosingBlock()

    // TODO Text is working with pixels, but we define our theme in dps, let's convert here for now.
    // b/127345041
    if (style.fontSize != null) {
        style = style.copy(fontSize = +withDensity { style.fontSize!!.dp.toPx().value })
    }

    style
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
    val button: Shape
    // TODO(Andrey): Add shapes for Card, other surfaces? will see what we need.
)

/**
 * Ambient used to specify the default shapes for the surfaces.
 *
 * @see [MaterialButtonShapeTheme] for the default Material Design value
 */
val CurrentShapeAmbient = Ambient.of<Shapes> {
    throw IllegalStateException("No default shapes provided.")
}

/**
 * Applies the default [ShapeBorder]s for all the surfaces.
 */
@Composable
fun MaterialButtonShapeTheme(@Children children: @Composable() () -> Unit) {
    val value = +withDensity {
        Shapes(
            button = RoundedCornerShape(CornerSizes(4.dp))
        )
    }
    CurrentShapeAmbient.Provider(value = value, children = children)
}

/**
 * Helps to resolve the [Shape] by applying [choosingBlock] for the [Shapes].
 */
@CheckResult(suggest = "+")
fun themeShape(
    choosingBlock: Shapes.() -> Shape
) = effectOf<Shape> { (+ambient(CurrentShapeAmbient)).choosingBlock() }
