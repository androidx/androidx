/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.compose.ui.text.android

import android.graphics.RectF
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import androidx.core.content.res.ResourcesCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.fonts.R
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(InternalPlatformTextApi::class)
@RunWith(AndroidJUnit4::class)
@SmallTest
class TextLayoutFillBoundingBoxesTest {
    lateinit var sampleTypeface: Typeface
    private val fontSize = 10f

    @Before
    fun setup() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        // This sample font provides the following features:
        // 1. The width of most of visible characters equals to font size.
        // 2. The LTR/RTL characters are rendered as ▶/◀.
        // 3. The fontMetrics passed to TextPaint has descend - ascend equal to 1.2 * fontSize.
        sampleTypeface = ResourcesCompat.getFont(instrumentation.context, R.font.sample_font)!!
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeStart() {
        val layout = createTextLayout("a")
        layout.getBoundingBoxes(1, 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun startEqualToLength() {
        val layout = createTextLayout("a")
        layout.getBoundingBoxes(1, 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun endGreaterThanLength() {
        val layout = createTextLayout("a")
        layout.getBoundingBoxes(0, 2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun endEqualToStart() {
        val layout = createTextLayout("a")
        layout.getBoundingBoxes(0, 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun arraySizeSmallerThanTextLength() {
        val text = "abc"
        val layout = createTextLayout(text)
        val array = FloatArray(text.length * 4 - 1)
        layout.fillBoundingBoxes(0, text.length, array, 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun arraySizeSmallerThanTextLengthWithStart() {
        val text = "abc"
        val layout = createTextLayout(text)
        val array = FloatArray(text.length * 8)
        val arrayStart = text.length * 4 + 1
        layout.fillBoundingBoxes(0, text.length, array, arrayStart)
    }

    @Test(expected = IllegalArgumentException::class)
    fun arraySizeSmallerThanRange() {
        val text = "abc"
        val layout = createTextLayout(text)
        val startIndex = 1
        val endIndex = text.length
        val array = FloatArray((endIndex - startIndex) * 4 - 1)
        layout.fillBoundingBoxes(startIndex, text.length, array, 0)
    }

    @Test
    fun singleCharacter() {
        val text = "a"
        val layout = createTextLayout(text)
        assertThat(layout.getBoundingBoxes(0, text.length)).isEqualTo(
            ltrCharacterBoundariesForTestFont(text)
        )
    }

    @Test
    fun singleCharacterLineHeight() {
        val text = "a"
        val layout = createTextLayout(
            text = text,
            lineSpacingMultiplier = 2f
        )

        // character bound is still based on character but not the line height
        assertThat(layout.getBoundingBoxes(0, text.length)).isEqualTo(
            ltrCharacterBoundariesForTestFont(text)
        )
    }

    @Test
    fun singleCharacterRtl() {
        val text = "\u05D0"
        val width = text.length * 2 * fontSize // a width wider than text
        val layout = createTextLayout(
            text = text,
            width = width
        )

        assertThat(layout.getBoundingBoxes(0, text.length)).isEqualTo(
            rtlCharacterBoundariesForTestFont(text, width)
        )
    }

    @Test
    fun singleLineLtr() {
        val text = "abc"
        val layout = createTextLayout(text = text)

        assertThat(layout.getBoundingBoxes(0, text.length)).isEqualTo(
            ltrCharacterBoundariesForTestFont(text)
        )
    }

    @Test
    fun singleLineRtl() {
        val text = "\u05D0\u05D1\u05D2"
        val width = text.length * 2 * fontSize // a width wider than text
        val layout = createTextLayout(
            text = text,
            width = width
        )

        assertThat(layout.getBoundingBoxes(0, text.length)).isEqualTo(
            rtlCharacterBoundariesForTestFont(text, width)
        )
    }

    @Test
    fun bidiLtrLine() {
        val text = "a" + "\u05D0\u05D1" + "b"
        val layout = createTextLayout(text = text)

        val expected = ltrCharacterBoundariesForTestFont(text)
        // text with indices 0123 is rendered as 0213
        assertThat(layout.getBoundingBoxes(0, text.length)).isEqualTo(
            arrayOf(expected[0], expected[2], expected[1], expected[3])
        )
    }

    @Test
    fun bidiRtlLine() {
        val text = "\u05D0" + "ab" + "\u05D1"
        val width = text.length * 2 * fontSize // a width wider than text
        val layout = createTextLayout(
            width = width,
            text = text
        )

        val expected = rtlCharacterBoundariesForTestFont(text, width)
        // text with indices 0123 is rendered as 3120
        assertThat(layout.getBoundingBoxes(0, text.length)).isEqualTo(
            arrayOf(expected[0], expected[2], expected[1], expected[3])
        )
    }

    @Test
    fun multiLineLtr() {
        val text = "a\nb\nc"
        val layout = createTextLayout(
            text = text
        )

        assertThat(layout.getBoundingBoxes(0, text.length)).isEqualTo(
            arrayOf(
                RectF(0f, 0f, fontSize, fontSize), // a
                RectF(fontSize, 0f, fontSize, fontSize), // \n
                RectF(0f, fontSize, fontSize, 2 * fontSize), // b
                RectF(fontSize, fontSize, fontSize, 2 * fontSize), // \n
                RectF(0f, 2 * fontSize, fontSize, 3 * fontSize) // c
            )
        )
    }

    @Test
    fun multiLineCenterAligned() {
        val text = "a\nb\nc"
        val width = fontSize * 3
        val layout = createTextLayout(
            text = text,
            width = width,
            alignment = LayoutCompat.ALIGN_CENTER
        )

        assertThat(layout.getBoundingBoxes(0, text.length)).isEqualTo(
            arrayOf(
                // left top right bottom
                RectF(fontSize, 0f, 2 * fontSize, fontSize), // a
                RectF(2 * fontSize, 0f, 2 * fontSize, fontSize), // \n
                RectF(fontSize, fontSize, 2 * fontSize, 2 * fontSize), // b
                RectF(2 * fontSize, fontSize, 2 * fontSize, 2 * fontSize), // \n
                RectF(fontSize, 2 * fontSize, 2 * fontSize, 3 * fontSize) // c
            )
        )
    }

    @Test
    fun multiLineRtl() {
        val text = "\u05D0\n\u05D1\n\u05D2"
        val width = 2 * fontSize
        val layout = createTextLayout(
            text = text,
            width = width
        )

        assertThat(layout.getBoundingBoxes(0, text.length)).isEqualTo(
            arrayOf(
                // left top right bottom
                RectF(width - fontSize, 0f, width, fontSize), // \u05D0
                RectF(width - fontSize, 0f, width - fontSize, fontSize), // \n
                RectF(width - fontSize, fontSize, width, 2 * fontSize), // \u05D1
                RectF(width - fontSize, fontSize, width - fontSize, 2 * fontSize), // \n
                RectF(width - fontSize, 2 * fontSize, width, 3 * fontSize) // \u05D2
            )
        )
    }

    @Test
    fun multiLineRtlCenterAligned() {
        val text = "\u05D0\n\u05D1\n\u05D2"
        val width = 3 * fontSize
        val layout = createTextLayout(
            text = text,
            width = width,
            alignment = LayoutCompat.ALIGN_CENTER
        )

        assertThat(layout.getBoundingBoxes(0, text.length)).isEqualTo(
            arrayOf(
                // left top right bottom
                RectF(fontSize, 0f, 2 * fontSize, fontSize), // \u05D0
                RectF(fontSize, 0f, fontSize, fontSize), // \n
                RectF(fontSize, fontSize, 2 * fontSize, 2 * fontSize), // \u05D1
                RectF(fontSize, fontSize, fontSize, 2 * fontSize), // \n
                RectF(fontSize, 2 * fontSize, fontSize * 2, 3 * fontSize) // \u05D2
            )
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = 24)
    fun zwjEmoji() {
        // Emoji 2.0 - family: man, woman, girl, boy
        // 2.0 released in Nov 2015; min version is set to SDK 24 which was released in 2016
        val text = "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67\u200D\uD83D\uDC66"
        val layout = createTextLayout(text = text)

        val expected = layout.getBoundingBoxes(0, text.length)

        // since we do not use the test font, the first rect should be non-zero
        // the remaining characters should have 0 width starting from the right of the
        // first character
        val initialRect = expected[0]
        assertThat(initialRect.right - initialRect.left).isNonZero()
        for (index in 1 until expected.size) {
            assertThat(expected[index]).isEqualTo(
                RectF(initialRect.right, initialRect.top, initialRect.right, initialRect.bottom)
            )
        }
    }

    @Test
    fun withStyling() {
        val doubleFontSize = fontSize * 2
        val text = SpannableStringBuilder().apply {
            append("a")
            append("b")
            setSpan(AbsoluteSizeSpan(doubleFontSize.toInt()), 1, 2, 0)
            append("c")
        }

        val layout = createTextLayout(
            text = text
        )

        assertThat(layout.getBoundingBoxes(0, text.length)).isEqualTo(
            arrayOf(
                // 1 width for a, height is doubleFontSize since line metrics change
                RectF(0f, 0f, fontSize, doubleFontSize),
                // 2 width for b
                RectF(fontSize, 0f, 3 * fontSize, doubleFontSize),
                // 1 width for c
                RectF(3 * fontSize, 0f, 4 * fontSize, doubleFontSize)
            )
        )
    }

    private fun TextLayout.getBoundingBoxes(startOffset: Int, endOffset: Int): Array<RectF> {
        val range = endOffset - startOffset
        val arraySize = range * 4
        val array = FloatArray(arraySize)
        this.fillBoundingBoxes(startOffset, endOffset, array, 0)
        return array.asRectF()
    }

    private fun FloatArray.asRectF(): Array<RectF> {
        return Array((size) / 4) { index ->
            RectF(
                this[4 * index],
                this[4 * index + 1],
                this[4 * index + 2],
                this[4 * index + 3]
            )
        }
    }

    private fun ltrCharacterBoundariesForTestFont(text: String): Array<RectF> {
        val array = FloatArray(text.length * 4)
        text.indices.forEach { index ->
            array[4 * index] = index * fontSize
            array[4 * index + 1] = 0f
            array[4 * index + 2] = (index + 1) * fontSize
            array[4 * index + 3] = fontSize
        }
        return array.asRectF()
    }

    private fun rtlCharacterBoundariesForTestFont(text: String, width: Float): Array<RectF> {
        val array = FloatArray(text.length * 4)
        text.indices.forEach { index ->
            array[4 * index] = width - (index + 1) * fontSize
            array[4 * index + 1] = 0f
            array[4 * index + 2] = width - index * fontSize
            array[4 * index + 3] = fontSize
        }
        return array.asRectF()
    }

    private fun createTextLayout(
        text: CharSequence,
        width: Float = Float.MAX_VALUE,
        lineSpacingMultiplier: Float = LayoutCompat.DEFAULT_LINESPACING_MULTIPLIER,
        alignment: Int = LayoutCompat.DEFAULT_ALIGNMENT,
    ): TextLayout {
        val textPaint = TextPaint().apply {
            typeface = sampleTypeface
            textSize = fontSize
        }

        return TextLayout(
            charSequence = text,
            width = width,
            textPaint = textPaint,
            lineSpacingMultiplier = lineSpacingMultiplier,
            alignment = alignment
        )
    }
}
