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

import androidx.compose.Composable
import androidx.compose.Providers
import androidx.compose.remember
import androidx.ui.foundation.Indication
import androidx.ui.foundation.IndicationAmbient
import androidx.ui.foundation.ProvideTextStyle
import androidx.ui.material.ripple.RippleIndication

/**
 * A MaterialTheme defines the styling principles from the Material design specification.
 *
 * Material components such as [Button] and [Checkbox] use values provided here when retrieving
 * default values.
 *
 * It defines colors as specified in the [Material Color theme creation spec](https://material.io/design/color/the-color-system.html#color-theme-creation),
 * typography defined in the [Material Type Scale spec](https://material.io/design/typography/the-type-system.html#type-scale),
 * and shapes defined in the [Shape scheme](https://material.io/design/shape/applying-shape-to-ui.html#shape-scheme).
 *
 * All values may be set by providing this component with the [colors][ColorPalette],
 * [typography][Typography], and [shapes][Shapes] attributes. Use this to configure the overall
 * theme of elements within this MaterialTheme.
 *
 * Any values that are not set will inherit the current value from the theme, falling back to the
 * defaults if there is no parent MaterialTheme. This allows using a MaterialTheme at the top
 * of your application, and then separate MaterialTheme(s) for different screens / parts of your
 * UI, overriding only the parts of the theme definition that need to change.
 *
 * @sample androidx.ui.material.samples.MaterialThemeSample
 *
 * @param colors A complete definition of the Material Color theme for this hierarchy
 * @param typography A set of text styles to be used as this hierarchy's typography system
 * @param shapes A set of shapes to be used by the components in this hierarchy
 */
@Composable
fun MaterialTheme(
    colors: ColorPalette = MaterialTheme.colors,
    typography: Typography = MaterialTheme.typography,
    shapes: Shapes = MaterialTheme.shapes,
    content: @Composable () -> Unit
) {
    ProvideColorPalette(colors) {
        val indicationFactory: @Composable () -> Indication = remember { { RippleIndication() } }
        Providers(
            IndicationAmbient provides indicationFactory,
            TypographyAmbient provides typography,
            ShapesAmbient provides shapes
        ) {
            ProvideTextStyle(value = typography.body1, children = content)
        }
    }
}

/**
 * Contains functions to access the current theme values provided at the call site's position in
 * the hierarchy.
 */
object MaterialTheme {
    /**
     * Retrieves the current [ColorPalette] at the call site's position in the hierarchy.
     *
     * @sample androidx.ui.material.samples.ThemeColorSample
     */
    @Composable
    val colors: ColorPalette
        get() = ColorAmbient.current

    /**
     * Retrieves the current [Typography] at the call site's position in the hierarchy.
     *
     * @sample androidx.ui.material.samples.ThemeTextStyleSample
     */
    @Composable
    val typography: Typography
        get() = TypographyAmbient.current

    /**
     * Retrieves the current [Shapes] at the call site's position in the hierarchy.
     */
    @Composable
    val shapes: Shapes
        get() = ShapesAmbient.current
}
