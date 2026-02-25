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

package androidx.pdf.ink.view.colorpalette.model

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.ContextCompat
import androidx.pdf.ink.R
import com.google.android.material.R as MaterialR
import com.google.android.material.color.MaterialColors

/**
 * Provides a predefined list of palette items designed for pen annotation tools.
 *
 * @param context The context used to resolve theme attributes and colors.
 * @return A [List] of [PaletteItem]s, primarily consisting of [Color] objects for pen tools.
 */
internal fun getPenPaletteItems(context: Context): List<PaletteItem> {

    val outlineColor =
        MaterialColors.getColor(
            context,
            MaterialR.attr.colorOutlineVariant,
            ContextCompat.getColor(context, R.color.default_outline_color),
        )

    return listOf<PaletteItem>(
        // Row#1 Colors
        Color(
            0xFF000000.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context, inverse = true),
            contentDescription = context.getString(R.string.pdf_pen_color_palette_item_black),
        ),
        Color(
            0xFF202FB0.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context, inverse = true),
            contentDescription = context.getString(R.string.pdf_pen_color_palette_item_dark_blue),
        ),
        Color(
            0xFFDD0000.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context, inverse = true),
            contentDescription = context.getString(R.string.pdf_pen_color_palette_item_red),
        ),
        Color(
            0xFF00854C.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context, inverse = true),
            contentDescription = context.getString(R.string.pdf_pen_color_palette_item_green),
        ),
        Color(
            0xFFD9C300.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context, inverse = true),
            contentDescription = context.getString(R.string.pdf_pen_color_palette_item_gold),
        ),
        Color(
            0xFFC2C44D.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context, inverse = true),
            contentDescription = context.getString(R.string.pdf_pen_color_palette_item_olive_green),
        ),
        Color(
            0xFF7B4B19.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context, inverse = true),
            contentDescription = context.getString(R.string.pdf_pen_color_palette_item_brown),
        ),

        // Row#2 Colors
        Color(
            0xFF757575.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context),
            contentDescription = context.getString(R.string.pdf_pen_color_palette_item_gray),
        ),
        Color(
            0xFF3D5FEC.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context),
            contentDescription = context.getString(R.string.pdf_pen_color_palette_item_blue),
        ),
        Color(
            0xFFFF4365.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context),
            contentDescription = context.getString(R.string.pdf_pen_color_palette_item_coral_red),
        ),
        Color(
            0xFF35C369.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context),
            contentDescription = context.getString(R.string.pdf_pen_color_palette_item_lime_green),
        ),
        Color(
            0xFFF9E100.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context),
            contentDescription = context.getString(R.string.pdf_pen_color_palette_item_yellow),
        ),
        Color(
            0xFFD7E871.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context),
            contentDescription = context.getString(R.string.pdf_pen_color_palette_item_pale_lime),
        ),
        Color(
            0xFFC48150.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context),
            contentDescription = context.getString(R.string.pdf_pen_color_palette_item_tan),
        ),

        // Row#3 Colors
        Color(
            0xFFC7C7C7.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context),
            contentDescription = context.getString(R.string.pdf_pen_color_palette_item_light_gray),
        ),
        Color(
            0xFF9976FF.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context),
            contentDescription = context.getString(R.string.pdf_pen_color_palette_item_purple),
        ),
        Color(
            0xFFFF8FEA.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context),
            contentDescription = context.getString(R.string.pdf_pen_color_palette_item_pink),
        ),
        Color(
            0xFF9BEEC7.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context),
            contentDescription = context.getString(R.string.pdf_pen_color_palette_item_mint_green),
        ),
        Color(
            0xFFFAB400.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context),
            contentDescription = context.getString(R.string.pdf_pen_color_palette_item_orange),
        ),
        Color(
            0xFF5888B2.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context),
            contentDescription = context.getString(R.string.pdf_pen_color_palette_item_steel_blue),
        ),
        Color(
            0xFFB8372F.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context),
            contentDescription = context.getString(R.string.pdf_pen_color_palette_item_deep_red),
        ),

        // Row#4 Colors
        Color(
            0xFFFFFFFF.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context),
            contentDescription = context.getString(R.string.pdf_pen_color_palette_item_white),
        ),
        Color(
            0xFFC6B9FF.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context),
            contentDescription = context.getString(R.string.pdf_pen_color_palette_item_lavender),
        ),
        Color(
            0xFFFFD4F8.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context),
            contentDescription = context.getString(R.string.pdf_pen_color_palette_item_pale_pink),
        ),
        Color(
            0xFFDEFFC9.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context),
            contentDescription = context.getString(R.string.pdf_pen_color_palette_item_pale_green),
        ),
        Color(
            0xFFFFC8A0.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context),
            contentDescription = context.getString(R.string.pdf_pen_color_palette_item_peach),
        ),
        Color(
            0xFFD1E5EE.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context),
            contentDescription = context.getString(R.string.pdf_pen_color_palette_item_light_blue),
        ),
        Color(
            0xFFED9D82.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context),
            contentDescription = context.getString(R.string.pdf_pen_color_palette_item_salmon),
        ),
    )
}

/**
 * Provides a predefined list of palette items designed for highlight annotation tools.
 *
 * @param context The context used to resolve theme attributes and colors.
 * @return A [List] of [PaletteItem]s, consisting of [Color] and [Emoji] objects for highlight
 *   tools.
 */
internal fun getHighlightPaletteItems(context: Context): List<PaletteItem> {
    val outlineColor =
        MaterialColors.getColor(
            context,
            MaterialR.attr.colorOutlineVariant,
            ContextCompat.getColor(context, R.color.default_outline_color),
        )

    return listOf(
        // Row#1 Colors
        Color(
            0xFF000000.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context, inverse = true),
            contentDescription =
                context.getString(R.string.pdf_highlighter_color_palette_item_dark_gray),
        ),
        Color(
            0xFFFFFFFF.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context),
            contentDescription =
                context.getString(R.string.pdf_highlighter_color_palette_item_white),
        ),
        Color(
            0xFFFF0000.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context, inverse = true),
            contentDescription =
                context.getString(R.string.pdf_highlighter_color_palette_item_coral_red),
        ),
        Color(
            0xFF00FF00.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context, inverse = true),
            contentDescription =
                context.getString(R.string.pdf_highlighter_color_palette_item_lime_green),
        ),
        Color(
            0xFF0000FF.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context, inverse = true),
            contentDescription =
                context.getString(R.string.pdf_highlighter_color_palette_item_periwinkle_blue),
        ),
        Color(
            0xFFFFA500.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context, inverse = true),
            contentDescription =
                context.getString(R.string.pdf_highlighter_color_palette_item_orange),
        ),
        Color(
            0xFFFFFF00.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context),
            contentDescription =
                context.getString(R.string.pdf_highlighter_color_palette_item_yellow),
        ),
        // Row#2 Colors
        Color(
            0xFFFFC0CB.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context),
            contentDescription =
                context.getString(R.string.pdf_highlighter_color_palette_item_pale_pink),
        ),
        Color(
            0xFFADD8E6.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context),
            contentDescription =
                context.getString(R.string.pdf_highlighter_color_palette_item_light_blue),
        ),
        Color(
            0xFF90EE90.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context),
            contentDescription =
                context.getString(R.string.pdf_highlighter_color_palette_item_pale_green),
        ),
        Color(
            0xFFFFED45.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context),
            contentDescription =
                context.getString(R.string.pdf_highlighter_color_palette_item_bright_yellow),
        ),
        Color(
            0xFFFF8279.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context),
            contentDescription =
                context.getString(R.string.pdf_highlighter_color_palette_item_peach),
        ),
        Color(
            0xFFA52A2A.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context),
            contentDescription =
                context.getString(R.string.pdf_highlighter_color_palette_item_dusty_rose),
        ),
        Color(
            0xFF808000.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context),
            contentDescription =
                context.getString(R.string.pdf_highlighter_color_palette_item_olive_green),
        ),
        // Row#3 Colors
        Color(
            0xFF800080.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context),
            contentDescription =
                context.getString(R.string.pdf_highlighter_color_palette_item_medium_purple),
        ),
        Color(
            0xFF008000.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context),
            contentDescription =
                context.getString(R.string.pdf_highlighter_color_palette_item_medium_green),
        ),
        Color(
            0xFFDC143C.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context),
            contentDescription =
                context.getString(R.string.pdf_highlighter_color_palette_item_rose_pink),
        ),
        Color(
            0xFF4682B4.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context),
            contentDescription =
                context.getString(R.string.pdf_highlighter_color_palette_item_steel_blue),
        ),
        Color(
            0xFF6A5ACD.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context),
            contentDescription =
                context.getString(R.string.pdf_highlighter_color_palette_item_lilac),
        ),
        Color(
            0xFF556B2F.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context),
            contentDescription =
                context.getString(R.string.pdf_highlighter_color_palette_item_sage_green),
        ),
        Color(
            0xFFDEB887.toInt(),
            outlineColor = outlineColor,
            tickColor = getTickColor(context),
            contentDescription =
                context.getString(R.string.pdf_highlighter_color_palette_item_beige),
        ),
    )
}

/**
 * Determines the appropriate tick color based on the theme (light/dark) and the desired contrast.
 *
 * This function selects between `colorOnSurfaceInverse` (typically light) and `colorSurfaceInverse`
 * (typically dark) to ensure the tick mark has good contrast.
 *
 * @param context The context to resolve theme attributes.
 * @param inverse If true, inverts the default tick color for the current theme.
 * @return The resolved integer value for the tick color.
 */
private fun getTickColor(context: Context, inverse: Boolean = false): Int {
    val colorOnSurfaceInverse =
        MaterialColors.getColor(
            context,
            MaterialR.attr.colorOnSurfaceInverse,
            ContextCompat.getColor(context, R.color.default_light_tick_color),
        )

    val colorSurfaceInverse =
        MaterialColors.getColor(
            context,
            MaterialR.attr.colorSurfaceInverse,
            ContextCompat.getColor(context, R.color.default_dark_tick_color),
        )

    val isDarkTheme =
        (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    return if (isDarkTheme == inverse) colorSurfaceInverse else colorOnSurfaceInverse
}
