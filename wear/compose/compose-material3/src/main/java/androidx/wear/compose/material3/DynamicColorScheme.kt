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

package androidx.wear.compose.material3

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.annotation.ColorRes
import androidx.annotation.FloatRange
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Creates a dynamic color scheme.
 *
 * Use this function to create a color scheme based on the current watchface. If the user changes
 * the watchface colors, this color scheme will change accordingly. This function checks whether the
 * dynamic color scheme can be used and returns null otherwise. It is expected that callers will
 * check the return value and fallback to their own default color scheme if it is null.
 *
 * @param context The context required to get system resource data.
 */
public fun dynamicColorScheme(
    context: Context,
): ColorScheme? =
    if (!isDynamicColorSchemeEnabled(context)) {
        null
    } else {
        ColorScheme(
            primary = ResourceHelper.getColor(context, android.R.color.system_primary_fixed),
            primaryDim = ResourceHelper.getColor(context, android.R.color.system_primary_fixed_dim),
            primaryContainer =
                ResourceHelper.getColor(context, android.R.color.system_primary_container_dark),
            onPrimary = ResourceHelper.getColor(context, android.R.color.system_on_primary_fixed),
            onPrimaryContainer =
                ResourceHelper.getColor(context, android.R.color.system_on_primary_container_dark),
            secondary = ResourceHelper.getColor(context, android.R.color.system_secondary_fixed),
            secondaryDim =
                ResourceHelper.getColor(context, android.R.color.system_secondary_fixed_dim),
            secondaryContainer =
                ResourceHelper.getColor(context, android.R.color.system_secondary_container_dark),
            onSecondary =
                ResourceHelper.getColor(context, android.R.color.system_on_secondary_fixed),
            onSecondaryContainer =
                ResourceHelper.getColor(
                    context,
                    android.R.color.system_on_secondary_container_dark
                ),
            tertiary = ResourceHelper.getColor(context, android.R.color.system_tertiary_fixed),
            tertiaryDim =
                ResourceHelper.getColor(context, android.R.color.system_tertiary_fixed_dim),
            tertiaryContainer =
                ResourceHelper.getColor(context, android.R.color.system_tertiary_container_dark),
            onTertiary = ResourceHelper.getColor(context, android.R.color.system_on_tertiary_fixed),
            onTertiaryContainer =
                ResourceHelper.getColor(context, android.R.color.system_on_tertiary_container_dark),
            surfaceContainerLow =
                ResourceHelper.getColor(context, android.R.color.system_surface_container_low_dark),
            surfaceContainer =
                ResourceHelper.getColor(context, android.R.color.system_surface_container_dark),
            surfaceContainerHigh =
                ResourceHelper.getColor(
                    context,
                    android.R.color.system_surface_container_high_dark
                ),
            onSurface = ResourceHelper.getColor(context, android.R.color.system_on_surface_dark),
            onSurfaceVariant =
                ResourceHelper.getColor(context, android.R.color.system_on_surface_variant_dark),
            outline = ResourceHelper.getColor(context, android.R.color.system_outline_dark),
            outlineVariant =
                ResourceHelper.getColor(context, android.R.color.system_outline_variant_dark),
            background = ResourceHelper.getColor(context, android.R.color.system_background_dark),
            onBackground =
                ResourceHelper.getColor(context, android.R.color.system_on_background_dark),
            error = ResourceHelper.getColor(context, android.R.color.system_error_dark),
            errorContainer =
                ResourceHelper.getColor(context, android.R.color.system_error_container_dark),
            errorDim =
                ResourceHelper.getColor(context, android.R.color.system_error_container_dark)
                    .setLuminance(68f),
            onError = ResourceHelper.getColor(context, android.R.color.system_on_error_dark),
            onErrorContainer =
                ResourceHelper.getColor(context, android.R.color.system_on_error_container_dark),
        )
    }

/** Returns whether dynamic color is currently enabled on this device. */
private fun isDynamicColorSchemeEnabled(context: Context): Boolean =
    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) &&
        (Settings.Global.getInt(context.contentResolver, DYNAMIC_THEMING_SETTING_NAME, 0) == 1)

private object ResourceHelper {
    fun getColor(context: Context, @ColorRes id: Int): Color {
        return Color(context.resources.getColor(id, context.theme))
    }
}

/**
 * Set the luminance(tone) of this color. Chroma may decrease because chroma has a different maximum
 * for any given hue and luminance.
 *
 * @param newLuminance 0 <= newLuminance <= 100; invalid values are corrected.
 */
private fun Color.setLuminance(@FloatRange(from = 0.0, to = 100.0) newLuminance: Float): Color {
    if ((newLuminance < 0.0001) or (newLuminance > 99.9999)) {
        return Color(CamUtils.argbFromLstar(newLuminance.toDouble()))
    }

    val baseCam: Cam = Cam.fromInt(this.toArgb())
    val baseColor = Cam.getInt(baseCam.hue, baseCam.chroma, newLuminance)

    return Color(baseColor)
}

private const val DYNAMIC_THEMING_SETTING_NAME = "dynamic_color_theme_enabled"
