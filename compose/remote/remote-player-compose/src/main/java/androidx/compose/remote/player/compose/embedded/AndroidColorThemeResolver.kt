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

package androidx.compose.remote.player.compose.embedded

import android.R
import android.content.Context
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.operations.ColorTheme

/**
 * Resolves Android system color resources for [ColorTheme] operations.
 *
 * In remote-core documents, Android theme colors are stored as indexed tokens referencing framework
 * color resources. This resolver maps the 196 standard [android.R.color] resources into the
 * document's [ColorTheme] operations to match the View player's theme resolution.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object AndroidColorThemeResolver {
    // TODO(b/533417030): Read this table from remote-player-view (ThemeSupport.AndroidColors) or
    //  share via remote-player-core instead of duplicating.
    /** 1:1 index mapping corresponding to `ThemeSupport.AndroidColors.mId`. */
    internal val colorResourceIds: IntArray =
        intArrayOf(
            R.color.background_dark,
            R.color.background_light,
            R.color.black,
            R.color.darker_gray,
            R.color.holo_blue_bright,
            R.color.holo_blue_dark,
            R.color.holo_blue_light,
            R.color.holo_green_dark,
            R.color.holo_green_light,
            R.color.holo_orange_dark,
            R.color.holo_orange_light,
            R.color.holo_purple,
            R.color.holo_red_dark,
            R.color.holo_red_light,
            R.color.system_accent1_0,
            R.color.system_accent1_10,
            R.color.system_accent1_100,
            R.color.system_accent1_1000,
            R.color.system_accent1_200,
            R.color.system_accent1_300,
            R.color.system_accent1_400,
            R.color.system_accent1_50,
            R.color.system_accent1_500,
            R.color.system_accent1_600,
            R.color.system_accent1_700,
            R.color.system_accent1_800,
            R.color.system_accent1_900,
            R.color.system_accent2_0,
            R.color.system_accent2_10,
            R.color.system_accent2_100,
            R.color.system_accent2_1000,
            R.color.system_accent2_200,
            R.color.system_accent2_300,
            R.color.system_accent2_400,
            R.color.system_accent2_50,
            R.color.system_accent2_500,
            R.color.system_accent2_600,
            R.color.system_accent2_700,
            R.color.system_accent2_800,
            R.color.system_accent2_900,
            R.color.system_accent3_0,
            R.color.system_accent3_10,
            R.color.system_accent3_100,
            R.color.system_accent3_1000,
            R.color.system_accent3_200,
            R.color.system_accent3_300,
            R.color.system_accent3_400,
            R.color.system_accent3_50,
            R.color.system_accent3_500,
            R.color.system_accent3_600,
            R.color.system_accent3_700,
            R.color.system_accent3_800,
            R.color.system_accent3_900,
            R.color.system_background_dark,
            R.color.system_background_light,
            R.color.system_control_activated_dark,
            R.color.system_control_activated_light,
            R.color.system_control_highlight_dark,
            R.color.system_control_highlight_light,
            R.color.system_control_normal_dark,
            R.color.system_control_normal_light,
            R.color.system_error_0,
            R.color.system_error_10,
            R.color.system_error_100,
            R.color.system_error_1000,
            R.color.system_error_200,
            R.color.system_error_300,
            R.color.system_error_400,
            R.color.system_error_50,
            R.color.system_error_500,
            R.color.system_error_600,
            R.color.system_error_700,
            R.color.system_error_800,
            R.color.system_error_900,
            R.color.system_error_container_dark,
            R.color.system_error_container_light,
            R.color.system_error_dark,
            R.color.system_error_light,
            R.color.system_neutral1_0,
            R.color.system_neutral1_10,
            R.color.system_neutral1_100,
            R.color.system_neutral1_1000,
            R.color.system_neutral1_200,
            R.color.system_neutral1_300,
            R.color.system_neutral1_400,
            R.color.system_neutral1_50,
            R.color.system_neutral1_500,
            R.color.system_neutral1_600,
            R.color.system_neutral1_700,
            R.color.system_neutral1_800,
            R.color.system_neutral1_900,
            R.color.system_neutral2_0,
            R.color.system_neutral2_10,
            R.color.system_neutral2_100,
            R.color.system_neutral2_1000,
            R.color.system_neutral2_200,
            R.color.system_neutral2_300,
            R.color.system_neutral2_400,
            R.color.system_neutral2_50,
            R.color.system_neutral2_500,
            R.color.system_neutral2_600,
            R.color.system_neutral2_700,
            R.color.system_neutral2_800,
            R.color.system_neutral2_900,
            R.color.system_on_background_dark,
            R.color.system_on_background_light,
            R.color.system_on_error_container_dark,
            R.color.system_on_error_container_light,
            R.color.system_on_error_dark,
            R.color.system_on_error_light,
            R.color.system_on_primary_container_dark,
            R.color.system_on_primary_container_light,
            R.color.system_on_primary_dark,
            R.color.system_on_primary_fixed,
            R.color.system_on_primary_fixed_variant,
            R.color.system_on_primary_light,
            R.color.system_on_secondary_container_dark,
            R.color.system_on_secondary_container_light,
            R.color.system_on_secondary_dark,
            R.color.system_on_secondary_fixed,
            R.color.system_on_secondary_fixed_variant,
            R.color.system_on_secondary_light,
            R.color.system_on_surface_dark,
            R.color.system_on_surface_disabled,
            R.color.system_on_surface_light,
            R.color.system_on_surface_variant_dark,
            R.color.system_on_surface_variant_light,
            R.color.system_on_tertiary_container_dark,
            R.color.system_on_tertiary_container_light,
            R.color.system_on_tertiary_dark,
            R.color.system_on_tertiary_fixed,
            R.color.system_on_tertiary_fixed_variant,
            R.color.system_on_tertiary_light,
            R.color.system_outline_dark,
            R.color.system_outline_disabled,
            R.color.system_outline_light,
            R.color.system_outline_variant_dark,
            R.color.system_outline_variant_light,
            R.color.system_palette_key_color_neutral_dark,
            R.color.system_palette_key_color_neutral_light,
            R.color.system_palette_key_color_neutral_variant_dark,
            R.color.system_palette_key_color_neutral_variant_light,
            R.color.system_palette_key_color_primary_dark,
            R.color.system_palette_key_color_primary_light,
            R.color.system_palette_key_color_secondary_dark,
            R.color.system_palette_key_color_secondary_light,
            R.color.system_palette_key_color_tertiary_dark,
            R.color.system_palette_key_color_tertiary_light,
            R.color.system_primary_container_dark,
            R.color.system_primary_container_light,
            R.color.system_primary_dark,
            R.color.system_primary_fixed,
            R.color.system_primary_fixed_dim,
            R.color.system_primary_light,
            R.color.system_secondary_container_dark,
            R.color.system_secondary_container_light,
            R.color.system_secondary_dark,
            R.color.system_secondary_fixed,
            R.color.system_secondary_fixed_dim,
            R.color.system_secondary_light,
            R.color.system_surface_bright_dark,
            R.color.system_surface_bright_light,
            R.color.system_surface_container_dark,
            R.color.system_surface_container_high_dark,
            R.color.system_surface_container_high_light,
            R.color.system_surface_container_highest_dark,
            R.color.system_surface_container_highest_light,
            R.color.system_surface_container_light,
            R.color.system_surface_container_low_dark,
            R.color.system_surface_container_low_light,
            R.color.system_surface_container_lowest_dark,
            R.color.system_surface_container_lowest_light,
            R.color.system_surface_dark,
            R.color.system_surface_dim_dark,
            R.color.system_surface_dim_light,
            R.color.system_surface_disabled,
            R.color.system_surface_light,
            R.color.system_surface_variant_dark,
            R.color.system_surface_variant_light,
            R.color.system_tertiary_container_dark,
            R.color.system_tertiary_container_light,
            R.color.system_tertiary_dark,
            R.color.system_tertiary_fixed,
            R.color.system_tertiary_fixed_dim,
            R.color.system_tertiary_light,
            R.color.system_text_hint_inverse_dark,
            R.color.system_text_hint_inverse_light,
            R.color.system_text_primary_inverse_dark,
            R.color.system_text_primary_inverse_disable_only_dark,
            R.color.system_text_primary_inverse_disable_only_light,
            R.color.system_text_primary_inverse_light,
            R.color.system_text_secondary_and_tertiary_inverse_dark,
            R.color.system_text_secondary_and_tertiary_inverse_disabled_dark,
            R.color.system_text_secondary_and_tertiary_inverse_disabled_light,
            R.color.system_text_secondary_and_tertiary_inverse_light,
            R.color.tab_indicator_text,
        )

    fun mapColors(context: Context, document: CoreDocument) {
        val themedColors = document.themedColors ?: return
        mapColors(context, themedColors)
    }

    fun mapColors(context: Context, themedColors: List<ColorTheme>) {
        for (i in themedColors.indices) {
            val theme = themedColors[i]
            // Skip non-Android color groups (or null group names) to match View player's
            // mColorEngineMap lookup.
            if (theme.mColorGroupName != "android") {
                continue
            }
            val darkIndex = theme.mDarkModeIndex.toInt()
            if (darkIndex in colorResourceIds.indices) {
                try {
                    theme.mDarkMode = context.getColor(colorResourceIds[darkIndex])
                } catch (_: Exception) {
                    // Fall back to authored color
                }
            }
            val lightIndex = theme.mLightModeIndex.toInt()
            if (lightIndex in colorResourceIds.indices) {
                try {
                    theme.mLightMode = context.getColor(colorResourceIds[lightIndex])
                } catch (_: Exception) {
                    // Fall back to authored color
                }
            }
            theme.markDirty()
        }
    }
}
