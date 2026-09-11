/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.material3.integration.a2ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun DemoTheme(content: @Composable () -> Unit) {
    val colorScheme =
        if (isSystemInDarkTheme()) {
            DarkColorScheme
        } else {
            LightColorScheme
        }

    MaterialTheme(colorScheme = colorScheme, content = content)
}

private val LightColorScheme: ColorScheme =
    lightColorScheme(
        primary = Color(0xFF144D2A),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFCBE6D5),
        onPrimaryContainer = Color(0xFF144D2A),
        inversePrimary = Color(0xFF98DBB2),
        secondary = Color(0xFFDEECE8),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFE4F8F5),
        onSecondaryContainer = Color(0xFF005E53),
        tertiary = Color(0xFFEF613D),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFE6F5),
        onTertiaryContainer = Color(0xFFEF613D),
        background = Color(0xFFFFFFFF),
        onBackground = Color(0xFF1A1C16),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF1D1D1D),
        surfaceVariant = Color(0xFFEAF1EB),
        onSurfaceVariant = Color(0xFF494949),
        surfaceTint = Color(0xFF144D2A),
        inverseSurface = Color(0xFF1D1D1D),
        inverseOnSurface = Color(0xFFFFFFFF),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF93000A),
        outline = Color(0xFF75777A),
        outlineVariant = Color(0xFFC5C6CA),
        surfaceBright = Color(0xFFFFFFFF),
        surfaceContainer = Color(0xFFF5F5F5),
        surfaceContainerHigh = Color(0xFFF0F0F0),
        surfaceContainerHighest = Color(0xFFEAEAEA),
        surfaceContainerLow = Color(0xFFFAFAFA),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceDim = Color(0xFFF2F2F2),
    )

private val DarkColorScheme: ColorScheme =
    darkColorScheme(
        primary = Color(0xFFC4E1CE),
        onPrimary = Color(0xFF111111),
        primaryContainer = Color(0xFF405E4D),
        onPrimaryContainer = Color(0xFF104424),
        inversePrimary = Color(0xFF98DBB2),
        secondary = Color(0xFFD3EFE6),
        onSecondary = Color(0xFF2D332F),
        secondaryContainer = Color(0xFF48726B),
        onSecondaryContainer = Color(0xFFD6E8E5),
        tertiary = Color(0xFFF1C5BF),
        onTertiary = Color(0xFF1F1F1F),
        tertiaryContainer = Color(0xFFA63378),
        onTertiaryContainer = Color(0xFFFFE8E7),
        background = Color(0xFF1D1D1D),
        onBackground = Color(0xFFFAFAFA),
        surface = Color(0xFF1D1D1D),
        onSurface = Color(0xFFE5E5E5),
        surfaceVariant = Color(0xFF2C2C2C),
        onSurfaceVariant = Color(0xFFE0E0E0),
        surfaceTint = Color(0xFFC4E1CE),
        inverseSurface = Color(0xFF1D1D1D),
        inverseOnSurface = Color(0xFFFFFFFF),
        error = Color(0xFFFFBEBE),
        onError = Color(0xFF383838),
        errorContainer = Color(0xFFD27257),
        onErrorContainer = Color(0xFFF8C5C8),
        outline = Color(0xFF353638),
        outlineVariant = Color(0xFF606060),
        surfaceBright = Color(0xFF1D1D1D),
        surfaceContainer = Color(0xFF2A2A2A),
        surfaceContainerHigh = Color(0xFF282828),
        surfaceContainerHighest = Color(0xFF2A2A2A),
        surfaceContainerLow = Color(0xFF262626),
        surfaceContainerLowest = Color(0xFF464646),
        surfaceDim = Color(0xFF1D1D1D),
    )
