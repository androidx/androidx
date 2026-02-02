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

package androidx.glance.material

import androidx.compose.material.Colors
import androidx.compose.material.primarySurface
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceTheme
import androidx.glance.color.ColorProvider
import androidx.glance.color.ColorProviders
import androidx.glance.color.colorProviders
import androidx.glance.unit.ColorProvider

/** Given Material [Colors], creates a [ColorProviders] that can be passed to [GlanceTheme] */
fun ColorProviders(light: Colors, dark: Colors): ColorProviders {

    val background = ColorProvider(light.background, dark.background)
    val onBackground = ColorProvider(light.onBackground, dark.onBackground)
    val primary = ColorProvider(light.primary, dark.primary)
    val onPrimary = ColorProvider(light.onPrimary, dark.onPrimary)
    val surface = ColorProvider(light.primarySurface, dark.primarySurface)
    val onSurface = ColorProvider(light.onSurface, dark.onSurface)
    val secondary = ColorProvider(light.secondary, dark.secondary)
    val onSecondary = ColorProvider(light.onSecondary, dark.onSecondary)
    val error = ColorProvider(light.error, dark.error)
    val onError = ColorProvider(light.onError, dark.onError)

    return colorProviders(
        primary = primary,
        onPrimary = onPrimary,
        surface = surface,
        onSurface = onSurface,
        secondary = secondary,
        onSecondary = onSecondary,
        error = error,
        onError = onError,
        background = background,
        onBackground = onBackground,
        primaryContainer = ColorProvider(ColorNotDefined, ColorNotDefined),
        onPrimaryContainer = ColorProvider(ColorNotDefined, ColorNotDefined),
        secondaryContainer = ColorProvider(ColorNotDefined, ColorNotDefined),
        onSecondaryContainer = ColorProvider(ColorNotDefined, ColorNotDefined),
        tertiary = ColorProvider(ColorNotDefined, ColorNotDefined),
        onTertiary = ColorProvider(ColorNotDefined, ColorNotDefined),
        tertiaryContainer = ColorProvider(ColorNotDefined, ColorNotDefined),
        onTertiaryContainer = ColorProvider(ColorNotDefined, ColorNotDefined),
        errorContainer = ColorProvider(ColorNotDefined, ColorNotDefined),
        onErrorContainer = ColorProvider(ColorNotDefined, ColorNotDefined),
        surfaceVariant = ColorProvider(ColorNotDefined, ColorNotDefined),
        onSurfaceVariant = ColorProvider(ColorNotDefined, ColorNotDefined),
        outline = ColorProvider(ColorNotDefined, ColorNotDefined),
        inverseOnSurface = ColorProvider(ColorNotDefined, ColorNotDefined),
        inverseSurface = ColorProvider(ColorNotDefined, ColorNotDefined),
        inversePrimary = ColorProvider(ColorNotDefined, ColorNotDefined),
        widgetBackground = background,
    )
}

/** Given Material [Colors], creates a [ColorProviders] that can be passed to [GlanceTheme] */
fun ColorProviders(colors: Colors): ColorProviders {

    val background = ColorProvider(colors.background)
    val onBackground = ColorProvider(colors.onBackground)
    val primary = ColorProvider(colors.primary)
    val onPrimary = ColorProvider(colors.onPrimary)
    val surface = ColorProvider(colors.primarySurface)
    val onSurface = ColorProvider(colors.onSurface)
    val secondary = ColorProvider(colors.secondary)
    val onSecondary = ColorProvider(colors.onSecondary)
    val error = ColorProvider(colors.error)
    val onError = ColorProvider(colors.onError)

    return colorProviders(
        primary = primary,
        onPrimary = onPrimary,
        surface = surface,
        onSurface = onSurface,
        secondary = secondary,
        onSecondary = onSecondary,
        error = error,
        onError = onError,
        background = background,
        onBackground = onBackground,
        primaryContainer = ColorProvider(ColorNotDefined, ColorNotDefined),
        onPrimaryContainer = ColorProvider(ColorNotDefined, ColorNotDefined),
        secondaryContainer = ColorProvider(ColorNotDefined, ColorNotDefined),
        onSecondaryContainer = ColorProvider(ColorNotDefined, ColorNotDefined),
        tertiary = ColorProvider(ColorNotDefined, ColorNotDefined),
        onTertiary = ColorProvider(ColorNotDefined, ColorNotDefined),
        tertiaryContainer = ColorProvider(ColorNotDefined, ColorNotDefined),
        onTertiaryContainer = ColorProvider(ColorNotDefined, ColorNotDefined),
        errorContainer = ColorProvider(ColorNotDefined, ColorNotDefined),
        onErrorContainer = ColorProvider(ColorNotDefined, ColorNotDefined),
        surfaceVariant = ColorProvider(ColorNotDefined, ColorNotDefined),
        onSurfaceVariant = ColorProvider(ColorNotDefined, ColorNotDefined),
        outline = ColorProvider(ColorNotDefined, ColorNotDefined),
        inverseOnSurface = ColorProvider(ColorNotDefined, ColorNotDefined),
        inverseSurface = ColorProvider(ColorNotDefined, ColorNotDefined),
        inversePrimary = ColorProvider(ColorNotDefined, ColorNotDefined),
        widgetBackground = background,
    )
}

private val ColorNotDefined = Color.Magenta
