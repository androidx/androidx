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

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.wear.compose.foundation.LocalSwipeToDismissBackgroundScrimColor
import androidx.wear.compose.foundation.LocalSwipeToDismissContentScrimColor

/**
 * MaterialTheme defines the styling principles from the Wear Material3 design specification which
 * extends the Material design specification.
 *
 * Wear Material components from package/sub-packages in [androidx.wear.compose.material3] use
 * values provided here when retrieving default values.
 *
 * All values may be set by providing this component with the [colors][ColorScheme],
 * [typography][Typography], and [shapes][Shapes] attributes. Use this to configure the overall
 * theme of elements within this MaterialTheme.
 *
 * Any values that are not set will inherit the current value from the theme, falling back to the
 * defaults if there is no parent MaterialTheme. This allows using a MaterialTheme at the top of
 * your application, and then separate MaterialTheme(s) for different screens / parts of your UI,
 * overriding only the parts of the theme definition that need to change.
 *
 * For more information, see the
 * [Theming](https://developer.android.com/training/wearables/components/theme) guide.
 *
 * @param colorScheme A complete definition of the Wear Material Color theme for this hierarchy
 * @param typography A set of text styles to be used as this hierarchy's typography system
 * @param shapes A set of shapes to be used by the components in this hierarchy
 * @param motionScheme a set of motion specs used to animate content for this hierarchy.
 * @param content Slot for composable content displayed with this theme
 */
// TODO(b/273543423) Update references to Material3 design specs
@Composable
fun MaterialTheme(
    colorScheme: ColorScheme = MaterialTheme.colorScheme,
    typography: Typography = MaterialTheme.typography,
    shapes: Shapes = MaterialTheme.shapes,
    motionScheme: MotionScheme = MaterialTheme.motionScheme,
    content: @Composable () -> Unit
) {
    val rippleIndication = ripple()
    val selectionColors = rememberTextSelectionColors(colorScheme)
    CompositionLocalProvider(
        LocalColorScheme provides colorScheme,
        LocalShapes provides shapes,
        LocalTypography provides typography,
        LocalMotionScheme provides motionScheme,
        LocalIndication provides rippleIndication,
        LocalTextSelectionColors provides selectionColors,
        LocalSwipeToDismissBackgroundScrimColor provides colorScheme.background,
        LocalSwipeToDismissContentScrimColor provides colorScheme.background
    ) {
        ProvideTextStyle(value = typography.bodyLarge, content = content)
    }
}

object MaterialTheme {
    val colorScheme: ColorScheme
        @ReadOnlyComposable @Composable get() = LocalColorScheme.current

    val typography: Typography
        @ReadOnlyComposable @Composable get() = LocalTypography.current

    val shapes: Shapes
        @ReadOnlyComposable @Composable get() = LocalShapes.current

    val motionScheme: MotionScheme
        @ReadOnlyComposable @Composable get() = LocalMotionScheme.current
}

@Composable
/*@VisibleForTesting*/
internal fun rememberTextSelectionColors(colorScheme: ColorScheme): TextSelectionColors {
    val primaryColor = colorScheme.primary
    return remember(primaryColor) {
        TextSelectionColors(
            handleColor = primaryColor,
            backgroundColor = primaryColor.copy(alpha = TextSelectionBackgroundOpacity),
        )
    }
}

/*@VisibleForTesting*/
internal const val TextSelectionBackgroundOpacity = 0.4f
