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

package androidx.pdf.ink.view.colorpalette

import android.content.Context
import androidx.pdf.ink.R
import androidx.pdf.ink.view.colorpalette.model.Color
import androidx.pdf.ink.view.colorpalette.model.getHighlightPaletteItems
import androidx.pdf.ink.view.colorpalette.model.getPenPaletteItems
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ColorsTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun penPalette_contentDescriptionsAreCorrect() {
        val penPalette = getPenPaletteItems(context)

        val expectedColors =
            mapOf(
                0xFF000000.toInt() to context.getString(R.string.pdf_pen_color_palette_item_black),
                0xFF202FB0.toInt() to
                    context.getString(R.string.pdf_pen_color_palette_item_dark_blue),
                0xFFDD0000.toInt() to context.getString(R.string.pdf_pen_color_palette_item_red),
                0xFF00854C.toInt() to context.getString(R.string.pdf_pen_color_palette_item_green),
                0xFFD9C300.toInt() to context.getString(R.string.pdf_pen_color_palette_item_gold),
                0xFFC2C44D.toInt() to
                    context.getString(R.string.pdf_pen_color_palette_item_olive_green),
                0xFF7B4B19.toInt() to context.getString(R.string.pdf_pen_color_palette_item_brown),
                0xFF757575.toInt() to context.getString(R.string.pdf_pen_color_palette_item_gray),
                0xFF3D5FEC.toInt() to context.getString(R.string.pdf_pen_color_palette_item_blue),
                0xFFFF4365.toInt() to
                    context.getString(R.string.pdf_pen_color_palette_item_coral_red),
                0xFF35C369.toInt() to
                    context.getString(R.string.pdf_pen_color_palette_item_lime_green),
                0xFFF9E100.toInt() to context.getString(R.string.pdf_pen_color_palette_item_yellow),
                0xFFD7E871.toInt() to
                    context.getString(R.string.pdf_pen_color_palette_item_pale_lime),
                0xFFC48150.toInt() to context.getString(R.string.pdf_pen_color_palette_item_tan),
                0xFFC7C7C7.toInt() to
                    context.getString(R.string.pdf_pen_color_palette_item_light_gray),
                0xFF9976FF.toInt() to context.getString(R.string.pdf_pen_color_palette_item_purple),
                0xFFFF8FEA.toInt() to context.getString(R.string.pdf_pen_color_palette_item_pink),
                0xFF9BEEC7.toInt() to
                    context.getString(R.string.pdf_pen_color_palette_item_mint_green),
                0xFFFAB400.toInt() to context.getString(R.string.pdf_pen_color_palette_item_orange),
                0xFF5888B2.toInt() to
                    context.getString(R.string.pdf_pen_color_palette_item_steel_blue),
                0xFFB8372F.toInt() to
                    context.getString(R.string.pdf_pen_color_palette_item_deep_red),
                0xFFFFFFFF.toInt() to context.getString(R.string.pdf_pen_color_palette_item_white),
                0xFFC6B9FF.toInt() to
                    context.getString(R.string.pdf_pen_color_palette_item_lavender),
                0xFFFFD4F8.toInt() to
                    context.getString(R.string.pdf_pen_color_palette_item_pale_pink),
                0xFFDEFFC9.toInt() to
                    context.getString(R.string.pdf_pen_color_palette_item_pale_green),
                0xFFFFC8A0.toInt() to context.getString(R.string.pdf_pen_color_palette_item_peach),
                0xFFD1E5EE.toInt() to
                    context.getString(R.string.pdf_pen_color_palette_item_light_blue),
                0xFFED9D82.toInt() to context.getString(R.string.pdf_pen_color_palette_item_salmon),
            )

        val actualColors =
            penPalette.filterIsInstance<Color>().associate { it.color to it.contentDescription }
        assertEquals(expectedColors.size, actualColors.size)
        expectedColors.forEach { (color, contentDesc) ->
            assertEquals(contentDesc, actualColors[color], "Mismatch for color $color")
        }
    }

    @Test
    fun highlighterPalette_contentDescriptionsAreCorrect() {
        val highlighterPalette = getHighlightPaletteItems(context)

        val expectedColors =
            mapOf(
                0xFF000000.toInt() to
                    context.getString(R.string.pdf_highlighter_color_palette_item_dark_gray),
                0xFFFFFFFF.toInt() to
                    context.getString(R.string.pdf_highlighter_color_palette_item_white),
                0xFFFF0000.toInt() to
                    context.getString(R.string.pdf_highlighter_color_palette_item_coral_red),
                0xFF00FF00.toInt() to
                    context.getString(R.string.pdf_highlighter_color_palette_item_lime_green),
                0xFF0000FF.toInt() to
                    context.getString(R.string.pdf_highlighter_color_palette_item_periwinkle_blue),
                0xFFFFA500.toInt() to
                    context.getString(R.string.pdf_highlighter_color_palette_item_orange),
                0xFFFFFF00.toInt() to
                    context.getString(R.string.pdf_highlighter_color_palette_item_yellow),
                0xFFFFC0CB.toInt() to
                    context.getString(R.string.pdf_highlighter_color_palette_item_pale_pink),
                0xFFADD8E6.toInt() to
                    context.getString(R.string.pdf_highlighter_color_palette_item_light_blue),
                0xFF90EE90.toInt() to
                    context.getString(R.string.pdf_highlighter_color_palette_item_pale_green),
                0xFFFFED45.toInt() to
                    context.getString(R.string.pdf_highlighter_color_palette_item_bright_yellow),
                0xFFFF8279.toInt() to
                    context.getString(R.string.pdf_highlighter_color_palette_item_peach),
                0xFFA52A2A.toInt() to
                    context.getString(R.string.pdf_highlighter_color_palette_item_dusty_rose),
                0xFF808000.toInt() to
                    context.getString(R.string.pdf_highlighter_color_palette_item_olive_green),
                0xFF800080.toInt() to
                    context.getString(R.string.pdf_highlighter_color_palette_item_medium_purple),
                0xFF008000.toInt() to
                    context.getString(R.string.pdf_highlighter_color_palette_item_medium_green),
                0xFFDC143C.toInt() to
                    context.getString(R.string.pdf_highlighter_color_palette_item_rose_pink),
                0xFF4682B4.toInt() to
                    context.getString(R.string.pdf_highlighter_color_palette_item_steel_blue),
                0xFF6A5ACD.toInt() to
                    context.getString(R.string.pdf_highlighter_color_palette_item_lilac),
                0xFF556B2F.toInt() to
                    context.getString(R.string.pdf_highlighter_color_palette_item_sage_green),
                0xFFDEB887.toInt() to
                    context.getString(R.string.pdf_highlighter_color_palette_item_beige),
            )

        val actualColors =
            highlighterPalette.filterIsInstance<Color>().associate {
                it.color to it.contentDescription
            }
        assertEquals(expectedColors.size, actualColors.size)
        expectedColors.forEach { (color, contentDesc) ->
            assertEquals(contentDesc, actualColors[color], "Mismatch for color $color")
        }
    }
}
