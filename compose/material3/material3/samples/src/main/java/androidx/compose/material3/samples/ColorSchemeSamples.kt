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

package androidx.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

@Composable
@Sampled
fun ColorSchemeFixedAccentColorSample() {
    data class FixedAccentColors(
        val primaryFixed: Color,
        val onPrimaryFixed: Color,
        val secondaryFixed: Color,
        val onSecondaryFixed: Color,
        val tertiaryFixed: Color,
        val onTertiaryFixed: Color,
        val primaryFixedDim: Color,
        val secondaryFixedDim: Color,
        val tertiaryFixedDim: Color,
    )
    val material3LightColors = lightColorScheme()
    val material3DarkColors = darkColorScheme()
    fun getFixedAccentColors() = FixedAccentColors(
        primaryFixed = material3LightColors.primaryContainer,
        onPrimaryFixed = material3LightColors.onPrimaryContainer,
        secondaryFixed = material3LightColors.secondaryContainer,
        onSecondaryFixed = material3LightColors.onSecondaryContainer,
        tertiaryFixed = material3LightColors.tertiaryContainer,
        onTertiaryFixed = material3LightColors.onTertiaryContainer,
        primaryFixedDim = material3DarkColors.primary,
        secondaryFixedDim = material3DarkColors.secondary,
        tertiaryFixedDim = material3DarkColors.tertiary
    )
    val LocalFixedAccentColors = compositionLocalOf { getFixedAccentColors() }

    @Composable
    fun MyMaterialTheme(
        fixedAccentColors: FixedAccentColors = LocalFixedAccentColors.current,
        content: @Composable () -> Unit
    ) {
        MaterialTheme(
            colorScheme = if (isSystemInDarkTheme()) material3DarkColors else material3LightColors
        ) {
            CompositionLocalProvider(LocalFixedAccentColors provides fixedAccentColors) {
                // Content has access to fixedAccentColors in both light and dark theme.
                content()
            }
        }
    }
}
