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

import android.text.TextPaint
import android.text.TextUtils.TruncateAt
import androidx.core.content.res.ResourcesCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.fonts.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(InternalPlatformTextApi::class)
@RunWith(AndroidJUnit4::class)
@SmallTest
class TextLayoutIsLineEllipsizedTest {
    private val sampleTypeface = ResourcesCompat.getFont(
        InstrumentationRegistry.getInstrumentation().context, R.font.sample_font
    )!!
    private val fontSize = 10f
    private val maxLines = 5
    private val charsInline = 10
    private val width = charsInline * fontSize
    private val ltrText = "a".repeat(charsInline).repeat(maxLines * 2)
    private val rtlText = "\u05D0".repeat(charsInline).repeat(maxLines * 2)

    @Test
    fun notEllipsized_ltr() {
        val textLayout = EllipsizedTextLayout(ltrText, null)
        textLayout.assertNotEllipsized()
    }

    @Test
    fun notEllipsized_rtl() {
        val textLayout = EllipsizedTextLayout(rtlText, null)
        textLayout.assertNotEllipsized()
    }

    @Test
    fun ellipsizeEnd_end_ltr() {
        val textLayout = EllipsizedTextLayout(ltrText, TruncateAt.END)
        textLayout.assertEndEllipsized()
    }

    @Test
    fun ellipsizeEnd_end_rtl() {
        val textLayout = EllipsizedTextLayout(rtlText, TruncateAt.END)
        textLayout.assertEndEllipsized()
    }

    @Test
    fun ellipsizeEnd_start_ltr() {
        // TruncateAt.START works only for single line
        val textLayout = EllipsizedTextLayout(ltrText, TruncateAt.START, maxLines = 1)
        textLayout.assertStartEllipsized()
    }

    @Test
    fun ellipsizeEnd_start_rtl() {
        // TruncateAt.START works only for single line
        val textLayout = EllipsizedTextLayout(rtlText, TruncateAt.START, maxLines = 1)
        textLayout.assertStartEllipsized()
    }

    private fun TextLayout.assertEndEllipsized() {
        for (line in 0 until this.lineCount) {
            if (line == maxLines - 1) {
                assertThat(this.isLineEllipsized(line)).isTrue()
            } else {
                assertThat(this.isLineEllipsized(line)).isFalse()
            }
        }
    }

    private fun TextLayout.assertStartEllipsized() {
        for (line in 0 until this.lineCount) {
            if (line == 0) {
                assertThat(this.isLineEllipsized(line)).isTrue()
            } else {
                assertThat(this.isLineEllipsized(line)).isFalse()
            }
        }
    }

    private fun TextLayout.assertNotEllipsized() {
        for (line in 0 until this.lineCount) {
            assertThat(this.isLineEllipsized(line)).isFalse()
        }
    }

    private fun EllipsizedTextLayout(
        text: CharSequence,
        truncateAt: TruncateAt?,
        maxLines: Int = this.maxLines
    ): TextLayout {
        val textPaint = TextPaint().apply {
            this.typeface = sampleTypeface
            this.textSize = fontSize
        }

        return TextLayout(
            charSequence = text,
            textPaint = textPaint,
            ellipsize = truncateAt,
            width = width,
            maxLines = maxLines
        )
    }
}
