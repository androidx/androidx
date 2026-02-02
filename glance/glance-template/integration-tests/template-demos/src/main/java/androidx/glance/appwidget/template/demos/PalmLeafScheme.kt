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

package androidx.glance.appwidget.template.demos

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.glance.material3.ColorProviders

/** Custom Color theme for the Palm Leaf themed demo templates */
object PalmLeafScheme {
    val md_theme_light_primary = Color(0xFF026E00)
    val md_theme_light_onPrimary = Color(0xFFFFFFFF)
    val md_theme_light_primaryContainer = Color(0xFFD7E8CD)
    val md_theme_light_onPrimaryContainer = Color(0xFF002200)
    val md_theme_light_secondary = Color(0xFFA900A9)
    val md_theme_light_onSecondary = Color(0xFFFFFFFF)
    val md_theme_light_secondaryContainer = Color(0xFFFFD7F5)
    val md_theme_light_onSecondaryContainer = Color(0xFF380038)
    val md_theme_light_tertiary = Color(0xFF006A6A)
    val md_theme_light_onTertiary = Color(0xFFFFFFFF)
    val md_theme_light_tertiaryContainer = Color(0xFF00FBFB)
    val md_theme_light_onTertiaryContainer = Color(0xFF002020)
    val md_theme_light_error = Color(0xFFBA1A1A)
    val md_theme_light_errorContainer = Color(0xFFFFDAD6)
    val md_theme_light_onError = Color(0xFFFFFFFF)
    val md_theme_light_onErrorContainer = Color(0xFF410002)
    val md_theme_light_background = Color(0xFFFFFBFF)
    val md_theme_light_onBackground = Color(0xFF1E1C00)
    val md_theme_light_surface = Color(0xFFFFFBFF)
    val md_theme_light_onSurface = Color(0xFF1E1C00)
    val md_theme_light_surfaceVariant = Color(0xFFDFE4D7)
    val md_theme_light_onSurfaceVariant = Color(0xFF43483F)
    val md_theme_light_outline = Color(0xFF73796E)
    val md_theme_light_inverseOnSurface = Color(0xFFFFF565)
    val md_theme_light_inverseSurface = Color(0xFF353200)
    val md_theme_light_inversePrimary = Color(0xFF02E600)
    val md_theme_light_shadow = Color(0xFF000000)
    val md_theme_light_surfaceTint = Color(0xFF026E00)

    val md_theme_dark_primary = Color(0xFF02E600)
    val md_theme_dark_onPrimary = Color(0xFF013A00)
    val md_theme_dark_primaryContainer = Color(0xFFD7E8CD)
    val md_theme_dark_onPrimaryContainer = Color(0xFF77FF61)
    val md_theme_dark_secondary = Color(0xFFFFABF3)
    val md_theme_dark_onSecondary = Color(0xFF5B005B)
    val md_theme_dark_secondaryContainer = Color(0xFF810081)
    val md_theme_dark_onSecondaryContainer = Color(0xFFFFD7F5)
    val md_theme_dark_tertiary = Color(0xFF00DDDD)
    val md_theme_dark_onTertiary = Color(0xFF003737)
    val md_theme_dark_tertiaryContainer = Color(0xFF004F4F)
    val md_theme_dark_onTertiaryContainer = Color(0xFF00FBFB)
    val md_theme_dark_error = Color(0xFFFFB4AB)
    val md_theme_dark_errorContainer = Color(0xFF93000A)
    val md_theme_dark_onError = Color(0xFF690005)
    val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)
    val md_theme_dark_background = Color(0xFF1E1C00)
    val md_theme_dark_onBackground = Color(0xFFF2E720)
    val md_theme_dark_surface = Color(0xFF1E1C00)
    val md_theme_dark_onSurface = Color(0xFFF2E720)
    val md_theme_dark_surfaceVariant = Color(0xFF43483F)
    val md_theme_dark_onSurfaceVariant = Color(0xFFC3C8BC)
    val md_theme_dark_outline = Color(0xFF8D9387)
    val md_theme_dark_inverseOnSurface = Color(0xFF1E1C00)
    val md_theme_dark_inverseSurface = Color(0xFFF2E720)
    val md_theme_dark_inversePrimary = Color(0xFF026E00)
    val md_theme_dark_shadow = Color(0xFF000000)
    val md_theme_dark_surfaceTint = Color(0xFF02E600)

    val seed = Color(0xFF00FF00)

    val LightColors =
        lightColorScheme(
            primary = md_theme_light_primary,
            onPrimary = md_theme_light_onPrimary,
            primaryContainer = md_theme_light_primaryContainer,
            onPrimaryContainer = md_theme_light_onPrimaryContainer,
            secondary = md_theme_light_secondary,
            onSecondary = md_theme_light_onSecondary,
            secondaryContainer = md_theme_light_secondaryContainer,
            onSecondaryContainer = md_theme_light_onSecondaryContainer,
            tertiary = md_theme_light_tertiary,
            onTertiary = md_theme_light_onTertiary,
            tertiaryContainer = md_theme_light_tertiaryContainer,
            onTertiaryContainer = md_theme_light_onTertiaryContainer,
            error = md_theme_light_error,
            onError = md_theme_light_onError,
            errorContainer = md_theme_light_errorContainer,
            onErrorContainer = md_theme_light_onErrorContainer,
            background = md_theme_light_background,
            onBackground = md_theme_light_onBackground,
            surface = md_theme_light_surface,
            onSurface = md_theme_light_onSurface,
            surfaceVariant = md_theme_light_surfaceVariant,
            onSurfaceVariant = md_theme_light_onSurfaceVariant,
            outline = md_theme_light_outline,
            inverseSurface = md_theme_light_inverseSurface,
            inverseOnSurface = md_theme_light_inverseOnSurface,
            inversePrimary = md_theme_light_inversePrimary,
            surfaceTint = md_theme_light_surfaceTint,
        )

    val DarkColors =
        darkColorScheme(
            primary = md_theme_dark_primary,
            onPrimary = md_theme_dark_onPrimary,
            primaryContainer = md_theme_dark_primaryContainer,
            onPrimaryContainer = md_theme_dark_onPrimaryContainer,
            secondary = md_theme_dark_secondary,
            onSecondary = md_theme_dark_onSecondary,
            secondaryContainer = md_theme_dark_secondaryContainer,
            onSecondaryContainer = md_theme_dark_onSecondaryContainer,
            tertiary = md_theme_dark_tertiary,
            onTertiary = md_theme_dark_onTertiary,
            tertiaryContainer = md_theme_dark_tertiaryContainer,
            onTertiaryContainer = md_theme_dark_onTertiaryContainer,
            error = md_theme_dark_error,
            onError = md_theme_dark_onError,
            errorContainer = md_theme_dark_errorContainer,
            onErrorContainer = md_theme_dark_onErrorContainer,
            background = md_theme_dark_background,
            onBackground = md_theme_dark_onBackground,
            surface = md_theme_dark_surface,
            onSurface = md_theme_dark_onSurface,
            surfaceVariant = md_theme_dark_surfaceVariant,
            onSurfaceVariant = md_theme_dark_onSurfaceVariant,
            outline = md_theme_dark_outline,
            inverseSurface = md_theme_dark_inverseSurface,
            inverseOnSurface = md_theme_dark_inverseOnSurface,
            inversePrimary = md_theme_dark_inversePrimary,
            surfaceTint = md_theme_dark_surfaceTint,
        )

    val colors = ColorProviders(light = LightColors, dark = DarkColors)
}
