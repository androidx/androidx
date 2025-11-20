/*
 * Copyright 2023 The Android Open Source Project
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

import android.graphics.Typeface
import android.os.Build
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.res.ResourcesCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.fonts.R
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class TextLayoutLineVisibleEndTest {
    private lateinit var sampleTypeface: Typeface

    @Before
    fun setup() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        // This sample font provides the following features:
        // 1. The width of most of visible characters equals to font size.
        // 2. The LTR/RTL characters are rendered as ▶/◀.
        // 3. The fontMetrics passed to TextPaint has descend - ascend equal to 1.2 * fontSize.
        // 4. The fontMetrics passed to TextPaint has ascend equal to fontSize.
        sampleTypeface = ResourcesCompat.getFont(instrumentation.context, R.font.sample_font)!!
    }

    @Test
    fun singleLine_withEllipsisStart() {
        val text = "abcdefghij"
        val textSize = 20.0f

        val layout =
            simpleLayout(
                text = text,
                textSize = textSize,
                layoutWidth = textSize * 4,
                maxLines = 1,
                ellipsize = TextUtils.TruncateAt.START,
            )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            assertThat(layout.getLineVisibleEnd(0)).isEqualTo(10)
        } else {
            assertThat(layout.getLineVisibleEnd(0)).isEqualTo(4)
        }
    }

    @Test
    fun singleLine_withEllipsisMiddle() {
        val text = "abcdefghij"
        val textSize = 20.0f

        val layout =
            simpleLayout(
                text = text,
                textSize = textSize,
                layoutWidth = textSize * 4,
                maxLines = 1,
                ellipsize = TextUtils.TruncateAt.MIDDLE,
            )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            assertThat(layout.getLineVisibleEnd(0)).isEqualTo(10)
        } else {
            assertThat(layout.getLineVisibleEnd(0)).isEqualTo(4)
        }
    }

    @Test
    fun excludesLineBreak_whenMaxLinesPresent_withoutEllipsis() {
        val text = "abc\ndef"
        val textSize = 20.0f

        val layout =
            simpleLayout(
                text = text,
                textSize = textSize,
                layoutWidth = textSize * 10,
                maxLines = 1,
            )

        assertThat(layout.getLineVisibleEnd(0)).isEqualTo(3)
    }

    @Test
    fun excludesLineBreak_whenMaxLinesPresent_withEllipsisEnd() {
        val text = "abc\ndef"
        val textSize = 20.0f

        val layout =
            simpleLayout(
                text = text,
                textSize = textSize,
                layoutWidth = textSize * 10,
                maxLines = 1,
                ellipsize = TextUtils.TruncateAt.END,
            )

        assertThat(layout.getLineVisibleEnd(0)).isEqualTo(3)
    }

    @Test
    fun excludesLineBreak_whenMaxLinesPresent_withEllipsisStart() {
        val text = "abc\ndef"
        val textSize = 20.0f

        val layout =
            simpleLayout(
                text = text,
                textSize = textSize,
                layoutWidth = textSize * 10,
                maxLines = 1,
                ellipsize = TextUtils.TruncateAt.START,
            )

        assertThat(layout.getLineVisibleEnd(0)).isEqualTo(3)
    }

    @Test
    fun excludesLineBreak_whenMaxLinesPresent_withEllipsisMiddle() {
        val text = "abc\ndef"
        val textSize = 20.0f

        val layout =
            simpleLayout(
                text = text,
                textSize = textSize,
                layoutWidth = textSize * 10,
                maxLines = 1,
                ellipsize = TextUtils.TruncateAt.MIDDLE,
            )

        assertThat(layout.getLineVisibleEnd(0)).isEqualTo(3)
    }

    @Test
    fun excludesWhitespace_singleLineContent_withEllipsis() {
        val text = "abc def ghi"
        val textSize = 20.0f
        val layoutWidth = textSize * 10

        val layout =
            simpleLayout(
                text = text,
                textSize = textSize,
                layoutWidth = layoutWidth,
                ellipsize = TextUtils.TruncateAt.END,
            )

        assertThat(layout.getLineVisibleEnd(0)).isEqualTo(7)
    }

    @Test
    fun excludesWhitespace_multiLineContent_withoutEllipsis() {
        val text = "abc def    \nghi"
        val textSize = 20.0f
        val layoutWidth = textSize * 10

        val layout = simpleLayout(text = text, textSize = textSize, layoutWidth = layoutWidth)

        // the way overflown text layout is calculated with ellipsis is vastly different before and
        // after API 23. Line visible end logic cannot be unified below API 23.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            assertThat(layout.getLineVisibleEnd(0)).isEqualTo(7)
        } else {
            assertThat(layout.getLineVisibleEnd(0)).isEqualTo(3)
        }
    }

    @Test
    fun excludesWhitespace_multiLineContent_withEllipsis() {
        val text = "abc def    \nghi"
        val textSize = 20.0f
        val layoutWidth = textSize * 10

        val layout =
            simpleLayout(
                text = text,
                textSize = textSize,
                layoutWidth = layoutWidth,
                ellipsize = TextUtils.TruncateAt.END,
            )

        // the way overflown text layout is calculated with ellipsis is vastly different before and
        // after API 23. Line visible end logic cannot be unified below API 23.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            assertThat(layout.getLineVisibleEnd(0)).isEqualTo(7)
        } else {
            assertThat(layout.getLineVisibleEnd(0)).isEqualTo(3)
        }
    }

    @Test
    fun noVisibleContent_multiLine_withoutEllipsis() {
        val text = "\n\nabc"
        val textSize = 20.0f
        val layoutWidth = textSize * 10

        val layout =
            simpleLayout(text = text, textSize = textSize, layoutWidth = layoutWidth, maxLines = 2)

        assertThat(layout.getLineVisibleEnd(0)).isEqualTo(0)
        assertThat(layout.getLineVisibleEnd(1)).isEqualTo(1)
    }

    @Test
    fun noVisibleContent_multiLine_withEllipsis() {
        val text = "\n\nabc"
        val textSize = 20.0f
        val layoutWidth = textSize * 10

        val layout =
            simpleLayout(
                text = text,
                textSize = textSize,
                layoutWidth = layoutWidth,
                maxLines = 2,
                ellipsize = TextUtils.TruncateAt.END,
            )

        assertThat(layout.getLineVisibleEnd(0)).isEqualTo(0)
        assertThat(layout.getLineVisibleEnd(1)).isEqualTo(1)
    }

    private fun simpleLayout(
        text: CharSequence,
        textSize: Float = Float.NaN,
        layoutWidth: Float = textSize * text.length,
        ellipsize: TextUtils.TruncateAt? = null,
        maxLines: Int = Int.MAX_VALUE,
    ): TextLayout {
        val textPaint = TextPaint()
        textPaint.typeface = sampleTypeface
        textPaint.textSize = if (!textSize.isNaN()) textSize else 14f

        return TextLayout(
            charSequence = text,
            width = layoutWidth,
            textPaint = textPaint,
            maxLines = maxLines,
            ellipsize = ellipsize,
        )
    }
}
