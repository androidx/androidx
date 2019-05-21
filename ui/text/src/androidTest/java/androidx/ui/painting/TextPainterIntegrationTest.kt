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

package androidx.ui.painting

import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Size
import androidx.ui.engine.text.FontTestData.Companion.BASIC_MEASURE_FONT
import androidx.ui.engine.text.TextDirection
import androidx.ui.engine.text.font.FontFamily
import androidx.ui.engine.text.font.asFontFamily
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@SmallTest
class TextPainterIntegrationTest {

    private lateinit var fontFamily: FontFamily

    @Before
    fun setup() {
        fontFamily = BASIC_MEASURE_FONT.asFontFamily()
        fontFamily.context = InstrumentationRegistry.getInstrumentation().context
    }

    @Test
    fun preferredLineHeight_style_set() {
        val fontSize = 20.0f
        val scaleFactor = 3.0f
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val textSpan = TextSpan(text = "Hello", style = textStyle)
        val textPainter = TextPainter(text = textSpan, textScaleFactor = scaleFactor)

        val prefferedHeight = textPainter.preferredLineHeight

        assertThat(prefferedHeight).isEqualTo(fontSize * scaleFactor)
    }

    // TODO(Migration/qqd): The default font size should be 14.0 but it returns 15.0. Need further
    // investigation. It is being changed in the native level, and probably related to the font.
//    @Test
//    fun preferredLineHeight_style_not_set() {
//        val textStyle = TextStyle(fontFamily = fontFallback)
//        val textSpan = TextSpan(text = "Hello", style = textStyle)
//        val textPainter = TextPainter(text = textSpan)
//
//        val prefferedHeight = textPainter.preferredLineHeight
//
//        assertThat(prefferedHeight).isEqualTo(14.0)
//    }

    @Test
    fun minIntrinsicWidth_getter() {
        val fontSize = 20.0f
        val text = "Hello"
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val textSpan = TextSpan(text = text, style = textStyle)
        val textPainter = TextPainter(text = textSpan, textDirection = TextDirection.Rtl)

        textPainter.layout()

        assertThat(textPainter.minIntrinsicWidth).isEqualTo(0.0f)
    }

    @Test
    fun maxIntrinsicWidth_getter() {
        val fontSize = 20.0f
        val text = "Hello"
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val textSpan = TextSpan(text = text, style = textStyle)
        val textPainter = TextPainter(text = textSpan, textDirection = TextDirection.Rtl)

        textPainter.layout()

        assertThat(textPainter.maxIntrinsicWidth).isEqualTo(fontSize * text.length)
    }

    @Test
    fun width_getter() {
        val fontSize = 20.0f
        val text = "Hello"
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val textSpan = TextSpan(text = text, style = textStyle)
        val textPainter = TextPainter(text = textSpan, textDirection = TextDirection.Rtl)

        textPainter.layout(0.0f, 200.0f)

        assertThat(textPainter.width).isEqualTo(fontSize * text.length)
    }

    @Test
    fun height_getter() {
        val fontSize = 20.0f
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val textSpan = TextSpan(text = "Hello", style = textStyle)
        val textPainter = TextPainter(text = textSpan, textDirection = TextDirection.Rtl)

        textPainter.layout()

        assertThat(textPainter.height).isEqualTo(fontSize)
    }

    @Test
    fun size_getter() {
        val fontSize = 20.0f
        val text = "Hello"
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
        val textSpan = TextSpan(text = text, style = textStyle)
        val textPainter = TextPainter(text = textSpan, textDirection = TextDirection.Rtl)

        textPainter.layout()

        assertThat(textPainter.size)
            .isEqualTo(Size(width = fontSize * text.length, height = fontSize))
    }

    @Test
    fun didExceedMaxLines_exceed() {
        var text = ""
        for (i in 1..50) text += " Hello"
        val textSpan = TextSpan(text = text)
        val textPainter =
            TextPainter(text = textSpan, textDirection = TextDirection.Rtl, maxLines = 2)

        textPainter.layout(0.0f, 200.0f)

        assertThat(textPainter.didExceedMaxLines).isTrue()
    }

    @Test
    fun didExceedMaxLines_not_exceed() {
        val text = "Hello"
        val textSpan = TextSpan(text = text)
        val textPainter =
            TextPainter(text = textSpan, textDirection = TextDirection.Rtl, maxLines = 2)

        textPainter.layout(0.0f, 200.0f)

        assertThat(textPainter.didExceedMaxLines).isFalse()
    }

    @Test
    fun layout_build_paragraph() {
        val textPainter =
            TextPainter(text = TextSpan(text = "Hello"), textDirection = TextDirection.Ltr)

        textPainter.layout(0.0f, 20.0f)

        assertThat(textPainter.paragraph).isNotNull()
    }

    @Test
    fun getPositionForOffset_First_Character() {
        val fontSize = 20.0f
        val textPainter =
            TextPainter(
                text = TextSpan(
                    text = "Hello",
                    style = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
                ), textDirection = TextDirection.Ltr
            )
        textPainter.layout()

        val selection = textPainter.getPositionForOffset(Offset(dx = 0f, dy = 0f))

        assertThat(selection.offset).isEqualTo(0)
    }

    @Test
    fun getPositionForOffset_other_Character() {
        val fontSize = 20.0f
        val characterIndex = 2 // Start from 0.
        val textPainter =
            TextPainter(
                text = TextSpan(
                    text = "Hello",
                    style = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
                ), textDirection = TextDirection.Ltr
            )
        textPainter.layout()

        val selection =
            textPainter.getPositionForOffset(Offset(dx = fontSize * characterIndex + 1f, dy = 0f))

        assertThat(selection.offset).isEqualTo(characterIndex)
    }
}
