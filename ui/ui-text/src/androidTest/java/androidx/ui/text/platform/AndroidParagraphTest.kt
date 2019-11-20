package androidx.ui.text.platform

import android.graphics.Paint
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.LeadingMarginSpan
import android.text.style.LocaleSpan
import android.text.style.RelativeSizeSpan
import android.text.style.ScaleXSpan
import android.text.style.StrikethroughSpan
import android.text.style.UnderlineSpan
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.text.TextLayout
import androidx.text.style.BaselineShiftSpan
import androidx.text.style.FontFeatureSpan
import androidx.text.style.LetterSpacingSpan
import androidx.text.style.ShadowSpan
import androidx.text.style.SkewXSpan
import androidx.text.style.TypefaceSpan
import androidx.ui.core.Density
import androidx.ui.core.em
import androidx.ui.core.px
import androidx.ui.core.sp
import androidx.ui.core.withDensity
import androidx.ui.engine.geometry.Offset
import androidx.ui.graphics.Color
import androidx.ui.graphics.Shadow
import androidx.ui.graphics.toArgb
import androidx.ui.text.AnnotatedString
import androidx.ui.text.FontTestData.Companion.BASIC_MEASURE_FONT
import androidx.ui.text.LocaleList
import androidx.ui.text.ParagraphConstraints
import androidx.ui.text.ParagraphStyle
import androidx.ui.text.TestFontResourceLoader
import androidx.ui.text.TextStyle
import androidx.ui.text.font.FontFamily
import androidx.ui.text.font.FontStyle
import androidx.ui.text.font.FontSynthesis
import androidx.ui.text.font.FontWeight
import androidx.ui.text.font.asFontFamily
import androidx.ui.text.matchers.assertThat
import androidx.ui.text.style.BaselineShift
import androidx.ui.text.style.TextAlign
import androidx.ui.text.style.TextDecoration
import androidx.ui.text.style.TextDirectionAlgorithm
import androidx.ui.text.style.TextGeometricTransform
import androidx.ui.text.style.TextIndent
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.math.ceil

@RunWith(JUnit4::class)
@SmallTest
class AndroidParagraphTest {
    // This sample font provides the following features:
    // 1. The width of most of visible characters equals to font size.
    // 2. The LTR/RTL characters are rendered as ▶/◀.
    // 3. The fontMetrics passed to TextPaint has descend - ascend equal to 1.2 * fontSize.
    private val fontFamily = BASIC_MEASURE_FONT.asFontFamily()
    private val defaultDensity = Density(density = 1f)
    private val context = InstrumentationRegistry.getInstrumentation().context

    @Test
    fun draw_with_newline_and_line_break_default_values() {
        withDensity(defaultDensity) {
            val fontSize = 50.sp
            for (text in arrayOf("abc\ndef", "\u05D0\u05D1\u05D2\n\u05D3\u05D4\u05D5")) {
                val paragraphAndroid = simpleParagraph(
                    text = text,
                    textStyle = TextStyle(
                        fontSize = fontSize,
                        fontFamily = fontFamily
                    ),
                    // 2 chars width
                    constraints = ParagraphConstraints(width = 2 * fontSize.toPx().value)
                )

                val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
                textPaint.textSize = fontSize.toPx().value
                textPaint.typeface = TypefaceAdapter().create(fontFamily)

                val layout = TextLayout(
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
        val textStyle = TextStyle(color = Color(0xFF0000FF))

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length)),
            constraints = ParagraphConstraints(width = 100.0f)
        )

        assertThat(paragraph.charSequence).hasSpan(ForegroundColorSpan::class, 0, text.length)
    }

    @Test
    fun testAnnotatedString_setColorOnPartOfText() {
        val text = "abcde"
        val textStyle = TextStyle(color = Color(0xFF0000FF))

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, "abc".length)),
            constraints = ParagraphConstraints(width = 100.0f)
        )

        assertThat(paragraph.charSequence).hasSpan(ForegroundColorSpan::class, 0, "abc".length)
    }

    @Test
    fun testAnnotatedString_setColorTwice_lastOneOverwrite() {
        val text = "abcde"
        val textStyle = TextStyle(color = Color(0xFF0000FF))
        val textStyleOverwrite = TextStyle(color = Color(0xFF00FF00))

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(
                AnnotatedString.Item(textStyle, 0, text.length),
                AnnotatedString.Item(textStyleOverwrite, 0, "abc".length)
            ),
            constraints = ParagraphConstraints(width = 100.0f)
        )

        assertThat(paragraph.charSequence).hasSpan(ForegroundColorSpan::class, 0, text.length)
        assertThat(paragraph.charSequence).hasSpan(ForegroundColorSpan::class, 0, "abc".length)
        assertThat(paragraph.charSequence).hasSpanOnTop(ForegroundColorSpan::class, 0, "abc".length)
    }

    @Test
    fun testStyle_setTextDecorationOnWholeText_withLineThrough() {
        val text = "abcde"
        val textStyle = TextStyle(decoration = TextDecoration.LineThrough)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length)),
            constraints = ParagraphConstraints(width = 100.0f)
        )

        assertThat(paragraph.charSequence.toString()).isEqualTo(text)
        assertThat(paragraph.charSequence).hasSpan(StrikethroughSpan::class, 0, text.length)
    }

    @Test
    fun testStyle_setTextDecorationOnWholeText_withUnderline() {
        val text = "abcde"
        val textStyle = TextStyle(decoration = TextDecoration.Underline)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length)),
            constraints = ParagraphConstraints(width = 100.0f)
        )

        assertThat(paragraph.charSequence.toString()).isEqualTo(text)
        assertThat(paragraph.charSequence).hasSpan(UnderlineSpan::class, 0, text.length)
    }

    @Test
    fun testStyle_setTextDecorationOnPartText_withLineThrough() {
        val text = "abcde"
        val textStyle = TextStyle(decoration = TextDecoration.LineThrough)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, "abc".length)),
            constraints = ParagraphConstraints(width = 100.0f)
        )

        assertThat(paragraph.charSequence.toString()).isEqualTo(text)
        assertThat(paragraph.charSequence).hasSpan(StrikethroughSpan::class, 0, "abc".length)
    }

    @Test
    fun testStyle_setTextDecorationOnPartText_withUnderline() {
        val text = "abcde"
        val textStyle = TextStyle(decoration = TextDecoration.Underline)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, "abc".length)),
            constraints = ParagraphConstraints(width = 100.0f)
        )

        assertThat(paragraph.charSequence.toString()).isEqualTo(text)
        assertThat(paragraph.charSequence).hasSpan(UnderlineSpan::class, 0, "abc".length)
    }

    @Test
    fun testStyle_setTextDecoration_withLineThroughAndUnderline() {
        val text = "abcde"
        val textStyle = TextStyle(
            decoration = TextDecoration.combine(
                listOf(TextDecoration.LineThrough, TextDecoration.Underline)
            )
        )

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, "abc".length)),
            constraints = ParagraphConstraints(width = 100.0f)
        )

        assertThat(paragraph.charSequence.toString()).isEqualTo(text)
        assertThat(paragraph.charSequence).hasSpan(UnderlineSpan::class, 0, "abc".length)
        assertThat(paragraph.charSequence).hasSpan(StrikethroughSpan::class, 0, "abc".length)
    }

    @Test
    fun testAnnotatedString_setFontSizeOnWholeText() {
        withDensity(defaultDensity) {
            val text = "abcde"
            val fontSize = 20.sp
            val paragraphWidth = text.length * fontSize.toPx().value
            val textStyle = TextStyle(fontSize = fontSize)

            val paragraph = simpleParagraph(
                text = text,
                textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length)),
                constraints = ParagraphConstraints(width = paragraphWidth)
            )

            assertThat(paragraph.charSequence).hasSpan(AbsoluteSizeSpan::class, 0, text.length)
        }
    }

    @Test
    fun testAnnotatedString_setFontSizeOnPartText() {
        withDensity(defaultDensity) {
            val text = "abcde"
            val fontSize = 20.sp
            val paragraphWidth = text.length * fontSize.toPx().value
            val textStyle = TextStyle(fontSize = fontSize)

            val paragraph = simpleParagraph(
                text = text,
                textStyles = listOf(AnnotatedString.Item(textStyle, 0, "abc".length)),
                constraints = ParagraphConstraints(width = paragraphWidth)
            )

            assertThat(paragraph.charSequence).hasSpan(AbsoluteSizeSpan::class, 0, "abc".length)
        }
    }

    @Test
    fun testAnnotatedString_setFontSizeTwice_lastOneOverwrite() {
        withDensity(defaultDensity) {
            val text = "abcde"
            val fontSize = 20.sp
            val fontSizeOverwrite = 30.sp
            val paragraphWidth = text.length * fontSizeOverwrite.toPx().value
            val textStyle = TextStyle(fontSize = fontSize)
            val textStyleOverwrite = TextStyle(fontSize = fontSizeOverwrite)

            val paragraph = simpleParagraph(
                text = text,
                textStyles = listOf(
                    AnnotatedString.Item(textStyle, 0, text.length),
                    AnnotatedString.Item(textStyleOverwrite, 0, "abc".length)
                ),
                constraints = ParagraphConstraints(width = paragraphWidth)
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
        val textStyle = TextStyle(fontSize = fontSizeScale)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length)),
            constraints = ParagraphConstraints(width = 100.0f)
        )

        assertThat(paragraph.charSequence).hasSpan(RelativeSizeSpan::class, 0, text.length) {
            it.sizeChange == fontSizeScale.value
        }
    }

    @Test
    fun testAnnotatedString_setFontSizeScaleOnPartText() {
        val text = "abcde"
        val fontSizeScale = 2.0f.em
        val textStyle = TextStyle(fontSize = fontSizeScale)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, "abc".length)),
            constraints = ParagraphConstraints(width = 100.0f)
        )

        assertThat(paragraph.charSequence).hasSpan(RelativeSizeSpan::class, 0, "abc".length) {
            it.sizeChange == fontSizeScale.value
        }
    }

    @Test
    fun testAnnotatedString_setLetterSpacingOnWholeText() {
        val text = "abcde"
        val letterSpacing = 2.0f
        val textStyle = TextStyle(letterSpacing = letterSpacing.em)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length)),
            constraints = ParagraphConstraints(width = 100.0f)
        )

        assertThat(paragraph.charSequence.toString()).isEqualTo(text)
        assertThat(paragraph.charSequence).hasSpan(LetterSpacingSpan::class, 0, text.length)
    }

    @Test
    fun testAnnotatedString_setLetterSpacingOnPartText() {
        val text = "abcde"
        val textStyle = TextStyle(letterSpacing = 2.em)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, "abc".length)),
            constraints = ParagraphConstraints(width = 100.0f)
        )

        assertThat(paragraph.charSequence.toString()).isEqualTo(text)
        assertThat(paragraph.charSequence).hasSpan(LetterSpacingSpan::class, 0, "abc".length)
    }

    @Test
    fun testAnnotatedString_setLetterSpacingTwice_lastOneOverwrite() {
        val text = "abcde"
        val textStyle = TextStyle(letterSpacing = 2.em)
        val textStyleOverwrite = TextStyle(letterSpacing = 3.em)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(
                AnnotatedString.Item(textStyle, 0, text.length),
                AnnotatedString.Item(textStyleOverwrite, 0, "abc".length)
            ),
            constraints = ParagraphConstraints(width = 100.0f)
        )

        assertThat(paragraph.charSequence.toString()).isEqualTo(text)
        assertThat(paragraph.charSequence).hasSpan(LetterSpacingSpan::class, 0, text.length)
        assertThat(paragraph.charSequence).hasSpan(LetterSpacingSpan::class, 0, "abc".length)
        assertThat(paragraph.charSequence).hasSpanOnTop(LetterSpacingSpan::class, 0, "abc".length)
    }

    @Test
    fun testAnnotatedString_setBackgroundOnWholeText() {
        val text = "abcde"
        val color = Color(0xFF0000FF)
        val textStyle = TextStyle(background = color)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length)),
            constraints = ParagraphConstraints(width = 100.0f)
        )

        assertThat(paragraph.charSequence.toString()).isEqualTo(text)
        assertThat(paragraph.charSequence)
            .hasSpan(BackgroundColorSpan::class, 0, text.length) { span ->
                span.backgroundColor == color.toArgb()
            }
    }

    @Test
    fun testAnnotatedString_setBackgroundOnPartText() {
        val text = "abcde"
        val color = Color(0xFF0000FF)
        val textStyle = TextStyle(background = color)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, "abc".length)),
            constraints = ParagraphConstraints(width = 100.0f)
        )

        assertThat(paragraph.charSequence.toString()).isEqualTo(text)
        assertThat(paragraph.charSequence)
            .hasSpan(BackgroundColorSpan::class, 0, "abc".length) { span ->
                span.backgroundColor == color.toArgb()
            }
    }

    @Test
    fun testAnnotatedString_setBackgroundTwice_lastOneOverwrite() {
        val text = "abcde"
        val color = Color(0xFF0000FF)
        val textStyle = TextStyle(background = color)
        val colorOverwrite = Color(0xFF00FF00)
        val textStyleOverwrite = TextStyle(background = colorOverwrite)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(
                AnnotatedString.Item(textStyle, 0, text.length),
                AnnotatedString.Item(textStyleOverwrite, 0, "abc".length)
            ),
            constraints = ParagraphConstraints(width = 100.0f)
        )

        assertThat(paragraph.charSequence.toString()).isEqualTo(text)
        assertThat(paragraph.charSequence)
            .hasSpan(BackgroundColorSpan::class, 0, text.length) { span ->
                span.backgroundColor == color.toArgb()
            }
        assertThat(paragraph.charSequence)
            .hasSpan(BackgroundColorSpan::class, 0, "abc".length) { span ->
                span.backgroundColor == colorOverwrite.toArgb()
            }
        assertThat(paragraph.charSequence)
            .hasSpanOnTop(BackgroundColorSpan::class, 0, "abc".length) { span ->
                span.backgroundColor == colorOverwrite.toArgb()
            }
    }

    @Test
    fun testAnnotatedString_setLocaleOnWholeText() {
        val text = "abcde"
        val localeList = LocaleList("en-US")
        val textStyle = TextStyle(localeList = localeList)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length)),
            constraints = ParagraphConstraints(width = 100.0f)
        )

        assertThat(paragraph.charSequence).hasSpan(LocaleSpan::class, 0, text.length)
    }

    @Test
    fun testAnnotatedString_setLocaleOnPartText() {
        val text = "abcde"
        val localeList = LocaleList("en-US")
        val textStyle = TextStyle(localeList = localeList)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, "abc".length)),
            constraints = ParagraphConstraints(width = 100.0f)
        )

        assertThat(paragraph.charSequence).hasSpan(LocaleSpan::class, 0, "abc".length)
    }

    @Test
    fun testAnnotatedString_setLocaleTwice_lastOneOverwrite() {
        val text = "abcde"
        val textStyle = TextStyle(localeList = LocaleList("en-US"))
        val textStyleOverwrite = TextStyle(localeList = LocaleList("ja-JP"))

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(
                AnnotatedString.Item(textStyle, 0, text.length),
                AnnotatedString.Item(textStyleOverwrite, 0, "abc".length)
            ),
            constraints = ParagraphConstraints(width = 100.0f)
        )

        assertThat(paragraph.charSequence).hasSpan(LocaleSpan::class, 0, text.length)
        assertThat(paragraph.charSequence).hasSpan(LocaleSpan::class, 0, "abc".length)
        assertThat(paragraph.charSequence).hasSpanOnTop(LocaleSpan::class, 0, "abc".length)
    }

    @Test
    fun testAnnotatedString_setBaselineShiftOnWholeText() {
        val text = "abcde"
        val textStyle = TextStyle(baselineShift = BaselineShift.Subscript)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length)),
            constraints = ParagraphConstraints(width = 100.0f) // width is not important
        )

        assertThat(paragraph.charSequence).hasSpan(BaselineShiftSpan::class, 0, text.length)
    }

    @Test
    fun testAnnotatedString_setBaselineShiftOnPartText() {
        val text = "abcde"
        val textStyle = TextStyle(baselineShift = BaselineShift.Superscript)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, "abc".length)),
            constraints = ParagraphConstraints(width = 100.0f) // width is not important
        )

        assertThat(paragraph.charSequence).hasSpan(BaselineShiftSpan::class, 0, "abc".length)
    }

    @Test
    fun testAnnotatedString_setBaselineShiftTwice_LastOneOnTop() {
        val text = "abcde"
        val textStyle = TextStyle(baselineShift = BaselineShift.Subscript)
        val textStyleOverwrite =
            TextStyle(baselineShift = BaselineShift.Superscript)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(
                AnnotatedString.Item(textStyle, 0, text.length),
                AnnotatedString.Item(textStyleOverwrite, 0, "abc".length)
            ),
            constraints = ParagraphConstraints(width = 100.0f) // width is not important
        )

        assertThat(paragraph.charSequence).hasSpan(BaselineShiftSpan::class, 0, text.length)
        assertThat(paragraph.charSequence).hasSpan(BaselineShiftSpan::class, 0, "abc".length)
        assertThat(paragraph.charSequence).hasSpanOnTop(BaselineShiftSpan::class, 0, "abc".length)
    }

    @Test
    fun testAnnotatedString_setTextGeometricTransformWithNull_noSpanSet() {
        val text = "abcde"
        val textStyle =
            TextStyle(textGeometricTransform = TextGeometricTransform(
                null,
                null
            )
            )

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length)),
            constraints = ParagraphConstraints(width = 100.0f) // width is not important
        )

        assertThat(paragraph.charSequence).spans(ScaleXSpan::class).isEmpty()
        assertThat(paragraph.charSequence).spans(SkewXSpan::class).isEmpty()
    }

    @Test
    fun testAnnotatedString_setTextGeometricTransformWithScaleX() {
        val text = "abcde"
        val scaleX = 0.5f
        val textStyle = TextStyle(
            textGeometricTransform = TextGeometricTransform(
                scaleX,
                null
            )
        )

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length)),
            constraints = ParagraphConstraints(width = 100.0f) // width is not important
        )

        assertThat(paragraph.charSequence).hasSpan(ScaleXSpan::class, 0, text.length) {
            it.scaleX == scaleX
        }
        assertThat(paragraph.charSequence).spans(SkewXSpan::class).isEmpty()
    }

    @Test
    fun testAnnotatedString_setTextGeometricTransformWithSkewX() {
        val text = "aa"
        val skewX = 1f
        val textStyle =
            TextStyle(textGeometricTransform = TextGeometricTransform(
                null,
                skewX
            )
            )

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length)),
            constraints = ParagraphConstraints(width = 100.0f) // width is not important
        )

        assertThat(paragraph.charSequence).hasSpan(SkewXSpan::class, 0, text.length) {
            it.skewX == skewX
        }
        assertThat(paragraph.charSequence).spans(ScaleXSpan::class).isEmpty()
    }

    @Test
    fun textIndent_onWholeParagraph() {
        val text = "abc\ndef"
        val firstLine = 40
        val restLine = 20

        val paragraph = simpleParagraph(
            text = text,
            textIndent = TextIndent(firstLine.sp, restLine.sp),
            constraints = ParagraphConstraints(width = 100.0f) // width is not important
        )

        assertThat(paragraph.charSequence)
            .hasSpan(LeadingMarginSpan.Standard::class, 0, text.length) {
                it.getLeadingMargin(true) == firstLine && it.getLeadingMargin(false) == restLine
            }
    }

    @Test
    fun testAnnotatedString_setShadow() {
        val text = "abcde"
        val color = Color(0xFF00FF00)
        val offset = Offset(1f, 2f)
        val radius = 3.px
        val textStyle = TextStyle(shadow = Shadow(color, offset, radius))

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(
                AnnotatedString.Item(textStyle, start = 0, end = text.length)
            ),
            constraints = ParagraphConstraints(width = 100.0f) // width is not important
        )

        assertThat(paragraph.charSequence)
            .hasSpan(ShadowSpan::class, start = 0, end = text.length) {
                return@hasSpan it.color == color.toArgb() &&
                        it.offsetX == offset.dx &&
                        it.offsetY == offset.dy &&
                        it.radius == radius.value
            }
    }

    @Test
    fun testAnnotatedString_setShadowTwice_lastOnTop() {
        val text = "abcde"
        val color = Color(0xFF00FF00)
        val offset = Offset(1f, 2f)
        val radius = 3.px
        val textStyle = TextStyle(shadow = Shadow(color, offset, radius))

        val colorOverwrite = Color(0xFF0000FF)
        val offsetOverwrite = Offset(3f, 2f)
        val radiusOverwrite = 1.px
        val textStyleOverwrite = TextStyle(
            shadow = Shadow(colorOverwrite, offsetOverwrite, radiusOverwrite)
        )

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(
                AnnotatedString.Item(textStyle, start = 0, end = text.length),
                AnnotatedString.Item(textStyleOverwrite, start = 0, end = "abc".length)
            ),
            constraints = ParagraphConstraints(width = 100.0f) // width is not important
        )

        assertThat(paragraph.charSequence)
            .hasSpan(ShadowSpan::class, start = 0, end = text.length) {
                return@hasSpan it.color == color.toArgb() &&
                        it.offsetX == offset.dx &&
                        it.offsetY == offset.dy &&
                        it.radius == radius.value
            }
        assertThat(paragraph.charSequence)
            .hasSpanOnTop(ShadowSpan::class, start = 0, end = "abc".length) {
                return@hasSpanOnTop it.color == colorOverwrite.toArgb() &&
                        it.offsetX == offsetOverwrite.dx &&
                        it.offsetY == offsetOverwrite.dy &&
                        it.radius == radiusOverwrite.value
            }
    }

    @Test
    fun testAnnotatedString_fontFamily_addsTypefaceSpanWithCorrectTypeface() {
        val text = "abcde"
        val textStyle = TextStyle(
            fontFamily = fontFamily,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Bold
        )
        val expectedTypeface = TypefaceAdapter().create(
            fontFamily = fontFamily,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Bold
        )
        val expectedStart = 0
        val expectedEnd = "abc".length

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(
                AnnotatedString.Item(
                    textStyle,
                    expectedStart,
                    expectedEnd
                )
            ),
            constraints = ParagraphConstraints(width = 100.0f)
        )

        assertThat(paragraph.charSequence.toString()).isEqualTo(text)
        assertThat(paragraph.charSequence)
            .hasSpan(TypefaceSpan::class, expectedStart, expectedEnd) { span ->
                span.typeface == expectedTypeface
            }
    }

    @Test
    fun testAnnotatedString_fontFamily_whenFontSynthesizeTurnedOff() {
        val text = "abcde"
        val textStyle = TextStyle(
            fontFamily = fontFamily,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Bold,
            fontSynthesis = FontSynthesis.None
        )
        val expectedTypeface = TypefaceAdapter().create(
            fontFamily = fontFamily,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Bold,
            fontSynthesis = FontSynthesis.None
        )
        val expectedStart = 0
        val expectedEnd = "abc".length

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(
                AnnotatedString.Item(
                    textStyle,
                    expectedStart,
                    expectedEnd
                )
            ),
            constraints = ParagraphConstraints(width = 100.0f)
        )

        assertThat(paragraph.charSequence.toString()).isEqualTo(text)
        assertThat(paragraph.charSequence)
            .hasSpan(TypefaceSpan::class, expectedStart, expectedEnd) { span ->
                span.typeface == expectedTypeface
            }
    }

    @Test
    fun testAnnotatedString_fontFeatureSetting_setSpanOnText() {
        val text = "abc"
        val fontFeatureSettings = "\"kern\" 0"
        val textStyle = TextStyle(fontFeatureSettings = fontFeatureSettings)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, "abc".length)),
            constraints = ParagraphConstraints(width = 100.0f) // width is not important
        )

        assertThat(paragraph.charSequence).hasSpan(FontFeatureSpan::class, 0, "abc".length) {
            it.fontFeatureSettings == fontFeatureSettings
        }
    }

    @Test
    fun testEmptyFontFamily() {
        val typefaceAdapter = mock<TypefaceAdapter>()
        val paragraph = simpleParagraph(
            text = "abc",
            typefaceAdapter = typefaceAdapter,
            constraints = ParagraphConstraints(width = Float.MAX_VALUE)
        )

        verify(typefaceAdapter, never()).create(
            fontFamily = any(),
            fontWeight = any(),
            fontStyle = any(),
            fontSynthesis = any()
        )
        assertThat(paragraph.textPaint.typeface).isNull()
    }

    @Test
    fun testEmptyFontFamily_withBoldFontWeightSelection() {
        val typefaceAdapter = spy(TypefaceAdapter())

        val paragraph = simpleParagraph(
            text = "abc",
            textStyle = TextStyle(
                fontFamily = null,
                fontWeight = FontWeight.Bold
            ),
            typefaceAdapter = typefaceAdapter,
            constraints = ParagraphConstraints(width = Float.MAX_VALUE)
        )

        verify(typefaceAdapter, times(1)).create(
            fontFamily = eq(null),
            fontWeight = eq(FontWeight.Bold),
            fontStyle = eq(FontStyle.Normal),
            fontSynthesis = eq(FontSynthesis.All)
        )

        val typeface = paragraph.textPaint.typeface
        assertThat(typeface).isNotNull()
        assertThat(typeface.isBold).isTrue()
        assertThat(typeface.isItalic).isFalse()
    }

    @Test
    fun testEmptyFontFamily_withFontStyleSelection() {
        val typefaceAdapter = spy(TypefaceAdapter())
        val paragraph = simpleParagraph(
            text = "abc",
            textStyle = TextStyle(
                fontFamily = null,
                fontStyle = FontStyle.Italic
            ),
            typefaceAdapter = typefaceAdapter,
            constraints = ParagraphConstraints(width = Float.MAX_VALUE)
        )

        verify(typefaceAdapter, times(1)).create(
            fontFamily = eq(null),
            fontWeight = eq(FontWeight.Normal),
            fontStyle = eq(FontStyle.Italic),
            fontSynthesis = eq(FontSynthesis.All)
        )

        val typeface = paragraph.textPaint.typeface
        assertThat(typeface).isNotNull()
        assertThat(typeface.isBold).isFalse()
        assertThat(typeface.isItalic).isTrue()
    }

    @Test
    fun testFontFamily_withGenericFamilyName() {
        val typefaceAdapter = spy(TypefaceAdapter())
        val fontFamily = FontFamily("sans-serif")

        val paragraph = simpleParagraph(
            text = "abc",
            textStyle = TextStyle(
                fontFamily = fontFamily
            ),
            typefaceAdapter = typefaceAdapter,
            constraints = ParagraphConstraints(width = Float.MAX_VALUE)
        )

        verify(typefaceAdapter, times(1)).create(
            fontFamily = eq(fontFamily),
            fontWeight = eq(FontWeight.Normal),
            fontStyle = eq(FontStyle.Normal),
            fontSynthesis = eq(FontSynthesis.All)
        )

        val typeface = paragraph.textPaint.typeface
        assertThat(typeface).isNotNull()
        assertThat(typeface.isBold).isFalse()
        assertThat(typeface.isItalic).isFalse()
    }

    @Test
    fun testFontFamily_withCustomFont() {
        val typefaceAdapter = spy(TypefaceAdapter())
        val paragraph = simpleParagraph(
            text = "abc",
            textStyle = TextStyle(
                fontFamily = fontFamily
            ),
            typefaceAdapter = typefaceAdapter,
            constraints = ParagraphConstraints(width = Float.MAX_VALUE)
        )

        verify(typefaceAdapter, times(1)).create(
            fontFamily = eq(fontFamily),
            fontWeight = eq(FontWeight.Normal),
            fontStyle = eq(FontStyle.Normal),
            fontSynthesis = eq(FontSynthesis.All)
        )
        val typeface = paragraph.textPaint.typeface
        assertThat(typeface.isBold).isFalse()
        assertThat(typeface.isItalic).isFalse()
    }

    @Test
    fun testEllipsis_withMaxLineEqualsNull_doesNotEllipsis() {
        withDensity(defaultDensity) {
            val text = "abc"
            val fontSize = 20.sp
            val paragraphWidth = (text.length - 1) * fontSize.toPx().value
            val paragraph = simpleParagraph(
                text = text,
                textStyle = TextStyle(
                    fontFamily = fontFamily,
                    fontSize = fontSize
                ),
                ellipsis = true,
                constraints = ParagraphConstraints(width = paragraphWidth)
            )

            for (i in 0 until paragraph.lineCount) {
                assertThat(paragraph.isEllipsisApplied(i)).isFalse()
            }
        }
    }

    @Test
    fun testEllipsis_withMaxLinesLessThanTextLines_doesEllipsis() {
        withDensity(defaultDensity) {
            val text = "abcde"
            val fontSize = 100.sp
            // Note that on API 21, if the next line only contains 1 character, ellipsis won't work
            val paragraphWidth = (text.length - 1.5f) * fontSize.toPx().value
            val paragraph = simpleParagraph(
                text = text,
                ellipsis = true,
                maxLines = 1,
                textStyle = TextStyle(
                    fontFamily = fontFamily,
                    fontSize = fontSize
                ),
                constraints = ParagraphConstraints(width = paragraphWidth)
            )

            assertThat(paragraph.isEllipsisApplied(0)).isTrue()
        }
    }

    @Test
    fun testEllipsis_withMaxLinesMoreThanTextLines_doesNotEllipsis() {
        withDensity(defaultDensity) {
            val text = "abc"
            val fontSize = 100.sp
            val paragraphWidth = (text.length - 1) * fontSize.toPx().value
            val maxLines = ceil(text.length * fontSize.toPx().value / paragraphWidth).toInt()
            val paragraph = simpleParagraph(
                text = text,
                ellipsis = true,
                maxLines = maxLines,
                textStyle = TextStyle(
                    fontFamily = fontFamily,
                    fontSize = fontSize
                ),
                constraints = ParagraphConstraints(width = paragraphWidth)
            )

            for (i in 0 until paragraph.lineCount) {
                assertThat(paragraph.isEllipsisApplied(i)).isFalse()
            }
        }
    }

    @Test
    fun testTextStyle_fontSize_appliedOnTextPaint() {
        withDensity(defaultDensity) {
            val fontSize = 100.sp
            val paragraph = simpleParagraph(
                text = "",
                textStyle = TextStyle(fontSize = fontSize),
                constraints = ParagraphConstraints(width = 0.0f)
            )

            assertThat(paragraph.textPaint.textSize).isEqualTo(fontSize.toPx().value)
        }
    }

    @Test
    fun testTextStyle_locale_appliedOnTextPaint() {
        val platformLocale = java.util.Locale.JAPANESE
        val localeList = LocaleList(platformLocale.toLanguageTag())

        val paragraph = simpleParagraph(
            text = "",
            textStyle = TextStyle(localeList = localeList),
            constraints = ParagraphConstraints(width = 0.0f)
        )

        assertThat(paragraph.textPaint.textLocale.language).isEqualTo(platformLocale.language)
        assertThat(paragraph.textPaint.textLocale.country).isEqualTo(platformLocale.country)
    }

    @Test
    fun testTextStyle_color_appliedOnTextPaint() {
        val color = Color(0x12345678)
        val paragraph = simpleParagraph(
            text = "",
            textStyle = TextStyle(color = color),
            constraints = ParagraphConstraints(width = 0.0f)
        )

        assertThat(paragraph.textPaint.color).isEqualTo(color.toArgb())
    }

    @Test
    fun testTextStyle_letterSpacing_appliedOnTextPaint() {
        val letterSpacing = 2
        val paragraph = simpleParagraph(
            text = "",
            textStyle = TextStyle(letterSpacing = letterSpacing.em),
            constraints = ParagraphConstraints(width = 0.0f)
        )

        assertThat(paragraph.textPaint.letterSpacing).isEqualTo((letterSpacing))
    }

    @Test
    fun testTextStyle_fontFeatureSettings_appliedOnTextPaint() {
        val fontFeatureSettings = "\"kern\" 0"
        val paragraph = simpleParagraph(
            text = "",
            textStyle = TextStyle(fontFeatureSettings = fontFeatureSettings),
            constraints = ParagraphConstraints(width = 0.0f)
        )

        assertThat(paragraph.textPaint.fontFeatureSettings).isEqualTo(fontFeatureSettings)
    }

    @Test
    fun testTextStyle_scaleX_appliedOnTextPaint() {
        val scaleX = 0.5f
        val paragraph = simpleParagraph(
            text = "",
            textStyle = TextStyle(
                textGeometricTransform = TextGeometricTransform(
                    scaleX = scaleX
                )
            ),
            constraints = ParagraphConstraints(width = 0.0f)
        )

        assertThat(paragraph.textPaint.textScaleX).isEqualTo(scaleX)
    }

    @Test
    fun testTextStyle_skewX_appliedOnTextPaint() {
        val skewX = 0.5f
        val paragraph = simpleParagraph(
            text = "",
            textStyle = TextStyle(
                textGeometricTransform = TextGeometricTransform(
                    skewX = skewX
                )
            ),
            constraints = ParagraphConstraints(width = 0.0f)
        )

        assertThat(paragraph.textPaint.textSkewX).isEqualTo(skewX)
    }

    @Test
    fun testTextStyle_decoration_underline_appliedOnTextPaint() {
        val paragraph = simpleParagraph(
            text = "",
            textStyle = TextStyle(decoration = TextDecoration.Underline),
            constraints = ParagraphConstraints(width = 0.0f)
        )

        assertThat(paragraph.textPaint.isUnderlineText).isTrue()
    }

    @Test
    fun testTextStyle_decoration_lineThrough_appliedOnTextPaint() {
        val paragraph = simpleParagraph(
            text = "",
            textStyle = TextStyle(decoration = TextDecoration.LineThrough),
            constraints = ParagraphConstraints(width = 0.0f)
        )

        assertThat(paragraph.textPaint.isStrikeThruText).isTrue()
    }

    @Test
    fun testTextStyle_background_appliedAsSpan() {
        // bgColor is reset in the Android Layout constructor.
        // therefore we cannot apply them on paint, have to use spans.
        val text = "abc"
        val color = Color(0x12345678)
        val paragraph = simpleParagraph(
            text = text,
            textStyle = TextStyle(background = color),
            constraints = ParagraphConstraints(width = 0.0f)
        )

        assertThat(paragraph.charSequence)
            .hasSpan(BackgroundColorSpan::class, 0, text.length) { span ->
                span.backgroundColor == color.toArgb()
            }
    }

    @Test
    fun testTextStyle_baselineShift_appliedAsSpan() {
        // baselineShift is reset in the Android Layout constructor.
        // therefore we cannot apply them on paint, have to use spans.
        val text = "abc"
        val baselineShift = BaselineShift.Subscript
        val paragraph = simpleParagraph(
            text = text,
            textStyle = TextStyle(baselineShift = baselineShift),
            constraints = ParagraphConstraints(width = 0.0f)
        )

        assertThat(paragraph.charSequence)
            .hasSpan(BaselineShiftSpan::class, 0, text.length) { span ->
                span.multiplier == BaselineShift.Subscript.multiplier
            }
    }

    @Test
    fun locale_isDefaultLocaleIfNotProvided() {
        val text = "abc"
        val paragraph = simpleParagraph(
            text = text,
            constraints = ParagraphConstraints(width = Float.MAX_VALUE)
        )

        assertThat(paragraph.textLocale.toLanguageTag())
            .isEqualTo(java.util.Locale.getDefault().toLanguageTag())
    }

    @Test
    fun locale_isSetOnParagraphImpl_enUS() {
        val localeList = LocaleList("en-US")
        val text = "abc"
        val paragraph = simpleParagraph(
            text = text,
            textStyle = TextStyle(localeList = localeList),
            constraints = ParagraphConstraints(width = Float.MAX_VALUE)
        )

        assertThat(paragraph.textLocale.toLanguageTag()).isEqualTo("en-US")
    }

    @Test
    fun locale_isSetOnParagraphImpl_jpJP() {
        val localeList = LocaleList("ja-JP")
        val text = "abc"
        val paragraph = simpleParagraph(
            text = text,
            textStyle = TextStyle(localeList = localeList),
            constraints = ParagraphConstraints(width = Float.MAX_VALUE)
        )

        assertThat(paragraph.textLocale.toLanguageTag()).isEqualTo("ja-JP")
    }

    @Test
    fun locale_noCountryCode_isSetOnParagraphImpl() {
        val localeList = LocaleList("ja")
        val text = "abc"
        val paragraph = simpleParagraph(
            text = text,
            textStyle = TextStyle(localeList = localeList),
            constraints = ParagraphConstraints(width = Float.MAX_VALUE)
        )

        assertThat(paragraph.textLocale.toLanguageTag()).isEqualTo("ja")
    }

    @Test
    fun floatingWidth() {
        val floatWidth = 1.3f
        val paragraph = simpleParagraph(
            text = "Hello, World",
            constraints = ParagraphConstraints(floatWidth)
        )

        assertThat(floatWidth).isEqualTo(paragraph.width)
    }

    private fun simpleParagraph(
        text: String = "",
        textStyles: List<AnnotatedString.Item<TextStyle>> = listOf(),
        textIndent: TextIndent? = null,
        textAlign: TextAlign? = null,
        textDirectionAlgorithm: TextDirectionAlgorithm? = TextDirectionAlgorithm.ContentOrLtr,
        ellipsis: Boolean? = null,
        maxLines: Int? = null,
        constraints: ParagraphConstraints,
        textStyle: TextStyle? = null,
        typefaceAdapter: TypefaceAdapter = TypefaceAdapter()
    ): AndroidParagraph {
        return AndroidParagraph(
            text = text,
            textStyles = textStyles,
            typefaceAdapter = typefaceAdapter,
            style = TextStyle().merge(textStyle),
            paragraphStyle = ParagraphStyle(
                textAlign = textAlign,
                textIndent = textIndent,
                textDirectionAlgorithm = textDirectionAlgorithm
            ),
            maxLines = maxLines,
            ellipsis = ellipsis,
            constraints = constraints,
            density = Density(density = 1f)
        )
    }

    private fun TypefaceAdapter() = TypefaceAdapter(
        resourceLoader = TestFontResourceLoader(context)
    )
}