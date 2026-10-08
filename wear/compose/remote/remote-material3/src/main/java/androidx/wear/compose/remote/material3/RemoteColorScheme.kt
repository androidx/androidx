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

package androidx.wear.compose.remote.material3

import android.R
import androidx.annotation.ColorRes
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.Operations
import androidx.compose.remote.creation.compose.capture.LocalRemoteComposeCreationState
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteColor.Companion.createNamedRemoteColor
import androidx.compose.remote.creation.compose.state.createThemedRemoteColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme

/**
 * Represents the color scheme for the Wear Material 3 theme in a remote context.
 *
 * A [RemoteColorScheme] holds [RemoteColor] parameters for a [RemoteMaterialTheme], mapping to the
 * standard Material Design 3 color roles for Wearables.
 *
 * Color schemes are designed to be harmonious, ensure accessible text, and distinguish UI elements
 * and surfaces from one another.
 *
 * To learn more about color schemes, see
 * [Material Design Color System](https://m3.material.io/styles/color/the-color-system/color-roles).
 *
 * @property primary The primary color is the color displayed most frequently across your app’s
 *   screens and components.
 * @property primaryDim is less prominent than [primary] for component backgrounds
 * @property primaryContainer is a standout container color for key components.
 * @property onPrimary Color used for text and icons displayed on top of the primary color.
 * @property onPrimaryContainer The color (and state variants) that should be used for content on
 *   top of [primaryContainer].
 * @property secondary The secondary color provides more ways to accent and distinguish your
 *   product.
 * @property secondaryDim is less prominent than [secondary] for component backgrounds.
 * @property secondaryContainer A tonal color to be used in containers.
 * @property onSecondary Color used for text and icons displayed on top of the secondary color.
 * @property onSecondaryContainer The color (and state variants) that should be used for content on
 *   top of [secondaryContainer].
 * @property tertiary The tertiary color that can be used to balance primary and secondary colors,
 *   or bring heightened attention to an element.
 * @property tertiaryDim A less prominent tertiary color that can be used to balance primary and
 *   secondary colors, or bring heightened attention to an element.
 * @property tertiaryContainer A tonal color to be used in containers.
 * @property onTertiary Color used for text and icons displayed on top of the tertiary color.
 * @property onTertiaryContainer The color (and state variants) that should be used for content on
 *   top of [tertiaryContainer].
 * @property surfaceContainerLow A surface color used for large containment components such as Card
 *   and Button with low prominence.
 * @property surfaceContainer The main surface color that affect surfaces of components with large
 *   containment areas, such as Card and Button.
 * @property surfaceContainerHigh A surface color used for large containment components such Card
 *   and Button with high prominence.
 * @property onSurface Color used for text and icons displayed on top of the surface color.
 * @property onSurfaceVariant The color for secondary text and icons on top of [surfaceContainer].
 * @property outline The main color for primary outline components. The outline color role adds
 *   contrast for accessibility purposes.
 * @property outlineVariant The secondary color for secondary outline components.
 * @property background The background color that appears behind other content.
 * @property onBackground Color used for text and icons displayed on top of the background color.
 * @property error Color that indicates remove, delete, close or dismiss actions, such as Swipe to
 *   Reveal. Added as a slightly less alarming and urgent alternative to errorContainer than the
 *   error dim color.
 * @property errorDim Indicates high priority errors or emergency actions, such as safety alerts,
 *   failed dialog overlays or stop buttons.
 * @property errorContainer A less prominent container color than [error], for components using the
 *   error state. Can also indicate an active error state which feels less interactive than a filled
 *   state, such as an active emergency sharing button, or on a failed overlay dialog..
 * @property onError Color used for text and icons displayed on top of the error color.
 * @property onErrorContainer Color used for text and icons on the errorContainer color.
 */
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

    /**
     * Creates a [RemoteColorScheme] from a standard [ColorScheme].
     *
     * Colors are resolved using [RemoteColor.createNamedRemoteColor] with predefined canonical
     * names (e.g., `"WearM3.primary"`).
     *
     * @param colorScheme The local [ColorScheme] to retrieve colors from.
     */
    public constructor(
        colorScheme: ColorScheme = ColorScheme()
    ) : this(colorScheme = colorScheme, useThemedColors = false)

    /**
     * Creates a [RemoteColorScheme] from a standard [ColorScheme], explicitly specifying whether to
     * use themed colors or named colors.
     *
     * @param colorScheme The local [ColorScheme] to retrieve fallback/default colors from.
     * @param useThemedColors When true, resolves colors using [RemoteColor.createThemedRemoteColor]
     *   with Android system color tokens; when false, uses [RemoteColor.createNamedRemoteColor].
     */
    internal constructor(
        colorScheme: ColorScheme,
        useThemedColors: Boolean,
    ) : this(
        primary =
            remoteSchemeColor(
                useThemedColors = useThemedColors,
                name = PRIMARY,
                colorRes = R.color.system_primary_dark,
                defaultValue = colorScheme.primary,
            ),
        // Gap: Android framework lacks a dynamic system_primary_dim_dark token;
        // system_primary_fixed_dim is the only primary dim token available.
        primaryDim =
            remoteSchemeColor(
                useThemedColors = useThemedColors,
                name = PRIMARY_DIM,
                colorRes = R.color.system_primary_fixed_dim,
                defaultValue = colorScheme.primaryDim,
            ),
        primaryContainer =
            remoteSchemeColor(
                useThemedColors = useThemedColors,
                name = PRIMARY_CONTAINER,
                colorRes = R.color.system_primary_container_dark,
                defaultValue = colorScheme.primaryContainer,
            ),
        onPrimary =
            remoteSchemeColor(
                useThemedColors = useThemedColors,
                name = ON_PRIMARY,
                colorRes = R.color.system_on_primary_dark,
                defaultValue = colorScheme.onPrimary,
            ),
        onPrimaryContainer =
            remoteSchemeColor(
                useThemedColors = useThemedColors,
                name = ON_PRIMARY_CONTAINER,
                colorRes = R.color.system_on_primary_container_dark,
                defaultValue = colorScheme.onPrimaryContainer,
            ),
        secondary =
            remoteSchemeColor(
                useThemedColors = useThemedColors,
                name = SECONDARY,
                colorRes = R.color.system_secondary_dark,
                defaultValue = colorScheme.secondary,
            ),
        // Gap: Android framework lacks a dynamic system_secondary_dim_dark token;
        // system_secondary_fixed_dim is the only secondary dim token available.
        secondaryDim =
            remoteSchemeColor(
                useThemedColors = useThemedColors,
                name = SECONDARY_DIM,
                colorRes = R.color.system_secondary_fixed_dim,
                defaultValue = colorScheme.secondaryDim,
            ),
        secondaryContainer =
            remoteSchemeColor(
                useThemedColors = useThemedColors,
                name = SECONDARY_CONTAINER,
                colorRes = R.color.system_secondary_container_dark,
                defaultValue = colorScheme.secondaryContainer,
            ),
        onSecondary =
            remoteSchemeColor(
                useThemedColors = useThemedColors,
                name = ON_SECONDARY,
                colorRes = R.color.system_on_secondary_dark,
                defaultValue = colorScheme.onSecondary,
            ),
        onSecondaryContainer =
            remoteSchemeColor(
                useThemedColors = useThemedColors,
                name = ON_SECONDARY_CONTAINER,
                colorRes = R.color.system_on_secondary_container_dark,
                defaultValue = colorScheme.onSecondaryContainer,
            ),
        tertiary =
            remoteSchemeColor(
                useThemedColors = useThemedColors,
                name = TERTIARY,
                colorRes = R.color.system_tertiary_dark,
                defaultValue = colorScheme.tertiary,
            ),
        // Gap: Android framework lacks a dynamic system_tertiary_dim_dark token;
        // system_tertiary_fixed_dim is the only tertiary dim token available.
        tertiaryDim =
            remoteSchemeColor(
                useThemedColors = useThemedColors,
                name = TERTIARY_DIM,
                colorRes = R.color.system_tertiary_fixed_dim,
                defaultValue = colorScheme.tertiaryDim,
            ),
        tertiaryContainer =
            remoteSchemeColor(
                useThemedColors = useThemedColors,
                name = TERTIARY_CONTAINER,
                colorRes = R.color.system_tertiary_container_dark,
                defaultValue = colorScheme.tertiaryContainer,
            ),
        onTertiary =
            remoteSchemeColor(
                useThemedColors = useThemedColors,
                name = ON_TERTIARY,
                colorRes = R.color.system_on_tertiary_dark,
                defaultValue = colorScheme.onTertiary,
            ),
        onTertiaryContainer =
            remoteSchemeColor(
                useThemedColors = useThemedColors,
                name = ON_TERTIARY_CONTAINER,
                colorRes = R.color.system_on_tertiary_container_dark,
                defaultValue = colorScheme.onTertiaryContainer,
            ),
        surfaceContainerLow =
            remoteSchemeColor(
                useThemedColors = useThemedColors,
                name = SURFACE_CONTAINER_LOW,
                colorRes = R.color.system_surface_container_low_dark,
                defaultValue = colorScheme.surfaceContainerLow,
            ),
        surfaceContainer =
            remoteSchemeColor(
                useThemedColors = useThemedColors,
                name = SURFACE_CONTAINER,
                colorRes = R.color.system_surface_container_dark,
                defaultValue = colorScheme.surfaceContainer,
            ),
        surfaceContainerHigh =
            remoteSchemeColor(
                useThemedColors = useThemedColors,
                name = SURFACE_CONTAINER_HIGH,
                colorRes = R.color.system_surface_container_high_dark,
                defaultValue = colorScheme.surfaceContainerHigh,
            ),
        onSurface =
            remoteSchemeColor(
                useThemedColors = useThemedColors,
                name = ON_SURFACE,
                colorRes = R.color.system_on_surface_dark,
                defaultValue = colorScheme.onSurface,
            ),
        onSurfaceVariant =
            remoteSchemeColor(
                useThemedColors = useThemedColors,
                name = ON_SURFACE_VARIANT,
                colorRes = R.color.system_on_surface_variant_dark,
                defaultValue = colorScheme.onSurfaceVariant,
            ),
        outline =
            remoteSchemeColor(
                useThemedColors = useThemedColors,
                name = OUTLINE,
                colorRes = R.color.system_outline_dark,
                defaultValue = colorScheme.outline,
            ),
        outlineVariant =
            remoteSchemeColor(
                useThemedColors = useThemedColors,
                name = OUTLINE_VARIANT,
                colorRes = R.color.system_outline_variant_dark,
                defaultValue = colorScheme.outlineVariant,
            ),
        background =
            remoteSchemeColor(
                useThemedColors = useThemedColors,
                name = BACKGROUND,
                colorRes = R.color.system_background_dark,
                defaultValue = colorScheme.background,
            ),
        onBackground =
            remoteSchemeColor(
                useThemedColors = useThemedColors,
                name = ON_BACKGROUND,
                colorRes = R.color.system_on_background_dark,
                defaultValue = colorScheme.onBackground,
            ),
        error =
            remoteSchemeColor(
                useThemedColors = useThemedColors,
                name = ERROR,
                colorRes = R.color.system_error_dark,
                defaultValue = colorScheme.error,
            ),
        // Gap: Android framework has NO system_error_fixed_dim or system_error_dim semantic token.
        // Uses tonal palette system_error_300 (tone 70, approximating Wear M3's tone 68 errorDim).
        errorDim =
            remoteSchemeColor(
                useThemedColors = useThemedColors,
                name = ERROR_DIM,
                colorRes = R.color.system_error_300,
                defaultValue = colorScheme.errorDim,
            ),
        errorContainer =
            remoteSchemeColor(
                useThemedColors = useThemedColors,
                name = ERROR_CONTAINER,
                colorRes = R.color.system_error_container_dark,
                defaultValue = colorScheme.errorContainer,
            ),
        onError =
            remoteSchemeColor(
                useThemedColors = useThemedColors,
                name = ON_ERROR,
                colorRes = R.color.system_on_error_dark,
                defaultValue = colorScheme.onError,
            ),
        onErrorContainer =
            remoteSchemeColor(
                useThemedColors = useThemedColors,
                name = ON_ERROR_CONTAINER,
                colorRes = R.color.system_on_error_container_dark,
                defaultValue = colorScheme.onErrorContainer,
            ),
    )

    /** Returns a copy of this RemoteColorScheme, optionally overriding some of the values. */
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

    private companion object {
        private const val PRIMARY = "WearM3.primary"
        private const val PRIMARY_DIM = "WearM3.primaryDim"
        private const val PRIMARY_CONTAINER = "WearM3.primaryContainer"
        private const val ON_PRIMARY = "WearM3.onPrimary"
        private const val ON_PRIMARY_CONTAINER = "WearM3.onPrimaryContainer"
        private const val SECONDARY = "WearM3.secondary"
        private const val SECONDARY_DIM = "WearM3.secondaryDim"
        private const val SECONDARY_CONTAINER = "WearM3.secondaryContainer"
        private const val ON_SECONDARY = "WearM3.onSecondary"
        private const val ON_SECONDARY_CONTAINER = "WearM3.onSecondaryContainer"
        private const val TERTIARY = "WearM3.tertiary"
        private const val TERTIARY_DIM = "WearM3.tertiaryDim"
        private const val TERTIARY_CONTAINER = "WearM3.tertiaryContainer"
        private const val ON_TERTIARY = "WearM3.onTertiary"
        private const val ON_TERTIARY_CONTAINER = "WearM3.onTertiaryContainer"
        private const val SURFACE_CONTAINER_LOW = "WearM3.surfaceContainerLow"
        private const val SURFACE_CONTAINER = "WearM3.surfaceContainer"
        private const val SURFACE_CONTAINER_HIGH = "WearM3.surfaceContainerHigh"
        private const val ON_SURFACE = "WearM3.onSurface"
        private const val ON_SURFACE_VARIANT = "WearM3.onSurfaceVariant"
        private const val OUTLINE = "WearM3.outline"
        private const val OUTLINE_VARIANT = "WearM3.outlineVariant"
        private const val BACKGROUND = "WearM3.background"
        private const val ON_BACKGROUND = "WearM3.onBackground"
        private const val ERROR = "WearM3.error"
        private const val ERROR_DIM = "WearM3.errorDim"
        private const val ERROR_CONTAINER = "WearM3.errorContainer"
        private const val ON_ERROR = "WearM3.onError"
        private const val ON_ERROR_CONTAINER = "WearM3.onErrorContainer"

        private fun remoteSchemeColor(
            useThemedColors: Boolean,
            name: String,
            @ColorRes colorRes: Int,
            defaultValue: Color,
        ): RemoteColor =
            if (useThemedColors) {
                // Wear is always dark, so the dark system token is used for both theme modes.
                RemoteColor.createThemedRemoteColor(
                    lightMode = colorRes,
                    darkMode = colorRes,
                    lightFallback = defaultValue,
                    darkFallback = defaultValue,
                )
            } else {
                createNamedRemoteColor(name, defaultValue)
            }
    }
}

/**
 * Creates a [RemoteColorScheme] whose colors follow the player's Android system (dynamic) colors.
 *
 * Each color role is mapped to the corresponding Android system color token (e.g.
 * `android.R.color.system_primary_dark`), so the player resolves it against the device's dynamic
 * colors. Wear is always dark, so only the dark system tokens are used.
 *
 * If the current document profile does not support themed colors (e.g. Wear widgets), the named
 * `"WearM3.*"` color scheme is returned instead.
 *
 * Use with [RemoteMaterialTheme] to opt in, e.g. `RemoteMaterialTheme(colorScheme =
 * dynamicRemoteColorScheme()) { ... }`.
 *
 * @param fallbackColorScheme The [ColorScheme] used for fallback values when a system color cannot
 *   be resolved, and for the named colors on profiles without themed color support.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
@RemoteComposable
public fun dynamicRemoteColorScheme(
    fallbackColorScheme: ColorScheme = ColorScheme()
): RemoteColorScheme =
    RemoteColorScheme(colorScheme = fallbackColorScheme, useThemedColors = supportsThemedColors())

/** Whether the current document profile supports themed (system) colors. */
@Suppress("RestrictedApiAndroidX", "PrimitiveInCollection") // LocalRemoteComposeCreationState
@Composable
@RemoteComposable
private fun supportsThemedColors(): Boolean =
    Operations.COLOR_THEME in LocalRemoteComposeCreationState.current.profile.supportedOperations

internal const val DisabledContentAlpha = 0.38f
