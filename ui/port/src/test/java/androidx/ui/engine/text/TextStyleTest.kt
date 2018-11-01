/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.ui.engine.text

import androidx.ui.engine.window.Locale
import androidx.ui.painting.Color
import androidx.ui.painting.Paint
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TextStyleTest {

    @Test(expected = AssertionError::class)
    fun `constructor with both color and foreground defined throws AssertionError`() {
        TextStyle(
            color = Color.fromARGB(1, 1, 1, 1),
            foreground = Paint()
        )
    }

    @Test
    fun `toString with null values`() {
        val textStyle = TextStyle()
        assertThat(
            textStyle.toString(),
            `is`(
                equalTo(
                    "TextStyle(" +
                        "color: unspecified, " +
                        "decoration: unspecified, " +
                        "decorationColor: unspecified, " +
                        "decorationStyle: unspecified, " +
                        "fontWeight: unspecified, " +
                        "fontStyle: unspecified, " +
                        "textBaseline: unspecified, " +
                        "fontFamily: unspecified, " +
                        "fontSize: unspecified, " +
                        "letterSpacing: unspecified, " +
                        "wordSpacing: unspecified, " +
                        "height: unspecified, " +
                        "locale: unspecified, " +
                        "background: unspecified, " +
                        "foreground: unspecified)"
                )
            )
        )
    }

    @Test
    fun `toString with values`() {
        val color = Color.fromARGB(1, 2, 3, 4)
        val decoration = TextDecoration.overline
        val decorationColor = Color.fromARGB(5, 6, 7, 8)
        val decorationStyle = TextDecorationStyle.dashed
        val fontWeight = FontWeight.bold
        val fontStyle = FontStyle.italic
        val textBaseline = TextBaseline.alphabetic
        val fontFamily = FontFallback()
        val fontSize = 1.0
        val letterSpacing = 2.0
        val wordSpacing = 3.0
        val height = 4.0
        val locale = Locale("en")
        val background = Paint()

        val textStyle = TextStyle(
            color = color,
            decoration = decoration,
            decorationColor = decorationColor,
            decorationStyle = decorationStyle,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            textBaseline = textBaseline,
            fontFamily = fontFamily,
            fontSize = fontSize,
            letterSpacing = letterSpacing,
            wordSpacing = wordSpacing,
            height = height,
            locale = locale,
            background = background,
            foreground = null
        )

        assertThat(
            textStyle.toString(),
            `is`(
                equalTo(
                    "TextStyle(" +
                        "color: $color, " +
                        "decoration: $decoration, " +
                        "decorationColor: $decorationColor, " +
                        "decorationStyle: $decorationStyle, " +
                        "fontWeight: $fontWeight, " +
                        "fontStyle: $fontStyle, " +
                        "textBaseline: $textBaseline, " +
                        "fontFamily: $fontFamily, " +
                        "fontSize: $fontSize, " +
                        "letterSpacing: ${letterSpacing}x, " +
                        "wordSpacing: ${wordSpacing}x, " +
                        "height: ${height}x, " +
                        "locale: $locale, " +
                        "background: $background, " +
                        "foreground: unspecified)"
                )
            )
        )
    }
}