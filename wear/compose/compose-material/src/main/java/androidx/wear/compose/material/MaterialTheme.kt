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
package androidx.wear.compose.material

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.wear.compose.foundation.LocalSwipeToDismissBackgroundScrimColor
import androidx.wear.compose.foundation.LocalSwipeToDismissContentScrimColor

// TODO: Provide references to the Wear material design specs.
/**
 * MaterialTheme defines the styling principles from the WearOS Material design specification
 * which extends the Material design specification.
 *
 * Wear Material components from package/sub-packages in [androidx.wear.compose.material] use values
 * provided here when retrieving default values.
 *
 * It defines colors as specified in the
 * [Wear Material Color theme spec](https://developer.android.com/training/wearables/components/theme#color),
 * typography defined in the
 * [Wear Material Type Scale spec](https://developer.android.com/training/wearables/components/theme#typography),
 * and shapes defined in the [Wear Shape scheme](https://).
 *
 * All values may be set by providing this component with the [colors][Colors],
 * [typography][Typography], and [shapes][Shapes] attributes. Use this to configure the
 * overall theme of elements within this MaterialTheme.
 *
 * Any values that are not set will inherit the current value from the theme, falling back to the
 * defaults if there is no parent MaterialTheme. This allows using a MaterialTheme at the
 * top of your application, and then separate MaterialTheme(s) for different screens / parts of
 * your UI, overriding only the parts of the theme definition that need to change.
 *
 * For more information, see the
 * [Theming](https://developer.android.com/training/wearables/components/theme)
 * guide.
 *
 * @param colors A complete definition of the Wear Material Color theme for this hierarchy
 * @param typography A set of text styles to be used as this hierarchy's typography system
 * @param shapes A set of shapes to be used by the components in this hierarchy
 */
@Composable
public fun MaterialTheme(
    colors: Colors = MaterialTheme.colors,
    typography: Typography = MaterialTheme.typography,
    shapes: Shapes = MaterialTheme.shapes,
    content: @Composable () -> Unit
) {
    val rippleIndication = rippleOrFallbackImplementation()
    val selectionColors = rememberTextSelectionColors(colors)
    @Suppress("DEPRECATION_ERROR")
    CompositionLocalProvider(
        LocalColors provides colors,
        LocalShapes provides shapes,
        LocalTypography provides typography,
        LocalContentAlpha provides ContentAlpha.high,
        LocalIndication provides rippleIndication,
        // TODO: b/304985887 - remove after one stable release
        androidx.compose.material.ripple.LocalRippleTheme provides CompatRippleTheme,
        LocalTextSelectionColors provides selectionColors,
        LocalSwipeToDismissBackgroundScrimColor provides colors.background,
        LocalSwipeToDismissContentScrimColor provides colors.background
    ) {
        ProvideTextStyle(value = typography.body1, content = content)
    }
}

public object MaterialTheme {
    public val colors: Colors
        @ReadOnlyComposable
        @Composable
        get() = LocalColors.current

    public val typography: Typography
        @ReadOnlyComposable
        @Composable
        get() = LocalTypography.current

    public val shapes: Shapes
        @ReadOnlyComposable
        @Composable
        get() = LocalShapes.current
}
