/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.integration.macrobenchmark.target.complexdifferenttypeslist.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf

object SquadTheme {
    val colors: SquadColorPalette
        @Composable @ReadOnlyComposable get() = LocalSquadColors.current

    val drawables: SquadDrawablePalette
        @Composable @ReadOnlyComposable get() = LocalSquadDrawables.current

    val typography: SquadTypography
        @Composable @ReadOnlyComposable get() = LocalSquadTypography.current

    val shapes: SquadShapes
        @Composable @ReadOnlyComposable get() = LocalSquadShapes.current
}

@Composable
fun SquadTheme(content: @Composable () -> Unit) {
    val isDarkMode = isSystemInDarkTheme()

    val materialColors =
        when {
            isDarkMode -> darkPalette
            else -> lightPalette
        }
    val squadColors =
        when {
            isDarkMode -> SquadDarkColorPalette()
            else -> SquadLightColorPalette()
        }
    val squadDrawables =
        when {
            isDarkMode -> SquadDarkDrawablesPalette()
            else -> SquadLightDrawablesPalette()
        }

    CompositionLocalProvider(
        LocalSquadColors provides squadColors,
        LocalSquadDrawables provides squadDrawables,
    ) {
        MaterialTheme(colors = materialColors, shapes = Shapes(), content = content)
    }
}

private val lightPalette =
    lightColors(
        primary = SquadColors.tomato,
        primaryVariant = SquadColors.rustyRed,
        secondary = SquadColors.sunflowerYellow,
        background = SquadColors.veryLightPink,
        surface = SquadColors.white,
        error = SquadColors.squash,
        onPrimary = SquadColors.white,
        onSecondary = SquadColors.black,
        onBackground = SquadColors.greyishBrownTwo,
        onSurface = SquadColors.brownGreyTwo,
    )

private val darkPalette =
    darkColors(
        primary = SquadColors.darkFive,
        primaryVariant = SquadColors.darkSeven,
        secondary = SquadColors.tomato,
        background = SquadColors.darkThree,
        surface = SquadColors.dark,
        error = SquadColors.squash,
        onPrimary = SquadColors.white,
        onSecondary = SquadColors.white,
        onBackground = SquadColors.white,
        onSurface = SquadColors.lightPeriwinkle,
    )

private val LocalSquadColors = compositionLocalOf<SquadColorPalette> { SquadLightColorPalette() }

private val LocalSquadDrawables =
    compositionLocalOf<SquadDrawablePalette> { SquadLightDrawablesPalette() }
