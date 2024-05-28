/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.glance.material3

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils.M3HCTToColor
import androidx.core.graphics.ColorUtils.colorToM3HCT
import androidx.glance.color.ColorProvider
import androidx.glance.color.ColorProviders
import androidx.glance.color.colorProviders
import androidx.glance.unit.ColorProvider

/**
 * Creates a Material 3 [ColorProviders] given a light and dark [ColorScheme]. Each color in the
 * theme will have a day and night mode.
 */
fun ColorProviders(light: ColorScheme, dark: ColorScheme): ColorProviders {
    return colorProviders(
        primary = ColorProvider(day = light.primary, night = dark.primary),
        onPrimary = ColorProvider(day = light.onPrimary, night = dark.onPrimary),
        primaryContainer =
            ColorProvider(day = light.primaryContainer, night = dark.primaryContainer),
        onPrimaryContainer =
            ColorProvider(day = light.onPrimaryContainer, night = dark.onPrimaryContainer),
        secondary = ColorProvider(day = light.secondary, night = dark.secondary),
        onSecondary = ColorProvider(day = light.onSecondary, night = dark.onSecondary),
        secondaryContainer =
            ColorProvider(day = light.secondaryContainer, night = dark.secondaryContainer),
        onSecondaryContainer =
            ColorProvider(day = light.onSecondaryContainer, night = dark.onSecondaryContainer),
        tertiary = ColorProvider(day = light.tertiary, night = dark.tertiary),
        onTertiary = ColorProvider(day = light.onTertiary, night = dark.onTertiary),
        tertiaryContainer =
            ColorProvider(day = light.tertiaryContainer, night = dark.tertiaryContainer),
        onTertiaryContainer =
            ColorProvider(day = light.onTertiaryContainer, night = dark.onTertiaryContainer),
        error = ColorProvider(day = light.error, night = dark.error),
        errorContainer = ColorProvider(day = light.errorContainer, night = dark.errorContainer),
        onError = ColorProvider(day = light.onError, night = dark.onError),
        onErrorContainer =
            ColorProvider(day = light.onErrorContainer, night = dark.onErrorContainer),
        background = ColorProvider(day = light.background, night = dark.background),
        onBackground = ColorProvider(day = light.onBackground, night = dark.onBackground),
        surface = ColorProvider(day = light.surface, night = dark.surface),
        onSurface = ColorProvider(day = light.onSurface, night = dark.onSurface),
        surfaceVariant = ColorProvider(day = light.surfaceVariant, night = dark.surfaceVariant),
        onSurfaceVariant =
            ColorProvider(day = light.onSurfaceVariant, night = dark.onSurfaceVariant),
        outline = ColorProvider(day = light.outline, night = dark.outline),
        inverseOnSurface =
            ColorProvider(day = light.inverseOnSurface, night = dark.inverseOnSurface),
        inverseSurface = ColorProvider(day = light.inverseSurface, night = dark.inverseSurface),
        inversePrimary = ColorProvider(day = light.inversePrimary, night = dark.inversePrimary),
        // Widget background is a widget / glace specific token it is generally derived from the
        // secondary container color.
        widgetBackground =
            ColorProvider(
                day = adjustColorToneForWidgetBackground(light.secondaryContainer),
                night = adjustColorToneForWidgetBackground(dark.secondaryContainer)
            ),
    )
}

/**
 * Creates a Material 3 [ColorProviders] given a [ColorScheme]. This is a fixed scheme and does not
 * have day/night modes.
 */
fun ColorProviders(scheme: ColorScheme): ColorProviders {
    return colorProviders(
        primary = ColorProvider(color = scheme.primary),
        onPrimary = ColorProvider(scheme.onPrimary),
        primaryContainer = ColorProvider(color = scheme.primaryContainer),
        onPrimaryContainer = ColorProvider(color = scheme.onPrimaryContainer),
        secondary = ColorProvider(color = scheme.secondary),
        onSecondary = ColorProvider(color = scheme.onSecondary),
        secondaryContainer = ColorProvider(color = scheme.secondaryContainer),
        onSecondaryContainer = ColorProvider(color = scheme.onSecondaryContainer),
        tertiary = ColorProvider(color = scheme.tertiary),
        onTertiary = ColorProvider(color = scheme.onTertiary),
        tertiaryContainer = ColorProvider(color = scheme.tertiaryContainer),
        onTertiaryContainer = ColorProvider(color = scheme.onTertiaryContainer),
        error = ColorProvider(color = scheme.error),
        onError = ColorProvider(color = scheme.onError),
        errorContainer = ColorProvider(color = scheme.errorContainer),
        onErrorContainer = ColorProvider(color = scheme.onErrorContainer),
        background = ColorProvider(color = scheme.background),
        onBackground = ColorProvider(color = scheme.onBackground),
        surface = ColorProvider(color = scheme.surface),
        onSurface = ColorProvider(color = scheme.onSurface),
        surfaceVariant = ColorProvider(color = scheme.surfaceVariant),
        onSurfaceVariant = ColorProvider(color = scheme.onSurfaceVariant),
        outline = ColorProvider(color = scheme.outline),
        inverseOnSurface = ColorProvider(color = scheme.inverseOnSurface),
        inverseSurface = ColorProvider(color = scheme.inverseSurface),
        inversePrimary = ColorProvider(color = scheme.inversePrimary),

        // Widget background is a widget / glace specific token it is generally derived from the
        // secondary container color.
        widgetBackground =
            ColorProvider(color = adjustColorToneForWidgetBackground(scheme.secondaryContainer))
    )
}

private const val WIDGET_BG_TONE_ADJUSTMENT_LIGHT = 5f
private const val WIDGET_BG_TONE_ADJUSTMENT_DARK = -10f

/**
 * Adjusts the input color to work as a widgetBackground token.
 *
 * widgetBackground is a Widgets / Glance specific role so won't be present in the original Scheme.
 * In the system it is defined as being a variation on the secondaryContainer, lighter for light
 * themes and darker for dark themes.
 */
private fun adjustColorToneForWidgetBackground(input: Color): Color {
    val hctColor = floatArrayOf(0f, 0f, 0f)
    colorToM3HCT(input.toArgb(), hctColor)
    // Check the Tone of the input color, if it is "light" (greater than 50) lighten it, otherwise
    // darken it.
    val adjustment =
        if (hctColor[2] > 50) WIDGET_BG_TONE_ADJUSTMENT_LIGHT else WIDGET_BG_TONE_ADJUSTMENT_DARK

    // Tone should be defined in the 0 - 100 range, ok to clamp here.
    val tone = (hctColor[2] + adjustment).coerceIn(0f, 100f)
    return Color(M3HCTToColor(hctColor[0], hctColor[1], tone))
}
