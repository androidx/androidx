/*
 * Copyright 2020 The Android Open Source Project
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

import android.text.TextPaint
import androidx.compose.ui.text.font.test.R
import androidx.core.content.res.ResourcesCompat
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@SmallTest
@OptIn(InternalPlatformTextApi::class)
@RunWith(AndroidJUnit4::class)
class LayoutGetHorizontalMultiLineTest {

    private val sampleTypeface = ResourcesCompat.getFont(
        InstrumentationRegistry.getInstrumentation().targetContext,
        R.font.sample_font
    )

    private val fontSize = 10f
    private val width = 3 * fontSize

    private val paint = TextPaint().apply {
        textSize = fontSize
        typeface = sampleTypeface
    }

    private fun createLayout(
        text: String,
        textDirectionHeuristic: Int
    ): TextLayout = TextLayout(
        charSequence = text,
        textPaint = paint,
        textDirectionHeuristic = textDirectionHeuristic,
        width = width
    )

    @Test
    fun ltrTextLtrDirection() {
        val char = "a"
        val text = (char + "\n").repeat(4)
        val layout = createLayout(text, LayoutCompat.TEXT_DIRECTION_LTR)
        val result = collectResult(text, layout)

        // width is 3 chars
        // \n is zero width
        // _ is empty space (not white space)
        // | → | \n | _ | _ |
        // | → | \n | _ | _ |
        // | → | \n | _ | _ |
        // | → | \n | _ | _ |
        // | _ | _  | _ | _ | // as a result of last new line
        val lineStart = 0f
        val lineEnd = fontSize
        assertThat(result).isEqualTo(
            arrayOf(
                // first ltr char in ltr direction is always at line start
                Pos(lineStart),
                // new line: all values are the end of the line
                Pos(lineEnd),
                // char at the beginning of line, upstream is previous lineEnd
                Pos(
                    primaryUpstream = lineEnd,
                    primaryDownstream = lineStart,
                    secondaryUpstream = lineEnd,
                    secondaryDownstream = lineStart
                ),
                // remaining is the repetition
                Pos(lineEnd),
                Pos(lineEnd, lineStart, lineEnd, lineStart),
                Pos(lineEnd),
                Pos(lineEnd, lineStart, lineEnd, lineStart),
                Pos(lineEnd),
            )
        )
    }

    @Test
    fun rtlTextRtlDirection() {
        val char = "\u05D0"
        val text = (char + "\n").repeat(4)
        val layout = createLayout(text, LayoutCompat.TEXT_DIRECTION_RTL)
        val result = collectResult(text, layout)

        // width is 3 chars
        // \n is zero width
        // _ is empty space (not white space)
        // | _ | _ | \n | ← |
        // | _ | _ | \n | ← |
        // | _ | _ | \n | ← |
        // | _ | _ | \n | ← |
        // | _ | _  | _ | _ | // as a result of last new line
        val lineStart = width
        val lineEnd = width - fontSize
        assertThat(result).isEqualTo(
            arrayOf(
                // first rtl char in rtl direction is always at line start
                Pos(lineStart),
                // new line: all values are the end of the line
                Pos(lineEnd),
                // char at the beginning of line, upstream is previous lineEnd
                Pos(
                    primaryUpstream = lineEnd,
                    primaryDownstream = lineStart,
                    secondaryUpstream = lineEnd,
                    secondaryDownstream = lineStart
                ),
                // remaining is the repetition
                Pos(lineEnd),
                Pos(lineEnd, lineStart, lineEnd, lineStart),
                Pos(lineEnd),
                Pos(lineEnd, lineStart, lineEnd, lineStart),
                Pos(lineEnd)
            )
        )
    }

    /**
     * This case is minimum repro for a magnifier tests.
     *
     * Failure was java.lang.IllegalArgumentException: Invalid ranges (start=2, limit=3, length=2)
     *
     * State is:
     *  RTL text
     *  LTR TextDirection
     *  multi line with line feeds in between
     */
    @Test
    fun rtlTextLtrDirection() {
        val char = "\u05D0"
        val text = (char + "\n").repeat(4)
        val layout = createLayout(text, LayoutCompat.TEXT_DIRECTION_LTR)
        val result = collectResult(text, layout)

        // width is 3 chars
        // \n is zero width
        // _ is empty space (not white space)
        // | ← | \n | _ | _ |
        // | ← | \n | _ | _ |
        // | ← | \n | _ | _ |
        // | ← | \n | _ | _ |
        // | _ | _  | _ | _ | // as a result of last new line

        assertThat(result).isEqualTo(
            arrayOf(
                Pos(
                    // primary (ltr) direction puts the position to 0, first char has no upstream
                    primaryUpstream = 0f,
                    primaryDownstream = 0f,
                    // secondary starts writing from 1 char right which is the line end
                    secondaryUpstream = fontSize,
                    secondaryDownstream = fontSize
                ),
                // new line
                Pos(
                    // new line for ltr starts from lineEnd
                    primaryUpstream = fontSize,
                    primaryDownstream = fontSize,
                    // new line for rtl continues on 0 on new line
                    secondaryUpstream = 0f,
                    secondaryDownstream = 0f
                ),
                // second line first char
                Pos(
                    // primary ltr direction upstream is previous line end
                    primaryUpstream = fontSize,
                    // primary ltr direction downstream is next line start
                    primaryDownstream = 0f,
                    // rtl direction *** I did not understand this part ***
                    secondaryUpstream = 0f,
                    // downstream is where character is rendered, from right to left starting
                    // from the 1 char width
                    secondaryDownstream = fontSize
                ),
                // remaining is the repetition
                Pos(fontSize, fontSize, 0f, 0f),
                Pos(fontSize, 0f, 0f, fontSize),
                Pos(fontSize, fontSize, 0f, 0f),
                Pos(fontSize, 0f, 0f, fontSize),
                Pos(fontSize, fontSize, 0f, 0f),
            )
        )
    }

    @Test
    fun ltrTextRtlDirection() {
        val char = "a"
        val text = (char + "\n").repeat(4)
        val layout = createLayout(text, LayoutCompat.TEXT_DIRECTION_RTL)
        val result = collectResult(text, layout)

        // width is 3 chars
        // \n is zero width
        // _ is empty space (not white space)
        // | _ | _ | \n | → |
        // | _ | _ | \n | → |
        // | _ | _ | \n | → |
        // | _ | _ | \n | → |
        // | _ | _ | _  | _ | // as a result of last new line
        val lineStart = width
        val lineEnd = width - fontSize
        assertThat(result).isEqualTo(
            arrayOf(
                Pos(
                    // rtl direction line start is width
                    primaryUpstream = width,
                    // rtl direction no upstream for first char
                    primaryDownstream = width,
                    // ltr direction char, *** I did not understand this part ***
                    secondaryUpstream = lineEnd,
                    secondaryDownstream = lineEnd
                ),
                Pos(
                    primaryUpstream = lineEnd,
                    primaryDownstream = lineEnd,
                    secondaryUpstream = width,
                    secondaryDownstream = width
                ),
                Pos(
                    primaryUpstream = lineEnd,
                    primaryDownstream = width,
                    secondaryUpstream = width,
                    secondaryDownstream = lineEnd
                ),
                // remaining is the repetition
                Pos(lineEnd, lineEnd, width, lineStart),
                Pos(lineEnd, width, width, lineEnd),
                Pos(lineEnd, lineEnd, width, width),
                Pos(lineEnd, width, width, lineEnd),
                Pos(lineEnd, lineEnd, width, width)
            )
        )
    }

    private fun collectResult(text: String, layout: TextLayout): Array<Pos> {
        return text.indices.map { index ->
            Pos(
                primaryUpstream = layout.getPrimaryHorizontal(offset = index, upstream = true),
                primaryDownstream = layout.getPrimaryHorizontal(offset = index, upstream = false),
                secondaryUpstream = layout.getSecondaryHorizontal(offset = index, upstream = true),
                secondaryDownstream = layout.getSecondaryHorizontal(
                    offset = index,
                    upstream = false
                )
            )
        }.toTypedArray()
    }

    private data class Pos(
        val primaryUpstream: Float,
        val primaryDownstream: Float,
        val secondaryUpstream: Float,
        val secondaryDownstream: Float,
    ) {
        constructor(value: Float) : this(
            primaryUpstream = value,
            primaryDownstream = value,
            secondaryUpstream = value,
            secondaryDownstream = value
        )
    }
}