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
import androidx.ui.core.CurrentTextStyleProvider

/**
 * This component defines the styling principles from the Material design specification. It must be
 * present within a hierarchy of components that includes Material components, as it defines key
 * values such as base colors and typography.
 *
 * Material components such as [Button] and [Checkbox] use this definition to set default values.
 *
 * It defines colors as specified in the [Material Color theme creation spec](https://material.io/design/color/the-color-system.html#color-theme-creation)
 * and the typography defined in the [Material Type Scale spec](https://material.io/design/typography/the-type-system.html#type-scale).
 *
 * All values may be set by providing this component with the [colors][ColorPalette] and
 * [typography][Typography] attributes. Use this to configure the overall theme of your
 * application.
 *
 * @sample androidx.ui.material.samples.MaterialThemeSample
 *
 * @param colors A complete definition of the Material Color theme for this hierarchy
 * @param typography A set of text styles to be used as this hierarchy's typography system
 */
@Composable
fun MaterialTheme(
    colors: ColorPalette = lightColorPalette(),
    typography: Typography = Typography(),
    children: @Composable() () -> Unit
) {
    ProvideColorPalette(colors) {
        Providers(TypographyAmbient provides typography) {
            CurrentTextStyleProvider(value = typography.body1, children = children)
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
    fun colors() = ColorAmbient.current

    /**
     * Retrieves the current [Typography] at the call site's position in the hierarchy.
     *
     * @sample androidx.ui.material.samples.ThemeTextStyleSample
     */
    @Composable
    fun typography() = TypographyAmbient.current

    /**
     * Retrieves the current [Shapes] at the call site's position in the hierarchy.
     */
    @Composable
    fun shapes() = ShapeAmbient.current

    /**
     * Retrieves the current [EmphasisLevels] at the call site's position in the hierarchy.
     */
    @Composable
    fun emphasisLevels() = EmphasisAmbient.current
}
