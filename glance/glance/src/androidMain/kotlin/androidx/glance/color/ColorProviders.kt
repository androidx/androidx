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

package androidx.glance.color

import androidx.annotation.RestrictTo
import androidx.glance.R
import androidx.glance.unit.ColorProvider

/**
 * Holds a set of Glance specific [ColorProvider] that can be used to represent a Material 3 color
 * scheme.
 */
class ColorProviders(
    val primary: ColorProvider,
    val onPrimary: ColorProvider,
    val primaryContainer: ColorProvider,
    val onPrimaryContainer: ColorProvider,
    val secondary: ColorProvider,
    val onSecondary: ColorProvider,
    val secondaryContainer: ColorProvider,
    val onSecondaryContainer: ColorProvider,
    val tertiary: ColorProvider,
    val onTertiary: ColorProvider,
    val tertiaryContainer: ColorProvider,
    val onTertiaryContainer: ColorProvider,
    val error: ColorProvider,
    val errorContainer: ColorProvider,
    val onError: ColorProvider,
    val onErrorContainer: ColorProvider,
    val background: ColorProvider,
    val onBackground: ColorProvider,
    val surface: ColorProvider,
    val onSurface: ColorProvider,
    val surfaceVariant: ColorProvider,
    val onSurfaceVariant: ColorProvider,
    val outline: ColorProvider,
    val inverseOnSurface: ColorProvider,
    val inverseSurface: ColorProvider,
    val inversePrimary: ColorProvider
    ) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ColorProviders

        if (primary != other.primary) return false
        if (onPrimary != other.onPrimary) return false
        if (primaryContainer != other.primaryContainer) return false
        if (onPrimaryContainer != other.onPrimaryContainer) return false
        if (secondary != other.secondary) return false
        if (onSecondary != other.onSecondary) return false
        if (secondaryContainer != other.secondaryContainer) return false
        if (onSecondaryContainer != other.onSecondaryContainer) return false
        if (tertiary != other.tertiary) return false
        if (onTertiary != other.onTertiary) return false
        if (tertiaryContainer != other.tertiaryContainer) return false
        if (onTertiaryContainer != other.onTertiaryContainer) return false
        if (error != other.error) return false
        if (errorContainer != other.errorContainer) return false
        if (onError != other.onError) return false
        if (onErrorContainer != other.onErrorContainer) return false
        if (background != other.background) return false
        if (onBackground != other.onBackground) return false
        if (surface != other.surface) return false
        if (onSurface != other.onSurface) return false
        if (surfaceVariant != other.surfaceVariant) return false
        if (onSurfaceVariant != other.onSurfaceVariant) return false
        if (outline != other.outline) return false
        if (inverseOnSurface != other.inverseOnSurface) return false
        if (inverseSurface != other.inverseSurface) return false
        if (inversePrimary != other.inversePrimary) return false

        return true
    }

    override fun hashCode(): Int {
        var result = primary.hashCode()
        result = 31 * result + onPrimary.hashCode()
        result = 31 * result + primaryContainer.hashCode()
        result = 31 * result + onPrimaryContainer.hashCode()
        result = 31 * result + secondary.hashCode()
        result = 31 * result + onSecondary.hashCode()
        result = 31 * result + secondaryContainer.hashCode()
        result = 31 * result + onSecondaryContainer.hashCode()
        result = 31 * result + tertiary.hashCode()
        result = 31 * result + onTertiary.hashCode()
        result = 31 * result + tertiaryContainer.hashCode()
        result = 31 * result + onTertiaryContainer.hashCode()
        result = 31 * result + error.hashCode()
        result = 31 * result + errorContainer.hashCode()
        result = 31 * result + onError.hashCode()
        result = 31 * result + onErrorContainer.hashCode()
        result = 31 * result + background.hashCode()
        result = 31 * result + onBackground.hashCode()
        result = 31 * result + surface.hashCode()
        result = 31 * result + onSurface.hashCode()
        result = 31 * result + surfaceVariant.hashCode()
        result = 31 * result + onSurfaceVariant.hashCode()
        result = 31 * result + outline.hashCode()
        result = 31 * result + inverseOnSurface.hashCode()
        result = 31 * result + inverseSurface.hashCode()
        result = 31 * result + inversePrimary.hashCode()
        return result
    }

    override fun toString(): String {
        return "ColorProviders(primary=$primary," +
            " onPrimary=$onPrimary, " +
            "primaryContainer=$primaryContainer, " +
            "onPrimaryContainer=$onPrimaryContainer, " +
            "secondary=$secondary, " +
            "onSecondary=$onSecondary, " +
            "secondaryContainer=$secondaryContainer, " +
            "onSecondaryContainer=$onSecondaryContainer, " +
            "tertiary=$tertiary, " +
            "onTertiary=$onTertiary, " +
            "tertiaryContainer=$tertiaryContainer, " +
            "onTertiaryContainer=$onTertiaryContainer, " +
            "error=$error, " +
            "errorContainer=$errorContainer, " +
            "onError=$onError, " +
            "onErrorContainer=$onErrorContainer, " +
            "background=$background, " +
            "onBackground=$onBackground, " +
            "surface=$surface, " +
            "onSurface=$onSurface, " +
            "surfaceVariant=$surfaceVariant, " +
            "onSurfaceVariant=$onSurfaceVariant, " +
            "outline=$outline, " +
            "inverseOnSurface=$inverseOnSurface, " +
            "inverseSurface=$inverseSurface, " +
            "inversePrimary=$inversePrimary)"
    }
}

/**
 * Creates a set of color providers that represents a Material 3 style dynamic color theme. On
 * devices that support it, this theme is derived from the user specific platform colors, on other
 * devices this falls back to the Material baseline theme.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun dynamicThemeColorProviders(): ColorProviders {
    return ColorProviders(
        primary = ColorProvider(R.color.glance_colorPrimary),
        onPrimary = ColorProvider(R.color.glance_colorOnPrimary),
        primaryContainer = ColorProvider(R.color.glance_colorPrimaryContainer),
        onPrimaryContainer = ColorProvider(R.color.glance_colorOnPrimaryContainer),
        secondary = ColorProvider(R.color.glance_colorSecondary),
        onSecondary = ColorProvider(R.color.glance_colorOnSecondary),
        secondaryContainer = ColorProvider(R.color.glance_colorSecondaryContainer),
        onSecondaryContainer = ColorProvider(R.color.glance_colorOnSecondaryContainer),
        tertiary = ColorProvider(R.color.glance_colorTertiary),
        onTertiary = ColorProvider(R.color.glance_colorOnTertiary),
        tertiaryContainer = ColorProvider(R.color.glance_colorTertiaryContainer),
        onTertiaryContainer = ColorProvider(R.color.glance_colorOnTertiaryContainer),
        error = ColorProvider(R.color.glance_colorError),
        errorContainer = ColorProvider(R.color.glance_colorErrorContainer),
        onError = ColorProvider(R.color.glance_colorOnError),
        onErrorContainer = ColorProvider(R.color.glance_colorOnErrorContainer),
        background = ColorProvider(R.color.glance_colorBackground),
        onBackground = ColorProvider(R.color.glance_colorOnBackground),
        surface = ColorProvider(R.color.glance_colorSurface),
        onSurface = ColorProvider(R.color.glance_colorOnSurface),
        surfaceVariant = ColorProvider(R.color.glance_colorSurfaceVariant),
        onSurfaceVariant = ColorProvider(R.color.glance_colorOnSurfaceVariant),
        outline = ColorProvider(R.color.glance_colorOutline),
        inverseOnSurface = ColorProvider(R.color.glance_colorOnSurfaceInverse),
        inverseSurface = ColorProvider(R.color.glance_colorSurfaceInverse),
        inversePrimary = ColorProvider(R.color.glance_colorPrimaryInverse),
    )
}
