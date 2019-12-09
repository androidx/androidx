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
import androidx.ui.core.LayoutDirection
import androidx.ui.framework.test.R
import androidx.ui.text.AnnotatedString
import androidx.ui.text.SpanStyle
import androidx.ui.text.TextDelegate
import androidx.ui.text.TextLayoutResult
import androidx.ui.text.font.Font
import androidx.ui.text.font.font
import androidx.ui.text.font.FontStyle
import androidx.ui.text.font.FontWeight
import androidx.ui.text.font.ResourceFont
import androidx.ui.text.font.asFontFamily
import androidx.ui.text.style.TextDirection
import androidx.ui.unit.Density
import androidx.ui.unit.PxPosition
import androidx.ui.unit.TextUnit
import androidx.ui.unit.px
import androidx.ui.unit.sp
import androidx.ui.unit.withDensity
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

val BASIC_MEASURE_FONT = font(
    resId = R.font.sample_font,
    weight = FontWeight.Normal,
    style = FontStyle.Normal
)

@RunWith(JUnit4::class)
@SmallTest
class TextSelectionDelegateTest {
    private val fontFamily = BASIC_MEASURE_FONT.asFontFamily()
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val defaultDensity = Density(density = 1f)
    private val resourceLoader = TestFontResourceLoader(context)

    @Test
    fun getTextSelectionInfo_long_press_select_word_ltr() {
        withDensity(defaultDensity) {
            val text = "hello world\n"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value

            val textLayoutResult = simpleTextLayout(
                text = text,
                fontSize = fontSize,
                density = defaultDensity
            )

            val start = PxPosition((fontSizeInPx * 2).px, (fontSizeInPx / 2).px)
            val end = start

            // Act.
            val textSelectionInfo = getTextSelectionInfo(
                textLayoutResult = textLayoutResult,
                selectionCoordinates = Pair(start, end),
                layoutCoordinates = mock(),
                wordBasedSelection = true
            )

            // Assert.
            assertThat(textSelectionInfo).isNotNull()

            assertThat(textSelectionInfo?.start).isNotNull()
            textSelectionInfo?.start?.let {
                assertThat(it.coordinates).isEqualTo(
                    PxPosition(0.px, fontSizeInPx.px)
                )
                assertThat(it.direction).isEqualTo(TextDirection.Ltr)
                assertThat(it.layoutCoordinates).isNotNull()
                assertThat(it.offset).isEqualTo(0)
            }

            assertThat(textSelectionInfo?.end).isNotNull()
            textSelectionInfo?.end?.let {
                assertThat(it.coordinates).isEqualTo(
                    PxPosition(("hello".length * fontSizeInPx).px, fontSizeInPx.px)
                )
                assertThat(it.direction).isEqualTo(TextDirection.Ltr)
                assertThat(it.layoutCoordinates).isNotNull()
                assertThat(it.offset).isEqualTo("hello".length)
            }
        }
    }

    @Test
    fun getTextSelectionInfo_long_press_select_word_rtl() {
        withDensity(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2 \u05D3\u05D4\u05D5\n"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value

            val textLayoutResult = simpleTextLayout(
                text = text,
                fontSize = fontSize,
                density = defaultDensity
            )

            val start = PxPosition((fontSizeInPx * 2).px, (fontSizeInPx / 2).px)
            val end = start

            // Act.
            val textSelectionInfo = getTextSelectionInfo(
                textLayoutResult = textLayoutResult,
                selectionCoordinates = Pair(start, end),
                layoutCoordinates = mock(),
                wordBasedSelection = true
            )

            // Assert.
            assertThat(textSelectionInfo).isNotNull()

            assertThat(textSelectionInfo?.start).isNotNull()
            textSelectionInfo?.start?.let {
                assertThat(it.coordinates).isEqualTo(
                    PxPosition(("\u05D3\u05D4\u05D5".length * fontSizeInPx).px, fontSizeInPx.px)
                )
                assertThat(it.direction).isEqualTo(TextDirection.Rtl)
                assertThat(it.layoutCoordinates).isNotNull()
                assertThat(it.offset).isEqualTo(text.indexOf("\u05D3"))
            }

            assertThat(textSelectionInfo?.end).isNotNull()
            textSelectionInfo?.end?.let {
                assertThat(it.coordinates).isEqualTo(
                    PxPosition(0.px, fontSizeInPx.px)
                )
                assertThat(it.direction).isEqualTo(TextDirection.Rtl)
                assertThat(it.layoutCoordinates).isNotNull()
                assertThat(it.offset).isEqualTo(text.indexOf("\u05D5") + 1)
            }
        }
    }

    @Test
    fun getTextSelectionInfo_long_press_drag_handle_not_cross_select_word() {
        withDensity(defaultDensity) {
            val text = "hello world"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value

            val textLayoutResult = simpleTextLayout(
                text = text,
                fontSize = fontSize,
                density = defaultDensity
            )

            val rawStartOffset = text.indexOf('e')
            val rawEndOffset = text.indexOf('r')
            val start = PxPosition((fontSizeInPx * rawStartOffset).px, (fontSizeInPx / 2).px)
            val end = PxPosition((fontSizeInPx * rawEndOffset).px, (fontSizeInPx / 2).px)

            // Act.
            val textSelectionInfo = getTextSelectionInfo(
                textLayoutResult = textLayoutResult,
                selectionCoordinates = Pair(start, end),
                layoutCoordinates = mock(),
                wordBasedSelection = true
            )

            // Assert.
            assertThat(textSelectionInfo).isNotNull()

            assertThat(textSelectionInfo?.start).isNotNull()
            textSelectionInfo?.start?.let {
                assertThat(it.coordinates).isEqualTo(
                    PxPosition(0.px, fontSizeInPx.px)
                )
                assertThat(it.direction).isEqualTo(TextDirection.Ltr)
                assertThat(it.layoutCoordinates).isNotNull()
                assertThat(it.offset).isEqualTo(0)
            }

            assertThat(textSelectionInfo?.end).isNotNull()
            textSelectionInfo?.end?.let {
                assertThat(it.coordinates).isEqualTo(
                    PxPosition((text.length * fontSizeInPx).px, fontSizeInPx.px)
                )
                assertThat(it.direction).isEqualTo(TextDirection.Ltr)
                assertThat(it.layoutCoordinates).isNotNull()
                assertThat(it.offset).isEqualTo(text.length)
            }
            assertThat(textSelectionInfo?.handlesCrossed).isFalse()
        }
    }

    @Test
    fun getTextSelectionInfo_long_press_drag_handle_cross_select_word() {
        withDensity(defaultDensity) {
            val text = "hello world"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value

            val textLayoutResult = simpleTextLayout(
                text = text,
                fontSize = fontSize,
                density = defaultDensity
            )

            val rawStartOffset = text.indexOf('r')
            val rawEndOffset = text.indexOf('e')
            val start = PxPosition((fontSizeInPx * rawStartOffset).px, (fontSizeInPx / 2).px)
            val end = PxPosition((fontSizeInPx * rawEndOffset).px, (fontSizeInPx / 2).px)

            // Act.
            val textSelectionInfo = getTextSelectionInfo(
                textLayoutResult = textLayoutResult,
                selectionCoordinates = Pair(start, end),
                layoutCoordinates = mock(),
                wordBasedSelection = true
            )

            // Assert.
            assertThat(textSelectionInfo).isNotNull()

            assertThat(textSelectionInfo?.start).isNotNull()
            textSelectionInfo?.start?.let {
                assertThat(it.coordinates).isEqualTo(
                    PxPosition((text.length * fontSizeInPx).px, fontSizeInPx.px)
                )
                assertThat(it.direction).isEqualTo(TextDirection.Ltr)
                assertThat(it.layoutCoordinates).isNotNull()
                assertThat(it.offset).isEqualTo(text.length)
            }

            assertThat(textSelectionInfo?.end).isNotNull()
            textSelectionInfo?.end?.let {
                assertThat(it.coordinates).isEqualTo(
                    PxPosition(0.px, fontSizeInPx.px)
                )
                assertThat(it.direction).isEqualTo(TextDirection.Ltr)
                assertThat(it.layoutCoordinates).isNotNull()
                assertThat(it.offset).isEqualTo(0)
            }
            assertThat(textSelectionInfo?.handlesCrossed).isTrue()
        }
    }

    @Test
    fun getTextSelectionInfo_drag_select_range_ltr() {
        withDensity(defaultDensity) {
            val text = "hello world\n"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value

            val textLayoutResult = simpleTextLayout(
                text = text,
                fontSize = fontSize,
                density = defaultDensity
            )

            // "llo wor" is selected.
            val startOffset = text.indexOf("l")
            val endOffset = text.indexOf("r") + 1
            val start = PxPosition((fontSizeInPx * startOffset).px, (fontSizeInPx / 2).px)
            val end = PxPosition((fontSizeInPx * endOffset).px, (fontSizeInPx / 2).px)

            // Act.
            val textSelectionInfo = getTextSelectionInfo(
                textLayoutResult = textLayoutResult,
                selectionCoordinates = Pair(start, end),
                layoutCoordinates = mock(),
                wordBasedSelection = false
            )

            // Assert.
            assertThat(textSelectionInfo).isNotNull()

            assertThat(textSelectionInfo?.start).isNotNull()
            textSelectionInfo?.start?.let {
                assertThat(it.coordinates).isEqualTo(
                    PxPosition((startOffset * fontSizeInPx).px, fontSizeInPx.px)
                )
                assertThat(it.direction).isEqualTo(TextDirection.Ltr)
                assertThat(it.layoutCoordinates).isNotNull()
                assertThat(it.offset).isEqualTo(startOffset)
            }

            assertThat(textSelectionInfo?.end).isNotNull()
            textSelectionInfo?.end?.let {
                assertThat(it.coordinates).isEqualTo(
                    PxPosition((endOffset * fontSizeInPx).px, fontSizeInPx.px)
                )
                assertThat(it.direction).isEqualTo(TextDirection.Ltr)
                assertThat(it.layoutCoordinates).isNotNull()
                assertThat(it.offset).isEqualTo(endOffset)
            }
        }
    }

    @Test
    fun getTextSelectionInfo_drag_select_range_rtl() {
        withDensity(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2 \u05D3\u05D4\u05D5\n"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value

            val textLayoutResult = simpleTextLayout(
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

            // Act.
            val textSelectionInfo = getTextSelectionInfo(
                textLayoutResult = textLayoutResult,
                selectionCoordinates = Pair(start, end),
                layoutCoordinates = mock(),
                wordBasedSelection = false
            )

            // Assert.
            assertThat(textSelectionInfo).isNotNull()

            assertThat(textSelectionInfo?.start).isNotNull()
            textSelectionInfo?.start?.let {
                assertThat(it.coordinates).isEqualTo(
                    PxPosition(
                        ((text.length - 1 - startOffset) * fontSizeInPx).px,
                        fontSizeInPx.px
                    )
                )
                assertThat(it.direction).isEqualTo(TextDirection.Rtl)
                assertThat(it.layoutCoordinates).isNotNull()
                assertThat(it.offset).isEqualTo(startOffset)
            }

            assertThat(textSelectionInfo?.end).isNotNull()
            textSelectionInfo?.end?.let {
                assertThat(it.coordinates).isEqualTo(
                    PxPosition(((text.length - 1 - endOffset) * fontSizeInPx).px, fontSizeInPx.px)
                )
                assertThat(it.direction).isEqualTo(TextDirection.Rtl)
                assertThat(it.layoutCoordinates).isNotNull()
                assertThat(it.offset).isEqualTo(endOffset)
            }
        }
    }

    @Test
    fun getTextSelectionInfo_drag_select_range_bidi() {
        withDensity(defaultDensity) {
            val textLtr = "Hello"
            val textRtl = "\u05D0\u05D1\u05D2\u05D3\u05D4"
            val text = textLtr + textRtl
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value

            val textLayoutResult = simpleTextLayout(
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

            // Act.
            val textSelectionInfo = getTextSelectionInfo(
                textLayoutResult = textLayoutResult,
                selectionCoordinates = Pair(start, end),
                layoutCoordinates = mock(),
                wordBasedSelection = false
            )

            // Assert.
            assertThat(textSelectionInfo).isNotNull()

            assertThat(textSelectionInfo?.start).isNotNull()
            textSelectionInfo?.start?.let {
                assertThat(it.coordinates).isEqualTo(
                    PxPosition((startOffset * fontSizeInPx).px, fontSizeInPx.px)
                )
                assertThat(it.direction).isEqualTo(TextDirection.Ltr)
                assertThat(it.layoutCoordinates).isNotNull()
                assertThat(it.offset).isEqualTo(startOffset)
            }

            assertThat(textSelectionInfo?.end).isNotNull()
            textSelectionInfo?.end?.let {
                assertThat(it.coordinates).isEqualTo(
                    PxPosition(
                        ((textLtr.length + text.length - endOffset) * fontSizeInPx).px,
                        fontSizeInPx.px
                    )
                )
                assertThat(it.direction).isEqualTo(TextDirection.Rtl)
                assertThat(it.layoutCoordinates).isNotNull()
                assertThat(it.offset).isEqualTo(endOffset)
            }
        }
    }

    @Test
    fun testTextSelectionProcessor_single_widget_handles_crossed_ltr() {
        withDensity(defaultDensity) {
            val text = "hello world\n"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value
            val textLayoutResult = simpleTextLayout(
                text = text,
                fontSize = fontSize,
                density = defaultDensity
            )
            // "llo wor" is selected.
            val startOffset = text.indexOf("r") + 1
            val endOffset = text.indexOf("l")
            val start = PxPosition((fontSizeInPx * startOffset).px, (fontSizeInPx / 2).px)
            val end = PxPosition((fontSizeInPx * endOffset).px, (fontSizeInPx / 2).px)
            // Act.
            val textSelectionInfo = getTextSelectionInfo(
                selectionCoordinates = Pair(start, end),
                textLayoutResult = textLayoutResult,
                layoutCoordinates = mock(),
                wordBasedSelection = false
            )
            // Assert.
            assertThat(textSelectionInfo).isNotNull()

            assertThat(textSelectionInfo?.start).isNotNull()
            textSelectionInfo?.start?.let {
                assertThat(it.coordinates).isEqualTo(
                    PxPosition((startOffset * fontSizeInPx).px, fontSizeInPx.px)
                )
                assertThat(it.direction).isEqualTo(TextDirection.Ltr)
                assertThat(it.layoutCoordinates).isNotNull()
                assertThat(it.offset).isEqualTo(startOffset)
            }

            assertThat(textSelectionInfo?.end).isNotNull()
            textSelectionInfo?.end?.let {
                assertThat(it.coordinates).isEqualTo(
                    PxPosition((endOffset * fontSizeInPx).px, fontSizeInPx.px)
                )
                assertThat(it.direction).isEqualTo(TextDirection.Ltr)
                assertThat(it.layoutCoordinates).isNotNull()
                assertThat(it.offset).isEqualTo(endOffset)
            }
            assertThat(textSelectionInfo?.handlesCrossed).isTrue()
        }
    }

    @Test
    fun testTextSelectionProcessor_single_widget_handles_crossed_rtl() {
        withDensity(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2 \u05D3\u05D4\u05D5\n"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value
            val textLayoutResult = simpleTextLayout(
                text = text,
                fontSize = fontSize,
                density = defaultDensity
            )
            // "\u05D1\u05D2 \u05D3" is selected.
            val startOffset = text.indexOf("\u05D3") + 1
            val endOffset = text.indexOf("\u05D1")
            val start = PxPosition(
                (fontSizeInPx * (text.length - 1 - startOffset)).px,
                (fontSizeInPx / 2).px
            )
            val end = PxPosition(
                (fontSizeInPx * (text.length - 1 - endOffset)).px,
                (fontSizeInPx / 2).px
            )

            // Act.
            val textSelectionInfo = getTextSelectionInfo(
                selectionCoordinates = Pair(start, end),
                textLayoutResult = textLayoutResult,
                layoutCoordinates = mock(),
                wordBasedSelection = false
            )
            // Assert.
            assertThat(textSelectionInfo).isNotNull()

            assertThat(textSelectionInfo?.start).isNotNull()
            textSelectionInfo?.start?.let {
                assertThat(it.coordinates).isEqualTo(
                    PxPosition(((text.length - 1 - startOffset) * fontSizeInPx).px, fontSizeInPx.px)
                )
                assertThat(it.direction).isEqualTo(TextDirection.Rtl)
                assertThat(it.layoutCoordinates).isNotNull()
                assertThat(it.offset).isEqualTo(startOffset)
            }

            assertThat(textSelectionInfo?.end).isNotNull()
            textSelectionInfo?.end?.let {
                assertThat(it.coordinates).isEqualTo(
                    PxPosition(((text.length - 1 - endOffset) * fontSizeInPx).px, fontSizeInPx.px)
                )
                assertThat(it.direction).isEqualTo(TextDirection.Rtl)
                assertThat(it.layoutCoordinates).isNotNull()
                assertThat(it.offset).isEqualTo(endOffset)
            }
            assertThat(textSelectionInfo?.handlesCrossed).isTrue()
        }
    }

    @Test
    fun testTextSelectionProcessor_single_widget_handles_crossed_bidi() {
        withDensity(defaultDensity) {
            val textLtr = "Hello"
            val textRtl = "\u05D0\u05D1\u05D2\u05D3\u05D4"
            val text = textLtr + textRtl
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value
            val textLayoutResult = simpleTextLayout(
                text = text,
                fontSize = fontSize,
                density = defaultDensity
            )
            // "llo"+"\u05D0\u05D1\u05D2" is selected
            val startOffset = text.indexOf("\u05D2") + 1
            val endOffset = text.indexOf("l")
            val start = PxPosition(
                (fontSizeInPx * (textLtr.length + text.length - startOffset)).px,
                (fontSizeInPx / 2).px
            )
            val end = PxPosition(
                (fontSizeInPx * endOffset).px,
                (fontSizeInPx / 2).px
            )

            // Act.
            val textSelectionInfo = getTextSelectionInfo(
                selectionCoordinates = Pair(start, end),
                textLayoutResult = textLayoutResult,
                layoutCoordinates = mock(),
                wordBasedSelection = false
            )
            // Assert.
            assertThat(textSelectionInfo).isNotNull()

            assertThat(textSelectionInfo?.start).isNotNull()
            textSelectionInfo?.start?.let {
                assertThat(it.coordinates).isEqualTo(
                    PxPosition(
                        ((textLtr.length + text.length - startOffset) * fontSizeInPx).px,
                        fontSizeInPx.px
                    )
                )
                assertThat(it.direction).isEqualTo(TextDirection.Rtl)
                assertThat(it.layoutCoordinates).isNotNull()
                assertThat(it.offset).isEqualTo(startOffset)
            }

            assertThat(textSelectionInfo?.end).isNotNull()
            textSelectionInfo?.end?.let {
                assertThat(it.coordinates).isEqualTo(
                    PxPosition((endOffset * fontSizeInPx).px, fontSizeInPx.px)
                )
                assertThat(it.direction).isEqualTo(TextDirection.Ltr)
                assertThat(it.layoutCoordinates).isNotNull()
                assertThat(it.offset).isEqualTo(endOffset)
            }
            assertThat(textSelectionInfo?.handlesCrossed).isTrue()
        }
    }

    @Test
    fun testTextSelectionProcessor_bound_to_one_character_ltr() {
        withDensity(defaultDensity) {
            val text = "hello world\n"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value
            val textLayoutResult = simpleTextLayout(
                text = text,
                fontSize = fontSize,
                density = defaultDensity
            )
            // "l" is selected.
            val startOffset = text.indexOf("l")
            val start = PxPosition((fontSizeInPx * startOffset).px, (fontSizeInPx / 2).px)
            val end = PxPosition((fontSizeInPx * startOffset).px, (fontSizeInPx / 2).px)

            // Act.
            val textSelectionInfo = getTextSelectionInfo(
                selectionCoordinates = Pair(start, end),
                textLayoutResult = textLayoutResult,
                layoutCoordinates = mock(),
                wordBasedSelection = false
            )
            // Assert.
            assertThat(textSelectionInfo).isNotNull()

            assertThat(textSelectionInfo?.start).isNotNull()
            textSelectionInfo?.start?.let {
                assertThat(it.coordinates).isEqualTo(
                    PxPosition((startOffset * fontSizeInPx).px, fontSizeInPx.px)
                )
                assertThat(it.direction).isEqualTo(TextDirection.Ltr)
                assertThat(it.layoutCoordinates).isNotNull()
                assertThat(it.offset).isEqualTo(startOffset)
            }

            assertThat(textSelectionInfo?.end).isNotNull()
            textSelectionInfo?.end?.let {
                assertThat(it.coordinates).isEqualTo(
                    PxPosition(((startOffset + 1) * fontSizeInPx).px, fontSizeInPx.px)
                )
                assertThat(it.direction).isEqualTo(TextDirection.Ltr)
                assertThat(it.layoutCoordinates).isNotNull()
                assertThat(it.offset).isEqualTo(startOffset + 1)
            }
        }
    }

    @Test
    fun testTextSelectionProcessor_bound_to_one_character_rtl() {
        withDensity(defaultDensity) {
            val text = "\u05D0\u05D1\u05D2 \u05D3\u05D4\u05D5\n"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value
            val textLayoutResult = simpleTextLayout(
                text = text,
                fontSize = fontSize,
                density = defaultDensity
            )
            // "\u05D1" is selected.
            val startOffset = text.indexOf("\u05D1")
            val start = PxPosition(
                (fontSizeInPx * (text.length - 1 - startOffset)).px,
                (fontSizeInPx / 2).px
            )
            val end = PxPosition(
                (fontSizeInPx * (text.length - 1 - startOffset)).px,
                (fontSizeInPx / 2).px
            )

            // Act.
            val textSelectionInfo = getTextSelectionInfo(
                selectionCoordinates = Pair(start, end),
                textLayoutResult = textLayoutResult,
                layoutCoordinates = mock(),
                wordBasedSelection = false
            )
            // Assert.
            assertThat(textSelectionInfo).isNotNull()

            assertThat(textSelectionInfo?.start).isNotNull()
            textSelectionInfo?.start?.let {
                assertThat(it.coordinates).isEqualTo(
                    PxPosition(((text.length - 1 - startOffset) * fontSizeInPx).px, fontSizeInPx.px)
                )
                assertThat(it.direction).isEqualTo(TextDirection.Rtl)
                assertThat(it.layoutCoordinates).isNotNull()
                assertThat(it.offset).isEqualTo(startOffset)
            }

            assertThat(textSelectionInfo?.end).isNotNull()
            textSelectionInfo?.end?.let {
                assertThat(it.coordinates).isEqualTo(
                    PxPosition(
                        ((text.length - 1 - (startOffset + 1)) * fontSizeInPx).px,
                        fontSizeInPx.px
                    )
                )
                assertThat(it.direction).isEqualTo(TextDirection.Rtl)
                assertThat(it.layoutCoordinates).isNotNull()
                assertThat(it.offset).isEqualTo(startOffset + 1)
            }
        }
    }

    @Test
    fun testTextSelectionProcessor_cross_widget_not_contain_start() {
        withDensity(defaultDensity) {
            val text = "hello world\n"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value
            val textLayoutResult = simpleTextLayout(
                text = text,
                fontSize = fontSize,
                density = defaultDensity
            )
            // "hello w" is selected.
            val endOffset = text.indexOf("w") + 1
            val start = PxPosition(-50.px, -50.px)
            val end = PxPosition((fontSizeInPx * endOffset).px, (fontSizeInPx / 2).px)

            // Act.
            val textSelectionInfo = getTextSelectionInfo(
                selectionCoordinates = Pair(start, end),
                textLayoutResult = textLayoutResult,
                layoutCoordinates = mock(),
                wordBasedSelection = false
            )
            // Assert.
            assertThat(textSelectionInfo).isNotNull()

            assertThat(textSelectionInfo?.start).isNotNull()
            textSelectionInfo?.start?.let {
                assertThat(it.coordinates).isEqualTo(
                    PxPosition(0.px, fontSizeInPx.px)
                )
                assertThat(it.direction).isEqualTo(TextDirection.Ltr)
                assertThat(it.layoutCoordinates).isNull()
                assertThat(it.offset).isEqualTo(0)
            }

            assertThat(textSelectionInfo?.end).isNotNull()
            textSelectionInfo?.end?.let {
                assertThat(it.coordinates).isEqualTo(
                    PxPosition(((endOffset) * fontSizeInPx).px, fontSizeInPx.px)
                )
                assertThat(it.direction).isEqualTo(TextDirection.Ltr)
                assertThat(it.layoutCoordinates).isNotNull()
                assertThat(it.offset).isEqualTo(endOffset)
            }
        }
    }

    @Test
    fun testTextSelectionProcessor_cross_widget_not_contain_end() {
        withDensity(defaultDensity) {
            val text = "hello world"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value
            val textLayoutResult = simpleTextLayout(
                text = text,
                fontSize = fontSize,
                density = defaultDensity
            )
            // "o world" is selected.
            val startOffset = text.indexOf("o")
            val start = PxPosition((fontSizeInPx * startOffset).px, (fontSizeInPx / 2).px)
            val end = PxPosition((fontSizeInPx * text.length * 2).px, (fontSizeInPx * 2)
                .px)

            // Act.
            val textSelectionInfo = getTextSelectionInfo(
                selectionCoordinates = Pair(start, end),
                textLayoutResult = textLayoutResult,
                layoutCoordinates = mock(),
                wordBasedSelection = false
            )
            // Assert.
            assertThat(textSelectionInfo).isNotNull()

            assertThat(textSelectionInfo?.start).isNotNull()
            textSelectionInfo?.start?.let {
                assertThat(it.coordinates).isEqualTo(
                    PxPosition((fontSizeInPx * startOffset).px, fontSizeInPx.px)
                )
                assertThat(it.direction).isEqualTo(TextDirection.Ltr)
                assertThat(it.layoutCoordinates).isNotNull()
                assertThat(it.offset).isEqualTo(startOffset)
            }

            assertThat(textSelectionInfo?.end).isNotNull()
            textSelectionInfo?.end?.let {
                assertThat(it.coordinates).isEqualTo(
                    PxPosition((fontSizeInPx * (text.length)).px, fontSizeInPx.px)
                )
                assertThat(it.direction).isEqualTo(TextDirection.Ltr)
                assertThat(it.layoutCoordinates).isNull()
                assertThat(it.offset).isEqualTo(text.length)
            }
        }
    }

    @Test
    fun testTextSelectionProcessor_cross_widget_not_contain_start_handles_crossed() {
        withDensity(defaultDensity) {
            val text = "hello world"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value
            val textLayoutResult = simpleTextLayout(
                text = text,
                fontSize = fontSize,
                density = defaultDensity
            )
            // "world" is selected.
            val endOffset = text.indexOf("w")
            val start =
                PxPosition((fontSizeInPx * text.length * 2).px, (fontSizeInPx * 2).px)
            val end = PxPosition((fontSizeInPx * endOffset).px, (fontSizeInPx / 2).px)

            // Act.
            val textSelectionInfo = getTextSelectionInfo(
                selectionCoordinates = Pair(start, end),
                textLayoutResult = textLayoutResult,
                layoutCoordinates = mock(),
                wordBasedSelection = false
            )
            // Assert.
            assertThat(textSelectionInfo).isNotNull()

            assertThat(textSelectionInfo?.start).isNotNull()
            textSelectionInfo?.start?.let {
                assertThat(it.coordinates).isEqualTo(
                    PxPosition(((text.length) * fontSizeInPx).px, fontSizeInPx.px)
                )
                assertThat(it.direction).isEqualTo(TextDirection.Ltr)
                assertThat(it.layoutCoordinates).isNull()
                assertThat(it.offset).isEqualTo(text.length)
            }

            assertThat(textSelectionInfo?.end).isNotNull()
            textSelectionInfo?.end?.let {
                assertThat(it.coordinates).isEqualTo(
                    PxPosition(((endOffset) * fontSizeInPx).px, fontSizeInPx.px)
                )
                assertThat(it.direction).isEqualTo(TextDirection.Ltr)
                assertThat(it.layoutCoordinates).isNotNull()
                assertThat(it.offset).isEqualTo(endOffset)
            }
            assertThat(textSelectionInfo?.handlesCrossed).isTrue()
        }
    }

    @Test
    fun testTextSelectionProcessor_cross_widget_not_contain_end_handles_crossed() {
        withDensity(defaultDensity) {
            val text = "hello world"
            val fontSize = 20.sp
            val fontSizeInPx = fontSize.toPx().value
            val textLayoutResult = simpleTextLayout(
                text = text,
                fontSize = fontSize,
                density = defaultDensity
            )
            // "hell" is selected.
            val startOffset = text.indexOf("o")
            val start =
                PxPosition((fontSizeInPx * startOffset).px, (fontSizeInPx / 2).px)
            val end = PxPosition(-50.px, -50.px)

            // Act.
            val textSelectionInfo = getTextSelectionInfo(
                selectionCoordinates = Pair(start, end),
                textLayoutResult = textLayoutResult,
                layoutCoordinates = mock(),
                wordBasedSelection = false
            )
            // Assert.
            assertThat(textSelectionInfo).isNotNull()

            assertThat(textSelectionInfo?.start).isNotNull()
            textSelectionInfo?.start?.let {
                assertThat(it.coordinates).isEqualTo(
                    PxPosition((startOffset * fontSizeInPx).px, fontSizeInPx.px)
                )
                assertThat(it.direction).isEqualTo(TextDirection.Ltr)
                assertThat(it.layoutCoordinates).isNotNull()
                assertThat(it.offset).isEqualTo(startOffset)
            }

            assertThat(textSelectionInfo?.end).isNotNull()
            textSelectionInfo?.end?.let {
                assertThat(it.coordinates).isEqualTo(
                    PxPosition(0.px, fontSizeInPx.px)
                )
                assertThat(it.direction).isEqualTo(TextDirection.Ltr)
                assertThat(it.layoutCoordinates).isNull()
                assertThat(it.offset).isEqualTo(0)
            }
            assertThat(textSelectionInfo?.handlesCrossed).isTrue()
        }
    }

    @Test
    fun testTextSelectionProcessor_not_selected() {
        withDensity(defaultDensity) {
            val text = "hello world\n"
            val fontSize = 20.sp
            val textLayoutResult = simpleTextLayout(
                text = text,
                fontSize = fontSize,
                density = defaultDensity
            )
            val start = PxPosition(-50.px, -50.px)
            val end = PxPosition(-20.px, -20.px)
            // Act.
            val textSelectionInfo = getTextSelectionInfo(
                selectionCoordinates = Pair(start, end),
                textLayoutResult = textLayoutResult,
                layoutCoordinates = mock(),
                wordBasedSelection = true
            )
            assertThat(textSelectionInfo).isNull()
        }
    }

    private fun simpleTextLayout(
        text: String = "",
        fontSize: TextUnit = TextUnit.Inherit,
        density: Density
    ): TextLayoutResult {
        val spanStyle = SpanStyle(fontSize = fontSize, fontFamily = fontFamily)
        val annotatedString = AnnotatedString(text, spanStyle)
        return TextDelegate(
            text = annotatedString,
            density = density,
            layoutDirection = LayoutDirection.Ltr,
            resourceLoader = resourceLoader
        ).layout(Constraints())
    }
}

class TestFontResourceLoader(val context: Context) : Font.ResourceLoader {
    override fun load(font: Font): Typeface {
        return when (font) {
            is ResourceFont -> ResourcesCompat.getFont(context, font.resId)!!
            else -> throw IllegalArgumentException("Unknown font type: $font")
        }
    }
}
