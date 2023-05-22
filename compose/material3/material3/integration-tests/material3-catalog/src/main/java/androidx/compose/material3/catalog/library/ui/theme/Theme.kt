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

package androidx.compose.material3.catalog.library.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.catalog.library.model.ColorMode
import androidx.compose.material3.catalog.library.model.FontScaleMode
import androidx.compose.material3.catalog.library.model.TextDirection
import androidx.compose.material3.catalog.library.model.Theme
import androidx.compose.material3.catalog.library.model.ThemeMode
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat

@Composable
fun CatalogTheme(
    theme: Theme,
    content: @Composable () -> Unit
) {
    val colorScheme =
        if (theme.colorMode == ColorMode.Dynamic &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val context = LocalContext.current
            colorSchemeFromThemeMode(
                themeMode = theme.themeMode,
                lightColorScheme = dynamicLightColorScheme(context),
                darkColorScheme = dynamicDarkColorScheme(context),
            )
        } else if (theme.colorMode == ColorMode.Custom) {
            colorSchemeFromThemeMode(
                themeMode = theme.themeMode,
                lightColorScheme = LightCustomColorScheme,
                darkColorScheme = DarkCustomColorScheme,
            )
        } else {
            colorSchemeFromThemeMode(
                themeMode = theme.themeMode,
                lightColorScheme = lightColorScheme(),
                darkColorScheme = darkColorScheme(),
            )
        }

    val layoutDirection = when (theme.textDirection) {
        TextDirection.LTR -> LayoutDirection.Ltr
        TextDirection.RTL -> LayoutDirection.Rtl
        TextDirection.System -> LocalLayoutDirection.current
    }

    val view = LocalView.current
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    SideEffect {
        WindowCompat.getInsetsController(context.findActivity().window, view)
            .isAppearanceLightStatusBars = !darkTheme
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalDensity provides
            Density(
                density = LocalDensity.current.density,
                fontScale = if (theme.fontScaleMode == FontScaleMode.System) {
                    LocalDensity.current.fontScale
                } else {
                    theme.fontScale
                }
            )
    ) {
        // TODO: Remove M2 MaterialTheme when using only M3 components
        androidx.compose.material.MaterialTheme(
            colors = if (darkTheme) darkColors() else lightColors()
        ) {
            MaterialTheme(
                colorScheme = colorScheme,
                content = content,
            )
        }
    }
}

@Composable
fun colorSchemeFromThemeMode(
    themeMode: ThemeMode,
    lightColorScheme: ColorScheme,
    darkColorScheme: ColorScheme
): ColorScheme {
    return when (themeMode) {
        ThemeMode.Light -> lightColorScheme
        ThemeMode.Dark -> darkColorScheme
        ThemeMode.System -> if (!isSystemInDarkTheme()) {
            lightColorScheme
        } else {
            darkColorScheme
        }
    }
}

private val LightCustomColorScheme = lightColorScheme(
    primary = Color(0xFF006E2C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF43B55F),
    onPrimaryContainer = Color(0xFF004117),
    inversePrimary = Color(0xFF6DDD81),
    secondary = Color(0xFF3F6743),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFC2F0C2),
    onSecondaryContainer = Color(0xFF466F4A),
    tertiary = Color(0xFF005EB3),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF5EA1FF),
    onTertiaryContainer = Color(0xFF00376C),
    background = Color(0xFFF5FBF0),
    onBackground = Color(0xFF171D17),
    surface = Color(0xFFF5FBF0),
    onSurface = Color(0xFF171D17),
    surfaceVariant = Color(0xFFD9E6D6),
    onSurfaceVariant = Color(0xFF3E4A3E),
    inverseSurface = Color(0xFF2C322B),
    inverseOnSurface = Color(0xFFECF3E8),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF6C786A),
    outlineVariant = Color(0xFFBDCABA),
    scrim = Color(0xFF000000),
    surfaceTint = Color(0xFF006E2C),
    surfaceContainerHighest = Color(0xFFDEE4DA),
    surfaceContainerHigh = Color(0xFFE4EADF),
    surfaceContainer = Color(0xFFE9F0E5),
    surfaceContainerLow = Color(0xFFEFF6EB),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceBright = Color(0xFFF5FBF0),
    surfaceDim = Color(0xFFD5DCD1)
)

private val DarkCustomColorScheme = darkColorScheme(
    primary = Color(0xFF6DDD81),
    onPrimary = Color(0xFF003914),
    primaryContainer = Color(0xFF008738),
    onPrimaryContainer = Color(0xFFF7FFF2),
    inversePrimary = Color(0xFF006E2C),
    secondary = Color(0xFFA5D2A6),
    onSecondary = Color(0xFF0F3819),
    secondaryContainer = Color(0xFF1D4524),
    onSecondaryContainer = Color(0xFF87B389),
    tertiary = Color(0xFFA7C8FF),
    onTertiary = Color(0xFF003061),
    tertiaryContainer = Color(0xFF0774D9),
    onTertiaryContainer = Color(0xFFFDFCFF),
    background = Color(0xFF0F150F),
    onBackground = Color(0xFFDEE4DA),
    surface = Color(0xFF0F150F),
    onSurface = Color(0xFFDEE4DA),
    surfaceVariant = Color(0xFF3E4A3E),
    onSurfaceVariant = Color(0xFFBDCABA),
    inverseSurface = Color(0xFFDEE4DA),
    inverseOnSurface = Color(0xFF2C322B),
    error = Color(0xFFFFB4A9),
    onError = Color(0xFF680003),
    errorContainer = Color(0xFF930006),
    onErrorContainer = Color(0xFFFFDAD4),
    outline = Color(0xFF6C786A),
    outlineVariant = Color(0xFF3E4A3E),
    scrim = Color(0xFF000000),
    surfaceTint = Color(0xFF6DDD81),
    surfaceContainerHighest = Color(0xFF30362F),
    surfaceContainerHigh = Color(0xFF252C25),
    surfaceContainer = Color(0xFF1B211B),
    surfaceContainerLow = Color(0xFF171D17),
    surfaceContainerLowest = Color(0xFF0A100A),
    surfaceBright = Color(0xFF343B34),
    surfaceDim = Color(0xFF0F150F)
)

private tailrec fun Context.findActivity(): Activity =
    when (this) {
        is Activity -> this
        is ContextWrapper -> this.baseContext.findActivity()
        else -> throw IllegalArgumentException("Could not find activity!")
    }
