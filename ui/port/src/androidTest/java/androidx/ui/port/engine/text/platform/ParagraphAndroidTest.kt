package androidx.ui.port.engine.text.platform

import android.graphics.Paint
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.LocaleSpan
import android.text.style.StrikethroughSpan
import android.text.style.UnderlineSpan
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.text.StaticLayoutCompat
import androidx.text.style.LetterSpacingSpan
import androidx.text.style.TypefaceSpan
import androidx.ui.engine.text.FontStyle
import androidx.ui.engine.text.FontSynthesis
import androidx.ui.engine.text.FontWeight
import androidx.ui.engine.text.ParagraphBuilder
import androidx.ui.engine.text.ParagraphStyle
import androidx.ui.engine.text.TextAlign
import androidx.ui.engine.text.TextDecoration
import androidx.ui.engine.text.TextStyle
import androidx.ui.engine.text.font.FontFamily
import androidx.ui.engine.text.font.asFontFamily
import androidx.ui.engine.text.platform.ParagraphAndroid
import androidx.ui.engine.text.platform.TypefaceAdapter
import androidx.ui.engine.window.Locale
import androidx.ui.painting.Color
import androidx.ui.port.engine.text.FontTestData.Companion.BASIC_MEASURE_FONT
import androidx.ui.port.matchers.equalToBitmap
import androidx.ui.port.matchers.hasSpan
import androidx.ui.port.matchers.hasSpanOnTop
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
import org.junit.Assert.assertThat
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
        val fontSize = 50.0
        for (text in arrayOf("abc\ndef", "\u05D0\u05D1\u05D2\n\u05D3\u05D4\u05D5")) {
            val paragraphAndroid = simpleParagraph(
                text = StringBuilder(text),
                fontSize = fontSize,
                fontFamily = fontFamily
            )

            // 2 chars width
            paragraphAndroid.layout(width = 2 * fontSize)

            val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
            textPaint.textSize = fontSize.toFloat()
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
            textStyles = listOf(ParagraphBuilder.TextStyleIndex(textStyle, 0, text.length))
        )
        paragraph.layout(100.0)

        assertThat(paragraph.underlyingText, hasSpan(ForegroundColorSpan::class, 0, text.length))
    }

    @Test
    fun textStyle_setColorOnPartOfText() {
        val text = "abcde"
        val textStyle = TextStyle(color = Color(0xFF0000FF.toInt()))

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(ParagraphBuilder.TextStyleIndex(textStyle, 0, "abc".length))
        )
        paragraph.layout(100.0)

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
                ParagraphBuilder.TextStyleIndex(textStyle, 0, text.length),
                ParagraphBuilder.TextStyleIndex(textStyleOverwrite, 0, "abc".length)
            )
        )
        paragraph.layout(100.0)

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
        val textStyle = TextStyle(decoration = TextDecoration.lineThrough)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(ParagraphBuilder.TextStyleIndex(textStyle, 0, text.length))
        )
        paragraph.layout(100.0)

        assertThat(paragraph.underlyingText.toString(), equalTo(text))
        assertThat(paragraph.underlyingText, hasSpan(StrikethroughSpan::class, 0, text.length))
    }

    @Test
    fun testStyle_setTextDecorationOnWholeText_withUnderline() {
        val text = "abcde"
        val textStyle = TextStyle(decoration = TextDecoration.underline)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(ParagraphBuilder.TextStyleIndex(textStyle, 0, text.length))
        )
        paragraph.layout(100.0)

        assertThat(paragraph.underlyingText.toString(), equalTo(text))
        assertThat(paragraph.underlyingText, hasSpan(UnderlineSpan::class, 0, text.length))
    }

    @Test
    fun testStyle_setTextDecorationOnPartText_withLineThrough() {
        val text = "abcde"
        val textStyle = TextStyle(decoration = TextDecoration.lineThrough)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(ParagraphBuilder.TextStyleIndex(textStyle, 0, "abc".length))
        )
        paragraph.layout(100.0)

        assertThat(paragraph.underlyingText.toString(), equalTo(text))
        assertThat(paragraph.underlyingText, hasSpan(StrikethroughSpan::class, 0, "abc".length))
    }

    @Test
    fun testStyle_setTextDecorationOnPartText_withUnderline() {
        val text = "abcde"
        val textStyle = TextStyle(decoration = TextDecoration.underline)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(ParagraphBuilder.TextStyleIndex(textStyle, 0, "abc".length))
        )
        paragraph.layout(100.0)

        assertThat(paragraph.underlyingText.toString(), equalTo(text))
        assertThat(paragraph.underlyingText, hasSpan(UnderlineSpan::class, 0, "abc".length))
    }

    @Test
    fun testStyle_setTextDecoration_withLineThroughAndUnderline() {
        val text = "abcde"
        val textStyle = TextStyle(
            decoration = TextDecoration.combine(
                listOf(TextDecoration.lineThrough, TextDecoration.underline)
            )
        )

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(ParagraphBuilder.TextStyleIndex(textStyle, 0, "abc".length))
        )
        paragraph.layout(100.0)

        assertThat(paragraph.underlyingText.toString(), equalTo(text))
        assertThat(paragraph.underlyingText, hasSpan(UnderlineSpan::class, 0, "abc".length))
        assertThat(paragraph.underlyingText, hasSpan(StrikethroughSpan::class, 0, "abc".length))
    }

    @Test
    fun textStyle_setFontSizeOnWholeText() {
        val text = "abcde"
        val fontSize = 20.0
        val paragraphWidth = text.length * fontSize
        val textStyle = TextStyle(fontSize = fontSize)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(ParagraphBuilder.TextStyleIndex(textStyle, 0, text.length))
        )
        paragraph.layout(paragraphWidth)

        assertThat(paragraph.underlyingText, hasSpan(AbsoluteSizeSpan::class, 0, text.length))
    }

    @Test
    fun textStyle_setFontSizeOnPartText() {
        val text = "abcde"
        val fontSize = 20.0
        val paragraphWidth = text.length * fontSize
        val textStyle = TextStyle(fontSize = fontSize)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(ParagraphBuilder.TextStyleIndex(textStyle, 0, "abc".length))
        )
        paragraph.layout(paragraphWidth)

        assertThat(paragraph.underlyingText, hasSpan(AbsoluteSizeSpan::class, 0, "abc".length))
    }

    @Test
    fun textStyle_setFontSizeTwice_lastOneOverwrite() {
        val text = "abcde"
        val fontSize = 20.0
        val fontSizeOverwrite = 30.0
        val paragraphWidth = text.length * fontSizeOverwrite
        val textStyle = TextStyle(fontSize = fontSize)
        val textStyleOverwrite = TextStyle(fontSize = fontSizeOverwrite)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(
                ParagraphBuilder.TextStyleIndex(textStyle, 0, text.length),
                ParagraphBuilder.TextStyleIndex(textStyleOverwrite, 0, "abc".length)
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
    fun textStyle_setLetterSpacingOnWholeText() {
        val text = "abcde"
        val letterSpacing = 2.0
        val textStyle = TextStyle(letterSpacing = letterSpacing)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(ParagraphBuilder.TextStyleIndex(textStyle, 0, text.length))
        )
        paragraph.layout(100.0)
        assertThat(paragraph.underlyingText.toString(), equalTo(text))
        assertThat(paragraph.underlyingText, hasSpan(LetterSpacingSpan::class, 0, text.length))
    }

    @Test
    fun textStyle_setLetterSpacingOnPartText() {
        val text = "abcde"
        val textStyle = TextStyle(letterSpacing = 2.0)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(ParagraphBuilder.TextStyleIndex(textStyle, 0, "abc".length))
        )
        paragraph.layout(100.0)
        assertThat(paragraph.underlyingText.toString(), equalTo(text))
        assertThat(paragraph.underlyingText, hasSpan(LetterSpacingSpan::class, 0, "abc".length))
    }

    @Test
    fun textStyle_setLetterSpacingTwice_lastOneOverwrite() {
        val text = "abcde"
        val textStyle = TextStyle(letterSpacing = 2.0)
        val textStyleOverwrite = TextStyle(letterSpacing = 3.0)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(
                ParagraphBuilder.TextStyleIndex(textStyle, 0, text.length),
                ParagraphBuilder.TextStyleIndex(textStyleOverwrite, 0, "abc".length)
            )
        )
        paragraph.layout(100.0)
        assertThat(paragraph.underlyingText.toString(), equalTo(text))
        assertThat(paragraph.underlyingText, hasSpan(LetterSpacingSpan::class, 0, text.length))
        assertThat(paragraph.underlyingText, hasSpan(LetterSpacingSpan::class, 0, "abc".length))
        assertThat(
            paragraph.underlyingText,
            hasSpanOnTop(LetterSpacingSpan::class, 0, "abc".length)
        )
    }

    @Test
    fun textStyle_setBackgroundOnWholeText() {
        val text = "abcde"
        val color = Color(0xFF0000FF.toInt())
        val textStyle = TextStyle(background = color)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(ParagraphBuilder.TextStyleIndex(textStyle, 0, text.length))
        )
        paragraph.layout(100.0)

        assertThat(paragraph.underlyingText.toString(), equalTo(text))
        assertThat(paragraph.underlyingText,
            hasSpan(BackgroundColorSpan::class, 0, text.length) { span ->
                span.backgroundColor == color.value
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
            textStyles = listOf(ParagraphBuilder.TextStyleIndex(textStyle, 0, "abc".length))
        )
        paragraph.layout(100.0)

        assertThat(paragraph.underlyingText.toString(), equalTo(text))
        assertThat(paragraph.underlyingText,
            hasSpan(BackgroundColorSpan::class, 0, "abc".length) { span ->
                span.backgroundColor == color.value
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
                ParagraphBuilder.TextStyleIndex(textStyle, 0, text.length),
                ParagraphBuilder.TextStyleIndex(textStyleOverwrite, 0, "abc".length)
            )
        )
        paragraph.layout(100.0)

        assertThat(paragraph.underlyingText.toString(), equalTo(text))
        assertThat(paragraph.underlyingText,
            hasSpan(BackgroundColorSpan::class, 0, text.length) { span ->
                span.backgroundColor == color.value
            }
        )
        assertThat(paragraph.underlyingText,
            hasSpan(BackgroundColorSpan::class, 0, "abc".length) { span ->
                span.backgroundColor == colorOverwrite.value
            }
        )
        assertThat(
            paragraph.underlyingText,
            hasSpanOnTop(BackgroundColorSpan::class, 0, "abc".length) { span ->
                span.backgroundColor == colorOverwrite.value
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
            textStyles = listOf(ParagraphBuilder.TextStyleIndex(textStyle, 0, text.length))
        )
        paragraph.layout(100.0)

        assertThat(paragraph.underlyingText, hasSpan(LocaleSpan::class, 0, text.length))
    }

    @Test
    fun textStyle_setLocaleOnPartText() {
        val text = "abcde"
        val locale = Locale("en", "US")
        val textStyle = TextStyle(locale = locale)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(ParagraphBuilder.TextStyleIndex(textStyle, 0, "abc".length))
        )
        paragraph.layout(100.0)

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
                ParagraphBuilder.TextStyleIndex(textStyle, 0, text.length),
                ParagraphBuilder.TextStyleIndex(textStyleOverwrite, 0, "abc".length)
            )
        )
        paragraph.layout(100.0)

        assertThat(paragraph.underlyingText, hasSpan(LocaleSpan::class, 0, text.length))
        assertThat(paragraph.underlyingText, hasSpan(LocaleSpan::class, 0, "abc".length))
        assertThat(
            paragraph.underlyingText,
            hasSpanOnTop(LocaleSpan::class, 0, "abc".length)
        )
    }

    @Test
    fun textStyle_fontFamily_addsTypefaceSpanWithCorrectTypeface() {
        val text = "abcde"
        val textStyle = TextStyle(
            fontFamily = fontFamily,
            fontStyle = FontStyle.italic,
            fontWeight = FontWeight.bold
        )
        val expectedTypeface = TypefaceAdapter().create(
            fontFamily = fontFamily,
            fontStyle = FontStyle.italic,
            fontWeight = FontWeight.bold
        )
        val expectedStart = 0
        val expectedEnd = "abc".length

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(
                ParagraphBuilder.TextStyleIndex(
                    textStyle,
                    expectedStart,
                    expectedEnd
                )
            )
        )
        paragraph.layout(100.0)

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
            fontStyle = FontStyle.italic,
            fontWeight = FontWeight.bold,
            fontSynthesis = FontSynthesis.none
        )
        val expectedTypeface = TypefaceAdapter().create(
            fontFamily = fontFamily,
            fontStyle = FontStyle.italic,
            fontWeight = FontWeight.bold,
            fontSynthesis = FontSynthesis.none
        )
        val expectedStart = 0
        val expectedEnd = "abc".length

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(
                ParagraphBuilder.TextStyleIndex(
                    textStyle,
                    expectedStart,
                    expectedEnd
                )
            )
        )
        paragraph.layout(100.0)

        assertThat(paragraph.underlyingText.toString(), equalTo(text))
        assertThat(
            paragraph.underlyingText,
            hasSpan(TypefaceSpan::class, expectedStart, expectedEnd) { span ->
                span.typeface == expectedTypeface
            })
    }

    @Test
    fun testEmptyFontFamily() {
        val typefaceAdapter = mock<TypefaceAdapter>()
        val paragraph = simpleParagraph(
            text = "abc",
            typefaceAdapter = typefaceAdapter
        )
        paragraph.layout(Double.MAX_VALUE)

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
        paragraph.layout(Double.MAX_VALUE)

        verify(typefaceAdapter, times(1)).create(
            fontFamily = eq(null),
            fontWeight = eq(FontWeight.bold),
            fontStyle = eq(FontStyle.normal),
            fontSynthesis = eq(FontSynthesis.all)
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
            fontStyle = FontStyle.italic,
            typefaceAdapter = typefaceAdapter
        )
        paragraph.layout(Double.MAX_VALUE)

        verify(typefaceAdapter, times(1)).create(
            fontFamily = eq(null),
            fontWeight = eq(FontWeight.normal),
            fontStyle = eq(FontStyle.italic),
            fontSynthesis = eq(FontSynthesis.all)
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
        paragraph.layout(Double.MAX_VALUE)

        verify(typefaceAdapter, times(1)).create(
            fontFamily = eq(fontFamily),
            fontWeight = eq(FontWeight.normal),
            fontStyle = eq(FontStyle.normal),
            fontSynthesis = eq(FontSynthesis.all)
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
        paragraph.layout(Double.MAX_VALUE)

        verify(typefaceAdapter, times(1)).create(
            fontFamily = eq(fontFamily),
            fontWeight = eq(FontWeight.normal),
            fontStyle = eq(FontStyle.normal),
            fontSynthesis = eq(FontSynthesis.all)
        )
        val typeface = paragraph.textPaint.typeface
        assertThat(typeface.isBold, equalTo(false))
        assertThat(typeface.isItalic, equalTo(false))
    }

    private fun simpleParagraph(
        text: CharSequence = "",
        textStyles: List<ParagraphBuilder.TextStyleIndex> = listOf(),
        textAlign: TextAlign? = null,
        fontSize: Double? = null,
        maxLines: Int? = null,
        fontFamily: FontFamily? = null,
        fontWeight: FontWeight? = null,
        fontStyle: FontStyle? = null,
        typefaceAdapter: TypefaceAdapter = TypefaceAdapter()
    ): ParagraphAndroid {
        return ParagraphAndroid(
            text = StringBuilder(text),
            textStyles = textStyles,
            typefaceAdapter = typefaceAdapter,
            paragraphStyle = ParagraphStyle(
                textAlign = textAlign,
                maxLines = maxLines,
                fontFamily = fontFamily,
                fontSize = fontSize,
                fontWeight = fontWeight,
                fontStyle = fontStyle
            )
        )
    }
}