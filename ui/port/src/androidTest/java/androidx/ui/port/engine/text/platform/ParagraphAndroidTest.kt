package androidx.ui.port.engine.text.platform

import android.app.Instrumentation
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.UnderlineSpan
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.text.StaticLayoutCompat
import androidx.ui.engine.text.FontFallback
import androidx.ui.engine.text.ParagraphBuilder
import androidx.ui.engine.text.ParagraphStyle
import androidx.ui.engine.text.TextAlign
import androidx.ui.engine.text.TextDecoration
import androidx.ui.engine.text.TextStyle
import androidx.ui.engine.text.platform.ParagraphAndroid
import androidx.ui.painting.Color
import androidx.ui.port.bitmap
import androidx.ui.port.matchers.equalToBitmap
import androidx.ui.port.matchers.hasSpan
import androidx.ui.port.matchers.hasSpanOnTop
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.math.ceil

@RunWith(JUnit4::class)
@SmallTest
class ParagraphAndroidTest {
    private lateinit var instrumentation: Instrumentation
    private lateinit var fontFallback: FontFallback

    @Before
    fun setup() {
        instrumentation = InstrumentationRegistry.getInstrumentation()
        // This sample font provides the following features:
        // 1. The width of most of visible characters equals to font size.
        // 2. The LTR/RTL characters are rendered as ▶/◀.
        // 3. The fontMetrics passed to TextPaint has descend - ascend equal to 1.2 * fontSize.
        val font = Typeface.createFromAsset(instrumentation.context.assets, "sample_font.ttf")!!
        fontFallback = FontFallback(font)
    }

    @Test
    fun draw_with_newline_and_line_break_default_values() {
        val fontSize = 50.0
        for (text in arrayOf("abc\ndef", "\u05D0\u05D1\u05D2\n\u05D3\u05D4\u05D5")) {
            val paragraphAndroid = simpleParagraph(
                text = StringBuilder(text),
                fontSize = fontSize
            )

            // 2 chars width
            paragraphAndroid.layout(width = 2 * fontSize)

            val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
            textPaint.textSize = fontSize.toFloat()
            textPaint.typeface = fontFallback.typeface

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
        val fontSize = 20.0
        val paragraphWidth = text.length * fontSize
        val textStyle = TextStyle(color = Color(0xFF0000FF.toInt()))

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(ParagraphBuilder.TextStyleIndex(textStyle, 0, text.length)),
            fontSize = fontSize
        )
        paragraph.layout(paragraphWidth)

        assertThat(paragraph.underlyingText, hasSpan(ForegroundColorSpan::class, 0, text.length))
    }

    @Test
    fun textStyle_setColorOnPartOfText() {
        val text = "abcde"
        val fontSize = 20.0
        val paragraphWidth = text.length * fontSize
        val textStyle = TextStyle(color = Color(0xFF0000FF.toInt()))

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(ParagraphBuilder.TextStyleIndex(textStyle, 0, "abc".length)),
            fontSize = fontSize
        )
        paragraph.layout(paragraphWidth)

        assertThat(paragraph.underlyingText, hasSpan(ForegroundColorSpan::class, 0, "abc".length))
    }

    @Test
    fun textStyle_setColorTwice_lastOneOverwrite() {
        val text = "abcde"
        val fontSize = 20.0
        val paragraphWidth = text.length * fontSize
        val textStyle = TextStyle(color = Color(0xFF0000FF.toInt()))
        val textStyleOverwrite = TextStyle(color = Color(0xFF00FF00.toInt()))

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(
                ParagraphBuilder.TextStyleIndex(textStyle, 0, text.length),
                ParagraphBuilder.TextStyleIndex(textStyleOverwrite, 0, "abc".length)
            ),
            fontSize = fontSize
        )
        paragraph.layout(paragraphWidth)

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

    private fun simpleParagraph(
        text: CharSequence = "",
        textStyles: List<ParagraphBuilder.TextStyleIndex> = listOf(),
        textAlign: TextAlign? = null,
        fontSize: Double? = null,
        maxLines: Int? = null
    ): ParagraphAndroid {
        return ParagraphAndroid(
            text = StringBuilder(text),
            textStyles = textStyles,
            paragraphStyle = ParagraphStyle(
                textAlign = textAlign,
                maxLines = maxLines,
                fontFamily = fontFallback,
                fontSize = fontSize
            )
        )
    }
}