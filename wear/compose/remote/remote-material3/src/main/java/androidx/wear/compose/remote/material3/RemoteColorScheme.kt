/*
 * Copyright 2025 The Android Open Source Project
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

@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.wear.compose.remote.material3

import android.R
import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.remote.material3.internal.RemoteColorRole
import androidx.wear.compose.remote.material3.internal.RemoteColorSource
import androidx.wear.compose.remote.material3.internal.dynamicRemoteColorSource

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Immutable
public class RemoteColorScheme(
    public val primary: RemoteColor,
    public val primaryDim: RemoteColor,
    public val primaryContainer: RemoteColor,
    public val onPrimary: RemoteColor,
    public val onPrimaryContainer: RemoteColor,
    public val secondary: RemoteColor,
    public val secondaryDim: RemoteColor,
    public val secondaryContainer: RemoteColor,
    public val onSecondary: RemoteColor,
    public val onSecondaryContainer: RemoteColor,
    public val tertiary: RemoteColor,
    public val tertiaryDim: RemoteColor,
    public val tertiaryContainer: RemoteColor,
    public val onTertiary: RemoteColor,
    public val onTertiaryContainer: RemoteColor,
    public val surfaceContainerLow: RemoteColor,
    public val surfaceContainer: RemoteColor,
    public val surfaceContainerHigh: RemoteColor,
    public val onSurface: RemoteColor,
    public val onSurfaceVariant: RemoteColor,
    public val outline: RemoteColor,
    public val outlineVariant: RemoteColor,
    public val background: RemoteColor,
    public val onBackground: RemoteColor,
    public val error: RemoteColor,
    public val errorDim: RemoteColor,
    public val errorContainer: RemoteColor,
    public val onError: RemoteColor,
    public val onErrorContainer: RemoteColor,
) {

    public constructor(
        colorScheme: ColorScheme = ColorScheme()
    ) : this(colorScheme = colorScheme, source = RemoteColorSource.Named)

    /**
     * Creates a [RemoteColorScheme] from a standard [ColorScheme], resolving each color role from
     * [source].
     *
     * @param colorScheme The local [ColorScheme] to retrieve fallback/default colors from.
     * @param source Whether colors are named colors or follow the Android system colors.
     */
    internal constructor(
        colorScheme: ColorScheme,
        source: RemoteColorSource,
    ) : this(
        primary = source.resolve(WearColorRoles.Primary, colorScheme.primary),
        primaryDim = source.resolve(WearColorRoles.PrimaryDim, colorScheme.primaryDim),
        primaryContainer =
            source.resolve(WearColorRoles.PrimaryContainer, colorScheme.primaryContainer),
        onPrimary = source.resolve(WearColorRoles.OnPrimary, colorScheme.onPrimary),
        onPrimaryContainer =
            source.resolve(WearColorRoles.OnPrimaryContainer, colorScheme.onPrimaryContainer),
        secondary = source.resolve(WearColorRoles.Secondary, colorScheme.secondary),
        secondaryDim = source.resolve(WearColorRoles.SecondaryDim, colorScheme.secondaryDim),
        secondaryContainer =
            source.resolve(WearColorRoles.SecondaryContainer, colorScheme.secondaryContainer),
        onSecondary = source.resolve(WearColorRoles.OnSecondary, colorScheme.onSecondary),
        onSecondaryContainer =
            source.resolve(WearColorRoles.OnSecondaryContainer, colorScheme.onSecondaryContainer),
        tertiary = source.resolve(WearColorRoles.Tertiary, colorScheme.tertiary),
        tertiaryDim = source.resolve(WearColorRoles.TertiaryDim, colorScheme.tertiaryDim),
        tertiaryContainer =
            source.resolve(WearColorRoles.TertiaryContainer, colorScheme.tertiaryContainer),
        onTertiary = source.resolve(WearColorRoles.OnTertiary, colorScheme.onTertiary),
        onTertiaryContainer =
            source.resolve(WearColorRoles.OnTertiaryContainer, colorScheme.onTertiaryContainer),
        surfaceContainerLow =
            source.resolve(WearColorRoles.SurfaceContainerLow, colorScheme.surfaceContainerLow),
        surfaceContainer =
            source.resolve(WearColorRoles.SurfaceContainer, colorScheme.surfaceContainer),
        surfaceContainerHigh =
            source.resolve(WearColorRoles.SurfaceContainerHigh, colorScheme.surfaceContainerHigh),
        onSurface = source.resolve(WearColorRoles.OnSurface, colorScheme.onSurface),
        onSurfaceVariant =
            source.resolve(WearColorRoles.OnSurfaceVariant, colorScheme.onSurfaceVariant),
        outline = source.resolve(WearColorRoles.Outline, colorScheme.outline),
        outlineVariant = source.resolve(WearColorRoles.OutlineVariant, colorScheme.outlineVariant),
        background = source.resolve(WearColorRoles.Background, colorScheme.background),
        onBackground = source.resolve(WearColorRoles.OnBackground, colorScheme.onBackground),
        error = source.resolve(WearColorRoles.Error, colorScheme.error),
        errorDim = source.resolve(WearColorRoles.ErrorDim, colorScheme.errorDim),
        errorContainer = source.resolve(WearColorRoles.ErrorContainer, colorScheme.errorContainer),
        onError = source.resolve(WearColorRoles.OnError, colorScheme.onError),
        onErrorContainer =
            source.resolve(WearColorRoles.OnErrorContainer, colorScheme.onErrorContainer),
    )

    public fun copy(
        primary: RemoteColor = this.primary,
        primaryDim: RemoteColor = this.primaryDim,
        primaryContainer: RemoteColor = this.primaryContainer,
        onPrimary: RemoteColor = this.onPrimary,
        onPrimaryContainer: RemoteColor = this.onPrimaryContainer,
        secondary: RemoteColor = this.secondary,
        secondaryDim: RemoteColor = this.secondaryDim,
        secondaryContainer: RemoteColor = this.secondaryContainer,
        onSecondary: RemoteColor = this.onSecondary,
        onSecondaryContainer: RemoteColor = this.onSecondaryContainer,
        tertiary: RemoteColor = this.tertiary,
        tertiaryDim: RemoteColor = this.tertiaryDim,
        tertiaryContainer: RemoteColor = this.tertiaryContainer,
        onTertiary: RemoteColor = this.onTertiary,
        onTertiaryContainer: RemoteColor = this.onTertiaryContainer,
        surfaceContainerLow: RemoteColor = this.surfaceContainerLow,
        surfaceContainer: RemoteColor = this.surfaceContainer,
        surfaceContainerHigh: RemoteColor = this.surfaceContainerHigh,
        onSurface: RemoteColor = this.onSurface,
        onSurfaceVariant: RemoteColor = this.onSurfaceVariant,
        outline: RemoteColor = this.outline,
        outlineVariant: RemoteColor = this.outlineVariant,
        background: RemoteColor = this.background,
        onBackground: RemoteColor = this.onBackground,
        error: RemoteColor = this.error,
        errorDim: RemoteColor = this.errorDim,
        errorContainer: RemoteColor = this.errorContainer,
        onError: RemoteColor = this.onError,
        onErrorContainer: RemoteColor = this.onErrorContainer,
    ): RemoteColorScheme =
        RemoteColorScheme(
            primary = primary,
            primaryDim = primaryDim,
            primaryContainer = primaryContainer,
            onPrimary = onPrimary,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            secondaryDim = secondaryDim,
            secondaryContainer = secondaryContainer,
            onSecondary = onSecondary,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            tertiaryDim = tertiaryDim,
            tertiaryContainer = tertiaryContainer,
            onTertiary = onTertiary,
            onTertiaryContainer = onTertiaryContainer,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            onSurface = onSurface,
            onSurfaceVariant = onSurfaceVariant,
            outline = outline,
            outlineVariant = outlineVariant,
            background = background,
            onBackground = onBackground,
            error = error,
            errorDim = errorDim,
            errorContainer = errorContainer,
            onError = onError,
            onErrorContainer = onErrorContainer,
        )
}

/**
 * The Wear Material 3 color roles, with their `"WearM3.*"` named color and the Android system color
 * token used for dynamic colors. Wear is always dark, so each role uses its dark token.
 */
private object WearColorRoles {
    val Primary = RemoteColorRole("WearM3.primary", R.color.system_primary_dark)
    // Gap: Android framework lacks a dynamic system_primary_dim_dark token;
    // system_primary_fixed_dim is the only primary dim token available.
    val PrimaryDim = RemoteColorRole("WearM3.primaryDim", R.color.system_primary_fixed_dim)
    val PrimaryContainer =
        RemoteColorRole("WearM3.primaryContainer", R.color.system_primary_container_dark)
    val OnPrimary = RemoteColorRole("WearM3.onPrimary", R.color.system_on_primary_dark)
    val OnPrimaryContainer =
        RemoteColorRole("WearM3.onPrimaryContainer", R.color.system_on_primary_container_dark)
    val Secondary = RemoteColorRole("WearM3.secondary", R.color.system_secondary_dark)
    // Gap: Android framework lacks a dynamic system_secondary_dim_dark token;
    // system_secondary_fixed_dim is the only secondary dim token available.
    val SecondaryDim = RemoteColorRole("WearM3.secondaryDim", R.color.system_secondary_fixed_dim)
    val SecondaryContainer =
        RemoteColorRole("WearM3.secondaryContainer", R.color.system_secondary_container_dark)
    val OnSecondary = RemoteColorRole("WearM3.onSecondary", R.color.system_on_secondary_dark)
    val OnSecondaryContainer =
        RemoteColorRole("WearM3.onSecondaryContainer", R.color.system_on_secondary_container_dark)
    val Tertiary = RemoteColorRole("WearM3.tertiary", R.color.system_tertiary_dark)
    // Gap: Android framework lacks a dynamic system_tertiary_dim_dark token;
    // system_tertiary_fixed_dim is the only tertiary dim token available.
    val TertiaryDim = RemoteColorRole("WearM3.tertiaryDim", R.color.system_tertiary_fixed_dim)
    val TertiaryContainer =
        RemoteColorRole("WearM3.tertiaryContainer", R.color.system_tertiary_container_dark)
    val OnTertiary = RemoteColorRole("WearM3.onTertiary", R.color.system_on_tertiary_dark)
    val OnTertiaryContainer =
        RemoteColorRole("WearM3.onTertiaryContainer", R.color.system_on_tertiary_container_dark)
    val SurfaceContainerLow =
        RemoteColorRole("WearM3.surfaceContainerLow", R.color.system_surface_container_low_dark)
    val SurfaceContainer =
        RemoteColorRole("WearM3.surfaceContainer", R.color.system_surface_container_dark)
    val SurfaceContainerHigh =
        RemoteColorRole("WearM3.surfaceContainerHigh", R.color.system_surface_container_high_dark)
    val OnSurface = RemoteColorRole("WearM3.onSurface", R.color.system_on_surface_dark)
    val OnSurfaceVariant =
        RemoteColorRole("WearM3.onSurfaceVariant", R.color.system_on_surface_variant_dark)
    val Outline = RemoteColorRole("WearM3.outline", R.color.system_outline_dark)
    val OutlineVariant =
        RemoteColorRole("WearM3.outlineVariant", R.color.system_outline_variant_dark)
    val Background = RemoteColorRole("WearM3.background", R.color.system_background_dark)
    val OnBackground = RemoteColorRole("WearM3.onBackground", R.color.system_on_background_dark)
    val Error = RemoteColorRole("WearM3.error", R.color.system_error_dark)
    // Gap: Android framework has NO system_error_fixed_dim or system_error_dim semantic token.
    // Uses tonal palette system_error_300 (tone 70, approximating Wear M3's tone 68 errorDim).
    val ErrorDim = RemoteColorRole("WearM3.errorDim", R.color.system_error_300)
    val ErrorContainer =
        RemoteColorRole("WearM3.errorContainer", R.color.system_error_container_dark)
    val OnError = RemoteColorRole("WearM3.onError", R.color.system_on_error_dark)
    val OnErrorContainer =
        RemoteColorRole("WearM3.onErrorContainer", R.color.system_on_error_container_dark)
}

@Composable
@RemoteComposable
public fun dynamicRemoteColorScheme(
    fallbackColorScheme: ColorScheme = ColorScheme()
): RemoteColorScheme =
    RemoteColorScheme(colorScheme = fallbackColorScheme, source = dynamicRemoteColorSource())

internal const val DisabledContentAlpha = 0.38f
