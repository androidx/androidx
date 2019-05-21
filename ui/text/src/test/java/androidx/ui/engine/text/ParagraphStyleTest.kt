/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.ui.engine.text.font.FontFamily
import androidx.ui.engine.window.Locale
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.hamcrest.CoreMatchers.nullValue
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ParagraphStyleTest {

    @Test
    fun `toString with null values`() {
        val paragraphStyle = ParagraphStyle()
        assertThat(
            paragraphStyle.toString(), `is`(
                equalTo(
                    "ParagraphStyle(" +
                        "textAlign: unspecified, " +
                        "textDirection: unspecified, " +
                        "fontWeight: unspecified, " +
                        "fontStyle: unspecified, " +
                        "maxLines: unspecified, " +
                        "fontFamily: unspecified, " +
                        "fontSize: unspecified, " +
                        "lineHeight: unspecified, " +
                        "ellipsis: unspecified, " +
                        "locale: unspecified, " +
                        "fontSynthesis: unspecified" +
                        ")"
                )
            )
        )
    }

    @Test
    fun `getTextStyle with non-null values`() {
        val textAlign = TextAlign.End
        val textDirection = TextDirection.Rtl
        val fontWeight = FontWeight.bold
        val fontStyle = FontStyle.Italic
        val maxLines = 2
        val fontFamily = FontFamily("sans-serif")
        val fontSize = 1.0f
        val lineHeight = 2.0f
        val ellipsis = true
        val locale = Locale("en")
        val fontSynthesis = FontSynthesis.style

        val paragraphStyle = ParagraphStyle(
            textAlign = textAlign,
            textDirection = textDirection,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            maxLines = maxLines,
            fontFamily = fontFamily,
            fontSize = fontSize,
            lineHeight = lineHeight,
            ellipsis = ellipsis,
            locale = locale,
            fontSynthesis = fontSynthesis
        )

        val textStyle = paragraphStyle.getTextStyle()
        assertThat(textStyle, not(nullValue()))

        assertThat(textStyle.fontWeight, equalTo(paragraphStyle.fontWeight))
        assertThat(textStyle.fontStyle, equalTo(paragraphStyle.fontStyle))
        assertThat(textStyle.fontFamily, equalTo(paragraphStyle.fontFamily))
        assertThat(textStyle.fontSize, equalTo(paragraphStyle.fontSize))
        assertThat(textStyle.locale, equalTo(paragraphStyle.locale))
        assertThat(textStyle.height, equalTo(paragraphStyle.lineHeight))
        assertThat(textStyle.fontSynthesis, equalTo(paragraphStyle.fontSynthesis))
    }

    @Test
    fun `getTextStyle with null values`() {
        val paragraphStyle = ParagraphStyle(
            textAlign = null,
            textDirection = null,
            fontWeight = null,
            fontStyle = null,
            maxLines = null,
            fontFamily = null,
            fontSize = null,
            lineHeight = null,
            ellipsis = null,
            locale = null,
            fontSynthesis = null
        )

        val textStyle = paragraphStyle.getTextStyle()
        assertThat(textStyle, not(nullValue()))

        assertThat(textStyle.fontWeight, `is`(nullValue()))
        assertThat(textStyle.fontStyle, `is`(nullValue()))
        assertThat(textStyle.fontFamily, `is`(nullValue()))
        assertThat(textStyle.fontSize, `is`(nullValue()))
        assertThat(textStyle.locale, `is`(nullValue()))
        assertThat(textStyle.height, `is`(nullValue()))
        assertThat(textStyle.fontSynthesis, `is`(nullValue()))
    }

    @Test
    fun `toString with values`() {
        val textAlign = TextAlign.End
        val textDirection = TextDirection.Rtl
        val fontWeight = FontWeight.bold
        val fontStyle = FontStyle.Italic
        val maxLines = 2
        val fontFamily = FontFamily("sans-serif")
        val fontSize = 1.0f
        val lineHeight = 2.0f
        val ellipsis = false
        val locale = Locale("en")
        val fontSynthesis = FontSynthesis.style

        val paragraphStyle = ParagraphStyle(
            textAlign = textAlign,
            textDirection = textDirection,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            maxLines = maxLines,
            fontFamily = fontFamily,
            fontSize = fontSize,
            lineHeight = lineHeight,
            ellipsis = ellipsis,
            locale = locale,
            fontSynthesis = fontSynthesis
        )

        assertThat(
            paragraphStyle.toString(), `is`(
                equalTo(
                    "ParagraphStyle(" +
                        "textAlign: $textAlign, " +
                        "textDirection: $textDirection, " +
                        "fontWeight: $fontWeight, " +
                        "fontStyle: $fontStyle, " +
                        "maxLines: $maxLines, " +
                        "fontFamily: $fontFamily, " +
                        "fontSize: $fontSize, " +
                        "lineHeight: ${lineHeight}x, " +
                        "ellipsis: \"$ellipsis\", " +
                        "locale: $locale, " +
                        "fontSynthesis: $fontSynthesis" +
                        ")"
                )
            )
        )
    }
}