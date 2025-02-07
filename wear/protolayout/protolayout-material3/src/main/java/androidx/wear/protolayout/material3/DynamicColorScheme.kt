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
package androidx.wear.protolayout.material3

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.annotation.ColorRes
import androidx.annotation.FloatRange
import androidx.annotation.VisibleForTesting
import androidx.wear.protolayout.types.LayoutColor
import androidx.wear.protolayout.types.argb

/**
 * Creates a dynamic color scheme.
 *
 * Use this function to create a color scheme based on the current system theme. If the user changes
 * the system theme, this color scheme will change accordingly. This function checks whether the
 * dynamic color scheme can be used and returns [defaultColorScheme] otherwise.
 *
 * @param context The context required to get system resource data.
 * @param defaultColorScheme The fallback [ColorScheme] to return if the dynamic color scheme is
 *   switched off or unavailable on this device.
 */
public fun dynamicColorScheme(
    context: Context,
    defaultColorScheme: ColorScheme = ColorScheme()
): ColorScheme {
    if (
        !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            isDynamicColorSchemeEnabled(context))
    ) {
        return defaultColorScheme
    }
    // From
    // https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/core/res/res/values/public-final.xml;l=3500;drc=2a8b6a18e0b7f696013ffede0cc0ab1904864d09
    // TODO: b/340192801 - Confirm once it's fully supported in system.
    return ColorScheme(
        primary = getLayoutColor(context, android.R.color.system_primary_fixed),
        primaryDim = getLayoutColor(context, android.R.color.system_primary_fixed_dim),
        primaryContainer = getLayoutColor(context, android.R.color.system_primary_container_dark),
        onPrimary = getLayoutColor(context, android.R.color.system_on_primary_fixed),
        onPrimaryContainer =
            getLayoutColor(context, android.R.color.system_on_primary_container_dark),
        secondary = getLayoutColor(context, android.R.color.system_secondary_fixed),
        secondaryDim = getLayoutColor(context, android.R.color.system_secondary_fixed_dim),
        secondaryContainer =
            getLayoutColor(context, android.R.color.system_secondary_container_dark),
        onSecondary = getLayoutColor(context, android.R.color.system_on_secondary_fixed),
        onSecondaryContainer =
            getLayoutColor(context, android.R.color.system_on_secondary_container_dark),
        tertiary = getLayoutColor(context, android.R.color.system_tertiary_fixed),
        tertiaryDim = getLayoutColor(context, android.R.color.system_tertiary_fixed_dim),
        tertiaryContainer = getLayoutColor(context, android.R.color.system_tertiary_container_dark),
        onTertiary = getLayoutColor(context, android.R.color.system_on_tertiary_fixed),
        onTertiaryContainer =
            getLayoutColor(context, android.R.color.system_on_tertiary_container_dark),
        surfaceContainerLow =
            getLayoutColor(context, android.R.color.system_surface_container_low_dark),
        surfaceContainer = getLayoutColor(context, android.R.color.system_surface_container_dark),
        surfaceContainerHigh =
            getLayoutColor(context, android.R.color.system_surface_container_high_dark),
        onSurface = getLayoutColor(context, android.R.color.system_on_surface_dark),
        onSurfaceVariant = getLayoutColor(context, android.R.color.system_on_surface_variant_dark),
        outline = getLayoutColor(context, android.R.color.system_outline_dark),
        outlineVariant = getLayoutColor(context, android.R.color.system_outline_variant_dark),
        background = getLayoutColor(context, android.R.color.system_background_dark),
        onBackground = getLayoutColor(context, android.R.color.system_on_background_dark),
        error = getLayoutColor(context, android.R.color.system_error_dark),
        errorContainer = getLayoutColor(context, android.R.color.system_error_container_dark),
        errorDim =
            context.resources
                .getColor(android.R.color.system_error_container_dark, context.theme)
                .setLuminance(68f)
                .argb,
        onError = getLayoutColor(context, android.R.color.system_on_error_dark),
        onErrorContainer = getLayoutColor(context, android.R.color.system_on_error_container_dark),
    )
}

/**
 * Returns whether the dynamic colors scheme (colors following the current system theme) is enabled.
 *
 * If enabled, and elements or [MaterialScope] are opted in to using dynamic theme, colors will
 * change whenever system theme changes.
 */
public fun isDynamicColorSchemeEnabled(context: Context): Boolean =
    // Guard this with API 35 check, as from that version, reading from the Setting is available.
    // Before API 35, reading from the Setting will throw an exception, like in b/379652439 or
    // b/372375270.
    // Dynamic theming is usually available from API 36.
    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) &&
        (Settings.Global.getInt(context.contentResolver, DYNAMIC_THEMING_SETTING_NAME, 0) == 1)

/** This maps to `android.provider.Settings.Global.Wearable.DYNAMIC_COLOR_THEME_ENABLED`. */
@VisibleForTesting
internal const val DYNAMIC_THEMING_SETTING_NAME: String = "dynamic_color_theme_enabled"

/** Retrieves the [LayoutColor] from the dynamic system theme with the given color token name. */
private fun getLayoutColor(context: Context, @ColorRes id: Int): LayoutColor =
    context.resources.getColor(id, context.theme).argb

/**
 * Forked from `androidx.compose.material3.DynamicTonalPaletteKt.setLuminance`.
 *
 * Set the luminance(tone) of this color. Chroma may decrease because chroma has a different maximum
 * for any given hue and luminance.
 *
 * Returns ARGB value.
 *
 * @param newLuminance 0 <= newLuminance <= 100; invalid values are corrected.
 */
@VisibleForTesting
internal fun Int.setLuminance(@FloatRange(from = 0.0, to = 100.0) newLuminance: Float): Int {
    if ((newLuminance < 0.0001) || (newLuminance > 99.9999)) {
        return CamUtils.argbFromLstar(newLuminance.toDouble())
    }

    val baseCam: Cam = Cam.fromInt(this)
    val baseColor = Cam.getInt(baseCam.hue, baseCam.chroma, newLuminance)

    return baseColor
}
