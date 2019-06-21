package androidx.ui.engine.text.platform

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
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.text.StaticLayoutCompat
import androidx.text.style.BaselineShiftSpan
import androidx.text.style.FontFeatureSpan
import androidx.text.style.LetterSpacingSpan
import androidx.text.style.ShadowSpan
import androidx.text.style.SkewXSpan
import androidx.text.style.TypefaceSpan
import androidx.text.style.WordSpacingSpan
import androidx.ui.core.px
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.text.BaselineShift
import androidx.ui.engine.text.FontStyle
import androidx.ui.engine.text.FontSynthesis
import androidx.ui.engine.text.FontTestData.Companion.BASIC_MEASURE_FONT
import androidx.ui.engine.text.FontWeight
import androidx.ui.engine.text.ParagraphStyle
import androidx.ui.engine.text.TextAlign
import androidx.ui.engine.text.TextDecoration
import androidx.ui.engine.text.TextGeometricTransform
import androidx.ui.engine.text.TextIndent
import androidx.ui.engine.text.font.FontFamily
import androidx.ui.engine.text.font.asFontFamily
import androidx.ui.engine.window.Locale
import androidx.ui.matchers.equalToBitmap
import androidx.ui.matchers.hasSpan
import androidx.ui.matchers.hasSpanOnTop
import androidx.ui.graphics.Color
import androidx.ui.painting.Shadow
import androidx.ui.painting.AnnotatedString
import androidx.ui.painting.TextStyle
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.nullValue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.math.ceil

@RunWith(JUnit4::class)
@SmallTest
class ParagraphAndroidTest {
    private lateinit var fontFamily: FontFamily

    @Before
    fun setup() {
        // This sample font provides the following features:
        // 1. The width of most of visible characters equals to font size.
        // 2. The LTR/RTL characters are rendered as ▶/◀.
        // 3. The fontMetrics passed to TextPaint has descend - ascend equal to 1.2 * fontSize.
        fontFamily = BASIC_MEASURE_FONT.asFontFamily()
        fontFamily.context = InstrumentationRegistry.getInstrumentation().context
    }

    @Test
    fun draw_with_newline_and_line_break_default_values() {
        val fontSize = 50.0f
        for (text in arrayOf("abc\ndef", "\u05D0\u05D1\u05D2\n\u05D3\u05D4\u05D5")) {
            val paragraphAndroid = simpleParagraph(
                text = text,
                fontSize = fontSize,
                fontFamily = fontFamily
            )

            // 2 chars width
            paragraphAndroid.layout(width = 2 * fontSize)

            val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
            textPaint.textSize = fontSize
            textPaint.typeface = TypefaceAdapter().create(fontFamily)

            val staticLayout = StaticLayoutCompat.Builder(
                text,
                textPaint,
                ceil(paragraphAndroid.width).toInt()
            )
                .setEllipsizedWidth(ceil(paragraphAndroid.width).toInt())
                .build()
            assertThat(paragraphAndroid.bitmap(), equalToBitmap(staticLayout.bitmap()))
        }
    }

    @Test
    fun textStyle_setColorOnWholeText() {
        val text = "abcde"
        val textStyle = TextStyle(color = Color(0xFF0000FF.toInt()))

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length))
        )
        paragraph.layout(100.0f)

        assertThat(paragraph.underlyingText, hasSpan(ForegroundColorSpan::class, 0, text.length))
    }

    @Test
    fun textStyle_setColorOnPartOfText() {
        val text = "abcde"
        val textStyle = TextStyle(color = Color(0xFF0000FF.toInt()))

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, "abc".length))
        )
        paragraph.layout(100.0f)

        assertThat(paragraph.underlyingText, hasSpan(ForegroundColorSpan::class, 0, "abc".length))
    }

    @Test
    fun textStyle_setColorTwice_lastOneOverwrite() {
        val text = "abcde"
        val textStyle = TextStyle(color = Color(0xFF0000FF.toInt()))
        val textStyleOverwrite = TextStyle(color = Color(0xFF00FF00.toInt()))

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(
                AnnotatedString.Item(textStyle, 0, text.length),
                AnnotatedString.Item(textStyleOverwrite, 0, "abc".length)
            )
        )
        paragraph.layout(100.0f)

        assertThat(paragraph.underlyingText, hasSpan(ForegroundColorSpan::class, 0, text.length))
        assertThat(paragraph.underlyingText, hasSpan(ForegroundColorSpan::class, 0, "abc".length))
        assertThat(
            paragraph.underlyingText,
            hasSpanOnTop(ForegroundColorSpan::class, 0, "abc".length)
        )
    }

    @Test
    fun testStyle_setTextDecorationOnWholeText_withLineThrough() {
        val text = "abcde"
        val textStyle = TextStyle(decoration = TextDecoration.LineThrough)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length))
        )
        paragraph.layout(100.0f)

        assertThat(paragraph.underlyingText.toString(), equalTo(text))
        assertThat(paragraph.underlyingText, hasSpan(StrikethroughSpan::class, 0, text.length))
    }

    @Test
    fun testStyle_setTextDecorationOnWholeText_withUnderline() {
        val text = "abcde"
        val textStyle = TextStyle(decoration = TextDecoration.Underline)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length))
        )
        paragraph.layout(100.0f)

        assertThat(paragraph.underlyingText.toString(), equalTo(text))
        assertThat(paragraph.underlyingText, hasSpan(UnderlineSpan::class, 0, text.length))
    }

    @Test
    fun testStyle_setTextDecorationOnPartText_withLineThrough() {
        val text = "abcde"
        val textStyle = TextStyle(decoration = TextDecoration.LineThrough)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, "abc".length))
        )
        paragraph.layout(100.0f)

        assertThat(paragraph.underlyingText.toString(), equalTo(text))
        assertThat(paragraph.underlyingText, hasSpan(StrikethroughSpan::class, 0, "abc".length))
    }

    @Test
    fun testStyle_setTextDecorationOnPartText_withUnderline() {
        val text = "abcde"
        val textStyle = TextStyle(decoration = TextDecoration.Underline)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, "abc".length))
        )
        paragraph.layout(100.0f)

        assertThat(paragraph.underlyingText.toString(), equalTo(text))
        assertThat(paragraph.underlyingText, hasSpan(UnderlineSpan::class, 0, "abc".length))
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
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, "abc".length))
        )
        paragraph.layout(100.0f)

        assertThat(paragraph.underlyingText.toString(), equalTo(text))
        assertThat(paragraph.underlyingText, hasSpan(UnderlineSpan::class, 0, "abc".length))
        assertThat(paragraph.underlyingText, hasSpan(StrikethroughSpan::class, 0, "abc".length))
    }

    @Test
    fun textStyle_setFontSizeOnWholeText() {
        val text = "abcde"
        val fontSize = 20.0f
        val paragraphWidth = text.length * fontSize
        val textStyle = TextStyle(fontSize = fontSize)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length))
        )
        paragraph.layout(paragraphWidth)

        assertThat(paragraph.underlyingText, hasSpan(AbsoluteSizeSpan::class, 0, text.length))
    }

    @Test
    fun textStyle_setFontSizeOnPartText() {
        val text = "abcde"
        val fontSize = 20.0f
        val paragraphWidth = text.length * fontSize
        val textStyle = TextStyle(fontSize = fontSize)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, "abc".length))
        )
        paragraph.layout(paragraphWidth)

        assertThat(paragraph.underlyingText, hasSpan(AbsoluteSizeSpan::class, 0, "abc".length))
    }

    @Test
    fun textStyle_setFontSizeTwice_lastOneOverwrite() {
        val text = "abcde"
        val fontSize = 20.0f
        val fontSizeOverwrite = 30.0f
        val paragraphWidth = text.length * fontSizeOverwrite
        val textStyle = TextStyle(fontSize = fontSize)
        val textStyleOverwrite = TextStyle(fontSize = fontSizeOverwrite)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(
                AnnotatedString.Item(textStyle, 0, text.length),
                AnnotatedString.Item(textStyleOverwrite, 0, "abc".length)
            )
        )
        paragraph.layout(paragraphWidth)

        assertThat(paragraph.underlyingText, hasSpan(AbsoluteSizeSpan::class, 0, text.length))
        assertThat(paragraph.underlyingText, hasSpan(AbsoluteSizeSpan::class, 0, "abc".length))
        assertThat(
            paragraph.underlyingText,
            hasSpanOnTop(AbsoluteSizeSpan::class, 0, "abc".length)
        )
    }

    @Test
    fun textStyle_setFontSizeScaleOnWholeText() {
        val text = "abcde"
        val fontSizeScale = 2.0f
        val textStyle = TextStyle(fontSizeScale = fontSizeScale)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length))
        )
        paragraph.layout(100f)

        assertThat(
            paragraph.underlyingText,
            hasSpan(RelativeSizeSpan::class, 0, text.length) {
                it.sizeChange == fontSizeScale
            }
        )
    }

    @Test
    fun textStyle_setFontSizeScaleOnPartText() {
        val text = "abcde"
        val fontSizeScale = 2.0f
        val textStyle = TextStyle(fontSizeScale = fontSizeScale)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, "abc".length))
        )
        paragraph.layout(100f)

        assertThat(
            paragraph.underlyingText,
            hasSpan(RelativeSizeSpan::class, 0, "abc".length) {
                it.sizeChange == fontSizeScale
            }
        )
    }

    @Test
    fun textStyle_setLetterSpacingOnWholeText() {
        val text = "abcde"
        val letterSpacing = 2.0f
        val textStyle = TextStyle(letterSpacing = letterSpacing)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length))
        )
        paragraph.layout(100.0f)
        assertThat(paragraph.underlyingText.toString(), equalTo(text))
        assertThat(paragraph.underlyingText, hasSpan(LetterSpacingSpan::class, 0, text.length))
    }

    @Test
    fun textStyle_setLetterSpacingOnPartText() {
        val text = "abcde"
        val textStyle = TextStyle(letterSpacing = 2.0f)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, "abc".length))
        )
        paragraph.layout(100.0f)
        assertThat(paragraph.underlyingText.toString(), equalTo(text))
        assertThat(paragraph.underlyingText, hasSpan(LetterSpacingSpan::class, 0, "abc".length))
    }

    @Test
    fun textStyle_setLetterSpacingTwice_lastOneOverwrite() {
        val text = "abcde"
        val textStyle = TextStyle(letterSpacing = 2.0f)
        val textStyleOverwrite = TextStyle(letterSpacing = 3.0f)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(
                AnnotatedString.Item(textStyle, 0, text.length),
                AnnotatedString.Item(textStyleOverwrite, 0, "abc".length)
            )
        )
        paragraph.layout(100.0f)
        assertThat(paragraph.underlyingText.toString(), equalTo(text))
        assertThat(paragraph.underlyingText, hasSpan(LetterSpacingSpan::class, 0, text.length))
        assertThat(paragraph.underlyingText, hasSpan(LetterSpacingSpan::class, 0, "abc".length))
        assertThat(
            paragraph.underlyingText,
            hasSpanOnTop(LetterSpacingSpan::class, 0, "abc".length)
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    fun textStyle_setWordSpacingOnWholeText() {
        val text = "ab cd"
        val wordSpacing = 2.0f
        val textStyle = TextStyle(wordSpacing = wordSpacing)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length))
        )
        // Notice that the width doesn't matter for this test.
        paragraph.layout(100.0f)

        assertThat(paragraph.underlyingText.toString(), equalTo(text))
        assertThat(
            paragraph.underlyingText,
            hasSpan(WordSpacingSpan::class, 0, text.length) { span ->
                span.wordSpacing == wordSpacing.toFloat()
            }
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    fun textStyle_setWordSpacingOnPartText() {
        val text = "abc d"
        val wordSpacing = 2.0f
        val textStyle = TextStyle(wordSpacing = wordSpacing)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, "abc".length))
        )
        // Notice that the width doesn't matter for this test.
        paragraph.layout(100.0f)

        assertThat(paragraph.underlyingText.toString(), equalTo(text))
        assertThat(
            paragraph.underlyingText,
            hasSpan(WordSpacingSpan::class, 0, "abc".length) { span ->
                span.wordSpacing == wordSpacing.toFloat()
            }
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    fun textStyle_setWordSpacingTwice_lastOneOverwrite() {
        val text = "abc d"
        val wordSpacing = 2.0f
        val textStyle = TextStyle(wordSpacing = wordSpacing)
        val wordSpacingOverwrite = 3.0f
        val textStyleOverwrite = TextStyle(wordSpacing = wordSpacingOverwrite)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(
                AnnotatedString.Item(textStyle, 0, text.length),
                AnnotatedString.Item(textStyleOverwrite, 0, "abc".length)
            )
        )
        // Notice that the width doesn't matter for this test.
        paragraph.layout(100.0f)

        assertThat(paragraph.underlyingText.toString(), equalTo(text))
        assertThat(
            paragraph.underlyingText,
            hasSpan(WordSpacingSpan::class, 0, text.length) { span ->
                span.wordSpacing == wordSpacing.toFloat()
            }
        )
        assertThat(
            paragraph.underlyingText,
            hasSpan(WordSpacingSpan::class, 0, "abc".length) { span ->
                span.wordSpacing == wordSpacingOverwrite.toFloat()
            }
        )
        assertThat(
            paragraph.underlyingText,
            hasSpanOnTop(WordSpacingSpan::class, 0, "abc".length) { span ->
                span.wordSpacing == wordSpacingOverwrite.toFloat()
            }
        )
    }

    @Test
    fun textStyle_setBackgroundOnWholeText() {
        val text = "abcde"
        val color = Color(0xFF0000FF.toInt())
        val textStyle = TextStyle(background = color)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length))
        )
        paragraph.layout(100.0f)

        assertThat(paragraph.underlyingText.toString(), equalTo(text))
        assertThat(paragraph.underlyingText,
            hasSpan(BackgroundColorSpan::class, 0, text.length) { span ->
                span.backgroundColor == color.toArgb()
            }
        )
    }

    @Test
    fun textStyle_setBackgroundOnPartText() {
        val text = "abcde"
        val color = Color(0xFF0000FF.toInt())
        val textStyle = TextStyle(background = color)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, "abc".length))
        )
        paragraph.layout(100.0f)

        assertThat(paragraph.underlyingText.toString(), equalTo(text))
        assertThat(paragraph.underlyingText,
            hasSpan(BackgroundColorSpan::class, 0, "abc".length) { span ->
                span.backgroundColor == color.toArgb()
            }
        )
    }

    @Test
    fun textStyle_setBackgroundTwice_lastOneOverwrite() {
        val text = "abcde"
        val color = Color(0xFF0000FF.toInt())
        val textStyle = TextStyle(background = color)
        val colorOverwrite = Color(0xFF00FF00.toInt())
        val textStyleOverwrite = TextStyle(background = colorOverwrite)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(
                AnnotatedString.Item(textStyle, 0, text.length),
                AnnotatedString.Item(textStyleOverwrite, 0, "abc".length)
            )
        )
        paragraph.layout(100.0f)

        assertThat(paragraph.underlyingText.toString(), equalTo(text))
        assertThat(paragraph.underlyingText,
            hasSpan(BackgroundColorSpan::class, 0, text.length) { span ->
                span.backgroundColor == color.toArgb()
            }
        )
        assertThat(paragraph.underlyingText,
            hasSpan(BackgroundColorSpan::class, 0, "abc".length) { span ->
                span.backgroundColor == colorOverwrite.toArgb()
            }
        )
        assertThat(
            paragraph.underlyingText,
            hasSpanOnTop(BackgroundColorSpan::class, 0, "abc".length) { span ->
                span.backgroundColor == colorOverwrite.toArgb()
            }
        )
    }

    @Test
    fun textStyle_setLocaleOnWholeText() {
        val text = "abcde"
        val locale = Locale("en", "US")
        val textStyle = TextStyle(locale = locale)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length))
        )
        paragraph.layout(100.0f)

        assertThat(paragraph.underlyingText, hasSpan(LocaleSpan::class, 0, text.length))
    }

    @Test
    fun textStyle_setLocaleOnPartText() {
        val text = "abcde"
        val locale = Locale("en", "US")
        val textStyle = TextStyle(locale = locale)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, "abc".length))
        )
        paragraph.layout(100.0f)

        assertThat(paragraph.underlyingText, hasSpan(LocaleSpan::class, 0, "abc".length))
    }

    @Test
    fun textStyle_setLocaleTwice_lastOneOverwrite() {
        val text = "abcde"
        val textStyle = TextStyle(locale = Locale("en", "US"))
        val textStyleOverwrite = TextStyle(locale = Locale("ja", "JP"))

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(
                AnnotatedString.Item(textStyle, 0, text.length),
                AnnotatedString.Item(textStyleOverwrite, 0, "abc".length)
            )
        )
        paragraph.layout(100.0f)

        assertThat(paragraph.underlyingText, hasSpan(LocaleSpan::class, 0, text.length))
        assertThat(paragraph.underlyingText, hasSpan(LocaleSpan::class, 0, "abc".length))
        assertThat(
            paragraph.underlyingText,
            hasSpanOnTop(LocaleSpan::class, 0, "abc".length)
        )
    }

    @Test
    fun textStyle_setBaselineShiftOnWholeText() {
        val text = "abcde"
        val textStyle = TextStyle(baselineShift = BaselineShift.Subscript)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length))
        )
        // width is not important
        paragraph.layout(100.0f)

        assertThat(paragraph.underlyingText, hasSpan(BaselineShiftSpan::class, 0, text.length))
    }

    @Test
    fun textStyle_setBaselineShiftOnPartText() {
        val text = "abcde"
        val textStyle = TextStyle(baselineShift = BaselineShift.Superscript)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, "abc".length))
        )
        // width is not important
        paragraph.layout(100.0f)

        assertThat(paragraph.underlyingText, hasSpan(BaselineShiftSpan::class, 0, "abc".length))
    }

    @Test
    fun textStyle_setBaselineShiftTwice_LastOneOnTop() {
        val text = "abcde"
        val textStyle = TextStyle(baselineShift = BaselineShift.Subscript)
        val textStyleOverwrite = TextStyle(baselineShift = BaselineShift.Superscript)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(
                AnnotatedString.Item(textStyle, 0, text.length),
                AnnotatedString.Item(textStyleOverwrite, 0, "abc".length)
            )
        )
        // width is not important
        paragraph.layout(100.0f)

        assertThat(paragraph.underlyingText, hasSpan(BaselineShiftSpan::class, 0, text.length))
        assertThat(paragraph.underlyingText, hasSpan(BaselineShiftSpan::class, 0, "abc".length))
        assertThat(
            paragraph.underlyingText,
            hasSpanOnTop(BaselineShiftSpan::class, 0, "abc".length)
        )
    }

    @Test
    fun textStyle_setTextGeometricTransformWithNull_noSpanSet() {
        val text = "abcde"
        val textStyle = TextStyle(textGeometricTransform = TextGeometricTransform(null, null))

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length))
        )
        // width is not important
        paragraph.layout(100.0f)

        assertThat(paragraph.underlyingText, not(hasSpan(ScaleXSpan::class, 0, text.length)))
        assertThat(paragraph.underlyingText, not(hasSpan(SkewXSpan::class, 0, text.length)))
    }

    @Test
    fun textStyle_setTextGeometricTransformWithScaleX() {
        val text = "abcde"
        val scaleX = 0.5f
        val textStyle = TextStyle(textGeometricTransform = TextGeometricTransform(scaleX, null))

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length))
        )
        // width is not important
        paragraph.layout(100.0f)

        assertThat(
            paragraph.underlyingText,
            hasSpan(ScaleXSpan::class, 0, text.length) { it.scaleX == scaleX }
        )
        assertThat(paragraph.underlyingText, not(hasSpan(SkewXSpan::class, 0, text.length)))
    }

    @Test
    fun textStyle_setTextGeometricTransformWithSkewX() {
        val text = "aa"
        val skewX = 1f
        val textStyle = TextStyle(textGeometricTransform = TextGeometricTransform(null, skewX))

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length))
        )
        // width is not important
        paragraph.layout(100.0f)

        assertThat(
            paragraph.underlyingText,
            hasSpan(SkewXSpan::class, 0, text.length) { it.skewX == skewX }
        )
        assertThat(paragraph.underlyingText, not(hasSpan(ScaleXSpan::class, 0, text.length)))
    }

    @Test
    fun textIndent_onWholeParagraph() {
        val text = "abc\ndef"
        val firstLine = 40
        val restLine = 20

        val paragraph = simpleParagraph(
            text = text,
            textIndent = TextIndent(firstLine.px, restLine.px)
        )
        // width is not important
        paragraph.layout(100.0f)

        assertThat(
            paragraph.underlyingText,
            hasSpan(LeadingMarginSpan.Standard::class, 0, text.length) {
                it.getLeadingMargin(true) == firstLine && it.getLeadingMargin(false) == restLine
            }
        )
    }

    @Test
    fun textStyle_setShadow() {
        val text = "abcde"
        val color = Color(0xFF00FF00.toInt())
        val offset = Offset(1f, 2f)
        val radius = 3.px
        val textStyle = TextStyle(shadow = Shadow(color, offset, radius))

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(
                AnnotatedString.Item(textStyle, start = 0, end = text.length)
            )
        )
        // width is not important
        paragraph.layout(100.0f)

        assertThat(
            paragraph.underlyingText,
            hasSpan(ShadowSpan::class, start = 0, end = text.length) {
                return@hasSpan it.color == color.toArgb() &&
                        it.offsetX == offset.dx &&
                        it.offsetY == offset.dy &&
                        it.radius == radius.value
            }
        )
    }

    @Test
    fun textStyle_setShadowTwice_lastOnTop() {
        val text = "abcde"
        val color = Color(0xFF00FF00.toInt())
        val offset = Offset(1f, 2f)
        val radius = 3.px
        val textStyle = TextStyle(shadow = Shadow(color, offset, radius))

        val colorOverwrite = Color(0xFF0000FF.toInt())
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
            )
        )
        // width is not important
        paragraph.layout(100.0f)

        assertThat(
            paragraph.underlyingText,
            hasSpan(ShadowSpan::class, start = 0, end = text.length) {
                return@hasSpan it.color == color.toArgb() &&
                        it.offsetX == offset.dx &&
                        it.offsetY == offset.dy &&
                        it.radius == radius.value
            }
        )
        assertThat(
            paragraph.underlyingText,
            hasSpanOnTop(ShadowSpan::class, start = 0, end = "abc".length) {
                return@hasSpanOnTop it.color == colorOverwrite.toArgb() &&
                        it.offsetX == offsetOverwrite.dx &&
                        it.offsetY == offsetOverwrite.dy &&
                        it.radius == radiusOverwrite.value
            }
        )
    }

    @Test
    fun textStyle_fontFamily_addsTypefaceSpanWithCorrectTypeface() {
        val text = "abcde"
        val textStyle = TextStyle(
            fontFamily = fontFamily,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.bold
        )
        val expectedTypeface = TypefaceAdapter().create(
            fontFamily = fontFamily,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.bold
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
            )
        )
        paragraph.layout(100.0f)

        assertThat(paragraph.underlyingText.toString(), equalTo(text))
        assertThat(
            paragraph.underlyingText,
            hasSpan(TypefaceSpan::class, expectedStart, expectedEnd) { span ->
                span.typeface == expectedTypeface
            })
    }

    @Test
    fun textStyle_fontFamily_whenFontSynthesizeTurnedOff() {
        val text = "abcde"
        val textStyle = TextStyle(
            fontFamily = fontFamily,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.bold,
            fontSynthesis = FontSynthesis.None
        )
        val expectedTypeface = TypefaceAdapter().create(
            fontFamily = fontFamily,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.bold,
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
            )
        )
        paragraph.layout(100.0f)

        assertThat(paragraph.underlyingText.toString(), equalTo(text))
        assertThat(
            paragraph.underlyingText,
            hasSpan(TypefaceSpan::class, expectedStart, expectedEnd) { span ->
                span.typeface == expectedTypeface
            })
    }

    @Test
    fun textStyle_fontFeatureSetting_setSpanOnText() {
        val text = "abc"
        val fontFeatureSettings = "\"kern\" 0"
        val textStyle = TextStyle(fontFeatureSettings = fontFeatureSettings)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, "abc".length))
        )
        // width is not important
        paragraph.layout(100.0f)

        assertThat(
            paragraph.underlyingText,
            hasSpan(FontFeatureSpan::class, 0, "abc".length) {
                it.fontFeatureSettings == fontFeatureSettings
            })
    }

    @Test
    fun testEmptyFontFamily() {
        val typefaceAdapter = mock<TypefaceAdapter>()
        val paragraph = simpleParagraph(
            text = "abc",
            typefaceAdapter = typefaceAdapter
        )
        paragraph.layout(Float.MAX_VALUE)

        verify(typefaceAdapter, never()).create(
            fontFamily = any(),
            fontWeight = any(),
            fontStyle = any(),
            fontSynthesis = any()
        )
        assertThat(paragraph.textPaint.typeface, nullValue())
    }

    @Test
    fun testEmptyFontFamily_withBoldFontWeightSelection() {
        val typefaceAdapter = spy(TypefaceAdapter())

        val paragraph = simpleParagraph(
            text = "abc",
            fontFamily = null,
            fontWeight = FontWeight.bold,
            typefaceAdapter = typefaceAdapter
        )
        paragraph.layout(Float.MAX_VALUE)

        verify(typefaceAdapter, times(1)).create(
            fontFamily = eq(null),
            fontWeight = eq(FontWeight.bold),
            fontStyle = eq(FontStyle.Normal),
            fontSynthesis = eq(FontSynthesis.All)
        )

        val typeface = paragraph.textPaint.typeface
        assertThat(typeface, not(nullValue()))
        assertThat(typeface.isBold, equalTo(true))
        assertThat(typeface.isItalic, equalTo(false))
    }

    @Test
    fun testEmptyFontFamily_withFontStyleSelection() {
        val typefaceAdapter = spy(TypefaceAdapter())
        val paragraph = simpleParagraph(
            text = "abc",
            fontFamily = null,
            fontStyle = FontStyle.Italic,
            typefaceAdapter = typefaceAdapter
        )
        paragraph.layout(Float.MAX_VALUE)

        verify(typefaceAdapter, times(1)).create(
            fontFamily = eq(null),
            fontWeight = eq(FontWeight.normal),
            fontStyle = eq(FontStyle.Italic),
            fontSynthesis = eq(FontSynthesis.All)
        )

        val typeface = paragraph.textPaint.typeface
        assertThat(typeface, not(nullValue()))
        assertThat(typeface.isBold, equalTo(false))
        assertThat(typeface.isItalic, equalTo(true))
    }

    @Test
    fun testFontFamily_withGenericFamilyName() {
        val typefaceAdapter = spy(TypefaceAdapter())
        val fontFamily = FontFamily("sans-serif")

        val paragraph = simpleParagraph(
            text = "abc",
            fontFamily = fontFamily,
            typefaceAdapter = typefaceAdapter
        )
        paragraph.layout(Float.MAX_VALUE)

        verify(typefaceAdapter, times(1)).create(
            fontFamily = eq(fontFamily),
            fontWeight = eq(FontWeight.normal),
            fontStyle = eq(FontStyle.Normal),
            fontSynthesis = eq(FontSynthesis.All)
        )

        val typeface = paragraph.textPaint.typeface
        assertThat(typeface, not(nullValue()))
        assertThat(typeface.isBold, equalTo(false))
        assertThat(typeface.isItalic, equalTo(false))
    }

    @Test
    fun testFontFamily_withCustomFont() {
        val typefaceAdapter = spy(TypefaceAdapter())
        val paragraph = simpleParagraph(
            text = "abc",
            fontFamily = fontFamily,
            typefaceAdapter = typefaceAdapter
        )
        paragraph.layout(Float.MAX_VALUE)

        verify(typefaceAdapter, times(1)).create(
            fontFamily = eq(fontFamily),
            fontWeight = eq(FontWeight.normal),
            fontStyle = eq(FontStyle.Normal),
            fontSynthesis = eq(FontSynthesis.All)
        )
        val typeface = paragraph.textPaint.typeface
        assertThat(typeface.isBold, equalTo(false))
        assertThat(typeface.isItalic, equalTo(false))
    }

    @Test
    fun testEllipsis_withMaxLineEqualsNull_doesNotEllipsis() {
        val text = "abc"
        val fontSize = 20f
        val paragraphWidth = (text.length - 1) * fontSize
        val paragraph = simpleParagraph(
            text = text,
            fontFamily = fontFamily,
            fontSize = fontSize,
            ellipsis = true
        )
        paragraph.layout(paragraphWidth)
        for (i in 0 until paragraph.lineCount) {
            assertFalse(paragraph.isEllipsisApplied(i))
        }
    }

    @Test
    fun testEllipsis_withMaxLinesLessThanTextLines_doesEllipsis() {
        val text = "abcde"
        val fontSize = 100f
        // Note that on API 21, if the next line only contains 1 character, ellipsis won't work
        val paragraphWidth = (text.length - 1.5f) * fontSize
        val paragraph = simpleParagraph(
            text = text,
            fontFamily = fontFamily,
            fontSize = fontSize,
            ellipsis = true,
            maxLines = 1
        )
        paragraph.layout(paragraphWidth)

        assertTrue(paragraph.isEllipsisApplied(0))
    }

    @Test
    fun testEllipsis_withMaxLinesMoreThanTextLines_doesNotEllipsis() {
        val text = "abc"
        val fontSize = 100f
        val paragraphWidth = (text.length - 1) * fontSize
        val maxLines = ceil(text.length * fontSize / paragraphWidth).toInt()
        val paragraph = simpleParagraph(
            text = text,
            fontFamily = fontFamily,
            fontSize = fontSize,
            ellipsis = true,
            maxLines = maxLines
        )
        paragraph.layout(paragraphWidth)

        for (i in 0 until paragraph.lineCount) {
            assertFalse(paragraph.isEllipsisApplied(i))
        }
    }

    private fun simpleParagraph(
        text: String = "",
        textStyles: List<AnnotatedString.Item<TextStyle>> = listOf(),
        textIndent: TextIndent? = null,
        textAlign: TextAlign? = null,
        fontSize: Float? = null,
        ellipsis: Boolean? = null,
        maxLines: Int? = null,
        fontFamily: FontFamily? = null,
        fontWeight: FontWeight? = null,
        fontStyle: FontStyle? = null,
        typefaceAdapter: TypefaceAdapter = TypefaceAdapter()
    ): ParagraphAndroid {
        return ParagraphAndroid(
            text = text,
            textStyles = textStyles,
            typefaceAdapter = typefaceAdapter,
            paragraphStyle = ParagraphStyle(
                textAlign = textAlign,
                textIndent = textIndent,
                ellipsis = ellipsis,
                maxLines = maxLines,
                fontFamily = fontFamily,
                fontSize = fontSize,
                fontWeight = fontWeight,
                fontStyle = fontStyle
            )
        )
    }
}