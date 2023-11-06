package androidx.compose.ui.text.android

import android.graphics.Path
import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import android.text.TextPaint
import androidx.core.content.res.ResourcesCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.fonts.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@OptIn(InternalPlatformTextApi::class)
@RunWith(AndroidJUnit4::class)
@SmallTest
@SdkSuppress(minSdkVersion = 29) // CustomFallbackBuilder was added in API 29, required for test
class FontPaddingWithCustomFallbackTest {
    private val context = InstrumentationRegistry.getInstrumentation().context

    private val fontSize = 100f
    private val latinText = "a"
    private val tallText = "1"
    private val mixedText = latinText + tallText
    private val mixedTextMultiLine = "$mixedText\n$mixedText\n$mixedText"

    private val latinTypeface = ResourcesCompat.getFont(context, R.font.sample_font)!!
    private val tallTypeface = ResourcesCompat.getFont(context, R.font.tall_font)!!
    private val latinFirstFallback = fontFallback(R.font.sample_font, R.font.tall_font)

    private fun fontFallback(first: Int, second: Int): Typeface {
        return Typeface.CustomFallbackBuilder(fontFamily(first))
            .addCustomFallback(fontFamily(second))
            .build()
    }

    private fun fontFamily(fontId: Int): FontFamily {
        return FontFamily.Builder(Font.Builder(context.resources, fontId).build()).build()
    }

    @Test
    fun mixedTypefaceWithMixedTextHasTheHeightOfTallSingleLine() {
        val mixedLayout = createTextLayout(mixedText, typeface = latinFirstFallback)
        val tallLayout = createTextLayout(tallText, typeface = tallTypeface)

        assertThat(mixedLayout.height).isEqualTo(tallLayout.height)
    }

    @Test
    fun mixedTypefaceHasPaddings() {
        val mixedLayout = createTextLayout(
            mixedTextMultiLine,
            width = fontSize * 2,
            typeface = latinFirstFallback
        )

        // tall font is 1500/500
        // latin font is 800/200
        // latin first font metrics total height is 1000, padding will be according to 1000
        // therefore the paddings are (1500-800)*1000*fontSize=70 and (500-200)/1000*fontSize=30
        // divided by 1000 because fontMetrics is for 1000.
        assertThat(mixedLayout.topPadding).isEqualTo(((1500 - 800) / 1000f * fontSize).toInt())
        assertThat(mixedLayout.bottomPadding).isEqualTo(((500 - 200) / 1000f * fontSize).toInt())
    }

    @Test
    fun latinFirstFallbackWithLatinTextHasTheHeightOfLatin() {
        val latinTextFallbackLayout = createTextLayout(latinText, typeface = latinFirstFallback)
        val latinLayout = createTextLayout(latinText, typeface = latinTypeface)

        assertThat(latinTextFallbackLayout.height).isEqualTo(latinLayout.height)
    }

    @Test
    fun latinFirstFallbackWithTallTextHasTheHeightOfTall() {
        val tallTextFallbackLayout = createTextLayout(tallText, typeface = latinFirstFallback)
        val tallLayout = createTextLayout(tallText, typeface = tallTypeface)

        assertThat(tallTextFallbackLayout.height).isEqualTo(tallLayout.height)
    }

    @Test
    fun getLineBottom_includeFontPaddingFalse() {
        val textSize = 100.0f
        val layoutWidth = textSize * mixedText.length

        val layoutPaddingFalse = createTextLayout(
            text = mixedTextMultiLine,
            width = layoutWidth,
            includePadding = false,
            typeface = latinFirstFallback
        )

        val layoutPaddingTrue = createTextLayout(
            text = mixedTextMultiLine,
            width = layoutWidth,
            includePadding = true,
            typeface = latinFirstFallback
        )

        assertThat(layoutPaddingFalse.bottomPadding).isGreaterThan(0)
        assertThat(layoutPaddingFalse.topPadding).isGreaterThan(0)

        for (line in 0..1) {
            assertThat(layoutPaddingFalse.getLineBottom(line)).isEqualTo(
                layoutPaddingTrue.getLineBottom(line) + layoutPaddingFalse.topPadding
            )
        }

        assertThat(layoutPaddingFalse.getLineBottom(2)).isEqualTo(
            layoutPaddingTrue.getLineBottom(2) +
                layoutPaddingFalse.topPadding +
                layoutPaddingFalse.bottomPadding
        )
    }

    @Test
    fun getSelectionPath_emptySelection() {
        val textSize = 100.0f
        val layoutWidth = textSize * mixedText.length

        val layout = createTextLayout(
            text = mixedTextMultiLine,
            width = layoutWidth,
            includePadding = false,
            typeface = latinFirstFallback
        )

        val path = spy(Path())
        layout.getSelectionPath(0, 0, path)

        assertThat(path.isEmpty).isTrue()
        verify(path, times(0)).offset(any(), any())
        verify(path, times(0)).offset(any(), any(), any())
    }

    @Test
    fun getSelectionPath_offsetsByTopPadding() {
        val textSize = 100.0f
        val layoutWidth = textSize * mixedText.length

        val layoutPaddingFalse = createTextLayout(
            text = mixedTextMultiLine,
            width = layoutWidth,
            includePadding = false,
            typeface = latinFirstFallback
        )

        val layoutPaddingTrue = createTextLayout(
            text = mixedTextMultiLine,
            width = layoutWidth,
            includePadding = true,
            typeface = latinFirstFallback
        )

        assertThat(layoutPaddingFalse.bottomPadding).isGreaterThan(0)
        assertThat(layoutPaddingFalse.topPadding).isGreaterThan(0)

        val pathPaddingFalse = Path()
        layoutPaddingFalse.getSelectionPath(0, 1, pathPaddingFalse)

        val pathPaddingTrue = Path()
        layoutPaddingTrue.getSelectionPath(0, 1, pathPaddingTrue)

        // padding true with offset is the expected path
        pathPaddingTrue.offset(0f, layoutPaddingFalse.topPadding.toFloat())
        val pathDifference = Path().apply {
            op(pathPaddingFalse, pathPaddingTrue, Path.Op.DIFFERENCE)
        }
        assertThat(pathDifference.isEmpty).isTrue()
    }

    /**
     * fallbackLineSpacing has to be false so that the real impact can be tested.
     * fallbackLineSpacing increases line height, and is enabled on starting API P StaticLayout,
     * Android T BoringLayout.
     */
    private fun createTextLayout(
        text: CharSequence,
        width: Float = Float.MAX_VALUE,
        includePadding: Boolean = false,
        fallbackLineSpacing: Boolean = false,
        lineSpacingMultiplier: Float = LayoutCompat.DEFAULT_LINESPACING_MULTIPLIER,
        alignment: Int = LayoutCompat.DEFAULT_ALIGNMENT,
        typeface: Typeface = latinTypeface
    ): TextLayout {
        val textPaint = TextPaint().apply {
            this.typeface = typeface
            this.textSize = fontSize
        }

        return TextLayout(
            charSequence = text,
            width = width,
            textPaint = textPaint,
            includePadding = includePadding,
            fallbackLineSpacing = fallbackLineSpacing,
            lineSpacingMultiplier = lineSpacingMultiplier,
            alignment = alignment
        )
    }
}
