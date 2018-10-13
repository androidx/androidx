package androidx.ui.engine.text

import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.Locale

@RunWith(JUnit4::class)
class ParagraphTest {

    @Test
    fun `width default value`() {
        val paragraphStyle = createParagraphStyle()
        val paragraph = Paragraph(StringBuilder(), paragraphStyle, listOf())
        assertThat(paragraph.width, equalTo(-1.0))
    }

    @Test
    fun `height default value`() {
        val paragraphStyle = createParagraphStyle()
        val paragraph = Paragraph(StringBuilder(), paragraphStyle, listOf())
        assertThat(paragraph.height, equalTo(0.0))
    }

    @Test
    fun `minIntrinsicWidth default value`() {
        val paragraphStyle = createParagraphStyle()
        val paragraph = Paragraph(StringBuilder(), paragraphStyle, listOf())
        assertThat(paragraph.minIntrinsicWidth, equalTo(0.0))
    }

    @Test
    fun `maxIntrinsicWidth  default value`() {
        val paragraphStyle = createParagraphStyle()
        val paragraph = Paragraph(StringBuilder(), paragraphStyle, listOf())
        assertThat(paragraph.maxIntrinsicWidth, equalTo(0.0))
    }

    @Test
    fun `alphabeticBaseline default value`() {
        val paragraphStyle = createParagraphStyle()
        val paragraph = Paragraph(StringBuilder(), paragraphStyle, listOf())
        assertThat(paragraph.alphabeticBaseline, equalTo(0.0))
    }

    @Test
    fun `ideographicBaseline default value`() {
        val paragraphStyle = createParagraphStyle()
        val paragraph = Paragraph(StringBuilder(), paragraphStyle, listOf())
        assertThat(paragraph.ideographicBaseline, equalTo(0.0))
    }

    @Test
    fun `didExceedMaxLines default value`() {
        val paragraphStyle = createParagraphStyle()
        val paragraph = Paragraph(StringBuilder(), paragraphStyle, listOf())
        assertThat(paragraph.didExceedMaxLines, equalTo(false))
    }

    private fun createParagraphStyle(): ParagraphStyle {
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

        return ParagraphStyle(
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
    }
}