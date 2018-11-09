package androidx.ui.port.engine.text.platform

import android.app.Instrumentation
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.text.StaticLayoutFactory
import androidx.ui.engine.text.FontFallback
import androidx.ui.engine.text.ParagraphStyle
import androidx.ui.engine.text.TextAlign
import androidx.ui.engine.text.platform.ParagraphAndroid
import androidx.ui.port.bitmap
import androidx.ui.port.matchers.equalToBitmap
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.lang.StringBuilder
import kotlin.math.ceil

@RunWith(JUnit4::class)
@SmallTest
class ParagraphAndroidTest {
    private lateinit var instrumentation: Instrumentation
    private lateinit var fontFallback: FontFallback

    @Before
    fun setup() {
        instrumentation = InstrumentationRegistry.getInstrumentation()
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

            val staticLayout = StaticLayoutFactory.create(
                textPaint = textPaint,
                charSequence = text,
                width = ceil(paragraphAndroid.width).toInt(),
                ellipsizeWidth = ceil(paragraphAndroid.width).toInt()
            )
            Assert.assertThat(paragraphAndroid.bitmap(), equalToBitmap(staticLayout.bitmap()))
        }
    }

    private fun simpleParagraph(
        text: CharSequence = "",
        textAlign: TextAlign? = null,
        fontSize: Double? = null,
        maxLines: Int? = null
    ): ParagraphAndroid {
        return ParagraphAndroid(
            text = StringBuilder(text),
            textStyles = listOf(),
            paragraphStyle = ParagraphStyle(
                textAlign = textAlign,
                maxLines = maxLines,
                fontFamily = fontFallback,
                fontSize = fontSize
            )
        )
    }
}