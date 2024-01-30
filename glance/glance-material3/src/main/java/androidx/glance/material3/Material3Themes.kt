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
        primaryContainer = ColorProvider(
            day = light.primaryContainer,
            night = dark.primaryContainer
        ),
        onPrimaryContainer = ColorProvider(
            day = light.onPrimaryContainer,
            night = dark.onPrimaryContainer
        ),
        secondary = ColorProvider(day = light.secondary, night = dark.secondary),
        onSecondary = ColorProvider(day = light.onSecondary, night = dark.onSecondary),
        secondaryContainer = ColorProvider(
            day = light.secondaryContainer,
            night = dark.secondaryContainer
        ),
        onSecondaryContainer = ColorProvider(
            day = light.onSecondaryContainer,
            night = dark.onSecondaryContainer
        ),
        tertiary = ColorProvider(day = light.tertiary, night = dark.tertiary),
        onTertiary = ColorProvider(day = light.onTertiary, night = dark.onTertiary),
        tertiaryContainer = ColorProvider(
            day = light.tertiaryContainer,
            night = dark.tertiaryContainer
        ),
        onTertiaryContainer = ColorProvider(
            day = light.onTertiaryContainer,
            night = dark.onTertiaryContainer
        ),
        error = ColorProvider(day = light.error, night = dark.error),
        errorContainer = ColorProvider(day = light.errorContainer, night = dark.errorContainer),
        onError = ColorProvider(day = light.onError, night = dark.onError),
        onErrorContainer = ColorProvider(
            day = light.onErrorContainer,
            night = dark.onErrorContainer
        ),
        background = ColorProvider(day = light.background, night = dark.background),
        onBackground = ColorProvider(day = light.onBackground, night = dark.onBackground),
        surface = ColorProvider(day = light.surface, night = dark.surface),
        onSurface = ColorProvider(day = light.onSurface, night = dark.onSurface),
        surfaceVariant = ColorProvider(day = light.surfaceVariant, night = dark.surfaceVariant),
        onSurfaceVariant = ColorProvider(
            day = light.onSurfaceVariant,
            night = dark.onSurfaceVariant
        ),
        outline = ColorProvider(day = light.outline, night = dark.outline),
        inverseOnSurface = ColorProvider(
            day = light.inverseOnSurface,
            night = dark.inverseOnSurface
        ),
        inverseSurface = ColorProvider(day = light.inverseSurface, night = dark.inverseSurface),
        inversePrimary = ColorProvider(day = light.inversePrimary, night = dark.inversePrimary),
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
    )
}
