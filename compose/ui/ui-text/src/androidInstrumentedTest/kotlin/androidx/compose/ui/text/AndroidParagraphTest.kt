/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.text

import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.CharacterStyle
import android.text.style.ForegroundColorSpan
import android.text.style.LeadingMarginSpan
import android.text.style.LocaleSpan
import android.text.style.RelativeSizeSpan
import android.text.style.ScaleXSpan
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.FontTestData.Companion.BASIC_MEASURE_FONT
import androidx.compose.ui.text.android.InternalPlatformTextApi
import androidx.compose.ui.text.android.TextLayout
import androidx.compose.ui.text.android.style.BaselineShiftSpan
import androidx.compose.ui.text.android.style.FontFeatureSpan
import androidx.compose.ui.text.android.style.LetterSpacingSpanEm
import androidx.compose.ui.text.android.style.LetterSpacingSpanPx
import androidx.compose.ui.text.android.style.ShadowSpan
import androidx.compose.ui.text.android.style.SkewXSpan
import androidx.compose.ui.text.android.style.TextDecorationSpan
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.testutils.AsyncTestTypefaceLoader
import androidx.compose.ui.text.font.testutils.BlockingFauxFont
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.matchers.assertThat
import androidx.compose.ui.text.platform.bitmap
import androidx.compose.ui.text.platform.style.DrawStyleSpan
import androidx.compose.ui.text.platform.style.ShaderBrushSpan
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlin.math.ceil
import kotlin.math.roundToInt
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(InternalPlatformTextApi::class)
@RunWith(AndroidJUnit4::class)
@SmallTest
class AndroidParagraphTest {
    // This sample font provides the following features:
    // 1. The width of most of visible characters equals to font size.
    // 2. The LTR/RTL characters are rendered as ▶/◀.
    // 3. The fontMetrics passed to TextPaint has descend - ascend equal to 1.2 * fontSize.
    private val basicFontFamily = BASIC_MEASURE_FONT.toFontFamily()
    private val defaultDensity = Density(density = 1f)
    private val context = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun draw_with_newline_and_line_break_default_values() {
        with(defaultDensity) {
            val fontSize = 50.sp
            for (text in arrayOf("abc\ndef", "\u05D0\u05D1\u05D2\n\u05D3\u05D4\u05D5")) {
                val paragraphAndroid =
                    simpleParagraph(
                        text = text,
                        style = TextStyle(fontSize = fontSize, fontFamily = basicFontFamily),
                        // 2 chars width
                        width = 2 * fontSize.toPx()
                    )

                val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
                textPaint.textSize = fontSize.toPx()
                textPaint.typeface =
                    UncachedFontFamilyResolver(context)
                        .resolve(BASIC_MEASURE_FONT.toFontFamily())
                        .value as Typeface

                val layout =
                    TextLayout(
                        charSequence = text,
                        width = ceil(paragraphAndroid.width),
                        textPaint = textPaint
                    )

                assertThat(paragraphAndroid.bitmap()).isEqualToBitmap(layout.bitmap())
            }
        }
    }

    @Test
    fun testAnnotatedString_setColorOnWholeText() {
        val text = "abcde"
        val spanStyle = SpanStyle(color = Color(0xFF0000FF))

        val paragraph =
            simpleParagraph(
                text = text,
                spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, text.length)),
                width = 100.0f
            )

        assertThat(paragraph.charSequence).hasSpan(ForegroundColorSpan::class, 0, text.length)
    }

    @Test
    fun testAnnotatedString_setColorOnPartOfText() {
        val text = "abcde"
        val spanStyle = SpanStyle(color = Color(0xFF0000FF))

        val paragraph =
            simpleParagraph(
                text = text,
                spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, "abc".length)),
                width = 100.0f
            )

        assertThat(paragraph.charSequence).hasSpan(ForegroundColorSpan::class, 0, "abc".length)
    }

    @Test
    fun testAnnotatedString_setColorTwice_lastOneOverwrite() {
        val text = "abcde"
        val spanStyle = SpanStyle(color = Color(0xFF0000FF))
        val spanStyleOverwrite = SpanStyle(color = Color(0xFF00FF00))

        val paragraph =
            simpleParagraph(
                text = text,
                spanStyles =
                    listOf(
                        AnnotatedString.Range(spanStyle, 0, text.length),
                        AnnotatedString.Range(spanStyleOverwrite, 0, "abc".length)
                    ),
                width = 100.0f
            )

        assertThat(paragraph.charSequence).hasSpan(ForegroundColorSpan::class, 0, text.length)
        assertThat(paragraph.charSequence).hasSpan(ForegroundColorSpan::class, 0, "abc".length)
        assertThat(paragraph.charSequence).hasSpanOnTop(ForegroundColorSpan::class, 0, "abc".length)
    }

    @Test
    fun testAnnotatedString_setBrushOnWholeText() {
        val text = "abcde"
        val brush = Brush.linearGradient(listOf(Color.Black, Color.White))
        val spanStyle = SpanStyle(brush = brush)

        val paragraph =
            simpleParagraph(
                text = text,
                spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, text.length)),
                width = 100.0f
            )

        assertThat(paragraph.charSequence).hasSpan(ShaderBrushSpan::class, 0, text.length) {
            it.shaderBrush == brush
        }
    }

    @Test
    fun testAnnotatedString_setSolidColorBrushOnWholeText() {
        val text = "abcde"
        val brush = SolidColor(Color.Red)
        val spanStyle = SpanStyle(brush = brush)

        val paragraph =
            simpleParagraph(
                text = text,
                spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, text.length)),
                width = 100.0f
            )

        assertThat(paragraph.charSequence).hasSpan(ForegroundColorSpan::class, 0, text.length)
    }

    @Test
    fun testAnnotatedString_setBrushOnPartOfText() {
        val text = "abcde"
        val brush = Brush.linearGradient(listOf(Color.Black, Color.White))
        val spanStyle = SpanStyle(brush = brush)

        val paragraph =
            simpleParagraph(
                text = text,
                spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, "abc".length)),
                width = 100.0f
            )

        assertThat(paragraph.charSequence).hasSpan(ShaderBrushSpan::class, 0, "abc".length) {
            it.shaderBrush == brush
        }
    }

    @Test
    fun testAnnotatedString_brushSpanReceivesSize() {
        with(defaultDensity) {
            val text = "abcde"
            val brush = Brush.linearGradient(listOf(Color.Black, Color.White))
            val spanStyle = SpanStyle(brush = brush)
            val fontSize = 10.sp

            val paragraph =
                simpleParagraph(
                    text = text,
                    spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, "abc".length)),
                    width = 100.0f,
                    style = TextStyle(fontSize = fontSize, fontFamily = basicFontFamily)
                )

            assertThat(paragraph.charSequence).hasSpan(ShaderBrushSpan::class, 0, "abc".length) {
                it.size == Size(100.0f, fontSize.toPx())
            }
        }
    }

    @Test
    fun testStyle_setTextDecorationOnWholeText_withLineThrough() {
        val text = "abcde"
        val spanStyle = SpanStyle(textDecoration = TextDecoration.LineThrough)

        val paragraph =
            simpleParagraph(
                text = text,
                spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, text.length)),
                width = 100.0f
            )

        assertThat(paragraph.charSequence.toString()).isEqualTo(text)
        assertThat(paragraph.charSequence).hasSpan(
            spanClazz = TextDecorationSpan::class,
            start = 0,
            end = text.length
        ) {
            !it.isUnderlineText && it.isStrikethroughText
        }
    }

    @Test
    fun testStyle_setTextDecorationOnWholeText_withUnderline() {
        val text = "abcde"
        val spanStyle = SpanStyle(textDecoration = TextDecoration.Underline)

        val paragraph =
            simpleParagraph(
                text = text,
                spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, text.length)),
                width = 100.0f
            )

        assertThat(paragraph.charSequence.toString()).isEqualTo(text)
        assertThat(paragraph.charSequence).hasSpan(
            spanClazz = TextDecorationSpan::class,
            start = 0,
            end = text.length
        ) {
            it.isUnderlineText && !it.isStrikethroughText
        }
    }

    @Test
    fun testStyle_setTextDecorationOnPartText_withLineThrough() {
        val text = "abcde"
        val spanStyle = SpanStyle(textDecoration = TextDecoration.LineThrough)

        val paragraph =
            simpleParagraph(
                text = text,
                spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, "abc".length)),
                width = 100.0f
            )

        assertThat(paragraph.charSequence.toString()).isEqualTo(text)
        assertThat(paragraph.charSequence).hasSpan(
            spanClazz = TextDecorationSpan::class,
            start = 0,
            end = "abc".length
        ) {
            !it.isUnderlineText && it.isStrikethroughText
        }
    }

    @Test
    fun testStyle_setTextDecorationOnPartText_withUnderline() {
        val text = "abcde"
        val spanStyle = SpanStyle(textDecoration = TextDecoration.Underline)

        val paragraph =
            simpleParagraph(
                text = text,
                spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, "abc".length)),
                width = 100.0f
            )

        assertThat(paragraph.charSequence.toString()).isEqualTo(text)
        assertThat(paragraph.charSequence).hasSpan(
            spanClazz = TextDecorationSpan::class,
            start = 0,
            end = "abc".length
        ) {
            it.isUnderlineText && !it.isStrikethroughText
        }
    }

    @Test
    fun testStyle_setTextDecoration_withLineThroughAndUnderline() {
        val text = "abcde"
        val spanStyle =
            SpanStyle(textDecoration = TextDecoration.LineThrough + TextDecoration.Underline)

        val paragraph =
            simpleParagraph(
                text = text,
                spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, "abc".length)),
                width = 100.0f
            )

        assertThat(paragraph.charSequence.toString()).isEqualTo(text)
        assertThat(paragraph.charSequence).hasSpan(
            spanClazz = TextDecorationSpan::class,
            start = 0,
            end = "abc".length
        ) {
            it.isUnderlineText && it.isStrikethroughText
        }
    }

    @Test
    fun testAnnotatedString_setFontSizeOnWholeText() {
        with(defaultDensity) {
            val text = "abcde"
            val fontSize = 20.sp
            val paragraphWidth = text.length * fontSize.toPx()
            val spanStyle = SpanStyle(fontSize = fontSize)

            val paragraph =
                simpleParagraph(
                    text = text,
                    spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, text.length)),
                    width = paragraphWidth
                )

            assertThat(paragraph.charSequence).hasSpan(AbsoluteSizeSpan::class, 0, text.length) {
                it.size == fontSize.toPx().roundToInt() && !it.dip
            }
        }
    }

    @Test
    fun testAnnotatedString_setFontSizeOnPartText() {
        with(defaultDensity) {
            val text = "abcde"
            val fontSize = 20.sp
            val paragraphWidth = text.length * fontSize.toPx()
            val spanStyle = SpanStyle(fontSize = fontSize)

            val paragraph =
                simpleParagraph(
                    text = text,
                    spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, "abc".length)),
                    width = paragraphWidth
                )

            assertThat(paragraph.charSequence).hasSpan(AbsoluteSizeSpan::class, 0, "abc".length)
        }
    }

    @Test
    fun testAnnotatedString_setFontSizeTwice_lastOneOverwrite() {
        with(defaultDensity) {
            val text = "abcde"
            val fontSize = 20.sp
            val fontSizeOverwrite = 30.sp
            val paragraphWidth = text.length * fontSizeOverwrite.toPx()
            val spanStyle = SpanStyle(fontSize = fontSize)
            val spanStyleOverwrite = SpanStyle(fontSize = fontSizeOverwrite)

            val paragraph =
                simpleParagraph(
                    text = text,
                    spanStyles =
                        listOf(
                            AnnotatedString.Range(spanStyle, 0, text.length),
                            AnnotatedString.Range(spanStyleOverwrite, 0, "abc".length)
                        ),
                    width = paragraphWidth
                )

            assertThat(paragraph.charSequence).hasSpan(AbsoluteSizeSpan::class, 0, text.length)
            assertThat(paragraph.charSequence).hasSpan(AbsoluteSizeSpan::class, 0, "abc".length)
            assertThat(paragraph.charSequence)
                .hasSpanOnTop(AbsoluteSizeSpan::class, 0, "abc".length)
        }
    }

    @Test
    fun testAnnotatedString_setFontSizeScaleOnWholeText() {
        val text = "abcde"
        val fontSizeScale = 2.0.em
        val spanStyle = SpanStyle(fontSize = fontSizeScale)

        val paragraph =
            simpleParagraph(
                text = text,
                spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, text.length)),
                width = 100.0f
            )

        assertThat(paragraph.charSequence).hasSpan(RelativeSizeSpan::class, 0, text.length) {
            it.sizeChange == fontSizeScale.value
        }
    }

    @Test
    fun testAnnotatedString_setFontSizeScaleOnPartText() {
        val text = "abcde"
        val fontSizeScale = 2.0f.em
        val spanStyle = SpanStyle(fontSize = fontSizeScale)

        val paragraph =
            simpleParagraph(
                text = text,
                spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, "abc".length)),
                width = 100.0f
            )

        assertThat(paragraph.charSequence).hasSpan(RelativeSizeSpan::class, 0, "abc".length) {
            it.sizeChange == fontSizeScale.value
        }
    }

    @Test
    fun testAnnotatedString_setLetterSpacingOnWholeText() {
        val text = "abcde"
        val letterSpacing = 2.0f
        val spanStyle = SpanStyle(letterSpacing = letterSpacing.em)

        val paragraph =
            simpleParagraph(
                text = text,
                spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, text.length)),
                width = 100.0f
            )

        assertThat(paragraph.charSequence.toString()).isEqualTo(text)
        assertThat(paragraph.charSequence).hasSpan(LetterSpacingSpanEm::class, 0, text.length)
    }

    @Test
    fun testAnnotatedString_setLetterSpacingOnPartText() {
        val text = "abcde"
        val spanStyle = SpanStyle(letterSpacing = 2.em)

        val paragraph =
            simpleParagraph(
                text = text,
                spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, "abc".length)),
                width = 100.0f
            )

        assertThat(paragraph.charSequence.toString()).isEqualTo(text)
        assertThat(paragraph.charSequence).hasSpan(LetterSpacingSpanEm::class, 0, "abc".length)
    }

    @Test
    fun testAnnotatedString_setLetterSpacingTwice_lastOneOverwrite() {
        val text = "abcde"
        val spanStyle = SpanStyle(letterSpacing = 2.em)
        val spanStyleOverwrite = SpanStyle(letterSpacing = 3.em)

        val paragraph =
            simpleParagraph(
                text = text,
                spanStyles =
                    listOf(
                        AnnotatedString.Range(spanStyle, 0, text.length),
                        AnnotatedString.Range(spanStyleOverwrite, 0, "abc".length)
                    ),
                width = 100.0f
            )

        assertThat(paragraph.charSequence.toString()).isEqualTo(text)
        assertThat(paragraph.charSequence).hasSpan(LetterSpacingSpanEm::class, 0, text.length)
        assertThat(paragraph.charSequence).hasSpan(LetterSpacingSpanEm::class, 0, "abc".length)
        assertThat(paragraph.charSequence).hasSpanOnTop(LetterSpacingSpanEm::class, 0, "abc".length)
    }

    @Test
    fun testAnnotatedString_setBackgroundOnWholeText() {
        val text = "abcde"
        val color = Color(0xFF0000FF)
        val spanStyle = SpanStyle(background = color)

        val paragraph =
            simpleParagraph(
                text = text,
                spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, text.length)),
                width = 100.0f
            )

        assertThat(paragraph.charSequence.toString()).isEqualTo(text)
        assertThat(paragraph.charSequence).hasSpan(BackgroundColorSpan::class, 0, text.length) {
            span ->
            span.backgroundColor == color.toArgb()
        }
    }

    @Test
    fun testAnnotatedString_setBackgroundOnPartText() {
        val text = "abcde"
        val color = Color(0xFF0000FF)
        val spanStyle = SpanStyle(background = color)

        val paragraph =
            simpleParagraph(
                text = text,
                spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, "abc".length)),
                width = 100.0f
            )

        assertThat(paragraph.charSequence.toString()).isEqualTo(text)
        assertThat(paragraph.charSequence).hasSpan(BackgroundColorSpan::class, 0, "abc".length) {
            span ->
            span.backgroundColor == color.toArgb()
        }
    }

    @Test
    fun testAnnotatedString_setBackgroundTwice_lastOneOverwrite() {
        val text = "abcde"
        val color = Color(0xFF0000FF)
        val spanStyle = SpanStyle(background = color)
        val colorOverwrite = Color(0xFF00FF00)
        val spanStyleOverwrite = SpanStyle(background = colorOverwrite)

        val paragraph =
            simpleParagraph(
                text = text,
                spanStyles =
                    listOf(
                        AnnotatedString.Range(spanStyle, 0, text.length),
                        AnnotatedString.Range(spanStyleOverwrite, 0, "abc".length)
                    ),
                width = 100.0f
            )

        assertThat(paragraph.charSequence.toString()).isEqualTo(text)
        assertThat(paragraph.charSequence).hasSpan(BackgroundColorSpan::class, 0, text.length) {
            span ->
            span.backgroundColor == color.toArgb()
        }
        assertThat(paragraph.charSequence).hasSpan(BackgroundColorSpan::class, 0, "abc".length) {
            span ->
            span.backgroundColor == colorOverwrite.toArgb()
        }
        assertThat(paragraph.charSequence).hasSpanOnTop(
            BackgroundColorSpan::class,
            0,
            "abc".length
        ) { span ->
            span.backgroundColor == colorOverwrite.toArgb()
        }
    }

    @Test
    fun testAnnotatedString_setLocaleOnWholeText() {
        val text = "abcde"
        val localeList = LocaleList("en-US")
        val spanStyle = SpanStyle(localeList = localeList)

        val paragraph =
            simpleParagraph(
                text = text,
                spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, text.length)),
                width = 100.0f
            )

        assertThat(paragraph.charSequence).hasSpan(LocaleSpan::class, 0, text.length)
    }

    @Test
    fun testAnnotatedString_setLocaleOnPartText() {
        val text = "abcde"
        val localeList = LocaleList("en-US")
        val spanStyle = SpanStyle(localeList = localeList)

        val paragraph =
            simpleParagraph(
                text = text,
                spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, "abc".length)),
                width = 100.0f
            )

        assertThat(paragraph.charSequence).hasSpan(LocaleSpan::class, 0, "abc".length)
    }

    @Test
    fun testAnnotatedString_setLocaleTwice_lastOneOverwrite() {
        val text = "abcde"
        val spanStyle = SpanStyle(localeList = LocaleList("en-US"))
        val spanStyleOverwrite = SpanStyle(localeList = LocaleList("ja-JP"))

        val paragraph =
            simpleParagraph(
                text = text,
                spanStyles =
                    listOf(
                        AnnotatedString.Range(spanStyle, 0, text.length),
                        AnnotatedString.Range(spanStyleOverwrite, 0, "abc".length)
                    ),
                width = 100.0f
            )

        assertThat(paragraph.charSequence).hasSpan(LocaleSpan::class, 0, text.length)
        assertThat(paragraph.charSequence).hasSpan(LocaleSpan::class, 0, "abc".length)
        assertThat(paragraph.charSequence).hasSpanOnTop(LocaleSpan::class, 0, "abc".length)
    }

    @Test
    fun testAnnotatedString_setBaselineShiftOnWholeText() {
        val text = "abcde"
        val spanStyle = SpanStyle(baselineShift = BaselineShift.Subscript)

        val paragraph =
            simpleParagraph(
                text = text,
                spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, text.length)),
                width = 100.0f // width is not important
            )

        assertThat(paragraph.charSequence).hasSpan(BaselineShiftSpan::class, 0, text.length)
    }

    @Test
    fun testAnnotatedString_setBaselineShiftOnPartText() {
        val text = "abcde"
        val spanStyle = SpanStyle(baselineShift = BaselineShift.Superscript)

        val paragraph =
            simpleParagraph(
                text = text,
                spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, "abc".length)),
                width = 100.0f // width is not important
            )

        assertThat(paragraph.charSequence).hasSpan(BaselineShiftSpan::class, 0, "abc".length)
    }

    @Test
    fun testAnnotatedString_setBaselineShiftTwice_LastOneOnTop() {
        val text = "abcde"
        val spanStyle = SpanStyle(baselineShift = BaselineShift.Subscript)
        val spanStyleOverwrite = SpanStyle(baselineShift = BaselineShift.Superscript)

        val paragraph =
            simpleParagraph(
                text = text,
                spanStyles =
                    listOf(
                        AnnotatedString.Range(spanStyle, 0, text.length),
                        AnnotatedString.Range(spanStyleOverwrite, 0, "abc".length)
                    ),
                width = 100.0f // width is not important
            )

        assertThat(paragraph.charSequence).hasSpan(BaselineShiftSpan::class, 0, text.length)
        assertThat(paragraph.charSequence).hasSpan(BaselineShiftSpan::class, 0, "abc".length)
        assertThat(paragraph.charSequence).hasSpanOnTop(BaselineShiftSpan::class, 0, "abc".length)
    }

    @Test
    fun testAnnotatedString_setDefaultTextGeometricTransform() {
        val text = "abcde"
        val spanStyle = SpanStyle(textGeometricTransform = TextGeometricTransform())

        val paragraph =
            simpleParagraph(
                text = text,
                spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, text.length)),
                width = 100.0f // width is not important
            )

        assertThat(paragraph.charSequence).hasSpan(ScaleXSpan::class, 0, text.length) {
            it.scaleX == 1.0f
        }
        assertThat(paragraph.charSequence).hasSpan(
            spanClazz = SkewXSpan::class,
            start = 0,
            end = text.length
        ) {
            it.skewX == 0.0f
        }
    }

    @Test
    fun testAnnotatedString_setTextGeometricTransformWithScaleX() {
        val text = "abcde"
        val scaleX = 0.5f
        val spanStyle = SpanStyle(textGeometricTransform = TextGeometricTransform(scaleX = scaleX))

        val paragraph =
            simpleParagraph(
                text = text,
                spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, text.length)),
                width = 100.0f // width is not important
            )

        assertThat(paragraph.charSequence).hasSpan(ScaleXSpan::class, 0, text.length) {
            it.scaleX == scaleX
        }
        assertThat(paragraph.charSequence).hasSpan(SkewXSpan::class, 0, text.length) {
            it.skewX == 0.0f
        }
    }

    @Test
    fun testAnnotatedString_setTextGeometricTransformWithSkewX() {
        val text = "aa"
        val skewX = 1f
        val spanStyle = SpanStyle(textGeometricTransform = TextGeometricTransform(skewX = skewX))

        val paragraph =
            simpleParagraph(
                text = text,
                spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, text.length)),
                width = 100.0f // width is not important
            )

        assertThat(paragraph.charSequence).hasSpan(SkewXSpan::class, 0, text.length) {
            it.skewX == skewX
        }
        assertThat(paragraph.charSequence).hasSpan(ScaleXSpan::class, 0, text.length) {
            it.scaleX == 1.0f
        }
    }

    @Test
    fun textIndent_onWholeParagraph() {
        val text = "abc\ndef"
        val firstLine = 40
        val restLine = 20

        val paragraph =
            simpleParagraph(
                text = text,
                textIndent = TextIndent(firstLine.sp, restLine.sp),
                width = 100.0f // width is not important
            )

        assertThat(paragraph.charSequence).hasSpan(
            LeadingMarginSpan.Standard::class,
            0,
            text.length
        ) {
            it.getLeadingMargin(true) == firstLine && it.getLeadingMargin(false) == restLine
        }
    }

    @Test
    fun testAnnotatedString_setShadow() {
        val text = "abcde"
        val color = Color(0xFF00FF00)
        val offset = Offset(1f, 2f)
        val radius = 3.0f
        val spanStyle = SpanStyle(shadow = Shadow(color, offset, radius))

        val paragraph =
            simpleParagraph(
                text = text,
                spanStyles = listOf(AnnotatedString.Range(spanStyle, start = 0, end = text.length)),
                width = 100.0f // width is not important
            )

        assertThat(paragraph.charSequence).hasSpan(
            ShadowSpan::class,
            start = 0,
            end = text.length
        ) {
            return@hasSpan it.color == color.toArgb() &&
                it.offsetX == offset.x &&
                it.offsetY == offset.y &&
                it.radius == radius
        }
    }

    @Test
    fun testAnnotatedString_setShadow_withZeroBlur() {
        val text = "abcde"
        val spanStyle =
            SpanStyle(
                shadow = Shadow(color = Color(0xFF00FF00), offset = Offset(1f, 2f), blurRadius = 0f)
            )
        val paragraph =
            simpleParagraph(
                text = text,
                spanStyles = listOf(AnnotatedString.Range(spanStyle, start = 0, end = text.length)),
                width = 100.0f // width is not important
            )

        assertThat(paragraph.charSequence).hasSpan(
            ShadowSpan::class,
            start = 0,
            end = text.length
        ) {
            return@hasSpan it.radius == Float.MIN_VALUE
        }
    }

    @Test
    fun testAnnotatedString_setShadow_withZeroBlur_bitmap() {
        val text = "abcde"
        val width = 100.0f // width is not important

        val shadow = Shadow(color = Color(0xFF00FF00), offset = Offset(1f, 2f), blurRadius = 0f)

        val textStyle = TextStyle(fontSize = 8.sp)

        val paragraphNoShadow =
            simpleParagraph(text = text, style = textStyle, spanStyles = listOf(), width = width)

        val paragraphZeroBlur =
            simpleParagraph(
                text = text,
                style = textStyle,
                spanStyles =
                    listOf(
                        AnnotatedString.Range(
                            SpanStyle(shadow = shadow),
                            start = 0,
                            end = text.length
                        )
                    ),
                width = width
            )

        val paragraphFloatMinBlur =
            simpleParagraph(
                text = text,
                style = textStyle,
                spanStyles =
                    listOf(
                        AnnotatedString.Range(
                            SpanStyle(shadow = shadow.copy(blurRadius = Float.MIN_VALUE)),
                            start = 0,
                            end = text.length
                        )
                    ),
                width = width
            )

        val paragraphOneBlur =
            simpleParagraph(
                text = text,
                style = textStyle,
                spanStyles =
                    listOf(
                        AnnotatedString.Range(
                            SpanStyle(shadow = shadow.copy(blurRadius = 1f)),
                            start = 0,
                            end = text.length
                        )
                    ),
                width = width
            )

        assertThat(paragraphZeroBlur.bitmap()).isNotEqualToBitmap(paragraphNoShadow.bitmap())
        assertThat(paragraphZeroBlur.bitmap()).isEqualToBitmap(paragraphFloatMinBlur.bitmap())
        assertThat(paragraphZeroBlur.bitmap()).isNotEqualToBitmap(paragraphOneBlur.bitmap())
    }

    @Test
    fun testAnnotatedString_setShadowTwice_lastOnTop() {
        val text = "abcde"
        val color = Color(0xFF00FF00)
        val offset = Offset(1f, 2f)
        val radius = 3.0f
        val spanStyle = SpanStyle(shadow = Shadow(color, offset, radius))

        val colorOverwrite = Color(0xFF0000FF)
        val offsetOverwrite = Offset(3f, 2f)
        val radiusOverwrite = 1.0f
        val spanStyleOverwrite =
            SpanStyle(shadow = Shadow(colorOverwrite, offsetOverwrite, radiusOverwrite))

        val paragraph =
            simpleParagraph(
                text = text,
                spanStyles =
                    listOf(
                        AnnotatedString.Range(spanStyle, start = 0, end = text.length),
                        AnnotatedString.Range(spanStyleOverwrite, start = 0, end = "abc".length)
                    ),
                width = 100.0f // width is not important
            )

        assertThat(paragraph.charSequence).hasSpan(
            ShadowSpan::class,
            start = 0,
            end = text.length
        ) {
            return@hasSpan it.color == color.toArgb() &&
                it.offsetX == offset.x &&
                it.offsetY == offset.y &&
                it.radius == radius
        }
        assertThat(paragraph.charSequence).hasSpanOnTop(
            ShadowSpan::class,
            start = 0,
            end = "abc".length
        ) {
            return@hasSpanOnTop it.color == colorOverwrite.toArgb() &&
                it.offsetX == offsetOverwrite.x &&
                it.offsetY == offsetOverwrite.y &&
                it.radius == radiusOverwrite
        }
    }

    @Test
    fun testAnnotatedString_setDrawStyle() {
        val text = "abcde"
        val strokeWidth = 8f
        val strokeMiter = 4f
        val strokeCap = StrokeCap.Round
        val strokeJoin = StrokeJoin.Bevel
        val spanStyle =
            SpanStyle(drawStyle = Stroke(strokeWidth, strokeMiter, strokeCap, strokeJoin))

        val paragraph =
            simpleParagraph(
                text = text,
                spanStyles = listOf(AnnotatedString.Range(spanStyle, start = 0, end = text.length)),
                width = 0.0f
            )

        assertThat(paragraph.charSequence).hasSpan(
            DrawStyleSpan::class,
            start = 0,
            end = text.length
        ) {
            return@hasSpan it.drawStyle == Stroke(strokeWidth, strokeMiter, strokeCap, strokeJoin)
        }
    }

    @Test
    fun testAnnotatedString_setDrawStyle_FillOnTopOfStroke() {
        val text = "abcde"
        val strokeWidth = 8f
        val strokeMiter = 4f
        val strokeCap = StrokeCap.Round
        val strokeJoin = StrokeJoin.Bevel
        val strokeSpanStyle =
            SpanStyle(drawStyle = Stroke(strokeWidth, strokeMiter, strokeCap, strokeJoin))
        val fillSpanStyle = SpanStyle(drawStyle = Fill)

        val paragraph =
            simpleParagraph(
                text = text,
                spanStyles =
                    listOf(
                        AnnotatedString.Range(strokeSpanStyle, start = 0, end = text.length),
                        AnnotatedString.Range(fillSpanStyle, start = 1, end = 3)
                    ),
                width = 0.0f
            )

        assertThat(paragraph.charSequence).hasSpan(
            DrawStyleSpan::class,
            start = 0,
            end = text.length
        ) {
            return@hasSpan it.drawStyle == Stroke(strokeWidth, strokeMiter, strokeCap, strokeJoin)
        }
        assertThat(paragraph.charSequence).hasSpanOnTop(DrawStyleSpan::class, start = 1, end = 3) {
            return@hasSpanOnTop it.drawStyle == Fill
        }
    }

    @Test
    fun testAnnotatedString_fontFeatureSetting_setSpanOnText() {
        val text = "abc"
        val fontFeatureSettings = "\"kern\" 0"
        val spanStyle = SpanStyle(fontFeatureSettings = fontFeatureSettings)

        val paragraph =
            simpleParagraph(
                text = text,
                spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, "abc".length)),
                width = 100.0f // width is not important
            )

        assertThat(paragraph.charSequence).hasSpan(FontFeatureSpan::class, 0, "abc".length) {
            it.fontFeatureSettings == fontFeatureSettings
        }
    }

    @Test
    @MediumTest
    fun testEmptyFontFamily() {
        val paragraph = simpleParagraph(text = "abc", width = Float.MAX_VALUE)

        assertThat(paragraph.textPaint.typeface).isNull()
    }

    @Test
    @MediumTest
    fun testEmptyFontFamily_withBoldFontWeightSelection() {
        val paragraph =
            simpleParagraph(
                text = "abc",
                style = TextStyle(fontFamily = null, fontWeight = FontWeight.Bold),
                width = Float.MAX_VALUE
            )
        val typeface = paragraph.textPaint.typeface
        assertThat(typeface).isNotNull()
        assertThat(typeface.isBold).isTrue()
        assertThat(typeface.isItalic).isFalse()
    }

    @Test
    @MediumTest
    fun testEmptyFontFamily_withFontStyleSelection() {
        val paragraph =
            simpleParagraph(
                text = "abc",
                style = TextStyle(fontFamily = null, fontStyle = FontStyle.Italic),
                width = Float.MAX_VALUE
            )

        val typeface = paragraph.textPaint.typeface
        assertThat(typeface).isNotNull()
        assertThat(typeface.isBold).isFalse()
        assertThat(typeface.isItalic).isTrue()
    }

    @Test
    @MediumTest
    fun testFontFamily_withGenericFamilyName() {
        val fontFamily = FontFamily.SansSerif

        val paragraph =
            simpleParagraph(
                text = "abc",
                style = TextStyle(fontFamily = fontFamily),
                width = Float.MAX_VALUE
            )

        val typeface = paragraph.textPaint.typeface
        assertThat(typeface).isNotNull()
        assertThat(typeface.isBold).isFalse()
        assertThat(typeface.isItalic).isFalse()
    }

    @Test
    @MediumTest
    fun testFontFamily_withCustomFont() {
        val typefaceLoader = AsyncTestTypefaceLoader()
        val expectedTypeface: Typeface =
            UncachedFontFamilyResolver(context).resolve(BASIC_MEASURE_FONT.toFontFamily()).value
                as Typeface
        val font = BlockingFauxFont(typefaceLoader, expectedTypeface)
        val paragraph =
            simpleParagraph(
                text = "abc",
                style = TextStyle(fontFamily = font.toFontFamily()),
                width = Float.MAX_VALUE
            )

        val typeface: Typeface = paragraph.textPaint.typeface

        assertThat(typeface).isSameInstanceAs(expectedTypeface)
    }

    @Test
    fun testEllipsis_withMaxLineEqualsNull_doesNotEllipsis() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 20.sp
            val paragraphWidth = (text.length - 1) * fontSize.toPx()
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontFamily = basicFontFamily, fontSize = fontSize),
                    overflow = TextOverflow.Ellipsis,
                    width = paragraphWidth
                )

            for (i in 0 until paragraph.lineCount) {
                assertThat(paragraph.isLineEllipsized(i)).isFalse()
            }
        }
    }

    @Test
    fun testEllipsis_withMaxLinesLessThanTextLines_doesEllipsis() {
        with(defaultDensity) {
            val text = "abcde"
            val fontSize = 100.sp
            // Note that on API 21, if the next line only contains 1 character, ellipsis won't work
            val paragraphWidth = (text.length - 1.5f) * fontSize.toPx()
            val paragraph =
                simpleParagraph(
                    text = text,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = TextStyle(fontFamily = basicFontFamily, fontSize = fontSize),
                    width = paragraphWidth
                )

            assertThat(paragraph.isLineEllipsized(0)).isTrue()
        }
    }

    @Test
    fun testEllipsis_withMaxLinesMoreThanTextLines_doesNotEllipsis() {
        with(defaultDensity) {
            val text = "abc"
            val fontSize = 100.sp
            val paragraphWidth = (text.length - 1) * fontSize.toPx()
            val maxLines = ceil(text.length * fontSize.toPx() / paragraphWidth).toInt()
            val paragraph =
                simpleParagraph(
                    text = text,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = maxLines,
                    style = TextStyle(fontFamily = basicFontFamily, fontSize = fontSize),
                    width = paragraphWidth
                )

            for (i in 0 until paragraph.lineCount) {
                assertThat(paragraph.isLineEllipsized(i)).isFalse()
            }
        }
    }

    @Test
    fun testEllipsis_withLimitedHeightFitAllLines_doesNotEllipsis() {
        with(defaultDensity) {
            val text = "This is a text"
            val fontSize = 30.sp
            val paragraph =
                simpleParagraph(
                    text = text,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(fontFamily = basicFontFamily, fontSize = fontSize),
                    width = 4 * fontSize.toPx(),
                    height = 6 * fontSize.toPx(),
                )

            for (i in 0 until paragraph.lineCount) {
                assertThat(paragraph.isLineEllipsized(i)).isFalse()
            }
        }
    }

    @Test
    fun testEllipsis_withLimitedHeight_doesEllipsis() {
        with(defaultDensity) {
            val text = "This is a text"
            val fontSize = 30.sp
            val paragraph =
                simpleParagraph(
                    text = text,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(fontFamily = basicFontFamily, fontSize = fontSize),
                    width = 4 * fontSize.toPx(),
                    height = 2.2f * fontSize.toPx(), // fits 2 lines
                )

            assertThat(paragraph.lineCount).isEqualTo(2)
            assertThat(paragraph.isLineEllipsized(paragraph.lineCount - 1)).isTrue()
        }
    }

    @Test
    fun testEllipsis_withLimitedHeight_overflowNotEllipsis_doesNotEllipsis() {
        with(defaultDensity) {
            val text = "This is a text"
            val fontSize = 30.sp
            val paragraph =
                simpleParagraph(
                    text = text,
                    overflow = TextOverflow.Clip,
                    style = TextStyle(fontFamily = basicFontFamily, fontSize = fontSize),
                    width = 4 * fontSize.toPx(),
                    height = 2.2f * fontSize.toPx(), // fits 2 lines
                )

            for (i in 0 until paragraph.lineCount) {
                assertThat(paragraph.isLineEllipsized(i)).isFalse()
            }
        }
    }

    @Test
    fun testEllipsis_withMaxLinesMoreThanTextLines_andLimitedHeight_doesEllipsis() {
        with(defaultDensity) {
            val text = "This is a text"
            val fontSize = 30.sp
            val paragraph =
                simpleParagraph(
                    text = text,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(fontFamily = basicFontFamily, fontSize = fontSize),
                    width = 4 * fontSize.toPx(),
                    height = 2.2f * fontSize.toPx(), // fits 2 lines
                    maxLines = 5
                )

            assertThat(paragraph.lineCount).isEqualTo(2)
            assertThat(paragraph.isLineEllipsized(paragraph.lineCount - 1)).isTrue()
        }
    }

    @Test
    fun testEllipsis_withMaxLines_andLimitedHeight_doesEllipsis() {
        with(defaultDensity) {
            val text = "This is a text"
            val fontSize = 30.sp
            val paragraph =
                simpleParagraph(
                    text = text,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(fontFamily = basicFontFamily, fontSize = fontSize),
                    width = 4 * fontSize.toPx(),
                    height = 4 * fontSize.toPx(),
                    maxLines = 2
                )

            assertThat(paragraph.lineCount).isEqualTo(2)
            assertThat(paragraph.isLineEllipsized(paragraph.lineCount - 1)).isTrue()
        }
    }

    @Test
    fun testEllipsis_whenHeightAllowsForZeroLines_doesEllipsis() {
        with(defaultDensity) {
            val text = "This is a text"
            val fontSize = 30.sp
            val paragraph =
                simpleParagraph(
                    text = text,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(fontFamily = basicFontFamily, fontSize = fontSize),
                    width = 4 * fontSize.toPx(),
                    height = fontSize.toPx() / 4
                )

            assertThat(paragraph.didExceedMaxLines).isTrue()
            assertThat(paragraph.lineCount).isEqualTo(1)
            assertThat(paragraph.isLineEllipsized(paragraph.lineCount - 1)).isTrue()
        }
    }

    @Test
    fun testEllipsis_withSpans_withLimitedHeight_doesEllipsis() {
        with(defaultDensity) {
            val text = "This is a text"
            val fontSize = 30.sp
            val paragraph =
                simpleParagraph(
                    text = text,
                    spanStyles =
                        listOf(AnnotatedString.Range(SpanStyle(fontSize = fontSize * 2), 0, 2)),
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(fontFamily = basicFontFamily, fontSize = fontSize),
                    width = 4 * fontSize.toPx(),
                    height = 2.2f * fontSize.toPx() // fits 2 lines
                )

            assertThat(paragraph.lineCount).isEqualTo(1)
            assertThat(paragraph.isLineEllipsized(paragraph.lineCount - 1)).isTrue()
        }
    }

    // Experimentally verified that middle and start ellipsis don't work correctly on API 21
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP_MR1)
    @Test
    fun testEllipsis_withMaxLinesOne_doesStartEllipsis() {
        with(defaultDensity) {
            val text = "abcde"
            val fontSize = 100.sp
            val paragraphWidth = (text.length - 2f) * fontSize.toPx()
            val paragraph =
                simpleParagraph(
                    text = text,
                    overflow = TextOverflow.StartEllipsis,
                    maxLines = 1,
                    style = TextStyle(fontFamily = basicFontFamily, fontSize = fontSize),
                    width = paragraphWidth
                )

            assertThat(paragraph.isLineEllipsized(0)).isTrue()
            assertThat(paragraph.getLineEllipsisOffset(0)).isEqualTo(0)
            assertThat(paragraph.getLineEllipsisCount(0)).isEqualTo(3)
        }
    }

    // Experimentally verified that middle and start ellipsis don't work correctly on API 21
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.LOLLIPOP_MR1)
    @Test
    fun testEllipsis_withMaxLinesOne_doesMiddleEllipsis() {
        with(defaultDensity) {
            val text = "abcde"
            val fontSize = 100.sp
            val paragraphWidth = (text.length - 2f) * fontSize.toPx()
            val paragraph =
                simpleParagraph(
                    text = text,
                    overflow = TextOverflow.MiddleEllipsis,
                    maxLines = 1,
                    style = TextStyle(fontFamily = basicFontFamily, fontSize = fontSize),
                    width = paragraphWidth
                )

            assertThat(paragraph.isLineEllipsized(0)).isTrue()
            assertThat(paragraph.getLineEllipsisOffset(0)).isEqualTo(1)
            assertThat(paragraph.getLineEllipsisCount(0)).isEqualTo(3)
        }
    }

    @Test
    fun testSpanStyle_fontSize_appliedOnTextPaint() {
        with(defaultDensity) {
            val fontSize = 100.sp
            val paragraph =
                simpleParagraph(text = "", style = TextStyle(fontSize = fontSize), width = 0.0f)

            assertThat(paragraph.textPaint.textSize).isEqualTo(fontSize.toPx())
        }
    }

    @Test
    fun testSpanStyle_locale_appliedOnTextPaint() {
        val platformLocale = java.util.Locale.JAPANESE
        val localeList = LocaleList(platformLocale.toLanguageTag())

        val paragraph =
            simpleParagraph(text = "", style = TextStyle(localeList = localeList), width = 0.0f)

        assertThat(paragraph.textPaint.textLocale.language).isEqualTo(platformLocale.language)
        assertThat(paragraph.textPaint.textLocale.country).isEqualTo(platformLocale.country)
    }

    @Test
    fun testSpanStyle_color_appliedOnTextPaint() {
        val color = Color(0x12345678)
        val paragraph = simpleParagraph(text = "", style = TextStyle(color = color), width = 0.0f)

        assertThat(paragraph.textPaint.color).isEqualTo(color.toArgb())
    }

    @Test
    fun testSpanStyle_brush_appliedOnTextPaint() {
        val brush = Brush.horizontalGradient(listOf(Color.Red, Color.Blue)) as ShaderBrush
        val paragraph = simpleParagraph(text = "", style = TextStyle(brush = brush), width = 1.0f)

        assertThat(paragraph.textPaint.shader).isNotNull()
    }

    @Test
    fun testTextStyle_letterSpacingInEm_appliedOnTextPaint() {
        val letterSpacing = 2
        val paragraph =
            simpleParagraph(
                text = "",
                style = TextStyle(letterSpacing = letterSpacing.em),
                width = 0.0f
            )

        assertThat(paragraph.textPaint.letterSpacing).isEqualTo((letterSpacing))
    }

    @Test
    fun testTextStyle_letterSpacingInSp_appliedAsSpan() {
        val letterSpacing = 5f
        val annotatedText = buildAnnotatedString {
            pushStyle(SpanStyle(fontWeight = FontWeight.W800))
            append("abc")
            pop()
        }
        val paragraph =
            simpleParagraph(
                text = annotatedText.text,
                spanStyles = annotatedText.spanStyles,
                style = TextStyle(letterSpacing = letterSpacing.sp),
                width = 0.0f
            )

        assertThat(paragraph.charSequence).hasSpan(
            LetterSpacingSpanPx::class,
            0,
            annotatedText.length
        ) {
            it.letterSpacing == letterSpacing
        }
    }

    @Test
    fun testTextStyle_letterSpacingInSp_noSpan_whenNoAnnoattions() {
        val letterSpacing = 5f
        val annotatedText = "abc"
        val paragraph =
            simpleParagraph(
                text = annotatedText,
                style = TextStyle(letterSpacing = letterSpacing.sp),
                width = 0.0f
            )

        assertThat(paragraph.charSequence).doesNotHaveSpan(LetterSpacingSpanPx::class)
    }

    @Test
    fun testSpanStyle_fontFeatureSettings_appliedOnTextPaint() {
        val fontFeatureSettings = "\"kern\" 0"
        val paragraph =
            simpleParagraph(
                text = "",
                style = TextStyle(fontFeatureSettings = fontFeatureSettings),
                width = 0.0f
            )

        assertThat(paragraph.textPaint.fontFeatureSettings).isEqualTo(fontFeatureSettings)
    }

    @Test
    fun testSpanStyle_scaleX_appliedOnTextPaint() {
        val scaleX = 0.5f
        val paragraph =
            simpleParagraph(
                text = "",
                style = TextStyle(textGeometricTransform = TextGeometricTransform(scaleX = scaleX)),
                width = 0.0f
            )

        assertThat(paragraph.textPaint.textScaleX).isEqualTo(scaleX)
    }

    @Test
    fun testSpanStyle_skewX_appliedOnTextPaint() {
        val skewX = 0.5f
        val paragraph =
            simpleParagraph(
                text = "",
                style = TextStyle(textGeometricTransform = TextGeometricTransform(skewX = skewX)),
                width = 0.0f
            )

        assertThat(paragraph.textPaint.textSkewX).isEqualTo(skewX)
    }

    @Test
    fun testSpanStyle_textDecoration_underline_appliedOnTextPaint() {
        val paragraph =
            simpleParagraph(
                text = "",
                style = TextStyle(textDecoration = TextDecoration.Underline),
                width = 0.0f
            )

        assertThat(paragraph.textPaint.isUnderlineText).isTrue()
    }

    @Test
    fun testSpanStyle_textDecoration_underline_appliedAsSpan() {
        val text = "abc"
        val paragraph =
            simpleParagraph(
                text = text,
                style = TextStyle(textDecoration = TextDecoration.Underline),
                width = 0.0f
            )

        assertThat(paragraph.charSequence).hasSpan(CharacterStyle::class, 0, text.length)
    }

    @Test
    fun testSpanStyle_textDecoration_lineThrough_appliedOnTextPaint() {
        val paragraph =
            simpleParagraph(
                text = "",
                style = TextStyle(textDecoration = TextDecoration.LineThrough),
                width = 0.0f
            )

        assertThat(paragraph.textPaint.isStrikeThruText).isTrue()
    }

    @Test
    fun testSpanStyle_drawStyle_stroke_appliedOnTextPaint() {
        val paragraph =
            simpleParagraph(
                text = "",
                style =
                    TextStyle(
                        drawStyle =
                            Stroke(
                                width = 8f,
                                miter = 6f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Bevel
                            )
                    ),
                width = 0.0f
            )

        assertThat(paragraph.textPaint.style).isEqualTo(Paint.Style.STROKE)
        assertThat(paragraph.textPaint.strokeWidth).isEqualTo(8f)
        assertThat(paragraph.textPaint.strokeMiter).isEqualTo(6f)
        assertThat(paragraph.textPaint.strokeCap).isEqualTo(Paint.Cap.ROUND)
        assertThat(paragraph.textPaint.strokeJoin).isEqualTo(Paint.Join.BEVEL)
    }

    @Test
    fun testSpanStyle_drawStyle_fill_appliedOnTextPaint() {
        val paragraph =
            simpleParagraph(text = "", style = TextStyle(drawStyle = Fill), width = 0.0f)

        assertThat(paragraph.textPaint.style).isEqualTo(Paint.Style.FILL)
    }

    @Test
    fun testSpanStyle_background_appliedAsSpan() {
        // bgColor is reset in the Android Layout constructor.
        // therefore we cannot apply them on paint, have to use spans.
        val text = "abc"
        val color = Color(0x12345678)
        val paragraph =
            simpleParagraph(text = text, style = TextStyle(background = color), width = 0.0f)

        assertThat(paragraph.charSequence).hasSpan(BackgroundColorSpan::class, 0, text.length) {
            span ->
            span.backgroundColor == color.toArgb()
        }
    }

    @Test
    fun testPaint_can_change_TextDecoration_to_Underline() {
        val paragraph =
            simpleParagraph(text = "", style = TextStyle(textDecoration = null), width = 0.0f)
        assertThat(paragraph.textPaint.isUnderlineText).isFalse()

        val canvas = Canvas(android.graphics.Canvas())
        paragraph.paint(canvas, textDecoration = TextDecoration.Underline)
        assertThat(paragraph.textPaint.isUnderlineText).isEqualTo(true)
    }

    @Test
    fun testPaint_can_change_TextDecoration_to_None() {
        val paragraph =
            simpleParagraph(
                text = "",
                style = TextStyle(textDecoration = TextDecoration.Underline),
                width = 0.0f
            )
        assertThat(paragraph.textPaint.isUnderlineText).isTrue()

        val canvas = Canvas(android.graphics.Canvas())
        paragraph.paint(canvas, textDecoration = TextDecoration.None)
        assertThat(paragraph.textPaint.isUnderlineText).isFalse()
    }

    @Test
    fun testPaint_TextDecoration_null_should_have_no_effect() {
        val paragraph =
            simpleParagraph(
                text = "",
                style = TextStyle(textDecoration = TextDecoration.Underline),
                width = 0.0f
            )
        assertThat(paragraph.textPaint.isUnderlineText).isTrue()

        val canvas = Canvas(android.graphics.Canvas())
        paragraph.paint(canvas, textDecoration = null)
        assertThat(paragraph.textPaint.isUnderlineText).isTrue()
    }

    @Test
    fun testPaint_can_change_DrawStyle_to_Stroke() {
        val paragraph =
            simpleParagraph(text = "", style = TextStyle(drawStyle = null), width = 0.0f)
        assertThat(paragraph.textPaint.style).isEqualTo(Paint.Style.FILL)

        val stroke = Stroke(width = 4f, miter = 2f, cap = StrokeCap.Square, join = StrokeJoin.Bevel)
        val canvas = Canvas(android.graphics.Canvas())
        paragraph.paint(canvas, drawStyle = stroke)

        assertThat(paragraph.textPaint.style).isEqualTo(Paint.Style.STROKE)
        assertThat(paragraph.textPaint.strokeWidth).isEqualTo(4f)
        assertThat(paragraph.textPaint.strokeMiter).isEqualTo(2f)
        assertThat(paragraph.textPaint.strokeCap).isEqualTo(Paint.Cap.SQUARE)
        assertThat(paragraph.textPaint.strokeJoin).isEqualTo(Paint.Join.BEVEL)
    }

    @Test
    fun testPaint_can_change_DrawStyle_to_Fill() {
        val paragraph =
            simpleParagraph(text = "", style = TextStyle(drawStyle = Stroke(8f)), width = 0.0f)
        assertThat(paragraph.textPaint.style).isEqualTo(Paint.Style.STROKE)
        assertThat(paragraph.textPaint.strokeWidth).isEqualTo(8f)

        val canvas = Canvas(android.graphics.Canvas())
        paragraph.paint(canvas, drawStyle = Fill)
        assertThat(paragraph.textPaint.style).isEqualTo(Paint.Style.FILL)
    }

    @Test
    fun testPaint_null_drawStyle_should_be_noop() {
        val paragraph =
            simpleParagraph(text = "", style = TextStyle(drawStyle = Stroke(8f)), width = 0.0f)
        assertThat(paragraph.textPaint.style).isEqualTo(Paint.Style.STROKE)
        assertThat(paragraph.textPaint.strokeWidth).isEqualTo(8f)

        val canvas = Canvas(android.graphics.Canvas())
        paragraph.paint(canvas, drawStyle = null)
        assertThat(paragraph.textPaint.style).isEqualTo(Paint.Style.STROKE)
        assertThat(paragraph.textPaint.strokeWidth).isEqualTo(8f)
    }

    @Test
    fun testPaint_cannot_change_blendMode_permanently() {
        val paragraph = simpleParagraph(text = "", width = 0.0f)
        assertThat(paragraph.textPaint.blendMode).isEqualTo(BlendMode.SrcOver)

        val canvas = Canvas(android.graphics.Canvas())
        paragraph.paint(canvas, blendMode = BlendMode.Multiply)
        assertThat(paragraph.textPaint.blendMode).isEqualTo(BlendMode.SrcOver)
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    fun testPaint_can_change_Shadow() {
        val paragraph = simpleParagraph(text = "", style = TextStyle(shadow = null), width = 0.0f)

        assertThat(paragraph.textPaint.shadowLayerColor).isEqualTo(0)
        assertThat(paragraph.textPaint.shadowLayerDx).isEqualTo(0f)
        assertThat(paragraph.textPaint.shadowLayerDy).isEqualTo(0f)
        assertThat(paragraph.textPaint.shadowLayerRadius).isEqualTo(0f)

        val canvas = Canvas(android.graphics.Canvas())
        val color = Color.Red
        val dx = 1f
        val dy = 2f
        val radius = 3f

        paragraph.paint(
            canvas,
            shadow = Shadow(color = color, offset = Offset(dx, dy), blurRadius = radius)
        )
        assertThat(paragraph.textPaint.shadowLayerColor).isEqualTo(color.toArgb())
        assertThat(paragraph.textPaint.shadowLayerDx).isEqualTo(dx)
        assertThat(paragraph.textPaint.shadowLayerDy).isEqualTo(dy)
        assertThat(paragraph.textPaint.shadowLayerRadius).isEqualTo(radius)
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    fun testPaint_can_change_Shadow_to_None() {
        val dx = 1f
        val dy = 2f
        val radius = 3f
        val color = Color.Red

        val paragraph =
            simpleParagraph(
                text = "",
                style = TextStyle(shadow = Shadow(color, Offset(dx, dy), radius)),
                width = 0.0f
            )

        assertThat(paragraph.textPaint.shadowLayerDx).isEqualTo(dx)
        assertThat(paragraph.textPaint.shadowLayerDy).isEqualTo(dy)
        assertThat(paragraph.textPaint.shadowLayerRadius).isEqualTo(radius)
        assertThat(paragraph.textPaint.shadowLayerColor).isEqualTo(color.toArgb())

        val canvas = Canvas(android.graphics.Canvas())
        paragraph.paint(canvas, shadow = Shadow.None)
        assertThat(paragraph.textPaint.shadowLayerDx).isEqualTo(0f)
        assertThat(paragraph.textPaint.shadowLayerDy).isEqualTo(0f)
        assertThat(paragraph.textPaint.shadowLayerRadius).isEqualTo(0f)
        assertThat(paragraph.textPaint.shadowLayerColor).isEqualTo(0)
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    fun testPaint_can_change_Shadow_null() {
        val dx = 1f
        val dy = 2f
        val radius = 3f
        val color = Color.Red

        val paragraph =
            simpleParagraph(
                text = "",
                style = TextStyle(shadow = Shadow(color, Offset(dx, dy), radius)),
                width = 0.0f
            )

        assertThat(paragraph.textPaint.shadowLayerDx).isEqualTo(dx)
        assertThat(paragraph.textPaint.shadowLayerDy).isEqualTo(dy)
        assertThat(paragraph.textPaint.shadowLayerRadius).isEqualTo(radius)
        assertThat(paragraph.textPaint.shadowLayerColor).isEqualTo(color.toArgb())

        val canvas = Canvas(android.graphics.Canvas())
        paragraph.paint(canvas, shadow = Shadow.None)
        assertThat(paragraph.textPaint.shadowLayerDx).isEqualTo(0f)
        assertThat(paragraph.textPaint.shadowLayerDy).isEqualTo(0f)
        assertThat(paragraph.textPaint.shadowLayerRadius).isEqualTo(0f)
        assertThat(paragraph.textPaint.shadowLayerColor).isEqualTo(0)
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    fun testPaint_shadow_blur_with_zero_resets_to_float_min() {
        val paragraph = simpleParagraph(text = "", style = TextStyle(shadow = null), width = 0.0f)

        assertThat(paragraph.textPaint.shadowLayerRadius).isEqualTo(0f)

        val canvas = Canvas(android.graphics.Canvas())
        val color = Color.Red
        val dx = 1f
        val dy = 2f
        val radius = 0f

        paragraph.paint(
            canvas,
            shadow = Shadow(color = color, offset = Offset(dx, dy), blurRadius = radius)
        )

        assertThat(paragraph.textPaint.shadowLayerRadius).isEqualTo(Float.MIN_VALUE)
    }

    @Test
    fun testPaint_shadow_blur_with_zero_resets_to_float_min_bitmap() {
        val text = "a"
        val width = 100f
        val fontSize = 8.sp

        val shadow = Shadow(color = Color(0xFF00FF00), offset = Offset(1f, 2f), blurRadius = 0f)

        val paragraphNoShadow =
            simpleParagraph(text = text, style = TextStyle(fontSize = fontSize), width = width)

        val paragraphZeroBlur =
            simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize, shadow = shadow),
                width = width
            )

        val paragraphFloatMinBlur =
            simpleParagraph(
                text = text,
                style =
                    TextStyle(
                        fontSize = fontSize,
                        shadow = shadow.copy(blurRadius = Float.MIN_VALUE)
                    ),
                width = width
            )

        val paragraphOneBlur =
            simpleParagraph(
                text = text,
                style = TextStyle(fontSize = fontSize, shadow = shadow.copy(blurRadius = 1f)),
                width = width
            )

        assertThat(paragraphZeroBlur.bitmap()).isNotEqualToBitmap(paragraphNoShadow.bitmap())
        assertThat(paragraphZeroBlur.bitmap()).isEqualToBitmap(paragraphFloatMinBlur.bitmap())
        assertThat(paragraphZeroBlur.bitmap()).isNotEqualToBitmap(paragraphOneBlur.bitmap())
    }

    @Test
    fun testPaint_can_change_Color() {
        val color1 = Color.Red

        val paragraph =
            simpleParagraph(text = "", style = TextStyle(color = Color.Red), width = 0.0f)
        assertThat(paragraph.textPaint.color).isEqualTo(color1.toArgb())

        val color2 = Color.Yellow
        val canvas = Canvas(android.graphics.Canvas())
        paragraph.paint(canvas, color = color2)
        assertThat(paragraph.textPaint.color).isEqualTo(color2.toArgb())
    }

    @Test
    fun testPaint_cannot_change_Color_to_Unspecified() {
        val color1 = Color.Red

        val paragraph =
            simpleParagraph(text = "", style = TextStyle(color = Color.Red), width = 0.0f)
        assertThat(paragraph.textPaint.color).isEqualTo(color1.toArgb())

        val color2 = Color.Unspecified
        val canvas = Canvas(android.graphics.Canvas())
        paragraph.paint(canvas, color = color2)
        assertThat(paragraph.textPaint.color).isEqualTo(color1.toArgb())
    }

    @Test
    fun testPaint_can_change_Color_to_Transparent() {
        val color1 = Color.Red

        val paragraph =
            simpleParagraph(text = "", style = TextStyle(color = Color.Red), width = 0.0f)
        assertThat(paragraph.textPaint.color).isEqualTo(color1.toArgb())

        val color2 = Color.Transparent
        val canvas = Canvas(android.graphics.Canvas())
        paragraph.paint(canvas, color = color2)
        assertThat(paragraph.textPaint.color).isEqualTo(color2.toArgb())
    }

    @Test
    fun testSpanStyle_baselineShift_appliedAsSpan() {
        // baselineShift is reset in the Android Layout constructor.
        // therefore we cannot apply them on paint, have to use spans.
        val text = "abc"
        val baselineShift = BaselineShift.Subscript
        val paragraph =
            simpleParagraph(
                text = text,
                style = TextStyle(baselineShift = baselineShift),
                width = 0.0f
            )

        assertThat(paragraph.charSequence).hasSpan(BaselineShiftSpan::class, 0, text.length) { span
            ->
            span.multiplier == BaselineShift.Subscript.multiplier
        }
    }

    @Test
    fun locale_isDefaultLocaleIfNotProvided() {
        val text = "abc"
        val paragraph = simpleParagraph(text = text, width = Float.MAX_VALUE)

        assertThat(paragraph.textLocale.toLanguageTag())
            .isEqualTo(java.util.Locale.getDefault().toLanguageTag())
    }

    @Test
    fun locale_isSetOnParagraphImpl_enUS() {
        val localeList = LocaleList("en-US")
        val text = "abc"
        val paragraph =
            simpleParagraph(
                text = text,
                style = TextStyle(localeList = localeList),
                width = Float.MAX_VALUE
            )

        assertThat(paragraph.textLocale.toLanguageTag()).isEqualTo("en-US")
    }

    @Test
    fun locale_isSetOnParagraphImpl_jpJP() {
        val localeList = LocaleList("ja-JP")
        val text = "abc"
        val paragraph =
            simpleParagraph(
                text = text,
                style = TextStyle(localeList = localeList),
                width = Float.MAX_VALUE
            )

        assertThat(paragraph.textLocale.toLanguageTag()).isEqualTo("ja-JP")
    }

    @Test
    fun locale_noCountryCode_isSetOnParagraphImpl() {
        val localeList = LocaleList("ja")
        val text = "abc"
        val paragraph =
            simpleParagraph(
                text = text,
                style = TextStyle(localeList = localeList),
                width = Float.MAX_VALUE
            )

        assertThat(paragraph.textLocale.toLanguageTag()).isEqualTo("ja")
    }

    @Test
    fun withIncludeFontPadding() {
        val text = "A"

        @Suppress("DEPRECATION")
        val style =
            TextStyle(
                fontSize = 20.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = true)
            )

        val paragraphPaddingTrue =
            simpleParagraph(text = text, style = style, width = Float.MAX_VALUE)

        @Suppress("DEPRECATION")
        val paragraphPaddingFalse =
            simpleParagraph(
                text = text,
                style = style.copy(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                width = Float.MAX_VALUE
            )

        assertThat(paragraphPaddingTrue.height).isNotEqualTo(paragraphPaddingFalse.height)
    }

    @Test(expected = IllegalArgumentException::class)
    fun setMinWidthConstraints_notSupported() {
        val minWidthConstraints = Constraints(minWidth = 100)
        AndroidParagraph(
            text = "",
            style = TextStyle(),
            spanStyles = listOf(),
            placeholders = listOf(),
            maxLines = Int.MAX_VALUE,
            overflow = TextOverflow.Ellipsis,
            constraints = minWidthConstraints,
            fontFamilyResolver = UncachedFontFamilyResolver(context),
            density = defaultDensity,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun setMinHeightConstraints_notSupported() {
        val minHeightConstraints = Constraints(minHeight = 100)
        AndroidParagraph(
            text = "",
            style = TextStyle(),
            spanStyles = listOf(),
            placeholders = listOf(),
            maxLines = Int.MAX_VALUE,
            overflow = TextOverflow.Ellipsis,
            constraints = minHeightConstraints,
            fontFamilyResolver = UncachedFontFamilyResolver(context),
            density = defaultDensity,
        )
    }

    @Test
    fun shaderBrushSpan_createsShaderOnlyOnce() {
        val fontSize = 20
        val text = "abcdef"
        val shaderBrush =
            object : ShaderBrush() {
                var callCount = 0
                private val brush =
                    Brush.linearGradient(listOf(Color.Red, Color.Blue)) as ShaderBrush

                override fun createShader(size: Size): android.graphics.Shader {
                    callCount++
                    return brush.createShader(size)
                }
            }
        val spanStyle = SpanStyle(brush = shaderBrush)
        val paragraph =
            simpleParagraph(
                text = "abcdef",
                width = 2f * fontSize * text.length,
                style = TextStyle(fontSize = fontSize.sp),
                spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, 2))
            )

        // Call paint on paragraph multiple times
        repeat(5) { paragraph.bitmap() }
        assertThat(shaderBrush.callCount).isEqualTo(1)
    }

    @Test
    fun drawText_withUnderlineStyle_equalToUnderlinePaint() =
        with(defaultDensity) {
            val fontSize = 30.sp
            val fontSizeInPx = fontSize.toPx()
            val text = "レンズ(単焦点)"
            val spanStyle = SpanStyle(textDecoration = TextDecoration.Underline)
            val paragraph =
                simpleParagraph(
                    text = text,
                    style = TextStyle(fontSize = fontSize),
                    spanStyles = listOf(AnnotatedString.Range(spanStyle, 0, text.length)),
                    width = fontSizeInPx * 20
                )

            val paragraph2 =
                simpleParagraph(
                    text = text,
                    style =
                        TextStyle(fontSize = fontSize, textDecoration = TextDecoration.Underline),
                    width = fontSizeInPx * 20
                )

            val bitmapWithSpan = paragraph.bitmap()
            // Our text rendering stack relies on the fact that given textstyle is also passed to
            // draw
            // functions of TextLayoutResult, MultiParagraph, Paragraph. If Underline is not
            // specified
            // here, it would be removed while drawing the MultiParagraph. We are simply mimicking
            // what TextPainter does.
            val bitmapNoSpan = paragraph2.bitmap(textDecoration = TextDecoration.Underline)

            assertThat(bitmapWithSpan).isEqualToBitmap(bitmapNoSpan)
        }

    @Test
    fun textMotionStatic_setsCorrectFlagsOnTextPaint() {
        val textMotion = TextMotion.Static
        val text = "abc"
        val paragraph =
            simpleParagraph(
                text = text,
                style = TextStyle(textMotion = textMotion),
                width = Float.MAX_VALUE
            )

        assertThat(paragraph.textPaint.flags and TextPaint.LINEAR_TEXT_FLAG).isEqualTo(0)
        assertThat(paragraph.textPaint.flags and TextPaint.SUBPIXEL_TEXT_FLAG).isEqualTo(0)
        assertThat(paragraph.textPaint.hinting).isEqualTo(TextPaint.HINTING_ON)
    }

    @Test
    fun textMotionAnimated_setsCorrectFlagsOnTextPaint() {
        val textMotion = TextMotion.Animated
        val text = "abc"
        val paragraph =
            simpleParagraph(
                text = text,
                style = TextStyle(textMotion = textMotion),
                width = Float.MAX_VALUE
            )

        assertThat(paragraph.textPaint.flags and TextPaint.LINEAR_TEXT_FLAG)
            .isEqualTo(TextPaint.LINEAR_TEXT_FLAG)
        assertThat(paragraph.textPaint.flags and TextPaint.SUBPIXEL_TEXT_FLAG)
            .isEqualTo(TextPaint.SUBPIXEL_TEXT_FLAG)
        assertThat(paragraph.textPaint.hinting).isEqualTo(TextPaint.HINTING_OFF)
    }

    private fun simpleParagraph(
        text: String = "",
        spanStyles: List<AnnotatedString.Range<SpanStyle>> = listOf(),
        textIndent: TextIndent? = null,
        textAlign: TextAlign = TextAlign.Unspecified,
        overflow: TextOverflow = TextOverflow.Clip,
        maxLines: Int = Int.MAX_VALUE,
        width: Float,
        height: Float = Float.POSITIVE_INFINITY,
        style: TextStyle? = null,
        fontFamilyResolver: FontFamily.Resolver = UncachedFontFamilyResolver(context)
    ): AndroidParagraph {
        return AndroidParagraph(
            text = text,
            spanStyles = spanStyles,
            placeholders = listOf(),
            style = TextStyle(textAlign = textAlign, textIndent = textIndent).merge(style),
            maxLines = maxLines,
            overflow = overflow,
            constraints = Constraints(maxWidth = width.ceilToInt(), maxHeight = height.ceilToInt()),
            density = Density(density = 1f),
            fontFamilyResolver = fontFamilyResolver
        )
    }
}
