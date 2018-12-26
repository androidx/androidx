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

package androidx.ui.port.rendering

import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.ui.engine.geometry.Size
import androidx.ui.engine.text.TextDirection
import androidx.ui.engine.text.font.FontFamily
import androidx.ui.engine.text.font.asFontFamily
import androidx.ui.painting.TextSpan
import androidx.ui.painting.TextStyle
import androidx.ui.port.engine.text.FontTestData.Companion.BASIC_MEASURE_FONT
import androidx.ui.rendering.paragraph.RenderParagraph
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.math.ceil
import kotlin.math.floor

@RunWith(JUnit4::class)
@SmallTest
class RenderParagraphTest {
    private lateinit var fontFamily: FontFamily

    @Before
    fun setup() {
        fontFamily = BASIC_MEASURE_FONT.asFontFamily()
        fontFamily.context = InstrumentationRegistry.getInstrumentation().context
    }

    @Test
    fun computeMinIntrinsicWidth_returnMinWidth() {
        val fontSize = 20.0
        val text = "Hello"
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val textSpan = TextSpan(text = text, style = textStyle)
        val paragraph = RenderParagraph(text = textSpan, textDirection = TextDirection.LTR)

        assertThat(paragraph.computeMinIntrinsicWidth(0.0)).isEqualTo(0.0)
    }

    @Test
    fun computeMaxIntrinsicWidth_returnParagraphWidth() {
        val fontSize = 20.0
        val text = "Hello"
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val textSpan = TextSpan(text = text, style = textStyle)
        val paragraph = RenderParagraph(text = textSpan, textDirection = TextDirection.LTR)

        assertThat(paragraph.computeMaxIntrinsicWidth(0.0)).isEqualTo(fontSize * text.length)
    }

    @Test
    fun computeIntrinsicHeight_wrap() {
        val fontSize = 16.8
        val text = "Hello"
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val textSpan = TextSpan(text = text, style = textStyle)
        val paragraph = RenderParagraph(text = textSpan, textDirection = TextDirection.LTR)
        val maxWidth = 38.0
        val charPerLine = floor(maxWidth / fontSize)
        val numberOfLines = ceil(text.length / charPerLine)

        assertThat(paragraph.computeIntrinsicHeight(maxWidth))
            .isEqualTo(
                ceil(
                    fontSize * numberOfLines +
                            (fontSize / 5 /*gap due to font*/) * (numberOfLines - 1)
                )
            )
    }

    @Test
    fun computeIntrinsicHeight_not_wrap() {
        val fontSize = 20.0
        val text = "Hello"
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val textSpan = TextSpan(text = text, style = textStyle)
        val paragraph =
            RenderParagraph(text = textSpan, textDirection = TextDirection.LTR, softWrap = false)
        val maxWidth = 38.0

        assertThat(paragraph.computeIntrinsicHeight(maxWidth)).isEqualTo(fontSize)
    }

    @Test
    fun textSizeGetter() {
        val fontSize = 20.0
        val text = "Hello"
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val textSpan = TextSpan(text = text, style = textStyle)
        val paragraph = RenderParagraph(text = textSpan, textDirection = TextDirection.LTR)

        paragraph.layoutText()

        assertThat(paragraph.textSize)
            .isEqualTo(Size(width = fontSize * text.length, height = fontSize))
    }

    @Test
    fun textWidthGetter() {
        val fontSize = 20.0
        val text = "Hello"
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val textSpan = TextSpan(text = text, style = textStyle)
        val paragraph = RenderParagraph(text = textSpan, textDirection = TextDirection.LTR)

        paragraph.layoutText()

        assertThat(paragraph.width).isEqualTo(fontSize * text.length)
    }

    @Test
    fun textHeightGetter() {
        val fontSize = 20.0
        val text = "Hello"
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val textSpan = TextSpan(text = text, style = textStyle)
        val paragraph = RenderParagraph(text = textSpan, textDirection = TextDirection.LTR)

        paragraph.layoutText()

        assertThat(paragraph.height).isEqualTo(fontSize)
    }
}