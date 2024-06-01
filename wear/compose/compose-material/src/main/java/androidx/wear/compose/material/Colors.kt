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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse

@Immutable
@Stable
public class Colors(
    val primary: Color = Color(0xFFAECBFA),
    val primaryVariant: Color = Color(0xFF8AB4F8),
    val secondary: Color = Color(0xFFFDE293),
    val secondaryVariant: Color = Color(0xFF594F33),
    val background: Color = Color.Black,
    val surface: Color = Color(0xFF303133),
    val error: Color = Color(0xFFEE675C),
    val onPrimary: Color = Color(0xFF303133),
    val onSecondary: Color = Color(0xFF303133),
    val onBackground: Color = Color.White,
    val onSurface: Color = Color.White,
    val onSurfaceVariant: Color = Color(0xFFDADCE0),
    val onError: Color = Color(0xFF000000)
) {

    /** Returns a copy of this Colors, optionally overriding some of the values. */
    public fun copy(
        primary: Color = this.primary,
        primaryVariant: Color = this.primaryVariant,
        secondary: Color = this.secondary,
        secondaryVariant: Color = this.secondaryVariant,
        background: Color = this.background,
        surface: Color = this.surface,
        error: Color = this.error,
        onPrimary: Color = this.onPrimary,
        onSecondary: Color = this.onSecondary,
        onBackground: Color = this.onBackground,
        onSurface: Color = this.onSurface,
        onSurfaceVariant: Color = this.onSurfaceVariant,
        onError: Color = this.onError
    ): Colors =
        Colors(
            primary = primary,
            primaryVariant = primaryVariant,
            secondary = secondary,
            secondaryVariant = secondaryVariant,
            background = background,
            surface = surface,
            error = error,
            onPrimary = onPrimary,
            onSecondary = onSecondary,
            onBackground = onBackground,
            onSurface = onSurface,
            onSurfaceVariant = onSurfaceVariant,
            onError = onError
        )

    override fun toString(): String {
        return "Colors(" +
            "primary=$primary, " +
            "primaryVariant=$primaryVariant, " +
            "secondary=$secondary, " +
            "secondaryVariant=$secondaryVariant, " +
            "background=$background, " +
            "surface=$surface, " +
            "error=$error, " +
            "onPrimary=$onPrimary, " +
            "onSecondary=$onSecondary, " +
            "onBackground=$onBackground, " +
            "onSurface=$onSurface, " +
            "onSurfaceVariant=$onSurfaceVariant, " +
            "onError=$onError" +
            ")"
    }
}

/**
 * The Material color system contains pairs of colors that are typically used for the background and
 * content color inside a component. For example, a [Button] typically uses `primary` for its
 * background, and `onPrimary` for the color of its content (usually text or iconography).
 *
 * This function tries to match the provided [backgroundColor] to a 'background' color in this
 * [Colors], and then will return the corresponding color used for content. For example, when
 * [backgroundColor] is [Colors.primary], this will return [Colors.onPrimary].
 *
 * If [backgroundColor] does not match a background color in the theme, this will return
 * [Color.Unspecified].
 *
 * @return the matching content color for [backgroundColor]. If [backgroundColor] is not present in
 *   the theme's [Colors], then returns [Color.Unspecified].
 * @see contentColorFor
 */
public fun Colors.contentColorFor(backgroundColor: Color): Color {
    return when (backgroundColor) {
        primary -> onPrimary
        primaryVariant -> onPrimary
        secondary -> onSecondary
        secondaryVariant -> onSecondary
        background -> onBackground
        surface -> onSurface
        error -> onError
        else -> Color.Unspecified
    }
}

/**
 * The Material color system contains pairs of colors that are typically used for the background and
 * content color inside a component. For example, a [Button] typically uses `primary` for its
 * background, and `onPrimary` for the color of its content (usually text or iconography).
 *
 * This function tries to match the provided [backgroundColor] to a 'background' color in this
 * [Colors], and then will return the corresponding color used for content. For example, when
 * [backgroundColor] is [Colors.primary], this will return [Colors.onPrimary].
 *
 * If [backgroundColor] does not match a background color in the theme, this will return the current
 * value of [LocalContentColor] as a best-effort color.
 *
 * @return the matching content color for [backgroundColor]. If [backgroundColor] is not present in
 *   the theme's [Colors], then returns the current value of [LocalContentColor].
 * @see Colors.contentColorFor
 */
@Composable
@ReadOnlyComposable
public fun contentColorFor(backgroundColor: Color): Color =
    MaterialTheme.colors.contentColorFor(backgroundColor).takeOrElse { LocalContentColor.current }

/**
 * Convert given color to disabled color.
 *
 * @param disabledContentAlpha Alpha used to represent disabled content colors.
 */
@Composable
internal fun Color.toDisabledColor(disabledContentAlpha: Float = ContentAlpha.disabled) =
    this.copy(alpha = disabledContentAlpha)

internal val LocalColors = staticCompositionLocalOf<Colors> { Colors() }
