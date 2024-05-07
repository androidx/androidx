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

package androidx.tv.material3.samples

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.tv.material3.ColorScheme as TvColorScheme

@Composable
fun mapColorScheme(tvColorScheme: TvColorScheme): ColorScheme {
    @Suppress("Deprecation")
    return ColorScheme(
        primary = tvColorScheme.primary,
        onPrimary = tvColorScheme.onPrimary,
        primaryContainer = tvColorScheme.primaryContainer,
        onPrimaryContainer = tvColorScheme.onPrimaryContainer,
        inversePrimary = tvColorScheme.inversePrimary,
        secondary = tvColorScheme.secondary,
        onSecondary = tvColorScheme.onSecondary,
        secondaryContainer = tvColorScheme.secondaryContainer,
        onSecondaryContainer = tvColorScheme.onSecondaryContainer,
        tertiary = tvColorScheme.tertiary,
        onTertiary = tvColorScheme.onTertiary,
        tertiaryContainer = tvColorScheme.tertiaryContainer,
        onTertiaryContainer = tvColorScheme.onTertiaryContainer,
        background = tvColorScheme.background,
        onBackground = tvColorScheme.onBackground,
        surface = tvColorScheme.surface,
        onSurface = tvColorScheme.onSurface,
        surfaceVariant = tvColorScheme.surfaceVariant,
        onSurfaceVariant = tvColorScheme.onSurfaceVariant,
        surfaceTint = tvColorScheme.surfaceTint,
        inverseSurface = tvColorScheme.inverseSurface,
        inverseOnSurface = tvColorScheme.inverseOnSurface,
        error = tvColorScheme.error,
        onError = tvColorScheme.onError,
        errorContainer = tvColorScheme.errorContainer,
        onErrorContainer = tvColorScheme.onErrorContainer,
        outline = tvColorScheme.border,
        outlineVariant = tvColorScheme.borderVariant,
        scrim = tvColorScheme.scrim,
    )
}
