package androidx.ui.engine.text

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.Locale

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
                        "locale: unspecified" +
                        ")"
                )
            )
        )
    }

    @Test
    fun `toString with values`() {
        val textAlign = TextAlign.end
        val textDirection = TextDirection.RTL
        val fontWeight = FontWeight.bold
        val fontStyle = FontStyle.italic
        val maxLines = 2
        val fontFamily = "san-serif"
        val fontSize = 1.0
        val lineHeight = 2.0
        val ellipsis = "dot dot"
        val locale = Locale.ENGLISH

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
            locale = locale
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
                        "locale: $locale" +
                        ")"
                )
            )
        )
    }
}