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

package androidx.ui.core.selection

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.ui.core.Constraints
import androidx.ui.core.Density
import androidx.ui.core.LayoutDirection
import androidx.ui.core.PxPosition
import androidx.ui.core.Sp
import androidx.ui.core.px
import androidx.ui.core.sp
import androidx.ui.core.withDensity
import androidx.ui.text.AnnotatedString
import androidx.ui.text.ParagraphStyle
import androidx.ui.text.TextDelegate
import androidx.ui.text.TextRange
import androidx.ui.text.TextStyle
import androidx.ui.text.font.Font
import androidx.ui.text.font.FontStyle
import androidx.ui.text.font.FontWeight
import androidx.ui.text.font.asFontFamily
import androidx.ui.text.style.TextDirection
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

val BASIC_MEASURE_FONT = Font(
    name = "sample_font.ttf",
    weight = FontWeight.Normal,
    style = FontStyle.Normal
)

@RunWith(JUnit4::class)
@SmallTest
class TextSelectionProcessorTest {
    private val fontFamily = BASIC_MEASURE_FONT.asFontFamily()
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val defaultDensity = Density(density = 1f)
    private val resourceLoader = TestFontResourceLoader(context)

    @Test
    fun testTextSelectionProcessor_tap_select_word_ltr() {
        withDensity(defaultDensity) {
            val text = "hello world\n"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value

            val textDelegate = simpleTextDelegate(
                text = text,
                fontSize = fontSize,
                density = defaultDensity
            )

            val start = PxPosition((fontSizeInPx * 2).px, (fontSizeInPx / 2).px)
            val end = start
            var selection: TextRange? = null

            // Act.
            val textSelectionProcessor = TextSelectionProcessor(
                selectionCoordinates = Pair(start, end),
                mode = SelectionMode.Horizontal,
                onSelectionChange = { selection = it },
                textDelegate = textDelegate
            )

            // Assert.
            assertThat(textSelectionProcessor.startCoordinates)
                .isEqualTo(PxPosition(0.px, fontSizeInPx.px))
            assertThat(textSelectionProcessor.endCoordinates)
                .isEqualTo(PxPosition(("hello".length * fontSizeInPx).px, fontSizeInPx.px))
            assertThat(textSelectionProcessor.startDirection).isEqualTo(TextDirection.Ltr)
            assertThat(textSelectionProcessor.endDirection).isEqualTo(TextDirection.Ltr)
            assertThat(textSelectionProcessor.containsWholeSelectionStart).isTrue()
            assertThat(textSelectionProcessor.containsWholeSelectionEnd).isTrue()
            assertThat(textSelectionProcessor.isSelected).isTrue()
            assertThat(selection).isEqualTo(TextRange(0, "hello".length))
        }
    }

    @Test
    fun testTextSelectionProcessor_tap_select_word_rtl() {
        withDensity(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2 \u05D3\u05D4\u05D5\n"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value

            val textDelegate = simpleTextDelegate(
                text = text,
                fontSize = fontSize,
                density = defaultDensity
            )

            val start = PxPosition((fontSizeInPx * 2).px, (fontSizeInPx / 2).px)
            val end = start
            var selection: TextRange? = null

            // Act.
            val textSelectionProcessor = TextSelectionProcessor(
                selectionCoordinates = Pair(start, end),
                mode = SelectionMode.Horizontal,
                onSelectionChange = { selection = it },
                textDelegate = textDelegate
            )

            // Assert.
            assertThat(textSelectionProcessor.startCoordinates).isEqualTo(
                PxPosition(("\u05D3\u05D4\u05D5".length * fontSizeInPx).px, fontSizeInPx.px)
            )
            assertThat(textSelectionProcessor.endCoordinates).isEqualTo(
                PxPosition(0.px, fontSizeInPx.px)
            )
            assertThat(textSelectionProcessor.startDirection).isEqualTo(TextDirection.Rtl)
            assertThat(textSelectionProcessor.endDirection).isEqualTo(TextDirection.Rtl)
            assertThat(textSelectionProcessor.containsWholeSelectionStart).isTrue()
            assertThat(textSelectionProcessor.containsWholeSelectionEnd).isTrue()
            assertThat(textSelectionProcessor.isSelected).isTrue()
            assertThat(selection).isEqualTo(
                TextRange(text.indexOf("\u05D3"), text.indexOf("\u05D5") + 1)
            )
        }
    }

    @Test
    fun testTextSelectionProcessor_drag_select_range_ltr() {
        withDensity(defaultDensity) {
            val text = "hello world\n"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value

            val textDelegate = simpleTextDelegate(
                text = text,
                fontSize = fontSize,
                density = defaultDensity
            )

            // "llo wor" is selected.
            val startOffset = text.indexOf("l")
            val endOffset = text.indexOf("r") + 1
            val start = PxPosition((fontSizeInPx * startOffset).px, (fontSizeInPx / 2).px)
            val end = PxPosition((fontSizeInPx * endOffset).px, (fontSizeInPx / 2).px)
            var selection: TextRange? = null

            // Act.
            val textSelectionProcessor = TextSelectionProcessor(
                selectionCoordinates = Pair(start, end),
                mode = SelectionMode.Horizontal,
                onSelectionChange = { selection = it },
                textDelegate = textDelegate
            )

            // Assert.
            assertThat(textSelectionProcessor.startCoordinates).isEqualTo(
                PxPosition((startOffset * fontSizeInPx).px, fontSizeInPx.px)
            )
            assertThat(textSelectionProcessor.endCoordinates).isEqualTo(
                PxPosition((endOffset * fontSizeInPx).px, fontSizeInPx.px)
            )
            assertThat(textSelectionProcessor.startDirection).isEqualTo(TextDirection.Ltr)
            assertThat(textSelectionProcessor.endDirection).isEqualTo(TextDirection.Ltr)
            assertThat(textSelectionProcessor.containsWholeSelectionStart).isTrue()
            assertThat(textSelectionProcessor.containsWholeSelectionEnd).isTrue()
            assertThat(textSelectionProcessor.isSelected).isTrue()
            assertThat(selection).isEqualTo(TextRange(startOffset, endOffset))
        }
    }

    @Test
    fun testTextSelectionProcessor_drag_select_range_rtl() {
        withDensity(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2 \u05D3\u05D4\u05D5\n"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value

            val textDelegate = simpleTextDelegate(
                text = text,
                fontSize = fontSize,
                density = defaultDensity
            )

            // "\u05D1\u05D2 \u05D3" is selected.
            val startOffset = text.indexOf("\u05D1")
            val endOffset = text.indexOf("\u05D3") + 1
            val start = PxPosition(
                (fontSizeInPx * (text.length - 1 - startOffset)).px,
                (fontSizeInPx / 2).px
            )
            val end = PxPosition(
                (fontSizeInPx * (text.length - 1 - endOffset)).px,
                (fontSizeInPx / 2).px
            )
            var selection: TextRange? = null

            // Act.
            val textSelectionProcessor = TextSelectionProcessor(
                selectionCoordinates = Pair(start, end),
                mode = SelectionMode.Horizontal,
                onSelectionChange = { selection = it },
                textDelegate = textDelegate
            )

            // Assert.
            assertThat(textSelectionProcessor.startCoordinates).isEqualTo(
                PxPosition(((text.length - 1 - startOffset) * fontSizeInPx).px, fontSizeInPx.px)
            )
            assertThat(textSelectionProcessor.endCoordinates).isEqualTo(
                PxPosition(((text.length - 1 - endOffset) * fontSizeInPx).px, fontSizeInPx.px)
            )
            assertThat(textSelectionProcessor.startDirection).isEqualTo(TextDirection.Rtl)
            assertThat(textSelectionProcessor.endDirection).isEqualTo(TextDirection.Rtl)
            assertThat(textSelectionProcessor.containsWholeSelectionStart).isTrue()
            assertThat(textSelectionProcessor.containsWholeSelectionEnd).isTrue()
            assertThat(textSelectionProcessor.isSelected).isTrue()
            assertThat(selection).isEqualTo(TextRange(startOffset, endOffset))
        }
    }

    @Test
    fun testTextSelectionProcessor_drag_select_range_bidi() {
        withDensity(defaultDensity) {
            val textLtr = "Hello"
            val textRtl = "\u05D0\u05D1\u05D2\u05D3\u05D4"
            val text = textLtr + textRtl
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value

            val textDelegate = simpleTextDelegate(
                text = text,
                fontSize = fontSize,
                density = defaultDensity
            )

            // "llo"+"\u05D0\u05D1\u05D2" is selected
            val startOffset = text.indexOf("l")
            val endOffset = text.indexOf("\u05D2") + 1
            val start = PxPosition(
                (fontSizeInPx * startOffset).px,
                (fontSizeInPx / 2).px
            )
            val end = PxPosition(
                (fontSizeInPx * (textLtr.length + text.length - endOffset)).px,
                (fontSizeInPx / 2).px
            )
            var selection: TextRange? = null

            // Act.
            val textSelectionProcessor = TextSelectionProcessor(
                selectionCoordinates = Pair(start, end),
                mode = SelectionMode.Horizontal,
                onSelectionChange = { selection = it },
                textDelegate = textDelegate
            )

            // Assert.
            assertThat(textSelectionProcessor.startCoordinates).isEqualTo(
                PxPosition((startOffset * fontSizeInPx).px, fontSizeInPx.px)
            )
            assertThat(textSelectionProcessor.endCoordinates).isEqualTo(
                PxPosition(
                    ((textLtr.length + text.length - endOffset) * fontSizeInPx).px,
                    fontSizeInPx.px
                )
            )
            assertThat(textSelectionProcessor.startDirection).isEqualTo(TextDirection.Ltr)
            assertThat(textSelectionProcessor.endDirection).isEqualTo(TextDirection.Rtl)
            assertThat(textSelectionProcessor.containsWholeSelectionStart).isTrue()
            assertThat(textSelectionProcessor.containsWholeSelectionEnd).isTrue()
            assertThat(textSelectionProcessor.isSelected).isTrue()
            assertThat(selection).isEqualTo(TextRange(startOffset, endOffset))
        }
    }

    private fun simpleTextDelegate(
        text: String = "",
        fontSize: Sp? = null,
        density: Density
    ): TextDelegate {
        val textStyle = TextStyle(fontSize = fontSize, fontFamily = fontFamily)

        val annotatedString = AnnotatedString(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length))
        )

        val textDelegate = TextDelegate(
            text = annotatedString,
            paragraphStyle = ParagraphStyle(),
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            resourceLoader = resourceLoader
        )

        textDelegate.layout(Constraints())

        return textDelegate
    }
}

class TestFontResourceLoader(val context: Context) : Font.ResourceLoader {
    override fun load(font: Font): Typeface {
        val resId = context.resources.getIdentifier(
            font.name.substringBefore("."),
            "font",
            context.packageName
        )

        return ResourcesCompat.getFont(context, resId)!!
    }
}